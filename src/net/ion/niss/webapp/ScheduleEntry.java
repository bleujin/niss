package net.ion.niss.webapp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.schedule.AtTime;
import net.ion.framework.schedule.Job;
import net.ion.framework.schedule.Scheduler;
import net.ion.framework.util.ObjectUtil;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.loaders.InstantJavaScript;
import net.ion.niss.webapp.loaders.JScriptEngine;
import net.ion.niss.webapp.loaders.ResultHandler;

public class ScheduleEntry {

	public final static String EntryName = "sentry" ;
	private Scheduler scheduler;
	
	public ScheduleEntry(Scheduler scheduler) {
		this.scheduler = scheduler ;
	}

	public final ScheduleEntry create(String name, ExecutorService worker){
		ScheduleEntry result = new ScheduleEntry(new Scheduler(name, worker));
		
		return result ;
	}

	public void addRegisterdSchedule(final JScriptEngine jengine, final REntry rentry) throws IOException {

		ReadSession rsession = rentry.login() ;
		rsession.ghostBy("/scripts").children().transform(new Function<Iterator<ReadNode>, Void>(){
			@Override
			public Void apply(Iterator<ReadNode> iter) {
				while(iter.hasNext()){
					ReadNode snode = iter.next() ;
					final String scriptId = snode.fqn().name() ;
					final String scriptContent = snode.property(Def.Script.Content).asString() ;
					if (snode.hasChild("schedule")){
						
						ReadNode sinfo = snode.child("schedule") ;
						
						if (! sinfo.property("enable").asBoolean()) continue ;
						
						AtTime attime = makeAtTime(sinfo) ;
						Callable<Void> callable = makeCallable(jengine, rentry, scriptId);
						
						

						

						
						
					}
				}
				return null;
			}

			private Callable<Void> makeCallable(final JScriptEngine jengine, final REntry rentry, final String scriptId) {
				return new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						ReadSession rsession = rentry.login() ;
						String scriptContent = rsession.ghostBy("/scripts/" + scriptId).property(Def.Script.Content).asString() ;
						
						
						StringWriter writer = new StringWriter() ;
						MultivaluedMap<String, String> params = new MultivaluedMapImpl<String, String>() ;
						InstantJavaScript script = jengine.createScript(IdString.create(scriptId), "", new StringReader(scriptContent)) ;
						
						StringWriter result = new StringWriter();
						final JsonWriter jwriter =  new JsonWriter(result) ;
						script.exec(new ResultHandler<Void>() {
							@Override
							public Void onSuccess(Object result, Object... args) {
								try {
									jwriter.beginObject().name("return").value(ObjectUtil.toString(result)) ;
								} catch (IOException ignore) {
								}
								return null;
							}

							@Override
							public Void onFail(Exception ex, Object... args) {
								try {
									jwriter.beginObject().name("return").value("").name("exception").value(ex.getMessage()) ;
								} catch (IOException e) {
								}
								return null;
							}
						}, writer, rsession, params, rentry, jengine) ;
						
						jwriter.name("writer").value(writer.toString()) ;
						
						jwriter.name("params") ;
						jwriter.beginArray() ;
						for (Entry<String, List<String>> entry : params.entrySet()) {
							jwriter.beginObject().name(entry.getKey()).beginArray() ;
							for(String val : entry.getValue()){
								jwriter.value(val) ;
							}
							jwriter.endArray().endObject() ;
						}
						jwriter.endArray() ;
						jwriter.endObject() ;
						jwriter.close();								
						
						// write log
						
						
						
						return null;
					}
				};
			}

			private AtTime makeAtTime(ReadNode sinfo) {
				String expr = sinfo.property("minute").asString() + " "  
						+ sinfo.property("hour").asString() + " "  
						+ sinfo.property("day").asString() + " "  
						+ sinfo.property("month").asString() + " "  
						+ sinfo.property("week").asString() + " "  
						+ sinfo.property("matchtime").asString() + " "  
						+ sinfo.property("year") ;
				
				return new AtTime(expr);
			}
			
		}) ;
		
	}
	
	
	public ScheduleEntry removeSchedule(String scriptId){
		scheduler.removeJob(scriptId); ;
		return this ;
	}
	
	
	
	public ScheduleEntry addSchedule(Job job){
		scheduler.addJob(job);
		return this ;
	}
}
