package net.ion.niss.webapp.util;

public class WebUtil {

	public final static boolean isStaticResource(String uri){
		if (uri == null) return false ;
		
		return uri.startsWith("/css/") || uri.startsWith("/img/") || uri.startsWith("/favicon.ico") || uri.startsWith("/fonts/") || uri.startsWith("/js/") ;
	}
}
