package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
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
import net.ion.craken.node.crud.ReadChildren;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.common.Def;
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

	private ReadSession rsession;

	public REntry(RepositoryImpl r, String wsName) throws IOException {
		this.r = r;
		this.wsName = wsName;

		this.rsession = login();
		initCDDListener(rsession);
	}


	// use test only (central not finished)
	public void reload(){
		indexManager = new IndexManager() ;
		searchManager = new SearchManager() ;
		initCDDListener(rsession);
	}
	
	private void initCDDListener(final ReadSession session) {

		final JScriptEngine jsengine = JScriptEngine.create();

		// load index
		session.ghostBy("/indexers").children().eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator iter) {
				try {
					for (ReadNode indexNode : iter) {
						IdString cid = IdString.create(indexNode.fqn().name());

						Central central = CentralConfig.newLocalFile().dirFile("./resource/index/" + cid.idString()).build();

						Analyzer indexAnal = makeIndexAnalyzer(new RNodePropertyReadable(indexNode), indexNode.property(Def.Indexer.IndexAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName())) ;
						Analyzer queryAnal = makeQueryAnalyzer(new RNodePropertyReadable(indexNode), indexNode.property(Def.Indexer.QueryAnalyzer).defaultValue(StandardAnalyzer.class.getCanonicalName())) ;

						central.indexConfig().indexAnalyzer(indexAnal) ;
						central.searchConfig().queryAnalyzer(queryAnal) ;
						
						ReadChildren schemas = session.ghostBy(indexNode.fqn().toString() + "/schema").children() ;
						for(ReadNode schemaNode : schemas.toList()) {
							if (StringUtil.equals(Def.SchemaType.MANUAL, schemaNode.property(Def.Schema.SchemaType).asString())){
								central.indexConfig().fieldAnalyzer(schemaNode.fqn().name(), makeIndexAnalyzer(schemaNode.property(Def.Schema.Analyzer).asString())) ;
							}
						}

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
						registerSearcher(new RNodePropertyReadable(sec), jsengine) ;
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
					registerSearcher(new EventPropertyReadable(cevent), jsengine);
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
		
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/indexers/{iid}/schema/{schemaid}";
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> rmap, CDDRemovedEvent event) {
				IdString iid = IdString.create(rmap.get("iid"));
				String schemaid = rmap.get("schemaid") ;
				if (! indexManager.hasIndex(iid)) return null ;
				
				Central saved = indexManager.index(iid);
				saved.indexConfig().removeFieldAnalyzer(schemaid) ;
				return null;
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent event) {
				IdString iid = IdString.create(rmap.get("iid"));
				String schemaid = rmap.get("schemaid") ;

				if (! indexManager.hasIndex(iid)) return null ;
				event.getValue() ;
				if (Def.SchemaType.MANUAL.equals(event.property(Def.Schema.SchemaType).asString()) && StringUtil.isNotBlank(event.property(Def.Schema.Analyzer).asString())){
					try {
						Central saved = indexManager.index(iid);
						Analyzer analClz = makeIndexAnalyzer(event.property(Def.Schema.Analyzer).asString());
						saved.indexConfig().fieldAnalyzer(schemaid, analClz) ;
						
					} catch (Exception e) {
						throw new IllegalStateException(e) ;
					}
				};

				return null;
			}
		}) ;

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
					if (! indexManager.hasIndex(iid)) { // created
						Central central = CentralConfig.newLocalFile().dirFile("./resource/index/" + iid.idString()).build();

						indexManager.newIndex(iid, central);
					} else if (indexManager.hasIndex(iid)) {
						
						Central saved = indexManager.index(iid);

						String indexAnalClzName = StringUtil.defaultIfEmpty(cevent.property(Def.Indexer.IndexAnalyzer).asString(), saved.indexConfig().indexAnalyzer().getClass().getCanonicalName()) ; 
						saved.indexConfig().indexAnalyzer(makeIndexAnalyzer(new EventPropertyReadable(cevent), indexAnalClzName));

						String queryAnalClzName = StringUtil.defaultIfEmpty(cevent.property(Def.Indexer.QueryAnalyzer).asString(), saved.searchConfig().queryAnalyzer().getClass().getCanonicalName()) ; 
						saved.searchConfig().queryAnalyzer(makeQueryAnalyzer(new EventPropertyReadable(cevent), queryAnalClzName));

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
	}
	
	private Analyzer makeIndexAnalyzer(PropertyReadable rnode, String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		Class<Analyzer> indexAnalClz = (Class<Analyzer>) Class.forName(modValue);
		
		Analyzer indexAnal = null ;
		Constructor con = null ; 
		if ( (con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[]{Version.class, CharArraySet.class})) != null){
			
			boolean useStopword = rnode.property(Def.Indexer.ApplyStopword).asBoolean() ;
			Collection<String> stopWord = ListUtil.EMPTY ;
			if (useStopword){
				stopWord = rnode.property(Def.Indexer.StopWord).asSet() ;
			}
			
			indexAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion, new CharArraySet(SearchConstant.LuceneVersion, stopWord, false)) ;
		} else if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[]{Version.class})) != null){
			indexAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion) ;
		} else {
			con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[0]) ;
			indexAnal = (Analyzer) con.newInstance() ;
		}
		return indexAnal;
	}
	
	private Analyzer makeIndexAnalyzer(String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		
		Class<Analyzer> indexAnalClz = (Class<Analyzer>) Class.forName(modValue);
		
		Analyzer indexAnal = null ;
		Constructor con = null ; 
		if ( (con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[]{Version.class, CharArraySet.class})) != null){
			Collection<String> stopWord = ListUtil.EMPTY ;
			indexAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion, new CharArraySet(SearchConstant.LuceneVersion, stopWord, false)) ;
		} else if ((con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[]{Version.class})) != null){
			indexAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion) ;
		} else {
			con = ConstructorUtils.getAccessibleConstructor(indexAnalClz, new Class[0]) ;
			indexAnal = (Analyzer) con.newInstance() ;
		}
		return indexAnal;
	}
	
	
	private Analyzer makeQueryAnalyzer(PropertyReadable rnode, String modValue) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class<?> queryAnalClz = Class.forName(modValue);
		
		Analyzer queryAnal = null ;
		Constructor con = null ; 
		if ( (con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[]{Version.class, CharArraySet.class})) != null){
			queryAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion, new CharArraySet(SearchConstant.LuceneVersion, ListUtil.EMPTY, false)) ;
		} else if ((con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[]{Version.class})) != null){
			queryAnal = (Analyzer) con.newInstance(SearchConstant.LuceneVersion) ;
		} else {
			con = ConstructorUtils.getAccessibleConstructor(queryAnalClz, new Class[0]) ;
			queryAnal = (Analyzer) con.newInstance() ;
		}
		return queryAnal;
	}
	
	
	private void registerSearcher(PropertyReadable rnode, JScriptEngine jsengine) throws CorruptIndexException, IOException {
		Set<String> cols = rnode.property(Def.Searcher.Target).asSet();
		IdString sid = IdString.create(rnode.fqn().name());
		
		Searcher searcher = null ;
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

		if (rnode.property(Def.Searcher.ApplyHandler).asBoolean()) {
			StringReader scontent = new StringReader(rnode.property(Def.Searcher.Handler).asString());
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
				}, searcher, rsession);
				searchManager.newSearch(sid, fsearcher);
			} catch (Exception e) { // otherwise system cant started
				e.printStackTrace(); 
				searchManager.newSearch(sid, searcher);
			}
		} else {
			searchManager.newSearch(sid, searcher);
		}
	}
	
	public final static REntry create() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location("./resource/admin"));
		r.start();

		return new REntry(r, "admin");
	}
	
	public final static REntry test() throws CorruptIndexException, IOException {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create().location(""));
		r.start();

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

interface PropertyReadable {
	public PropertyValue property(String propId) ;
	public PropertyValue property(PropertyId propId) ;
	public Fqn fqn() ;
}

class EventPropertyReadable implements PropertyReadable {

	private CDDModifiedEvent event;
	public EventPropertyReadable(CDDModifiedEvent event) {
		this.event = event ;
	}
	
	@Override
	public PropertyValue property(String propId) {
		return event.property(propId);
	}

	@Override
	public PropertyValue property(PropertyId propId) {
		return event.property(propId);
	}
	
	public Fqn fqn(){
		return event.getKey().getFqn() ;
	}
}

class RNodePropertyReadable implements PropertyReadable {

	private ReadNode rnode;
	public RNodePropertyReadable(ReadNode node) {
		this.rnode = node ;
	}
	
	@Override
	public PropertyValue property(String propId) {
		return rnode.property(propId);
	}

	@Override
	public PropertyValue property(PropertyId propId) {
		return rnode.propertyId(propId);
	}
	
	public Fqn fqn(){
		return rnode.fqn() ;
	}
} 
