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
 * $Id:: BackVehiclePanel.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
                 // System.out.println("espera...");
                Thread.sleep(50);
                // System.out.println("esperou");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
