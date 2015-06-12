package net.ion.craken.loaders;

import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.framework.util.Debug;
import net.ion.niss.config.NSConfig;
import net.ion.niss.config.builder.ConfigBuilder;
import net.ion.niss.webapp.REntry;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.Searcher;

import com.google.common.base.Function;

public class TestRead extends TestCase{

	public void testRun() throws Exception {
		NSConfig nsconfig = ConfigBuilder.create("./resource/config/niss-config.xml").build();
		REntry rentry = REntry.create(nsconfig) ;
		
		ReadSession session = rentry.login() ;
		session.root().children().debugPrint(); 
		
		
		session.root().walkChildren().transform(new Function<Iterator<ReadNode>, Void>(){
			@Override
			public Void apply(Iterator<ReadNode> iter) {
				while(iter.hasNext()){
					ReadNode next = iter.next() ;
					Set<PropertyId> keys = next.keys() ;
					for (PropertyId key : keys) {
						if (key.type() == PType.REFER){
							Debug.line(next);
						}
					}
					
				}
				
				return null;
			}
			
		}) ;
		
		Searcher searcher = session.workspace().central().newSearcher() ;
		
		
		ReadDocument doc = searcher.createRequestByKey("/events/loaders/znq6s57xs_1416465945243").find().first() ;
		Debug.line(doc.asString(EntryKey.VALUE));

		rentry.close();
	}
}
