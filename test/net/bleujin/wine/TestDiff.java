package net.bleujin.wine;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import net.ion.framework.db.bean.handlers.CSVReader;
import net.ion.framework.util.Debug;

public class TestDiff extends TestCase {

	
	public void testBody() throws Exception {
		WineGroup wines = makeWineInfos();

		Wine w1 = wines.findByKoName("트루아젤 리제르바 까베르네 소비뇽") ;
		assertEquals(5, w1.body());
	}
	
	public void testLoad() throws Exception {
		WineGroup wines = makeWineInfos();

		// 트루아젤 리제르바 까베르네 소비뇽, 트루아젤, 보샤뗄 소비뇽, 보샤뗄 메를로, 보샤뗄 까베르네 소비뇽, 비르지니 드 발랑드로, 빈도로 프리미티보 디 만두리아, 비나 코보스, 브라마레 레본 말벡
		Wine w1 = wines.findByKoName("트루아젤 리제르바 까베르네 소비뇽") ;
		Wine w2 = wines.findByKoName("보샤뗄 소비뇽") ;
		
		Debug.line(w1, w2, w1.compareValue(w2, "Body"));
	}
	
	public void testSort() throws Exception {
		WineGroup wines = makeWineInfos();
		
		Wine w1 = wines.findByKoName("트루아젤 리제르바 까베르네 소비뇽") ;
		WeightContext wcontext = WeightContext.DEFAULT ;
		List<Wine> result = wines.similary(w1, new String[]{"BODY", "TANNIN"}, wcontext) ;
		for (Wine wine : result) {
			Debug.line(wine.name(), wine.body(), wine.tannin());
		}
	}
	
	public void testWeight() throws Exception {
		WineGroup wines = makeWineInfos();
		
		Wine w1 = wines.findByKoName("트루아젤 리제르바 까베르네 소비뇽") ;
		WeightContext wcontext = new WeightContext() ;
		wcontext.put("tannin", 2) ;
		List<Wine> result = wines.similary(w1, new String[]{"BODY", "TANNIN"}, wcontext) ;
		for (Wine wine : result) {
			Debug.line(wine.name(), wine.body(), wine.tannin());
		}
	}

	private WineGroup makeWineInfos() throws FileNotFoundException, EOFException, IOException {
		FileReader freader = new FileReader(new File("./resource/wine_all/wine_sample")) ;
		CSVReader reader = new CSVReader(freader) ;
		String[] titles = reader.getLine() ;
		reader.skipToNextLine();
		
		WineGroup wg = new WineGroup() ;
		try {
			while(true) {
				String[] lines = reader.getLine() ;
				if (lines == null || lines.length == 9) break ;
				
				Wine w = new Wine() ;
				for(int i=0 ; i<lines.length ; i++) {
					w.put(titles[i], lines[i]) ;
				}
				wg.add(w) ;
			}
		} catch(EOFException e) {}
		return wg;
	}
}
