package net.ion.niss.webapp.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.REntry;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.handler.AbstractHttpHandler;
import net.ion.nradon.wrapper.HttpResponseWrapper;


public class TraceHandler extends AbstractHttpHandler{

	private ReadSession rsession;
	public TraceHandler(REntry rentry) throws IOException {
		this.rsession = rentry.login() ;
	}

	public void handleHttpRequest(final HttpRequest request, final HttpResponse response, HttpControl control) throws Exception {

		if ("GET".equals(request.method())) {
			control.nextHandler(request,  response, control);
			return ;
		}
		
		final JsonObject params = new JsonObject() ;
		Set<String> keys = request.postParamKeys() ;
		for (String key : keys) {
			params.put(key, StringUtil.abbreviate(request.postParam(key), 20)) ;
		}
		
		HttpResponse responseWrapper = new HttpResponseWrapper(response) {
			InetSocketAddress iaddress = (InetSocketAddress)request.remoteAddress() ;
			Object userName = ObjectUtil.coalesce(request.data(MyAuthenticationHandler.USERNAME), "anonymous");

			@Override
			public HttpResponseWrapper end() {
				rsession.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						
						wsession.pathBy("/traces/" +  userName).child(new ObjectId().toString())
							.property("method", request.method())
							.property("uri", request.uri())
							.property("address", iaddress.getAddress().getHostAddress())
							.property("time", System.currentTimeMillis())
							.property("params", params.toString())
							.property("status", response.status());
							
						return null;
					}
				}) ;
				return super.end();
			}

			@Override
			public HttpResponseWrapper error(final Throwable error) {
				rsession.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/traces/" +  userName).child(new ObjectId().toString())
							.property("method", request.method())
							.property("uri", request.uri())
							.property("address", iaddress.getAddress().getHostAddress())
							.property("exception", error.getMessage())
							.property("time", System.currentTimeMillis())
							.property("params", params.toString())
							.property("status", response.status());
						return null;
					}
				}) ;
				return super.error(error);
			}
		};

		control.nextHandler(request, responseWrapper, control);
	}

	public int order() {
		return 0;
	}
}
