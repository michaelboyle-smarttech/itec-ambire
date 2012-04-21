package org.eun.itec.ambire.teacher;

import java.io.InputStream;

public interface StorageProvider {
	public StoredFileInfo storeFile(InputStream content, long contentLength, String contentType, String suggestedFileName, String baseUrl);
	public void deleteFiles(Iterable<String> tokens, boolean force);
	public void close();
}
