package net.ion.niss.apps.loader;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.framework.parse.gson.Gson;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectUtil;
import net.ion.icrawler.Request;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;

public class JsonWriterPipeline implements Pipeline{

	private JsonWriter writer;
	private Writer out;
	private Gson gson;
	
	public JsonWriterPipeline(Writer out){
		this.out = out ;
		this.writer = new JsonWriter(out) ;
		this.gson = new Gson() ;
	}
	
	
	public void begin() throws IOException{
		writer.setIndent("    ");
		writer.beginArray() ;
	}
	
	
	public void end() throws IOException{
		writer.endArray() ;
		writer.close();
	}
	
	
	@Override
	public void process(ResultItems ritems, Task task) {
		Map<String, Object> items = ritems.getAll() ;
		JsonObject json = new JsonObject();
//		for(Entry<String, Object> entry : items.entrySet()){
//			json.put(entry.getKey(), ObjectUtil.toString(entry.getValue())) ;
//		}
		
		Request request = ritems.getRequest() ;
		
		json.put("uri", request.getUrl()) ;
		json.put("title", ritems.asString("title")) ;
		
		gson.toJson(json, writer);
	}

}
