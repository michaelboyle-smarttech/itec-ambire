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

import java.net.InetAddress;

import javax.servlet.ServletContext;

public final class AmazonWebServices {
	public static String ACCOUNT_ID;
	public static String ACCESS_KEY;
	public static String SECRET_KEY;
	public static String REGION;
	public static String ENDPOINT;
	private static long g_identity;
	private static final long IDENTITY_BIT_MASK = 0xFFFFFFFFFFFFFL;

	public static void init(ServletContext context) {
		ACCOUNT_ID = Deployment.getParameter(context, "AmazonWebServices.ACCOUNT_ID", null);
		ACCESS_KEY = Deployment.getParameter(context, "AmazonWebServices.ACCESS_KEY", null);
		SECRET_KEY = Deployment.getParameter(context, "AmazonWebServices.SECRET_KEY", null);
		REGION = Deployment.getParameter(context, "AmazonWebServices.REGION", "EU_Ireland");
		ENDPOINT = Deployment.getParameter(context, "AmazonWebServices.ENDPOINT", "eu-west-1.amazonaws.com");
		try {
			InetAddress localhost = InetAddress.getLocalHost();
			byte[] addr = localhost.getAddress();
			g_identity = (System.currentTimeMillis() | (addr[addr.length - 1] << 8 | addr[addr.length - 2]) << 41) & IDENTITY_BIT_MASK;
		} catch(Exception e) {
			g_identity = Double.doubleToRawLongBits(Math.random()) & IDENTITY_BIT_MASK;
		}
	}

	public static synchronized double identity() {
		g_identity = (g_identity + 1) & IDENTITY_BIT_MASK;
		return g_identity;
	}

}
