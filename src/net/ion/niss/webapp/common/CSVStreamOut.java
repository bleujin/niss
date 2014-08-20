package net.ion.niss.webapp.common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchResponse;
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
			nameSet.addAll(ListUtil.toList(doc.getFieldNames())) ;
		}
		
		cwriter.writeLine(nameSet.toArray(new String[0]));
		for(ReadDocument doc : sresponse.getDocument()) {
			for (String fname : nameSet) {
				String value = doc.get(fname);
				cwriter.writeField(value == null ? "" : value);
			}
			cwriter.endBlock(); 
		}
		cwriter.flush(); 
	}
	
}
