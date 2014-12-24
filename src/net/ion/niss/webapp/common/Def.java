package net.ion.niss.webapp.common;


public class Def {

	public static class Indexer {
		public static final String Created = "created" ;
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
	
	
	public static class IndexSchema {
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
		public static final String Created = "created" ;
		public static final String Target = "target" ;
		public static final String Handler = "handler";
		public static final String ApplyHandler = "applyhandler";
		public static final String Template = "template";
		public static final String StopWord = "stopword";
		public static final String QueryAnalyzer = "queryanalyzer" ;
		public static final String ApplyStopword = "applystopword";
	}

	public static class SearchSchema extends IndexSchema{
		public static String path(String sid, String schemaid) {
			return "/searchers/" + sid + "/schema/" + schemaid;
		}

		public static String path(String sid) {
			return "/searchers/" + sid + "/schema";
		}
		
	}

	
	public static class Loader {
		public static final String Time = "time" ;
		public static final String Status = "status" ;
		public static final String Created = "created";
		public static final String Content = "content" ;
	}

	
	public static class User {
		public static final String Name = "name" ;
		public static final String Password = "password" ;
	}
	
	public static class Script {
		public static final String Sid = "sid" ;
		public static final String Content = "content" ;
		public static final String Running = "running" ;
	}
	

	public static class Schedule {
		public static final String Sid = "sid" ;
		public static final String MINUTE = "minute" ;
		public static final String HOUR = "hour" ;
		public static final String DAY = "day" ;
		public static final String MONTH = "month" ;
		public static final String WEEK = "week" ;
		public static final String MATCHTIME = "matchtime" ;
		public static final String YEAR = "year" ;
		
		public static final String ENABLE = "enable" ;
		public static final String Parity = "parity";
	}
	
	public static class SLog {
		public static final String CIndex = "cindex" ;
		public static final String Sid = "sid" ;
		public static final String Runtime = "runtime" ;
		public static final String Status = "status" ;
		public static final String Success = "success";
		public static final String Fail = "fail";
		public static final String Result = "result";
		
		public static String path(String sid){
			return "/scripts/" + sid + "/slogs" ;
		}
	}
}
