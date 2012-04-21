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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ParameterList;
import org.openid4java.server.RealmVerifierFactory;
import org.openid4java.util.HttpFetcherFactory;

public class OpenIDSingleSignOnProvider implements SingleSignOnProvider {

	class ProviderUrls {
		public String login;
		public String logout;
	}
	private static final String DISCOVERY_SESSION_ATTRIBUTE = "OpenIDSingleSignOnProvider.discovery";
	private static final String LOGOUTURL_SESSION_ATTRIBUTE = "OpenIDSingleSignOnProvider.logoutUrl";
	ConsumerManager m_consumer;
	TreeMap<String,ProviderUrls> m_providerUrls;
	TreeMap<String,List<DiscoveryInformation>> m_discoveries;
	
	private static HttpFetcherFactory createHttpFetcherFactory() {
		try {
			SSLContext context = SSLContext.getInstance("SSLv3");
			TrustManager tm = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
	
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
	
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			context.init(null, new TrustManager[] { tm }, null);
			return new HttpFetcherFactory(context, new AllowAllHostnameVerifier());
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public OpenIDSingleSignOnProvider(ServletContext context) {
		Log _log = LogFactory.getLog(OpenIDSingleSignOnProvider.class);
		m_consumer = new ConsumerManager(new RealmVerifierFactory(new YadisResolver(createHttpFetcherFactory())), new Discovery(), createHttpFetcherFactory());
		m_providerUrls = new TreeMap<String,ProviderUrls>();
		m_discoveries = new TreeMap<String,List<DiscoveryInformation>>();
		String providers = Deployment.getParameter(context, "OpenIDSingleSignOnProvider.PROVIDER_URLS", "@gmail.com -> login=https://www.google.com/accounts/o8/id, logout=https://www.google.com/accounts/Logout\n@yahoo.com -> login=https://me.yahoo.com, logout=https://login.yahoo.com/config/login?logout=1");
		int N = providers.length();
		for(int startIndex = 0; startIndex < N; ++startIndex) {
			int newline = providers.indexOf('\n', startIndex);
			if(newline < startIndex) {
				newline = N - 1;
			}
			String provider = providers.substring(startIndex, newline);
			startIndex = newline;
			int M = provider.length();
			int arrow = provider.indexOf(" -> ");
			if(arrow < 0 || arrow > (M - 4)) {
				continue;
			}
			ProviderUrls urls = new ProviderUrls();
			for(String url : provider.substring(arrow + 4).split(",")) {
				url = url.trim();
				if(url.startsWith("login=")) {
					String login = url.substring(6);
					List<DiscoveryInformation> list = null;
					try {
						list = m_consumer.discover(login);
					} catch (DiscoveryException e) {
						e.printStackTrace();
					}
					if(list != null && list.size() > 0) {
						m_discoveries.put(login, list);
						urls.login = login;
					}
				} else if(url.startsWith("logout=")) {
					urls.logout = url.substring(7);
				} else {
					_log.info(String.format("Badly formatted OpenIDSingleSignOnProvider.PROVIDER_URLS: %s", provider));
					continue;
				}
			}
			if(!urls.login.isEmpty()) {
				for(String domain : provider.substring(0, arrow).split(",")) {
					domain = domain.trim();
					m_providerUrls.put(domain, urls);
				}
			}
		}
		return;
	}
	
	@Override
	public void close() {
	}

	@Override
	public boolean requiresRedirection() {
		return true;
	}

	private ProviderUrls providerUrlsForIdentity(String identity) {
		if(!identity.startsWith("http://") && !identity.startsWith("https://")) {
			int atsign = identity.lastIndexOf('@');
			if(atsign > 0 && atsign < identity.length() - 1) {
				String domain = identity.substring(atsign);
				if(m_providerUrls.containsKey(domain)) {
					return m_providerUrls.get(domain);
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<DiscoveryInformation> discoveriesForIdentity(String identity) {
		String provider = identity;
		ProviderUrls urls = providerUrlsForIdentity(identity);
		if(urls != null) {
			try {
				provider = Deployment.deasterisk(urls.login, URLEncoder.encode(identity.substring(0, identity.indexOf('@') - 1), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if(m_discoveries.containsKey(provider)) {
			return m_discoveries.get(provider);
		} else {
			try {
				return m_consumer.discover(provider);
			} catch (DiscoveryException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public String startSignOn(String identity, String returnToUrl, HttpSession session, Map<SingleSignOnProvider.Attribute,SingleSignOnProvider.InfoLevel> infoLevels) {
		try {
			ProviderUrls providerUrls = providerUrlsForIdentity(identity);
			if(providerUrls != null) {
				session.setAttribute(LOGOUTURL_SESSION_ATTRIBUTE, providerUrls.logout);
			}
			DiscoveryInformation discovery = m_consumer.associate(discoveriesForIdentity(identity));
			session.setAttribute(DISCOVERY_SESSION_ATTRIBUTE, discovery);
			AuthRequest req = m_consumer.authenticate(discovery, returnToUrl);
			return req.getDestinationUrl(true);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Map<SingleSignOnProvider.Attribute,String> finishSignOn(String requestUrl, Map<String,String[]> parameters, HttpSession session) {
		try {
			DiscoveryInformation discovery = (DiscoveryInformation)session.getAttribute(DISCOVERY_SESSION_ATTRIBUTE);
			session.removeAttribute(DISCOVERY_SESSION_ATTRIBUTE);
			VerificationResult verification = m_consumer.verify(requestUrl, new ParameterList(parameters), discovery);
			Identifier verifiedId = verification.getVerifiedId();
			return new WithMap<SingleSignOnProvider.Attribute,String>()
					.with(SingleSignOnProvider.Attribute.IDENTITY,verifiedId.getIdentifier());
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String signOff(HttpSession session) {
		String logoutUrl = (String)session.getAttribute(LOGOUTURL_SESSION_ATTRIBUTE);
		session.removeAttribute(LOGOUTURL_SESSION_ATTRIBUTE);
		session.removeAttribute(DISCOVERY_SESSION_ATTRIBUTE);
		return logoutUrl;
	}
}
