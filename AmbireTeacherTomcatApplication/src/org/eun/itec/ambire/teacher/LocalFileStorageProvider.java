package org.eun.itec.ambire.teacher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

public class LocalFileStorageProvider implements StorageProvider {

	private String m_location;

	LocalFileStorageProvider(ServletContext context) {
		String location = context.getInitParameter("LocalFileStorageProvider.LOCATION");
		if (location != null) {
			m_location = location;
		}
		if (m_location == null) {
			m_location = context.getRealPath("/d");
		}
		File f = new File(m_location);
		if (!f.isDirectory()) {
			f.delete();
		}
		if (!f.exists()) {
			f.mkdirs();
		}
	}

	@Override
	public void close() {
		
	}
	
	@Override
	public StoredFileInfo storeFile(InputStream content, long contentLength, String contentType, String suggestedFileName, String baseUrl) {
		String filename = suggestedFileName;
		String path = m_location + File.separator + filename;
		int ver = 1;
		while(true) {
			File f = new File(path);
			if(f.exists()) {
				filename = String.format("v%d-%s", ++ver, suggestedFileName);
				path = m_location + File.separator + filename;
			} else {
				break;
			}
		}
		IOException err = null;
		FileOutputStream o = null;
		try {
			o = new FileOutputStream(path);
			byte[] buf = new byte[32 * 1024];
			while (true) {
				int n = content.read(buf, 0, (int)Math.min(buf.length, contentLength));
				if (n <= 0) {
					break;
				} else if (n > 0) {
					o.write(buf, 0, n);
					contentLength -= n;
				}
			}
		} catch (IOException e) {
			err = e;
		} finally {
			try {
				if(o != null) {
					o.close();
				}
			} catch (IOException e) {
				err = e;
			}
		}
		if (err != null) {
			return null;
		}
		StoredFileInfo info = new StoredFileInfo();
		info.href = baseUrl + "d/" + filename;
		info.token = path;
		return info;
	}

	@Override
	public void deleteFiles(Iterable<String> tokens, boolean force) {
		for (String token : tokens) {
			File f = new File(token);
			if (f.exists()) {
				f.delete();
			}
		}
	}

}
