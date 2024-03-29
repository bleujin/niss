package net.ion.niss.webapp.indexers;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.ecs.xml.XML;

import com.google.common.base.Function;

import net.bleujin.searcher.common.ReadDocument;
import net.bleujin.searcher.search.SearchRequest;
import net.bleujin.searcher.search.SearchResponse;
import net.bleujin.searcher.search.SearchSession;
import net.bleujin.searcher.search.TransformerSearchKey;
import net.ion.framework.parse.gson.Gson;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.radon.util.csv.CsvWriter;

public class ResFns {

	public final static Function<ReadDocument, JsonObject> ReadDocToJson = new Function<ReadDocument, JsonObject>() {
		@Override
		public JsonObject apply(ReadDocument rdoc) {
			JsonObject json = JsonObject.create();
			String[] fields = rdoc.fieldNames();
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
			String[] fields = rdoc.fieldNames();
			for (String field : fields) {
				xml.addAttribute("name", field) ;
				xml.addElement(rdoc.getField(field).stringValue());
			}
			return xml;
		}
	};


	public final static Function<TransformerSearchKey, JsonObject> ResponseToJson = new Function<TransformerSearchKey, JsonObject>() {
		@Override
		public JsonObject apply(TransformerSearchKey tkey) {
			List<Integer> docs = tkey.docs();
			SearchRequest request = tkey.request();
			SearchSession searcher = tkey.session();
			try {
				JsonObject body = JsonObject.create();
				body.put("numFound", docs.size());
				JsonArray jarray = new JsonArray();
				body.put("docs", jarray);
				for (int did : docs) {
					ReadDocument rdoc = searcher.readDocument(did, request);
					jarray.add(rdoc.transformer(ReadDocToJson));
				}

				return body;
			} catch (IOException ex) {
				return new JsonObject().put("numFound", "0").put("docs", new JsonArray()).put("exception", ex.getMessage());
			}
		}
	};

	public final static Function<TransformerSearchKey, Void> createjsonWriterFn(final SearchResponse response, final Map<String, Object> addParam, final Writer writer) {
		final Gson gson = new Gson() ;
		return new Function<TransformerSearchKey, Void>() {
			@Override
			public Void apply(TransformerSearchKey tkey) {
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

	public static Function<TransformerSearchKey, Void> createCSVWriterFn(SearchResponse response, final Writer swriter) {
		return new Function<TransformerSearchKey, Void>() {
			@Override
			public Void apply(TransformerSearchKey tkey) {
				CsvWriter writer = new CsvWriter(swriter);
				
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				SearchSession searcher = tkey.session();
				try {
					boolean first = true ;
					String[] fieldNames = null ;
					for (int docId : docs) {
						ReadDocument rdoc = searcher.readDocument(docId, request);
						if (first){
							fieldNames = rdoc.fieldNames() ;
							writer.writeLine(fieldNames);
							first = false ;
						}
						
						for (String fname : fieldNames) {
							writer.writeField(rdoc.asString(fname)) ;
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
