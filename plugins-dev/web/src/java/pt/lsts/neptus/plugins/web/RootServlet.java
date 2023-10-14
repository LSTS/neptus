/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2010/06/26
 */
package pt.lsts.neptus.plugins.web;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ZP
 *
 */
@NeptusServlet(path="/", name="Service Listing")
public class RootServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
	protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
			throws ServletException, IOException {
		arg1.setContentType("text/html");
		arg1.getWriter().write("<html><head><title>Neptus Web Services</title></head><body><h1>Neptus Web Services</h1>");
		
		for (String path : WebServer.installedServlets.keySet()) {
			arg1.getWriter().write("<li><a href="+path.replaceAll("\\*", "")+">"+getServletName(WebServer.installedServlets.get(path))+"</a></li>");
		}
		arg1.getWriter().write("</body></html>");
		arg1.getWriter().close();
	}
	
	String getServletName(Servlet servlet) {
		NeptusServlet an =  servlet.getClass().getAnnotation(NeptusServlet.class);
		if (an != null && an.name().length() > 0)
			return an.name();
		return servlet.getClass().getSimpleName();
	}
}
