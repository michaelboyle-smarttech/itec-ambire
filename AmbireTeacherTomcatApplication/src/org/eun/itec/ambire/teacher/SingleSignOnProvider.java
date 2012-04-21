package org.eun.itec.ambire.teacher;

import java.util.Map;

import javax.servlet.http.HttpSession;

public interface SingleSignOnProvider {
	public enum Attribute {
		IDENTITY,
		LOGOUT_URL
	}
	public enum InfoLevel {
		NOT_REQUESTED,
		REQUESTED,
		REQUIRED,
	}
	public void close();
	public boolean requiresRedirection();
	public String startSignOn(String identity, String returnUrl, HttpSession session, Map<Attribute,InfoLevel> infoLevel);
	public Map<Attribute,String> finishSignOn(String requestUrl, Map<String,String[]> parameters, HttpSession session);
	public String signOff(HttpSession session);
}
