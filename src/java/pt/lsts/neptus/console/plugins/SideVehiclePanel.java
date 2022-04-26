/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ImageUtils;

/**
 * 
 * @author RJPG Side vehicle panel to represent the pitch value
 * 
 */
public class SideVehiclePanel extends JPanel {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7234818649430917670L;

    public float pitch = 0.0f;
    public float depth = 0.0f;
    public Graphics2D g2 = null;
    private Image image = null;

    private boolean sea = true;

    public float vsizeX;
    public float vsizeZ;

    public SideVehiclePanel() {
        super();
        initialize();
    }

    public void setVehicle(String id) {
        VehicleType veh = VehiclesHolder.getVehicleById(id);
        if (veh == null)
            return;
        
        vsizeX = veh.getXSize();
        vsizeZ = veh.getZSize();

        image = ImageUtils.getImage(VehiclesHolder.getVehicleById(id).getSideImageHref());
        pitch = 0.0f;
        this.repaint();
    }

    private void initialize() {
        this.setLayout(null);
        this.setDoubleBuffered(true);
    }

    public float getPitch() {
        return pitch;
    }

    @Override
    public void paint(Graphics arg0) {
        //setBackground(Color.white);
        //setOpaque(true);
        super.paint(arg0);
        update(arg0);

    }

    private BufferedImage bi = null;

    public void setDepth(float z) {
        this.depth = z;
        this.repaint();
    }

    public void setPitch(float y) {
        this.pitch = y;
        this.repaint();
    }

    public void setValues(float pitcha, float deptha) {
        this.depth = deptha;
        this.pitch = pitcha;
        this.repaint();
    }

    int lastWidth = 0;

    public void update(Graphics arg0) {

        if (this.image == null)
            return;
            this.g2 = (Graphics2D) arg0;
            if (g2 == null)
                return;

            if (bi == null || bi.getWidth() < getWidth() || bi.getHeight() < getHeight())
                bi = (BufferedImage) createImage(this.getWidth(), this.getHeight());
            Graphics2D g = (Graphics2D) bi.getGraphics();

            g.clearRect(0, 0, getWidth(), getHeight());
            g.translate(getWidth() / 2, getHeight() / 2);
            g.rotate(-this.pitch);

            double factorthis = (double) this.getWidth() / (double) this.getHeight();
            double factorim = (double) image.getWidth(null) / (double) image.getHeight(null);

            double scale = 1.0;
            if (factorthis < factorim) {
                scale = (double) this.getWidth() / (double) image.getWidth(null);
            }
            else {
                scale = (double) this.getHeight() / (double) image.getHeight(this);
            }

            float metersByPixel = 0;
            if (this.getWidth() != 0)
                metersByPixel = (float) ((this.getWidth() / vsizeX) * scale);

            g.setColor(Color.BLUE);
            int watterPos = (int) (getHeight() / 2 - (this.depth * metersByPixel));

            g.scale(scale, scale);
            g.drawImage(image, -image.getWidth(this) / 2, -image.getHeight(this) / 2, this);
            g.scale(1. / scale, 1. / scale);
            g.rotate(this.pitch);
            g.translate(-getWidth() / 2, -getHeight() / 2);

            if (sea) {
                g.setColor(new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 125));
                g.fillRect(0, watterPos, getWidth() - 1, getHeight() - watterPos - 1);

            }
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[] { 2f, 5f, 2f,
                    5f }, 0f));

            g.draw(new Line2D.Double(0, getHeight() / 2, getWidth(), getHeight() / 2));
            g2.drawImage(bi, 0, 0, this);
            
            g2.setFont(new Font("Helvetica", Font.BOLD, 14));
            g2.setColor(Color.BLACK);
            if (depth > 0)
                g2.drawString(String.format("D: %.2f", depth), 10, 15);
            else
                g2.drawString(String.format("A: %.2f", -depth), 10, 15);
    }

    public void drawImage(Graphics2D g, Image img, int imgx, int imgy, int x, int y, int w, int h) {
        Graphics2D ng = (Graphics2D) g.create();
        ng.clipRect(x, y, w, h);
        ng.drawImage(img, imgx, imgy, this);
    }

    public boolean isSea() {
        return sea;
    }

    public void setSea(boolean sea) {
        this.sea = sea;
    }

    public static void main(String[] arg) {
//        SideVehiclePanel buss = new SideVehiclePanel();
//        GuiUtils.testFrame(buss, "compass");
//        for (int i = 0;; i -= 2) {
//            buss.setPitch(((float) Math.toRadians((double) i)));
//            try {// nada
//                 // NeptusLog.pub().info("<###>espera...");
//                Thread.sleep(50);
//                // NeptusLog.pub().info("<###>esperou");
//            }
//            catch (Exception e) {
//                NeptusLog.pub().info("<###>excepcao");
//            }
//        }
    }

}
