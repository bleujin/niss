package net.ion.niss.webapp.indexers;

import java.util.Map;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.IdString;
import net.ion.nsearcher.config.Central;

public class IndexManager {

	private Map<IdString, Central> indexes = MapUtil.newMap() ;
	
	public IndexManager newIndex(IdString cid, Central central) {
		indexes.put(cid, central) ;
		return this ;
	}
	
	public boolean hasIndex(IdString cid){
		return indexes.containsKey(cid) ;
	}
	
	public boolean hasIndex(String cid) {
		return hasIndex(IdString.create(cid));
	}
	
	public Central index(IdString cid){
		return indexes.get(cid) ;
	}	
	
	public Central index(String cid){
		return index(IdString.create(cid)) ;
	}

	public void removeIndex(IdString cid) {
		Central removed = indexes.remove(cid) ;
		IOUtil.close(removed); 
		
		// TODO : file remove ?
	}

	
	

}
