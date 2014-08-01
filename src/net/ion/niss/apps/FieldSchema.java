package net.ion.niss.apps;

import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.nsearcher.common.MyField;

public interface FieldSchema {

	public final static FieldSchema DEFAULT = new FieldSchema() {
		
		@Override
		public MyField toMyField(String key, JsonPrimitive value) {
			return MyField.unknown(key, value.getValue());
		}
	};
	
	public MyField toMyField(String key, JsonPrimitive value) ;
}
