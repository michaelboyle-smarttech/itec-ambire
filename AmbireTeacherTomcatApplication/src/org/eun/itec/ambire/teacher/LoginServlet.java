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
