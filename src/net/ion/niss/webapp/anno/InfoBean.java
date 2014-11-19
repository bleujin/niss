package net.ion.niss.webapp.anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.Path;

import net.ion.framework.util.ListUtil;
import net.ion.niss.webapp.Webapp;

public class InfoBean {

	private String prefixPath;
	private List<MethodInfo> methods = ListUtil.newList() ;
	private String clsName;

	public String prefixPath() {
		return prefixPath;
	}

	public void classPath(Class<? extends Webapp> clz, Path clzAnno) {
		this.clsName = clz.getSimpleName() ;
		this.prefixPath = clzAnno.value() ;
	}

	public String clsName(){
		return clsName ;
	}
	
	public void methodPath(Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
		 methods.add(MethodInfo.create(this, m, path, annos, paramAnnos)) ;
	}

	public List<MethodInfo> methods() {
		return methods ;
	}

}
