package net.ion.niss.webapp.indexers;

import net.ion.craken.node.ReadNode;
import net.ion.niss.webapp.common.Def;
import net.ion.niss.webapp.common.Def.Schema;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.MyField.MyFieldType;

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
	
	public String schemaType(){
		return node.property(Schema.SchemaType).asString() ;
	}
	
	
	public String toString(){
		return "Analyze:" + isAnalyze() + ",Store" + isStore() + ",Boost" + boost() +  ",AnalClz" + analClz() ; 
	}

	public MyFieldType getType() {
		if ("text".equals(schemaType())){
			return MyFieldType.Text ;
		} else if ("keyword".equals(schemaType())){
			return MyFieldType.Keyword ;
		} else if ("number".equals(schemaType())){
			return MyFieldType.Number ;
		} else if ("date".equals(schemaType())){
			return MyFieldType.Date ;
		} else if ("manual".equals(schemaType())){
			return MyFieldType.Unknown ;
		}
		return MyFieldType.Unknown ;
	}

	public boolean isManualType() {
		return "manual".equals(schemaType());
	}

	
}
