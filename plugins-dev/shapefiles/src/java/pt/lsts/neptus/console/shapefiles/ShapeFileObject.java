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
 * 31/01/2015
 */
package pt.lsts.neptus.console.shapefiles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.PointData;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.MultiPointZShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PointShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolygonShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PolylineShape;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author pdias
 *
 */
public class ShapeFileObject {

    private Ellipse2D circle = new Ellipse2D.Double(-4, -4, 8, 8);
    
    private String name;
    private ArrayList<AbstractShape> shapes;
    
    private boolean visible = true;
    
    private Color color = Color.CYAN;
    
    public ShapeFileObject(String name, ArrayList<AbstractShape> shapes) {
        this.name = name;
        this.shapes = shapes;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return the shapes
     */
    public ArrayList<AbstractShape> getShapes() {
        return shapes;
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
     * @return the color
     */
    public Color getColor() {
        return color;
    }
    
    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }
    
    public void paintObject(StateRenderer2D renderer, Graphics2D g) {
        if (!isVisible())
            return;
        
        for (AbstractShape shp : shapes) {
            switch (shp.getShapeType()) {
                case POINT:
                    paintPointShape(shp, renderer, g);
                    break;
                case MULTIPOINT_Z:
                    paintMultiPointZShape(shp, renderer, g);
                    break;
                case POLYGON:
                    paintPolygonShape(shp, renderer, g);
                    break;
                case POLYLINE:
                    paintPolyLineShape(shp, renderer, g);
                    break;
                case MULTIPATCH:
                case MULTIPOINT:
                case MULTIPOINT_M:
                case NULL:
                case POINT_M:
                case POINT_Z:
                case POLYGON_M:
                case POLYGON_Z:
                case POLYLINE_M:
                case POLYLINE_Z:
                default:
                  ; // System.out.println("Read other type of shape." + shp.getShapeType());

            }
        }
    }

    private void paintPointShape(AbstractShape shp, StateRenderer2D renderer, Graphics2D g) {
        PointShape s = (PointShape) shp;
        LocationType loc = getAsLocation(s.getY(), s.getX());
        paintPointShape(loc, renderer, g);
    }

    private void paintPointShape(LocationType loc, StateRenderer2D renderer, Graphics2D g) {
        Point2D pt = renderer.getScreenPosition(loc);
        Graphics2D gt = (Graphics2D) g.create();
        gt.translate(pt.getX(), pt.getY());
        gt.setColor(color.darker());
        gt.fill(circle);
        gt.dispose();
    }

    private void paintMultiPointZShape(AbstractShape shp, StateRenderer2D renderer, Graphics2D g) {
        MultiPointZShape s = (MultiPointZShape) shp;
        for (PointData pd : s.getPoints()) {
            LocationType loc = getAsLocation(pd.getY(), pd.getX());
            paintPointShape(loc, renderer, g);
        }
    }

    private void paintPolygonShape(AbstractShape shp, StateRenderer2D renderer, Graphics2D g) {
        PolygonShape s = (PolygonShape) shp;
        Graphics2D gt = (Graphics2D) g.create();
        gt.setColor(color);

        for (int i = 0; i < s.getNumberOfParts(); i++) {
            PointData[] points = s.getPointsOfPart(i);
            GeneralPath gp = new GeneralPath();
            Point2D firstPoint = null;
            for (PointData pd : points) {
                LocationType loc = getAsLocation(pd.getY(), pd.getX());
                Point2D pt = renderer.getScreenPosition(loc);
                if (firstPoint == null) {
                    firstPoint = pt;
                    gp.moveTo(pt.getX(), pt.getY());
                }
                else {
                    gp.lineTo(pt.getX(), pt.getY());
                }
            }
            if (firstPoint != null) {
                gp.lineTo(firstPoint.getX(), firstPoint.getY());
                gt.fill(gp);
            }
        }
        gt.dispose();
    }

    private void paintPolyLineShape(AbstractShape shp, StateRenderer2D renderer, Graphics2D g) {
        PolylineShape s = (PolylineShape) shp;
        Graphics2D gt = (Graphics2D) g.create();
        gt.setColor(color.darker());

        for (int i = 0; i < s.getNumberOfParts(); i++) {
            PointData[] points = s.getPointsOfPart(i);
            GeneralPath gp = new GeneralPath();
            boolean first = true;
            for (PointData pd : points) {
                LocationType loc = getAsLocation(pd.getY(), pd.getX());
                Point2D pt = renderer.getScreenPosition(loc);
                if (first) {
                    first = false;
                    gp.moveTo(pt.getX(), pt.getY());
                }
                else {
                    gp.lineTo(pt.getX(), pt.getY());
                }
            }
            gt.draw(gp);
        }
        gt.dispose();
    }

    private LocationType getAsLocation(double lat, double lon) {
        return new LocationType(lat, lon);
    }
}
