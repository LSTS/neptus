/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2010/06/27
 */
package pt.lsts.neptus.plugins.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.security.Credential.MD5;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.ZipUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
				NeptusLog.pub().info("<###>zipping "+new File("log",dir).getAbsolutePath()+" to "+temp);
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
