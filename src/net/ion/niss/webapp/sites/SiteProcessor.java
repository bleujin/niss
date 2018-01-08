package net.ion.niss.webapp.sites;

import java.sql.SQLException;
import java.util.List;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.rcraken.extend.Sequence;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.StringUtil;
import net.ion.icrawler.Page;
import net.ion.icrawler.Request;
import net.ion.icrawler.Site;
import net.ion.icrawler.Spider;
import net.ion.icrawler.processor.PageProcessor;
import net.ion.icrawler.selector.Link;

public class SiteProcessor implements PageProcessor {

	private String urlPattern;
	private ReadSession rsession ;
	private CrawlOption coption;

	private SiteProcessor(ReadSession rsession, String urlPattern, CrawlOption coption) {
		this.rsession = rsession ;
		this.urlPattern = urlPattern;
		this.coption = coption;
	}

	public Spider newSpider() {

		Site site = coption.createSite();
		return site.newSpider(this).addPipeline(new OutPipeline()).setScheduler(coption.scheduler()).setExitWhenComplete(true);
	}

	public static SiteProcessor create(ReadSession rsession, String siteUrl, CrawlOption coption) throws SQLException {
		if (StringUtil.isBlank(coption.startPageUrl())) {
			coption.startPageUrl(siteUrl);
		}
		String urlPattern = siteUrl + (StringUtil.endsWith(siteUrl, "/") ? "*" : "/*");
		urlPattern = (new StringBuilder("(")).append(urlPattern.replace(".", "\\.").replace("*", "[^\"'#]*")).append(")").toString();

		rsession.tran(wsession ->{
			wsession.pathBy("sites", coption.siteId(), coption.crawlId())
				.property("crawlid", coption.crawlId())
				.property("siteurl", siteUrl)
				.property("creuserid", coption.createUserId())
				.property("credate", System.currentTimeMillis())
				.property("options", coption.toJsonString()).merge();
		}) ;
		
		return new SiteProcessor(rsession, urlPattern, coption);
	}

	public void process(Page page) {

		Sequence pageseq = rsession.workspace().sequence("pageseq") ;
		Sequence linkseq = rsession.workspace().sequence("linkseq") ;
		final Request request = page.getRequest();
		final List<Link> targets = page.getHtml().links().targets();
		page.addTargets(targets);
		
		long pageno = pageseq.incrementAndGet();
		rsession.tran(wsession -> {
			wsession.pathBy("sites", coption.siteId(), coption.crawlId(), "" + pageno)
				.property("url", request.getUrl())
				.property("urlhash", request.getUrl().hashCode())
				.property("method", request.getMethod() + "")
				.property("scode", page.getStatusCode() + "")
				.property("queryparam", StringUtil.toString(JsonObject.fromObject(request.getQueryParameter())))
				.property("title", page.getHtml().xpath("//title/text()").toString())
				.property("html", page.getHtml().toString())
				.property("content", StringUtil.toString(page.getHtml().smartContent())).merge();
			
			for (Link link : targets) {
				wsession.pathBy("sites", coption.siteId(), coption.crawlId(), "" + pageno, "" + linkseq.incrementAndGet())
//					.property("fromurl", request.getUrl())
//					.property("fromurlhash", request.getUrl().hashCode())
					.property("anchor", link.anchor())
					.property("referer", link.target())
					.property("referhash", link.target().hashCode()).merge();
			}

		}) ;
	}

}
