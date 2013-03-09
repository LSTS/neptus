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
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * 
 * @author RJPG Compass panel, North is up when yaw is 0.0 Degrees
 * 
 */
public class CompassPanel extends JPanel {

    private static final long serialVersionUID = 4060353229890430778L;

    public float yaw = 0.0f;
    public Graphics2D g2 = null;
    private Image image = ImageUtils.getImage("images/bussola.png");

    public CompassPanel() {
        super();
        initialize();
    }

    private void initialize() {
        this.setLayout(null);
        this.setBounds(new java.awt.Rectangle(0, 0, image.getWidth(null), image.getHeight(null)));
        this.setSize(image.getWidth(null), image.getHeight(null));
        // this.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        this.setPreferredSize(new Dimension(140, 140));
        // this.setPreferredSize(new Dimension(image.getWidth(null),image.getHeight(null)));
        this.setDoubleBuffered(true);
        // this.setBackground(new Color(1.0f,0.0f,0.0f));
    }

    public float getYaw() {
        return yaw;
    }

    @Override
    public void paint(Graphics arg0) {

        super.paint(arg0);
        update(arg0);

    }

    private BufferedImage bi = (BufferedImage) createImage(image.getWidth(null), image.getHeight(null));

    public void setYaw(float y) {
        this.yaw = y;
        this.repaint();
    }

    int lastWidth = 0;

    public void update(Graphics arg0) {

        // if (getWidth() != lastWidth) {

        // }
        if (this.image == null) {
            NeptusLog.pub().error(this + ": Tried to draw a null image.");
            return;
        }

        try {

            this.g2 = (Graphics2D) arg0;

            if (g2 == null)
                return;

            // this.g2 = (Graphics2D)this.getGraphics();
            if (bi == null || bi.getWidth() < getWidth() || bi.getHeight() < getHeight())
                bi = (BufferedImage) createImage(image.getWidth(null), image.getHeight(null));
            Graphics2D g = (Graphics2D) bi.getGraphics();
            // g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            // RenderingHints.VALUE_ANTIALIAS_ON);
            // g.setRenderingHint(RenderingHints.KEY_RENDERING,
            // RenderingHints.VALUE_RENDER_QUALITY);
            // g.setPaintMode();
            g.clearRect(0, 0, image.getWidth(null), image.getHeight(null));

            // Graphics2D gimage=(Graphics2D)image.getGraphics();
            // gimage.translate(10,10);
            // gimage.rotate(Math.PI/20);
            // gimage.finalize();

            // g2.translate();
            // g2.translate(image.getWidth(null)/2,image.getHeight(null)/2);

            g.rotate(-this.yaw, image.getWidth(null) / 2, image.getHeight(null) / 2);

            // g2.translate(x,y);
            // g2.translate(image.getWidth(null)/2,image.getHeight(null)/2);

            g.drawImage(image, 0, 0, null);
            // g.drawImage(image,0,0,100,100,null);

            g.rotate(this.yaw, image.getWidth(null) / 2, image.getHeight(null) / 2);
            int yaux = (int) Math.toDegrees(this.yaw);
            yaux = yaux % 360;

            Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds("yaw:" + yaux + "\u00B0", g2);

            g.drawString("yaw:" + yaux + "\u00B0", (int) ((image.getWidth(null) / 2) - stringBounds.getCenterX()),
                    (int) ((image.getHeight(null)) / 2 - stringBounds.getCenterY()));

            // g2.drawImage(bi.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), 0, 0, this);

            g2.drawImage(bi, 0, 0, this.getWidth(), this.getHeight(), this);

            /*
             * g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); GradientPaint gp
             * = new GradientPaint(12,12,new Color(170, 170, 255), getWidth(), getHeight(), Color.WHITE);
             * g2.setPaint(gp); g2.fill(new Ellipse2D.Double(16,16,getWidth()-32, getHeight()-32));
             * g2.setColor(Color.black); g2.draw(new Ellipse2D.Double(16,16,getWidth()-32, getHeight()-32));
             * g2.drawString("yaw:"+yaux+"\u00B0", (int)((image.getWidth(null)/2)-stringBounds.getCenterX()),
             * (int)((image.getHeight(null))/2-stringBounds.getCenterY()));
             * 
             * for (double i = 0; i < 360; i+=22.5) { g2.draw(new Line2D.Double(getWidth()/2, 8, getWidth()/2,18));
             * g2.rotate(Math.toRadians(22.5), getWidth()/2, getHeight()/2); }
             * 
             * //g2.setTransform(new AffineTransform()); g2.setColor(new Color(255,0,0,100)); g2.draw(new
             * Line2D.Double(getWidth()/2,0,getWidth()/2,getHeight()/2-20));
             */
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
        CompassPanel buss = new CompassPanel();
        GuiUtils.testFrame(buss, "compass");
        for (int i = 0;; i -= 2) {
            buss.setYaw((float) Math.toRadians((double) i));
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
