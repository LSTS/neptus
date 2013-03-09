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
 */
package pt.up.fe.dceg.neptus.plugins.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ConsoleSystem;
import pt.up.fe.dceg.neptus.imc.state.ImcSysState;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

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
            for (ConsoleSystem v : console.getConsoleSystems().values()) {
                page.write("<h2><a href='/state/"+v.getVehicleId()+"'>"+v.getVehicleId()+"</a></h2>");
            }
            page.write("</blockquote></body></html>");
            page.close();
        }
        else {
            String parts[] = req.getPathInfo().split("/");
            String vehicle = parts[1];
            

            ImcSysState state = console.getImcState(vehicle);

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
