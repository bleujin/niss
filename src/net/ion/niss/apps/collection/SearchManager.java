package net.ion.niss.apps.collection;

import java.util.Map;

import net.ion.framework.util.MapUtil;
import net.ion.niss.apps.IdString;
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
		return searcher(IdString.create(sid)) ;
	}

	public Searcher searcher(IdString sid) {
		return searchers.get(sid);
	}

	public void removeSearcher(IdString sid) {
		Searcher removed = searchers.remove(sid) ;
		
	}
	
}
