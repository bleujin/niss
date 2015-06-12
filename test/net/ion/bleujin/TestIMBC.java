package net.ion.bleujin;

import java.nio.charset.Charset;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

public class TestIMBC extends TestCase{

	public void testURL() throws Exception {
//		URL url = new URL("http://imnews.imbc.com/news/2015/politic/index.html") ;
//		InputStream input = url.openStream() ;
//		Debug.line(IOUtil.toStringWithClose(input, "EUC-KR")) ;
		String s = "&nbsp;&nbsp;" ;
		byte[] bytes = s.getBytes(Charset.forName("UTF-8")) ;
		Debug.line(bytes);
		
	}
	
}
