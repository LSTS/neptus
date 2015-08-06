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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: tsmarques
 * 6 Aug 2015
 */
package pt.lsts.neptus.hyperspectral.display;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import pt.lsts.neptus.hyperspectral.utils.HyperspecUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author tsmarques
 *
 */
public class VerticalLayer{
    private static final float FRAME_OPACITY = 0.9f;

    public BufferedImage dataDisplay;
    private int layerWidth;
    private int layerHeight;

    /* draw frames with opacity */
    private final AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, FRAME_OPACITY);
    private final AffineTransform transform = new AffineTransform();

    public VerticalLayer(int width, int height) {
        init(width, height);
    }

    public BufferedImage getDisplay() {
        return dataDisplay;
    }

    private void init(int width, int height) {
        dataDisplay = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        layerWidth = width;
        layerHeight = height;

        Graphics g = dataDisplay.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, dataDisplay.getWidth(), dataDisplay.getHeight());
        g.dispose();
    }

    public void positionLayer(int posX, int posY) {       
        transform.translate(posX, posY);
        transform.rotate(Math.toRadians(-90), layerWidth / 2, layerHeight / 2);
    }

    public void display(Graphics2D g, StateRenderer2D renderer) {
        g.setTransform(transform);
        g.setComposite(composite);
        g.drawImage(dataDisplay, 0, 0, renderer);
    }

    public void update(byte[] frameBytes) {
        BufferedImage newFrame = HyperspecUtils.rawToBuffImage(frameBytes);

        if(newFrame == null) {
            System.out.println("VerticalLayer: newFrame == null");
            return;    
        }
        /* remove oldest frame */
        dataDisplay = dataDisplay.getSubimage(1, 0, layerWidth - 1, layerHeight);
        /* merge current data with new frame */
        dataDisplay = HyperspecUtils.joinBufferedImage(dataDisplay, newFrame, layerWidth, layerHeight);
    }
}
