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
 * Author: Paulo Dias
 * 26/06/2011
 */
package pt.lsts.neptus.plugins.web;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.JMenuItem;

import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.StreamUtil;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@NeptusServlet (name="Local State", path="/localstate/*", author="Paulo Dias")
public class LocalStateServlet extends HttpServlet implements IConsoleMenuItemServlet {

    public boolean allowConsoleExposure = false;
    
    private File console = null;
    private long consoleLastRTime = -1;
    protected JMenuItem exposuredMenuItem;
    
    
    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            IOException {
        
        if (req.getPathInfo().equals("/location")) {
            resp.setContentType("application/neptus+xml");
            PrintWriter page = resp.getWriter();
            LocationType start = MyState.getLocation();
            page.write(start.asXML(ImcMsgManager.getManager().getLocalId() + " Location"));
            page.close();
            return;
        }
        else if (req.getPathInfo().equals("/console") || req.getPathInfo().equals("/console.png")) {
            if (System.currentTimeMillis() - consoleLastRTime > 10000) {
                File logImages = new File("log/images");
                File[] fileList = logImages.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.isFile() && "png".equalsIgnoreCase(FileUtil.getFileExtension(pathname)))
                                return true;
                        else
                            return false;
                    }
                });
                Vector<File> fileVec = new Vector<File>();
                for (File f : fileList) {
                    fileVec.add(f);
                }
                Collections.sort(fileVec);
                if (fileVec.size() > 0)
                    console = fileVec.lastElement();
                else
                    console = null;
                consoleLastRTime = System.currentTimeMillis();
            }
            
            if (req.getPathInfo().equals("/console.png")) {
                if (console != null && console.exists() && allowConsoleExposure) {
                    resp.setContentType("image/png");
                    FileInputStream fxStream = new FileInputStream(console);
                    StreamUtil.copyStreamToStream(fxStream, resp.getOutputStream());
                    fxStream.close();
                }
                resp.getOutputStream().close();
            }
            else {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().write("<html><head><meta http-equiv='refresh' content='30'></head><body>");
                if (console != null && console.exists() && allowConsoleExposure)
                    resp.getWriter().write("<img src=\"/localstate/console.png\" alt=\"" + console.getName() +
                    		"\" " + "id=\"" + console.getName() + " border=\"0\">");
                else
                    resp.getWriter().write("No consoles active.");
                resp.getWriter().write("</body></html>");
                resp.getWriter().close();
                //<img src="guides.ign.com4_files/header220001101.gif" alt="IGNguides.com" usemap="#HEADER" width="590" border="0" height="90">
            }
        }
        else if (req.getPathInfo().equals("/")) {
            resp.setContentType("text/html;charset=UTF-8");
            if (!ImcMsgManager.getManager().isRunning()) {
                    resp.getWriter().write("<html><head><meta http-equiv='refresh' content='30'>" +
                    		"<body>Comms are not running.</body></html>");
                    resp.getWriter().close();
                return;
            }
            
            ImcId16 idImc = ImcMsgManager.getManager().getLocalId();
            
            String ret = "<html>";
            
            ret += "<h2>Location</h2><blockquote>";
            LocationType home = MyState.getLocation();
            String homeLoc = home.getLatitudeAsPrettyString(LatLonFormatEnum.DMS)+", "+home.getLongitudeAsPrettyString(LatLonFormatEnum.DMS);
            //ret += homeLoc+"<br/>";
            ret += "<a href='/localstate/location'>"+homeLoc+"</a><br/>";
            ret += "</blockquote><br>";
            
            ret += "<h2>State</h2>";
            ret += "<b>ID:</b> " + idImc + "<br>";
            ret += "<b>Services:</b> "
                    + ImcMsgManager.getManager().getAllServicesString().replaceAll(";", ";<br>")
                    + "<br><br>";
            
            ret += "<b>Comms info:</b><br>"
                + ImcMsgManager.getManager().getCommStatusAsHtmlFragment();
            
            if (System.currentTimeMillis()
                    - ImcMsgManager.getManager().getAnnounceLastArriveTime() > DateTimeUtil.MINUTE * 5) {
                ret += "<b color='red'>Announce not arriving for some time</b><br>";
            }
            
            ret += "</html>";

            resp.getWriter().write(ret);
            resp.getWriter().close();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.web.IConsoleMenuItemServlet#getConsoleMenuItems()
     */
    @Override
    public ConsoleMenuItem[] getConsoleMenuItems() {
        return new ConsoleMenuItem[] {new ConsoleMenuItem(I18n.text("Allow console exposure to the outside"),
                null, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (exposuredMenuItem.getText().equals(I18n.text("Allow console exposure to the outside"))) {
                            allowConsoleExposure = true;
                            exposuredMenuItem.setText(I18n.text("Disallow console exposure to the outside"));
                        }
                        else {
                            allowConsoleExposure = false;
                            exposuredMenuItem.setText(I18n.text("Allow console exposure to the outside"));
                        }
                    }
                })};
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.web.IConsoleMenuItemServlet#informCreatedConsoleMenuItem(java.util.Hashtable)
     */
    @Override
    public void informCreatedConsoleMenuItem(Hashtable<String, JMenuItem> consoleMenuItems) {
        for (String path : consoleMenuItems.keySet()) {
            if (I18n.text("Allow console exposure to the outside").equalsIgnoreCase(path)) 
                exposuredMenuItem = consoleMenuItems.get(path);
        }
    }
}
