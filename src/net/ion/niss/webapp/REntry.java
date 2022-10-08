package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.script.ScriptException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import net.bleujin.rcraken.Craken;
import net.bleujin.rcraken.Fqn;
import net.bleujin.rcraken.Property;
import net.bleujin.rcraken.ReadChildren;
import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.WriteJobNoReturn;
import net.bleujin.rcraken.WriteNode;
import net.bleujin.rcraken.WriteStream;
import net.bleujin.rcraken.extend.CDDHandler;
import net.bleujin.rcraken.extend.CDDModifiedEvent;
import net.bleujin.rcraken.extend.CDDRemovedEvent;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.SearchControllerConfig;
import net.bleujin.searcher.Searcher;
import net.ion.framework.db.DBController;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.schedule.AtTime;
import net.ion.framework.schedule.Job;
import net.ion.framework.schedule.ScheduledRunnable;
import net.ion.framework.schedule.Scheduler;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.Script;
import net.ion.niss.webapp.dscripts.ScriptDBManger;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.SearchManager;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.niss.webapp.scripters.ScriptWeb;
import net.ion.niss.webapp.sites.SiteManager;

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

	private ScriptDBManger sdbm;
	private DBController scriptDc;

	public REntry(Craken r, String wsName, NSConfig nsconfig) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		this.r = r;
		this.wsName = wsName;
		this.nsconfig = nsconfig;

		this.rsession = login();
		
		ReadSession dsession = login("datas") ;
		this.sdbm = ScriptDBManger.create(dsession) ;
		this.scriptDc = new DBController("craken", sdbm);
		this.scriptDc.initSelf(); 
		
		initCDDListener(rsession);
	}

	// use test only (central not finished)
	public void reload() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, CorruptIndexException, IOException {
		indexManager = new IndexManager();
		searchManager = new SearchManager();
		siteManager = new SiteManager(indexManager) ;
		initCDDListener(rsession);
	}

	private void initCDDListener(final ReadSession session) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, CorruptIndexException, IOException {

		final JScriptEngine jsengine = JScriptEngine.create();

		// load index
		// session.ghostBy("/indexers").children().debugPrint();

		for(ReadNode indexNode :session.pathBy("/indexers").children().stream()) {
				IdString cid = IdString.create(indexNode.fqn().name());

				SearchController central = createIndexerCentral(cid);

				Analyzer indexAnal = makeAnalyzer(new RNodePropertyReadable(indexNode), indexNode.property(Def.Indexer.IndexAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()));
				Analyzer queryAnal = makeQueryAnalyzer(new RNodePropertyReadable(indexNode), indexNode.property(Def.Indexer.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName()));

				central.defaultIndexConfig().analyzer(indexAnal);
				central.defaultSearchConfig().queryAnalyzer(queryAnal);

				ReadChildren schemas = session.pathBy(indexNode.fqn().toString() + "/schema").children();
				for (ReadNode schemaNode : schemas) {
					if (StringUtil.equals(Def.SchemaType.MANUAL, schemaNode.property(Def.IndexSchema.SchemaType).asString())) {
						central.defaultIndexConfig().fieldAnalyzer(schemaNode.fqn().name(), makeAnalyzer(schemaNode.property(Def.IndexSchema.Analyzer).asString()));
					}
				}

				indexManager.newIndex(cid, central);
				log.info(cid + " indexer loaded");
		};
		
		// load searcher
		for(ReadNode sec : session.pathBy("/searchers").children().stream()){
			IdString sid = registerSearcher(session, new RNodePropertyReadable(sec), jsengine);
			log.info(sid + " searcher loaded");
		};

		session.workspace().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/searchers/{sid}";
			}
			public String id() {
				return "niss.searchers";
			}

			public WriteJobNoReturn modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
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

			public WriteJobNoReturn deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				IdString sid = IdString.create(rmap.get("sid"));
				searchManager.removeSearcher(sid);
				return null;
			}
		});

		session.workspace().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/indexers/{iid}/schema/{schemaid}";
			}
			public String id() {
				return "niss.indexers.schema";
			}

			@Override
			public WriteJobNoReturn deleted(Map<String, String> rmap, CDDRemovedEvent event) {
				IdString iid = IdString.create(rmap.get("iid"));
				String schemaid = rmap.get("schemaid");
				if (!indexManager.hasIndex(iid))
					return null;

				SearchController saved = indexManager.index(iid);
				saved.defaultIndexConfig().removeFieldAnalyzer(schemaid);

				return null;
			}

			@Override
			public WriteJobNoReturn modified(Map<String, String> rmap, CDDModifiedEvent event) {
				IdString iid = IdString.create(rmap.get("iid"));
				String schemaid = rmap.get("schemaid");

				if (!indexManager.hasIndex(iid))
					return null;

				if (Def.SchemaType.MANUAL.equals(event.newProperty(Def.IndexSchema.SchemaType).asString()) && StringUtil.isNotBlank(event.newProperty(Def.IndexSchema.Analyzer).asString())) {
					try {
						SearchController saved = indexManager.index(iid);
						Analyzer analClz = makeAnalyzer(event.newProperty(Def.IndexSchema.Analyzer).asString());
						saved.defaultIndexConfig().fieldAnalyzer(schemaid, analClz);

					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
				;

				return null;
			}
		});

		session.workspace().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/searchers/{sid}/schema/{schemaid}";
			}
			public String id() {
				return "niss.searchers.schema";
			}

			public WriteJobNoReturn deleted(Map<String, String> rmap, CDDRemovedEvent event) {
				IdString sid = IdString.create(rmap.get("sid"));
				String schemaid = rmap.get("schemaid");
				if (!searchManager.hasSearch(sid))
					return null;

				Searcher saved = searchManager.searcher(sid);
				saved.sconfig().removeFieldAnalyzer(schemaid);
				return null;
			}

			public WriteJobNoReturn modified(Map<String, String> rmap, CDDModifiedEvent event) {
				IdString sid = IdString.create(rmap.get("sid"));
				String schemaid = rmap.get("schemaid");

				if (!searchManager.hasSearch(sid))
					return null;
				try {
					Searcher saved = searchManager.searcher(sid);
					Analyzer analClz = makeAnalyzer(event.newProperty(Def.IndexSchema.Analyzer).asString());
					saved.sconfig().fieldAnalyzer(schemaid, analClz);

				} catch (Exception e) {
					throw new IllegalStateException(e);
				}

				return null;
			}
		});

		// add index listener
		session.workspace().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/indexers/{iid}";
			}

			public String id() {
				return "niss.indexers";
			}

			public WriteJobNoReturn modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				IdString iid = IdString.create(rmap.get("iid"));
				try {
					if (!indexManager.hasIndex(iid)) { // created
						SearchController central = createIndexerCentral(iid);

						indexManager.newIndex(iid, central);
						log.info(iid + " indexer defined");
					} else if (indexManager.hasIndex(iid)) {

						SearchController saved = indexManager.index(iid);

						String indexAnalClzName = StringUtil.defaultIfEmpty(cevent.newProperty(Def.Indexer.IndexAnalyzer).asString(), saved.defaultIndexConfig().analyzer().getClass().getCanonicalName());
						saved.defaultIndexConfig().analyzer(makeAnalyzer(new EventPropertyReadable(cevent), indexAnalClzName));

						String queryAnalClzName = StringUtil.defaultIfEmpty(cevent.newProperty(Def.Indexer.QueryAnalyzer).asString(), saved.defaultSearchConfig().queryAnalyzer().getClass().getCanonicalName());
						saved.defaultSearchConfig().queryAnalyzer(makeQueryAnalyzer(new EventPropertyReadable(cevent), queryAnalClzName));

						log.info(iid + " indexer defined");
					} else {
						throw new IllegalArgumentException("not have index " + iid.idString());
					}

				} catch (ClassNotFoundException | IllegalArgumentException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
					throw new IllegalStateException(ex);
				}

				return null;
			}

			@Override
			public WriteJobNoReturn deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				IdString iid = IdString.create(rmap.get("iid"));

				indexManager.removeIndex(iid);

				return null;
			}
		});
		
		
		session.workspace().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/dscripts/{did}";
			}

			public String id() {
				return "niss.datascript";
			}

			public WriteJobNoReturn modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				IdString did = IdString.create(rmap.get("did"));
				sdbm.loadPackage(did.idString(), cevent.newProperty("content").asString());
				log.info(did + " package defined");

				return null;
			}

			@Override
			public WriteJobNoReturn deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				IdString did = IdString.create(rmap.get("did"));
				sdbm.removePackage(did.idString()) ;
				return null;
			}
		});

		session.workspace().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/scripts/{sid}/schedule";
			}

			public String id() {
				return "niss.scripts";
			}

			public WriteJobNoReturn deleted(Map<String, String> rmap, CDDRemovedEvent cevent) {
				String sid = rmap.get("sid");
				scheduler.removeJob(sid);

				return null;
			}

			public WriteJobNoReturn modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				EventPropertyReadable rnode = new EventPropertyReadable(cevent);
				String jobId = rmap.get("sid");
				if (rnode.property(Def.Schedule.ENABLE).asBoolean()) {
					ReadNode sinfo = rsession.pathBy("/scripts/" + jobId + "/schedule");
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
						final ReadNode scriptNode = rsession.pathBy("/scripts/" + scriptId);

						// should check running(in distribute mode)
						if (scriptNode.property(Script.Running).asBoolean())
							return;
						rsession.tran(wsession -> {
							wsession.pathBy(scriptNode.fqn()).property(Script.Running, true).merge();
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
										rsession.tran(wsession -> {
											wsession.pathBy(scriptNode.fqn()).property(Script.Running, false).merge();
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
										rsession.tran(wsession -> {
											wsession.pathBy(scriptNode.fqn()).property(Script.Running, false).merge();
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
		session.tran(wsession -> {
			WriteStream scripts = wsession.pathBy("/scripts").children().stream() ;
			for (WriteNode wnode : scripts){
				wnode.property(Script.Running, false);
				if (wnode.hasChild("schedule")) {
					WriteNode scheduleNode = wnode.child("schedule");
					if (scheduleNode.property(Def.Schedule.ENABLE).asBoolean()) {
						scheduleNode.property(Def.Schedule.ENABLE, true).merge();
					}
				}
			}
		});

	}

	// create analyzer
	private Analyzer makeAnalyzer(PropertyReadable rnode, String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			if (StringUtil.isBlank(modValue))
				return new StandardAnalyzer();

			Class<Analyzer> indexAnalClz = (Class<Analyzer>) Class.forName(modValue);

			Analyzer resultAnalyzer = null;
			Constructor con = null;
			if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[] { CharArraySet.class })) != null) {

				boolean useStopword = rnode.property(Def.Indexer.ApplyStopword).asBoolean();
				Collection<String> stopWord = ListUtil.EMPTY;
				if (useStopword) {
					String stopwords = rnode.property(Def.Indexer.StopWord).asString();
					stopWord = ListUtil.toList(StringUtil.split(stopwords, " ,\n"));
				}

				resultAnalyzer = (Analyzer) con.newInstance(new CharArraySet(stopWord, false));
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
		if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[] { CharArraySet.class })) != null) {
			Collection<String> stopWord = ListUtil.EMPTY;
			indexAnal = (Analyzer) con.newInstance(new CharArraySet(stopWord, false));
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
		if ((con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[] { CharArraySet.class })) != null) {
			queryAnal = (Analyzer) con.newInstance(new CharArraySet(ListUtil.EMPTY, false));
		} else {
			con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[0]);
			queryAnal = (Analyzer) con.newInstance();
		}
		return queryAnal;
	}

	private IdString registerSearcher(ReadSession session, PropertyReadable rnode, JScriptEngine jsengine) throws CorruptIndexException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		String[] cols = rnode.property(Def.Searcher.Target).asSet().stream().map(s -> s.toString()).collect(Collectors.toSet()).toArray(new String[0]);
		IdString sid = IdString.create(rnode.fqn().name());

		Searcher searcher = null;
		if (cols.length == 0) {
			return sid ; // blank..
		} else if(cols.length == 1) {
			SearchController sdc = indexManager.index(cols[0]) ;
			searcher = sdc.newSearcher() ;
		} else {
			SearchController sdc = indexManager.index(cols[0]) ;
			String[] appendCols = ArrayUtil.newSubArray(cols, 1, cols.length) ;
			List<SearchController> target = ListUtil.newList();
			for (String colId : cols) {
				if (indexManager.hasIndex(colId)) {
					target.add(indexManager.index(colId));
				}
			}
			searcher = sdc.newSearcher(target.toArray(new SearchController[0])) ;
		}
		
		Analyzer queryAnalyzer = makeAnalyzer(rnode, rnode.property(Def.Searcher.QueryAnalyzer).asString());
		searcher.sconfig().queryAnalyzer(queryAnalyzer) ;

		ReadChildren schemas = session.pathBy(rnode.fqn().toString() + "/schema").children();
		for (ReadNode schemaNode : schemas.stream()) {
			searcher.sconfig().fieldAnalyzer(schemaNode.fqn().name(), makeAnalyzer(schemaNode.property(Def.IndexSchema.Analyzer).asString()));
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

	private SearchController createIndexerCentral(IdString iid) {

//		String name = iid.idString();
//		DefaultCacheManager dm = r.dm();
//		String path = nsconfig.repoConfig().indexHomeDir() + "/" + name;
//		Configuration meta_config = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).persistence().passivation(false).addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path).async().enable()
//				.flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3).build();
//		dm.defineConfiguration(name + "-meta", meta_config);
//
//		Configuration chunk_config = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).persistence().passivation(false).addSingleFileStore().fetchPersistentState(true).preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).location(path).async().enable()
//				.flushLockTimeout(300000).shutdownTimeout(2000).modificationQueueSize(10).threadPoolSize(3).build();
//		dm.defineConfiguration(name + "-chunk", chunk_config);
//
//		Cache<?, ?> metaCache = dm.getCache(name + "-meta");
//		Cache<?, ?> chunkCache = dm.getCache(name + "-chunk");
//
//		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, chunkCache, metaCache, name);
//		bcontext.chunkSize(16384 * 8);
//		Directory directory = bcontext.create();
//		Central central = CentralConfig.oldFromDir(directory).build();
//
//		log.info(name + " Index ClusterMode : " + metaCache.getCacheConfiguration().clustering().cacheMode());
//
//		return central;
		try {
			return SearchControllerConfig.newLocalFile(new File(nsconfig.repoConfig().indexHomeDir(), iid.idString()).getCanonicalPath()).build();
		} catch (IOException e) {
			throw new IllegalStateException(e) ;
		}
	}

	// test
	public final static REntry create() throws CorruptIndexException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		NSConfig nsconfig = ConfigBuilder.createDefault(9000).build();
		return create(nsconfig);
	}

	public final static REntry create(NSConfig nsconfig) throws CorruptIndexException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		return nsconfig.createREntry();
	}

	public final static REntry test() throws CorruptIndexException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SQLException {
		NSConfig nsconfig = ConfigBuilder.createDefault(9000).build();
		return nsconfig.testREntry();
	}

	public ReadSession login() throws IOException {
		return r.login(wsName);
	}

	public ReadSession login(String otherWsname) throws IOException {
		return r.login(otherWsname);
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
	
	public DBController scriptDBController() {
		return scriptDc ;
	}
}

interface PropertyReadable {
	public Property property(String propId);

	public Fqn fqn();
}

class EventPropertyReadable implements PropertyReadable {

	private CDDModifiedEvent event;

	public EventPropertyReadable(CDDModifiedEvent event) {
		this.event = event;
	}

	@Override
	public Property property(String propId) {
		return event.newProperty(propId);
	}


	public Fqn fqn() {
		return event.getKey() ;
	}
}

class RNodePropertyReadable implements PropertyReadable {

	private ReadNode rnode;

	public RNodePropertyReadable(ReadNode node) {
		this.rnode = node;
	}

	@Override
	public Property property(String propId) {
		return rnode.property(propId);
	}


	public Fqn fqn() {
		return rnode.fqn();
	}
}
