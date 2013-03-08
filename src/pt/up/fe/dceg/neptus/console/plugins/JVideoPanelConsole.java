/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: JVideoPanelConsole.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.gui.ImagePanel;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.renderer3d.Util3D;
import pt.up.fe.dceg.neptus.util.ImageUtils;

@SuppressWarnings("serial")
@PluginDescription(icon = "images/buttons/quicktimebutt.png", name = "Video Panel JMF")
public class JVideoPanelConsole extends SubPanel {
    private JWebCam jwc;

    BufferedImage image = new BufferedImage(Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE, BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();

    public JVideoPanelConsole(ConsoleLayout console) {
        super(console);
        super.setSize(320, 240);
        super.setVisible(true);
        super.repaint();

        Image img = ImageUtils.getImage("images/novideo.png");

        // Graphics2D g2 = image.createGraphics();
        g.drawImage(img, 0, 0, Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE, null);

        ImagePanel J = new ImagePanel(image);
        J.setFit_to_panel(true);

        // GuiUtils.testFrame(J,"image,");

        try {
            jwc = new JWebCam();

            if (!jwc.initialise()) {
                System.out.println("Web Cam not detected / initialised");
                setLayout(new BorderLayout());
                // JPanel JP=new JPanel();
                // JP.setBackground(Color.BLACK);
                add(J, BorderLayout.CENTER);
            }
            else {
                setLayout(new BorderLayout());
                add(jwc, BorderLayout.CENTER);
                jwc.setBackground(Color.BLACK);
                jwc.setVisible(true);

            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(" No video Capture found by JMF video capture", ex);
        }

    }

    @Override
    public String toString() {
        if (jwc == null)
            return "Video OFF line (no device connected)";
        else if (jwc.webCamDeviceInfo == null)
            return "Video OFF line (no device connected)";
        else
            return jwc.webCamDeviceInfo.toString();
    }

    public Image grabFrameImage(int top, int down, int left, int right) {
        if (jwc != null && jwc.webCamDeviceInfo != null && jwc.player != null) {
            // System.err.println("não é null jwc");

            Image im = jwc.grabFrameImage();
            if (im == null) {
                // System.out.println("No grabbed image");
                return null;
            }

            // convert the image to a BufferedImage

            g.drawImage(im, 0, 0, Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE, left, top, im.getWidth(null) - right,
                    im.getHeight(null) - down, null);

            // g.drawImage(im, 0, 0, Util3D.FORMAT_SIZE, Util3D.FORMAT_SIZE,null);

            // Overlay current time on top of the image

            // g.dispose();

            return image;

            /*
             * Image im=jwc.grabFrameImage(); im.getGraphics(). return ;
             */
        }
        else {
            // System.err.println("é null jwc--- ok ");
            return image;
        }
    }

    public void clean() {
        super.clean();
        try {
            jwc.finalize();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
