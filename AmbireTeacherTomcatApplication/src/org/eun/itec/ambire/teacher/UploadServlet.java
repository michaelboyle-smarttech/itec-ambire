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
