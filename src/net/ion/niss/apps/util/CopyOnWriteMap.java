package net.ion.niss.apps.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A thread-safe version of {@link Map} in which all operations that change the Map are implemented by making a new copy of the underlying Map.
 * 
 * While the creation of a new Map can be expensive, this class is designed for cases in which the primary function is to read data from the Map, not to modify the Map. Therefore the operations that do not cause a change to this class happen quickly and concurrently.
 * 
 */

public class CopyOnWriteMap<K, V> implements Map<K, V>, Cloneable {
	private volatile Map<K, V> internalMap;

	public CopyOnWriteMap() {
		internalMap = new HashMap<K, V>();
	}

	public CopyOnWriteMap(int initialCapacity) {
		internalMap = new HashMap<K, V>(initialCapacity);
	}

	public CopyOnWriteMap(Map<K, V> data) {
		internalMap = new HashMap<K, V>(data);
	}

	public V put(K key, V value) {
		synchronized (this) {
			Map<K, V> newMap = new HashMap<K, V>(internalMap);
			V val = newMap.put(key, value);
			internalMap = newMap;
			return val;
		}
	}

	public V remove(Object key) {
		synchronized (this) {
			Map<K, V> newMap = new HashMap<K, V>(internalMap);
			V val = newMap.remove(key);
			internalMap = newMap;
			return val;
		}
	}

	public void putAll(Map<? extends K, ? extends V> newData) {
		synchronized (this) {
			Map<K, V> newMap = new HashMap<K, V>(internalMap);
			newMap.putAll(newData);
			internalMap = newMap;
		}
	}

	public void clear() {
		synchronized (this) {
			internalMap = new HashMap<K, V>();
		}
	}

	public int size() {
		return internalMap.size();
	}

	public boolean isEmpty() {
		return internalMap.isEmpty();
	}

	public boolean containsKey(Object key) {
		return internalMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return internalMap.containsValue(value);
	}

	public V get(Object key) {
		return internalMap.get(key);
	}

	public Set<K> keySet() {
		return internalMap.keySet();
	}

	public Collection<V> values() {
		return internalMap.values();
	}

	public Set<Entry<K, V>> entrySet() {
		return internalMap.entrySet();
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
}