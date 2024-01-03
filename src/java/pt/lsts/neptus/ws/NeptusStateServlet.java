/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.ws;

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
