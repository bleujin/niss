package net.ion.niss.webapp.searchers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.rcraken.ReadSession;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.scripters.ScheduleUtil;

public class PopularQueryEntry {

	public static final String DFT_TEMPLATE = "[{id:'${q1}'}, {id:'${q2}'}, {id:'${q3}'}, {id:'${q4}'}, {id:'${q5}'}]";
	public final static String EntryName = "pqentry" ;
	private final Cache<String, String> scache;
	private final ReadSession rsession;
	private Engine engine;
	private ExecutorService es;
	
	public PopularQueryEntry(ReadSession rsession, ExecutorService es){
		this.scache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build() ;
		this.rsession = rsession ;
		this.engine = Engine.createDefaultEngine() ;
		this.es = es ;
	}
	
	public String result(final String sid) throws ExecutionException{
		
		return scache.get(sid, new Callable<String>(){
			@Override
			public String call() throws Exception {
				
				final ReadNode snode = rsession.pathBy("/searchlogs/" + sid);
				final ReadNode pqnode = rsession.pathBy("/searchers/" + sid + "/popularquery");
				String result = pqnode.property(Def.Searcher.Popular.Transformed).asString() ;

				Callable<String> call = new Callable<String>(){
					public String call() {
						try {
							return rsession.tranSync(wsession -> {
								String transformed = snode.children().stream().transform(new Function<Iterable<ReadNode>, String>() {
									@Override
									public String apply(Iterable<ReadNode> iter) {
										
										ScheduleUtil su = new ScheduleUtil() ; 
										int dayRange = snode.property(Def.Searcher.Popular.DayRange).defaultValue(3) ;
										
										List<String> props =  ListUtil.newList() ;
										for (int i = 0; i < dayRange; i++) {
											props.add("d" + su.nextDate((i * -1))) ;
										}
										
										TopEntryCollector<JsonObject> coll = new TopEntryCollector<JsonObject>(10, new Comparator<JsonObject>() {
											@Override
											public int compare(JsonObject o1, JsonObject o2) {
												return o1.asInt("sum") - o2.asInt("sum") ;
											}
										}) ;
										for(ReadNode rnode : iter){
											int sum = 0 ;
											for(String prop : props){
												sum+= rnode.property(prop).defaultValue(0) ;
											}
											coll.add(new JsonObject().put("query", rnode.property("query").asString()).put("sum", sum)) ;
										}
										JsonArray topQuerys = new JsonArray().addCollection(coll.result());
										Map<String, Object> mv = MapUtil.newMap() ;
										for (int i = 1; i <= topQuerys.size(); i++) {
											mv.put("q" + i, topQuerys.get(i-1).getAsJsonObject().asString("query")) ;
										}
										
										String template = pqnode.property(Def.Searcher.Popular.Template).defaultValue(DFT_TEMPLATE) ;
										String result = engine.transform(template, mv) ;
										
										return result;
									}
								}) ;
								wsession.pathBy(pqnode.fqn()).property(Def.Searcher.Popular.Transformed, transformed).merge();
								scache.put(sid, transformed);
								return transformed;
							}) ;
						} catch (Exception e) {
							return String.format("[{'q1':%s}]", e.getMessage()) ;
						}
					} ;
				} ;
				
				Future<String> future = es.submit(call) ; 
				
				if (StringUtil.isBlank(result)){
					result = future.get() ; 
				}
				
				return result ;
			}
			
		}) ;
		
	}

	public void invalidate(String sid) {
		scache.invalidate(sid); 
	}
	
	
}
