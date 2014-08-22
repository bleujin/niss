package net.ion.niss.webapp.util;

import java.util.*;
import java.io.Serializable;

public class NamedList<T> implements Cloneable, Serializable, Iterable<Map.Entry<String, T>> {
	protected final List<Object> nvPairs;

	/** Creates an empty instance */
	public NamedList() {
		nvPairs = new ArrayList<Object>();
	}

	public NamedList(Map.Entry<String, ? extends T>[] nameValuePairs) {
		nvPairs = nameValueMapToList(nameValuePairs);
	}

	@Deprecated
	public NamedList(List<Object> nameValuePairs) {
		nvPairs = nameValuePairs;
	}

	@Deprecated
	private List<Object> nameValueMapToList(Map.Entry<String, ? extends T>[] nameValuePairs) {
		List<Object> result = new ArrayList<Object>();
		for (Map.Entry<String, ?> ent : nameValuePairs) {
			result.add(ent.getKey());
			result.add(ent.getValue());
		}
		return result;
	}

	/** The total number of name/value pairs */
	public int size() {
		return nvPairs.size() >> 1;
	}

	/**
	 * The name of the pair at the specified List index
	 * 
	 * @return null if no name exists
	 */
	public String getName(int idx) {
		return (String) nvPairs.get(idx << 1);
	}

	/**
	 * The value of the pair at the specified List index
	 * 
	 * @return may be null
	 */
	@SuppressWarnings("unchecked")
	public T getVal(int idx) {
		return (T) nvPairs.get((idx << 1) + 1);
	}

	/**
	 * Adds a name/value pair to the end of the list.
	 */
	public void add(String name, T val) {
		nvPairs.add(name);
		nvPairs.add(val);
	}

	/**
	 * Modifies the name of the pair at the specified index.
	 */
	public void setName(int idx, String name) {
		nvPairs.set(idx << 1, name);
	}

	/**
	 * Modifies the value of the pair at the specified index.
	 * 
	 * @return the value that used to be at index
	 */
	public T setVal(int idx, T val) {
		int index = (idx << 1) + 1;
		@SuppressWarnings("unchecked")
		T old = (T) nvPairs.get(index);
		nvPairs.set(index, val);
		return old;
	}

	/**
	 * Removes the name/value pair at the specified index.
	 * 
	 * @return the value at the index removed
	 */
	public T remove(int idx) {
		int index = (idx << 1);
		nvPairs.remove(index);
		@SuppressWarnings("unchecked")
		T result = (T) nvPairs.remove(index); // same index, as things shifted in previous remove
		return result;
	}

	/**
	 * Scans the list sequentially beginning at the specified index and returns the index of the first pair with the specified name.
	 * 
	 * @param name
	 *            name to look for, may be null
	 * @param start
	 *            index to begin searching from
	 * @return The index of the first matching pair, -1 if no match
	 */
	public int indexOf(String name, int start) {
		int sz = size();
		for (int i = start; i < sz; i++) {
			String n = getName(i);
			if (name == null) {
				if (n == null)
					return i; // matched null
			} else if (name.equals(n)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Gets the value for the first instance of the specified name found.
	 * <p>
	 * NOTE: this runs in linear time (it scans starting at the beginning of the list until it finds the first pair with the specified name).
	 * 
	 * @return null if not found or if the value stored was null.
	 * @see #indexOf
	 * @see #get(String,int)
	 * 
	 */
	public T get(String name) {
		return get(name, 0);
	}

	/**
	 * Gets the value for the first instance of the specified name found starting at the specified index.
	 * <p>
	 * NOTE: this runs in linear time (it scans starting at the specified position until it finds the first pair with the specified name).
	 * 
	 * @return null if not found or if the value stored was null.
	 * @see #indexOf
	 */
	public T get(String name, int start) {
		int sz = size();
		for (int i = start; i < sz; i++) {
			String n = getName(i);
			if (name == null) {
				if (n == null)
					return getVal(i);
			} else if (name.equals(n)) {
				return getVal(i);
			}
		}
		return null;
	}

	/**
	 * Gets the values for the the specified name
	 * 
	 * @param name
	 *            Name
	 * @return List of values
	 */
	public List<T> getAll(String name) {
		List<T> result = new ArrayList<T>();
		int sz = size();
		for (int i = 0; i < sz; i++) {
			String n = getName(i);
			if (name == n || (name != null && name.equals(n))) {
				result.add(getVal(i));
			}
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		int sz = size();
		for (int i = 0; i < sz; i++) {
			if (i != 0)
				sb.append(',');
			sb.append(getName(i));
			sb.append('=');
			sb.append(getVal(i));
		}
		sb.append('}');

		return sb.toString();
	}

	/**
	 * 
	 * Helper class implementing Map.Entry&lt;String, T&gt; to store the key-value relationship in NamedList (the keys of which are String-s)
	 */
	public static final class NamedListEntry<T> implements Map.Entry<String, T> {

		public NamedListEntry() {

		}

		public NamedListEntry(String _key, T _value) {
			key = _key;
			value = _value;
		}

		public String getKey() {
			return key;
		}

		public T getValue() {
			return value;
		}

		public T setValue(T _value) {
			T oldValue = value;
			value = _value;
			return oldValue;
		}

		private String key;

		private T value;
	}

	/**
	 * Iterates over the Map and sequentially adds it's key/value pairs
	 */
	public boolean addAll(Map<String, T> args) {
		for (Map.Entry<String, T> entry : args.entrySet()) {
			add(entry.getKey(), entry.getValue());
		}
		return args.size() > 0;
	}

	/** Appends the elements of the given NamedList to this one. */
	public boolean addAll(NamedList<T> nl) {
		nvPairs.addAll(nl.nvPairs);
		return nl.size() > 0;
	}

	/**
	 * Makes a <i>shallow copy</i> of the named list.
	 */
	@Override
	public NamedList<T> clone() {
		ArrayList<Object> newList = new ArrayList<Object>(nvPairs.size());
		newList.addAll(nvPairs);
		return new NamedList<T>(newList);
	}

	// ----------------------------------------------------------------------------
	// Iterable interface
	// ----------------------------------------------------------------------------

	/**
	 * Support the Iterable interface
	 */
	public Iterator<Map.Entry<String, T>> iterator() {

		final NamedList<T> list = this;

		Iterator<Map.Entry<String, T>> iter = new Iterator<Map.Entry<String, T>>() {

			int idx = 0;

			public boolean hasNext() {
				return idx < list.size();
			}

			public Map.Entry<String, T> next() {
				final int index = idx++;
				Map.Entry<String, T> nv = new Map.Entry<String, T>() {
					public String getKey() {
						return list.getName(index);
					}

					@SuppressWarnings("unchecked")
					public T getValue() {
						return list.getVal(index);
					}

					@Override
					public String toString() {
						return getKey() + "=" + getValue();
					}

					public T setValue(T value) {
						return list.setVal(index, value);
					}
				};
				return nv;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return iter;
	}

	/**
	 * NOTE: this runs in linear time (it scans starting at the beginning of the list until it finds the first pair with the specified name).
	 */
	public T remove(String name) {
		int idx = indexOf(name, 0);
		if (idx != -1)
			return remove(idx);
		return null;
	}

	public void clear() {
		nvPairs.clear();
	}

	@Override
	public int hashCode() {
		return nvPairs.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NamedList))
			return false;
		NamedList nl = (NamedList) obj;
		return this.nvPairs.equals(nl.nvPairs);
	}
}
