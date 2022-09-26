package net.ion.niss.webapp.indexers;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.poi.util.LongField;

import net.bleujin.rcraken.ReadNode;
import net.bleujin.searcher.common.IKeywordField;
import net.bleujin.searcher.common.MyField;
import net.bleujin.searcher.common.MyField.MyFieldType;
import net.bleujin.searcher.common.WriteDocument;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;

public class SchemaInfos {

	private Map<String, SchemaInfo> infos = MapUtil.newMap();
	
	private SchemaInfos(){
	}

	public static SchemaInfos create(Iterable<ReadNode> iter) {
		SchemaInfos result = new SchemaInfos() ;
		for(ReadNode node : iter){
			SchemaInfo sinfo = SchemaInfo.create(node) ;
			result.put(sinfo.fieldId(), sinfo) ;
		}
		return result;
	}


	private void put(String fieldId, SchemaInfo sinfo) {
		this.infos.put(fieldId, sinfo) ;
	}


	public void addFields(WriteDocument wdoc, JsonObject json) {
		for(Entry<String, JsonElement> jele : json.entrySet()){
			addFieldFromJson(wdoc, jele.getKey(), jele.getValue());
		}
	}

	public void addField(Document doc, MyField myField, IndexableField ifield) {
		String name = myField.name() ;
		String value = myField.stringValue();
		if (IKeywordField.Field.reservedId(name)) {
			doc.add(ifield) ;
		} else if (myField.myFieldtype() == MyFieldType.Unknown && infos.containsKey(myField.name())){
			SchemaInfo sinfo = infos.get(name) ;
			if (sinfo.getType() == MyFieldType.Keyword) doc.add(new StringField(name, value, Store.YES)) ;
			else if (sinfo.getType() == MyFieldType.Number) doc.add(new NumericDocValuesField(name, NumberUtil.toLong(value, 0L))) ;
			else if (sinfo.getType() == MyFieldType.Text) doc.add(new TextField(name, value, Store.NO)) ;
			else if ("manual".equals(sinfo.schemaType())) {
				Store store = sinfo.isStore() ? Store.YES : Store.NO;
				doc.add(sinfo.isAnalyze() ? new TextField(name, value, store) : new StringField(name, value, store)) ;
			} else doc.add(ifield) ;
		} else {
			doc.add(ifield);
		}
	}


	public void addFields(WriteDocument wdoc, Map<String, String> map) {
		for (Entry<String, String> entry : map.entrySet()) {
			String name = entry.getKey() ;
			String value = entry.getValue() ;
			if (IKeywordField.Field.reservedId(name)) wdoc.add(MyField.keyword(name, value)) ;
			else if (infos.containsKey(name)){
				SchemaInfo sinfo = infos.get(name) ;
				if (sinfo.getType() == MyFieldType.Keyword) wdoc.add(MyField.keyword(name, value)) ;
				else if (sinfo.getType() == MyFieldType.Number) wdoc.add(MyField.number(name, NumberUtil.toLong(value, 0L))) ;
				else if (sinfo.getType() == MyFieldType.Text) wdoc.add(MyField.text(name, value)) ;
				else if ("manual".equals(sinfo.schemaType())) wdoc.add(MyField.manual(name, value, sinfo.isStore() ? Store.YES : Store.NO, sinfo.isAnalyze(), sinfo.getType())) ;
				else wdoc.add(MyField.unknown(name, value)) ;
			} else {
				wdoc.add(MyField.unknown(name, value)) ;
			}
		}
	}


	private void addFieldFromJson(WriteDocument wdoc, String name, JsonElement jvalue) {
		if (jvalue.isJsonPrimitive()){
			JsonPrimitive pvalue = jvalue.getAsJsonPrimitive() ;
			
			if (IKeywordField.Field.reservedId(name)) wdoc.add(MyField.keyword(name, pvalue.getAsString())) ;
			else if (infos.containsKey(name)){
				SchemaInfo sinfo = infos.get(name) ;
				if (sinfo.getType() == MyFieldType.Keyword) wdoc.add(MyField.keyword(name, pvalue.getAsString())) ;
				else if (sinfo.getType() == MyFieldType.Number) wdoc.add(MyField.number(name, NumberUtil.toLong(pvalue.getAsString(), 0L))) ;
				else if (sinfo.getType() == MyFieldType.Text) wdoc.add(MyField.text(name, pvalue.getAsString())) ;
				else if ("manual".equals(sinfo.schemaType())) wdoc.add(MyField.manual(name, pvalue.getAsString(), sinfo.isStore() ? Store.YES : Store.NO, sinfo.isAnalyze(), sinfo.getType())) ;
				else MyField.unknown(name, pvalue.getAsString()) ;
			} else {
				if (pvalue.isNumber()){
					wdoc.add(MyField.number(name, pvalue.getAsLong())) ;
				} else wdoc.add(MyField.unknown(name, pvalue.getAsString())) ;
			}
		} else if (jvalue.isJsonArray()) {
			for (JsonElement jele : jvalue.getAsJsonArray().toArray()) {
				addFieldFromJson(wdoc, name, jele);
			}
		} else if (jvalue.isJsonObject()){
			for(Entry<String, JsonElement> jele : jvalue.getAsJsonObject().entrySet()){
				addFieldFromJson(wdoc, name + "." + jele.getKey(), jele.getValue());
			}
		}
	}


	private MyField myField(MyField myfield, Field ifield) {
		if (IKeywordField.Field.reservedId(ifield.name())) return myfield ;
		if (infos.containsKey(myfield.name())){
			SchemaInfo sinfo = infos.get(myfield.name()) ;
			
			MyField result = null ;
			if (sinfo.getType() == MyFieldType.Keyword){
				result = MyField.keyword(ifield.name(), ifield.stringValue(), sinfo.isStore() ? Store.YES : Store.NO) ;
			} else if (sinfo.getType() == MyFieldType.Text){
				result = MyField.text(ifield.name(), ifield.stringValue(), sinfo.isStore() ? Store.YES : Store.NO) ;
			} else if (sinfo.getType() == MyFieldType.Number){
				result = MyField.number(ifield.name(), NumberUtil.toLong(ifield.stringValue())) ;
			} else if ("manual".equals(sinfo.schemaType())){
				result = MyField.manual(ifield.name(), ifield.stringValue(), sinfo.isStore() ? Store.YES : Store.NO, sinfo.isAnalyze(), sinfo.getType()) ;
			} else {
				result = MyField.unknown(ifield.name(), ifield.stringValue()) ;
			}
//			if (sinfo.isAnalyze()) {
//				result.boost(Double.valueOf(sinfo.boost()).floatValue()) ;
//			}
			return result ;
//			if (sinfo.isManualType()) {
//				return MyField.manual(ifield.name(), ifield.stringValue(), sinfo.isStore() ? Store.YES : Store.NO, sinfo.isAnalyze(), sinfo.getType()).boost(sinfo.boost()) ;
//			} else return MyField.manual(ifield.name(), ifield.stringValue(), sinfo.isStore() ? Store.YES : Store.NO, sinfo.isAnalyze(), sinfo.getType()).boost(sinfo.boost()) ;
		} else {
			return myfield ;
		}
	}


}
