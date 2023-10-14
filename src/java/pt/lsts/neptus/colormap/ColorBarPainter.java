/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 29, 2018
 */
package pt.lsts.neptus.colormap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
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
@SuppressWarnings("serial")
public class ColorBarPainter extends JPanel {

    private ColorMap colormap = ColorMapFactory.createJetColorMap();
    private ColorBar colorBar = new ColorBar(ColorBar.HORIZONTAL_ORIENTATION, colormap);
    private BufferedImage cachedImage = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
    
    private boolean invalidateCache = true;

    private double minVal = 0;
    private double maxVal = 1;
    private boolean isLog10 = false;
    
    private boolean outliersBoxFill = true;
    
    /**
     * 
     */
    public ColorBarPainter() {
        this.setOpaque(false);
    }    
    
    private void forceRepaint() {
        invalidateCache = true;
        repaint();
    }

    /**
     * @return the colormap
     */
    public ColorMap getColormap() {
        return colormap;
    }
    
    /**
     * @param colormap the colormap to set
     */
    public void setColormap(ColorMap colormap) {
        if (colormap == null)
            return;
        this.colormap = colormap;
        this.colorBar.setCmap(colormap);
        forceRepaint();
    }

    /**
     * @return the minVal
     */
    public double getMinVal() {
        return minVal;
    }
    
    /**
     * @param minVal the minVal to set
     */
    public void setMinVal(double minVal) {
        this.minVal = minVal;
        forceRepaint();
    }
    
    /**
     * @return the maxVal
     */
    public double getMaxVal() {
        return maxVal;
    }
    
    /**
     * @param maxVal the maxVal to set
     */
    public void setMaxVal(double maxVal) {
        this.maxVal = maxVal;
        forceRepaint();
    }
    
    /**
     * @return the isLog10
     */
    public boolean isLog10() {
        return isLog10;
    }
    
    /**
     * @param isLog10 the isLog10 to set
     */
    public void setLog10(boolean isLog10) {
        this.isLog10 = isLog10;
        forceRepaint();
    }
    
    /**
     * @return the outliersBoxFill
     */
    public boolean isOutliersBoxFill() {
        return outliersBoxFill;
    }
    
    /**
     * @param outliersBoxFill the outliersBoxFill to set
     */
    public void setOutliersBoxFill(boolean outliersBoxFill) {
        this.outliersBoxFill = outliersBoxFill;
        forceRepaint();
    }
    
    private static NumberFormat getDecimalFormat(int fractionDigits) {
        NumberFormat df = DecimalFormat.getInstance(Locale.US);
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(fractionDigits);
        return df;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#paint(java.awt.Graphics)
     */
    @Override
    public void paint(Graphics go) {
        Dimension dim = ColorBarPainter.this.getSize();
        
        boolean cacheInvalidated = invalidateCache || getWidth() != cachedImage.getWidth()
                || getHeight() != cachedImage.getHeight();

        if (cacheInvalidated) {
            cachedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) cachedImage.getGraphics();
            
            g.setColor(new Color(250, 250, 250, 100));
            g.fillRoundRect(0, 0, (int) dim.getWidth(), (int) dim.getHeight(), (int) (dim.getWidth() * 0.05),
                    (int) (dim.getHeight() * 0.05));

            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate((dim.getWidth() * 0.02), (dim.getHeight() * 0.95));
            g2.setColor(Color.BLACK);
            try {
                double medValue = (maxVal - minVal) / 2. + minVal;
                if (isLog10) {
                    double minL = Math.log10(minVal);
                    double maxL = Math.log10(maxVal);
                    double vMedL = (maxL - minL) / 2.;
                    medValue = Math.pow(10, maxL - vMedL);
                }
                
                String minStr = getDecimalFormat(2).format(minVal);
                g2.setColor(Color.BLACK);
                g2.drawString(minStr, 0, 0);

                String maxStr = getDecimalFormat(2).format(maxVal);
                Rectangle2D bdsMaxTxt = g2.getFontMetrics().getStringBounds(maxStr, g2);
                g2.setColor(Color.BLACK);
                g2.drawString(maxStr, (int) (dim.getWidth() * 0.96 - bdsMaxTxt.getWidth()), 0);

                String medStr = getDecimalFormat(2).format(medValue);
                Rectangle2D bdsMedTxt = g2.getFontMetrics().getStringBounds(medStr, g2);
                g2.setColor(Color.BLACK);
                g2.drawString(medStr, (int) (dim.getWidth() * 0.48 - bdsMedTxt.getWidth() / 2.), 0);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }
            g2.dispose();

            g2 = (Graphics2D) g.create();
            colorBar.setSize((int) (dim.getWidth() * 0.9), (int) (dim.getHeight() * 0.55));
            g2.translate((dim.getWidth() * 0.05), (dim.getHeight() * 0.05));
            colorBar.paint(g2);
            g2.dispose();

            g2 = (Graphics2D) g.create();
            g2.translate((dim.getWidth() * 0.02), (dim.getHeight() * 0.05));
            if (outliersBoxFill) {
                g2.setColor(colormap.getColor(-0.1));
                g2.fillRect(0, 0, (int) (dim.getWidth() * 0.02), (int) (dim.getHeight() * 0.55));
            }
            else {
                g2.setColor(Color.BLACK);
                g2.drawRect(0, 0, (int) (dim.getWidth() * 0.02), (int) (dim.getHeight() * 0.55));
            }
            g2.translate((dim.getWidth() * 0.94), 0);
            if (outliersBoxFill) {
                g2.setColor(colormap.getColor(1.1));
                g2.fillRect(0, 0, (int) (dim.getWidth() * 0.02), (int) (dim.getHeight() * 0.55));
            }
            else {
                g2.setColor(Color.BLACK);
                g2.drawRect(0, 0, (int) (dim.getWidth() * 0.02), (int) (dim.getHeight() * 0.55));
            }
            g2.dispose();
        }
        
        go.drawImage(cachedImage, 0, 0, null);
    }
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        ColorBarPainter cb = new ColorBarPainter();
        GuiUtils.testFrame(cb, "", 200, 75);

        Thread.sleep(2000);
        cb.setMinVal(0.001);
        cb.setMaxVal(23.71931);

        Thread.sleep(2000);
        cb.setLog10(true);
        
        cb.setOutliersBoxFill(false);
        for (String c : ColorMapFactory.colorMapNamesList) {
            Thread.sleep(2000);
            System.out.println(c);
            cb.setOutliersBoxFill(false);
            cb.setColormap(ColorMapFactory.getColorMapByName(c));
            Thread.sleep(1000);
            cb.setOutliersBoxFill(true);
        }
    }
}
