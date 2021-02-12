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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.ws;

import java.awt.geom.Point2D;

import pt.lsts.neptus.types.coord.UTMCoordinates;

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