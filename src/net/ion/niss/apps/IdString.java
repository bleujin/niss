package net.ion.niss.apps;

import net.ion.framework.util.StringUtil;

public class IdString {

	private String id;
	public IdString(String id) {
		this.id = id ;
	}

	public final static IdString create(String id){
		if (StringUtil.isSmallAlphaNumUnderBar(id)){
			return new IdString(id) ;
		} throw new IllegalArgumentException("not id type :" + id) ;
	}
	
	@Override
	public int hashCode(){
		return id.hashCode() ;
	}
	
	@Override 
	public boolean equals(Object cid){
		if (cid instanceof IdString){
			IdString that = (IdString) cid ;
			return this.id.equals(that.id) ;
		}
		return false ;
	}

	public String idString() {
		return id;
	}
	
	
}
