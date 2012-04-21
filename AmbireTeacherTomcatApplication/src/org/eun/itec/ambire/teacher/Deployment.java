package org.eun.itec.ambire.teacher;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;


public class Deployment implements ServletContextListener {
	public static double MIN_SESSION_AGE_MILLIS = 2 * 60 * 1000; // two minutes
	public static double MIN_GARBAGE_COLLECTION_INTERVAL_MILLIS = 6 * 1000; // six seconds
	public static double MAX_GARBAGE_COLLECTION_INTERVAL_MILLIS = 60 * 1000; // one minute
	public static double MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS = 1000; // sessions must have an age is that is at least one second longer than uploads
    public static double MAX_SESSION_AGE_MILLIS = 3 * 60 * 60 * 1000; // three hours
    public static double MAX_UPLOAD_AGE_MILLIS = 20 * 60 * 1000; // twenty minutes
	public static StorageProvider STORAGE_PROVIDER;
	public static DataProvider DATA_PROVIDER;
	public static SingleSignOnProvider SSO_PROVIDER;
	private Thread m_thread;
    private static int g_pinseq = (int)Math.floor(Math.random() * 900);

	public static String getParameter(ServletContext context, String paramName, String defaultValue) {
		String s = context.getInitParameter(paramName);
		if(s == null) {
			return defaultValue;
		} else {
			return s;
		}
	}

	public Deployment() {
    }

	public void contextInitialized(ServletContextEvent ev) {
		ServletContext context = ev.getServletContext();
		try {
			double d = Math.floor(Double.parseDouble(context.getInitParameter("Deployment.MAX_SESSION_AGE_MILLIS")));
			if(!Double.isNaN(d) && !Double.isInfinite(d) && d >= MIN_SESSION_AGE_MILLIS) {
				MAX_SESSION_AGE_MILLIS = Math.max(d, MIN_SESSION_AGE_MILLIS);
				if((MAX_SESSION_AGE_MILLIS - MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS) < MAX_UPLOAD_AGE_MILLIS) {
					MAX_UPLOAD_AGE_MILLIS = MAX_SESSION_AGE_MILLIS - MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS;
				}
			}
		} catch(Exception e) {}
		try {
			double d = Math.floor(Double.parseDouble(context.getInitParameter("Deployment.MAX_UPLOAD_AGE_MILLIS")));
			if(!Double.isNaN(d) && !Double.isInfinite(d) && d >= (MIN_SESSION_AGE_MILLIS - MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS)) {
				MAX_UPLOAD_AGE_MILLIS = Math.max(d, (MIN_SESSION_AGE_MILLIS - MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS));
				if((MAX_UPLOAD_AGE_MILLIS + MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS) > MAX_SESSION_AGE_MILLIS) {
					MAX_SESSION_AGE_MILLIS = MAX_UPLOAD_AGE_MILLIS + MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS;
				}
			}
		} catch(Exception e) {}
		try {
			double d = Double.parseDouble(context.getInitParameter("Deployment.MAX_GARBAGE_COLLECTION_INTERVAL_MILLIS"));
			if(!Double.isNaN(d) && !Double.isInfinite(d) && d >= MIN_GARBAGE_COLLECTION_INTERVAL_MILLIS) {
				MAX_GARBAGE_COLLECTION_INTERVAL_MILLIS = Math.floor(Math.max(MIN_GARBAGE_COLLECTION_INTERVAL_MILLIS, Math.min(d, MIN_DELTA_SESSION_UPLOAD_AGE_MILLIS / 3)));
			}
		} catch(Exception e) {}
		AmazonWebServices.init(context);
		try {
			String className = context.getInitParameter("Deployment.STORAGE_PROVIDER");
			if(className != null) {
				STORAGE_PROVIDER = (StorageProvider)Class.forName(className).getConstructor(new Class[]{ServletContext.class}).newInstance(new Object[]{context});
			}
		} catch(Exception e) {}
		if(STORAGE_PROVIDER == null) {
			STORAGE_PROVIDER = new LocalFileStorageProvider(context);
		}
		try {
			String className = context.getInitParameter("Deployment.DATA_PROVIDER");
			if(className != null) {
				DATA_PROVIDER = (DataProvider)Class.forName(className).getConstructor(new Class[]{ServletContext.class}).newInstance(new Object[]{context});
			}
		} catch(Exception e) {}
		if(DATA_PROVIDER == null) {
			DATA_PROVIDER = new SqlDataProvider(context);
		}
		try {
			String className = context.getInitParameter("Deployment.SSO_PROVIDER");
			if(className != null) {
				SSO_PROVIDER = (SingleSignOnProvider)Class.forName(className).getConstructor(new Class[]{ServletContext.class}).newInstance(new Object[]{context});
			}
		} catch(Exception e) {}
		if(SSO_PROVIDER == null) {
			SSO_PROVIDER = new OpenIDSingleSignOnProvider(context);
		}
		m_thread = new Thread(new Runnable() {
			public void run() {
				try {
					while(!m_thread.isInterrupted()) {
						DATA_PROVIDER.collectGarbage();
						Thread.sleep((long)MAX_GARBAGE_COLLECTION_INTERVAL_MILLIS);
					}
				} catch(InterruptedException ie) {}
			}
		});
		m_thread.setDaemon(true);
		m_thread.start();
	}
	
	public void contextDestroyed(ServletContextEvent ev) {
		m_thread.interrupt();
		try {
			m_thread.join();
		} catch(Exception e) {
		} finally {
			m_thread = null;
		}
		if(STORAGE_PROVIDER != null) {
			try {
				STORAGE_PROVIDER.close();
			} catch(Exception e) {}
			STORAGE_PROVIDER = null;
		}
		if(DATA_PROVIDER != null) {
			try {
				DATA_PROVIDER.close();
			} catch(Exception e) {}
			DATA_PROVIDER = null;
		}
	}
	
    public static String extensionForMimeType(String mimeType) {
    	mimeType = mimeType.trim().toLowerCase();
    	if(mimeType.contentEquals("image/jpeg")) {
    		return ".jpg";
    	} else if(mimeType.contentEquals("image/gif")) {
    		return ".gif";
    	} else if(mimeType.contentEquals("image/png")) {
    		return ".png";
    	} else {
    		return "";
    	}
    }
   
    public static String urlbase(String url) {
    	int colonSlashSlash = url.indexOf("://") + 2;
    	int lastSlash = url.lastIndexOf('/');
    	if(lastSlash > colonSlashSlash) {
    		url = url.substring(0, lastSlash + 1);
    	}
    	if(url.charAt(url.length() - 1) != '/') {
    		url = url + "/";
    	}
    	return url;
    }
    
    public static String filenameFromContentDisposition(String contentDisposition) {
    	for(String s : contentDisposition.split(";")) {
    		String[] p = s.split("=");
    		if(p.length > 1 && p[0].trim().toLowerCase().contentEquals("filename")) {
				StringBuffer q = new StringBuffer(p[1]);
				for(int i = 2; i < p.length; ++i) {
					q.append('=');
					q.append(p[i]);
				}
				if(q.charAt(0) == '\"' && q.charAt(q.length() - 1) == '\"') {
					q.deleteCharAt(0);
					q.deleteCharAt(q.length() - 1);
				}
				return q.toString().trim();
    		}
    	}
    	return null;
    }
    
    public static String getFormField(HttpServletRequest request, String name) {
    	StringBuffer buf = new StringBuffer();
		try {
			Part p = request.getPart(name);
	    	if(p != null) {
		    	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    	while(true) {
		    		String line = br.readLine();
		    		if(line == null) {
		    			break;
		    		} else {
		    			if(buf.length() > 0) {
		    				buf.append("\n");
		    			}
		    			buf.append(line);
		    		}
		    	}
	    	}
		} catch(Exception e) {
			e.printStackTrace();
		}
    	return buf.toString();
    }

    public static String suggestPin(String owner, int seed) {
        g_pinseq = (g_pinseq + 1) % 900;
        int n = (g_pinseq + 100) * 1000 + (owner.hashCode() + seed) % 1000;
        return Integer.toString(n);
    }
    
    public static String requesturl(String requestUrl, String queryString) {
    	if(queryString == null || queryString.length() == 0) {
    		return requestUrl;
    	} else {
    		StringBuffer b = new StringBuffer();
    		b.append(requestUrl);
    		b.append('?');
    		b.append(queryString);
    		return b.toString();
    	}
    }
    
    public static String deasterisk(String pattern, String replacement) {
		int asterisk = pattern.indexOf('*');
		if(asterisk != -1) {
			StringBuffer buf = new StringBuffer();
			if(asterisk > 0) {
				buf.append(pattern, 0, asterisk);
			}
			buf.append(replacement);
			int N = pattern.length();
			if(asterisk < N - 1) {
				buf.append(pattern, asterisk + 1, N);
			}
			return buf.toString();
		} else {
			return pattern;
		}
    }
}
