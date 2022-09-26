package net.ion.niss.webapp.indexers;

import net.bleujin.searcher.common.MyField;
import net.ion.framework.parse.gson.JsonPrimitive;

public interface FieldSchema {

	public final static FieldSchema DEFAULT = new FieldSchema() {
		
		@Override
		public MyField toMyField(String key, JsonPrimitive value) {
			return MyField.unknown(key, value.getValue());
		}
	};
	
	public MyField toMyField(String key, JsonPrimitive value) ;
}
