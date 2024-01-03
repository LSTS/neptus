/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 2006/02/23
 */
package pt.lsts.neptus.types.coord;

import pt.lsts.neptus.NeptusLog;

/**
 * Code from http://www.gpsy.com/gpsinfo/geotoutm/gantz/LatLong-UTMconversion.cpp
 * @author ZP
 * @author pdias (fixed)
 *
 */
public class UTMCoordinates {
	private final double wgs84Radius = 6378137.0d; 
	private final double wgs84EccSquared = 0.00669438d;
	
	private double easting = 0.0;
	private double northing = 0.0;

	private double latitudeDegrees = 0.0;
	private double longitudeDegrees = 0.0;
	
	private int zoneNumber = 0;
	
	private char zoneLetter = 'N';
	
	public UTMCoordinates(double easting, double northing, int zoneNumber, char zoneLetter) {
		this.northing = northing;
		this.easting = easting;
		this.zoneNumber = zoneNumber;
		this.zoneLetter = zoneLetter;
		UTMtoLL();
	}
	
	public UTMCoordinates(double latitudeDegrees, double longitudeDegrees) {
		this.latitudeDegrees = latitudeDegrees;
		this.longitudeDegrees = longitudeDegrees;
		LLtoUTM();		
		//NeptusLog.pub().info("<###> "+latitude);
		//NeptusLog.pub().info("<###> "+longitude);
	}
	
	public void UTMtoLL() {
        //check the ZoneNummber is valid
        if (zoneNumber < 0 || zoneNumber > 60) {
            return;
        }

        double k0 = 0.9996;
        double a = wgs84Radius;
        double eccSquared = wgs84EccSquared;
        double eccPrimeSquared;
        double e1 = (1 - Math.sqrt(1 - eccSquared))
                / (1 + Math.sqrt(1 - eccSquared));
        double N1, T1, C1, R1, D, M;
        double longOrigin;
        double mu, phi1Rad;

        // remove 500,000 meter UTM standard offset for longitude
        double x = easting - 500000.0; 
        double y = northing;

        //We must know somehow if we are in the Northern or Southern
        //hemisphere, this is the only time we use the letter So even
        //if the Zone letter isn't exactly correct it should indicate
        //the hemisphere correctly
        if (zoneLetter < 'N') {
            y -= 10000000.0d;//remove 10,000,000 meter offset used
                             // for southern hemisphere
        }

        //There are 60 zones with zone 1 being at West -180 to -174
        
        longOrigin = (zoneNumber - 1) * 6 - 180 + 3; //+3 puts origin
                                                     // in middle of
                                                     // zone

        eccPrimeSquared = (eccSquared) / (1 - eccSquared);

        M = y / k0;
        mu = M / (a * (1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5
                        * eccSquared * eccSquared * eccSquared / 256));

        phi1Rad = mu + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * mu)
                + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32)
                * Math.sin(4 * mu) + (151 * e1 * e1 * e1 / 96)
                * Math.sin(6 * mu);
//        double phi1 = ProjMath.radToDeg(phi1Rad);

        N1 = a / Math.sqrt(1 - eccSquared * Math.sin(phi1Rad)
                        * Math.sin(phi1Rad));
        T1 = Math.tan(phi1Rad) * Math.tan(phi1Rad);
        C1 = eccPrimeSquared * Math.cos(phi1Rad) * Math.cos(phi1Rad);
        R1 = a * (1 - eccSquared)
                / Math.pow(1 - eccSquared * Math.sin(phi1Rad)
                        * Math.sin(phi1Rad), 1.5);
        D = x / (N1 * k0);

        double lat = phi1Rad
                - (N1 * Math.tan(phi1Rad) / R1)
                * (D * D / 2
                        - (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * eccPrimeSquared)
                        * D * D * D * D / 24 + (61 + 90 * T1 + 298 * C1 + 45
                        * T1 * T1 - 252 * eccPrimeSquared - 3 * C1 * C1)
                        * D * D * D * D * D * D / 720);
        latitudeDegrees = Math.toDegrees(lat);

        double lng = (D - (1 + 2 * T1 + C1) * D * D * D / 6 + (5 - 2 * C1 + 28
                * T1 - 3 * C1 * C1 + 8 * eccPrimeSquared + 24 * T1 * T1)
                * D * D * D * D * D / 120)
                / Math.cos(phi1Rad);
        longitudeDegrees = longOrigin + Math.toDegrees(lng);
    }
	
	private void LLtoUTM() {
		double lat = latitudeDegrees;
        double lng = longitudeDegrees;
        
        lng = (lng + 180) - (int) ((lng + 180) / 360) * 360 - 180; // -180.00 .. 179.9;
        
        double a = wgs84Radius;
        double eccSquared = wgs84EccSquared;
        double k0 = 0.9996;

        double longOrigin;
        double eccPrimeSquared;
        double N, T, C, A, M;

        double latRad = Math.toRadians(lat);
        double longRad = Math.toRadians(lng);
        double longOriginRad;
        int zoneNumberTmp = (int) ((lng + 180) / 6) + 1;

        //Make sure the longitude 180.00 is in Zone 60
        if (lng == 180) {
            zoneNumberTmp = 60;
        }

        // Special zone for Norway
        if (lat >= 56.0f && lat < 64.0f && lng >= 3.0f && lng < 12.0f) {
            zoneNumberTmp = 32;
        }

        // Special zones for Svalbard
        if (lat >= 72.0f && lat < 84.0f) {
            if (lng >= 0.0f && lng < 9.0f)
                zoneNumberTmp = 31;
            else if (lng >= 9.0f && lng < 21.0f)
                zoneNumberTmp = 33;
            else if (lng >= 21.0f && lng < 33.0f)
                zoneNumberTmp = 35;
            else if (lng >= 33.0f && lng < 42.0f)
                zoneNumberTmp = 37;
        }
        
        longOrigin = (zoneNumberTmp - 1) * 6 - 180 + 3; //+3 puts origin
                                                     // in middle of
                                                     // zone
        longOriginRad = Math.toRadians(longOrigin);

        eccPrimeSquared = (eccSquared) / (1 - eccSquared);

        N = a / Math.sqrt(1 - eccSquared * Math.sin(latRad) * Math.sin(latRad));
        T = Math.tan(latRad) * Math.tan(latRad);
        C = eccPrimeSquared * Math.cos(latRad) * Math.cos(latRad);
        A = Math.cos(latRad) * (longRad - longOriginRad);

        M = a * ((1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5
                        * eccSquared * eccSquared * eccSquared / 256)
                        * latRad
                        - (3 * eccSquared / 8 + 3 * eccSquared * eccSquared
                                / 32 + 45 * eccSquared * eccSquared
                                * eccSquared / 1024)
                        * Math.sin(2 * latRad)
                        + (15 * eccSquared * eccSquared / 256 + 45 * eccSquared
                                * eccSquared * eccSquared / 1024)
                        * Math.sin(4 * latRad) - (35 * eccSquared * eccSquared
                        * eccSquared / 3072)
                        * Math.sin(6 * latRad));

        double utmEasting = (k0 * N
                * (A + (1 - T + C) * A * A * A / 6.0d + (5 - 18 * T + T * T
                        + 72 * C - 58 * eccPrimeSquared)
                        * A * A * A * A * A / 120.0d) + 500000.0d); // adding UTM standard 500km offset

        double utmNorthing =  (k0 * (M + N
                * Math.tan(latRad)
                * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A
                        / 24.0d + (61 - 58 * T + T * T + 600 * C - 330 * eccPrimeSquared)
                        * A * A * A * A * A * A / 720.0d)));
        if (lat < 0.0f) {
            utmNorthing += 10000000.0f; //10000000 meter offset for
                                        // southern hemisphere
        }

        northing = utmNorthing;
        easting = utmEasting;
        
        //NeptusLog.pub().info("<###>Easting: "+easting+ " Northing: "+northing);
        
        zoneNumber = zoneNumberTmp;
        zoneLetter = 'N';
        if (lat < 0)
        	zoneLetter = 'S';
	}

    public static char utmLetterDesignator(double latitude) {
        // This routine determines the correct UTM letter designator for the given latitude
        // returns 'Z' if latitude is outside the UTM limits of 84N to 80S
        // Written by Chuck Gantz- chuck.gantz@globalstar.com
        char letterDesignator;

        if ((84 >= latitude) && (latitude >= 72))
            letterDesignator = 'X';
        else if ((72 > latitude) && (latitude >= 64))
            letterDesignator = 'W';
        else if ((64 > latitude) && (latitude >= 56))
            letterDesignator = 'V';
        else if ((56 > latitude) && (latitude >= 48))
            letterDesignator = 'U';
        else if ((48 > latitude) && (latitude >= 40))
            letterDesignator = 'T';
        else if ((40 > latitude) && (latitude >= 32))
            letterDesignator = 'S';
        else if ((32 > latitude) && (latitude >= 24))
            letterDesignator = 'R';
        else if ((24 > latitude) && (latitude >= 16))
            letterDesignator = 'Q';
        else if ((16 > latitude) && (latitude >= 8))
            letterDesignator = 'P';
        else if ((8 > latitude) && (latitude >= 0))
            letterDesignator = 'N';
        else if ((0 > latitude) && (latitude >= -8))
            letterDesignator = 'M';
        else if ((-8 > latitude) && (latitude >= -16))
            letterDesignator = 'L';
        else if ((-16 > latitude) && (latitude >= -24))
            letterDesignator = 'K';
        else if ((-24 > latitude) && (latitude >= -32))
            letterDesignator = 'J';
        else if ((-32 > latitude) && (latitude >= -40))
            letterDesignator = 'H';
        else if ((-40 > latitude) && (latitude >= -48))
            letterDesignator = 'G';
        else if ((-48 > latitude) && (latitude >= -56))
            letterDesignator = 'F';
        else if ((-56 > latitude) && (latitude >= -64))
            letterDesignator = 'E';
        else if ((-64 > latitude) && (latitude >= -72))
            letterDesignator = 'D';
        else if ((-72 > latitude) && (latitude >= -80))
            letterDesignator = 'C';
        else
            letterDesignator = 'Z'; // This is here as an error flag to show that the Latitude is outside the UTM limits

        return letterDesignator;
    }

	public double getEasting() {
		return easting;
	}

	public void setEasting(double easting) {
		this.easting = easting;
		UTMtoLL();
	}

	public double getNorthing() {
		return northing;
	}

	public void setNorthing(double northing) {
		this.northing = northing;
		UTMtoLL();
	}

	public char getZoneLetter() {
		return zoneLetter;
	}

    /**
     * @param zoneLetter the zoneLetter to set
     */
    public void setZoneLetter(char zoneLetter) {
        this.zoneLetter = zoneLetter;
        UTMtoLL();
    }
    
	public int getZoneNumber() {
		return zoneNumber;
	}
	
    /**
     * @param zoneNumber the zoneNumber to set
     */
    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
        UTMtoLL();
    }

	public double getLatitudeDegrees() {
		return latitudeDegrees;
	}

	public void setLatitudeDegrees(double latitudeDegrees) {
		this.latitudeDegrees = latitudeDegrees;
		LLtoUTM();
	}

	public double getLongitudeDegrees() {
		return longitudeDegrees;
	}

	public void setLongitudeDegrees(double longitudeDegrees) {
		this.longitudeDegrees = longitudeDegrees;
		LLtoUTM();
	}	 
	
	public static void main(String[] args) {
	    
	    
	    
	    
	    UTMCoordinates utmcadiz = new UTMCoordinates( 745370.0, 4042995.0, 29, 'N');
	    utmcadiz.UTMtoLL();
	    double lat = utmcadiz.getLatitudeDegrees();
	    double lon = utmcadiz.getLongitudeDegrees();
	    
	    NeptusLog.pub().info("Latitude: " + lat + " Longitude: " + lon);
	    
	    UTMCoordinates utm = new UTMCoordinates(41, -8); 
	    NeptusLog.pub().info("<###>\nUTM: northing:4539238.6   easthing:584102.1 zone:29 N");
	    // NATO UTM (Military Grid Reference System (MGRS)): northing:39238.6     easthing:84102.1 long_zone:29 lat_zone:T digraph:NF
	    
	    
	    
	    NeptusLog.pub().info("<###>northing: " + utm.getNorthing() + "   easting: " + utm.getEasting() +
	            "  zone number: " + utm.getZoneNumber() + 
	            "  zone letter: " + utm.getZoneLetter());
	    
        UTMCoordinates utm2 = new UTMCoordinates(584102.1, 4539238.6, 29, 'N'); 
        
        
        
        
        NeptusLog.pub().info("<###>lat: " + utm2.getLatitudeDegrees() + "   lon: " + utm2.getLongitudeDegrees());
        
        
        utm = new UTMCoordinates(40.5, -73.5);
        NeptusLog.pub().info("<###>\nUTM: northing:4484335.4   easthing:627103.1 zone:18 N");
        NeptusLog.pub().info("<###>northing: " + utm.getNorthing() + "   easting: " + utm.getEasting() +
                "  zone number: " + utm.getZoneNumber() + 
                "  zone letter: " + utm.getZoneLetter());

        utm = new UTMCoordinates(-26, 65);
        NeptusLog.pub().info("<###>\nUTM: northing:7122784.2   easthing:700180.5 zone:41 S");
        NeptusLog.pub().info("<###>northing: " + utm.getNorthing() + "   easting: " + utm.getEasting() +
                "  zone number: " + utm.getZoneNumber() + 
                "  zone letter: " + utm.getZoneLetter());

        utm = new UTMCoordinates(38.094097, -118.86528);
        NeptusLog.pub().info("<###>\nUTM: northing:4217898.3   easthing:336435.9 zone:11 N");
        NeptusLog.pub().info("<###>northing: " + utm.getNorthing() + "   easting: " + utm.getEasting() +
                "  zone number: " + utm.getZoneNumber() + 
                "  zone letter: " + utm.getZoneLetter());
	}
	
}
