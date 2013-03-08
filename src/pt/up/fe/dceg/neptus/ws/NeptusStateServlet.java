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
 * $Id:: NeptusStateServlet.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.ws;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;

public class NeptusStateServlet implements Servlet {

	
	private static String stateXML, wsnXML, planXML;
	
	public void destroy() {
		// TODO Auto-generated method stub

	}

	public ServletConfig getServletConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getServletInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	public void init(ServletConfig arg0) throws ServletException {
		// TODO Auto-generated method stub

	}

	public void service(ServletRequest arg0, ServletResponse arg1)
	throws ServletException, IOException {


		Request request = arg0 instanceof Request?(Request)arg0:HttpConnection.getCurrentConnection().getRequest();
		String path = request.getUri().toString();
		
		OutputStream out = arg1.getOutputStream();
		BufferedWriter html = new BufferedWriter(new OutputStreamWriter(out));
		arg1.setContentType("text/xml");
		
		
		if (path.equalsIgnoreCase("/state.xml") && stateXML != null) {
			html.write(stateXML);
		}		
		else if (path.equalsIgnoreCase("/wsn.xml") && wsnXML != null) {
			html.write(wsnXML);
		}
		
		else if (path.equalsIgnoreCase("/plan.xml") && planXML != null) {
			html.write(planXML);
		}
		else {
			html.write("<?xml version=\"1.0\"?>\n");
			html.write("<ERROR>The path '"+path+"' cannot be serviced.</ERROR>\n");	
		}
		
		html.close();
		out.close();

	}

	public static void setStateXML(String stateXML) {
		NeptusStateServlet.stateXML = stateXML;
	}

	public static void setWsnXML(String wsnXML) {
		NeptusStateServlet.wsnXML = wsnXML;
	}

	public static void setPlanXML(String planXML) {
		NeptusStateServlet.planXML = planXML;
	}
}
