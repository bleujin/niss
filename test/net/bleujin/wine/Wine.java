package net.bleujin.wine;

import java.util.Map;

import org.apache.commons.lang.CharUtils;

import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;

public class Wine {

	public static final Wine NOTFOUND = new Wine() ;
	
	
	private Map<String, String> vals = MapUtil.newCaseInsensitiveMap();
	
	public Wine put(String key, String value) {
		vals.put(key, value) ;
		return this ;
	}
	
	public String asString(String key) {
		return vals.get(key) ;
	}
	
	public int asInt(String field) {
		return asInt(field, -1) ;
	}

	public int asInt(String field, int dft) {
		if ("Sweet".equalsIgnoreCase(field)) return sweet();
		if ("Acidity".equalsIgnoreCase(field)) return acid();
		if ("Body".equalsIgnoreCase(field)) return body();
		if ("Tannin".equalsIgnoreCase(field)) return tannin();
		
		
		return dft ;
	}

	
	public final static boolean numericField(String field) {
		return StringUtil.isIncludeIgnoreCase(new String[] {"Sweet", "Acidity", "Body", "Tannin"}, field) ;
	}
	
	// "thumb_url","name_ko","name_en","nation","badge","price","생산자","생산지역","주품종","주 종","용 도","Alcole","DTemp","Incense","Sweet","Acidity","Body","Tannin","Cook","CPrice","Importer","maker_note"
	public String name() {
		return asString("name_ko") ;
	}
	
	
	public int sweet() {
		return NumberUtil.toInt(StringUtil.substringAfter(asString("Sweet"), "SWEET"), -1) ;
	}

	public int acid() {
		return NumberUtil.toInt(StringUtil.substringAfter(asString("Acidity"), "ACIDITY"), -1) ;
	}
	

	public int body() {
		return NumberUtil.toInt(StringUtil.substringAfter(asString("Body"), "BODY"), -1) ;
	}
	

	public int tannin() {
		return NumberUtil.toInt(StringUtil.substringAfter(asString("Tannin"), "TANNIN"), -1) ;
	}
	
	
	public int compareValue(Wine other, String field) {
		int lnum = asInt(field, -1) ;
		int rnum = other.asInt(field, -1) ;
		if (lnum < 0 || rnum < 0 ) return 0 ;
		return Math.abs(lnum - rnum) ;
	}
	
	
	public String toString() {
		return asString("name_ko") ;
	}
}
