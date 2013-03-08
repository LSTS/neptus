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
 * $Id:: MainPlanServlet.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.plugins.web;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;

/**
 * @author ZP
 * 
 */
@NeptusServlet(name = "Main Plan", path = "/plan.png")
public class MainPlanServlet extends HttpServlet implements IConsoleServlet {
    private static final long serialVersionUID = 1L;

    protected ConsoleLayout console = null;

    @Override
    public void setConsole(ConsoleLayout c) {
        this.console = c;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        if (console != null && console.getPlan() != null) {
            resp.setContentType("image/png");

            synchronized (image) {
                console.getPlan().getAsImage(image);
            }

            ImageIO.write(image, "png", resp.getOutputStream());
            resp.getOutputStream().close();
        }
    }
}
