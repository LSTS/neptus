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
 * 23/04/2017
 */
package pt.lsts.neptus.plugins.envdisp.painter;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * @author pdias
 *
 */
class EnvDataShapesHelper {

    final static int ARROW_RADIUS = 12;
    final static Path2D.Double arrow = new Path2D.Double();
    static {
        arrow.moveTo(-5, 6);
        arrow.lineTo(0, -6);
        arrow.lineTo(5, 6);
        arrow.lineTo(0, 3);
        arrow.lineTo(-5, 6);
        arrow.closePath();
    }

    final static int CIRCLE_RADIUS = 8;
    final static Ellipse2D circle = new Ellipse2D.Double(-CIRCLE_RADIUS / 2., -CIRCLE_RADIUS / 2., CIRCLE_RADIUS, CIRCLE_RADIUS);
    final static Rectangle2D rectangle = new Rectangle2D.Double(-CIRCLE_RADIUS / 2., -CIRCLE_RADIUS / 2., CIRCLE_RADIUS, CIRCLE_RADIUS);
    
    final static int WIND_BARB_RADIUS = 28;
    final static Path2D.Double windPoleKnots = new Path2D.Double();
    static {
        windPoleKnots.moveTo(0, 0);
        windPoleKnots.lineTo(0, 14*2);
        windPoleKnots.closePath();
    }
    final static Path2D.Double wind50Knots1 = new Path2D.Double();
    static {
        wind50Knots1.moveTo(0, 14*2);
        wind50Knots1.lineTo(-8*2, 14*2);
        wind50Knots1.lineTo(0, 12*2);
        wind50Knots1.closePath();
    }
    final static Path2D.Double wind10Knots1 = new Path2D.Double();
    static {
        wind10Knots1.moveTo(0, 14*2);
        wind10Knots1.lineTo(-8*2, 14*2);
        wind10Knots1.closePath();
    }
    final static Path2D.Double wind5Knots2 = new Path2D.Double();
    static {
        wind5Knots2.moveTo(0, 12*2);
        wind5Knots2.lineTo(-4*2, 12*2);
        wind5Knots2.closePath();
    }
    final static Path2D.Double wind10Knots2 = new Path2D.Double();
    static {
        wind10Knots2.moveTo(0, 12*2);
        wind10Knots2.lineTo(-8*2, 12*2);
        wind10Knots2.closePath();
    }
    final static Path2D.Double wind5Knots3 = new Path2D.Double();
    static {
        wind5Knots3.moveTo(0, 10*2);
        wind5Knots3.lineTo(-4*2, 10*2);
        wind5Knots3.closePath();
    }
    final static Path2D.Double wind10Knots3 = new Path2D.Double();
    static {
        wind10Knots3.moveTo(0, 10*2);
        wind10Knots3.lineTo(-8*2, 10*2);
        wind10Knots3.closePath();
    }
    final static Path2D.Double wind5Knots4 = new Path2D.Double();
    static {
        wind5Knots4.moveTo(0, 8*2);
        wind5Knots4.lineTo(-4*2, 8*2);
        wind5Knots4.closePath();
    }
    final static Path2D.Double wind10Knots4 = new Path2D.Double();
    static {
        wind10Knots4.moveTo(0, 8*2);
        wind10Knots4.lineTo(-8*2, 8*2);
        wind10Knots4.closePath();
    }
    final static Path2D.Double wind5Knots5 = new Path2D.Double();
    static {
        wind5Knots5.moveTo(0, 6*2);
        wind5Knots5.lineTo(-4*2, 6*2);
        wind5Knots5.closePath();
    }

    private EnvDataShapesHelper() {
    }

    /**
     * Will paint the wind barb according with the speed in knots.
     * It will not rotate or any other transformation on the graphics.
     * 
     * @param g
     * @param speedKnots
     */
    static void paintWindBarb(Graphics2D g, double speedKnots) {
        if (speedKnots >= 2) {
            g.draw(windPoleKnots);
        }
        
        if (speedKnots >= 5 && speedKnots < 10) {
            g.draw(wind5Knots2);
        }
        else if (speedKnots >= 10 && speedKnots < 15) {
            g.draw(wind10Knots1);
        }
        else if (speedKnots >= 15 && speedKnots < 20) {
            g.draw(wind10Knots1);
            g.draw(wind5Knots2);
        }
        else if (speedKnots >= 20 && speedKnots < 25) {
            g.draw(wind10Knots1);
            g.draw(wind10Knots2);
        }
        else if (speedKnots >= 25 && speedKnots < 30) {
            g.draw(wind10Knots1);
            g.draw(wind10Knots2);
            g.draw(wind5Knots3);
        }
        else if (speedKnots >= 30 && speedKnots < 35) {
            g.draw(wind10Knots1);
            g.draw(wind10Knots2);
            g.draw(wind10Knots3);
        }
        else if (speedKnots >= 35 && speedKnots < 40) {
            g.draw(wind10Knots1);
            g.draw(wind10Knots2);
            g.draw(wind10Knots3);
            g.draw(wind5Knots4);
        }
        else if (speedKnots >= 40 && speedKnots < 45) {
            g.draw(wind10Knots1);
            g.draw(wind10Knots2);
            g.draw(wind10Knots3);
            g.draw(wind10Knots4);
        }
        else if (speedKnots >= 45 && speedKnots < 50) {
            g.draw(wind10Knots1);
            g.draw(wind10Knots2);
            g.draw(wind10Knots3);
            g.draw(wind10Knots4);
            g.draw(wind5Knots5);
        }
        else if (speedKnots >= 50) {
            g.draw(wind50Knots1);
            g.fill(wind50Knots1);
        }
    }
}
