/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 10 de Dez de 2011
 * $Id:: SystemIconsUtil.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.gui.system;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class SystemIconsUtil {

    private final static GeneralPath uavShape = new GeneralPath();
    static {
        uavShape.moveTo(-20 / 2, 14 / 2);
        uavShape.lineTo(0, -14 / 2);
        uavShape.lineTo(20 / 2, 14 / 2);
        uavShape.lineTo(0, 14 / 2 * 3 / 5);
        uavShape.closePath();
        
//        ret.moveTo((-width/2)    ,   (-height/2));
//        ret.lineTo(0             ,   (height/2));
//        ret.lineTo((width/2)     ,   (-height/2));
//        ret.lineTo(0             ,   ((-height/2)*3/5));
    }
    
    private final static GeneralPath uavAltShape = new GeneralPath();
    static {
        int width = 20, height = width / 2; 
        uavAltShape.moveTo(width / 4, height / 3);

        // plane's nose
        uavAltShape.lineTo(width / 4, height / 3);
        uavAltShape.curveTo(width / 2 + 1, height / 3, width / 2 + 1, -height / 8, width / 4, -height / 8);

        // plane's wing
        uavAltShape.lineTo(width / 4 - 2, -height / 8);
        uavAltShape.lineTo(width / 4 - 6, -height / 8 - 6);
        uavAltShape.lineTo(width / 4 - 8, -height / 8 - 6);
        uavAltShape.lineTo(width / 4 - 8, -height / 8);

        // plane's tail
        uavAltShape.lineTo(-width / 4 - 1, -height / 8);
        uavAltShape.lineTo(-width * 5 / 12, -height / 2);
        uavAltShape.lineTo(-width / 2, -height / 2);
        uavAltShape.lineTo(-width / 2, 0);
        uavAltShape.lineTo(-width / 4, height / 3);

        uavAltShape.closePath();
    }
    
    private final static GeneralPath uuvShape = new GeneralPath();
    static {
        uuvShape.moveTo(-2,10);
        uuvShape.lineTo(2, 10);
        uuvShape.lineTo(2, 0);
        uuvShape.lineTo(5, 0);
        uuvShape.lineTo(0, -10);
        uuvShape.lineTo(-5, 0);
        uuvShape.lineTo(-2, 0);
        uuvShape.closePath();
    }

    private final static Shape circleFillShape = new Ellipse2D.Double(-20 / 10., -20 / 10., 20 / 5., 20 / 5.);
    
    private SystemIconsUtil() {
    }
    
    public static GeneralPath getUAV() {
        return uavShape;
    }

    public static GeneralPath getUAVAlt() {
        return uavAltShape;
    }

    public static Shape getCircle() {
        return circleFillShape;
    }

    /**
     * 
     */
    public static GeneralPath getUUV() {
        return uuvShape;
    }
    
    @SuppressWarnings("serial")
    public static void main(String[] args) {
        JLabel lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GeneralPath gp = getUUV();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.fill(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

        lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GeneralPath gp = getUAV();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.fill(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

        lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GeneralPath gp = getUAVAlt();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.draw(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

        lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Shape gp = getCircle();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.fill(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

    }
}
