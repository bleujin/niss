package net.ion.niss.webapp.common;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.StringUtil;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ToJsonHandler extends DefaultHandler {
	private MessageEntity root = new MessageEntityImpl("");
	private MessageEntity current = root;
	private Cache<String, MessageEntity> mes = CacheBuilder.newBuilder().maximumSize(10).build() ;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		MessageEntity created = new MessageEntityImpl(localName);

		for (int i = 0; i < atts.getLength(); i++) {
			created.add(atts.getQName(i), new JsonPrimitive(atts.getValue(i)));
		}
		created.parent(current);
		this.current = created;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (current.localName().equals(localName)) {
			this.current = current.parent();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String string = new String(ch, start, length);
		
		if (StringUtil.isBlank(string))
			return;
		current.add("text", new JsonPrimitive(StringUtil.trim(string)));
	}

	public MessageEntity root() {
		return root;
	}

	public MessageEntity root(final String langCode) throws RuntimeException {
		try {
			return mes.get(langCode, new Callable<MessageEntity>() {
				@Override
				public MessageEntity call() throws Exception {
					return new LanguageMessageEntity("", root, langCode);
				}
			});
		} catch (ExecutionException e) {
			throw new RuntimeException(e) ;
		}
	}
}

class LanguageMessageEntity extends MessageEntity{
	private MessageEntity original;
	private String postFix;

	protected LanguageMessageEntity(String localName, MessageEntity original, String lang) {
		super(localName);
		this.original = original ;
		this.postFix = "." + lang ;
	}

	@Override
	public String asString(String path) {
		String result = original.asString(path + this.postFix);
		if (result.equals(path + postFix)) return original.asString(path) ; 
		return result;
	}

	@Override
	public String asString(String path, Object... param) {
		String result = original.asString(path + this.postFix, param);
		if (result.equals(path + postFix)) return original.asString(path, param) ; 
		return result ;
	}
	
}