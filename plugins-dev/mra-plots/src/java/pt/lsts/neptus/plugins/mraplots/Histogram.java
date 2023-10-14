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
 * Author: José Pinto
 * Dec 16, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class Histogram extends JPanel {

    private static final long serialVersionUID = 1L;
    protected BufferedImage histogram = null;

    public Histogram() {
        setData(new double[]{0}, 0, 255);
    }
    
    public void setData(double[] data, double fixedMin, double fixedMax) {

        double min = fixedMin, max = fixedMax;
        if (Double.isNaN(fixedMin) || Double.isNaN(fixedMax)) {
            min = data[0];
            max = data[0];
            
            for (int i = 1; i < data.length; i++) {
                if (data[i] > max)
                    max = data[i];
                if (data[i] < min)
                    min = data[i];
            }
        }
        
        int width = data.length;
        int height = 255;
        histogram = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = histogram.createGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.red);
        for (int i = 0; i < data.length; i++) {
            g.draw(new Line2D.Double(i, height-1, i, height-1-(data[i]/max) * 255));
        }
        
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        if (histogram != null) {
            g.drawImage(histogram, 0, 0, getWidth(), getHeight(), 0, 0, histogram.getWidth(), histogram.getHeight(), this);
        }
    }

    
    public static void main(String[] args) {
        Histogram hist = new Histogram();
        double[] vals = new double[2000];
        for (int i = 0; i < vals.length; i++)
            vals[i] = Math.random() * 255;
        
        hist.setData(vals, 0, 255);
        GuiUtils.testFrame(hist);
    }
    
}
