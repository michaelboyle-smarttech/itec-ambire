package org.eun.itec.ambire.teacher;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/select")
public class SelectUploadsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public SelectUploadsServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		double sessionId = Double.NaN;
		if(request.getParameter("s") != null) {
			try {
				sessionId = Double.parseDouble(request.getParameter("s"));
			} catch(Exception e) {}
		} else if(request.getParameter("p") != null) {
			sessionId = Deployment.DATA_PROVIDER.verify(request.getParameter("p"));
		} else if(request.getSession().getAttribute("sid") != null) {
			sessionId = ((Double)request.getSession().getAttribute("sid")).doubleValue();
		}
		if(Double.isNaN(sessionId)) {
			response.sendError(404);
			return;
		}
		boolean keepalive = false;
		String s = request.getParameter("keepalive");
		if(s != null) {
			s = s.trim().toLowerCase();
			if(s.length() > 0) {
				if(!s.contentEquals("false") && !s.contentEquals("0") && !s.contentEquals("no")) { 
					keepalive = true;
				}
			}
		}
		if(keepalive) {
			Deployment.DATA_PROVIDER.keepSignedIn(sessionId, System.currentTimeMillis());
		}
		double timestamp = (request.getParameter("t") != null) ? Double.parseDouble(request.getParameter("t")) : Double.NaN;
		List<UploadInfo> uploads = Deployment.DATA_PROVIDER.selectUploads(sessionId, timestamp);
		if(uploads.isEmpty()) {
			response.setStatus(204);
		} else {
			response.setStatus(200);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(JSON.stringify(uploads));
		}
	}
}
