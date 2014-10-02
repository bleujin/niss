package net.ion.niss.webapp.searchers;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.StringUtil;

import org.apache.velocity.runtime.resource.util.StringResource;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

public class CrakenResourceRepository implements StringResourceRepository {

	private ReadSession session;
	public CrakenResourceRepository(ReadSession session) {
		this.session = session ; 
		encoding = "UTF-8";
	}

	public StringResource getStringResource(String path) {
		final String[] vals = StringUtil.split(path, ".") ;
		return new StringResource(session.ghostBy(vals[0]).property(vals[1]).asString(), encoding) ;
	}

	public void removeStringResource(String path) {
		final String[] vals = StringUtil.split(path, ".") ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(vals[0]).unset(vals[1]) ;
				return null;
			}
		}) ;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		;
	}

	public void putStringResource(String path, final String body) {
		final String[] vals = StringUtil.split(path, ".") ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy(vals[0]).property(vals[1], body) ;
				return null;
			}
		}) ;
	}

	public void putStringResource(String path, String body, String encoding) {
		putStringResource(path, body);
	}
	
	private String encoding = "UTF-8";
}