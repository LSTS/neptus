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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.up.fe.dceg.neptus.console.plugins.LogBookPanel;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author ZP
 *
 */
@NeptusServlet(path="/logbook.html", name="Log Book")
public class LogBookServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/html;charset=UTF-8");
		
		String contents = null;
		
		if (new File (LogBookPanel.filename).exists())
			contents = FileUtil.getFileAsString(LogBookPanel.filename);
		
		if (contents == null)
			contents = "<table><tr><td>(empty)</td></tr>";
		resp.getWriter().write(contents);
		
		resp.getWriter().write("</table><hr/><form method='post' action='logbook.html'><input type='text' id='text' name='text' size=80/> <input type='submit' value='log'/></form></body></html>");
		
		resp.getWriter().close();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String text = req.getParameter("text");
	//	System.out.println(text);
		if (text != null)
			LogBookPanel.logPlain(text);		
		doGet(req, resp);
	}
	
	
}
