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
 * 24/06/2011
 * $Id:: SelectionIcon.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.gui.system.img;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JButton;

import pt.up.fe.dceg.neptus.util.GuiUtils;


/**
 * @author pdias
 *
 */
public class SelectionIcon implements Icon {

    protected boolean isSelectionOrMain = true;
    private double diam, margin;

    public SelectionIcon (int diameter, int margin) {
        this.diam = diameter;
        this.margin = margin;
    }

    public SelectionIcon (int diameter) {
        this(diameter, 0);
    }

    /* (non-Javadoc)
     * @see javax.swing.Icon#getIconWidth()
     */
    @Override
    public int getIconWidth() {
        return (int)(diam + margin * 2);
    }

    /* (non-Javadoc)
     * @see javax.swing.Icon#getIconHeight()
     */
    @Override
    public int getIconHeight() {
        return getIconWidth();
    }

    /* (non-Javadoc)
     * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
     */
    @Override
    public void paintIcon(Component c, Graphics gr, int x, int y) {
        Graphics2D g2 = (Graphics2D) gr.create();
        g2.translate(x, y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //g2.translate(margin+diam/2,margin+diam/2);
        
        g2.scale(getIconWidth()/32.0, getIconHeight()/32.0);

        RoundRectangle2D rect = new RoundRectangle2D.Double(0, 0, 32, 32, 0, 0);
        g2.setColor(isSelectionOrMain?new Color(255, 255/2, 0).brighter():Color.GREEN.darker());
        g2.fill(rect);

        g2.translate(16, 16);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        if (isSelectionOrMain)
            g2.drawString("S", -10, 10);
        else
            g2.drawString("M", -13, 10);
    }
    
    public static void main(String[] args) {
        SelectionIcon icon = new SelectionIcon(100);
        JButton but = new JButton(icon);
        
        GuiUtils.testFrame(but);
    }
}
