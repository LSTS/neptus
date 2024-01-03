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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class ColorBarPainterUtil {

    private static NumberFormat getDecimalFormat(int fractionDigits) {
        NumberFormat df = DecimalFormat.getInstance(Locale.US);
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(fractionDigits);
        return df;
    }

    private ColorBarPainterUtil() {
    }

    /**
     * Paint a color bar with captions sized w x h=70 x 110.
     * 
     * @param g
     * @param cmap
     * @param varName
     * @param units
     * @param minValue
     * @param maxValue
     */
    public static void paintColorBar(Graphics2D g, ColorMap cmap, String varName, String units, double minValue,
            double maxValue) {
        paintColorBarVertical(g, cmap, varName, units, minValue, maxValue, false);
    }
    
    private static void paintColorBarVertical(Graphics2D g, ColorMap cmap, String varName, String units, double minValue,
            double maxValue, boolean showInLog10) {
        
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
            g2.setColor(cmap.getColor(-0.1));
            g2.fillRect(0, 81, 15, 1);
            cb.paint(g2);
            g2.setColor(cmap.getColor(1.1));
            g2.fillRect(0, -1, 15, 1);
            g2.translate(-10, -15);
            
            g2.setColor(Color.WHITE);
            g2.drawString(varName, 2, 11);
            g2.setColor(Color.BLACK);
            g2.drawString(varName, 2, 12);

            try {
                double medValue = (maxValue - minValue) / 2. + minValue;
                if (showInLog10) {
                    double minL = Math.log10(minValue);
                    double maxL = Math.log10(maxValue);
                    double vMedL = (maxL - minL) / 2.;
                    medValue = Math.pow(10, maxL - vMedL);
                    System.out.println(String.format("minL=%f   maxL=%f   medL=%f   med=%f", minL, maxL, vMedL, medValue));
                }
                g2.setColor(Color.WHITE);
                g2.drawString(getDecimalFormat(2).format(maxValue), 28, 20+5);
                g2.setColor(Color.BLACK);
                g2.drawString(getDecimalFormat(2).format(maxValue), 29, 21+5);
                g2.setColor(Color.WHITE);
                g2.drawString(getDecimalFormat(2).format(medValue), 28, 60);
                g2.setColor(Color.BLACK);
                g2.drawString(getDecimalFormat(2).format(medValue), 29, 61);
                g2.setColor(Color.WHITE);
                g2.drawString(getDecimalFormat(2).format(minValue), 28, 100-5);
                g2.setColor(Color.BLACK);
                g2.drawString(getDecimalFormat(2).format(minValue), 29, 101-5);
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

    private static void paintColorBarHorizontal(Graphics2D g, ColorMap cmap, String varName, String units, double minValue,
            double maxValue, boolean showInLog10) {
        
            Graphics2D g2 = (Graphics2D) g.create();
            
            g2.translate(-5, -30);
            g2.setColor(new Color(250, 250, 250, 100));
            g2.fillRect(5, 30, 110 - 8, 70);

            ColorBar cb = new ColorBar(ColorBar.HORIZONTAL_ORIENTATION, cmap);
            cb.setSize(80, 15);
            g2.setColor(Color.WHITE);
            Font prev = g2.getFont();
            g2.translate(15, 45);
            if (varName == null || varName.isEmpty())
                g2.translate(0, -10);
            g2.setColor(cmap.getColor(-0.1));
            g2.fillRect(-2, 0, 1, 15);
            cb.paint(g2);
            g2.setColor(cmap.getColor(1.1));
            g2.fillRect(81, 0, 1, 15);
            g2.translate(-10, -15);
            
            g2.setColor(Color.WHITE);
            g2.drawString(varName, 2, 11);
            g2.setColor(Color.BLACK);
            g2.drawString(varName, 2, 12);

            try {
                double medValue = (maxValue - minValue) / 2. + minValue;
                if (showInLog10) {
                    double minL = Math.log10(minValue);
                    double maxL = Math.log10(maxValue);
                    double vMedL = (maxL - minL) / 2.;
                    medValue = Math.pow(10, maxL - vMedL);
                    System.out.println(String.format("minL=%f   maxL=%f   medL=%f   med=%f", minL, maxL, vMedL, medValue));
                }
                g2.setFont(new Font("Helvetica", Font.PLAIN, 9));
                g2.setColor(Color.WHITE);
                String maxStr = getDecimalFormat(2).format(maxValue);
                Rectangle2D bds = g2.getFontMetrics().getStringBounds(maxStr, g2);
                g2.drawString(maxStr, (int) (85 - 2 - bds.getWidth() / 2.), 40);
                g2.setColor(Color.BLACK);
                g2.drawString(maxStr, (int) (86 - 2 - bds.getWidth() / 2.), 41);
                String medStr = getDecimalFormat(2).format(medValue);
                bds = g2.getFontMetrics().getStringBounds(medStr, g2);
                g2.setColor(Color.WHITE);
                g2.drawString(medStr, (int) (45 + 5 - bds.getWidth() / 2.), (int) (40 + bds.getHeight()));
                g2.setColor(Color.BLACK);
                g2.drawString(medStr, (int) (46 + 5 - bds.getWidth() / 2.), (int) (41 + bds.getHeight()));
                String minStr = getDecimalFormat(2).format(minValue);
                bds = g2.getFontMetrics().getStringBounds(minStr, g2);
                g2.setColor(Color.WHITE);
                g2.drawString(minStr, 5, 40);
                g2.setColor(Color.BLACK);
                g2.drawString(minStr, 6, 41);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }

            g2.setFont(prev);
            g2.setColor(Color.WHITE);
            g2.drawString(units, 10, 65);
            g2.setColor(Color.BLACK);
            g2.drawString(units, 10, 66);

            g2.dispose();
    }

    public static void main(String[] args) {
        
        double min = 0.001;
        double max = 23.71931;

//        min = -3;
//        max = 3;

        JPanel panel = new JPanel();
        panel.setSize(new Dimension(500, 400));
        
        GuiUtils.testFrame(panel, "", 500, 400);
        Graphics2D g = (Graphics2D) panel.getGraphics();
        
//        g.scale(3, 3);
        paintColorBarVertical(g, ColorMapFactory.createJetColorMap(), "sla", "m", min, max, false);
//        g.translate(90, 0);
//        paintColorBarVertical(g, ColorMapFactory.createJetColorMap(), "sla", "m", min, max, true);
        
//        g.scale(2, 2);
        g.translate(80, 0);
        paintColorBarHorizontal(g, ColorMapFactory.createJetColorMap(), "", "m", min, max, false);
        g.translate(0, 90);
        paintColorBarHorizontal(g, ColorMapFactory.createJetColorMap(), "sla", "m", min, max, true);

    }
}
