package net.ion.bleujin.tika;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import rcc.h2tlib.parser.H2TParser;
import rcc.h2tlib.parser.HWPMeta;

public class TestFirstTika extends TestCase {

	public void testFromPDF() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.pdf"));
		printData(is);
	}

	public void testFromDOCX() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.docx"));
		printData(is);
	}

	public void testFromPPTX() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.pptx"));
		printData(is);
	}

	public void testFromXls() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.xls"));
		printData(is);
	}

	public void testFromText() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.txt"));
		printData(is);
	}

	public void testFromRTF() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.rtf"));
		printData(is);
	}


	public void testFromHwp() throws Exception {
		InputStream is = new BufferedInputStream(getClass().getResourceAsStream("sample.hwp"));
		HWPMeta meta = new HWPMeta();
	    H2TParser parser = new H2TParser();
	    parser.GetText(is, meta, System.out, 2) ;
	    
	    
	    Debug.line(meta, meta.getTitle(), meta.getSubject(), meta.getCreatetime(), meta.getKeyword(), meta.getComment(), meta.getVer());
	    
	    
	    
//	    boolean flg = parser.GetMeta(is, meta, 2);
//	    if (flg)
//	    {
//	        System.out.println("HWP 파일입니다.");
//	        System.out.println("title => " + meta.getTitle());
//	        System.out.println("subject => " + meta.getSubject());
//	        System.out.println("author => " + meta.getAuthor());
//	        System.out.println("createtime => " + meta.getCreatetime());
//	        System.out.println("keyword => " + meta.getKeyword());
//	        System.out.println("comment => " + meta.getComment());
//	        if(meta.getVer() == HWPVER.HML2) System.out.println("ver => HML2");
//	        if(meta.getVer() == HWPVER.HWP3) System.out.println("ver => HWP3");
//	        if(meta.getVer() == HWPVER.HWP5) System.out.println("ver => HWP5");
//	        
//	        parser.GetText(is, meta, System.out, 2) ;
//	    }
	}




	private void printData(InputStream is) throws IOException, SAXException, TikaException {
		Parser parser = new AutoDetectParser() ;
		StringWriter writer = new StringWriter() ;
		ContentHandler handler = new BodyContentHandler(writer) ;

		Metadata metadata = new Metadata();

		parser.parse(is, handler, metadata, new ParseContext());

		for (String name : metadata.names()) {
			Debug.line(name, metadata.get(name));
		}
		
		Debug.line(parser, writer);
		IOUtil.closeQuietly(is);
	}
}
