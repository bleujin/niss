package net.ion.niss;

import net.bleujin.rcraken.WriteJob;
import net.bleujin.rcraken.WriteSession;
import net.ion.framework.util.ListUtil;

public class TransactionJobs {
	public static final WriteJob<Void> HelloBleujin = new WriteJob<Void>() {
		@Override
		public Void handle(WriteSession wsession) throws Exception {
			wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).merge();
			return null;
		}
	};
	public static final WriteJob<Void> REMOVE_ALL = new WriteJob<Void>() {
		@Override
		public Void handle(WriteSession wsession) throws Exception {
			wsession.root().removeChild();
			return null;
		}
	};

	public final static WriteJob<Void> dummy(final String prefixFqn, final int count){
		return new WriteJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(count)) {
					wsession.pathBy("/").child(prefixFqn).property("prefix", prefixFqn).child("" + i).property("name", "bleujin").property("dummy", i).merge();
				}
				return null;
			}
		} ;
	}
	
	public final static WriteJob<Void> dummyEmp(final int count){
		return dummy("/emp", count) ; 
	}

}
