package net.ion.niss.webapp.common;

import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonUtil;
import net.ion.framework.util.StringUtil;

public abstract class MessageEntity {
	private JsonObject inner = new JsonObject();
	private MessageEntity parent = null;
	private String localName;

	protected MessageEntity(String localName) {
		this.localName = localName;
	}

	public abstract String asString(String path) ;

	public abstract String asString(String path, Object... param)  ;

	protected JsonObject inner() {
		return inner;
	}

	void add(String qName, JsonElement jp) {
		inner.add(qName, jp);
	}

	MessageEntity parent(MessageEntity parent) {
		parent.add(localName, this.inner);
		this.parent = parent;
		return this;
	}

	String localName() {
		return localName;
	}

	MessageEntity parent() {
		return parent;
	}

	public String toString() {
		return inner.toString();
	}
}

class MessageEntityImpl extends MessageEntity {
	
	protected MessageEntityImpl(String localName) {
		super(localName);
	}

	public String asString(String path) {
		String fullPath = "messages." + path;
		JsonElement found = JsonUtil.findElement(inner(), fullPath);
		if (found == null) {
			return path;
		} else if (found.isJsonObject() && found.getAsJsonObject().has("text")){
			return found.getAsJsonObject().asString("text") ;
		} else if (found.isJsonPrimitive()){
			return found.getAsJsonPrimitive().getAsString() ;
		}

		return path;
	}

	public String asString(String path, Object... param) {
		String fullPath = "messages." + path;
		JsonElement found = JsonUtil.findElement(inner(), fullPath);
		if (found == null) {
			return path ;
		} else if (found.isJsonObject() && found.getAsJsonObject().has("text")){
			return String.format(found.getAsJsonObject().asString("text"), param) ;
		} else if (found.isJsonPrimitive()){
			return String.format(found.getAsJsonPrimitive().getAsString(), param) ;
		}

		return path;
	}	
}