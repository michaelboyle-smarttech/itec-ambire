/*   Copyright (C) 2012, SMART Technologies.
     All rights reserved.
  
     Redistribution and use in source and binary forms, with or without modification, are permitted
     provided that the following conditions are met:
   
      * Redistributions of source code must retain the above copyright notice, this list of
        conditions and the following disclaimer.
   
      * Redistributions in binary form must reproduce the above copyright notice, this list of
        conditions and the following disclaimer in the documentation and/or other materials
        provided with the distribution.
   
      * Neither the name of SMART Technologies nor the names of its contributors may be used to
         endorse or promote products derived from this software without specific prior written
         permission.
   
     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
     IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
     FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
     CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
     CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
     SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
     THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
     OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
     POSSIBILITY OF SUCH DAMAGE.
   
     Author: Michael Boyle
*/
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
