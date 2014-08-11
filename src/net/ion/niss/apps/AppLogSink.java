package net.ion.niss.apps ;

import static net.ion.nradon.helpers.Hex.toHex;

import java.io.Flushable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.Date;

import net.ion.framework.util.StringUtil;
import net.ion.nradon.EventSourceConnection;
import net.ion.nradon.HttpRequest;
import net.ion.nradon.HttpResponse;
import net.ion.nradon.WebSocketConnection;
import net.ion.nradon.handler.logging.LogSink;

public class AppLogSink implements LogSink {

	// TODO: Offload filesystem IO to another thread

	protected final Appendable out;
	protected final String[] dataValuesToLog;

	protected final String lineSeparator = System.getProperty("line.separator", "\n");

	protected boolean trouble = false;

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
		custom(request, "HTTP-START:" + request.method(), null);
	}

	public void httpEnd(HttpRequest request, HttpResponse response) {
//		custom(request, "HTTP-END", null); // TODO: Time request
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
		custom(connection.httpRequest(), "EVENT-SOURCE-OUT", data);
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
		formatValue(out, now);
		formatValue(out, now.getTime());
		formatValue(out, cumulativeTimeOfRequest);
		formatValue(out, request.id());
		formatValue(out, address(request.remoteAddress()));
		formatValue(out, action);
		formatValue(out, request.uri());
		formatValue(out, data);
		for (String key : dataValuesToLog) {
			formatValue(out, request.data(key));
		}
		return out.append(lineSeparator);
	}

	protected Appendable formatHeader(Appendable out) throws IOException {
		out.append("#Log started at ").append(new Date().toString()).append(" (").append(String.valueOf(System.currentTimeMillis())).append(")").append(lineSeparator).append('#');
		formatValue(out, "Date");
		formatValue(out, "Timestamp");
		formatValue(out, "MillsSinceRequestStart");
		formatValue(out, "RequestID");
		formatValue(out, "RemoteHost");
		formatValue(out, "Action");
		formatValue(out, "Path");
		formatValue(out, "Payload");
		for (String key : dataValuesToLog) {
			formatValue(out, "Data:" + key);
		}
		return out.append(lineSeparator);
	}

	private long cumulativeTimeOfRequest(HttpRequest request) {
		return System.currentTimeMillis() - request.timestamp();
	}

	protected Appendable formatValue(Appendable out, Object value) throws IOException {
		if (value == null) {
			return out.append("-\t");
		}
		String string = value.toString().trim();
		if (StringUtil.isEmpty(string)) {
			return out.append("-\t");
		}
		return out.append(string).append('\t');
	}

	protected String address(SocketAddress address) {
		return address.toString();
	}
}
