package net.ion.niss.webapp.scripters;

import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;
import net.ion.niss.webapp.searchers.TopEntryCollector;

public class TestTopList extends TestCase{
	
	public void testEntry() throws Exception {
		
		TopEntryCollector<Integer> te = new TopEntryCollector<Integer>(10, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return o1 - o2;
			}
		}) ;
		
		for (int i = 0; i < 100 ; i++) {
			te.add(RandomUtil.nextInt(100)) ;
		}
		
		List<Integer> result = te.result() ;
		Debug.line(result);
	}

}

