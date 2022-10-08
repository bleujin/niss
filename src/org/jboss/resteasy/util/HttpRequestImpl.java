package org.jboss.resteasy.util;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

public abstract class HttpRequestImpl implements HttpRequest {
	protected HttpHeaders httpHeaders;
	protected InputStream inputStream;
	protected UriInfo uri;
	protected String httpMethod;
	protected String preProcessedPath;
	protected MultivaluedMap<String, String> formParameters;
	protected MultivaluedMap<String, String> decodedFormParameters;

	public HttpRequestImpl(InputStream inputStream, HttpHeaders httpHeaders, String httpMethod, UriInfo uri) {
		this.inputStream = inputStream;
		this.httpHeaders = httpHeaders;
		this.httpMethod = httpMethod;
		this.uri = uri;
		this.preProcessedPath = uri.getPath(false);
	}

	public HttpHeaders getHttpHeaders() {
		return this.httpHeaders;
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public UriInfo getUri() {
		return this.uri;
	}

	public String getHttpMethod() {
		return this.httpMethod;
	}

	public String getPreprocessedPath() {
		return this.preProcessedPath;
	}

	public void setPreprocessedPath(String path) {
		this.preProcessedPath = path;
	}

	public MultivaluedMap<String, String> getFormParameters() {
		if (this.formParameters != null) {
			return this.formParameters;
		} else {
			MediaType mediaType = this.getHttpHeaders().getMediaType();
			if (mediaType != null && mediaType.isCompatible(MediaType.valueOf("application/json"))) {
				this.formParameters = new MultivaluedMapImpl() ;
				
				return this.formParameters ;
			} else if (mediaType != null && !mediaType.isCompatible(MediaType.valueOf("application/x-www-form-urlencoded"))) {
				throw new IllegalArgumentException("Request media type is not application/x-www-form-urlencoded");
			} else {
				try {
					this.formParameters = FormUrlEncodedProvider.parseForm(this.getInputStream());
				} catch (IOException var3) {
					throw new RuntimeException(var3);
				}

				return this.formParameters;
			}
		}
	}

	public MultivaluedMap<String, String> getDecodedFormParameters() {
		if (this.decodedFormParameters != null) {
			return this.decodedFormParameters;
		} else {
			this.decodedFormParameters = Encode.decode(this.getFormParameters());
			return this.decodedFormParameters;
		}
	}

	public void suspend() {
		throw new UnsupportedOperationException("UNSUPPORTED OPERATION");
	}

	public void suspend(long timeout) {
		throw new UnsupportedOperationException("UNSUPPORTED OPERATION");
	}

	public void complete() {
		throw new UnsupportedOperationException("UNSUPPORTED OPERATION");
	}

	public boolean isInitial() {
		return true;
	}

	public boolean isSuspended() {
		return false;
	}

	public boolean isTimeout() {
		return false;
	}
}