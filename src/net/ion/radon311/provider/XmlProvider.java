package net.ion.radon311.provider;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.parse.gson.stream.JsonWriter;

import org.apache.ecs.xml.XML;
import org.apache.ecs.xml.XMLDocument;

public class XmlProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

	@Override
	public long getSize(Object arg0, Class<?> clz, Type arg2, Annotation[] arg3, MediaType mtype) {
		return -1;
	}

	@Override
	public boolean isWriteable(Class<?> clz, Type type, Annotation[] annotation, MediaType mtype) {
		return mtype.isCompatible(MediaType.APPLICATION_XML_TYPE) && (XML.class.isAssignableFrom(clz));
	}

	@Override
	public boolean isReadable(Class<?> clz, Type type, Annotation[] annotation, MediaType mtype) {
		return false;
	}


	@Override
	public void writeTo(Object obj, Class<?> clz, Type type, Annotation[] annotations, MediaType mtype, MultivaluedMap<String, Object> mmap, OutputStream output) throws IOException, WebApplicationException {
		XML xml = (XML) obj ;
		BufferedWriter xwriter = new BufferedWriter(new OutputStreamWriter(output, "UTF-8")) ;
		
		XMLDocument doc = new XMLDocument() ;
		doc.addElement(xml) ;
		
		doc.output(output);
	}

	@Override
	public Object readFrom(Class<Object> clz, Type type, Annotation[] annotations, MediaType mtype, MultivaluedMap<String, String> mmap, InputStream input) throws IOException, WebApplicationException {
		
		return null;
	}

	private void writeJsonElement(JsonWriter jwriter, JsonElement parent, String name, JsonElement json) throws IOException {
		if (json.isJsonPrimitive()){
			if (parent.isJsonObject()) jwriter.name(name) ;
			final JsonPrimitive preEle = json.getAsJsonPrimitive();
			if (preEle.isBoolean()){
				jwriter.value(preEle.getAsBoolean()) ;
			} else if (preEle.isNumber()) {
				jwriter.value(preEle.getAsNumber()) ;
			} else {
				jwriter.value(preEle.getAsString()) ;
			} 
		} else if (json.isJsonObject()){
			if (parent.isJsonObject()) jwriter.name(name) ;
			jwriter.beginObject() ;
			for(Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()){
				writeJsonElement(jwriter, json, entry.getKey(), entry.getValue()) ;
			}
			jwriter.endObject() ;
		} else if (json.isJsonArray()){
			if (parent.isJsonObject()) jwriter.name(name) ;
			jwriter.beginArray() ;
			for(JsonElement ele : json.getAsJsonArray()){
				writeJsonElement(jwriter, json, name, ele) ;
			} 
			jwriter.endArray() ;
		} else if (json.isJsonNull()){
			; // ignore
		}
	}
}
