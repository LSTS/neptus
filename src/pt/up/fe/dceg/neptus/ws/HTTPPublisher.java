/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: HTTPPublisher.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.ws;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import pt.up.fe.dceg.neptus.NeptusLog;

public class HTTPPublisher {
	
	private URL serverURL;

	public HTTPPublisher(URL serverURL) {
		setServerURL(serverURL);
	}
	
	/**
	 * Server URL defaults to "http://whale.fe.up.pt/neptusWS/update.php"
	 */
	public HTTPPublisher() {
		try {
			serverURL = new URL("http://whale.fe.up.pt/neptusWS/update.php");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
    private void publish(String type, String xml) {
		HttpClient client = new DefaultHttpClient();
//		client.getHostConfiguration().setHost(serverURL.getHost(), serverURL.getPort(), serverURL.getProtocol());
		HttpHost target = new HttpHost(serverURL.getHost(), serverURL.getPort(), serverURL.getProtocol());
		HttpPost post = new HttpPost(serverURL.getPath());
		NameValuePair nvp_type = new BasicNameValuePair("type", type);
		NameValuePair nvp_xml = new BasicNameValuePair("xml", xml);
//		post.setRequestBody(new NameValuePair[] {nvp_type, nvp_xml});
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(nvp_type);
        nvps.add(nvp_xml);
		try {
			post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
//			client.executeMethod(post);
			HttpResponse res = client.execute(target, post);
			
			NeptusLog.pub().info("HTTP Response: " +
					//post.getResponseBodyAsString()
					EntityUtils.toString(res.getEntity()));			
		}
		catch (Exception e) {
            NeptusLog.pub().error("HTTP Response: "+e.getMessage());
		}
		finally {
//			post.releaseConnection();
			post.abort();
			client.getConnectionManager().shutdown();
		}
	}
	
	public void publishPlan(String xml) {
		publish("plan", xml);
		NeptusStateServlet.setPlanXML(xml);
	}
	
	public void publishState(String xml) {
		publish("state", xml);
		NeptusStateServlet.setStateXML(xml);
	}
	
	public void publishWSN(String xml) {
		publish("wsn", xml);
		NeptusStateServlet.setWsnXML(xml);
	}
	
	public static void main(String[] args) throws Exception {
		new HTTPPublisher(new URL("http://127.0.0.1:8080/state")).publishPlan("<?xml version=\"1.0\" ?><t>\nIsto é um teste</t>");
	}

	public URL getServerURL() {
		return serverURL;
	}

	public void setServerURL(URL serverURL) {
		this.serverURL = serverURL;
	}
}
