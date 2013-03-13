/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Mar 12, 2013
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

/**
 * Label to dynamically show the current range of a Sidescan panel
 * @author jqcorreia
 *
 */
public class SidescanRangeLabel extends JPanel {
    private static final long serialVersionUID = 1L;

    private float range = 50;
    private float rangeStep;
    int margin = 3;
    
    
    public SidescanRangeLabel() {
        rangeStep = 10;
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw Horizontal Line
        g2d.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
        
        // Draw the zero
        g2d.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
        g2d.drawString("0", getWidth() / 2 - 10, 10);
        
        //Draw the maxes
        g2d.drawLine(0, 0, 0, getHeight());
        g2d.drawString("" + (int)range, 2 , 10);
        
        g2d.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
        g2d.drawString("" + (int)range, getWidth() - 20, 10);

        double step = (getWidth() / ((range * 2) / rangeStep));
        double r = rangeStep;
        
        int c1 = (int) (getWidth() / 2 - step);
        int c2 = (int) (getWidth() / 2 + step);
        
        for(; c1 > 0; c1 -= step, c2 += step, r += rangeStep) {
            g2d.drawLine(c1, 0, c1, getHeight());
            g2d.drawLine(c2, 0, c2, getHeight());
            g2d.drawString("" + (int)r, c1 , 10);
            g2d.drawString("" + (int)r, c2 - 20, 10);
        }
    }

    /**
     * @return the range
     */
    public float getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(float range) {
        this.range = range;
        rangeStep = 10;
    }
    
    
}
