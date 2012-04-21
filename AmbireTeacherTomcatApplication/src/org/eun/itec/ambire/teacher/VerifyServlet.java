package org.eun.itec.ambire.teacher;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/verify")
public class VerifyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public VerifyServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean exists = false;
		String pin = request.getParameter("p") != null ? request.getParameter("p").trim() : "";
		if(pin.length() > 0) {
			double sessionId = Deployment.DATA_PROVIDER.verify(pin);
			exists = (!Double.isNaN(sessionId) && sessionId > 0);
		}
		response.setStatus(200);
		response.setContentType("text/plain");
		response.getWriter().write(exists ? "true" : "false");
	}

}
