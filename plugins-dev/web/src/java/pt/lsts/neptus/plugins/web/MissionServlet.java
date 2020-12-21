/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.StreamUtil;

/**
 * @author ZP
 *
 */
@NeptusServlet(path="/mission/*", name="Mission Elements")
public class MissionServlet extends HttpServlet implements IConsoleServlet {

    private static final long serialVersionUID = 1L;
    protected ConsoleLayout myConsole = null;
	
	@Override
	public void setConsole(ConsoleLayout c) {
		this.myConsole = c;
	}
	
    protected void sendHomeRef(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (myConsole == null || myConsole.getMission() == null) {
            resp.setContentType("text/html");
            PrintWriter page = resp.getWriter();
            page.write("<p><font color='red'>ERROR: </font>No mission has been set in the console.<p></body></html>");
            page.close();
            return;
        }
        
        resp.setContentType("application/neptus+xml");
        PrintWriter page = resp.getWriter();
        HomeReference homeRef = myConsole.getMission().getHomeRef();
        page.write(homeRef.asXML("home-reference"));
        page.close();
    }
	
	protected void sendStart(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (myConsole == null || myConsole.getMission() == null) {
			resp.setContentType("text/html");
			PrintWriter page = resp.getWriter();
			page.write("<p><font color='red'>ERROR: </font>No mission has been set in the console.<p></body></html>");
			page.close();
			return;
		}
		
		resp.setContentType("application/neptus+xml");
		PrintWriter page = resp.getWriter();
		LocationType start = IMCUtils.lookForStartPosition(myConsole.getMission());
		page.write(start.asXML("StartLocation"));
		page.close();
	}
	
	protected void sendMission(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (myConsole == null || myConsole.getMission() == null) {
			resp.setContentType("text/html");
			PrintWriter page = resp.getWriter();
			page.write("<p><font color='red'>ERROR: </font>No mission has been set in the console.<p></body></html>");
			page.close();
			return;
		}
		resp.setContentType("application/zip+nmis");
		StreamUtil.copyStreamToStream(new FileInputStream(myConsole.getMission().getMissionFile()), resp.getOutputStream());
		
		resp.getOutputStream().close();
	}
	
	protected void sendTransponder(String id, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (myConsole == null || myConsole.getMission() == null) {
			resp.setContentType("text/html");
			PrintWriter page = resp.getWriter();
			page.write("<p><font color='red'>ERROR: </font>No mission has been set in the console.<p></body></html>");
			page.close();
			return;
		}

		PrintWriter page = resp.getWriter();
		try {			
			TransponderElement el = (TransponderElement) MapGroup.getMapGroupInstance(myConsole.getMission()).getMapObjectsByID(id)[0];
			resp.setContentType("application/neptus+xml");
			page.write(el.asXML("Transponder"));
			
		}
		catch (Exception e) {
			throw new ServletException("Invalid transponder id");
		}
		
		page.close();		
	}

	protected void sendPlan(String id, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (myConsole == null || myConsole.getMission() == null) {
			resp.setContentType("text/html");
			PrintWriter page = resp.getWriter();
			page.write("<p><font color='red'>ERROR: </font>No mission has been set in the console.<p></body></html>");
			page.close();
			return;
		}
		

		
			
		PlanType plan = myConsole.getMission().getIndividualPlansList().get(id);
		if (plan == null)
			throw new ServletException("Invalid plan id");
		
		PrintWriter page = resp.getWriter();
		resp.setContentType("application/neptus+xml");
		page.write(plan.asXML("Plan"));
		page.close();	
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
        if (req.getPathInfo().equals("/homeRef")) {
            sendHomeRef(req, resp);
            return;
        }

        if (req.getPathInfo().equals("/startLoc")) {
			sendStart(req, resp);
			return;
		}
		
		if (req.getPathInfo().startsWith("/download")) {
			sendMission(req, resp);
			return;
		}
		
		if (req.getPathInfo().startsWith("/transponder/")) {
			sendTransponder(req.getPathInfo().substring(13), req, resp);
			return;
		}
		
		if (req.getPathInfo().startsWith("/plan/")) {
			sendPlan(req.getPathInfo().substring(6), req, resp);
			return;
		}
		
		if (myConsole == null || myConsole.getMission() == null) {
			resp.setContentType("text/html");
			PrintWriter page = resp.getWriter();
			page.write("<p><font color='red'>ERROR: </font>No mission has been set in the console.<p></body></html>");
			page.close();
			return;
		}
		
		resp.setContentType("text/html");
		PrintWriter page = resp.getWriter();
		page.write("<html><head><title>Loaded Mission</title></head><body><h1>Loaded Mission</h1>");
		
		MissionType mission = myConsole.getMission();
		
		page.write("<h2>Mission Information</h2><blockquote>");
		page.write("<b>Mission ID: </b>"+mission.getId()+"<br/>");
		page.write("<b>Mission Name: </b>"+mission.getName()+"<br/>");
		page.write("<b>Mission Description: </b>"+mission.getDescription()+"<br/>");
		page.write("<b>Mission Filename: </b>"+mission.getMissionFile().getAbsolutePath()+"<br/>");
		page.write("<a href='/mission/download/"+myConsole.getMission().getMissionFile().getName()+"'>Download</a></blockquote>");
	
		page.write("<h2>Map</h2><blockquote>");
		LocationType home = new LocationType(mission.getHomeRef()), start = IMCUtils.lookForStartPosition(mission);
		String homeLoc = home.getLatitudeAsPrettyString(LatLonFormatEnum.DMS)+", "+home.getLongitudeAsPrettyString(LatLonFormatEnum.DMS);
		String startLoc = (start != null)? start.getLatitudeAsPrettyString(LatLonFormatEnum.DMS)+", "+start.getLongitudeAsPrettyString(LatLonFormatEnum.DMS) : "";
		
		page.write("<b>Home Reference: </b><a href='/mission/homeRef'>"+homeLoc+"</a><br/>");
		page.write("<b>Start Location: </b><a href='/mission/startLoc'>"+startLoc+"</a><br/>");
		
		//LinkedList<TransponderElement> transpondersList = new LinkedList<TransponderElement>();
		
		for (MapMission mpm : mission.getMapsList().values()) {
			LinkedHashMap<String, TransponderElement> transList = mpm.getMap()
					.getTranspondersList();
			for (TransponderElement tmp : transList.values()) {
				String tLoc = tmp.getCenterLocation().getLatitudeAsPrettyString(LatLonFormatEnum.DMS)+", "+tmp.getCenterLocation().getLongitudeAsPrettyString(LatLonFormatEnum.DMS)+", "+tmp.getCenterLocation().getAllZ();
				page.write("<b>Transponder "+tmp.getId()+":</b> <a href='/mission/transponder/"+tmp.getId()+"'>"+tLoc+"</a><br/>");				
			}
		}
		
		page.write("</blockquote><h2>Plans</h2><blockquote>");
		
		for (PlanType plan : mission.getIndividualPlansList().values()) {
			page.write("<b><a href='/mission/plan/"+plan.getId()+"'>"+plan.getId()+"</a></b> ("+plan.getVehicle()+")<br/>");			
		}
			
		page.write("</blockquote></body></html>");
		page.close();		
	}
}
