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
 * 13/Jun/2005
 */
package pt.lsts.neptus.util;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * @author Paulo Dias
 * 
 */
public class MathMiscUtils {
	/**
	 * @param val
	 * @param decimalHouses
	 * @return
	 */
	public static double round(double val, int decimalHouses) {
		double base = Math.pow(10d, decimalHouses);
		double result = Math.round(val * base) / base;
		return result;
	}

	/**
	 * @param val
	 * @param decimalHouses
	 * @return
	 */
	public static float round(float val, int decimalHouses) {
		float base = (float) Math.pow(10f, decimalHouses);
		float result = Math.round(val * base) / base;
		return result;
	}

	public static double pointLineDistance(double px, double py, double lx1, double ly1, double lx2, double ly2) {
	    double up = Math.abs(
	            (lx2-lx1)*(ly1-py)-(lx1-px)*(ly2-ly1)
	    );
	    double below = Math.sqrt(
	            (lx2-lx1)*(lx2-lx1) + (ly2-ly1)*(ly2-ly1)
	    );
	    return up/below;
	}

	public static double pointLineDistance(Point2D point, Line2D line) {
	    return pointLineDistance(point.getX(), point.getY(), line.getX1(), line.getY1(), line.getX2(), line.getY2());
	}

    public static double[] calcCentroidPolygonPoint(ArrayList<Point> polyPoints) {
        ArrayList<Point2D> points = new ArrayList<Point2D>();
        for (Point point : polyPoints) {
            Point2D point2d = new Point2D.Double(point.getX(), point.getY());
            points.add(point2d);
        }
        return calcCentroidPolygon(points);
    }
    
	public static double[] calcCentroidPolygon(ArrayList<Point2D> polyPoints) {
        double sumX = 0, sumY = 0;
        for (Point2D dp : polyPoints) {
            sumX += dp.getX();
            sumY += dp.getY();
        }
        double cX = sumX / polyPoints.size(), cY = sumY / polyPoints.size();
        return new double[] {cX, cY};
	}
	
	public static ArrayList<Point> dilatePolygonPoint(ArrayList<Point> polyPoints, double growByValue) {
        ArrayList<Point2D> points = new ArrayList<Point2D>();
        for (Point point : polyPoints) {
            Point2D point2d = new Point2D.Double(point.getX(), point.getY());
            points.add(point2d);
        }

        ArrayList<Point2D> ret = dilatePolygon(points, growByValue);
        ArrayList<Point> pointsRet = new ArrayList<Point>();
        for (Point2D point2d : ret) {
            Point point = new Point(Double.valueOf(point2d.getX()).intValue(), Double.valueOf(point2d.getY()).intValue());
            pointsRet.add(point);
        }

        return pointsRet;
	}
	
	/**
	 * Dilation of a polygon by 'x'. A transformation in which a polygon is enlarged or 
	 * reduced by a given factor around a the center point.
	 * @param polyPoints
	 * @param growByValue Value to grow (+) or shrink (-).
	 * @return
	 */
	public static ArrayList<Point2D> dilatePolygon(ArrayList<Point2D> polyPoints, double growByValue) {
        ArrayList<Point2D> pointsGrow = new ArrayList<Point2D>();
        
        // Centroid calc
        double[] centroid = calcCentroidPolygon(polyPoints);
        double cX = centroid[0], cY = centroid[1];
        
        // Dilation
        for (Point2D dp : polyPoints) {
            Point2D np = new Point2D.Double();
            double ang = AngleUtils.calcAngle(dp.getX(), dp.getY(), cX, cY);
            double dist = Math.sqrt((dp.getX() - cX) * (dp.getX() - cX) + (dp.getY() - cY) * (dp.getY() - cY));
            double[] rotD = AngleUtils.rotate(ang + Math.PI / 2, dist + growByValue, 0, true);
            np.setLocation(cX + rotD[0], cY + rotD[1]);
            pointsGrow.add(np);
        }
        
        return pointsGrow;
	}

	/*-------------------------------------------------------------*/

	/**
	 * @param val
	 * @param decimalHouses
	 * @return
	 */
	public static String parseToEngineeringNotation(double val,
			int decimalHouses) {
	    if (Double.isInfinite(val) || Double.isNaN(val))
	        return "" + val;
	    
	    Locale locale  = new Locale("en", "US");
	    DecimalFormat engNot = (DecimalFormat) NumberFormat.getNumberInstance(locale);
		engNot.applyPattern("##0.###E0");
		String pl = engNot.format(val);
		String[] pl2 = pl.split("E");
		double vl = Double.parseDouble(pl2[0].replace(',', '.'));
		vl = round(vl, decimalHouses);
		int mul = Integer.parseInt(pl2[1]);
		String mulStr = "";
		switch (mul) {
    		case 24:
    			mulStr = "Y";
    			break;
    		case 21:
    			mulStr = "Z";
    			break;
    		case 18:
    			mulStr = "E";
    			break;
    		case 15:
    			mulStr = "P";
    			break;
    		case 12:
    			mulStr = "T";
    			break;
    		case 9:
    			mulStr = "G";
    			break;
    		case 6:
    			mulStr = "M";
    			break;
    		case 3:
    			mulStr = "k";
    			break;
    		case -3:
    			mulStr = "m";
    			break;
    		case -6:
    			mulStr = "u";
    			mulStr = "\u00B5";
    			break;
    		case -9:
    			mulStr = "n";
    			break;
    		case -12:
    			mulStr = "p";
    			break;
    		case -15:
    			mulStr = "f";
    			break;
    		case -18:
    			mulStr = "a";
    			break;
    		case -21:
    			mulStr = "z";
    			break;
    		case -24:
    			mulStr = "y";
    			break;
    		default:
    			mulStr = mul == 0 ? "" : "E" + mul;
    			break;
		}

		return (decimalHouses == 0 ? (long) vl + "" : vl) + mulStr;
	}

	/**
	 * @param engValue
	 * @return
	 */
	public static double parseEngineeringModeToDouble(String engValue) {
		double doubleValue = Double.NaN;
		String delim = "YZEPTGMkmu\u00B5npfazy";
		StringTokenizer strt, strtTkn;

		if (engValue == null)
			return Double.NaN;
		if (engValue.equalsIgnoreCase(""))
			return Double.NaN;

		strt = new StringTokenizer(engValue, delim);
		strtTkn = new StringTokenizer(engValue, delim, true);
		// NeptusLog.pub().info("<###> "+strt.countTokens());
		// NeptusLog.pub().info("<###> "+strtTkn.countTokens());
		if ((strt.countTokens() == 1)
				&& (strtTkn.countTokens() <= (strt.countTokens() + 1))) {
			// NOP
		} 
		else
			return Double.NaN;
		for (int i = 1; strtTkn.hasMoreTokens(); i++) {
			if (i == 1) {
				try {
					doubleValue = Double.parseDouble(strtTkn.nextToken());
				} 
				catch (NumberFormatException e) {
					return Double.NaN;
				}
			} 
			else {
				String multiplierStr = strtTkn.nextToken();
				double multiplier = getEngMultiplier(multiplierStr);
				doubleValue *= multiplier;
			}
		}

		return doubleValue;
	}

	/**
	 * @param multiplierStr
	 *            One of {Y, Z, E, P, T, G, M, k, m, u or \u00B5, n, p, f, a, z,
	 *            y}
	 * @return
	 */
	private static double getEngMultiplier(String multiplierStr) {
		double multiplier = 1;
		if (multiplierStr.length() != 1)
			return 1;
		char key = multiplierStr.charAt(0);
		switch (key) {
    		case 'Y':
    			multiplier = 1E24;
    			break;
    		case 'Z':
    			multiplier = 1E21;
    			break;
    		case 'E':
    			multiplier = 1E18;
    			break;
    		case 'P':
    			multiplier = 1E15;
    			break;
    		case 'T':
    			multiplier = 1E12;
    			break;
    		case 'G':
    			multiplier = 1E9;
    			break;
    		case 'M':
    			multiplier = 1E6;
    			break;
    		case 'k':
    			multiplier = 1E3;
    			break;
    		case 'm':
    			multiplier = 1E-3;
    			break;
    		case 'u':
    		case '\u00B5':
    			multiplier = 1E-6;
    			break;
    		case 'n':
    			multiplier = 1E-9;
    			break;
    		case 'p':
    			multiplier = 1E-12;
    			break;
    		case 'f':
    			multiplier = 1E-15;
    			break;
    		case 'a':
    			multiplier = 1E-18;
    			break;
    		case 'z':
    			multiplier = 1E-21;
    			break;
    		case 'y':
    			multiplier = 1E-24;
    			break;
    		default:
    			multiplier = 1;
    			break;
		}
		return multiplier;
	}

	/*-------------------------------------------------------------*/
	/**
	 * <p>
	 * IEEE 1541 recommends:
	 * </p>
	 * <ul>
	 * <li>a set of units to refer to quantities used in digital electronics and
	 * computing:
	 * <ul>
	 * <li><i>bit</i> (symbol 'b'), a binary digit;</li>
	 * <li><i>byte</i> (symbol 'B'), a set of adjacent bits (usually, but not
	 * necessarily, eight) operated on as a group;</li>
	 * <li><i>octet</i> (symbol 'o'), a group of eight bits;</li>
	 * 
	 * </ul>
	 * </li>
	 * <li>a set of prefixes to indicate binary multiples of the aforesaid
	 * units:
	 * <ul>
	 * <li><i>kibi</i> (symbol 'Ki'), 2<sup>10</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">024</span></span>;</li>
	 * <li><i>mebi</i> (symbol 'Mi'), 2<sup>20</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">048</span><span
	 * style="margin-left: 0.25em;">576</span></span>;</li>
	 * 
	 * <li><i>gibi</i> (symbol 'Gi'), 2<sup>30</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">073</span><span
	 * style="margin-left: 0.25em;">741</span><span
	 * style="margin-left: 0.25em;">824</span></span>;</li>
	 * <li><i>tebi</i> (symbol 'Ti'), 2<sup>40</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">099</span><span
	 * style="margin-left: 0.25em;">511</span><span
	 * style="margin-left: 0.25em;">627</span><span
	 * style="margin-left: 0.25em;">776</span></span>;</li>
	 * 
	 * <li><i>pebi</i> (symbol 'Pi'), 2<sup>50</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">125</span><span
	 * style="margin-left: 0.25em;">899</span><span
	 * style="margin-left: 0.25em;">906</span><span
	 * style="margin-left: 0.25em;">842</span><span
	 * style="margin-left: 0.25em;">624</span></span>;</li>
	 * <li><i>exbi</i> (symbol 'Ei'), 2<sup>60</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">152</span><span
	 * style="margin-left: 0.25em;">921</span><span
	 * style="margin-left: 0.25em;">504</span><span
	 * style="margin-left: 0.25em;">606</span><span
	 * style="margin-left: 0.25em;">846</span><span
	 * style="margin-left: 0.25em;">976</span></span>;</li>
	 * <li><i>zebi</i> (symbol 'Zi'), 2<sup>70</sup> = <span
	 * style="white-space: nowrap;">11<span
	 * style="margin-left: 0.25em;">805</span><span
	 * style="margin-left: 0.25em;">916</span><span
	 * style="margin-left: 0.25em;">207</span><span
	 * style="margin-left: 0.25em;">174</span><span
	 * style="margin-left: 0.25em;">113</span><span
	 * style="margin-left: 0.25em;">034</span><span
	 * style="margin-left: 0.25em;">241</span></span>;</li>
	 * <li><i>yobi</i> (symbol 'Yi'), 2<sup>80</sup> = <span
	 * style="white-space: nowrap;">1<span
	 * style="margin-left: 0.25em;">208</span><span
	 * style="margin-left: 0.25em;">925</span><span
	 * style="margin-left: 0.25em;">819</span><span
	 * style="margin-left: 0.25em;">614</span><span
	 * style="margin-left: 0.25em;">629</span><span
	 * style="margin-left: 0.25em;">174</span><span
	 * style="margin-left: 0.25em;">706</span><span
	 * style="margin-left: 0.25em;">176</span></span>;</li>
	 * 
	 * </ul>
	 * </li>
	 * <li>that the first part of the binary prefix is pronounced as the
	 * analogous SI prefix, and the second part is pronounced as <i>bee</i>;</li>
	 * <li>that SI prefixes are not used to indicate binary multiples.</li>
	 * </ul>
	 * <p>
	 * The <i>bi</i> part of the prefix comes from the word binary, so for
	 * example, kibibyte means a kilobinary byte, that is 1024 bytes.
	 * </p>
	 * <p>
	 * Note the capital 'K' for the <i>kibi-</i> symbol: while the symbol for
	 * the analogous SI prefix <i>kilo-</i> is a small 'k', a capital 'K' has
	 * been selected for consistency with the other prefixes and with the
	 * widespread use of the misspelled SI prefix (as in 'KB').
	 * </p>
	 * 
	 * <p>
	 * IEEE 1541 is closely related to Amendment 2 to IEC International Standard
	 * <a href="/wiki/IEC_60027" title="IEC 60027">IEC 60027</a>-2, except the
	 * latter uses 'bit' as the symbol for bit, as opposed to 'b'.
	 * </p>
	 * <p>
	 * Today the harmonized <a href="/wiki/ISO" title="ISO"
	 * class="mw-redirect">ISO</a>/<a
	 * href="/wiki/International_Electrotechnical_Commission"
	 * title="International Electrotechnical Commission">IEC</a> <a
	 * href="/wiki/ISO/IEC_80000" title="ISO/IEC 80000">IEC 80000-13:2008 -
	 * Quantities and units -- Part 13: Information science and technology</a>
	 * standard cancels and replaces subclauses 3.8 and 3.9 of IEC 60027-2:2005
	 * (those related to Information theory and Prefixes for binary multiples).
	 * </p>
	 * 
	 * @param val
	 * @param decimalHouses
	 * @return
	 */
	public static String parseToEngineeringRadix2Notation(double val,
			int decimalHouses) {
		int mulTmp = 0;
		int signal = 1;
		if (val < 0)
			signal = -1;
		else
			signal = 1;
		double valTmp = val < 0 ? val * -1 : val;
		if (val >= 1024) {
			do {
				mulTmp++;
				valTmp = valTmp / 1024.0;
			} while (valTmp >= 1024 || mulTmp == 8);
		}
		else if (val == 0) {
		    // Nothing to do
		}
		else if (val < 1.0) {
			do {
				mulTmp--;
				valTmp = valTmp * 1024.0;
			} while (valTmp < 1.0 || mulTmp == -8);
		}

		double vl = valTmp;
		vl = round(vl, decimalHouses);
		int mul = mulTmp * 10;
		String mulStr = "";
		switch (mul) {
    		case 80:
    			mulStr = "Yi";
    			break;
    		case 70:
    			mulStr = "Zi";
    			break;
    		case 60:
    			mulStr = "Ei";
    			break;
    		case 50:
    			mulStr = "Pi";
    			break;
    		case 40:
    			mulStr = "Ti";
    			break;
    		case 30:
    			mulStr = "Gi";
    			break;
    		case 20:
    			mulStr = "Mi";
    			break;
    		case 10:
    			mulStr = "Ki";
    			break;
    		case -10:
    			mulStr = "mi";
    			break;
    		case -20:
    			mulStr = "ui";
    			mulStr = "\u00B5i";
    			break;
    		case -30:
    			mulStr = "ni";
    			break;
    		case -40:
    			mulStr = "pi";
    			break;
    		case -50:
    			mulStr = "fi";
    			break;
    		case -60:
    			mulStr = "ai";
    			break;
    		case -70:
    			mulStr = "zi";
    			break;
    		case -80:
    			mulStr = "yi";
    			break;
    		default:
    			mulStr =  mul == 0 ? "" : "E" + mul;
    			break;
		}
		if ("".equalsIgnoreCase(mulStr) && vl < 1024) {
			if (vl == (long) vl)
				return (signal > 0 ? "" : "-") + ((long) vl) + mulStr;
		}
		return (signal > 0 ? "" : "-") + vl + mulStr;
	}

	/**
	 * @param engValue
	 * @return
	 */
	public static double parseEngineeringRadix2ModeToDouble(String engValue) {
		double doubleValue = Double.NaN;
		String delim = "YZEPTGMKkmu\u00B5npfazyi";
		StringTokenizer strt, strtTkn;

		if (engValue == null)
			return Double.NaN;
		if (engValue.equalsIgnoreCase(""))
			return Double.NaN;

		strt = new StringTokenizer(engValue, delim);
		strtTkn = new StringTokenizer(engValue, delim, true);
		if ((strt.countTokens() == 1)
				&& (strtTkn.countTokens() <= (strt.countTokens() + 2))) {
			// NOP
		} 
		else
			return Double.NaN;
		for (int i = 1; strtTkn.hasMoreTokens(); i++) {
			if (i == 1) {
				try {
					doubleValue = Double.parseDouble(strtTkn.nextToken());
				} 
				catch (NumberFormatException e) {
					return Double.NaN;
				}
			} 
			else {
				String multiplierStr = strtTkn.nextToken();
				if (strtTkn.hasMoreTokens())
					multiplierStr += strtTkn.nextToken();
				double multiplier = getEngRadix2Multiplier(multiplierStr);
				doubleValue *= multiplier;
			}
		}

		return doubleValue;
	}

	/**
	 * @param multiplierStr
	 *            One of {Yi, Zi, Ei, Pi, Ti, Gi, Mi, Ki} These doesn't name
	 *            much sense here but... {mi, ui or \u00B5i, ni, pi, fi, ai, zi,
	 *            yi}
	 * @return
	 */
	private static double getEngRadix2Multiplier(String multiplierStr) {
		double multiplier = 1;
		if (multiplierStr.length() != 2)
			return 1;
		char key = multiplierStr.charAt(0);
		char keyI = multiplierStr.charAt(1);
		if (keyI != 'i')
			return 1;
		switch (key) {
    		case 'Y':
    			multiplier = Math.pow(1024, 8);
    			break;
    		case 'Z':
    			multiplier = Math.pow(1024, 7);
    			break;
    		case 'E':
    			multiplier = Math.pow(1024, 6);
    			break;
    		case 'P':
    			multiplier = Math.pow(1024, 5);
    			break;
    		case 'T':
    			multiplier = Math.pow(1024, 4);
    			break;
    		case 'G':
    			multiplier = Math.pow(1024, 3);
    			break;
    		case 'M':
    			multiplier = Math.pow(1024, 2);
    			break;
    		case 'K':
    		case 'k':
    			multiplier = 1024;
    			break;
    		case 'm':
    			multiplier = Math.pow(1024, -1);
    			break;
    		case 'u':
    		case '\u00B5':
    			multiplier = Math.pow(1024, -2);
    			break;
    		case 'n':
    			multiplier = Math.pow(1024, -3);
    			break;
    		case 'p':
    			multiplier = Math.pow(1024, -4);
    			break;
    		case 'f':
    			multiplier = Math.pow(1024, -5);
    			break;
    		case 'a':
    			multiplier = Math.pow(1024, -6);
    			break;
    		case 'z':
    			multiplier = Math.pow(1024, -7);
    			break;
    		case 'y':
    			multiplier = Math.pow(1024, -8);
    			break;
    		default:
    			multiplier = 1;
    			break;
		}
		return multiplier;
	}

	/*-------------------------------------------------------------*/
	/**
	 * Implements a deadZone filter. <code>
	 * <br>
	 * ...................<br>
	 * ........^..........<br>
	 * ........|..........<br>
	 * ....max +----/.....<br>
	 * ........|.../|.....<br>
	 * .----/dz|--/-+--->.<br>
	 * ..../...|....max...<br>
	 * .../....|..........<br>
	 * ........|..........<br>
	 * ...................<br>
	 * </code>
	 * 
	 * @param xx
	 * @param deadZone
	 *            In teh picture 'dz'
	 * @param maxValue
	 * @return
	 */
	public static double filterDeadZone(double xx, double deadZone,
			double maxValue) {
		if (deadZone >= maxValue)
			return Double.NaN;

		double slope = maxValue / (maxValue - deadZone);
		double bb = -(slope * maxValue - maxValue);
		double yy = 0;
		if ((xx < deadZone) && (xx > -deadZone))
			yy = 0;
		else if (xx > 0)
			yy = slope * xx + bb;
		else
			yy = slope * xx - bb;

		return yy;
	}

	/**
	 * @see {@link #filterDeadZone(double, double, double)}.
	 * @param xx
	 * @param deadZone
	 * @return
	 */
	public static double filterDeadZone(double xx, double deadZone) {
		return filterDeadZone(xx, deadZone, 1);
	}

	/**
	 * @see {@link #filterDeadZone(double, double, double)}.
	 * @param xx
	 * @param deadZone
	 * @param maxValue
	 * @return
	 */
	public static float filterDeadZone(float xx, float deadZone, float maxValue) {
		double xxD = Double.parseDouble("" + xx);
		double deadZoneD = Double.parseDouble("" + deadZone);
		double maxValueD = Double.parseDouble("" + maxValue);
		double yyD = filterDeadZone(xxD, deadZoneD, maxValueD);
		float yyF = Double.valueOf(yyD).floatValue();
		return yyF;
	}

	/**
	 * @see {@link #filterDeadZone(double, double, double)}.
	 * @param xx
	 * @param deadZone
	 * @return
	 */
	public static float filterDeadZone(float xx, float deadZone) {
		return filterDeadZone(xx, deadZone, 1);
	}

	public static double clamp(double val, double min, double max) {
	    return Math.max(min, Math.min(max, val));
	}

	/**
	 * Returns a number limited by [minFractionDigits, maxFractionDigits]
	 * @param maxFractionDigits
	 * @param minFractionDigits
	 * @return NumberFormat
	 * */
	public static NumberFormat getNumberFormat(int maxFractionDigits, int minFractionDigits) {
		NumberFormat df = DecimalFormat.getInstance(Locale.US);
		df.setGroupingUsed(false);

		df.setMaximumFractionDigits(maxFractionDigits);
		df.setMinimumFractionDigits(minFractionDigits);
		return df;
	}
	/*-------------------------------------------------------------*/

	public static void main(String[] args) {
		System.out.println(parseEngineeringModeToDouble("10M"));
		System.out.println(parseToEngineeringNotation(0.000003, 2));

        System.out.println(MathMiscUtils.parseToEngineeringNotation(3.56, 0) + "=4");
        System.out.println(MathMiscUtils.parseToEngineeringNotation(3.56, 1) + "=3.6");

		
		if (MathMiscUtils.parseEngineeringModeToDouble("100M") > MathMiscUtils
				.parseEngineeringModeToDouble("10M")) {
		    System.out.println("Hello");
		}
		else {
		    System.out.println("Nops");
		}

		System.out.println(MathMiscUtils.parseToEngineeringRadix2Notation(
				1887115, 1) + "=1.8Mi");
		System.out.println(MathMiscUtils
				.parseEngineeringRadix2ModeToDouble("1.79969310760498046875Mi")
				+ "=1887115.0");

		System.out.println(MathMiscUtils.parseToEngineeringRadix2Notation(876,
				1) + "=876");

		System.out.println(MathMiscUtils.parseToEngineeringRadix2Notation(0,
		        1) + "=0");

		System.out.println(parseEngineeringRadix2ModeToDouble("1ki"));
		System.out.println(parseEngineeringRadix2ModeToDouble("1Ki"));
		System.out.println(parseEngineeringRadix2ModeToDouble("1Mi"));
		System.out.println(parseEngineeringRadix2ModeToDouble("1\u00B5i"));
		System.out.println(parseEngineeringRadix2ModeToDouble("1ui"));

        System.out.println(round(Math.PI, 2));
        System.out.println(round(Math.PI, 1));
        System.out.println(round(Math.PI, 0));
        System.out.println(clamp(41, 50, 100));
        
        System.out.println(parseToEngineeringNotation(0.0000484746447, 1));
	}
}
