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
 * 2009/10/19
 */
package pt.lsts.neptus.ws;

import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.logdb.SQLiteSerialization;

/**
 * @author zp
 *
 */
public class RESTServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	//FIXME ZP
	protected void messagesReceived(IMCMessage[] messages) {
		for (int i = 0; i < messages.length; i++) {
			try {
				MessageInfo info = new MessageInfoImpl();
				info.setTimeReceivedNanos(System.currentTimeMillis()*(long)1E6);
				info.setTimeSentNanos((long)messages[i].getHeader().getTimestamp()*(long)1E9);
				ImcMsgManager.getManager().onMessage(info, messages[i]);
			}
			catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
	
	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// FIXME
		super.doHead(req, resp);
	}
	
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String[] parts = req.getRequestURI().split("/");
		Vector<String> errors = new Vector<String>();
			
		if (parts.length > 2 && parts[parts.length-2].equals("imc")) {
			String[] p2 = parts[parts.length-1].split("\\.");
			String msgName = (p2.length > 0)? p2[0] : parts[parts.length-1];
			String format = (p2.length <= 1)? "xml" : p2[1].toLowerCase();
	
			
			if (IMCDefinition.getInstance().getMessageId(msgName) != -1) {
				try {
					if (format.equalsIgnoreCase("xml")) {
						String xml = StreamUtil.copyStreamToString(req.getInputStream());
						req.getInputStream().close();
						IMCMessage[] messages = IMCUtils.parseImcXml(xml);
						messagesReceived(messages);
						resp.setStatus(200);
						
					}
					else if (format.equalsIgnoreCase("lsf")) {
						IMCMessage[] msgs = IMCUtils.parseLsf(req.getInputStream());
						messagesReceived(msgs);
						resp.setStatus(200);
					}
					else {
						resp.setStatus(500);
						errors.add("Currently only IMC-XML and LSF are suppported");
					}
				}
				catch (Exception e) {
					resp.setStatus(500);
					errors.add(e.getMessage());
				}
			}
			else {
				resp.setStatus(404);
				errors.add("Message not found: "+msgName);
			}
		}
		
		resp.setContentType("text/plain");		
		for (String e : errors)
			resp.getWriter().println(e);
		resp.getWriter().close();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		//super.doGet(req, resp);
		
		String[] parts = req.getRequestURI().split("/");
		Vector<String> errors = new Vector<String>();
		
		if (parts.length > 2 && parts[parts.length-2].equals("imc")) {
			
			String[] p2 = parts[parts.length-1].split("\\.");
			
			String msgName = (p2.length > 0)? p2[0] : parts[parts.length-1];
			
			String format = (p2.length <= 1)? "xml" : p2[1].toLowerCase();

			resp.setContentType("application/"+format);
			
			if (IMCDefinition.getInstance().getMessageId(msgName) != -1) {
				
				if (format.equals("xml")) {
					try {
						resp.setContentType("text/xml");
						resp.getWriter().println(IMCUtils.getAsImcXml(new IMCMessage[] {SQLiteSerialization.getDb().getLastMessageOfType(msgName)}).asXML());
						resp.getWriter().close();
						return;
					}
					catch (Exception e) {
						resp.setStatus(500);
						errors.add(e.getMessage());
					}
				}
				else if (format.equals("lsf")) {
					resp.setContentType("application/lsf");
					try {
						IMCUtils.writeAsLsf(SQLiteSerialization.getDb().getLastMessageOfType(msgName), resp.getOutputStream());
					}
					catch (Exception e) {
						resp.setStatus(500);
						errors.add(e.getMessage());
					}
				}
				else if (format.equals("txt")) {
					resp.setContentType("text/plain");
					
					try {
						IMCUtils.writeAsTxt(SQLiteSerialization.getDb().getLastMessageOfType(msgName), resp.getOutputStream());						
					}
					catch (Exception e) {
						resp.setStatus(500);
						errors.add(e.getMessage());
					}
				}
			}
			else {
				resp.setStatus(404);
				errors.add("Message not found: "+msgName);
			}							
		}
		else {
			resp.setStatus(403);
			errors.add("Malformed URL");
		}
		
		resp.setContentType("text/plain");				
		for (String e : errors)
			resp.getWriter().println(e);
		//resp.getWriter().close();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPut(req, resp);
	}
}
