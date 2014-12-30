package net.ion.niss.webapp.searchers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.ion.framework.util.ListUtil;

import org.apache.http.annotation.NotThreadSafe;

@NotThreadSafe
public class TopEntryCollector<T> {

	private List<T> entries = ListUtil.newList();
	private int maxEntry;
	private Comparator<T> compare;

	public TopEntryCollector(int maxEntry, Comparator<T> compare) {
		this.maxEntry = maxEntry;
		this.compare = compare;
	}

	public List<T> result() {
		Collections.reverse(entries);
		return entries;
	}

	public TopEntryCollector<T> add(T element) {
		if (entries.size() < maxEntry || compare.compare(element, entries.get(0)) > 0) {
			entries.add(element);
			Collections.sort(entries, compare);
		}

		if (entries.size() > maxEntry) {
			entries.remove(0);
		}
		return this;
	}
}
