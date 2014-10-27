package net.ion.niss.webapp.indexers;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.common.Def.IndexSchema;
import net.ion.nsearcher.common.FieldIndexingStrategy;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.MyField.MyFieldType;
import net.ion.nsearcher.config.Central;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;

import com.google.common.base.Function;

public class IndexManager {

	private Map<IdString, Central> indexes = MapUtil.newMap() ;
	
	public IndexManager newIndex(IdString iid, Central central) {
		indexes.put(iid, central) ;
		return this ;
	}
	
	
	public Set<IdString> keys(){
		return indexes.keySet() ;
	}
	
	public boolean hasIndex(IdString iid){
		return indexes.containsKey(iid) ;
	}
	
	public boolean hasIndex(String iid) {
		return hasIndex(IdString.create(iid));
	}
	
	public Central index(IdString iid){
		return indexes.get(iid) ;
	}	
	
	public Central index(String iid){
		return index(IdString.create(iid)) ;
	}

	public FieldIndexingStrategy fieldIndexStrategy(ReadSession session, String iid){
	
		final SchemaInfos sinfos = session.ghostBy(IndexSchema.path(iid)).children().transform(new Function<Iterator<ReadNode>, SchemaInfos>(){
			@Override
			public SchemaInfos apply(Iterator<ReadNode> iter) {
				return SchemaInfos.create(iter) ;
			}
		}) ;
		
		
		return new FieldIndexingStrategy() {
			@Override
			public void save(Document doc, MyField myField, final Field ifield) {

				final MyField newField = sinfos.myField(myField, ifield) ;
				
				final String fieldName = IKeywordField.Field.reservedId(ifield.name()) ? ifield.name() :  StringUtil.lowerCase(ifield.name());
				
				if (newField.myFieldtype() == MyFieldType.Number){
					doc.add(new StringField(fieldName, ifield.stringValue(), Store.NO));
				}
				if (newField.myFieldtype() == MyFieldType.Unknown && NumberUtil.isNumber(ifield.stringValue())){
					doc.add(new DoubleField(fieldName, Double.parseDouble(ifield.stringValue()), Store.NO));
				}
				if (newField.myFieldtype() == MyFieldType.Date){
					// new Date().getTime();
					Date date = DateUtil.stringToDate(ifield.stringValue(), "yyyyMMdd HHmmss") ;
					doc.add(new StringField(fieldName, StringUtil.substringBefore(ifield.stringValue(), " "), Store.NO)) ;
					doc.add(new StringField(fieldName, ifield.stringValue(), Store.NO)) ;
					doc.add(new LongField(fieldName, Long.parseLong(DateUtil.dateToString(date, "yyyyMMdd")), Store.NO)) ;
				}
				
				
				
				doc.add(new IndexableField() {
					@Override
					public TokenStream tokenStream(Analyzer analyzer) throws IOException {
						return ifield.tokenStream(analyzer);
					}
					
					@Override
					public String stringValue() {
						return ifield.stringValue();
					}
					
					@Override
					public Reader readerValue() {
						return ifield.readerValue();
					}
					
					@Override
					public Number numericValue() {
						return ifield.numericValue();
					}
					
					@Override
					public String name() {
						return fieldName ;
					}
					
					@Override
					public IndexableFieldType fieldType() {
						return newField.fieldType(); // redefine
					}
					
					@Override
					public float boost() {
						return newField.boost();
					}
					
					@Override
					public BytesRef binaryValue() {
						return ifield.binaryValue();
					}
				}) ;
			}
		};
	}
	
	public void removeIndex(IdString iid) {
		Central removed = indexes.remove(iid) ;
		IOUtil.close(removed); 
		
		// TODO : file remove ?
	}

	
	

}
