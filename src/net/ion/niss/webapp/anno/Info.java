package net.ion.niss.webapp.anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.Path;

import net.ion.niss.webapp.Webapp;

public interface Info<T> {

	
	public final static Info<InfoBean> DEFAULT = new Info<InfoBean>(){

		private InfoBean bean = new InfoBean() ;
		@Override
		public void classPath(Class<? extends Webapp> clz, Path clzAnno) {
			bean.classPath(clz, clzAnno) ;
		}

		@Override
		public void methodPath(Method m, Path path, Annotation[] annos, Annotation[][] paramAnnos) {
			bean.methodPath(m, path, annos, paramAnnos) ;
		}
		
		@Override
		public InfoBean build() {
			InfoBean result = bean ;
			bean = new InfoBean() ;
			return result ;
		}
		
	} ;
	
	
	public void classPath(Class<? extends Webapp> clz, Path clzAnno) ;

	public void methodPath(Method m, Path path, Annotation[] declaredAnnotations, Annotation[][] annotations);

	public T build() ;
	
}
