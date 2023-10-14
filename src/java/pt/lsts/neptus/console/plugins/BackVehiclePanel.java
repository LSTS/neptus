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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * 
 * @author RJPG Back image vehicle panel to represent the roll value
 * 
 */
public class BackVehiclePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    public float roll = 0.0f;
    public Graphics2D g2 = null;
    private Image image = null;

    public BackVehiclePanel() {
        super();
        initialize();
    }

    public void setVehicle(String id) {
        VehicleType veh = VehiclesHolder.getVehicleById(id);
        if (veh == null)
            return;
        

        String backHref = veh.getBackImageHref();

        if ("".equalsIgnoreCase(backHref))
            image = ImageUtils.getImage("images/lauv-black-back.png");
        else
            image = ImageUtils.getImage(VehiclesHolder.getVehicleById(id).getBackImageHref());

        roll = 0.0f;
        this.repaint();
    }

    private void initialize() {
        this.setLayout(null);
        // this.setBounds(new java.awt.Rectangle(0,0,image.getWidth(null),image.getHeight(null)));
        // this.setSize(image.getWidth(null),image.getHeight(null));
        // this.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        // this.setPreferredSize(new Dimension(140,140));
        // this.setPreferredSize(new Dimension(image.getWidth(null),image.getHeight(null)));
        this.setDoubleBuffered(true);
        // this.setBackground(new Color(1.0f,0.0f,0.0f));
    }

    public float getRoll() {
        return roll;
    }

    @Override
    public void paint(Graphics arg0) {

        super.paint(arg0);
        update(arg0);

    }

    private BufferedImage bi = null;// (BufferedImage)createImage(image.getWidth(null),image.getHeight(null));

    public void setRoll(float y) {
        this.roll = y;
        this.repaint();
    }

    int lastWidth = 0;

    public void update(Graphics arg0) {

        if (this.image == null)
            return;

        try {

            this.g2 = (Graphics2D) arg0;

            if (g2 == null)
                return;

            if (bi == null || bi.getWidth() < getWidth() || bi.getHeight() < getHeight())
                bi = (BufferedImage) createImage(this.getWidth(), this.getHeight());
            Graphics2D g = (Graphics2D) bi.getGraphics();

            g.clearRect(0, 0, getWidth(), getHeight());
            g.translate(getWidth() / 2, getHeight() / 2);
            g.rotate(this.roll);

            double factorthis = (double) this.getWidth() / (double) this.getHeight();
            double factorim = (double) image.getWidth(null) / (double) image.getHeight(null);

            double scale = 1.0;
            if (factorthis < factorim) {
                scale = (double) this.getWidth() / (double) image.getWidth(null);

            }
            else {
                scale = (double) this.getHeight() / (double) image.getHeight(this);
            }
            g.scale(scale, scale);
            g.drawImage(image, -image.getWidth(this) / 2, -image.getHeight(this) / 2, this);

            g.scale(1. / scale, 1. / scale);
            g.rotate(-this.roll);
            g.translate(-getWidth() / 2, -getHeight() / 2);

            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[] { 2f, 5f, 2f,
                    5f }, 0f));
            g.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());

            // g.drawImage(image,0,0,null);
            // g.drawImage(image,0,0,100,100,null);

            // g.rotate(this.pitch,image.getWidth(null)/2,image.getHeight(null)/2);

            // g2.drawImage(bi.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), 0, 0, this);

            g2.drawImage(bi, 0, 0, this);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void drawImage(Graphics2D g, Image img, int imgx, int imgy, int x, int y, int w, int h) {
        Graphics2D ng = (Graphics2D) g.create();
        ng.clipRect(x, y, w, h);
        ng.drawImage(img, imgx, imgy, this);
    }

    public static void main(String[] arg) {
        SideVehiclePanel buss = new SideVehiclePanel();
        GuiUtils.testFrame(buss, "compass");
        for (int i = 0;; i -= 2) {
            buss.setPitch(((float) Math.toRadians((double) i)));
            try {// nada
                 // NeptusLog.pub().info("<###>espera...");
                Thread.sleep(50);
                // NeptusLog.pub().info("<###>esperou");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
