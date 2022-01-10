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
 * Author: zp
 * 30/05/2017
 */
package pt.lsts.neptus.types.coord;

/**
 * @author zp
 *
 */
import static java.lang.Double.NaN;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;

public abstract class Areas {
    public static double approxArea(Area area, double flatness, int limit) {
        PathIterator i = new FlatteningPathIterator(area.getPathIterator(identity), flatness, limit);
        return approxArea(i);
    }

    public static double approxArea(Area area, double flatness) {
        PathIterator i = area.getPathIterator(identity, flatness);
        return approxArea(i);
    }

    public static double approxArea(PathIterator i) {
        double a = 0.0;
        double[] coords = new double[6];
        double startX = NaN, startY = NaN;
        Line2D segment = new Line2D.Double(NaN, NaN, NaN, NaN);
        while (!i.isDone()) {
            int segType = i.currentSegment(coords);
            double x = coords[0], y = coords[1];
            switch (segType) {
                case PathIterator.SEG_CLOSE:
                    segment.setLine(segment.getX2(), segment.getY2(), startX, startY);
                    a += hexArea(segment);
                    startX = startY = NaN;
                    segment.setLine(NaN, NaN, NaN, NaN);
                    break;
                case PathIterator.SEG_LINETO:
                    segment.setLine(segment.getX2(), segment.getY2(), x, y);
                    a += hexArea(segment);
                    break;
                case PathIterator.SEG_MOVETO:
                    startX = x;
                    startY = y;
                    segment.setLine(NaN, NaN, x, y);
                    break;
                default:
                    throw new IllegalArgumentException("PathIterator contains curved segments");
            }
            i.next();
        }
        if (Double.isNaN(a)) {
            throw new IllegalArgumentException("PathIterator contains an open path");
        }
        else {
            return 0.5 * Math.abs(a);
        }
    }

    private static double hexArea(Line2D seg) {
        return seg.getX1() * seg.getY2() - seg.getX2() * seg.getY1();
    }

    private static final AffineTransform identity = AffineTransform.getQuadrantRotateInstance(0);
}