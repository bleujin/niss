package net.ion.niss.webapp.common;

import java.util.function.Function;

import net.bleujin.rcraken.ReadNode;
import net.ion.framework.parse.gson.JsonObject;

public class Trans {

	public static final Function<ReadNode, JsonObject> DECENT = new Function<ReadNode, JsonObject>(){
		@Override
		public JsonObject apply(ReadNode target) {
			final JsonObject result = new JsonObject() ;
		
			target.walkBreadth(true, 10).stream().transform(new Function<Iterable<ReadNode>, Void>() {
				@Override
				public Void apply(Iterable<ReadNode> decent) {
					for(ReadNode node : decent){
						result.add(node.fqn().toString(), node.toJson());
					}
					return null;
				}
			}) ;
			
			return result;
		}
	};
	
}
