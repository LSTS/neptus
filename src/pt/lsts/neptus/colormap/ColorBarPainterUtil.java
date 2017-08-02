/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 23/05/2017
 */
package pt.lsts.neptus.colormap;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class ColorBarPainterUtil {

    private ColorBarPainterUtil() {
    }

    /**
     * Paint a color bar with captions sized w x h=70 x 110.
     * 
     * @param g
     * @param renderer
     * @param cmap
     * @param varName
     * @param units
     * @param minValue
     * @param maxValue
     */
    public static void paintColorBar(Graphics2D g, StateRenderer2D renderer, ColorMap cmap, String varName, 
            String units, double minValue, double maxValue) {
            Graphics2D g2 = (Graphics2D) g.create();
            
            g2.translate(-5, -30);
            g2.setColor(new Color(250, 250, 250, 100));
            g2.fillRect(5, 30, 70, 110);

            ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
            cb.setSize(15, 80);
            g2.setColor(Color.WHITE);
            Font prev = g2.getFont();
            g2.setFont(new Font("Helvetica", Font.BOLD, 18));
            g2.setFont(prev);
            g2.translate(15, 45);
            cb.paint(g2);
            g2.translate(-10, -15);
            
            g2.setColor(Color.WHITE);
            g2.drawString(varName, 2, 11);
            g2.setColor(Color.BLACK);
            g2.drawString(varName, 2, 12);

            try {
                double medValue = (maxValue - minValue) / 2. + minValue;
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 28, 20+5);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 29, 21+5);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(medValue), 28, 60);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(medValue), 29, 61);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 28, 100-5);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 29, 101-5);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }

            g2.setColor(Color.WHITE);
            g2.drawString(units, 10, 105);
            g2.setColor(Color.BLACK);
            g2.drawString(units, 10, 106);

            g2.dispose();
    }

}
