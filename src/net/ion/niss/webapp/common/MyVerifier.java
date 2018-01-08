package net.ion.niss.webapp.common;

import java.util.concurrent.Executor;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.handler.authentication.PasswordAuthenticator;

public class MyVerifier implements PasswordAuthenticator {

	private ReadSession session;

	private MyVerifier(ReadSession session) {
		this.session = session;
	}

	public static MyVerifier test(ReadSession session) throws Exception {
		return new MyVerifier(session).addUser("admin", "admin", "success");
	}

	// Only Use Test
	MyVerifier addUser(final String userId, final String name, final String password) throws Exception {
		session.tranSync(wsession -> {
			if (! wsession.exist("/users/" + userId)){
				wsession.pathBy("/users/" + userId).property(Def.User.Name, name).property(Def.User.Password, password).merge();
			}
		});

		session.workspace().name() ;

		return this;
	}

	public void authenticate(HttpRequest request, String username, String password, ResultCallback callback, Executor handlerExecutor) {
		ReadNode found = session.pathBy("/users/" + username);
		String expectedPassword = found.property(Def.User.Password).asString();
		if (expectedPassword != null && password.equals(expectedPassword)) {
			String langcode = found.property(MyAuthenticationHandler.LANGCODE).defaultValue("us");
			request.data(MyAuthenticationHandler.LANGCODE, langcode) ;
			
			callback.success();
		} else {
			callback.failure();
		}
	}
}
