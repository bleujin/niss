package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.bleujin.rcraken.ReadSession;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.util.CopyOnWriteMap;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceMessage;

public class EventSourceEntry implements Closeable{

	public final static String EntryName = "esentry" ;
	private CopyOnWriteMap<Object, EventSourceConnection> connMap = new CopyOnWriteMap<Object, EventSourceConnection>() ;
	private Map<Object, CountDownLatch> latchs = MapUtil.newMap() ;
	
	private EventSourceEntry(){
	}
	
	@Override
	public void close() throws IOException {
		
	}

	public static EventSourceEntry create() {
		return new EventSourceEntry();
	}

	public EventSourceEntry onOpen(EventSourceConnection conn) {
		Object eventId = conn.data("id");
		CountDownLatch latch = latchs.get(eventId) ;
		if (latch != null) latch.countDown(); 
		
		connMap.put(eventId, conn) ;
		return this ;
	}

	public EventSourceEntry onClose(EventSourceConnection conn) {
		Object eventId = conn.data("id");

		latchs.remove(eventId) ;
		connMap.remove(eventId) ;
		return this ;
	}

	public Writer newWriter(REntry rentry, String lid, String eventId) throws IOException {
		return new EventSourceWriter(this, rentry, lid, eventId);
	}

	public void sendTo(String eventId, String data) {
		EventSourceConnection conn = connMap.get(eventId) ;
		if (conn != null) conn.send(new EventSourceMessage(data)) ;
		else System.err.println("not connected " + eventId + " : " + data);
	}

	public void closeEvent(String eventId) {
		EventSourceConnection econn = connMap.get(eventId) ;
		if (econn != null){
			econn.send(new EventSourceMessage(eventId)) ;
			econn.close() ;
		}
		
	}
	
	public CountDownLatch createEvent(String eventId) {
		CountDownLatch latch = new CountDownLatch(1) ;
		latchs.put(eventId, latch) ;
		return latch ;
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
		
		rsession.tran(wsession -> {
			FileInputStream fis = new FileInputStream(targetFile);
			
			wsession.pathBy("/events/loaders/" + EventSourceWriter.this.eventId)
				.property("status", "end")
				.refTo("loader", "/loaders/" + EventSourceWriter.this.lid)
				.property("content", fis).merge();
			IOUtil.close(fis);
			ese.closeEvent(EventSourceWriter.this.eventId) ;
		}) ;
	}
}

