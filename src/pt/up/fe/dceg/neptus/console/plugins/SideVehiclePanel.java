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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
        vsizeX = veh.getZSize();

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

        super.paint(arg0);
        update(arg0);

    }

    private BufferedImage bi = null;// (BufferedImage)createImage(image.getWidth(null),image.getHeight(null));

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
        try {

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

            // System.out.println("depth:"+this.depth +"\nmeterbyPixel:"+metersByPixel+"\nwatterPos:"+watterPos
            // +"this.depth*metersByPixel:"+this.depth*metersByPixel);

            g.scale(scale, scale);
            g.drawImage(image, -image.getWidth(this) / 2, -image.getHeight(this) / 2, this);
            g.scale(1. / scale, 1. / scale);
            g.rotate(this.pitch);
            g.translate(-getWidth() / 2, -getHeight() / 2);

            if (sea) {
                g.setColor(new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 125));
                // g.fillRect(0, 0, (int) (width * zoom), (int) (length * zoom));
                g.fillRect(0, watterPos, getWidth() - 1, getHeight() - watterPos - 1);

            }
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[] { 2f, 5f, 2f,
                    5f }, 0f));

            // pdias: Substituído o drawLine q estava de vez em quando a estourar a virtual machine em Win
            g.draw(new Line2D.Double(0, getHeight() / 2, getWidth(), getHeight() / 2));

            // g.drawImage(image,0,0,null);
            // g.drawImage(image,0,0,100,100,null);

            // g.rotate(this.pitch,image.getWidth(null)/2,image.getHeight(null)/2);

            // g2.drawImage(bi.getScaledInstance(this.getWidth(), this.getHeight(), Image.SCALE_SMOOTH), 0, 0, this);

            g2.drawImage(bi, 0, 0, this);
        }
        catch (Exception e) {
            // repaint();
        }
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
                System.out.println("excepcao");
            }
        }
    }

}
