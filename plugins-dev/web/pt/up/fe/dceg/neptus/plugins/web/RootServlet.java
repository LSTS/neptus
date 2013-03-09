/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2010/06/26
 */
package pt.up.fe.dceg.neptus.plugins.web;

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
