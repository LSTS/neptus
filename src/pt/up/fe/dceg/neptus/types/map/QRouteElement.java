/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 17, 2011
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Vector;

import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

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
    public void setCenterLocation(LocationType centralLocation) {
        System.out.println("Set center location");
        LocationType prevLocation = new LocationType(getCenterLocation());
        super.setCenterLocation(centralLocation);
        
        double[] offsets = getCenterLocation().getOffsetFrom(prevLocation);
        
        for (Point2D pt : otherPoints) {
            LocationType l = new LocationType(pt.getX(), pt.getY());
            l.translatePosition(offsets);
            l.convertToAbsoluteLatLonDepth();
            pt.setLocation(l.getLatitudeAsDoubleValue(), l.getLongitudeAsDoubleValue());
        }
    }
}
