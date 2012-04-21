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

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/upload")
@MultipartConfig(fileSizeThreshold=64*1024, maxFileSize=1*1024*1024, maxRequestSize=1*1024*1024)
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public UploadServlet() {
        super();
    }

   
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String name = Deployment.getFormField(request, "name").trim();
		String pin = Deployment.getFormField(request, "pin").trim();
		String kind = Deployment.getFormField(request, "kind").trim().toLowerCase();
		String Width = Deployment.getFormField(request, "width");
		String Height = Deployment.getFormField(request, "height");
		int width = Integer.parseInt(Width);
		int height = Integer.parseInt(Height);
		boolean replace = Deployment.getFormField(request, "replace").trim().toLowerCase().contentEquals("replace");
		Part file = request.getPart("file");
		if(name.length() == 0 || pin.length() == 0 || file == null || file.getSize() == 0) {
			response.sendError(400);
			return;
		}
		double sessionId = Deployment.DATA_PROVIDER.verify(pin);
		if(Double.isNaN(sessionId) || sessionId <= 0) {
			response.sendError(404);
		}
		String mimeType = file.getContentType();
		if(mimeType == null || mimeType.length() == 0) {
			mimeType = "application/octet-stream";
		}
		String filename = Deployment.filenameFromContentDisposition(file.getHeader("Content-Disposition"));
		if(filename == null) {
			filename = String.format("upload.%x%s", System.currentTimeMillis(), Deployment.extensionForMimeType(mimeType));
		}
		StoredFileInfo storedFile = Deployment.STORAGE_PROVIDER.storeFile(file.getInputStream(), file.getSize(), mimeType, filename, Deployment.urlbase(request.getRequestURL().toString()));
		if(storedFile == null) {
			response.sendError(500);
			return;
		}
		double uploadId = Deployment.DATA_PROVIDER.upload(sessionId, storedFile.href, storedFile.token, name, kind, mimeType, (double)System.currentTimeMillis(), width, height, replace);
		if(Double.isNaN(uploadId) || uploadId <= 0) {
			LinkedList<String> tokens = new LinkedList<String>();
			tokens.add(storedFile.token);
			Deployment.STORAGE_PROVIDER.deleteFiles(tokens, false);
		}
		response.setStatus(200);
		response.setContentType("text/plain");
		response.getWriter().write(storedFile.href);
		return;
	}
}
