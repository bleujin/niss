package net.ion.niss.webapp;

import java.io.IOException;
import java.util.concurrent.Executor;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.niss.webapp.common.Def;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.handler.authentication.PasswordAuthenticator;

public class UserVerifier implements PasswordAuthenticator {

	private ReadSession session;

	private UserVerifier(ReadSession session) {
		this.session = session;
	}

	public static UserVerifier test(ReadSession session) throws IOException {
		return new UserVerifier(session).addUser("admin", "admin", "success");
	}

	// Only Use Test
	UserVerifier addUser(final String userId, final String name, final String password) throws IOException {
		try {
			session.tranSync(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					if (! wsession.exists("/users/" + userId)){
						wsession.pathBy("/users/" + userId).property(Def.User.Name, name).property(Def.User.Password, password);
					}
					return null;
				}
			});
		} catch (Exception ex) {
			throw new IOException(ex);
		}
		session.workspace().wsName() ;

		return this;
	}

	public void authenticate(HttpRequest request, String username, String password, ResultCallback callback, Executor handlerExecutor) {
		session.ghostBy("/users").children().toList().size() ;
		String expectedPassword = session.ghostBy("/users/" + username).property(Def.User.Password).stringValue();
		if (expectedPassword != null && password.equals(expectedPassword)) {
			callback.success();
		} else {
			callback.failure();
		}
	}
}
