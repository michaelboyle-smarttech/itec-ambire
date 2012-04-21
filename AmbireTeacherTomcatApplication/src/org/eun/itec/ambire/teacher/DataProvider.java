package org.eun.itec.ambire.teacher;

import java.util.List;


public interface DataProvider {
	public double verify(String pin);
	public String selectPin(String owner);
	public double signIn(String owner, String pin, double timestamp);
	public void signOut(double sessionId);
	public double upload(double sessionId, String href, String token, String name, String kind, String mimeType, double timestamp, int width, int height, boolean replace);
	public List<UploadInfo> selectUploads(double sessionId, double sinceTimestamp);
	public void keepSignedIn(double sessionId, double timestamp);
	public void collectGarbage();
	public void close();
}
