package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.concurrent.CopyOnWriteArraySet;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.niss.webapp.util.CopyOnWriteMap;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceMessage;

public class EventSourceEntry implements Closeable{

	public final static String EntryName = "esentry" ;
	private CopyOnWriteMap<Object, EventSourceConnection> connMap = new CopyOnWriteMap<Object, EventSourceConnection>() ;
	
	private EventSourceEntry(){
	}
	
	@Override
	public void close() throws IOException {
		
	}

	public static EventSourceEntry create() {
		return new EventSourceEntry();
	}

	public EventSourceEntry onOpen(EventSourceConnection conn) {
		connMap.put(conn.data("id"), conn) ;
		return this ;
	}

	public EventSourceEntry onClose(EventSourceConnection conn) {
		connMap.remove(conn.data("id")) ;
		return this ;
	}

	public Writer newWriter(REntry rentry, String lid, String eventId) throws IOException {
		return new EventSourceWriter(this, rentry, lid, eventId);
	}

	public void sendTo(String eventId, String data) {
		EventSourceConnection conn = connMap.get(eventId) ;
		if (conn != null) conn.send(new EventSourceMessage(data)) ;
	}

	public void closeEvent(String eventId) {
		EventSourceConnection econn = connMap.get(eventId) ;
		if (econn != null){
			econn.send(new EventSourceMessage(eventId)) ;
			econn.close() ;
		}
		
	}
}


class EventSourceWriter extends Writer{

	private String eventId;
	private EventSourceEntry ese;
	private StringBuilder buffer = new StringBuilder() ;
	private ReadSession rsession;
	private String lid;
	private File targetFile;
	private FileWriter fwriter ;

	public EventSourceWriter(EventSourceEntry ese, REntry rentry, String lid, String eventId) throws IOException {
		this.ese = ese ;
		this.lid = lid ;
		this.eventId = eventId ;
		this.rsession = rentry.login();
		this.targetFile = File.createTempFile("load", eventId);
		this.fwriter = new FileWriter(targetFile) ;
 	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		buffer.append(cbuf, off, len) ;
	}

	@Override
	public void flush() throws IOException {
		ese.sendTo(eventId, buffer.toString()) ;
		fwriter.write(buffer.toString());
		fwriter.flush(); 
		buffer.setLength(0);
	}

	@Override
	public void close() throws IOException {
		flush(); 
//		IOUtil.closeQuietly(fwriter);
		
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				FileInputStream fis = new FileInputStream(targetFile);
				
				wsession.pathBy("/events/loaders/" + EventSourceWriter.this.eventId)
					.property("status", "end")
					.refTo("loader", "/loaders/" + EventSourceWriter.this.lid)
					.blob("content", fis) ;
				IOUtil.close(fis);
				ese.closeEvent(EventSourceWriter.this.eventId) ;
				return null;
			}
		}) ;
	}
}

