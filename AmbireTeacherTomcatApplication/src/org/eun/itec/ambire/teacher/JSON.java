package org.eun.itec.ambire.teacher;

import org.codehaus.jackson.map.ObjectMapper;

public abstract class JSON {
	private static ObjectMapper g_mapper = null;
	public static String stringify(Object o) {
		try {
			if(g_mapper == null) {
				g_mapper = new ObjectMapper();
			}
			return g_mapper.writeValueAsString(o);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
