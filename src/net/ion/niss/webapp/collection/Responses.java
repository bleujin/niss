package net.ion.niss.webapp.collection;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.ecs.xml.XML;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.ISearchable;
import net.ion.nsearcher.search.TransformerKey;

import com.google.common.base.Function;

public class Responses {

	public static Function<TransformerKey, JsonObject> toJson(final MultivaluedMap<String, String> paramMap, final SearchResponse response) {
		return new Function<TransformerKey, JsonObject>() {
			@Override
			public JsonObject apply(TransformerKey tkey) {
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				ISearchable searcher = tkey.searcher();

				JsonObject result = JsonObject.create();
				JsonObject header = JsonObject.create();
				result.add("header", header);

				header.put("size", docs.size());
				header.put("total", response.totalCount());
				header.put("skip", request.skip());
				header.put("offset", request.offset());

				header.put("elapsedTime", response.elapsedTime());
				header.put("params", JsonObject.fromObject(paramMap));
				JsonArray jarray = new JsonArray();
				result.put("docs", jarray);

				try {
					for (int did : docs) {
						ReadDocument rdoc = searcher.doc(did, request);
						jarray.add(rdoc.transformer(ResFns.ReadDocToJson));
					}

					return result;
				} catch (IOException ex) {
					header.put("exception", ex.getMessage());
					return result;
				}
			}
		};
	}

	public static Function<TransformerKey, XML> toXML(final MultivaluedMap<String, String> paramMap, final SearchResponse response) {
		return new Function<TransformerKey, XML>() {
			@Override
			public XML apply(TransformerKey tkey) {
				List<Integer> docs = tkey.docs();
				SearchRequest request = tkey.request();
				ISearchable searcher = tkey.searcher();
				XML result = new XML("response");

				XML header = new XML("header");
				result.addElement(header);

				header.addAttribute("size", docs.size());
				header.addAttribute("total", response.totalCount());
				header.addAttribute("skip", request.skip());
				header.addAttribute("offset", request.offset());

				header.addAttribute("elapsedTime", response.elapsedTime());
				// header.put("params", JsonObject.fromObject(paramMap));

				XML docsXML = new XML("docs");
				result.addElement(docsXML);

				try {

					for (int did : docs) {
						ReadDocument rdoc = searcher.doc(did, request);
						docsXML.addElement(rdoc.transformer(ResFns.ReadDocToXML));
					}

					return result;
				} catch (IOException ex) {
					return result;
				}
			}
		};
	}

	public static Function<TransformerKey, Source> toXMLSource(final MultivaluedMap<String, String> paramMap, final SearchResponse response) {

		return new Function<TransformerKey, Source>() {
			@Override
			public Source apply(TransformerKey tkey) {
				try {
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
					final Document xmlDoc = docBuilder.newDocument();
					xmlDoc.setXmlStandalone(true);
					Element rootElement = xmlDoc.createElement("response");
					xmlDoc.appendChild(rootElement);

					Element header = xmlDoc.createElement("header");
					rootElement.appendChild(header);

					List<Integer> docs = tkey.docs();
					SearchRequest request = tkey.request();
					ISearchable searcher = tkey.searcher();

					header.setAttribute("size", String.valueOf(docs.size()));
					header.setAttribute("total", String.valueOf(response.totalCount()));
					header.setAttribute("skip", String.valueOf(request.skip()));
					header.setAttribute("offset", String.valueOf(request.offset()));
					header.setAttribute("elapsedTime", String.valueOf( response.elapsedTime()));
					for(String key : paramMap.keySet()){
						header.setAttribute(key,  StringUtil.join(paramMap.get(key), ",")) ;
					}

					Element docsEle = xmlDoc.createElement("docs");
					rootElement.appendChild(docsEle);

					for (int did : docs) {
						ReadDocument rdoc = searcher.doc(did, request);
						Element doc = xmlDoc.createElement("doc") ;
						String[] fields = rdoc.getFieldNames();
						for (String field : fields) {
							Element fieldEle = xmlDoc.createElement("field") ;
							fieldEle.setAttribute("name", field) ;
							fieldEle.setTextContent(rdoc.getField(field).stringValue());
							doc.appendChild(fieldEle) ;
						}
						docsEle.appendChild(doc) ;
					}

					DOMSource source = new DOMSource(xmlDoc);
					return source;
					
				} catch (IOException ex) {
					return null ;
				} catch (ParserConfigurationException ex) {
					return null ;
				}
			}
		};
	}

}
