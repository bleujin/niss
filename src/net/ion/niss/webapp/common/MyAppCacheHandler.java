package net.ion.niss.webapp.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import net.ion.framework.util.IOUtil;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.handler.event.ServerEvent.EventType;

import org.apache.commons.lang.SystemUtils;
import org.jboss.resteasy.util.HttpHeaderNames;

public class MyAppCacheHandler implements HttpHandler {

	private File oriFile;
	private String staticContent = "";
	private String startupContent = "";

	public MyAppCacheHandler(String originalFile) {
		this.oriFile = new File(originalFile);
	}

	@Override
	public void onEvent(EventType etype, Radon radon) {
		try {
			if (etype == EventType.START && oriFile.exists()) {
				this.staticContent = IOUtil.toStringWithClose(new FileInputStream(oriFile));
				this.startupContent = staticContent + SystemUtils.LINE_SEPARATOR + "#starup :" + new Date().toString() ;
			}
		} catch (IOException ex) {
			System.err.println("not found cache.manifest file");
		}
	}

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
		if ("/cache.appcache".equals(request.uri())) {
			response.header(HttpHeaderNames.CACHE_CONTROL, "").header(HttpHeaderNames.EXPIRES, "0").content(staticContent).header(HttpHeaderNames.CONTENT_TYPE, "text/cache-manifest").end();
		} else if ("/cache.appcache.startup".equals(request.uri())) {
			response.header(HttpHeaderNames.CACHE_CONTROL, "no-store, no-cache").header(HttpHeaderNames.EXPIRES, "0").content(startupContent).header(HttpHeaderNames.CONTENT_TYPE, "text/cache-manifest").end();
		} else if ("/cache.appcache.reload".equals(request.uri())) {
			String nocacheContent = "CACHE MANIFEST" + SystemUtils.LINE_SEPARATOR + "# "+ new Date().toString() + SystemUtils.LINE_SEPARATOR + "NETWORK:" + SystemUtils.LINE_SEPARATOR + "*" ;
			response.header(HttpHeaderNames.CACHE_CONTROL, "no-store, no-cache").header(HttpHeaderNames.EXPIRES, "0").content(nocacheContent).header(HttpHeaderNames.CONTENT_TYPE, "text/cache-manifest").end();
		} else
			control.nextHandler();

	}

}
