/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Miguel Carvalho
 * 2024/11/04
 */
package pt.lsts.neptus.util;

import javax.swing.ImageIcon;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.image.ImageObserver;

public class RenderStringUtils {

    /** To avoid instantiation */
    private RenderStringUtils() {
    }

    /**
     * Renders an outline of string text.
     *
     * @param g
     * @param font
     * @param textColor
     * @param outlineColor
     * @param text
     * @param x
     * @param y
     * @return
     */
    public static void drawStringWOutline(Graphics2D g, Font font, Color textColor, Color outlineColor, String text, double x, double y) {
        Graphics2D gTemp = (Graphics2D) g.create();
        try {
            gTemp.setFont(font);
            gTemp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gTemp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Calculate dynamic outline thickness based on font size
            float outlineThickness = font.getSize() * 0.25f;

            GlyphVector glyphVector = font.createGlyphVector(gTemp.getFontRenderContext(), text);
            Shape textShape = glyphVector.getOutline((int) (x), (int) (y));

            // Draw the outline
            gTemp.setColor(outlineColor);
            gTemp.setStroke(new BasicStroke(outlineThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); // Thickness of the outline
            gTemp.draw(textShape);

            // Draw the fill
            gTemp.setColor(textColor);
            gTemp.fill(textShape);
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            gTemp.dispose();
        }
    }

    public static void drawStringVideo(Graphics2D g, Font font, Color textColor, String text, double widthConsole, double x, double y, Image icon, int iconSpacing, int lineHeight, boolean background, boolean rightSide, ImageObserver io) {
        Graphics2D gTemp = (Graphics2D) g.create();
        Graphics2D gBackground = (Graphics2D) g.create();
        try {
            gTemp.setFont(font);
            gTemp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gTemp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            GlyphVector glyphVector = font.createGlyphVector(gTemp.getFontRenderContext(), text);
            Shape textShape = glyphVector.getOutline((int) (x) + icon.getWidth(io) + iconSpacing, (int) (y));

            FontMetrics fm = gTemp.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();

            int rectX = -10;
            int rectY = (int) ((int) y - (icon.getHeight(io) * 0.7) - ((double) lineHeight / 10));
            int rectWidth = (int) (x + icon.getWidth(io) + iconSpacing + textWidth) + (-2 * rectX);
            int rectHeight = (int) (icon.getHeight(io) + ((double) lineHeight / 5));

            if (rightSide) {
                textShape = glyphVector.getOutline((float) ((int) (x) - textWidth - 10) , (int) (y));
                rectX = (int) x - icon.getWidth(io) - textWidth - (iconSpacing) - (-2 * rectX);
            }

            if (background) {
                AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); // 50% opacity
                gBackground.setComposite(composite);
                gBackground.setColor(Color.BLACK);
                gBackground.fillRoundRect(rectX, rectY, rectWidth, rectHeight, 20, 20);
            }

            gTemp.setColor(textColor);
            gTemp.fill(textShape);

            if (rightSide) {
                gTemp.drawImage(icon, (int) x - icon.getWidth(io) - textWidth - (iconSpacing) - 10, (int) ((int) y - (icon.getHeight(io) * 0.7)), io);
            }
            else {
                gTemp.drawImage(icon, (int) x, (int) ((int) y - (icon.getHeight(io) * 0.7)), io);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            gBackground.dispose();
            gTemp.dispose();
        }
    }
}
