/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 07/10/2016
 */
package pt.lsts.neptus.mra.api;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * @author pdias
 *
 */
public class SidescanGuiUtils {

    private SidescanGuiUtils() {
    }

    /**
     * Return a step for the ruler for a range in meters.
     *  
     * @param rangeForRuler
     * @return
     */
    public static int calcStepForRangeForRuler(int rangeForRuler) {
        int rangeForRulerStep = 1;
        if (rangeForRuler > 100)
            rangeForRulerStep = 20;
        else if (rangeForRuler > 10)
            rangeForRulerStep = 10;
        
        return rangeForRulerStep;
    }
    
    /**
     * Draws a ruler in a image for sidescan.
     * 
     * @param layerImage The image to draw.
     * @param maxRullerVerticalSizePixels The vertical size in pixels for the ruler.
     * @param rangeForRulerMeters The range of the sidescan in meters.
     * @param rangeForRulerStepMeters The range steps in meters for the ruler.
     */
    public static void drawRuler(BufferedImage layerImage, int maxRullerVerticalSizePixels, int rangeForRulerMeters,
            int rangeForRulerStepMeters) {
        
        Graphics g = layerImage.getGraphics();
        Graphics2D g2d = (Graphics2D) g.create();

        int fontSize = 11;

        // Draw Horizontal Line
        g2d.drawLine(0, 0, layerImage.getWidth(), 0);

        Rectangle drawRulerHere = new Rectangle(0, 0, layerImage.getWidth(), maxRullerVerticalSizePixels);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(drawRulerHere);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        // Draw top line
        g2d.drawLine(0, 0, layerImage.getWidth(), 0);

        // Draw the zero
        g2d.drawLine(layerImage.getWidth() / 2, 0, layerImage.getWidth() / 2, maxRullerVerticalSizePixels);
        g2d.drawString("0", layerImage.getWidth() / 2 - 10, fontSize);

        // Draw the axes
        g2d.drawLine(0, 0, 0, 15);
        g2d.drawString("" + (int) rangeForRulerMeters, 2, 11);

        g2d.drawLine(layerImage.getWidth() - 1, 0, layerImage.getWidth() - 1, maxRullerVerticalSizePixels);
        g2d.drawString("" + (int) rangeForRulerMeters, layerImage.getWidth() - 20, fontSize);

        double step = (layerImage.getWidth() / ((rangeForRulerMeters * 2) / rangeForRulerStepMeters));
        double r = rangeForRulerStepMeters;

        int c1 = (int) (layerImage.getWidth() / 2 - step);
        int c2 = (int) (layerImage.getWidth() / 2 + step);

        for (; c1 > 0; c1 -= step, c2 += step, r += rangeForRulerStepMeters) {
            g2d.drawLine(c1, 0, c1, maxRullerVerticalSizePixels);
            g2d.drawLine(c2, 0, c2, maxRullerVerticalSizePixels);
            g2d.drawString("" + (int) r, c1 + 5, fontSize);
            g2d.drawString("" + (int) r, c2 - 20, fontSize);
        }

        g2d.dispose();
    }

}
