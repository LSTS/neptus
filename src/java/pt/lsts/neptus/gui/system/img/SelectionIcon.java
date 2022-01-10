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
 * Author: Paulo Dias
 * 24/06/2011
 */
package pt.lsts.neptus.gui.system.img;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JButton;

import pt.lsts.neptus.util.GuiUtils;


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
