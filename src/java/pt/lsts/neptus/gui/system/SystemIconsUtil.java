/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 10 de Dez de 2011
 */
package pt.lsts.neptus.gui.system;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class SystemIconsUtil {

    private final static GeneralPath uavShape = new GeneralPath();
    static {
        uavShape.moveTo(-20 / 2, 14 / 2);
        uavShape.lineTo(0, -14 / 2);
        uavShape.lineTo(20 / 2, 14 / 2);
        uavShape.lineTo(0, 14 / 2 * 3 / 5);
        uavShape.closePath();
        
//        ret.moveTo((-width/2)    ,   (-height/2));
//        ret.lineTo(0             ,   (height/2));
//        ret.lineTo((width/2)     ,   (-height/2));
//        ret.lineTo(0             ,   ((-height/2)*3/5));
    }
    
    private final static GeneralPath uavAltShape = new GeneralPath();
    static {
        int width = 20, height = width / 2; 
        uavAltShape.moveTo(width / 4, height / 3);

        // plane's nose
        uavAltShape.lineTo(width / 4, height / 3);
        uavAltShape.curveTo(width / 2 + 1, height / 3, width / 2 + 1, -height / 8, width / 4, -height / 8);

        // plane's wing
        uavAltShape.lineTo(width / 4 - 2, -height / 8);
        uavAltShape.lineTo(width / 4 - 6, -height / 8 - 6);
        uavAltShape.lineTo(width / 4 - 8, -height / 8 - 6);
        uavAltShape.lineTo(width / 4 - 8, -height / 8);

        // plane's tail
        uavAltShape.lineTo(-width / 4 - 1, -height / 8);
        uavAltShape.lineTo(-width * 5 / 12, -height / 2);
        uavAltShape.lineTo(-width / 2, -height / 2);
        uavAltShape.lineTo(-width / 2, 0);
        uavAltShape.lineTo(-width / 4, height / 3);

        uavAltShape.closePath();
    }
    
    private final static GeneralPath uuvShape = new GeneralPath();
    static {
        uuvShape.moveTo(-2,10);
        uuvShape.lineTo(2, 10);
        uuvShape.lineTo(2, 0);
        uuvShape.lineTo(5, 0);
        uuvShape.lineTo(0, -10);
        uuvShape.lineTo(-5, 0);
        uuvShape.lineTo(-2, 0);
        uuvShape.closePath();
    }

    private final static Shape circleFillShape = new Ellipse2D.Double(-20 / 10., -20 / 10., 20 / 5., 20 / 5.);
    
    private SystemIconsUtil() {
    }
    
    public static GeneralPath getUAV() {
        return uavShape;
    }

    public static GeneralPath getUAVAlt() {
        return uavAltShape;
    }

    public static Shape getCircle() {
        return circleFillShape;
    }

    /**
     * 
     */
    public static GeneralPath getUUV() {
        return uuvShape;
    }
    
    @SuppressWarnings("serial")
    public static void main(String[] args) {
        JLabel lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GeneralPath gp = getUUV();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.fill(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

        lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GeneralPath gp = getUAV();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.fill(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

        lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GeneralPath gp = getUAVAlt();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.draw(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

        lb = new JLabel() {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Shape gp = getCircle();
                int w = this.getWidth();
                int h = this.getHeight();
                Rectangle2D bound = gp.getBounds2D();
                double iw = bound.getWidth();
                double ih = bound.getHeight();
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Color.GREEN);
                g2.translate(w / 2, h / 2);
                g2.scale(w/Math.max(iw, ih), h/Math.max(iw, ih));
                g2.fill(gp);
            }
        };
        GuiUtils.testFrame(lb, "test", 300, 300);

    }
}
