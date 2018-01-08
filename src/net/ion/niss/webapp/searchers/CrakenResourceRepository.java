package net.ion.niss.webapp.searchers;

import org.apache.velocity.runtime.resource.util.StringResource;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.StringUtil;

public class CrakenResourceRepository implements StringResourceRepository {

	private ReadSession session;
	public CrakenResourceRepository(ReadSession session) {
		this.session = session ; 
		encoding = "UTF-8";
	}

	public StringResource getStringResource(String path) {
		final String[] vals = StringUtil.split(path, ".") ;
		return new StringResource(session.pathBy(vals[0]).property(vals[1]).asString(), encoding) ;
	}

	public void removeStringResource(String path) {
		final String[] vals = StringUtil.split(path, ".") ;
		session.tran(wsession -> {
			wsession.pathBy(vals[0]).unsetWith(vals[1]).merge();
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
		session.tran(wsession -> {
			wsession.pathBy(vals[0]).property(vals[1], body).merge();
		}) ;
	}

	public void putStringResource(String path, String body, String encoding) {
		putStringResource(path, body);
	}
	
	private String encoding = "UTF-8";
}