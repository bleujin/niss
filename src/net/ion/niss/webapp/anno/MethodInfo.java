package net.ion.niss.webapp.anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.ListUtil;

public class MethodInfo {

	private InfoBean parent;
	private Method m;
	private Path path;
	private Annotation[] annos;
	private List<Annotation[]> paramAnnos;

	public MethodInfo(InfoBean parent, Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
		this.parent = parent ;
		this.m = m ;
		this.path = path ;
		this.annos = annos ;
		this.paramAnnos = ListUtil.toList(paramAnnos) ;
	}

	public static MethodInfo create(InfoBean parent, Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
		return new MethodInfo(parent, m, path, annos, paramAnnos) ;
	}

	public String path(){
		return parent.prefixPath() + path.value() ;
	}

	public boolean isGET() {
		return m.getAnnotation(GET.class) != null ; 
	}

	public boolean isPOST() {
		return m.getAnnotation(POST.class) != null ; 
	}

	public boolean isDELETE() {
		return m.getAnnotation(DELETE.class) != null ; 
	}

	public boolean isPUT() {
		return m.getAnnotation(PUT.class) != null ; 
	}

	public Produces produce() {
		return m.getAnnotation(Produces.class);
	}

	public List<Annotation[]> parameters() {
		
		return paramAnnos;
	}

	public Method method() {
		return m;
	}

	public StringBuilder description(String prefix) {
		MyStringWriter result = new MyStringWriter() ;
		result.appendLine("path : ", prefix + path()) ;
		result.appendLine("httpMethod : ", isGET() ? "GET " : "" , isPOST() ? "POST " : "" , isDELETE() ? "DELETE " : "" , isPUT() ? "PUT " : "" ) ;
		result.appendLine("javaMethod : ", method().toString()) ;
		for (Annotation[] param : parameters()) {
			result.appendLine("  ", ArrayUtil.toString(param)) ;
		}
		
		
		return result.stringBuilder() ;
	}
	
}

