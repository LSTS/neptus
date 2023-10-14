/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.ImagePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer3d.Util3D;
import pt.lsts.neptus.util.ImageUtils;

@SuppressWarnings("serial")
@PluginDescription(icon = "images/buttons/quicktimebutt.png", name = "Video Panel JMF")
public class JVideoPanelConsole extends ConsolePanel {
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
                NeptusLog.pub().info("<###>Web Cam not detected / initialised");
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
                // NeptusLog.pub().info("<###>No grabbed image");
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

    @Override
    public void cleanSubPanel() {
        try {
            jwc.finalize();
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void initSubPanel() {
        
    }

}
