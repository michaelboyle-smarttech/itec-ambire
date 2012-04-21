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

import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.ServletContext;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchDeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeletableItem;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;

public class AmazonSimpleDBDataProvider implements DataProvider {

	@SuppressWarnings("unused")
	private String ACCOUNT_ID;
	private String ACCESS_KEY;
	private String SECRET_KEY;
	private String DOMAIN;
	private String ENDPOINT;
	private AmazonSimpleDBClient m_client;
	
	@SuppressWarnings("serial")
	private class WithList<T> extends LinkedList<T> {
		public WithList() {
			super();
		}
		public WithList<T> with(T elem) {
			add(elem);
			return this;
		}
	}
	
	public AmazonSimpleDBDataProvider(ServletContext context) {
		ACCOUNT_ID = Deployment.getParameter(context, "AmazonSimpleDBDataProvider.ACCOUNT_ID", AmazonWebServices.ACCOUNT_ID);
		ACCESS_KEY = Deployment.getParameter(context, "AmazonSimpleDBDataProvider.ACCESS_KEY", AmazonWebServices.ACCESS_KEY);
		SECRET_KEY = Deployment.getParameter(context, "AmazonSimpleDBDataProvider.SECRET_KEY", AmazonWebServices.SECRET_KEY);
		DOMAIN = Deployment.getParameter(context, "AmazonSimpleDBDataProvider.DOMAIN", "itecambire");
		ENDPOINT = Deployment.getParameter(context, "AmazonS3StorageProvider.ENDPOINT", "sdb." + AmazonWebServices.ENDPOINT);
		AmazonSimpleDBClient db = open();
		if(db != null) {
			try {
				boolean sessions = false, uploads = false;
				for(String domainName : db.listDomains().getDomainNames()) {
					if(!sessions && domainName.contentEquals(DOMAIN + ".sessions")) {
						sessions = true;
					} else if(!uploads && domainName.contentEquals(DOMAIN + ".uploads")) {
						uploads = true;
					}
				}
				if(!sessions) {
					db.createDomain(new CreateDomainRequest(DOMAIN + ".sessions"));
				}
				if(!uploads) {
					db.createDomain(new CreateDomainRequest(DOMAIN + ".uploads"));
				}
				db.putAttributes(new PutAttributesRequest(DOMAIN + ".sessions", "_1", new WithList<ReplaceableAttribute>()
					.with(new ReplaceableAttribute("sessionId", "1", true))
					.with(new ReplaceableAttribute("pin", "8675309", true))
					.with(new ReplaceableAttribute("owner", "michaelboyle.smarttech@gmail.com", true))
					.with(new ReplaceableAttribute("timestamp", String.format("%d", System.currentTimeMillis() + 120 * 24 * 60 * 60 * 1000), true))
				));
				m_client = db;
			} catch(Exception e) {
				close(db);
			}
		}
	}
	
	private AmazonSimpleDBClient open() {
		AmazonSimpleDBClient db = new AmazonSimpleDBClient(new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY));
		try {
			if(ENDPOINT != null) {
				db.setEndpoint(ENDPOINT);
			}
			return db;
		} catch(Exception e) {
			close(db);
		}
		return null;
	}
	
	private static void close(AmazonSimpleDBClient db) {
		if(db != null) {
			try {
				db.shutdown();
			} catch(Exception e) {}
		}
	}

	private static String stringValue(int index, Item item, String defaultValue) {
		if(item != null) {
			for(Attribute a : item.getAttributes()) {
				if(index <= 0) {
					return a.getValue();
				} else {
					--index;
				}
			}
		}
		return defaultValue;
	}

	private static String stringValue(String name, Item item, String defaultValue) {
		if(item != null) {
			for(Attribute a : item.getAttributes()) {
				if(a.getName().contentEquals(name)) {
					return a.getValue();
				}
			}
		}
		return defaultValue;
	}

	@SuppressWarnings("unused")
	private static String stringValue(int index, Item item) {
		return stringValue(index, item, null);
	}
	private static String stringValue(String name, Item item) {
		return stringValue(name, item, null);
	}

	private static String stringValue(int index, SelectResult result, String defaultValue) {
		if(result != null) {
			List<Item> items = result.getItems();
			if(!items.isEmpty()) {
				return stringValue(index, items.get(0), defaultValue);
			}
		}
		return defaultValue;
	}

	private static String stringValue(String name, SelectResult result, String defaultValue) {
		if(result != null) {
			List<Item> items = result.getItems();
			if(!items.isEmpty()) {
				return stringValue(name, items.get(0), defaultValue);
			}
		}
		return defaultValue;
	}

	@SuppressWarnings("unused")
	private static String stringValue(int index, SelectResult result) {
		return stringValue(index, result, null);
	}

	private static String stringValue(String name, SelectResult result) {
		return stringValue(name, result, null);
	}
	
	private static List<String> stringValues(String name, SelectResult result) {
		LinkedList<String> rv = new LinkedList<String>();
		for(Item item : result.getItems()) {
			for(Attribute a : item.getAttributes()) {
				if(a.getName().contentEquals(name)) {
					rv.add(a.getValue());
					break;
				}
			}
		}
		return rv;
	}
	
	private static List<Double> doubleValues(String name, SelectResult result) {
		LinkedList<Double> rv = new LinkedList<Double>();
		for(Item item : result.getItems()) {
			for(Attribute a : item.getAttributes()) {
				if(a.getName().contentEquals(name)) {
					try {
						rv.add(Double.parseDouble(a.getValue()));
					} catch(Exception e) {}
					break;
				}
			}
		}
		return rv;
	}
	
	private static List<Double> uniqueDoubleValues(List<Double> list) {
		TreeSet<Double> set = new TreeSet<Double>();
		set.addAll(list);
		return new LinkedList<Double>(set);
	}

	private static List<String> uniqueStringValues(List<String> list) {
		TreeSet<String> set = new TreeSet<String>();
		set.addAll(list);
		return new LinkedList<String>(set);
	}

	private static double doubleValue(int index, Item item, double defaultValue) {
		String n = stringValue(index, item, null);
		if(n != null) {
			try {
				return Double.parseDouble(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	private static double doubleValue(String name, Item item, double defaultValue) {
		String n = stringValue(name, item, null);
		if(n != null) {
			try {
				return Double.parseDouble(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	@SuppressWarnings("unused")
	private static double doubleValue(int index, Item item) {
		return doubleValue(index, item, Double.NaN);
	}

	private static double doubleValue(String name, Item item) {
		return doubleValue(name, item, Double.NaN);
	}

	private static int intValue(int index, Item item, int defaultValue) {
		String n = stringValue(index, item, null);
		if(n != null) {
			try {
				return Integer.parseInt(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	private static int intValue(String name, Item item, int defaultValue) {
		String n = stringValue(name, item, null);
		if(n != null) {
			try {
				return Integer.parseInt(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	@SuppressWarnings("unused")
	private static int intValue(int index, Item item) {
		return intValue(index, item, 0);
	}

	private static int intValue(String name, Item item) {
		return intValue(name, item, 0);
	}

	private static double doubleValue(int index, SelectResult result, double defaultValue) {
		String n = stringValue(index, result, null);
		if(n != null) {
			try {
				return Double.parseDouble(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	private static double doubleValue(String name, SelectResult result, double defaultValue) {
		String n = stringValue(name, result, null);
		if(n != null) {
			try {
				return Double.parseDouble(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	@SuppressWarnings("unused")
	private static double doubleValue(int index, SelectResult result) {
		return doubleValue(index, result, Double.NaN);
	}

	private static double doubleValue(String name, SelectResult result) {
		return doubleValue(name, result, Double.NaN);
	}

	private static int intValue(int index, SelectResult result, int defaultValue) {
		String n = stringValue(index, result, null);
		if(n != null) {
			try {
				return Integer.parseInt(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	private static int intValue(String name, SelectResult result, int defaultValue) {
		String n = stringValue(name, result, null);
		if(n != null) {
			try {
				return Integer.parseInt(n);
			} catch(Exception e) {}
		}
		return defaultValue;
	}

	@SuppressWarnings("unused")
	private static int intValue(int index, SelectResult result) {
		return intValue(index, result, 0);
	}

	@SuppressWarnings("unused")
	private static int intValue(String name, SelectResult result) {
		return intValue(name, result, 0);
	}
	
	@Override
	public double verify(String pin) {
		double sessionId = Double.NaN;
		try {
			String expr = String.format("SELECT sessionId FROM `%s.sessions` WHERE pin = \'%s\'", DOMAIN, pin);
			SelectResult result = m_client.select(new SelectRequest(expr));
			sessionId = doubleValue("sessionId", result);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return sessionId;
	}

	@Override
	public String selectPin(String owner) {
		String pin = null;
		try {
			pin = stringValue("pin", m_client.select(new SelectRequest(String.format("SELECT pin FROM `%s.sessions` WHERE owner = \'%s\'", DOMAIN, owner))));
		} catch(Exception e) {}
		if(pin == null) {
			try {
				for(int seed = 1; true; ++seed) {
					String suggestion = Deployment.suggestPin(owner, seed);
					int count = intValue(0, m_client.select(new SelectRequest(String.format("SELECT count(*) from `%s.sessions` WHERE pin = \'%s\'", DOMAIN, suggestion))), -1);
					if(count == 0) {
						pin = suggestion;
						break;
					}
				}
			} catch(Exception e) {}
		}
		return pin;
	}

	@Override
	public double signIn(String owner, String pin, double timestamp) {
		double sessionId = Double.NaN;
		try {
			double i = AmazonWebServices.identity();
			String d = String.format("%d", (long)i);
			m_client.putAttributes(new PutAttributesRequest(DOMAIN + ".sessions",  "_" + d, new WithList<ReplaceableAttribute>()
					.with(new ReplaceableAttribute("sessionId", d, true))
					.with(new ReplaceableAttribute("pin", pin, true))
					.with(new ReplaceableAttribute("owner", owner, true))
					.with(new ReplaceableAttribute("timestamp", String.format("%d", System.currentTimeMillis()), true))));
			sessionId = i;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return sessionId;
	}
	
	@Override
	public void keepSignedIn(double sessionId, double timestamp) {
		try {
			String d = String.format("%d", (long)sessionId);
			m_client.putAttributes(new PutAttributesRequest(DOMAIN + ".sessions",  "_" + d, new WithList<ReplaceableAttribute>()
					.with(new ReplaceableAttribute("timestamp", String.format("%d", System.currentTimeMillis()), true))));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	@Override
	public void signOut(double sessionId) {
		if(!Double.isNaN(sessionId)) {
			LinkedList<String> tokens = new LinkedList<String>();
			try {
				String d = String.format("%d", (long)sessionId);
				m_client.deleteAttributes(new DeleteAttributesRequest(DOMAIN + ".sessions", "_" + d));
				for(Item item : m_client.select(new SelectRequest(String.format("SELECT token FROM `%s.uploads` WHERE sessionId = \'%s\'", DOMAIN, d))).getItems()) {
					String token = stringValue("token", item);
					if(token != null) {
						tokens.add(token);
					}
				}
				m_client.deleteAttributes(new DeleteAttributesRequest(DOMAIN + ".uploads", "_" + d));
			} catch(Exception e) {
				e.printStackTrace();
			}
			Deployment.STORAGE_PROVIDER.deleteFiles(tokens, true);
		}
	}

	@Override
	public double upload(double sessionId, String href, String token, String name, String kind, String mimeType, double timestamp, int width, int height, boolean replace) {
		double uploadId = Double.NaN;
		try {
		  double uid = Double.NaN;
		  if(replace) {
		    try {
		    	uid = doubleValue("uploadId", m_client.select(new SelectRequest(String.format("SELECT uploadId FROM `%s.uploads` WHERE sessionId = \'%d\' AND kind = %s", DOMAIN, (long)sessionId, SimpleDBUtils.quoteValue(kind)))));
		    } catch(Exception e) {
		    	e.printStackTrace();
		    }
		  }
		  if(Double.isNaN(uid) || (uid == 0)) {
		    uid = AmazonWebServices.identity();
		    replace = false;
		  }
		  m_client.putAttributes(new PutAttributesRequest()
		    .withDomainName(DOMAIN + ".uploads")
		    .withItemName(String.format("_%d", (long)uid))
		    .withAttributes(
		      new ReplaceableAttribute("sessionId", String.format("%d", (long)sessionId), replace),
		      new ReplaceableAttribute("uploadId", String.format("%d", (long)uid), replace),
		      new ReplaceableAttribute("href", href, replace),
		      new ReplaceableAttribute("token", token, replace),
		      new ReplaceableAttribute("name", name, replace),
		      new ReplaceableAttribute("kind", kind, replace),
		      new ReplaceableAttribute("mimeType", mimeType, replace),
		      new ReplaceableAttribute("timestamp", String.format("%d", (long)timestamp), replace),
		      new ReplaceableAttribute("width", String.format("%d", width), replace),
		      new ReplaceableAttribute("height", String.format("%d", height), replace)
		      
		    ));
		    uploadId = uid;
		} catch(Exception e) {
		  e.printStackTrace();
		}
		return uploadId;
	}

	@Override
	public List<UploadInfo> selectUploads(double sessionId, double sinceTimestamp) {
		LinkedList<UploadInfo> uploads = new LinkedList<UploadInfo>();
		if(!Double.isNaN(sessionId)) {
			if(Double.isNaN(sinceTimestamp)) {
				sinceTimestamp = 0;
			}
			try {
				String d = String.format("%d", (long)sessionId);
				String t = String.format("%d", (long)sinceTimestamp);
				for(Item item : m_client.select(new SelectRequest(String.format("SELECT * FROM `%s.uploads` WHERE sessionId = \'%s\' AND timestamp > \'%s\'", DOMAIN, d, t))).getItems()) {
					UploadInfo u = new UploadInfo();
					u.uploadId = doubleValue("uploadId", item);
					u.href = stringValue("href", item);
					u.kind = stringValue("kind", item);
					u.name = stringValue("name", item);
					u.timestamp = doubleValue("timestamp", item);
					u.width = intValue("width", item);
					u.height = intValue("height", item);
					uploads.add(u);
				}
			} catch(Exception e) {}
		}
		return uploads;
	}

	@Override
	public void collectGarbage() {
		try {
			double now = System.currentTimeMillis();
			SelectResult sessions = m_client.select(new SelectRequest(String.format("SELECT sessionId FROM `%s.sessions` WHERE timestamp < \'%d\'", DOMAIN, (long)(now - Deployment.MAX_SESSION_AGE_MILLIS))));
			LinkedList<DeletableItem> items = new LinkedList<DeletableItem>();
			for(double sessionId : uniqueDoubleValues(doubleValues("sessionId", sessions))) {
			  items.add(new DeletableItem().withName(String.format("_%d", (long)sessionId)));
			}
			if(!items.isEmpty()) {
				m_client.batchDeleteAttributes(new BatchDeleteAttributesRequest()
				  .withDomainName(DOMAIN + ".sessions")
				  .withItems(items));
			}
			SelectResult uploads = m_client.select(new SelectRequest(String.format("SELECT uploadId,token FROM `%s.uploads` WHERE timestamp < \'%d\'", DOMAIN, (long)(now - Deployment.MAX_UPLOAD_AGE_MILLIS))));
			List<String> tokens = uniqueStringValues(stringValues("token", uploads));
			items = new LinkedList<DeletableItem>();
			for(Double uploadId : uniqueDoubleValues(doubleValues("uploadId", uploads))) {
			  items.add(new DeletableItem().withName(String.format("_%d", (long)uploadId.doubleValue())));
			}
			if(!items.isEmpty()) {
				m_client.batchDeleteAttributes(new BatchDeleteAttributesRequest()
				  .withDomainName(DOMAIN + ".uploads")
				  .withItems(items));
			}
			if(!tokens.isEmpty()) {
				Deployment.STORAGE_PROVIDER.deleteFiles(tokens, false /* do not force */);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		AmazonSimpleDBClient db = m_client;
		m_client = null;
		close(db);
	}

}
