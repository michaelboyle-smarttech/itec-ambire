package org.eun.itec.ambire.teacher;

import java.util.TreeMap;


public class WithMap<K,V> extends TreeMap<K,V> {
	private static final long serialVersionUID = 5199015670626300562L;
	public WithMap() {
		super();
	}
	public WithMap<K,V> with(K k, V v) {
		put(k,v);
		return this;
	}
}
