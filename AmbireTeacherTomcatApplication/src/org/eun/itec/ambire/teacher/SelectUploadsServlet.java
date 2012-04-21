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
