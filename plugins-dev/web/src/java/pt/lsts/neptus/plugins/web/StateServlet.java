/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 2010/06/27
 */
package pt.lsts.neptus.plugins.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsoleSystem;

/**
 * @author ZP
 *
 */
@NeptusServlet(path="/state/*", name="Current State")
public class StateServlet extends HttpServlet implements IConsoleServlet {

    private static final long serialVersionUID = 1L;
    private ConsoleLayout console = null;

    @Override
    public void setConsole(ConsoleLayout c) {
        this.console = c;		
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (console == null)
            throw new ServletException("Console is null");

        if (req.getPathInfo().equals("/")) {
            resp.setContentType("text/html");
            PrintWriter page = resp.getWriter();
            page.write("<html><head><title>Current Neptus State</title></head><body><h1>Current Neptus State</h1><blockquote>");
            for (ConsoleSystem v : console.getSystems().values()) {
                page.write("<h2><a href='/state/"+v.getVehicleId()+"'>"+v.getVehicleId()+"</a></h2>");
            }
            page.write("</blockquote></body></html>");
            page.close();
        }
        else {
            String parts[] = req.getPathInfo().split("/");
            String vehicle = parts[1];
            

            ImcSystemState state = console.getImcState(vehicle);

            if (state != null && parts.length < 3) {
                resp.setContentType("text/html");
                PrintWriter page = resp.getWriter();
                page.write("<html><head><title>"+vehicle+" data</title></head><body><h1>"+vehicle+" data</h1><blockquote>");

                for (String msg : state.availableMessages()) {
                    page.write("<li><a href=/state/"+vehicle+"/"+msg+">"+msg+"</a></li>");
                }
                page.write("</body></html>");
                page.close();				
            }
            else {
                String variable = parts[2];
                resp.setContentType("text/html");
                PrintWriter page = resp.getWriter();
                
                try {
                    page.write("<html><head><title>"+variable+" data</title>");
                    page.write("<meta http-equiv='refresh' content='3'>");
                    String html = IMCUtils.getAsHtml(state.get(variable));
                    page.write(html.substring("<html>".length()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                page.close();
            }

        }

    }


}
