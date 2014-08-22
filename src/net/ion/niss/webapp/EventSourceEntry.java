package net.ion.niss.webapp;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.CopyOnWriteArraySet;

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

	public Writer newWriter(String eventId) {
		return new EventSourceWriter(this, eventId);
	}

	public void sendTo(String eventId, String data) {
		EventSourceConnection conn = connMap.get(eventId) ;
		if (conn != null) conn.send(new EventSourceMessage(data)) ;
	}
}


class EventSourceWriter extends Writer{

	private String eventId;
	private EventSourceEntry ese;
	private StringBuilder buffer = new StringBuilder() ;

	public EventSourceWriter(EventSourceEntry ese, String eventId) {
		this.ese = ese ;
		this.eventId = eventId ;
	}

	@Override
	public synchronized void write(char[] cbuf, int off, int len) throws IOException {
		buffer.append(cbuf, off, len) ;
	}

	@Override
	public synchronized void flush() throws IOException {
		ese.sendTo(eventId, buffer.toString()) ;
		buffer = new StringBuilder() ;
	}

	@Override
	public void close() throws IOException {
		
	}
}

