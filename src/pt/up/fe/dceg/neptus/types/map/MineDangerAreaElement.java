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
 * $Id:: MineDangerAreaElement.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class MineDangerAreaElement extends SimpleMapElement {

    @NeptusProperty
    public double radius = 50;
    
    @NeptusProperty
    public Color color = Color.red;
    
    public MineDangerAreaElement(MapGroup mg, MapType map) {
        super(mg, map);
    }
    
    public MineDangerAreaElement() {
        super();
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D pt = renderer.getScreenPosition(getCenterLocation());
        g.translate(pt.getX(), pt.getY());
        g.rotate(getYawRad()-renderer.getRotation());
        
        double widthScaled = radius * 2 * renderer.getZoom();
        double lengthScaled = radius * 2 * renderer.getZoom();
        
        Ellipse2D.Double tmp = new Ellipse2D.Double(-widthScaled/2, -lengthScaled/2, widthScaled, lengthScaled);
        
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        
        g.fill(tmp);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        g.setStroke(new BasicStroke(2));
        g.draw(tmp);
    }
    
    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        return point.getDistanceInMeters(getCenterLocation()) <= radius;
    }
    
}
