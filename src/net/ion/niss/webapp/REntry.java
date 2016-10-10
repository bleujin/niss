package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.script.ScriptException;
import javax.ws.rs.core.MultivaluedMap;

import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.schedule.AtTime;
import net.ion.framework.schedule.Job;
import net.ion.framework.schedule.ScheduledRunnable;
import net.ion.framework.schedule.Scheduler;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.Script;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.SearchManager;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.niss.webapp.misc.ScriptWeb;
import net.ion.niss.webapp.sites.SiteManager;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.config.IndexConfig;
import net.ion.nsearcher.config.SearchConfig;
import net.ion.nsearcher.search.CompositeSearcher;
import net.ion.nsearcher.search.Searcher;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.util.concurrent.WithinThreadExecutor;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

public class REntry implements Closeable {

	public final static String EntryName = "rentry";

	private Craken r;
	private String wsName;
	private NSConfig nsconfig;

	private IndexManager indexManager = new IndexManager();
	private SearchManager searchManager = new SearchManager();
	private SiteManager siteManager = new SiteManager(indexManager);

	private ReadSession rsession;
	private final Log log = LogFactory.getLog(REntry.class);
	private Scheduler scheduler = new Scheduler("scripter", Executors.newCachedThreadPool(ThreadFactoryBuilder.createThreadFactory("scripters-thread-%d")));

	public REntry(Craken r, String wsName, NSConfig nsconfig) throws IOException {
		this.r = r;
		this.wsName = wsName;
		this.nsconfig = nsconfig;

		this.rsession = login();
		initCDDListener(rsession);
	}

	// use test only (central not finished)
	public void reload() {
		indexManager = new IndexManager();
		searchManager = new SearchManager();
		siteManager = new SiteManager(indexManager) ;
		initCDDListener(rsession);
	}

	private void initCDDListener(final ReadSession session) {

		final JScriptEngine jsengine = JScriptEngine.create();

		// load index
		// session.ghostBy("/indexers").children().debugPrint();

		session.ghostBy("/indexers").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {
				try {
					for (ReadNode indexNode : iter) {
						IdString cid = IdString.create(indexNode.fqn().name());

						Central central = createIndexerCentral(cid);

						Analyzer indexAnal = makeAnalyzer(new RNodePropertyReadable(indexNode), indexNode.property(Def.Indexer.IndexAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()));
						Analyzer queryAnal = makeQueryAnalyzer(new RNodePropertyReadable(indexNode), indexNode.property(Def.Indexer.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()));

						central.indexConfig().indexAnalyzer(indexAnal);
						central.searchConfig().queryAnalyzer(queryAnal);

						ReadChildren schemas = session.ghostBy(indexNode.fqn().toString() + "/schema").children();
						for (ReadNode schemaNode : schemas.toList()) {
							if (StringUtil.equals(Def.SchemaType.MANUAL, schemaNode.property(Def.IndexSchema.SchemaType).asString())) {
								central.indexConfig().fieldAnalyzer(schemaNode.fqn().name(), makeAnalyzer(schemaNode.property(Def.IndexSchema.Analyzer).asString()));
							}
						}

						indexManager.newIndex(cid, central);
						log.info(cid + " indexer loaded");
					}
				} catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
				return null;
			}

		});

		// load searcher
		session.ghostBy("/searchers").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {

				try {
					for (ReadNode sec : iter) {
						IdString sid = registerSearcher(session, new RNodePropertyReadable(sec), jsengine);
						log.info(sid + " searcher loaded");
					}
				} catch (IOException ex) {
					throw new IllegalStateException(ex);
				} catch (ClassNotFoundException ex) {
					throw new IllegalStateException(ex);
				} catch (InstantiationException ex) {
					throw new IllegalStateException(ex);
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException(ex);
				} catch (InvocationTargetException ex) {
					throw new IllegalStateException(ex);
				}

				return null;
			}

		});

		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/searchers/{sid}";
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				try {
					IdString sid = registerSearcher(session, new EventPropertyReadable(cevent), jsengine);
					log.info(sid + " searcher defined");
				} catch (IOException ex) {
					throw new IllegalStateException(ex);
				} catch (ClassNotFoundException ex) {
					throw new IllegalStateException(ex);
				} catch (InstantiationException ex) {
					throw new IllegalStateException(ex);
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException(ex);
				} catch (InvocationTargetException ex) {
					throw new IllegalStateException(ex);
				}
				return null;
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				IdString sid = IdString.create(rmap.get("sid"));
				searchManager.removeSearcher(sid);
				return null;
			}
		});

		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/indexers/{iid}/schema/{schemaid}";
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> rmap, CDDRemovedEvent event) {
				IdString iid = IdString.create(rmap.get("iid"));
				String schemaid = rmap.get("schemaid");
				if (!indexManager.hasIndex(iid))
					return null;

				Central saved = indexManager.index(iid);
				saved.indexConfig().removeFieldAnalyzer(schemaid);

				return null;
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent event) {
				IdString iid = IdString.create(rmap.get("iid"));
				String schemaid = rmap.get("schemaid");

				if (!indexManager.hasIndex(iid))
					return null;
				event.getValue();
				if (Def.SchemaType.MANUAL.equals(event.property(Def.IndexSchema.SchemaType).asString()) && StringUtil.isNotBlank(event.property(Def.IndexSchema.Analyzer).asString())) {
					try {
						Central saved = indexManager.index(iid);
						Analyzer analClz = makeAnalyzer(event.property(Def.IndexSchema.Analyzer).asString());
						saved.indexConfig().fieldAnalyzer(schemaid, analClz);

					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
				;

				return null;
			}
		});

		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/searchers/{sid}/schema/{schemaid}";
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> rmap, CDDRemovedEvent event) {
				IdString sid = IdString.create(rmap.get("sid"));
				String schemaid = rmap.get("schemaid");
				if (!searchManager.hasSearch(sid))
					return null;

				Searcher saved = searchManager.searcher(sid);
				saved.config().removeFieldAnalyzer(schemaid);
				return null;
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent event) {
				IdString sid = IdString.create(rmap.get("sid"));
				String schemaid = rmap.get("schemaid");

				if (!searchManager.hasSearch(sid))
					return null;
				try {
					Searcher saved = searchManager.searcher(sid);
					Analyzer analClz = makeAnalyzer(event.property(Def.IndexSchema.Analyzer).asString());
					saved.config().fieldAnalyzer(schemaid, analClz);

				} catch (Exception e) {
					throw new IllegalStateException(e);
				}

				return null;
			}
		});

		// add index listener
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/indexers/{iid}";
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				IdString iid = IdString.create(rmap.get("iid"));
				try {
					if (!indexManager.hasIndex(iid)) { // created
						Central central = createIndexerCentral(iid);

						indexManager.newIndex(iid, central);
						log.info(iid + " indexer defined");
					} else if (indexManager.hasIndex(iid)) {

						Central saved = indexManager.index(iid);

						String indexAnalClzName = StringUtil.defaultIfEmpty(cevent.property(Def.Indexer.IndexAnalyzer).asString(), saved.indexConfig().indexAnalyzer().getClass().getCanonicalName());
						saved.indexConfig().indexAnalyzer(makeAnalyzer(new EventPropertyReadable(cevent), indexAnalClzName));

						String queryAnalClzName = StringUtil.defaultIfEmpty(cevent.property(Def.Indexer.QueryAnalyzer).asString(), saved.searchConfig().queryAnalyzer().getClass().getCanonicalName());
						saved.searchConfig().queryAnalyzer(makeQueryAnalyzer(new EventPropertyReadable(cevent), queryAnalClzName));

						log.info(iid + " indexer defined");
					} else {
						throw new IllegalArgumentException("not have index " + iid.idString());
					}

				} catch (IOException ex) {
					throw new IllegalStateException(ex);
				} catch (ClassNotFoundException ex) {
					throw new IllegalStateException(ex);
				} catch (IllegalArgumentException ex) {
					throw new IllegalStateException(ex);
				} catch (SecurityException ex) {
					throw new IllegalStateException(ex);
				} catch (InstantiationException ex) {
					throw new IllegalStateException(ex);
				} catch (IllegalAccessException ex) {
					throw new IllegalStateException(ex);
				} catch (InvocationTargetException ex) {
					throw new IllegalStateException(ex);
				}

				return null;
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				IdString iid = IdString.create(rmap.get("iid"));

				indexManager.removeIndex(iid);

				return null;
			}
		});

		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/scripts/{sid}/schedule";
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				String sid = rmap.get("sid");
				scheduler.removeJob(sid);

				return null;
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				EventPropertyReadable rnode = new EventPropertyReadable(cevent);
				String jobId = rmap.get("sid");
				if (rnode.property(Def.Schedule.ENABLE).asBoolean()) {
					ReadNode sinfo = rsession.ghostBy("/scripts/" + jobId + "/schedule");
					scheduler.removeJob(jobId);
					AtTime at = makeAtTime(sinfo);
					scheduler.addJob(new Job(jobId, makeCallable(jobId), at));
				} else {
					scheduler.removeJob(jobId);
				}

				return null;
			}

			private ScheduledRunnable makeCallable(final String scriptId) {
				return new ScheduledRunnable() {
					@Override
					public void run() {
						final ReadNode scriptNode = rsession.ghostBy("/scripts/" + scriptId);

						// should check running(in distribute mode)
						if (scriptNode.property(Script.Running).asBoolean())
							return;
						rsession.tran(new TransactionJob<Void>() {
							@Override
							public Void handle(WriteSession wsession) throws Exception {
								wsession.pathBy(scriptNode.fqn()).property(Script.Running, true);
								return null;
							}
						});
						//

						String scriptContent = scriptNode.property(Def.Script.Content).asString();
						StringWriter result = new StringWriter();
						final JsonWriter jwriter = new JsonWriter(result);

						try {
							StringWriter writer = new StringWriter();
							MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>();
							InstantJavaScript script = jsengine.createScript(IdString.create(scriptId), "", new StringReader(scriptContent));

							String[] execResult = script.exec(new ResultHandler<String[]>() {
								@Override
								public String[] onSuccess(Object result, Object... args) {
									try {
										jwriter.beginObject().name("return").value(ObjectUtil.toString(result));
									} catch (IOException ignore) {
									} finally {
										rsession.tran(new TransactionJob<Void>() {
											@Override
											public Void handle(WriteSession wsession) throws Exception {
												wsession.pathBy(scriptNode.fqn()).property(Script.Running, false);
												return null;
											}
										});
									}
									return new String[] { "schedule success", ObjectUtil.toString(result) };
								}

								@Override
								public String[] onFail(Exception ex, Object... args) {
									try {
										jwriter.beginObject().name("return").value("").name("exception").value(ex.getMessage());
									} catch (IOException e) {
									} finally {
										rsession.tran(new TransactionJob<Void>() {
											@Override
											public Void handle(WriteSession wsession) throws Exception {
												wsession.pathBy(scriptNode.fqn()).property(Script.Running, false);
												return null;
											}
										});
									}
									return new String[] { "schedule fail", ex.getMessage() };
								}
							}, writer, rsession, params, REntry.this, jsengine);

							jwriter.name("writer").value(writer.toString());

							jwriter.name("params");
							jwriter.beginArray();
							for (Entry<String, List<String>> entry : params.entrySet()) {
								jwriter.beginObject().name(entry.getKey()).beginArray();
								for (String val : entry.getValue()) {
									jwriter.value(val);
								}
								jwriter.endArray().endObject();
							}
							jwriter.endArray();
							jwriter.endObject();
							jwriter.close();
							rsession.tran(ScriptWeb.end(scriptId, execResult[0], execResult[1]));
						} catch (IOException ex) {
							rsession.tran(ScriptWeb.end(scriptId, "schedule fail", ex.getMessage()));
						} catch (ScriptException ex) {
							rsession.tran(ScriptWeb.end(scriptId, "schedule fail", ex.getMessage()));
						} finally {
							IOUtil.close(jwriter);
						}
						// write log

					}
				};
			}

			private AtTime makeAtTime(ReadNode sinfo) {
				String expr = StringUtil.coalesce(sinfo.property("minute").asString(), "*") + " " + StringUtil.coalesce(sinfo.property("hour").asString(), "*") + " " + StringUtil.coalesce(sinfo.property("day").asString(), "*") + " " + StringUtil.coalesce(sinfo.property("month").asString(), "*")
						+ " " + StringUtil.coalesce(sinfo.property("week").asString(), "*") + " " + StringUtil.coalesce(sinfo.property("matchtime").asString(), "*") + " " + StringUtil.coalesce(sinfo.property("year").asString(), "*");

				return new AtTime(expr);
			}

		});

		scheduler.start();

		// register schedule job
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				IteratorList<WriteNode> scripts = wsession.pathBy("/scripts").children().iterator();
				while (scripts.hasNext()) {
					WriteNode wnode = scripts.next();
					wnode.property(Script.Running, false);
					if (wnode.hasChild("schedule")) {
						WriteNode scheduleNode = wnode.child("schedule");
						if (scheduleNode.property(Def.Schedule.ENABLE).asBoolean()) {
							scheduleNode.property(Def.Schedule.ENABLE, true);
						}
					}
				}
				return null;
			}
		});

	}

	// create analyzer
	private Analyzer makeAnalyzer(PropertyReadable rnode, String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (StringUtil.isBlank(modValue))
			return new StandardAnalyzer(SearchConstant.LuceneVersion);

		Class<Analyzer> indexAnalClz = (Class<Analyzer>) Class.forName(modValue);

		Analyzer resultAnalyzer = null;
		Constructor con = null;
		if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[] { Version.class, CharArraySet.class })) != null) {

			boolean useStopword = rnode.property(Def.Indexer.ApplyStopword).asBoolean();
			Collection<String> stopWord = ListUtil.EMPTY;
			if (useStopword) {
				String stopwords = rnode.property(Def.Indexer.StopWord).asString();
				stopWord = ListUtil.toList(StringUtil.split(stopwords, " ,\n"));
			}

			resultAnalyzer = (Analyzer) con.newInstance(SearchConstant.LuceneVersion, new CharArraySet(SearchConstant.LuceneVersion, stopWord, false));
		} else if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[] { Version.class })) != null) {
			resultAnalyzer = (Analyzer) con.newInstance(SearchConstant.LuceneVersion);
		} else {
			con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[0]);
			resultAnalyzer = (Analyzer) con.newInstance();
		}
		return resultAnalyzer;
	}

	// create sub per field analayzer
	private Analyzer makeAnalyzer(String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {

		Class<Analyzer> indexAnalClz = (Class<Analyzer>) Class.forName(modValue);

		Analyzer indexAnal = null;
		Constructor con = null;
		if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[] { Version.class, CharArraySet.class })) != null) {
			Collection<String> stopWord = ListUtil.EMPTY;
			indexAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion, new CharArraySet(SearchConstant.LuceneVersion, stopWord, false));
		} else if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[] { Version.class })) != null) {
			indexAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion);
		} else {
			con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[0]);
			indexAnal = (Analyzer) con.newInstance();
		}
		return indexAnal;
	}

	// with no stopword(when indexers)
	private Analyzer makeQueryAnalyzer(PropertyReadable rnode, String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?> queryAnalClz = Class.forName(modValue);

		Analyzer queryAnal = null;
		Constructor con = null;
		if ((con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[] { Version.class, CharArraySet.class })) != null) {
			queryAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion, new CharArraySet(SearchConstant.LuceneVersion, ListUtil.EMPTY, false));
		} else if ((con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[] { Version.class })) != null) {
			queryAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion);
		} else {
			con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[0]);
			queryAnal = (Analyzer) con.newInstance();
		}
		return queryAnal;
	}

	private IdString registerSearcher(ReadSession session, PropertyReadable rnode, JScriptEngine jsengine) throws CorruptIndexException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Set<String> cols = rnode.property(Def.Searcher.Target).asSet();
		IdString sid = IdString.create(rnode.fqn().name());

		Searcher searcher = null;
		if (cols.size() == 0) {
			searcher = CompositeSearcher.createBlank();
		} else {
			List<Central> target = ListUtil.newList();
			for (String colId : cols) {
				if (indexManager.hasIndex(colId)) {
					target.add(indexManager.index(colId));
				}
			}

			Analyzer queryAnalyzer = makeAnalyzer(rnode, rnode.property(Def.Searcher.QueryAnalyzer).asString());
			SearchConfig sconfig = SearchConfig.create(new WithinThreadExecutor(), SearchConstant.LuceneVersion, queryAnalyzer, SearchConstant.ISALL_FIELD);
			IndexConfig iconfig = IndexConfig.create(SearchConstant.LuceneVersion, new WithinThreadExecutor(), queryAnalyzer, new IndexWriterConfig(SearchConstant.LuceneVersion, queryAnalyzer), FieldIndexingStrategy.DEFAULT);

			ReadChildren schemas = session.ghostBy(rnode.fqn().toString() + "/schema").children();
			for (ReadNode schemaNode : schemas.toList()) {
				sconfig.fieldAnalyzer(schemaNode.fqn().name(), makeAnalyzer(schemaNode.property(Def.IndexSchema.Analyzer).asString()));
			}

			searcher = CompositeSearcher.create(sconfig, iconfig, target);
		}

		String scontent = rnode.property(Def.Searcher.Handler).asString();
		if (rnode.property(Def.Searcher.ApplyHandler).asBoolean() && StringUtil.isNotBlank(scontent)) {

			StringReader contentReader = new StringReader(scontent);
			try {
				InstantJavaScript script = jsengine.createScript(IdString.create("handler"), "", contentReader);
				Searcher fsearcher = script.exec(new ResultHandler<Searcher>() {
					@Override
					public Searcher onSuccess(Object result, Object... args) {
						return (Searcher) result;
					}

					@Override
					public Searcher onFail(Exception ex, Object... args) {
						ex.printStackTrace();
						return (Searcher) args[0];
					}
				}, searcher, rsession, sid.idString());
				searchManager.newSearch(sid, fsearcher);
			} catch (Exception e) { // otherwise system cant started
				e.printStackTrace();
				searchManager.newSearch(sid, searcher);
			}
		} else {
			searchManager.newSearch(sid, searcher);
		}
		return sid;
	}

	private Central createIndexerCentral(IdString iid) throws CorruptIndexException, IOException {

		String name = iid.idString();
		DefaultCacheManager dm = r.dm();
		String path = nsconfig.repoConfig().indexHomeDir() + "/" + name;
		Configuration meta_config = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).persistence().passivation(false).addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path).async().enable()
				.flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3).build();
		dm.defineConfiguration(name + "-meta", meta_config);

		Configuration chunk_config = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).persistence().passivation(false).addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path).async().enable()
				.flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3).build();
		dm.defineConfiguration(name + "-chunk", chunk_config);

		Cache<?, ?> metaCache = dm.getCache(name + "-meta");
		Cache<?, ?> chunkCache = dm.getCache(name + "-chunk");

		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, chunkCache, metaCache, name);
		bcontext.chunkSize(16384 * 8);
		Directory directory = bcontext.create();
		Central central = CentralConfig.oldFromDir(directory).build();

		log.info(name + " Index ClusterMode : " + metaCache.getCacheConfiguration().clustering().cacheMode());

		return central;
		// return CentralConfig.newLocalFile().dirFile(new File(nsconfig.repoConfig().indexHomeDir(), iid.idString()).getCanonicalPath()).build();
	}

	// test
	public final static REntry create() throws CorruptIndexException, IOException {
		NSConfig nsconfig = ConfigBuilder.createDefault(9000).build();
		return create(nsconfig);
	}

	public final static REntry create(NSConfig nsconfig) throws CorruptIndexException, IOException {
		return nsconfig.createREntry();
	}

	public final static REntry test() throws CorruptIndexException, IOException {
		NSConfig nsconfig = ConfigBuilder.createDefault(9000).build();
		return nsconfig.testREntry();
	}

	public ReadSession login() throws IOException {
		return r.login(wsName);
	}

	public Craken repository() {
		return r;
	}

	@Override
	public void close() throws IOException {
		r.shutdown();
	}

	public IndexManager indexManager() {
		return indexManager;
	}

	public SearchManager searchManager() {
		return searchManager;
	}

	public SiteManager siteManager() {
		return siteManager;
	}

	public NSConfig nsConfig(){
		return nsconfig ;
	}
}

interface PropertyReadable {
	public PropertyValue property(String propId);

	public PropertyValue property(PropertyId propId);

	public Fqn fqn();
}

class EventPropertyReadable implements PropertyReadable {

	private CDDModifiedEvent event;

	public EventPropertyReadable(CDDModifiedEvent event) {
		this.event = event;
	}

	@Override
	public PropertyValue property(String propId) {
		return event.property(propId);
	}

	@Override
	public PropertyValue property(PropertyId propId) {
		return event.property(propId);
	}

	public Fqn fqn() {
		return event.getKey().getFqn();
	}
}

class RNodePropertyReadable implements PropertyReadable {

	private ReadNode rnode;

	public RNodePropertyReadable(ReadNode node) {
		this.rnode = node;
	}

	@Override
	public PropertyValue property(String propId) {
		return rnode.property(propId);
	}

	@Override
	public PropertyValue property(PropertyId propId) {
		return rnode.propertyId(propId);
	}

	public Fqn fqn() {
		return rnode.fqn();
	}
}
