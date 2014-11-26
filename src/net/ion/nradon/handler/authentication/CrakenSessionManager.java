package net.ion.nradon.handler.authentication;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.ObjectUtil;

import org.infinispan.Cache;

public class CrakenSessionManager implements SessionManager {

	private Cache<String, SessionInfo> cache;
	private int liveSeconds = 60 * 10 ;

	public CrakenSessionManager(Cache<String, SessionInfo> cache) {
		this.cache = cache;
	}

	@Override
	public SessionInfo findSession(String sessionKey) {
		SessionInfo result = cache.get(sessionKey);
		return ObjectUtil.coalesce(result, SimpleSessionInfo.NOTEXIST);
	}

	@Override
	public boolean hasSession(String sessionKey) {
		return cache.containsKey(sessionKey);
	}

	@Override
	public SessionInfo newSession(String sessionKey) {
		CrakenSessionInfo created = new CrakenSessionInfo(new JsonObject().put("_radon_sessionid", sessionKey));
		cache.put(sessionKey, created, liveSeconds, TimeUnit.SECONDS);
		return created;
	}

	public CrakenSessionManager liveSeconds(int liveSeconds) {
		this.liveSeconds = liveSeconds ;
		return this ;
	}

}

class CrakenSessionInfo implements SessionInfo, Serializable {

	private static final long serialVersionUID = 2675935249263260371L;
	private volatile JsonObject values;

	public CrakenSessionInfo(JsonObject values) {
		this.values = values;
	}

	@Override
	public boolean downTouched(long arg0) {
		return false;
	}

	@Override
	public boolean hasValue(String name) {
		return values.has(name);
	}

	@Override
	public SessionInfo register(String name, Object value) {
		values.put(name, value);
		return this;
	}

	@Override
	public String sessionKey() {
		return values.asString("_radon_sessionid");
	}

	@Override
	public void touch() {

	}

	protected Object writeReplace() throws ObjectStreamException {
		return new SerializedSessionInfo(values);
	}

	@Override
	public Object value(String name) {
		return values.getAsJsonPrimitive(name).getValue() ;
	}

	@Override
	public <T> T value(String name, T dftValue) {
		JsonPrimitive found = values.getAsJsonPrimitive(name);
		return (found == null) ? dftValue : (T)found.getValue();
	}
}

class SerializedSessionInfo implements Serializable {

	private static final long serialVersionUID = 1937738767870375587L;
	private String jsonString;

	public SerializedSessionInfo(JsonObject values) {
		this.jsonString = values.toString();
	}

	private Object readResolve() throws ObjectStreamException {
		return new CrakenSessionInfo(JsonParser.fromString(jsonString).getAsJsonObject()) ;
	}

}
