package net.ion.niss.webapp;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

@Path("/analysis")
public class AnalysisWeb implements Webapp {
	
	
	private static List<Class<? extends Analyzer>> analyzers = ListUtil.<Class<? extends Analyzer>> toList(MyKoreanAnalyzer.class, StandardAnalyzer.class, CJKAnalyzer.class, WhitespaceAnalyzer.class, SimpleAnalyzer.class);

	
	@GET
	@Path("")
	public JsonArray list(){
		JsonArray result = new JsonArray() ;
		
		for(Class<? extends Analyzer> clz : analyzers){
			result.add(new JsonObject().put("clz", clz.getCanonicalName()).put("name", clz.getSimpleName())) ;
		}
		return result ;
	}
	
	
}
