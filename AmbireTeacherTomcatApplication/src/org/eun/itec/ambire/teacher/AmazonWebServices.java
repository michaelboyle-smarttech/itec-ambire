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
		ACCOUNT_ID = Deployment.getParameter(context, "AmazonWebServices.ACCOUNT_ID", "8044-4856-6677");
		ACCESS_KEY = Deployment.getParameter(context, "AmazonWebServices.ACCESS_KEY", "AKIAI3V4DAFKSPPMC7GA");
		SECRET_KEY = Deployment.getParameter(context, "AmazonWebServices.SECRET_KEY", "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");
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
