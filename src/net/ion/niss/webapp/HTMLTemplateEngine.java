package net.ion.niss.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;

import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.script.StringInputStream;
import net.ion.framework.db.ThreadFactoryBuilder;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.jci.monitor.AbstractListener;
import net.ion.jci.monitor.FileAlterationMonitor;
import net.ion.niss.webapp.common.MyAuthenticationHandler;
import net.ion.niss.webapp.common.ToJsonHandler;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.handler.TemplateEngine;
import net.ion.radon.core.TreeContext;

public class HTMLTemplateEngine implements TemplateEngine {

	private VelocityContext vcontext;
	private Charset utf8;
	private VelocityEngine ve;
	private ReadSession rsession;
	private REntry rentry;
	private ToJsonHandler handler;

	public HTMLTemplateEngine(TreeContext tcontext) throws Exception {
		this.utf8 = Charset.forName("UTF-8");
		this.ve = new VelocityEngine();
		this.vcontext = new VelocityContext();
		vcontext.put(TreeContext.class.getCanonicalName(), tcontext);
		this.rentry = tcontext.getAttributeObject(REntry.EntryName, REntry.class);
		this.rsession = rentry.login();

		ve.setProperty("resource.loader", "file");
		ve.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		ve.setProperty("file.resource.loader.path", new File("./").getCanonicalPath() + "/webapps/admin");
		ve.setProperty("file.resource.loader.cache", "false");
		// ve.setProperty("file.resource.loader.modificationCheckInterval", "5");
		ve.setProperty("output.encoding", "UTF-8");
		ve.setProperty("input.encoding", "UTF-8");

		ve.init();

		XMLReader xreader = XMLReaderFactory.createXMLReader();
		String xmlString = IOUtil.toStringWithClose(new FileInputStream(Webapp.MESSAGE_RESOURCE_FILE));
		InputSource input = new InputSource(new StringInputStream(xmlString));

		this.handler = new ToJsonHandler();
		xreader.setContentHandler(handler);
		xreader.parse(input);

		final Logger logger = Logger.getLogger(HTMLTemplateEngine.class);

		File messageDir = new File(Webapp.MESSAGE_RESOURCE_DIR);
		FileAlterationObserver fo = new FileAlterationObserver(messageDir);
		fo.addListener(new AbstractListener() {
			@Override
			public void onFileChange(File file) {
				try {
					logger.info(file + " changed");
					XMLReader xreader = XMLReaderFactory.createXMLReader();
					String xmlString = IOUtil.toStringWithClose(new FileInputStream(Webapp.MESSAGE_RESOURCE_FILE));
					InputSource input = new InputSource(new StringInputStream(xmlString));

					ToJsonHandler newHandler = new ToJsonHandler();
					xreader.setContentHandler(newHandler);
					xreader.parse(input);

					HTMLTemplateEngine.this.handler = newHandler;
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				}
			}
		});
		FileAlterationMonitor fam = new FileAlterationMonitor(3000, Executors.newScheduledThreadPool(1, ThreadFactoryBuilder.createThreadFactory("monitor-message-thread-%d")), fo);
		fam.start();

	}

	@Override
	public byte[] process(byte[] contentByte, String templatePath, Object arg) throws RuntimeException {

		if (WebUtil.isStaticResource(templatePath)) {
			return contentByte;
		}

		// if (ArrayUtil.contains(template_html, templatePath)) {
		if (templatePath.equals("/"))
			templatePath = "/index.html";

		try {
			if (templatePath.endsWith(".html")) {
				HttpRequest request = (HttpRequest) arg;

				if (!ve.resourceExists(templatePath))
					return contentByte;

				Template tpl = ve.getTemplate(templatePath, "UTF-8");

				StringWriter sw = new StringWriter();
				VelocityContext vc = new VelocityContext(this.vcontext);

				vc.put("request", request);
				vc.put("rsession", rsession);

				String langcode = ObjectUtil.coalesce(request.data(MyAuthenticationHandler.LANGCODE), "us").toString();
				vc.put("m", handler.root(langcode));

				tpl.merge(vc, sw);
				return sw.toString().getBytes(utf8);
			}
		} catch (org.apache.velocity.exception.ParseErrorException ex) {
			ex.printStackTrace(); 
			return contentByte;
		} catch (org.apache.velocity.exception.MethodInvocationException ex){
			ex.printStackTrace(); 
			return contentByte;
		}
		return contentByte;
	}

}
