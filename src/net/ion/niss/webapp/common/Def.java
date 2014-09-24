package net.ion.niss.webapp.common;

import net.ion.craken.tree.Fqn;
import net.ion.framework.util.StringUtil;

public class Def {

	public static class Indexer {
		public static final String IndexAnalyzer = "indexanalyzer" ;
		public static final String ApplyStopword = "applystopword";
		public static final String QueryAnalyzer = "queryanalyzer";
		public static final String StopWord = "stopword";
	}
	
	
	public static class SchemaType {
		public static final String MANUAL = "manual" ;
		public static final String TEXT = "text" ;
		public static final String KEYWORD = "keyword" ;
		public static final String NUMBER = "number" ;
	}
	
	
	public static class Schema {
		// .property("schematype", schematype).property("analyze", analyzer).property("store", store).property("boost", Double.valueOf(StringUtil.defaultIfEmpty(boost, "1.0"))) ;
		
        public static final String SchemaType = "schematype";
		public static final String Analyzer = "analyzer";
		public static final String Analyze = "analyze";
		public static final String Store = "store";
		public static final String Boost = "boost";
		
		public static String path(String iid, String schemaid) {
			return "/indexers/" + iid + "/schema/" + schemaid;
		}

		public static String path(String iid) {
			return "/indexers/" + iid + "/schema";
		}
	}


	public static class Searcher {
		public static final String Target = "target" ;
		public static final String Handler = "handler";
		public static final String ApplyHandler = "applyhandler";
		public static final String Template = "template";
		public static final String StopWord = "stopword";
		public static final String QueryAnalyzer = "queryanalyzer" ;
		public static final String ApplyStopword = "applystopword";
	}
	
}
