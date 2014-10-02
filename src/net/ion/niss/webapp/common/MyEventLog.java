package net.ion.niss.webapp.common;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReadWriteLock;

import net.ion.framework.util.ObjectUtil;
import net.ion.niss.webapp.util.CopyOnWriteMap;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.EventSourceMessage;

public class MyEventLog implements Appendable{

	private Appendable out;
	private CopyOnWriteArraySet<EventSourceConnection> conns = new CopyOnWriteArraySet<EventSourceConnection>() ;

	public MyEventLog(Appendable out) {
		this.out = out ;
	}

	public static MyEventLog create() {
		return new MyEventLog(System.out);
	}

	public static MyEventLog create(Appendable out) {
		return new MyEventLog(out);
	}

	@Override
	public Appendable append(CharSequence csq) throws IOException {
		out.append(csq) ;
		sendEventSource(csq);
		return this;
	}

	private void sendEventSource(CharSequence csq) {
		for (EventSourceConnection econn : conns) {
			econn.send(new EventSourceMessage(ObjectUtil.toString(csq))) ;
		}
	}

	@Override
	public Appendable append(CharSequence csq, int start, int end) throws IOException {
		return append(csq.subSequence(start, end)) ;
	}

	@Override
	public Appendable append(char c) throws IOException {
		out.append(c) ;
		return this;
	}

	
	public void onClose(EventSourceConnection econn) {
		conns.remove(econn) ;
	}

	public void onOpen(EventSourceConnection econn) {
		conns.add(econn) ;
	}



}
