package net.ion.niss.webapp.indexers;

import net.bleujin.searcher.common.WriteDocument;
import net.bleujin.searcher.index.IndexJob;
import net.bleujin.searcher.index.IndexSession;

public class IndexJobs {

	private IndexJobs(){} ;
	
	
	public final static IndexJob<Void> create(final String prefix, final int count){
		return new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i = 0; i < count; i++) {
					WriteDocument wdoc = isession.newDocument(prefix + i).keyword("prefix", prefix).number("idx", i) ;
					isession.updateDocument(wdoc) ;
				}
				return null;
			}
		};
	}
}
