package net.ion.niss.webapp.anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.Path;

import net.ion.framework.util.ListUtil;

public class InfoBean {

	private String prefixPath;
	private List<MethodInfo> methods = ListUtil.newList() ;

	public String prefixPath() {
		return prefixPath;
	}

	public void classPath(Path clzAnno) {
		this.prefixPath = clzAnno.value() ;
	}

	public void methodPath(Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
		 methods.add(MethodInfo.create(this, m, path, annos, paramAnnos)) ;
	}

	public List<MethodInfo> methods() {
		return methods ;
	}

}
