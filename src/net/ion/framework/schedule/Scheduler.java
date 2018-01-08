package net.ion.framework.schedule;

import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import net.ion.framework.util.StackTrace;

public class Scheduler extends Thread {
	private String name = null;

	private Hashtable<String, Job> jobs = new Hashtable<String, Job>();
	private Logger logger = Logger.getLogger(Scheduler.class);
	private ExecutorService workers ; 
	
	public Scheduler(String name, ExecutorService workers) {
		super(name);
		this.name = name;
		this.workers = workers ;
		this.setDaemon(true);
	}

	public synchronized void addJob(Job job) {
		this.jobs.put(job.getName(), job);
		logger.info("job added.:" + job);
	}

	public void removeJob(Job job) {
		removeJob(job.getName());
	}

	public synchronized void removeJob(String jobName) {
		Job job = getJob(jobName);
		if (job == null) return ;
		
		logger.info("job removed.:" + job);
		this.jobs.remove(jobName);
	}

	public Job getJob(String jobName) {
		return this.jobs.get(jobName);
	}

	public Job[] getJobs() {
		return this.jobs.values().toArray(new Job[0]);
	}

	public String[] getJobNames() {
		return this.jobs.keySet().toArray(new String[0]);
	}

	public String toString() {
		String[] jobNames = getJobNames();

		StringBuffer buff = new StringBuffer();

		buff.append("{name=" + this.name + ",jobs={");

		for (int i = 0; i < jobNames.length; ++i) {
			buff.append(jobNames[i]);
			if (i + 1 != jobNames.length) {
				buff.append(",");
			}
		}
		buff.append("}}");

		return buff.toString();
	}

	public void run() {
		logger.info("Scheduler started. :" + this.toString());

		int minute = 0;
		Calendar cal = null;

		try {
			while (!isInterrupted()) {
				cal = Calendar.getInstance();

				if (minute != cal.get(Calendar.MINUTE)) {
					minute = cal.get(Calendar.MINUTE);

					synchronized (this) {
						Enumeration<Job> je = jobs.elements();
						while (je.hasMoreElements()) {
							Job j = je.nextElement();
							AtTime at = j.getAtTime();
							Runnable r = j.getScheduledRunnable();

							if (at.match()) {
								try {
									workers.submit(r) ;
								} catch (Throwable t) {
									logger.warn(j.toString() + ":" + StackTrace.trace(t));
								}
							}

							if (at.wasExpired()) {
								removeJob(j);
							}
						}
					}
				}
				sleep(1000); // 1 sec
			}
		} catch (Exception e) {
		}

		logger.info("Scheduler stopped. :" + this.toString());
	}

	public void start() {
		super.start();
	}

	public void interrupt() {
		super.interrupt();
	}

}
