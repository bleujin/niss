package net.ion.niss.webapp.sites;

import java.sql.SQLException;
import java.util.List;

import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.db.procedure.IUserProcedures;
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
	private IDBController dc;
	private CrawlOption coption;

	private SiteProcessor(IDBController dc, String urlPattern, CrawlOption coption) {
		this.dc = dc;
		this.urlPattern = urlPattern;
		this.coption = coption;
	}

	public Spider newSpider() {

		Site site = coption.createSite();
		return site.newSpider(this).addPipeline(new OutPipeline()).setScheduler(coption.scheduler()).setExitWhenComplete(true);
	}

	public static SiteProcessor create(IDBController dc, String siteUrl, CrawlOption coption) throws SQLException {
		if (StringUtil.isBlank(coption.startPageUrl())) {
			coption.startPageUrl(siteUrl);
		}
		String urlPattern = siteUrl + (StringUtil.endsWith(siteUrl, "/") ? "*" : "/*");
		urlPattern = (new StringBuilder("(")).append(urlPattern.replace(".", "\\.").replace("*", "[^\"'#]*")).append(")").toString();

		dc.createUserProcedure("CRAWL@createWith(?,?,?,?)").addParam(coption.crawlId()).addParam(siteUrl).addParam(coption.createUserId()).addParam(coption.toJsonString()).execUpdate();

		return new SiteProcessor(dc, urlPattern, coption);
	}

	public void process(Page page) {

		IUserProcedures pageUPTS = dc.createUserProcedures("crawl@perPage");
		IUserProcedureBatch upts = dc.createUserProcedureBatch("CRAWL@toLinkWith(?,?,?,?)");
		final List<Link> targets = page.getHtml().links().targets();
		for (Link link : targets) {
			upts.addBatchParam(0, coption.crawlId());
			upts.addBatchParam(1, page.getUrl().toString());
			upts.addBatchParam(2, link.anchor());
			upts.addBatchParam(3, link.target());
		}
		pageUPTS.add(upts);

		List<Link> requests = page.getHtml().links().regex(urlPattern).targets();
		page.addTargets(requests);
		page.putField("title", page.getHtml().xpath("//title"));
		page.putField("html", page.getHtml().toString());
		page.putField("content", page.getHtml().smartContent());

		final Request request = page.getRequest();
		pageUPTS.add(dc.createUserProcedure("CRAWL@addPageWith(?,?,?,?,?, ?,?,?)").addParam(coption.crawlId()).addParam(request.getUrl()).addParam(request.getMethod().toString()).addParam(page.getStatusCode() + "").addParam(StringUtil.toString(JsonObject.fromObject(request.getQueryParameter())))
				.addParam(page.getHtml().xpath("//title/text()").toString()).addParam(page.getHtml().toString()).addParam(StringUtil.toString(page.getHtml().smartContent())));

		try {
			pageUPTS.execUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

}
