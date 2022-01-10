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
 * 18 de Dez de 2011
 */
package pt.lsts.neptus.renderer2d;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;

import pt.lsts.neptus.util.ColorUtils;

/**
 * Hovering button to be used on a {@link StateRenderer2D}. 
 * 
 * @author pdias
 *
 */
public class HoveringButton {
    private static final Color COLOR_RED_TRANSP = ColorUtils.setTransparencyToColor(Color.RED, 170);

    private final Image image;
    
    private int xPos = -55; 
    private int yPos = 100;
    private int xRealPos = xPos;
    private int yRealPos = yPos;
    
    private final int width;
    private final int height;
    
    private boolean selected = false;
    private boolean hovering = false;
    private boolean useHoveringAlfa = true;
    private boolean visible = true;
    private boolean isToggle = true;
    
    public HoveringButton(Image image) {
        this.image = image;
        width = image.getWidth(null);
        height = image.getHeight(null);
    }
    
    /**
     * @return the image
     */
    public Image getImage() {
        return image;
    }
    
    public int[] getXYConfiguredPos() {
        return new int[] { xPos, yPos };
    }
    
    public void setXYConfiguredPos(int xPos, int yPos) {
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public int[] getXYRealPos() {
        return new int[] { xRealPos, yRealPos };
    }

    public void setRealXYPos(int xRealPos, int yRealPos) {
        this.xRealPos = xRealPos;
        this.yRealPos = yRealPos;
    }

    public int[] getImageWidthHeight() {
        return new int[] { width, height };
    }

    /**
     * @return the selected
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected) {
        if (!isVisible())
            return;
        if (!isToggle())
            this.selected = true;
        else    
            this.selected = selected;
        onSelectedChange(this.selected);
    }
    
    public void toggleSelected() {
        setSelected(!selected);
    }
    
    /**
     * @return the hovering
     */
    public boolean isHovering() {
        return hovering;
    }
    
    /**
     * @param hovering the hovering to set
     */
    public void setHovering(boolean hovering) {
        if (!isVisible())
            return;
        this.hovering = hovering;
        onHoveringChange(hovering);
    }
    
    /**
     * @return the useHouveringAlfa
     */
    public boolean isUseHoveringAlfa() {
        return useHoveringAlfa;
    }
    
    /**
     * @param useHoveringAlfa the useHoveringAlfa to set
     */
    public void setUseHoveringAlfa(boolean useHoveringAlfa) {
        this.useHoveringAlfa = useHoveringAlfa;
    }
    
    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * @return the isToggle
     */
    public boolean isToggle() {
        return isToggle;
    }
    
    /**
     * @param isToggle the isToggle to set
     */
    public void setToggle(boolean isToggle) {
        this.isToggle = isToggle;
    }
    
    /**
     * Override it if you want to do something everytime {@link #setSelected(boolean)}
     * is called (if is not {@link #isToggle()} the selected parameter is always true).
     * @param selected
     */
    public void onSelectedChange(boolean selected) {
    }

    /**
     * Override it if you want to do something everytime {@link #setHovering(boolean)}
     * is called.
     * @param hovering
     */
    public void onHoveringChange(boolean hovering) {
    }

    /**
     * 
     */
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!isVisible())
            return;
        
        Graphics2D g2 = (Graphics2D) g.create();
        
        int x = xPos, y = yPos;
        int[] realXYPos = calcRealXYPos(x, y, width, height, renderer.getWidth(), renderer.getHeight());
        x = realXYPos[0];
        y = realXYPos[1];
        
        int type = AlphaComposite.SRC_OVER;
        g2.setComposite(AlphaComposite.getInstance(type, !useHoveringAlfa ? 0.8f : (hovering ? 0.8f : 0.3f)));
        g2.drawImage(image, x, y, width, height, null);

        if (selected && isToggle) {
            g2.setColor(COLOR_RED_TRANSP);
            float sz = width / 3.0f;
            g2.setStroke(new BasicStroke(sz));
            g2.drawLine((int)(x + width - sz / 2), (int)(y + sz / 2), (int)(x + sz / 2), (int)(y + height - sz / 2));
        }
        xRealPos = x;
        yRealPos = y;
        g2.dispose();
    }

    /**
     * @return 
     * 
     */
    public Rectangle2D createRectangle2DBounds() {
        return new Rectangle2D.Double(xRealPos, yRealPos, width, height);
    }
    
    /**
     * @param xPos
     * @param yPos
     * @param iconWidth
     * @param iconHeight
     * @param rendererWidth
     * @param rendererHeight
     * @return
     */
    public static int[] calcRealXYPos(int xPos, int yPos, int iconWidth, int iconHeight, int rendererWidth,
            int rendererHeight) {
        int x = xPos, y = yPos;
        if (x < 0)
            x = rendererWidth + x;
        if (y < 0)
            y = rendererHeight + x;
        if (x + iconWidth + 2 >= rendererWidth)
            x = Math.max(0, x - 2 - (x + iconWidth + 2 - rendererWidth));
        if (y + iconHeight + 2 >= rendererHeight)
            y = Math.max(0, y - 2 - (y + iconHeight + 2 - rendererHeight));
        return new int[] { x, y };
    }
}