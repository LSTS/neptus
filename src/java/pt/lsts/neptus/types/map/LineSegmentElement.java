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
 * Author: José Pinto
 * Nov 17, 2011
 */
package pt.lsts.neptus.types.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class LineSegmentElement extends SimpleMapElement {

    @NeptusProperty
    public double length = 100;
    
    @NeptusProperty
    public Color color = Color.blue;
    
    public LineSegmentElement(MapGroup mg, MapType map) {
        super(mg, map);
    }
    
    public LineSegmentElement() {
        super();
    }
    
    @Override
    public String getTypeAbbrev() {
        return "line";
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D pt = renderer.getScreenPosition(getCenterLocation());
        double lengthScaled = length * renderer.getZoom();
        
        g.translate(pt.getX(), pt.getY());
        
        g.rotate(Math.PI+getYawRad()-renderer.getRotation());
        g.fill(new Ellipse2D.Double(-5, -5, 10, 10));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        g.drawArc((int)-lengthScaled, (int)-lengthScaled, (int)lengthScaled*2, (int)lengthScaled*2, -92, 4);
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(0, 0, 0, lengthScaled));
        
        
        g.setStroke(new BasicStroke(1.5f));        
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));        
        g.draw(new Line2D.Double(0, 0, 0, lengthScaled));
    }
    
    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        return renderer.getZoom() * point.getDistanceInMeters(getCenterLocation()) <= 5;
    }
    
}
