/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.ws;

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

import pt.lsts.neptus.NeptusLog;

@SuppressWarnings("deprecation")
public class HTTPPublisher {
	
	private URL serverURL;
	private HttpClient client = new DefaultHttpClient();
	
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
	
    private void publish(String type, String xml) {
		
//		client.getHostConfiguration().setHost(serverURL.getHost(), serverURL.getPort(), serverURL.getProtocol());
		HttpHost target = new HttpHost(serverURL.getHost(), serverURL.getPort(), serverURL.getProtocol());
		HttpPost post = new HttpPost(serverURL.getPath());
		NameValuePair nvp_type = new BasicNameValuePair("type", type);
		NameValuePair nvp_xml = new BasicNameValuePair("xml", xml);
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(nvp_type);
        nvps.add(nvp_xml);
		try {
			post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse res = client.execute(target, post);
			
			NeptusLog.pub().info("HTTP Response: " +
					//post.getResponseBodyAsString()
					EntityUtils.toString(res.getEntity()));			
		}
		catch (Exception e) {
            NeptusLog.pub().error("HTTP Response: "+e.getMessage());
		}
		finally {

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
	
	@Override
	protected void finalize() throws Throwable {
	    client.getConnectionManager().shutdown();
	    super.finalize();
	}
}
