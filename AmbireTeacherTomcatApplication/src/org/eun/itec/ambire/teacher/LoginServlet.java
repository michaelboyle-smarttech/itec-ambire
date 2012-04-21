package org.eun.itec.ambire.teacher;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public LoginServlet() {
        super();
    }

    private void login(HttpServletRequest request, HttpServletResponse response, Map<SingleSignOnProvider.Attribute,String> attributes) throws ServletException, IOException {
    	String owner = attributes.get(SingleSignOnProvider.Attribute.IDENTITY);
		String pin = Deployment.DATA_PROVIDER.selectPin(owner);
		double sessionId = Deployment.DATA_PROVIDER.signIn(owner, pin, System.currentTimeMillis());
		HttpSession session = request.getSession();
		session.setAttribute("owner", owner);
		session.setAttribute("sid", sessionId);
		session.setAttribute("pin", pin);
		response.sendRedirect("/slides");
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		login(request, response, Deployment.SSO_PROVIDER.finishSignOn(Deployment.requesturl(request.getRequestURL().toString(), request.getQueryString()), request.getParameterMap(), request.getSession()));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String identity = request.getParameter("email");
		String result = Deployment.SSO_PROVIDER.startSignOn(identity, request.getRequestURL().toString(), request.getSession(), new WithMap<SingleSignOnProvider.Attribute,SingleSignOnProvider.InfoLevel>()
			.with(SingleSignOnProvider.Attribute.IDENTITY,SingleSignOnProvider.InfoLevel.REQUIRED)
		);
		if(Deployment.SSO_PROVIDER.requiresRedirection()) {
			response.sendRedirect(result);
		} else {
			login(request,response,Deployment.SSO_PROVIDER.finishSignOn(result, new WithMap<String,String[]>(), request.getSession()));
		}
	}

}
