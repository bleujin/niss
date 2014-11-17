package net.ion.niss.webapp.anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.Path;

public class MethodInfo {

	private InfoBean parent;
	private Method m;
	private Path path;
	private Annotation[] annos;
	private Annotation[][] paramAnnos;

	public MethodInfo(InfoBean parent, Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
		this.parent = parent ;
		this.m = m ;
		this.path = path ;
		this.annos = annos ;
		this.paramAnnos = paramAnnos ;
	}

	public static MethodInfo create(InfoBean parent, Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
		return new MethodInfo(parent, m, path, annos, paramAnnos) ;
	}

	public String path(){
		return parent.prefixPath() + path.value() ;
	}
	
}

