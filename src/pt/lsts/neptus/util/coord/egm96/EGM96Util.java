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
 * Author: Paulo Dias
 * 2010/05/03
 */
package pt.lsts.neptus.util.coord.egm96;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * This is a Java implementation of C translation by D.Ineiev.
 * 
 * The original files (FORTRAN, original data files) can be found at:
 * http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm96/egm96.html
 *	
 * And the C translation by D.Ineiev can be found at:
 * http://sourceforge.net/projects/egm96-f477-c
 * 
 * --- START LICENCE ---

	(C) 2008 Damien Dusha, with portions from NIMA and D. Ineiev
	
	  This software is provided 'as-is', without any express or implied
	  warranty.  In no event will the authors be held liable for any damages
	  arising from the use of this software.
	
	  Permission is granted to anyone to use this software for any purpose,
	  including commercial applications, and to alter it and redistribute it
	  freely, subject to the following restrictions:
	
	  1. The origin of this software must not be misrepresented; you must not
	     claim that you wrote the original software. If you use this software
	     in a product, an acknowledgment in the product documentation would be
	     appreciated but is not required.
	  2. Altered source versions must be plainly marked as such, and must not be
	     misrepresented as being the original software.
	  3. This notice may not be removed or altered from any source distribution.

 * --- END LICENCE ---
 *
 * @author pdias
 *
 */
public class EGM96Util {

	private static final int L_VALUE = 65341;
	private static final int _361 = 361;
	
	private static InputStream inputStreamCORCOEF, inputStreamEGM96;
	
	private static double[] cc = new double[L_VALUE + 1], cs = new double[L_VALUE + 1],
			hc = new double[L_VALUE + 1], hs = new double[L_VALUE + 1];
			//p = new double[L_VALUE + 1];
			//sinml = new double[_361 + 1], cosml = new double[_361 + 1], rleg = new double[_361 + 1];
	private static final int nmax = 360;
	
	static {
		try {
			inputStreamCORCOEF = new GZIPInputStream(EGM96Util.class
					.getResourceAsStream("CORCOEF.gz"));
			inputStreamEGM96 = new GZIPInputStream(EGM96Util.class
					.getResourceAsStream("EGM96.gz"));
			init_arrays();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
//	private double hundu(int nmax,double p[l_value+1],
//			 double hc[l_value+1],double hs[l_value+1],
//			 double sinml[_361+1],double cosml[_361+1],double gr,double re,
//			 double cc[l_value+1],double cs[l_value+1])
	private static double hundu(int nmax, double[] p, double[] hc, double[] hs,
			double[] sinml, double[] cosml, double gr, double re, double[] cc,
			double[] cs) {
		/* constants for wgs84(g873);gm in units of m**3/s**2 */
		final double gm = 0.3986004418e15, ae = 6378137.0;
		@SuppressWarnings("unused")
		double arn, ar, ac, a, b, sum, sumc, sum2, tempc, temp;
		int k, n, m;
		ar = ae / re;
		arn = ar;
		ac = a = b = 0;
		k = 3;
		for (n = 2; n <= nmax; n++) {
			arn *= ar;
			k++;
			sum = p[k] * hc[k];
			sumc = p[k] * cc[k];
			sum2 = 0;
			for (m = 1; m <= n; m++) {
				k++;
				tempc = cc[k] * cosml[m] + cs[k] * sinml[m];
				temp = hc[k] * cosml[m] + hs[k] * sinml[m];
				sumc += p[k] * tempc;
				sum += p[k] * temp;
			}
			ac += sumc;
			a += sum * arn;
		}
		ac += cc[1] + p[2] * cc[2] + p[3]
				* (cc[3] * cosml[1] + cs[3] * sinml[1]);
		// add haco=ac/100 to convert height anomaly on the ellipsoid to the
		// undulation
		// add -0.53m to make undulation refer to the wgs84 ellipsoid.
		return a * gm / (gr * re) + ac / 100 - .53;
	}

	
	//void dscml(double rlon,unsigned nmax,double sinml[_361+1],double cosml[_361+1])
	private static double[][] dscml(double rlon, int nmax/*, double[] sinml, double[] cosml*/) {
		double[] sinml = new double[_361 + 1], cosml = new double[_361 + 1];
		double a, b;
		int m;
		a = Math.sin(rlon);
		b = Math.cos(rlon);
		sinml[1] = a;
		cosml[1] = b;
		sinml[2] = 2 * b * a;
		cosml[2] = 2 * b * b - 1;
		for (m = 3; m <= nmax; m++) {
			sinml[m] = 2 * b * sinml[m - 1] - sinml[m - 2];
			cosml[m] = 2 * b * cosml[m - 1] - cosml[m - 2];
		}
		return new double[][] {sinml, cosml};
	}

	
	//void dhcsin(unsigned nmax,double hc[l_value+1],double hs[l_value+1])
	private static void dhcsin(int nmax, double[] hc, double[] hs) {
		int n, m;
		@SuppressWarnings("unused")
		double j2, j4, j6, j8, j10, c, s, ec, es;
		/*
		 * the even degree zonal coefficients given below were computed for the
		 * wgs84(g873) system of constants and are identical to those values
		 * used in the NIMA gridding procedure. computed using subroutine grs
		 * written by N.K. PAVLIS
		 */
		j2 = 0.108262982131e-2;
		j4 = -.237091120053e-05;
		j6 = 0.608346498882e-8;
		j8 = -0.142681087920e-10;
		j10 = 0.121439275882e-13;
		m = ((nmax + 1) * (nmax + 2)) / 2;
		for (n = 1; n <= m; n++)
			hc[n] = hs[n] = 0;
//		while (6 == fscanf(inputStreamEGM96, "%i %i %lf %lf %lf %lf", n, m, c, s, ec, es)) {
//			if (n > nmax)
//				continue;
//			n = (n * (n + 1)) / 2 + m + 1;
//			hc[n] = c;
//			hs[n] = s;
//		}
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStreamEGM96));
		try {
			String line = br.readLine();
			while(line != null) {
				if (line.startsWith("#") || "".equalsIgnoreCase(line)
						|| line.startsWith(";")) {
					line = br.readLine();
					continue;
				}
				String[] tokens = line.trim().split("[ \\t;]+");
				n = Integer.parseInt(tokens[0]);
				if (n > nmax) {
					line = br.readLine();
					continue;
				}
				m = Integer.parseInt(tokens[1]);
				c = Double.parseDouble(tokens[2]);
				s = Double.parseDouble(tokens[3]);
				ec = Double.parseDouble(tokens[4]);
				es = Double.parseDouble(tokens[5]);
				n = (n * (n + 1)) / 2 + m + 1;
				hc[n] = c;
				hs[n] = s;
				line = br.readLine();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		hc[4] += j2 / Math.sqrt(5);
		hc[11] += j4 / 3;
		hc[22] += j6 / Math.sqrt(13);
		hc[37] += j8 / Math.sqrt(17);
		hc[56] += j10 / Math.sqrt(21);
	}


	static double[] drts = new double[1301], dirt = new double[1301], rlnn = new double[_361 + 1];// static
	static double cothet, sithet;// static
	static int ir = 0; // static

	/**
	 * 	This subroutine computes  all normalized legendre function
	 *  in "rleg". order is always
	 *  m, and colatitude is always theta  (radians). maximum deg
	 *  is  nmx. all calculations in double precision.
	 *  ir  must be set to zero before the first call to this sub.
	 *  the dimensions of arrays  rleg must be at least equal to  nmx+1.
	 *  Original programmer :Oscar L. Colombo, Dept. of Geodetic Science
	 *  the Ohio State University, August 1980
	 *  ineiev: I removed the derivatives, for they are never computed here
	 *  
	 * @param m
	 * @param theta
	 * @param rleg
	 * @param nmx
	 */
	// void legfdn(unsigned m,double theta,double rleg[_361+1],unsigned nmx)
	private static double[] legfdn(int m, double theta, int nmx) {
//		double[] drts = new double[1301], dirt = new double[1301], rlnn = new double[_361 + 1];// static
//		double cothet, sithet;// static
//		int ir = 0; // static
		int nmx1 = nmx + 1, nmx2p = 2 * nmx + 1, m1 = m + 1, m2 = m + 2, m3 = m + 3, n, n1, n2;
		double[] rleg  = new double[_361 + 1];
		if (ir == 0) {
			ir = 1;
			for (n = 1; n <= nmx2p; n++) {
				drts[n] = Math.sqrt(n);
				dirt[n] = 1 / drts[n];
			}
		}
		cothet = Math.cos(theta);
		sithet = Math.sin(theta);
		/* compute the legendre functions */
		rlnn[1] = 1;
		rlnn[2] = sithet * drts[3];
		for (n1 = 3; n1 <= m1; n1++) {
			n = n1 - 1;
			n2 = 2 * n;
			rlnn[n1] = drts[n2 + 1] * dirt[n2] * sithet * rlnn[n];
		}
		switch (m) {
		case 1:
			rleg[2] = rlnn[2];
			rleg[3] = drts[5] * cothet * rleg[2];
			break;
		case 0:
			rleg[1] = 1;
			rleg[2] = cothet * drts[3];
			break;
		}
		rleg[m1] = rlnn[m1];
		if (m2 <= nmx1) {
			rleg[m2] = drts[m1 * 2 + 1] * cothet * rleg[m1];
			if (m3 <= nmx1)
				for (n1 = m3; n1 <= nmx1; n1++) {
					n = n1 - 1;
					if ((m == 0 && n < 2) || (m == 1 && n < 3))
						continue;
					n2 = 2 * n;
					rleg[n1] = drts[n2 + 1]
							* dirt[n + m]
							* dirt[n - m]
							* (drts[n2 - 1] * cothet * rleg[n1 - 1] - drts[n
									+ m - 1]
									* drts[n - m - 1]
									* dirt[n2 - 3]
									* rleg[n1 - 2]);
				}
		}
		//NeptusLog.pub().info("<###> "+Arrays.toString(rleg));
		return rleg;
	}

	/**
	 * this subroutine computes geocentric distance to the point, the geocentric
	 * latitude,and an approximate value of normal gravity at the point based
	 * the constants of the wgs84(g873) system are used
	 */	
	//void radgra(double lat,double lon,double*rlat,double*gr,double*re)
	private static double[] radgra(double lat, double lon)
	{
		final double a = 6378137., e2 = .00669437999013, geqt = 9.7803253359, k = .00193185265246;
		double n, t1 = Math.sin(lat) * Math.sin(lat), t2, x, y, z;
		n = a / Math.sqrt(1 - e2 * t1);
		t2 = n * Math.cos(lat);
		x = t2 * Math.cos(lon);
		y = t2 * Math.sin(lon);
		z = (n * (1 - e2)) * Math.sin(lat);
		double rlat, gr, re;
		re = Math.sqrt(x * x + y * y + z * z);/* compute the geocentric radius */
		rlat = Math.atan(z / Math.sqrt(x * x + y * y));/*
														 * compute the
														 * geocentric latitude
														 */
		gr = geqt * (1 + k * t1) / Math.sqrt(1 - e2 * t1);/*
														 * compute normal
														 * gravity:units are
														 * m/sec**2
														 */
		double[] ret = { rlat, gr, re };
		return ret;
	}

	
	private static double undulation(double lat, double lon, int nmax, int k) {
		double rlat, gr, re;
		int i, j, m;
		double[] ret = radgra(lat, lon);
		rlat = ret[0];
		gr = ret[1];
		re = ret[2];
		rlat = Math.PI / 2 - rlat;
		//System.out.printf("_________ %f %f %f\n",rlat,gr,re); 

		double[] p = new double[L_VALUE + 1];
		for (j = 1; j <= k; j++) {
			m = j - 1;
			double[] rleg = legfdn(m, rlat, nmax);
			for (i = j; i <= k; i++) {
				p[(i - 1) * i / 2 + m + 1] = rleg[i];
			}
		}
		double[][] retll = dscml(lon, nmax);
		double[] sinml = retll[0], cosml = retll[1];
		return hundu(nmax, p, hc, hs, sinml, cosml, gr, re, cc, cs);
	}

	private static void init_arrays() {
		int ig, i, n, m;
		double t1, t2;
		/*correction coefficient file: 
		 modified with 'sed -e"s/D/e/g"' to be read with fscanf*/
		//f_1=fopen("CORCOEF","rb");
		/*potential coefficient file*/
		//f_12=fopen("EGM96","rb");
		for (i = 1; i <= L_VALUE; i++)
			cc[i] = cs[i] = 0;
		//while(4==fscanf(inputStreamCORCOEF,"%i %i %lg %lg",&n,&m,&t1,&t2)) {
		//	ig=(n*(n+1))/2+m+1;
		//	cc[ig]=t1;
		//	cs[ig]=t2;
		//}
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStreamCORCOEF));
		try {
			String line = br.readLine();
			while(line != null) {
				if (line.startsWith("#") || "".equalsIgnoreCase(line)
						|| line.startsWith(";")) {
					line = br.readLine();
					continue;
				}
				String[] tokens = line.trim().split("[ \\t;]+");
				n = Integer.parseInt(tokens[0]);
				m = Integer.parseInt(tokens[1]);
				t1 = Double.parseDouble(tokens[2]);
				t2 = Double.parseDouble(tokens[3]);
				ig=(n*(n+1))/2+m+1;
				cc[ig]=t1;
				cs[ig]=t2;
				line = br.readLine();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		/*the correction coefficients are now read in*/
		/*the potential coefficients are now read in and the reference
		 even degree zonal harmonic coefficients removed to degree 6*/
		dhcsin(nmax,hc,hs);
		//fclose(f_1);
		//fclose(f_12);
	}

	/**
	 * This calculates the MSL in meters in WGS84 in a given latitude/longitude
	 * using EGM96 geoid.
	 * @param latDegrees
	 * @param lonDegrees
	 * @return
	 */
	public static double calcHeight(double latDegrees, double lonDegrees) {
		double u;
		//init_arrays();
		/* compute the geocentric latitude,geocentric radius,normal gravity */
		u = undulation(Math.toRadians(latDegrees), Math.toRadians(lonDegrees),
				nmax, nmax + 1);
		/*
		 * u is the geoid undulation from the egm96 potential coefficient model
		 * including the height anomaly to geoid undulation correction term and
		 * a correction term to have the undulations refer to the wgs84
		 * ellipsoid. the geoid undulation unit is meters.
		 */
		return u;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// 38.8259300   -9.3316070    53.9372077
		System.out.printf("Lat: %f | Lon: %f | MSL(Height to WGS84): %f = %f\n", 
				39.1070, -8.9820, 53.9372077, calcHeight(39.1070, -8.9820));
		System.out.printf("Lat: %f | Lon: %f | MSL(Height to WGS84): %f = %f\n", 
				38.8259300, -9.3316070, 53.9372077, calcHeight(38.8259300, -9.3316070));
		System.out.printf("Lat: %f | Lon: %f | MSL(Height to WGS84): %f = %f\n", 
				38.8259300, -9.3316070, 53.9372077, calcHeight(38.8259300, -9.3316070));
		System.out.printf("Lat: %f | Lon: %f | MSL(Height to WGS84): %f = %f\n", 
				38.8259300, -9.3316070, 53.9372077, calcHeight(38.8259300, -9.3316070));
		LocationType locFEUP = new LocationType();
		locFEUP.setLatitudeStr("41N10'42.4368''");
		locFEUP.setLongitudeStr("8W35'45.4572''");
		System.out.printf("Lat: %f | Lon: %f | MSL(Height to WGS84): %f = %f\n", 
				locFEUP.getLatitudeDegs(), locFEUP.getLongitudeDegs(), 124.0, calcHeight(locFEUP.getLatitudeDegs(), locFEUP.getLongitudeDegs()));
		
		//http://maps.google.com/maps/api/elevation/xml?locations=38.8259300,-9.3316070&sensor=false
		//<?xml version="1.0" encoding="UTF-8"?>
		//<ElevationResponse>
		// <status>OK</status>
		// <result>
		//  <location>
		//   <lat>38.8259300</lat>
		//   <lng>-9.3316070</lng>
		//  </location>
		//  <elevation>138.4690399</elevation>
		// </result>
		//</ElevationResponse>
		
		//JSON
		//{
		//	  "status": "OK",
		//	  "results": [ {
		//	    "location": {
		//	      "lat": 38.8259300,
		//	      "lng": -9.3316070
		//	    },
		//	    "elevation": 138.4690399
		//	  } ]
		//	}

		//192,4062476

	}

}
