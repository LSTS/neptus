/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 16, 2012
 * $Id:: Histogram.java 9615 2012-12-30 23:08:28Z pdias                         $:
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class Histogram extends JPanel {

    private static final long serialVersionUID = 1L;
    protected BufferedImage histogram = null;

    public Histogram() {
        setData(new double[]{0}, 0, 255);
    }
    
    public void setData(double[] data, double fixedMin, double fixedMax) {

        double min = fixedMin, max = fixedMax;
        if (Double.isNaN(fixedMin) || Double.isNaN(fixedMax)) {
            min = data[0];
            max = data[0];
            
            for (int i = 1; i < data.length; i++) {
                if (data[i] > max)
                    max = data[i];
                if (data[i] < min)
                    min = data[i];
            }
        }
        
        int width = data.length;
        int height = 255;
        histogram = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = histogram.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.red);
        for (int i = 0; i < data.length; i++) {
            g.draw(new Line2D.Double(i, height-1, i, height-1-(data[i]/max) * 255));
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (histogram != null) {
            g.drawImage(histogram, 0, 0, getWidth(), getHeight(), 0, 0, histogram.getWidth(), histogram.getHeight(), this);
        }
    }

    
    public static void main(String[] args) {
        Histogram hist = new Histogram();
        double[] vals = new double[2000];
        for (int i = 0; i < vals.length; i++)
            vals[i] = Math.random() * 255;
        
        hist.setData(vals, 0, 255);
        GuiUtils.testFrame(hist);
    }
    
}
