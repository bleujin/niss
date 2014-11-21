package net.ion.niss.webapp.common;

import net.ion.niss.webapp.util.WebUtil;
import net.ion.nradon.HttpControl;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.handler.AbstractHttpHandler;
import net.ion.nradon.handler.authentication.PasswordAuthenticator;
import net.ion.nradon.helpers.Base64;

public class MyAuthenticationHandler extends AbstractHttpHandler {

	public static final String USERNAME = "user";

	private static final String BASIC_PREFIX = "Basic ";

	private final String realm;
	private final PasswordAuthenticator authenticator;

	public MyAuthenticationHandler(PasswordAuthenticator authenticator) {
		this.authenticator = authenticator;
		this.realm = "Secure Area";
	}

	public void handleHttpRequest(final HttpRequest request, final HttpResponse response, final HttpControl control) throws Exception {

		String authHeader = request.header("Authorization");
		
		if (request.uri().startsWith("/open/") || request.uri().endsWith("/login.html") || WebUtil.isStaticResource(request.uri())) {
			control.nextHandler();
			return ;
		}
		
		if (authHeader == null) {
			needAuthentication(response);
		} else {
			if (authHeader.startsWith(BASIC_PREFIX)) {
				String decoded = new String(Base64.decode(authHeader.substring(BASIC_PREFIX.length())));
				final String[] pair = decoded.split(":", 2);
				if (pair.length == 2) {
					final String username = pair[0];
					final String password = pair[1];
					PasswordAuthenticator.ResultCallback callback = new PasswordAuthenticator.ResultCallback() {
						public void success() {
							request.data(USERNAME, username);
							control.nextHandler();
						}

						public void failure() {
							needAuthentication(response);
						}
					};

					authenticator.authenticate(request, username, password, callback, control);
				} else {
					needAuthentication(response);
				}
			}
		}
	}

	private void needAuthentication(HttpResponse response) {
		// response.status(301).header(HttpHeaders.LOCATION, "/login.html").end() ;
		response.status(401).header("WWW-Authenticate", "Basic realm=\"" + realm + "\"").content("Need authentication").end();
	}
	
	public int order() {
		return -1;
	}
}
