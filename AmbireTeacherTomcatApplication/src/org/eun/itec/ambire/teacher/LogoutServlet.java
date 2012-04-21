package org.eun.itec.ambire.teacher;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public LogoutServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();
		String logoutUrl = null;
		try {
			Deployment.DATA_PROVIDER.signOut(((Double)session.getAttribute("sid")).doubleValue());
		} finally {
			session.removeAttribute("sid");
			session.removeAttribute("pin");
			session.removeAttribute("owner");
			logoutUrl = Deployment.SSO_PROVIDER.signOff(session);
		}
		if(logoutUrl == null) {
			logoutUrl = "/";
		}
		logoutUrl = Deployment.deasterisk(logoutUrl, URLEncoder.encode(Deployment.urlbase(request.getRequestURL().toString()), "UTF-8"));
		response.sendRedirect(logoutUrl);
	}

}
