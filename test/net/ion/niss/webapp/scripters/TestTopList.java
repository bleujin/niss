package net.ion.niss.webapp.scripters;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.http.annotation.NotThreadSafe;

public class TestTopList extends TestCase{
	
	public void testEntry() throws Exception {
		
		TopEntryCollector<Integer> te = new TopEntryCollector<Integer>(10) ;
		
		Comparator<Integer> compare = new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return o1 - o2;
			}
		};
		for (int i = 0; i < 100 ; i++) {
			te.add(RandomUtil.nextInt(100), compare) ;
		}
		
		List<Integer> result = te.result() ;
		Debug.line(result);
	}

}


@NotThreadSafe
class TopEntryCollector<T> {

	private int maxEntry;
	private List<T> entries = ListUtil.newList();
	public TopEntryCollector(int maxEntry){
		this.maxEntry = maxEntry ;
	}
	
	public List<T> result() {
		Collections.reverse(entries);
		return entries;
	}

	public TopEntryCollector<T> add(T element, Comparator<T> compare){
		if (entries.size() < maxEntry || compare.compare(element,  entries.get(0)) > 0){
			entries.add(element) ;
			Collections.sort(entries, compare);
		}
		
		if (entries.size() > maxEntry) {
			entries.remove(0) ;
		}
		return this ;
	}
}
