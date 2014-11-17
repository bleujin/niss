package net.ion.niss.webapp;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.Path;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringBuilderWriter;
import net.ion.niss.webapp.anno.InfoFac;
import net.ion.niss.webapp.anno.InfoBean;
import net.ion.niss.webapp.anno.Info;
import net.ion.niss.webapp.anno.MethodInfo;
import net.ion.niss.webapp.searchers.SearcherWeb;

public class TestWebAppInfo extends TestCase {

	public void testClass() throws Exception {
		Class<SearcherWeb> clz = SearcherWeb.class;
		
		InfoFac<SearcherWeb> winfo = InfoFac.create(clz) ;
		
		InfoBean bean = winfo.visit(Info.DEFAULT) ;
		
		assertEquals("/searchers", bean.prefixPath());
		
		List<MethodInfo> methods = bean.methods() ;
		for (MethodInfo mi : methods) {
			// Debug.line(mi.path(), mi.httpMethods(), mi.produce(), mi.parameters());
		}
		
	}
}


