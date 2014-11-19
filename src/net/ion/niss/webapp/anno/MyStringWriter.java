package net.ion.niss.webapp.anno;

import org.apache.commons.lang.SystemUtils;

public class MyStringWriter {

	private StringBuilder inner = new StringBuilder() ;
	
	public MyStringWriter append(CharSequence... vals){
		for (CharSequence val : vals) {
			inner.append(val) ;
		}
		return this ;
	}
	
	public MyStringWriter appendLine(CharSequence... vals){
		for (CharSequence val : vals) {
			inner.append(val) ;
		}
		inner.append(SystemUtils.LINE_SEPARATOR) ;
		
		return this ;
	}
	
	public StringBuilder stringBuilder() {
		return inner ;
	}
}
