package net.ion.niss.webapp.common;

import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.Radon;
import net.ion.nradon.handler.event.ServerEvent.EventType;

public class FavIconHandler implements HttpHandler {

	
	@Override
	public void onEvent(EventType eventtype, Radon radon) {
	}

	@Override
	public int order() {
		return 0;
	}

	@Override
	public void handleHttpRequest(HttpRequest request, HttpResponse response, HttpControl control) throws Exception {
		response.status(404).end() ;
	}

}
