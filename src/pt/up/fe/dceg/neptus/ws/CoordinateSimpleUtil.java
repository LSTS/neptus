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
 * 20??/??/??
 * $Id:: CoordinateSimpleUtil.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.ws;

import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.types.coord.UTMCoordinates;

public class CoordinateSimpleUtil {

	/**
	 * Computes the offset (north, east) in meters from
	 * (lat, lon) to (alat, alon) [both of these in decimal degrees].<br>
	 * Subtract the two latlons and come up with the distance
	 * in meters N/S and E/W between them.
	 */
	public static double[] latLonDiff(double lat, double lon, double alat,double alon)
	{

		if (lat == alat && lon == alon)
			return new double[] {0,0};
		
		UTMCoordinates coords1 = new UTMCoordinates(lat, lon);
		UTMCoordinates coords2 = new UTMCoordinates(alat, alon);
		
		double diff[] =  new double[2];
		
		diff[0] = coords2.getNorthing() - coords1.getNorthing();
		diff[1] = coords2.getEasting() - coords1.getEasting();
		return diff;
	}
	
	public static double distance(double lat, double lon, double alat, double alon) {
		if (lat == alat && lon == alon)
			return 0;
		
		UTMCoordinates coords1 = new UTMCoordinates(lat, lon);
		UTMCoordinates coords2 = new UTMCoordinates(alat, alon);
	
		Point2D.Double pt1 = new Point2D.Double(coords1.getEasting(), coords1.getNorthing());
		Point2D.Double pt2 = new Point2D.Double(coords2.getEasting(), coords2.getNorthing());
		
		return pt1.distance(pt2);
	}
}