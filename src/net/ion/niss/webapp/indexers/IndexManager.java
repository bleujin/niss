package net.ion.niss.webapp.indexers;

import java.util.Map;
import java.util.Set;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.IdString;
import net.ion.nsearcher.config.Central;

public class IndexManager {

	private Map<IdString, Central> indexes = MapUtil.newMap() ;
	
	public IndexManager newIndex(IdString iid, Central central) {
		indexes.put(iid, central) ;
		return this ;
	}
	
	
	public Set<IdString> keys(){
		return indexes.keySet() ;
	}
	
	public boolean hasIndex(IdString iid){
		return indexes.containsKey(iid) ;
	}
	
	public boolean hasIndex(String iid) {
		return hasIndex(IdString.create(iid));
	}
	
	public Central index(IdString iid){
		return indexes.get(iid) ;
	}	
	
	public Central index(String iid){
		return index(IdString.create(iid)) ;
	}

	public void removeIndex(IdString iid) {
		Central removed = indexes.remove(iid) ;
		IOUtil.close(removed); 
		
		// TODO : file remove ?
	}

	
	

}
