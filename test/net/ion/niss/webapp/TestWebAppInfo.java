package net.ion.niss.webapp;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.apache.commons.lang.SystemUtils;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.anno.Info;
import net.ion.niss.webapp.anno.InfoBean;
import net.ion.niss.webapp.anno.InfoFac;
import net.ion.niss.webapp.anno.MethodInfo;
import net.ion.niss.webapp.indexers.IndexerWeb;
import net.ion.niss.webapp.loaders.LoaderWeb;
import net.ion.niss.webapp.misc.AnalysisWeb;
import net.ion.niss.webapp.misc.CrakenLet;
import net.ion.niss.webapp.misc.MenuWeb;
import net.ion.niss.webapp.misc.MiscWeb;
import net.ion.niss.webapp.misc.OpenScriptWeb;
import net.ion.niss.webapp.misc.ScriptWeb;
import net.ion.niss.webapp.misc.TraceWeb;
import net.ion.niss.webapp.searchers.OpenSearchWeb;
import net.ion.niss.webapp.searchers.SearcherWeb;
import net.ion.niss.webapp.searchers.TemplateWeb;

public class TestWebAppInfo extends TestCase {

	public void testClass() throws Exception {
		List<Class<? extends Webapp>> apps = ListUtil
				.<Class<? extends Webapp>> toList(AnalysisWeb.class, CrakenLet.class, IndexerWeb.class, LoaderWeb.class, MenuWeb.class, MiscWeb.class, OpenScriptWeb.class, OpenSearchWeb.class, ScriptWeb.class, SearcherWeb.class, TemplateWeb.class, TraceWeb.class);

//		List<Class<? extends Webapp>> apps = ListUtil
//				.<Class<? extends Webapp>> toList(AnalysisWeb.class, IndexerWeb.class);

		
		File parentDir = new File("./resource/api") ;
		for(File apiFile : parentDir.listFiles()){
			apiFile.delete() ;
		}
//		FileUtil.deleteDirectory(parentDir);
//		
//		if (! parentDir.exists()){
//			parentDir.mkdirs() ;
//		}
		
		for (Class<? extends Webapp> appClass : apps) {
			InfoFac<? extends Webapp> winfo = InfoFac.create(appClass);
			InfoBean bean = winfo.visit(Info.DEFAULT);
			
			String prefix = bean.clsName().startsWith("Open") ? "open" : "admin" ;
			String path = prefix + StringUtil.replace(bean.prefixPath(), "/", "_") + ".api";
			  
			File file = new File(parentDir, path) ;
			Debug.line(file);
			FileWriter writer = new FileWriter(file);
			
			List<MethodInfo> methods = bean.methods();
			for (MethodInfo mi : methods) {
				writer.write(mi.description("/" + prefix).toString());
				writer.write(SystemUtils.LINE_SEPARATOR);
			}
			writer.close();
			// break ;
		}

	}
}
