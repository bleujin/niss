package net.ion.niss.webapp.misc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;

import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

public class HttpParams extends HashMap<String, List<String>> implements MultivaluedMap<String, String> {
	
	public void putSingle(String key, String value) {
		List<String> list = ListUtil.newList();
		list.add(value);
		this.put(key, list);
	}

	public final void add(String key, String value) {
		this.getList(key).add(value);
	}

	public final void addMultiple(String key, Collection<String> values) {
		this.getList(key).addAll(values);
	}

	public String getFirst(String key) {
		List<String> list = this.get(key);
		return list == null ? null : list.get(0);
	}

	public String getFirst(String key, String dftString) {
		List<String> list = this.get(key);
		return  ObjectUtil.coalesce(list == null ? null : list.get(0), dftString);
	}


	public final List<String> getList(String key) {
		List<String> list = this.get(key);
		if (list == null) {
			this.put(key, list = ListUtil.newList());
		}
		return list;
	}

	public void addAll(MultivaluedMapImpl<String, String> other) {
		Iterator<Entry<String, List<String>>> arg2 = other.entrySet().iterator();
		while (arg2.hasNext()) {
			Entry<String, List<String>> entry = arg2.next();
			this.getList(entry.getKey()).addAll(entry.getValue());
		}
	}

	public MultivaluedMap<String, String> addValue(String key, String value) {
		this.add(key, value);
		return this;
	}

	public static HttpParams create(HttpRequest request) {
		HttpParams params = new HttpParams();
		for (Entry<String, List<String>> entry : request.getUri().getQueryParameters().entrySet()) {
			if (StringUtil.isNotBlank(entry.getKey())) params.put(entry.getKey(), entry.getValue()) ;
		}
		
		for (Entry<String, List<String>> entry : request.getDecodedFormParameters().entrySet()) {
			if (StringUtil.isNotBlank(entry.getKey())) params.put(entry.getKey(), entry.getValue()) ;
		}
		return params ;
	}
}
