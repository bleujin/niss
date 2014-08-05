package net.ion.niss.apps.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SimpleOrderedMap<T> extends NamedList<T> {
	private static final long serialVersionUID = -2241319542754527074L;

	public SimpleOrderedMap() {
		super();
	}

	/**
	 * Creates an instance backed by an explicitly specified list of pairwise names/values.
	 * 
	 * @param nameValuePairs
	 *            underlying List which should be used to implement a SimpleOrderedMap; modifying this List will affect the SimpleOrderedMap.
	 */
	@Deprecated
	public SimpleOrderedMap(List<Object> nameValuePairs) {
		super(nameValuePairs);
	}

	public SimpleOrderedMap(Map.Entry<String, T>[] nameValuePairs) {
		super(nameValuePairs);
	}

	@Override
	public SimpleOrderedMap<T> clone() {
		ArrayList<Object> newList = new ArrayList<Object>(nvPairs.size());
		newList.addAll(nvPairs);
		return new SimpleOrderedMap<T>(newList);
	}
}