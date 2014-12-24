package net.ion.niss.webapp.scripters;

import java.util.Date;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.framework.schedule.AtTime;
import net.ion.framework.schedule.Job;
import net.ion.framework.schedule.ScheduledRunnable;
import net.ion.framework.schedule.Scheduler;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

public class TestScheduleUtil extends TestCase {

	public void testInterface() throws Exception {
		AtTime atime = new AtTime("19,20,21,22 * * * * -1 2014-2020");
		
		Scheduler sche = new Scheduler("test", Executors.newCachedThreadPool()) ;
		sche.addJob(new Job("newjob", new ScheduledRunnable() {
			@Override
			public void run() {
				Debug.line(new Date()); 
			}
		}, atime));
		
		sche.start(); 
		
		
//		sche.removeJob("newjob");
		
		
		new InfinityThread().startNJoin(); 
	}
	
	public void testUse() throws Exception {
		AtTime atime = new AtTime("* * * * * * *");
		Scheduler sche = new Scheduler("test", Executors.newCachedThreadPool()) ;
		sche.addJob(new Job("newjob", new ScheduledRunnable() {
			@Override
			public void run() {
				Debug.line(new Date()); 
			}
		}, atime));
		
		sche.start(); 
		
		new InfinityThread().startNJoin(); 
	}
	
	
}
