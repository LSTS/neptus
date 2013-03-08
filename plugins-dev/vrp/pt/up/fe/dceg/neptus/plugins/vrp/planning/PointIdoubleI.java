/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Rui Gonçalves
 * 2010/04/18
 * $Id:: PointIdoubleI.java 9635 2013-01-02 17:52:23Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.plugins.vrp.planning;

import javax.vecmath.Point2d;

import drasys.or.DoubleI;
import drasys.or.geom.rect2.Point;


/**
 * @author Rui Gonçalves
 *
 */
public class PointIdoubleI  extends Point implements DoubleI{
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    double load;
    Point2d point2d;

  

	PointIdoubleI(double ld, Point2d p2d)
    {
        super(p2d.x,p2d.y);
        load = ld;
        
       point2d=p2d;
    }
    
	public void setLoad(double load) {
			this.load = load;
		}
	
	  public Point2d getPoint2d() {
			return point2d;
		}
	 
    public double doubleValue(){return load;}

}
