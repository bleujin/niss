package net.bleujin.mapdb;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

public class TestConfirm extends TestCase {

	public void testRead() throws Exception {
		DB db = DBMaker.fileDB("./resource/admin/mapdb.db").make() ;
		db.getAll().entrySet().forEach(entry ->{
			Debug.line(entry.getKey(), entry.getValue());
			HTreeMap hmap = (HTreeMap) (entry.getValue()) ;
			hmap.getKeys().forEach(key ->{
				Debug.line(key, hmap.get(key), hmap.get(key).getClass());
			}) ;
		}) ;
		
	}
}
