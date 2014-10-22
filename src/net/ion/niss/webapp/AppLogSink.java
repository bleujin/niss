package net.ion.niss.webapp ;

import static net.ion.nradon.helpers.Hex.toHex;

import java.io.Flushable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Date;

import org.apache.log4j.Logger;

import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.util.WebUtil;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.WebSocketConnection;
import net.ion.nradon.handler.logging.LogSink;
import scala.collection.mutable.StringBuilder;

public class AppLogSink implements LogSink {

	// TODO: Offload filesystem IO to another thread

	protected final Appendable out;
	protected final String[] dataValuesToLog;

	protected final char lineSeparator = '\n';

	protected boolean trouble = false;
	private Logger logger = Logger.getLogger(AppLogSink.class) ;

	public AppLogSink(Appendable out, String... dataValuesToLog) {
		this.out = out;
		this.dataValuesToLog = dataValuesToLog;
		try {
			formatHeader(out);
			flush();
		} catch (IOException e) {
			trouble = true;
			panic(e);
		}
	}

	public AppLogSink(String... dataValuesToLog) {
		this(System.out, dataValuesToLog);
	}

	public void httpStart(HttpRequest request) {
		request.data("_etime", System.currentTimeMillis()) ;
//		custom(request, "HTTP-START:" + request.method(), null);
	}

	public void httpEnd(HttpRequest request, HttpResponse response) {
		long etime = (Long)ObjectUtil.coalesce(request.data("_etime"), 0L) ;
		String uri = request.uri() ;
		if (WebUtil.isStaticResource(uri)) return ;
		custom(request, "HTTP-END " + response.status(), "" + (System.currentTimeMillis() - etime)); // TODO: Time request
	}

	public void webSocketConnectionOpen(WebSocketConnection connection) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-OPEN", null);
	}

	public void webSocketConnectionClose(WebSocketConnection connection) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-CLOSE", null);
	}

	public void webSocketInboundData(WebSocketConnection connection, String data) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-IN-STRING", data);
	}

	public void webSocketInboundData(WebSocketConnection connection, byte[] data) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-IN-HEX", toHex(data));
	}

	public void webSocketInboundPing(WebSocketConnection connection, byte[] message) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-IN-PONG", toHex(message));
	}

	public void webSocketInboundPong(WebSocketConnection connection, byte[] message) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-IN-PONG", toHex(message));
	}

	public void webSocketOutboundData(WebSocketConnection connection, String data) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-OUT-STRING", data);
	}

	public void webSocketOutboundData(WebSocketConnection connection, byte[] data) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-OUT-HEX", toHex(data));
	}

	public void webSocketOutboundPing(WebSocketConnection connection, byte[] message) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-OUT-PING", toHex(message));
	}

	public void webSocketOutboundPong(WebSocketConnection connection, byte[] message) {
		custom(connection.httpRequest(), "WEB-SOCKET-" + connection.version() + "-OUT-PING", toHex(message));
	}

	public void error(HttpRequest request, Throwable error) {
		custom(request, "ERROR-OPEN", error.toString());
	}

	public void custom(HttpRequest request, String action, String data) {
		if (trouble) {
			return;
		}
		try {
			formatLogEntry(out, request, action, data);
			flush();
		} catch (IOException e) {
			trouble = true;
			panic(e);
		}
	}

	public void eventSourceConnectionOpen(EventSourceConnection connection) {
		custom(connection.httpRequest(), "EVENT-SOURCE-OPEN", null);
	}

	public void eventSourceConnectionClose(EventSourceConnection connection) {
		custom(connection.httpRequest(), "EVENT-SOURCE-CLOSE", null);
	}

	public void eventSourceOutboundData(EventSourceConnection connection, String data) {
		// custom(connection.httpRequest(), "EVENT-SOURCE-OUT", data);
	}

	protected void flush() throws IOException {
		if (out instanceof Flushable) {
			Flushable flushable = (Flushable) out;
			flushable.flush();
		}
	}

	protected void panic(IOException exception) {
		// If we can't log, be rude!
		exception.printStackTrace();
	}

	protected Appendable formatLogEntry(Appendable out, HttpRequest request, String action, String data) throws IOException {
		long cumulativeTimeOfRequest = cumulativeTimeOfRequest(request);
		Date now = new Date();
		StringBuilder sb = new StringBuilder() ;
		formatValue(sb, now);
		formatValue(sb, now.getTime());
		formatValue(sb, cumulativeTimeOfRequest);
		formatValue(sb, request.id());
		formatValue(sb, address(request.remoteAddress()));
		formatValue(sb, action);
		formatValue(sb, request.uri());
		formatValue(sb, request.method()) ;
		formatValue(sb, data);
		for (String key : dataValuesToLog) {
			formatValue(sb, request.data(key));
		}
		sb.append(lineSeparator) ;
		
		logger.info(sb);
		
		return out.append(sb);
	}

	protected Appendable formatHeader(Appendable out) throws IOException {
		StringBuilder sb = new StringBuilder() ;
		sb.append("#Log started at ").append(new Date().toString()).append(" (").append(String.valueOf(System.currentTimeMillis())).append(")").append(lineSeparator).append('#');
		formatValue(sb, "Date");
		formatValue(sb, "Timestamp");
		formatValue(sb, "MillsSinceRequestStart");
		formatValue(sb, "RequestID");
		formatValue(sb, "RemoteHost");
		formatValue(sb, "Action");
		formatValue(sb, "Path");
		formatValue(sb, "Method") ;
		formatValue(sb, "Payload");
		for (String key : dataValuesToLog) {
			formatValue(sb, "Data:" + key);
		}
		sb.append(lineSeparator);
		
		
		
		return out.append(sb) ;
	}

	private long cumulativeTimeOfRequest(HttpRequest request) {
		return System.currentTimeMillis() - request.timestamp();
	}

	protected StringBuilder formatValue(StringBuilder sb, Object value) throws IOException {
		if (value == null) {
			return sb.append("-\t");
		}
		String string = value.toString().trim();
		if (StringUtil.isEmpty(string)) {
			return sb.append("-\t");
		}
		return sb.append(string).append('\t');
	}

	protected String address(SocketAddress address) {
		return address.toString();
	}
}
