/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 2010/04/15
 */
package pt.up.fe.dceg.neptus.plugins.vrp.planning;

import javax.vecmath.Point2d;

import drasys.or.DoubleI;
import drasys.or.geom.rect2.Point;
import drasys.or.geom.rect2.PointI;


/**
 * @author Rui Gonçalves
 *
 */

class Customer extends Point implements DoubleI
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	double load;
 
	PointI screenPoint;
    Point2d point2d;
    
    Customer(double ld, PointI projectedPt, PointI screenPt)
    {
        super(projectedPt);
        load = ld;
        screenPoint = screenPt;
    }
    
    Customer(double ld, double xx,double yy, PointI screenPt)
    {
        super(xx,yy);
        load = ld;
        screenPoint = screenPt;
       
    }
    
    
    Customer(double ld, Point2d p2d)
    {
        super(p2d.x,p2d.y);
        load = ld;
        screenPoint = null;
       point2d=p2d;
    }
    
    public void setLoad(double load) {
		this.load = load;
	}
    
    public double doubleValue(){return load;}
}

