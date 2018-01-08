package net.bleujin.wine.crawl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import junit.framework.TestCase;
import net.ion.framework.db.bean.handlers.CSVWriter;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;
import net.ion.icrawler.Page;
import net.ion.icrawler.ResultItems;
import net.ion.icrawler.Site;
import net.ion.icrawler.Spider;
import net.ion.icrawler.Task;
import net.ion.icrawler.pipeline.Pipeline;
import net.ion.icrawler.processor.PageProcessor;
import net.ion.icrawler.scheduler.MaxLimitScheduler;
import net.ion.icrawler.scheduler.QueueScheduler;

public class TestCrawl extends TestCase {

	public void testRegular() throws Exception {
		Pattern pattern = Pattern.compile("http://www.wine21.com/13_search/wine_view.html\\?Idx=(\\d+)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher("http://www.wine21.com/13_search/wine_view.html?Idx=162442");

		while (matcher.find()) {
			Debug.line(matcher.group(), matcher.group(1));
		}
	}

	public void testURLEncode() {
		Debug.line(URLEncoder.encode("|"));
	}

	public void testCollectPage() throws Exception {
		Site site = Site.create("http://www.wine21.com").sleepTime(50);

		Spider spider = site.newSpider(new CollectDetailProcessor()).startUrls("http://www.wine21.com/13_search/wine_list.html").scheduler(new MaxLimitScheduler(new QueueScheduler(), 500));

		spider.addPipeline(new DebugPipeline()).run();
	}

	public void testListPage() throws Exception {
		File cdir = new File("./resource/wine/");
		Collection files = FileUtil.listFiles(cdir, TrueFileFilter.TRUE, TrueFileFilter.TRUE);

		files.iterator().forEachRemaining(new Consumer<File>() {
			public void accept(File file) {
				Debug.line(file.getName());
			}
		});
	}

	public void testParsePage() throws Exception {
		Site site = Site.create("http://www.wine21.com").sleepTime(60);

		Spider spider = site.newSpider(new FileDetailPageProcessor()).startUrls("http://www.wine21.com/13_search/wine_list.html").scheduler(new MaxLimitScheduler(new QueueScheduler(), 16000));

		File file = new File("./resource/wine_all/wine.xls");
		file.getParentFile().mkdirs();
		CSVWriter writer = new CSVWriter(file, "UTF-8");
		writer.writeHeader(new String[] { "thumb_url", "name_ko", "name_en", "nation", "badge", "price", "생산자", "생산지역", "주품종", "주 종", "용 도", "알코올", "음용온도", "향", "당 도", "산 도", "바 디", "타 닌", "음 식", "소비자가", "수입사", "maker_note" });

		spider.addPipeline(new ParsePagePipeline(writer)).run();
		IOUtil.close(writer);
	}

}

class FileDetailPageProcessor implements PageProcessor {

	public void process(final Page page) {
		if (page.getRequest().getUrl().endsWith("html")) {
			try {
				File[] files = new File("./resource/wine/").listFiles() ;
				for (int i = 0; i < files.length; i++) {
					File file = files[i] ;
					List<String> address = FileUtil.readLines(file);
					for (String add : address) {
						page.addTarget(add);
					}
				} 
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} else {
			Debug.line(page.getRequest().getUrl());

			Document doc = page.getHtml().getDocument();
			Elements div = doc.select("div.column_block_1");

			page.putField("thumb_url", div.select("div.thumb").html());
			String code = div.select("span.code").html().replace("CODE", "").trim();
			Debug.debug("code", code);

			page.putField("name_ko", div.select("h4.name_ko").html());
			page.putField("name_en", div.select("div.name_en").html());
			page.putField("nation", div.select("div.price img").attr("alt"));
			page.putField("badge", div.select("div.price em.badge").html());
			page.putField("price", div.select("span.price").html());

			Elements stitle = doc.select("div.wine_info dl").select("dt");
			stitle.listIterator().forEachRemaining(new Consumer<Element>() {
				public void accept(Element e) {
					String[] range = new String[] { "당 도", "산 도", "바 디", "타 닌" };
					if (ArrayUtil.contains(range, e.text())) {
						page.putField(e.text(), e.nextElementSibling().select("img").attr("alt"));
					} else {
						page.putField(e.text(), e.nextElementSibling().text());
					}

				}
			});

			String makerNote = doc.select("div.column_detail3 div.item dl span").text();
			if (StringUtil.isNotBlank(makerNote)) {
				String[] mns = StringUtil.split(makerNote, " ");
				StringBuilder mnote = new StringBuilder();
				for (String mn : mns) {
					mnote.append(mn + ":" + getMakerNote(code, mn) + "\r\n");
				}

				page.putField("maker_note", mnote.toString());
			}
		}

	}

	private String getMakerNote(String code, String mn) {
		try {
			InputStream input = new URL("http://www.wine21.com/13_search/load_wine_makernote.html?WineIdx=" + code + "&VINTAGE=" + mn).openStream();
			String note = IOUtil.toStringWithClose(input);
			return note;
		} catch (IOException e) {
			return "";
		}
	}
}

class ParsePagePipeline implements Pipeline {

	private CSVWriter writer;

	public ParsePagePipeline(CSVWriter writer) {
		this.writer = writer;
	}

	@Override
	public void process(ResultItems ritems, Task task) {
		Map<String, String> fields = MapUtil.newMap();
		for (Entry<String, Object> entry : ritems.getAll().entrySet()) {
			fields.put(entry.getKey(), ((String) entry.getValue()).trim());
		}

		String[] datas = new String[] { fields.get("thumb_url"), fields.get("name_ko"), fields.get("name_en"), fields.get("nation"), fields.get("badge"), fields.get("price"), fields.get("생산자"), fields.get("생산지역"), fields.get("주품종"), fields.get("주 종"), fields.get("용 도"), fields.get("알코올"),
				fields.get("음용온도"), fields.get("향"), fields.get("당 도"), fields.get("산 도"), fields.get("바 디"), fields.get("타 닌"), fields.get("음 식"), fields.get("소비자가"), fields.get("수입사"), fields.get("maker_note") };
		try {
			writer.writeData(datas);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class CollectDetailProcessor implements PageProcessor {
	public CollectDetailProcessor() {
	}

	public void process(Page page) {

		if (page.getRequest().getUrl().endsWith("html")) {
			int maxpage = 155;
			for (int p = 1; p <= maxpage; p++) {
				page.addTarget("http://www.wine21.com/13_search/wine_list.html?Page=" + p + "&PageSize=100&shKeyword=&shOrder=CREATE_DATE%7CDESC");
			}
		} else {
			Debug.line(page.getRequest().getUrl());

			Document doc = page.getHtml().getDocument();
			Elements links = doc.select("a[href]");

			StringBuilder sb = new StringBuilder();
			for (Element elink : links) {
				String href = elink.attr("abs:href");
				if (StringUtil.isNotBlank(href) && StringUtil.isNotBlank(elink.ownText()) && href.endsWith("#view")) {
					sb.append(String.format("http://www.wine21.com/13_search/wine_view.html?Idx=%s\r\n", elink.attr("idx")));
				}
			}
			page.putField("address", sb.toString());
			page.putField("pageindex", StringUtil.substringBetween(page.getRequest().getUrl(), "?Page=", "&PageSize="));
		}

	}
}

class DebugPipeline implements Pipeline {
	private File cdir = new File("./resource/wine/");

	public void process(ResultItems ritems, Task task) {
		try {
			if (!cdir.exists())
				FileUtil.forceMkdir(cdir);
			String fileName = String.format("pageaddress_%s.txt", ritems.asString("pageindex"));

			if (StringUtil.isBlank(fileName))
				return;

			String content = ritems.asString("address");
			IOUtil.copyNClose(new StringReader(content), new FileWriter(new File(cdir, fileName)));
			// Debug.line(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}