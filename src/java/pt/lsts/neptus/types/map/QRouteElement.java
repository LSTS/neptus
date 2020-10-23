/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author zp
 *
 */
public class QRouteElement extends SimpleMapElement implements ConfigurationListener {

    @NeptusProperty
    public double width = 10;
    
    @NeptusProperty
    public Color color = Color.cyan;
    
    @NeptusProperty
    public String path = "";    
    
    public Vector<Point2D.Double> otherPoints = new Vector<Point2D.Double>();
    
    public String validatePath(String path) {
        try {
            String[] parts = path.split(";");
            for (String part : parts) {
                String p[] = part.split(",");
                Double.parseDouble(p[0]);
                Double.parseDouble(p[1]);
            }
        }
        catch (Exception e) {
            return e.getClass().getSimpleName()+": "+e.getMessage();
        }
        return null;
    }
    
    public QRouteElement() {
        super();
    }
        
    public QRouteElement(MapGroup mg, MapType map) {
        super(mg, map);
    }
    
    public void finish() {
        path = "";
        for (Point2D.Double pt : otherPoints) {
            path += pt.getX()+","+pt.getY()+";";            
        }
        
        path = path.substring(0, path.length()-1);
    }
    
    @Override
    public void propertiesChanged() {
        otherPoints.clear();
        
        String[] points = path.split(";");
        for (String p : points) {
            String[] p2 = p.split(",");
            otherPoints.add(new Point2D.Double(Double.parseDouble(p2[0]), Double.parseDouble(p2[1])));            
        }
    }
    
    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        
        Point2D clicked = renderer.getScreenPosition(point);
        Point2D prevPoint = renderer.getScreenPosition(getCenterLocation());
        
        for (int i = 0; i < otherPoints.size(); i++) {
            Point2D curPoint = renderer.getScreenPosition(new LocationType(otherPoints.get(i).x, otherPoints.get(i).y));
            Line2D l = new Line2D.Double(prevPoint, curPoint);
            
            if (MathMiscUtils.pointLineDistance(clicked, l) < (width / 2) * renderer.getZoom())
                return true;
            
            prevPoint = curPoint;
        }
        
        return false;
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {

        float w = (float)(this.width * renderer.getZoom());
        g.setStroke(new BasicStroke(w, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        GeneralPath gp = new GeneralPath();
        Point2D first = renderer.getScreenPosition(getCenterLocation());
        gp.moveTo(first.getX(), first.getY());
        for (Point2D cur : otherPoints) {            
            Point2D screenCoords = renderer.getScreenPosition(new LocationType(cur.getX(), cur.getY()));
            gp.lineTo(screenCoords.getX(), screenCoords.getY());
        }
        
        g.draw(gp);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(Color.red);
        g.draw(gp);        
    }
    
    @Override
    public String getTypeAbbrev() {
        return "qr";
    }
    
    
    @Override
    public void setCenterLocation(LocationType centralLocation) {
        NeptusLog.pub().info("<###>Set center location");
        LocationType prevLocation = new LocationType(getCenterLocation());
        super.setCenterLocation(centralLocation);
        
        double[] offsets = getCenterLocation().getOffsetFrom(prevLocation);
        
        for (Point2D pt : otherPoints) {
            LocationType l = new LocationType(pt.getX(), pt.getY());
            l.translatePosition(offsets);
            l.convertToAbsoluteLatLonDepth();
            pt.setLocation(l.getLatitudeDegs(), l.getLongitudeDegs());
        }
    }
}
