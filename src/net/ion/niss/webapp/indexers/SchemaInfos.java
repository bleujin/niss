package net.ion.niss.webapp.indexers;

import java.util.Iterator;
import java.util.Map;

import net.ion.craken.node.ReadNode;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyField;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;

public class SchemaInfos {

	private Map<String, SchemaInfo> infos = MapUtil.newMap();
	
	private SchemaInfos(){
	}

	public static SchemaInfos create(Iterator<ReadNode> iter) {
		SchemaInfos result = new SchemaInfos() ;
		while(iter.hasNext()){
			SchemaInfo sinfo = SchemaInfo.create(iter.next()) ;
			result.put(sinfo.fieldId(), sinfo) ;
		}
		return result;
	}


	private void put(String fieldId, SchemaInfo sinfo) {
		this.infos.put(fieldId, sinfo) ;
	}

	public MyField myField(MyField myfield, Field ifield) {
		if (IKeywordField.Field.reservedId(ifield.name())) return myfield ;
		
		if (infos.containsKey(myfield.name())){
			SchemaInfo sinfo = infos.get(myfield.name()) ;
			
			if (sinfo.isManualType()) {
				return MyField.manual(ifield.name(), ifield.stringValue(), sinfo.isStore() ? Store.YES : Store.NO, sinfo.isAnalyze(), sinfo.getType()) ;
			}
			return new MyField(ifield, sinfo.getType()) ;
		} else {
			return myfield ;
		}
	}

}
