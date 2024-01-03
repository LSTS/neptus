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
 * Author: hfq
 * Feb 18, 2014
 */
package pt.lsts.neptus.plugins.echosounder;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JLabel;

/**
 * @author hfq
 *
 */
public class EchoSounderMRARuler extends JLabel {

    private static final long serialVersionUID = 1L;

    private EchoSounderMRA echoSounderMRA;

    public final static int RULER_WIDTH = 20;

    protected int maxValue;
    protected int minValue;

    private int rulerHeight;

    /**
     * @param echoSounderMRA 
     */
    public EchoSounderMRARuler(EchoSounderMRA echoSounderMRA) {
        super();
        this.echoSounderMRA = echoSounderMRA;
        this.maxValue = echoSounderMRA.maxRange;
        this.minValue = echoSounderMRA.minRange;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        rulerHeight = echoSounderMRA.getHeight() - 1;

        Rectangle drawRulerHere = new Rectangle(0, 0, RULER_WIDTH, rulerHeight);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fill(drawRulerHere);

        // Do the ruler labels in a small font that's black.
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g2d.setColor(Color.BLACK);

        g2d.drawLine(0, 0, 0, rulerHeight);
        g2d.drawLine(RULER_WIDTH, 0, RULER_WIDTH, rulerHeight);

        double stepRuler = (double) rulerHeight / (double) (maxValue - minValue);

        int i = 0;
        for(double step = 0; i <= maxValue; step += stepRuler, ++i) {
            if ((i % 10) == 0) {
                g2d.drawLine(RULER_WIDTH - (RULER_WIDTH / 3), (int) step, RULER_WIDTH, (int) step);
                if (i == minValue)
                    g2d.drawString("" + i, 5, (int) step + 7);
                else if (i == maxValue)
                    g2d.drawString("" + i, 1, (int) step - 1);
                else
                    g2d.drawString("" + i, 1, (int)  step + 3);
            }
            else if ((i % 5) == 0){
                g2d.drawLine(RULER_WIDTH - (RULER_WIDTH / 4), (int) step, RULER_WIDTH, (int) step);
            }
            else {
                g2d.drawLine(RULER_WIDTH - (RULER_WIDTH / 6), (int) step, RULER_WIDTH, (int) step);
            }
        }
    }
}
