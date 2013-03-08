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
 * 2010/06/27
 * $Id:: LogsServlet.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.plugins.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.security.Credential.MD5;

import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.StreamUtil;
import pt.up.fe.dceg.neptus.util.ZipUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 *
 */
@NeptusServlet(name="Logs", path="/log/*")
public class LogsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		if (req.getPathInfo().equals("/")) {
			resp.setContentType("text/html");
			resp.getWriter().write("<html><head><title>Recorded Logs</title></head><body>");
			File f = new File("log");
			Vector<File> logs = new Vector<File>();
			listLogs(f, logs);
			for (File logDir : logs) {
				String shorter = FileUtil.relativizeFilePath(f.getAbsolutePath(), logDir.getAbsolutePath()).replaceAll("\\\\", "/");				
				resp.getWriter().write("<li><a href='"+shorter+".zip'>"+logDir.getName()+"</a>");
			}
			resp.getWriter().close();
		}
		else if (req.getPathInfo().endsWith(".zip")) {
			String dir = req.getPathInfo().substring(0, req.getPathInfo().length()-4);
			File temp = new File(ConfigFetch.getNeptusTmpDir()+"/"+MD5.digest(dir).substring(4));
			
			if ((req.getHeader("pragma") != null && req.getHeader("pragma").equalsIgnoreCase("no-cache")) ||
					(req.getHeader("cacheControl") != null && req.getHeader("cacheControl").equalsIgnoreCase("no-store")) || 
					!temp.exists())
			{
				System.out.println("zipping "+new File("log",dir).getAbsolutePath()+" to "+temp);
				ZipUtils.zipDir(temp.getAbsolutePath(), new File("log",dir).getAbsolutePath());
				
			}
			resp.setContentType("application/zip");
			StreamUtil.copyStreamToStream(new FileInputStream(temp), resp.getOutputStream());
			resp.getOutputStream().close();
			
		}
		
		
	}
	
	protected void listLogs(File parent, Vector<File> logs) {
		for (File f : parent.listFiles()) {
			if (f.isDirectory())
				listLogs(f, logs);
			else if (f.getName().equalsIgnoreCase("Data.lsf") || f.getName().equalsIgnoreCase("EstimatedState.llf"))
				logs.add(parent);
		}
	}
	
}
