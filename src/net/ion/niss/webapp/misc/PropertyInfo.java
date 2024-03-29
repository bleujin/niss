package net.ion.niss.webapp.misc;

import java.util.Map.Entry;
import java.util.Properties;

import net.ion.framework.parse.gson.JsonObject;

public class PropertyInfo {
	public JsonObject list() {
		JsonObject result = new JsonObject();
		Properties props = System.getProperties();
		for (Entry<Object, Object> entry : props.entrySet()) {
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}
}
