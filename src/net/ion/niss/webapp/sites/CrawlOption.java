package net.ion.niss.webapp.sites;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.StringUtil;
import net.ion.icrawler.Site;
import net.ion.icrawler.scheduler.MaxLimitScheduler;
import net.ion.icrawler.scheduler.QueueScheduler;
import net.ion.icrawler.scheduler.Scheduler;

public class CrawlOption {

	private final String crawlId;
	private int maxPage = 50 ;
	private int sleepTime = 50 ;
	private String startPageUrl ;
	private String userId = "emanon";
	private String siteUrl = "" ;
	private String sid = "";
	
	public CrawlOption(){
		this.crawlId = new ObjectId().toString() ;
	}
	
	public static CrawlOption loadFrom(String jsonOption){
		if (StringUtil.isBlank(jsonOption)) return new CrawlOption() ;
		else {
			CrawlOption result = new CrawlOption() ;
			JsonObject json = JsonObject.fromString(jsonOption) ;
			
			result.maxPage(json.asInt("maxPage")) ;
			result.sleepTime(json.asInt("sleepTime")) ;
			result.startPageUrl(json.asString("startPageUrl")) ;
			result.createUserId(json.asString("userId")) ;
			
			return result ;
		}
	}
	
	public CrawlOption maxPage(int mpage){
		this.maxPage = Math.max(mpage, 1) ;
		return this ;
	}

	public CrawlOption sleepTime(int sleepTime){
		this.sleepTime = Math.max(sleepTime, 50) ;
		return this ;
	}
	

	public int maxPage(){
		return maxPage ;
	}
	
	public Scheduler scheduler() {
		return new MaxLimitScheduler(new QueueScheduler(), maxPage) ;
	}
	
	public CrawlOption startPageUrl(String startPageUrl){
		this.startPageUrl = startPageUrl ;
		return this ;
	}

	public String startPageUrl(){
		if (StringUtil.isBlank(startPageUrl)) this.startPageUrl = siteUrl ;
		return startPageUrl ;
	}
	
	public Site createSite() {
		return Site.create(startPageUrl()).sleepTime(sleepTime);
	}

	public String crawlId() {
		return crawlId;
	}
	
	public CrawlOption createUserId(String userId){
		this.userId = userId ;
		return this ;
	}

	public String createUserId() {
		return userId;
	}

	public String toJsonString() {
		return JsonObject.create().put("sleepTime", sleepTime).put("maxPage", maxPage).put("startPageUrl", startPageUrl).toString();
	}

	public String siteUrl() {
		return siteUrl;
	}

	public CrawlOption siteInfo(String sid, String siteUrl) {
		this.sid = sid ;
		this.siteUrl = siteUrl ;
		return this ;
	}

	public String siteId() {
		return sid;
	}

}
