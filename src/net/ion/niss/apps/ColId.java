package net.ion.niss.apps;

import net.ion.framework.util.StringUtil;

public class ColId {

	private String id;
	public ColId(String id) {
		this.id = id ;
	}

	public final static ColId create(String id){
		if (StringUtil.isSmallAlphaNumUnderBar(id)){
			return new ColId(id) ;
		} throw new IllegalArgumentException("not id type :" + id) ;
	}
	
	@Override
	public int hashCode(){
		return id.hashCode() ;
	}
	
	@Override 
	public boolean equals(Object cid){
		if (cid instanceof ColId){
			ColId that = (ColId) cid ;
			return this.id.equals(that.id) ;
		}
		return false ;
	}

	public String idString() {
		return id;
	}
	
}
