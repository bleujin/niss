package net.ion.niss.webapp.misc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.Webapp;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

@Path("/analysis")
public class AnalysisWeb implements Webapp {
	
	
	private static List<Class<? extends Analyzer>> analyzers = 
				ListUtil.<Class<? extends Analyzer>> toList(MyKoreanAnalyzer.class, StandardAnalyzer.class, CJKAnalyzer.class, WhitespaceAnalyzer.class, SimpleAnalyzer.class);

	
	public static List<Class<? extends Analyzer>> analysis(){
		return analyzers ;
	}
	
	@GET
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject list(){
		JsonObject result = new JsonObject() ;
		
		JsonArray analysis = new JsonArray() ;
		for(Class<? extends Analyzer> clz : analyzers){
			analysis.add(new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName())) ;
		}
		result.add("analyzer", analysis);
		
		return result ;
	}
	
	
	@POST
	@Path("")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject tokenAnalyzer(@DefaultValue("") @FormParam("content") String content, @FormParam("analyzer") String clzNames, @DefaultValue("") @FormParam("stopword") String stopword) throws Exception{
		JsonObject result = new JsonObject() ;
		for (String clzName : StringUtil.split(clzNames, ",")) {
			Class<? extends Analyzer> aclz = (Class<? extends Analyzer>) Class.forName(clzName) ;
			
			CharArraySet set = new CharArraySet(SearchConstant.LuceneVersion, ListUtil.toList(stopword.split("\\s+")), true) ;
			
			Constructor<? extends Analyzer> findCon = ConstructorUtils.getAccessibleConstructor(aclz, new Class[]{Version.class, CharArraySet.class}) ;
			Analyzer analyzer = null ;
			if (findCon == null){
				analyzer = aclz.getConstructor(Version.class).newInstance(SearchConstant.LuceneVersion) ;
			} else {
				analyzer = findCon.newInstance(SearchConstant.LuceneVersion, set) ;
			}
			
			TokenStream tokenStream = analyzer.tokenStream("text", content);
			OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			tokenStream.reset();

			JsonArray array = new JsonArray() ;
			while (tokenStream.incrementToken()) {
			    int startOffset = offsetAttribute.startOffset();
			    int endOffset = offsetAttribute.endOffset();
			    array.add(new JsonObject().put("term", charTermAttribute.toString()).put("start", startOffset).put("end", endOffset));
			}
			IOUtil.close(tokenStream);
			IOUtil.close(analyzer);

			result.add(aclz.getCanonicalName(), array);
		}
		return result ;
	}
	
	
	// util
	public static final JsonArray analParse(Analyzer analyzer, String content) throws IOException{
		TokenStream tokenStream = analyzer.tokenStream("text", content);
		OffsetAttribute offsetAttribute = tokenStream.getAttribute(OffsetAttribute.class);
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		JsonArray array = new JsonArray() ;
		while (tokenStream.incrementToken()) {
		    int startOffset = offsetAttribute.startOffset();
		    int endOffset = offsetAttribute.endOffset();
		    array.add(new JsonObject().put("term", charTermAttribute.toString()).put("start", startOffset).put("end", endOffset));
		}
		IOUtil.close(tokenStream);
		IOUtil.close(analyzer);
		return array ;
	}
}
