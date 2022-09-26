package net.ion.niss.webapp.indexers;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableField;

import net.bleujin.rcraken.ReadSession;
import net.bleujin.searcher.SearchController;
import net.bleujin.searcher.common.FieldIndexingStrategy;
import net.bleujin.searcher.common.MyField;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.niss.webapp.IdString;
import net.ion.niss.webapp.common.Def.IndexSchema;

public class IndexManager {

	private Map<IdString, SearchController> indexes = MapUtil.newMap() ;
	
	public IndexManager newIndex(IdString iid, SearchController central) {
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
	
	public SearchController index(IdString iid){
		return indexes.get(iid) ;
	}	
	
	public SearchController index(String iid){
		return index(IdString.create(iid)) ;
	}

	public FieldIndexingStrategy fieldIndexStrategy(ReadSession session, String iid){
	
		final SchemaInfos sinfos = SchemaInfos.create(session.pathBy(IndexSchema.path(iid)).children()) ;

		return new FieldIndexingStrategy() {
			@Override
			public void save(final Document doc, final MyField myField, final IndexableField ifield) {
				sinfos.addField(doc, myField, ifield);
			}
		};
	}
	
	public void removeIndex(IdString iid) {
		SearchController removed = indexes.remove(iid) ;
		IOUtil.close(removed); 
		
		// TODO : file remove ?
	}

	
	

}
