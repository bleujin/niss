package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.util.Version;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.util.concurrent.WithinThreadExecutor;

import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.niss.webapp.indexers.IndexManager;
import net.ion.niss.webapp.indexers.SearchManager;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.config.SearchConfig;
import net.ion.nsearcher.search.CompositeSearcher;
import net.ion.nsearcher.search.Searcher;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

public class REntry implements Closeable {

	public final static String EntryName = "rentry";

	private RepositoryImpl r;
	private String wsName;
	private IndexManager indexManager = new IndexManager();

	private SearchManager searchManager = new SearchManager();

	public REntry(RepositoryImpl r, String wsName) throws IOException {
		this.r = r;
		this.wsName = wsName;

		initCDDListener(login());
	}

	private void initCDDListener(final ReadSession session) {

		final JScriptEngine jsengine = JScriptEngine.create();

		// load index
		session.ghostBy("/indexers").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {
				try {
					for (ReadNode col : iter) {
						IdString cid = IdString.create(col.fqn().name());

						Class<?> indexAnalClz = Class.forName(col.property("indexanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName()));
						Analyzer indexAnal = (Analyzer) indexAnalClz.getConstructor(Version.class).newInstance(SearchConstant.LuceneVersion);

						Class<?> queryAnalClz = Class.forName(col.property("queryanalyzer").defaultValue(MyKoreanAnalyzer.class.getCanonicalName()));
						Analyzer queryAnal = (Analyzer) queryAnalClz.getConstructor(Version.class).newInstance(SearchConstant.LuceneVersion);

						Central central = CentralConfig.newLocalFile().dirFile("./resource/index/" + cid.idString()).indexConfigBuilder().indexAnalyzer(indexAnal).parent().searchConfigBuilder().queryAnalyzer(queryAnal).build();

						indexManager.newIndex(cid, central);
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
						IdString sid = IdString.create(sec.fqn().name());
						Searcher searcher = null;
						Set<String> cols = sec.property("target").asSet();
						if (cols.size() == 0) {
							searcher = CompositeSearcher.createBlank();
						} else {
							List<Central> target = ListUtil.newList();
							for (String colId : cols) {
								if (indexManager.hasIndex(colId)) {
									target.add(indexManager.index(colId));
								}
							}
							SearchConfig nconfig = SearchConfig.create(new WithinThreadExecutor(), SearchConstant.LuceneVersion, new StandardAnalyzer(SearchConstant.LuceneVersion), SearchConstant.ISALL_FIELD);
							searcher = CompositeSearcher.create(nconfig, target);
						}

						if (sec.property("applyhandler").asBoolean()) {
							StringReader scontent = new StringReader(sec.property("handler").asString());
							try {
								InstantJavaScript script = jsengine.createScript(IdString.create("handler"), "", scontent);
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
								}, searcher, session);
								searchManager.newSearch(sid, fsearcher);
							} catch (Exception e) { // otherwise system cant started
								e.printStackTrace(); 
								searchManager.newSearch(sid, searcher);
							}
						} else {
							searchManager.newSearch(sid, searcher);
						}
					}
				} catch (IOException ex) {
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
					TreeNodeKey key = cevent.getKey();
					IdString sid = IdString.create(key.getFqn().name());
					Searcher searcher = null;
					Set<String> cols = cevent.property("target").asSet();
					if (cols.size() == 0) {
						searcher = CompositeSearcher.createBlank();
					} else {
						List<Central> target = ListUtil.newList();
						for (String colId : cols) {
							if (indexManager.hasIndex(colId)) {
								target.add(indexManager.index(colId));
							}
						}
						SearchConfig nconfig = SearchConfig.create(new WithinThreadExecutor(), SearchConstant.LuceneVersion, new StandardAnalyzer(SearchConstant.LuceneVersion), SearchConstant.ISALL_FIELD);
						searcher = CompositeSearcher.create(nconfig, target);
					}

					if (cevent.property("applyhandler").asBoolean()) {
						StringReader scontent = new StringReader(cevent.property("handler").asString());
						InstantJavaScript script = jsengine.createScript(IdString.create("handler"), "", scontent);
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
						}, searcher, session);
						searchManager.newSearch(sid, fsearcher);
					} else {
						searchManager.newSearch(sid, searcher);
					}

				} catch (IOException ex) {
					throw new IllegalStateException(ex);
				} catch (ScriptException ex) {
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
					if (cevent.getValue().containsKey(PropertyId.normal("created"))) { // created
						Central central = CentralConfig.newLocalFile().dirFile("./resource/index/" + iid.idString()).build();

						indexManager.newIndex(iid, central);
					} else if (indexManager.hasIndex(iid)) {
						Central saved = indexManager.index(iid);
						for (PropertyId key : cevent.getValue().keySet()) {
							String modValue = cevent.getValue().get(key).asString();
							if ("indexanalyzer".equals(key.idString()) && (!cevent.getValue().get(key).asString().equals(saved.indexConfig().indexAnalyzer().getClass().getCanonicalName()))) {
								Class<?> indexAnalClz = Class.forName(modValue);
								Analyzer indexAnal = (Analyzer) indexAnalClz.getConstructor(Version.class).newInstance(SearchConstant.LuceneVersion);
								saved.indexConfig().indexAnalyzer(indexAnal);
							} else if ("queryanalyzer".equals(key.idString())) {
								Class<?> queryAnalClz = Class.forName(modValue);
								Analyzer queryAnal = (Analyzer) queryAnalClz.getConstructor(Version.class).newInstance(SearchConstant.LuceneVersion);
								saved.searchConfig().queryAnalyzer(queryAnal);
							} else if ("applystopword".equals(key.idString())) {

							}
						}

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
				} catch (NoSuchMethodException ex) {
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
	}

	public final static REntry create() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location("./resource/admin"));
		r.start();

		// RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		return new REntry(r, "admin");
	}
	
	public final static REntry test() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create().location(""));
		r.start();

		// RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		return new REntry(r, "test");
	}

	public ReadSession login() throws IOException {
		return r.login(wsName);
	}

	public RepositoryImpl repository() {
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

}
