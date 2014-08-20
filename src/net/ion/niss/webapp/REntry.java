package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
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
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.niss.apps.IdString;
import net.ion.niss.apps.collection.IndexManager;
import net.ion.niss.apps.collection.IndexManager;
import net.ion.niss.apps.collection.SearchManager;
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

		// load index
		session.ghostBy("/collections").children().eachNode(new ReadChildrenEach<Void>() {
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
		session.ghostBy("/sections").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {

				try {
					for (ReadNode sec : iter) {
						IdString sid = IdString.create(sec.fqn().name());
						Searcher searcher = null;
						Set<String> cols = sec.property("collection").asSet();
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
						searchManager.newSearch(sid, searcher);
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
				return "/sections/{sid}";
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				try {
					TreeNodeKey key = cevent.getKey();
					IdString sid = IdString.create(key.getFqn().name());
					Searcher searcher = null;
					Set<String> cols = cevent.getValue().get(PropertyId.normal("collection")).asSet();
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
					searchManager.newSearch(sid, searcher);
				} catch (IOException ex) {
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
				return "/collections/{cid}";
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent cevent) {
				IdString cid = IdString.create(rmap.get("cid"));
				try {
					if (cevent.getValue().containsKey(PropertyId.normal("created"))) { // created
						Central central = CentralConfig.newLocalFile().dirFile("./resource/index/" + cid.idString()).build();

						indexManager.newIndex(cid, central);
					} else if (indexManager.hasIndex(cid)) {
						Central saved = indexManager.index(cid);
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
						throw new IllegalArgumentException("not have index " + cid.idString());
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
				IdString cid = IdString.create(rmap.get("cid"));

				indexManager.removeIndex(cid);

				return null;
			}
		});
	}

	public final static REntry test() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location("./resource/admin"));
		r.start();

		// RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		return new REntry(r, "admin");
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
