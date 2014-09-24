package net.ion.niss.webapp.indexers;

import net.ion.craken.node.ReadNode;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.Schema;

public class SchemaInfo {

	
	 // [schematype, analyzer, analyze, store, boost]
	
	private ReadNode node;
	private SchemaInfo(ReadNode node) {
		this.node = node ;
	}

	public static SchemaInfo create(ReadNode node) {
		return new SchemaInfo(node);
	}

	
	public String fieldId(){
		return node.fqn().name() ;
	}
	
	public boolean isAnalyze(){
		return node.property(Def.Schema.Analyze).asBoolean() ;
	}
	
	public float boost(){
		return node.property(Def.Schema.Boost).asFloat(1.0f) ;
	}
	
	public boolean isStore(){
		return node.property(Schema.Store).asBoolean() ;
	}
	
	public String analClz(){
		return node.property(Schema.Analyzer).asString() ;
				
	}
	
}
