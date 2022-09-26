package net.ion.niss.webapp.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import net.bleujin.searcher.common.ReadDocument;
import net.bleujin.searcher.search.SearchResponse;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;
import net.ion.radon.util.csv.CsvWriter;

public class CSVStreamOut implements StreamingOutput {

	private SearchResponse sresponse;
	public CSVStreamOut(SearchResponse sresponse) {
		this.sresponse = sresponse ;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		CsvWriter cwriter = new CsvWriter(new BufferedWriter(new OutputStreamWriter(output))) ;

		Set<String> nameSet = SetUtil.newOrdereddSet() ;
		for(ReadDocument doc : sresponse.getDocument()) {
			nameSet.addAll(ListUtil.toList(doc.fieldNames())) ;
		}
		
		cwriter.writeLine(nameSet.toArray(new String[0]));
		for(ReadDocument doc : sresponse.getDocument()) {
			for (String fname : nameSet) {
				String value = doc.asString(fname);
				cwriter.writeField(value == null ? "" : value);
			}
			cwriter.endBlock(); 
		}
		cwriter.flush(); 
	}
	
}
