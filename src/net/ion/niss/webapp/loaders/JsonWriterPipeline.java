package net.ion.niss.webapp.loaders;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.ObjectUtil;
import net.ion.icrawler.Request;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;

public class JsonWriterPipeline implements Pipeline{

	private JsonWriter writer;
	private Writer out;
	
	public JsonWriterPipeline(Writer out){
		this.out = out ;
		this.writer = new JsonWriter(out) ;
	}
	
	
	public void begin() throws IOException{
		writer.setIndent("    ");
		writer.beginArray() ;
	}
	
	
	public void end() throws IOException{
		writer.endArray() ;
		writer.flush();
		writer.close(); 
	}
	
	@Override
	public void process(ResultItems ritems, Task task) {
		Map<String, Object> items = ritems.getAll() ;
		
		Request request = ritems.getRequest() ;
		
		try {
			writer.beginObject()
				.name("uri").value(request.getUrl()) ;
			for(Entry<String, Object> entry : items.entrySet()){
				writer.name(entry.getKey()).value(ObjectUtil.toString(entry.getValue())) ;
			}
			
			writer.endObject() ;
			writer.flush();
		} catch (IOException e) {
			try {
				writer.name("exeption occured").value(e.getMessage()) ;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} 
	}

}
