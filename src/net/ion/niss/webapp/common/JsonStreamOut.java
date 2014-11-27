package net.ion.niss.webapp.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.parse.gson.stream.JsonWriter;

public class JsonStreamOut implements StreamingOutput {

	private JsonObject json;
	private boolean indent;

	public JsonStreamOut(JsonObject json, boolean indent) {
		this.json = json;
		this.indent = indent;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(output, "UTF-8"));
		if (indent)
			jwriter.setIndent("  ");

		jwriter.beginObject();
		for (Entry<String, JsonElement> entry : json.entrySet()) {
			writeJsonElement(jwriter, json, entry.getKey(), entry.getValue());
		}
		jwriter.endObject();
		jwriter.flush();
	}

	private void writeJsonElement(JsonWriter jwriter, JsonElement parent, String name, JsonElement json) throws IOException {
		if (json.isJsonPrimitive()) {
			if (parent.isJsonObject())
				jwriter.name(name);
			final JsonPrimitive preEle = json.getAsJsonPrimitive();
			if (preEle.isBoolean()) {
				jwriter.value(preEle.getAsBoolean());
			} else if (preEle.isNumber()) {
				jwriter.value(preEle.getAsNumber());
			} else {
				jwriter.value(preEle.getAsString());
			}
		} else if (json.isJsonObject()) {
			if (parent.isJsonObject())
				jwriter.name(name);
			jwriter.beginObject();
			for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
				writeJsonElement(jwriter, json, entry.getKey(), entry.getValue());
			}
			jwriter.endObject();
		} else if (json.isJsonArray()) {
			if (parent.isJsonObject())
				jwriter.name(name);
			jwriter.beginArray();
			for (JsonElement ele : json.getAsJsonArray() ) {
				writeJsonElement(jwriter, json, name, ele);
			}
			jwriter.endArray();
		} else if (json.isJsonNull()) {
			; // ignore
		}
	}
}