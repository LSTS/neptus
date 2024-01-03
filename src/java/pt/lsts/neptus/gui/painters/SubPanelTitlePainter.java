/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 6/3/2011
 */
package pt.lsts.neptus.gui.painters;

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
