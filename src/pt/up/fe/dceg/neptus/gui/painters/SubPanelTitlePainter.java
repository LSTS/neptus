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
 * 6/3/2011
 * $Id:: SubPanelTitlePainter.java 9615 2012-12-30 23:08:28Z pdias              $:
 */
package pt.up.fe.dceg.neptus.gui.painters;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Box;
import javax.swing.JComponent;

import org.jdesktop.swingx.painter.Painter;

/**
 * If title is different from null give a 10 pixel space on top of the panel
 * (using for example {@link Box#createVerticalStrut(int)})
 * @author pdias
 *
 */
public class SubPanelTitlePainter implements Painter<JComponent> {

    private String title = null;
    private Font titleFont = new Font("Sans", Font.ITALIC+Font.BOLD, 13);
    private Color titleColor = Color.blue.darker().darker();
    private Color titleColor2 = Color.red.darker().darker();
    private Color color = new Color(235, 245, 255);
    private Color color2 = new Color(255, 194, 193);
    
    private boolean active = true;
    
    /**
     * 
     */
    public SubPanelTitlePainter() {
    }

    public SubPanelTitlePainter(String title) {
        setTitle(title);
    }
    
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /* (non-Javadoc)
     * @see org.jdesktop.swingx.painter.Painter#paint(java.awt.Graphics2D, java.lang.Object, int, int)
     */
    @Override
    public void paint(Graphics2D g, JComponent c, int width, int height) {
        g = (Graphics2D) g.create();
        RoundRectangle2D rect = new RoundRectangle2D.Double(1, 1, c.getWidth() - 2,
                c.getHeight() - 2, 10, 10);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new LinearGradientPaint(0, 0, c.getWidth() / 2, c.getHeight(), new float[] { 0f,
                1f }, new Color[] { isActive() ? color : color2,
                (isActive() ? color : color2).darker() }));
        g.fill(rect);

        if (title != null) {
            g.setFont(titleFont);
            g.setColor(isActive() ? titleColor : titleColor2);
            g.drawString(title, 0, 10);
        }
        
        g.dispose();
    }
}
