package net.ion.niss.webapp;

import java.io.IOException;
import java.util.concurrent.Executor;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.handler.authentication.PasswordAuthenticator;

public class UserVerifier implements PasswordAuthenticator {

	private ReadSession session;

	private UserVerifier(ReadSession session) {
		this.session = session;
	}

	public static UserVerifier test(ReadSession session) throws IOException {
		return new UserVerifier(session).addUser("bleujin", "1");
	}

	// Only Use Test
	UserVerifier addUser(final String userId, final String password) throws IOException {
		try {
			session.tranSync(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/users/" + userId).property("password", password);
					return null;
				}
			});
		} catch (Exception ex) {
			throw new IOException(ex);
		}
		return this;
	}

	public void authenticate(HttpRequest request, String username, String password, ResultCallback callback, Executor handlerExecutor) {
		String expectedPassword = session.pathBy("/users/" + username).property("password").stringValue();
		if (expectedPassword != null && password.equals(expectedPassword)) {
			callback.success();
		} else {
			callback.failure();
		}
	}
}
