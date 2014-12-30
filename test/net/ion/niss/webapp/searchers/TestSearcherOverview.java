package net.ion.niss.webapp.searchers;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.scripters.ScheduleUtil;
import net.ion.nradon.stub.StubHttpResponse;


public class TestSearcherOverview extends TestBaseSearcher {

	public void testOvierview() throws Exception {
		
		StubHttpResponse response = ss.request("/searchers/sec1/overview").get() ;
		
		JsonObject json = JsonObject.fromString(response.contentsString()) ; 
		assertEquals(true, json.has("info"));
		assertEquals(true, json.has("recent"));
		assertEquals(true, json.has("popular"));
		
		Debug.line(response.contentsString());
	}
	
	
	
	public void testEditPopularQuery() throws Exception {
		StubHttpResponse response = ss.request("/searchers/sec1/popularquery").post() ;
		Debug.line(response.contentsString());
	}
	
	public void testGetPopularQuery() throws Exception {
		final ReadSession session = rentry.login() ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				String[] querys = new String[]{"bleujin", "jin", "hero"} ;
				
				for (int i = 0; i < querys.length; i++) {
					WriteNode wnode = wsession.pathBy("/searchlogs/sec1/" + querys[i]).property("query", querys[i]) ;
					for (int m = 1; m <= 31; m++) {
						if (m % (i+1) == 0) wnode.property("d" + m, i) ;
					}
				}
				
				return null;
			}
		}) ;
		
		session.pathBy("/searchlogs/sec1").children().debugPrint();
		
		PopularQueryEntry pquery = new PopularQueryEntry(session, Executors.newSingleThreadExecutor());
		String orderedQuery = pquery.result("sec1") ;
		
		Debug.line(orderedQuery);
	}

	
	public void xtestTransform() throws Exception {
		JsonArray values  = JsonParser.fromString("[{'query':'hero','sum':2},{'query':'jin','sum':1},{'query':'bleujin','sum':0}]").getAsJsonArray() ;
		JsonArray template = JsonParser.fromString("[{id:'${q1}'}, {id:'${q2}'}, {id:'hello'}, {id:'${q5}'}]").getAsJsonArray() ;
		
		Map<String, Object> mv = MapUtil.newMap() ;
		for (int i = 1; i <= values.size(); i++) {
			mv.put("q" + i, values.get(i-1).getAsJsonObject().asString("query")) ;
		}
		
		
		Engine engine = Engine.createDefaultEngine() ;
		String result = engine.transform(template.toString(), mv) ;
		
		Debug.line(result);
	}
	
	
	
}
