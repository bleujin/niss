package net.ion.niss.webapp.collection;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.ecs.xml.XML;

import net.ion.framework.parse.gson.Gson;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.ISearchable;
import net.ion.nsearcher.search.TransformerKey;
import net.ion.radon.util.csv.CsvWriter;

import com.google.common.base.Function;

public class ResFns {

	public final static Function<ReadDocument, JsonObject> ReadDocToJson = new Function<ReadDocument, JsonObject>() {
		@Override
		public JsonObject apply(ReadDocument rdoc) {
			JsonObject json = JsonObject.create();
			String[] fields = rdoc.getFieldNames();
			for (String field : fields) {
				json.put(field, rdoc.getField(field).stringValue());
			}
			return json;
		}
	};

	
	public final static Function<ReadDocument, XML> ReadDocToXML = new Function<ReadDocument, XML>() {
		@Override
		public XML apply(ReadDocument rdoc) {
			XML xml = new XML("doc") ;
			String[] fields = rdoc.getFieldNames();
			for (String field : fields) {
				xml.addAttribute("name", field) ;
				xml.addElement(rdoc.getField(field).stringValue());
			}
			return xml;
		}
	};


	public final static Function<TransformerKey, JsonObject> ResponseToJson = new Function<TransformerKey, JsonObject>() {
		@Override
		public JsonObject apply(TransformerKey tkey) {
			List<Integer> docs = tkey.docs();
			SearchRequest request = tkey.request();
			ISearchable searcher = tkey.searcher();
			try {
				JsonObject body = JsonObject.create();
				body.put("numFound", docs.size());
				JsonArray jarray = new JsonArray();
				body.put("docs", jarray);
				for (int did : docs) {
					ReadDocument rdoc = searcher.doc(did, request);
					jarray.add(rdoc.transformer(ReadDocToJson));
				}

				return body;
			} catch (IOException ex) {
				return new JsonObject().put("numFound", "0").put("docs", new JsonArray()).put("exception", ex.getMessage());
			}
		}
	};

	public final static Function<TransformerKey, Void> createjsonWriterFn(final SearchResponse response, final Map<String, Object> addParam, final Writer writer) {
		final Gson gson = new Gson() ;
		return new Function<TransformerKey, Void>() {
			@Override
			public Void apply(TransformerKey tkey) {
				JsonObject body = response.transformer(ResFns.ResponseToJson);
				JsonObject result = JsonObject.create();

				JsonObject header = new JsonObject();
				header.put("status", body.has("exception") ? -1 : 0).put("QTime", response.elapsedTime());
				for (String pkey : addParam.keySet()) {
					header.put(pkey, addParam.get(pkey));
				}

				result.add("responseHeader", header);

				result.add("response", body);

				JsonWriter jwriter = new JsonWriter(writer);
				jwriter.setIndent("\t");
				gson.toJson(result, jwriter);

				return null;
			}
		};
	}

	public static Function<TransformerKey, Void> createCSVWriterFn(SearchResponse response, final Writer swriter) {
		return new Function<TransformerKey, Void>() {
			@Override
			public Void apply(TransformerKey tkey) {
				CsvWriter writer = new CsvWriter(swriter);
				
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				ISearchable searcher = tkey.searcher();
				try {
					boolean first = true ;
					String[] fieldNames = null ;
					for (int docId : docs) {
						ReadDocument rdoc = searcher.doc(docId, request);
						if (first){
							fieldNames = rdoc.getFieldNames() ;
							writer.writeLine(fieldNames);
							first = false ;
						}
						
						for (String fname : fieldNames) {
							writer.writeField(rdoc.get(fname)) ;
						}
						writer.endBlock(); 
					}
				} catch (IOException ex) {
					ex.printStackTrace(); 
				}
				// writer.writeField(field);

				return null;
			}

		};
	}

}
