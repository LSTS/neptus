/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 17 de Nov de 2012
 */
package pt.lsts.neptus.gui.system;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.jdesktop.swingx.JXPanel;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class FuelLevelSymbol extends SymbolLabel {

    private double percentage = -1;
    
    public FuelLevelSymbol() {
    }

    /**
     * @return the percentage
     */
    public double getPercentage() {
        return percentage;
    }
    
    /**
     * @param percentage the percentage to set
     */
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.system.SymbolLabel#paint(java.awt.Graphics2D, org.jdesktop.swingx.JXPanel, int, int)
     */
    @Override
    public void paint(Graphics2D g, JXPanel c, int width, int height) {
//        if (blinkingState == BlinkingStateEnum.BLINKING_BRILLIANT) {
//            NeptusLog.pub().info("<###>FuelLevel state: " + blinkingState + "  ::   " + (System.currentTimeMillis() - time) + "ms");
//        }
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.scale(width/10.0, height/10.0);
        
        Color cErase = new Color(0,0,0,0); 

        RoundRectangle2D rect = new RoundRectangle2D.Double(0,0,10,10, 0,0);
        g2.setColor(cErase);
        g2.fill(rect);
        
        double value = (long) (MathMiscUtils.clamp(percentage, 0.0, 100.0)) / 100.0;
        int xwAdd = 0;
        double threshold = 0.1;
        if (value > threshold || percentage < 0)
            xwAdd = 2;

        int x = 4, y = 1, xw = 3 + xwAdd, yh = 8;

        g2.setColor(getActiveColor());

        rect = new RoundRectangle2D.Double(x, y, xw, yh, 0, 0);
        if (percentage >= 0) {
            g2.draw(rect);
        }
        
        g2.setFont(new Font("Arial", Font.BOLD, 5));
        g2.drawString("F", 0, 4);
        g2.drawString("L", 0, 9);

        if (!isActive() || percentage < 0) {
            g2.setColor(getActiveColor());
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.drawString("?", 4 - (2 - xwAdd) / 2, 8);
        }
        if (percentage >= 0) {
            g2.setColor(getActiveColor());
            if (value <= threshold) {
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.drawString("!", 7, 9);
            }

            rect = new RoundRectangle2D.Double(x, y + yh * (1 - value), xw, yh - yh * (1 - value), 0, 0);
            g2.fill(rect);
        }
        if (!isActive() && percentage >= 0) {
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            Graphics2D g2tmp = (Graphics2D) g2.create();
//            g2tmp.setXORMode(getActiveColor());
            g2tmp.setColor(cErase);
            g2tmp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT));
            g2tmp.clip(rect);
            g2tmp.drawString("?", 4 - (2 - xwAdd) / 2, 8);
            g2tmp.dispose();
            if (xwAdd == 0) {
                Rectangle2D rect1 = new Rectangle2D.Double(x-(xw/2.), y, xw/2.+.5, yh);
                g2tmp = (Graphics2D) g2.create();
//                g2tmp.setXORMode(getActiveColor());
                g2tmp.setColor(cErase);
                g2tmp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT));
                g2tmp.clip(rect1);
                g2tmp.drawString("?", 4 - (2 - xwAdd) / 2, 8);
                g2tmp.dispose();
                Rectangle2D rect2 = new Rectangle2D.Double(x+xw-.5, y, xw/2.+.5, yh);
                g2tmp = (Graphics2D) g2.create();
//                g2tmp.setXORMode(getActiveColor());
                g2tmp.setColor(cErase);
                g2tmp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OUT));
                g2tmp.clip(rect2);
                g2tmp.drawString("?", 4 - (2 - xwAdd) / 2, 8);
                g2tmp.dispose();
            }
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        FuelLevelSymbol symb1 = new FuelLevelSymbol();
        symb1.setSize(20, 50);
        symb1.setActive(true);
        JXPanel panel = new JXPanel();
        panel.setBackground(Color.BLACK);
        panel.setLayout(new BorderLayout());
        panel.add(symb1, BorderLayout.CENTER);
        GuiUtils.testFrame(panel, "", 300, 300);
        
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(0);
//        symb1.setActive(false);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(9.3);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(13.2);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(34.1);
//        symb1.setActive(false);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(50.8);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(66.6);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(74.2);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(88.5);
//        symb1.setActive(true);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(98.3);
//        symb1.repaint();
//
//        try { Thread.sleep(2000); } catch (InterruptedException e) { }
//        symb1.setPercentage(100);
//        symb1.repaint();

    }
}
