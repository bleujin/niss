package net.bleujin.wine;

import java.util.Map;

import net.ion.framework.util.MapUtil;

public class WeightContext {

	public static final WeightContext DEFAULT = new WeightContext();
	private Map<String, Integer> map = MapUtil.newCaseInsensitiveMap() ;
	
	
	public int weight(String field) {
		if (map.containsKey(field)) {
			return map.get(field) ;
		}
		else return 1 ;
	}


	public WeightContext put(String field, int i) {
		map.put(field, i) ;
		return this ;
	}
	
}
