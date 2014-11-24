package net.ion.niss.webapp.indexers;

import java.util.Map;

import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.IdString;
import net.ion.nsearcher.search.Searcher;

public class SearchManager {

	private Map<IdString, Searcher> searchers = MapUtil.newMap() ;

	public void newSearch(IdString sid, Searcher searcher) {
		searchers.put(sid, searcher) ;
	}
	
	public boolean hasSearch(String sid){
		return hasSearch(IdString.create(sid)) ;
	}

	public boolean hasSearch(IdString sid) {
		return searchers.containsKey(sid);
	}
	
	public Searcher searcher(String sid){
		Searcher result = searcher(IdString.create(sid));
		if (result == null) throw new IllegalArgumentException("not found searcher : " + sid) ;
		return result ;
	}

	public Searcher searcher(IdString sid) {
		Searcher result = searchers.get(sid);
		if (result == null) throw new IllegalArgumentException("not found searcher : " + sid) ;
		return result;
	}

	public void removeSearcher(IdString sid) {
		Searcher removed = searchers.remove(sid) ;
		
	}
	
}
