/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/10/19
 * $Id:: RESTServlet.java 9616 2012-12-30 23:23:22Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.ws;

import java.io.IOException;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfoImpl;
import pt.up.fe.dceg.neptus.util.StreamUtil;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.logdb.SQLiteSerialization;

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
