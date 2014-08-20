package net.ion.niss.webapp.common;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

public class SourceStreamOut implements StreamingOutput {

	private Source source;
	private boolean indent;

	public SourceStreamOut(Source source, boolean indent) {
		this.source = source;
		this.indent = indent;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		try {
			StreamResult xmlOutput = new StreamResult(output);
			Transformer transformer = SAXTransformerFactory.newInstance().newTransformer();
			if (indent) {
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			}

			// transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.transform(source, xmlOutput);
		} catch (TransformerException e) {
			throw new IOException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new IOException(e);
		}
	}

}