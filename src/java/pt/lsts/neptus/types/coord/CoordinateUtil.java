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
 * Author: Zé Pinto, Paulo Dias
 * 2005/01/15
 */
package pt.lsts.neptus.types.coord;

import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.vecmath.Matrix3d;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author Zé Pinto
 * @author Paulo Dias
 * @author Sergio Fraga
 * @author Rui Gomes
 */
public class CoordinateUtil {

    public static final char CHAR_DEGREE = '\u00B0'; // º Unicode

    /**
     * Used for indicating Lat/Lon decimal degrees decimal places.
     * Don't change this
     */
    public static int LAT_LON_DDEGREES_DECIMAL_PLACES = 8;
    /**
     * Used for indicating Lat/Lon decimal minites decimal places.
     * Don't change this
     */
    public static int LAT_LON_DM_DECIMAL_PLACES = 6;
    /**
     * Used for indicating Lat/Lon DMS decimal places.
     * Don't change this
     */
    public static int LAT_LON_DMS_DECIMAL_PLACES = 5;

    public final static float MINUTE = 1 / 60.0f;
    public final static double MINUTE_D = 1 / 60.0d;
    public final static float SECOND = 1 / 3600.0f;
    public final static double SECOND_D = 1 / 3600.0d;

    public static final double c_wgs84_a = 6378137.0;
    public static final double c_wgs84_e2 = 0.00669437999013;
    public static final double c_wgs84_f = 0.0033528106647475;

    public final static NumberFormat heading3DigitsFormat = new DecimalFormat("000");

    private static final int DEFAULT_DHOUSES_FOR_DMS = 2;
    private static final int DEFAULT_DHOUSES_FOR_DM  = 4;

    public static final Pattern LOC_DECIMAL = Pattern
            .compile("([NS])(\\d+\\.\\d+)\\, ([EW])(\\d+\\.\\d+)(\\, (-?\\d+\\.\\d+))?");
    public static final Pattern LOC_DM = Pattern
            .compile("(\\d+)([NS])(\\d+\\.\\d+)\\, (\\d+)([EW])(\\d+\\.\\d+)(\\, (-?\\d+\\.\\d+))?");
    public static final Pattern LOC_DMS = Pattern
            .compile("(\\d+)([NS])(\\d+)'(\\d+\\.\\d+)\\'', (\\d+)([EW])(\\d+)'(\\d+\\.\\d+)\\''(\\, (-?\\d+\\.\\d+))?");

    public static final Pattern COORD_DECIMAL = Pattern.compile("([-NSEW])(\\d+\\.\\d+)");
    public static final Pattern COORD_DM = Pattern.compile("(\\d+)([NSEW])(\\d+\\.\\d+)");
    public static final Pattern COORD_DMS = Pattern.compile("(\\d+)([NSEW])(\\d+)'(\\d+\\.\\d+)\\''");

    public static LocationType parseLocation(String locString) {
        double latDegs = 0, latMins = 0, latSecs = 0, latSign = 0;
        double lonDegs = 0, lonMins = 0, lonSecs = 0, lonSign = 0;
        double height = 0;

        Matcher m = LOC_DECIMAL.matcher(locString);

        if (m.matches()) {
            latSign = m.group(1).equals("N")? 1 : -1;
            latDegs = Double.parseDouble(m.group(2));

            lonSign = m.group(3).equals("E")? 1 : -1;
            lonDegs = Double.parseDouble(m.group(4));

            if (m.group(6) != null)
                height = Double.parseDouble(m.group(6));
        }
        else  {
            m = LOC_DM.matcher(locString);
            if (m.matches()) {
                latSign = m.group(2).equals("N")? 1 : -1;
                latDegs = Double.parseDouble(m.group(1));
                latMins = Double.parseDouble(m.group(3));

                lonSign = m.group(5).equals("E")? 1 : -1;
                lonDegs = Double.parseDouble(m.group(4));
                lonMins = Double.parseDouble(m.group(6));

                if (m.group(8) != null)
                    height = Double.parseDouble(m.group(8));
            }
            else {
                m = LOC_DMS.matcher(locString);
                if (m.matches()) {
                    latSign = m.group(2).equals("N")? 1 : -1;
                    latDegs = Double.parseDouble(m.group(1));
                    latMins = Double.parseDouble(m.group(3));
                    latSecs = Double.parseDouble(m.group(4));

                    lonSign = m.group(6).equals("E")? 1 : -1;
                    lonDegs = Double.parseDouble(m.group(5));
                    lonMins = Double.parseDouble(m.group(7));
                    lonSecs = Double.parseDouble(m.group(8));

                    if (m.group(10) != null)
                        height = Double.parseDouble(m.group(10));
                }
            }
        }

        LocationType loc = new LocationType(latSign * (latDegs + latMins / 60.0 + latSecs / 3600.0), 
                lonSign * (lonDegs + lonMins / 60.0 + lonSecs / 3600.0));
        loc.setHeight(height);
        
        return loc;
    }

    public static Double parseCoordString(String coord) {

        try {
            return Double.parseDouble(coord);
        }
        catch (Exception e) {
            // Let us try other formats
        }

        Matcher m = COORD_DECIMAL.matcher(coord.trim()); 
        if (m.matches()) {            
            if (m.group(1).equals("N") || m.group(1).equals("E"))
                return Double.valueOf(m.group(2));
            else
                return -Double.parseDouble(m.group(2));
        }

        m = COORD_DM.matcher(coord.trim());
        if (m.matches()) {
            double degs = Double.parseDouble(m.group(1));
            double mins = Double.parseDouble(m.group(3));
            double value = degs + (mins / 60.0);
            if (m.group(2).equals("N") || m.group(2).equals("E"))
                return value;
            else
                return -value;
        }

        m = COORD_DMS.matcher(coord.trim());
        if (m.matches()) {
            double degs = Double.parseDouble(m.group(1));
            double mins = Double.parseDouble(m.group(3));
            double secs = Double.parseDouble(m.group(4));
            double value = degs + (mins / 60.0) + (secs / 3600.0);

            if (m.group(2).equals("N") || m.group(2).equals("E"))
                return value;
            else
                return -value;
        }

        return Double.NaN;
    }

    /**
     * @param degrees
     * @param minutes
     * @param seconds
     * @return
     */
    public static double dmsToDecimalDegrees(double degrees, double minutes, double seconds) {
        double signal = 1;

        while (degrees > 90)
            degrees -= 180;
        while (degrees < -90)
            degrees += 180.0;

        if (degrees < 0)
            signal = -1;

        return degrees + minutes * signal * MINUTE_D + seconds * signal * SECOND_D;
    }

    /**
     * @param degrees
     * @param minutes
     * @param seconds
     * @return Result as Degrees Minutes
     */
    public static double[] dmsToDm(double degrees, double minutes, double seconds) {
        while (degrees > 90)
            degrees -= 180;
        while (degrees < -90)
            degrees += 180.0;

        double[] res = new double[2];
        res[0] = degrees;
        res[1] = minutes + seconds * MINUTE_D;
        return res;
    }

    /**
     * @param decimalDegress
     * @return
     */
    public static double[] decimalDegreesToDMS(double decimalDegress) {
        double[] dms = new double[3];
        int multiplier = 1;

        if (decimalDegress < 0) {
            multiplier = -1;
            decimalDegress = -decimalDegress;
        }

        double remainder = decimalDegress - Double.valueOf(decimalDegress).intValue();

        dms[0] = Math.floor(decimalDegress);

        dms[1] = Math.floor(remainder * 60.0d);

        remainder = remainder - (dms[1] / 60d);

        dms[2] = remainder * 3600d;

        dms[0] = multiplier * dms[0];

        return dms;
    }

    /**
     * @param decimalDegress
     * @return
     */
    public static double[] decimalDegreesToDM(double decimalDegress) {
        double[] dm = new double[2];

        double[] dms = decimalDegreesToDMS(decimalDegress);
        dm[0] = dms[0];
        dm[1] = dms[1] + dms[2] / 60d;
        return dm;
    }

    /**
     * @param dms
     * @param isLat
     * @return
     */
    private static String dmsToLatLonString(double[] dms, boolean isLat, int maxDecimalHouses) {
        return dmsToLatLonString(dms, isLat, false, maxDecimalHouses);
    }

    /**
     * @param dm
     * @param isLat
     * @return
     */
    private static String dmToLatLonString(double[] dm, boolean isLat, int maxDecimalHouses) {
        return dmsToLatLonString(dm, isLat, true, maxDecimalHouses);
    }

    /**
     * @param dms
     * @param isLat
     * @param dmonly
     * @param maxDecimalHouses if -1 it will not round the value.
     * @return
     */
    private static String dmsToLatLonString(double[] dms, boolean isLat, boolean dmonly, int maxDecimalHouses) {
        if (maxDecimalHouses < 0)
            maxDecimalHouses = dmonly ? DEFAULT_DHOUSES_FOR_DM : DEFAULT_DHOUSES_FOR_DMS;
        String l = "N";
        if (!isLat)
            l = "E";
        double d = dms[0];
        double m = dms[1];
        double s = 0d;
        if (!dmonly)
            s = dms[2];

        if ((d < 0 || "-0.0".equalsIgnoreCase("" + d)) && (Math.abs(d) + Math.abs(m) + Math.abs(s) != 0)) {
            l = "S";
            if (!isLat)
                l = "W";
            d = Math.abs(d);
        }

        NumberFormat nformat = DecimalFormat.getInstance(Locale.US);
        nformat.setMaximumFractionDigits(maxDecimalHouses);
        nformat.setMinimumFractionDigits(Math.min(3, maxDecimalHouses));
        nformat.setGroupingUsed(false);

        if (hasFracPart(d)) {
            nformat.setMaximumFractionDigits(maxDecimalHouses); // 8
            return l+nformat.format(d);// +"0' 0''";
        }

        if (dmonly) {
            return String.format("%d%s%s", (int) d, l, nformat.format(m));
        }

        if (hasFracPart(m)) {
            nformat.setMaximumFractionDigits(maxDecimalHouses); // 10
            return (int) Math.floor(d) + l + nformat.format(m);// +"' 0''";
        }

        nformat.setMaximumFractionDigits(maxDecimalHouses); // 8

        return String.format("%d%s%d'%s''", (int) Math.floor(d), l, (int) Math.floor(m), nformat.format(s));
    }

    private static boolean hasFracPart(double num) {
        double intPart = Math.floor(num);
        return (num - intPart) > 0;
    }

    /**
     * @param dms
     * @return
     */
    public static String dmsToLatString(double[] dms) {
        return dmsToLatLonString(dms, true, DEFAULT_DHOUSES_FOR_DMS);
    }

    /**
     * @param dms
     * @param maxDecimalHouses
     * @return
     */
    public static String dmsToLatString(double[] dms, int maxDecimalHouses) {
        return dmsToLatLonString(dms, true, maxDecimalHouses);
    }

    /**
     * @param dm
     * @return
     */
    public static String dmToLatString(double[] dm) {
        return dmToLatLonString(dm, true, DEFAULT_DHOUSES_FOR_DM);
    }

    /**
     * @param dm
     * @param maxDecimalHouses
     * @return
     */
    public static String dmToLatString(double[] dm, int maxDecimalHouses) {
        return dmToLatLonString(dm, true, maxDecimalHouses);
    }

    /**
     * @param d
     * @param m
     * @param s
     * @return
     */
    public static String dmsToLatString(double d, double m, double s) {
        double[] dms = { d, m, s };
        return dmsToLatLonString(dms, true, DEFAULT_DHOUSES_FOR_DMS);
    }

    /**
     * @param d
     * @param m
     * @param s
     * @param maxDecimalHouses
     * @return
     */
    public static String dmsToLatString(double d, double m, double s, int maxDecimalHouses) {
        double[] dms = { d, m, s };
        return dmsToLatLonString(dms, true, maxDecimalHouses);
    }

    /**
     * @param d
     * @param m
     * @return
     */
    public static String dmToLatString(double d, double m) {
        double[] dm = { d, m };
        return dmToLatLonString(dm, true, DEFAULT_DHOUSES_FOR_DM);
    }

    /**
     * @param d
     * @param m
     * @param maxDecimalHouses
     * @return
     */
    public static String dmToLatString(double d, double m, int maxDecimalHouses) {
        double[] dm = { d, m };
        return dmToLatLonString(dm, true, maxDecimalHouses);
    }

    /**
     * @param dms
     * @return
     */
    public static String dmsToLonString(double[] dms) {
        return dmsToLatLonString(dms, false, DEFAULT_DHOUSES_FOR_DMS);
    }

    /**
     * @param dms
     * @param maxDecimalHouses
     * @return
     */
    public static String dmsToLonString(double[] dms, int maxDecimalHouses) {
        return dmsToLatLonString(dms, false, maxDecimalHouses);
    }

    /**
     * @param dm
     * @return
     */
    public static String dmToLonString(double[] dm) {
        return dmToLatLonString(dm, false, DEFAULT_DHOUSES_FOR_DM);
    }

    /**
     * @param dm
     * @param maxDecimalHouses
     * @return
     */
    public static String dmToLonString(double[] dm, int maxDecimalHouses) {
        return dmToLatLonString(dm, false, maxDecimalHouses);
    }

    /**
     * @param d
     * @param m
     * @param s
     * @return
     */
    public static String dmsToLonString(double d, double m, double s) {
        double[] dms = { d, m, s };
        return dmsToLatLonString(dms, false, DEFAULT_DHOUSES_FOR_DMS);
    }

    /**
     * @param d
     * @param m
     * @param s
     * @param maxDecimalHouses
     * @return
     */
    public static String dmsToLonString(double d, double m, double s, int maxDecimalHouses) {
        double[] dms = { d, m, s };
        return dmsToLatLonString(dms, false, maxDecimalHouses);
    }

    /**
     * @param d
     * @param m
     * @return
     */
    public static String dmToLonString(double d, double m) {
        double[] dm = { d, m };
        return dmToLatLonString(dm, false, DEFAULT_DHOUSES_FOR_DM);
    }

    /**
     * @param d
     * @param m
     * @param maxDecimalHouses
     * @return
     */
    public static String dmToLonString(double d, double m, int maxDecimalHouses) {
        double[] dm = { d, m };
        return dmToLatLonString(dm, false, maxDecimalHouses);
    }


    private static String latLonTo83PFormatWorker(double latLonDegrees, boolean isLatOrLon) {
        // 33-46    -   GNSS Ships Positon Latitude (14 bytes) "_dd.mm.xxxxx_N" dd = degrees, mm = minutes, xxxxx = decimal Minutes, _ = Space, N = North or S = South
        // 47-60    -   GNSS Ships Postion Longitude (14 byes) "ddd.mm.xxxxx_E" ddd= degrees, mm = minutes, xxxxx = decimal minutes, E = East or W = West

        String letter;
        if (latLonDegrees >= 0)
            letter = isLatOrLon ? "N" : "E";
        else
            letter = isLatOrLon ? "S" : "W";

        double[] latLonDM = CoordinateUtil.decimalDegreesToDM(AngleUtils.nomalizeAngleDegrees180(latLonDegrees));
        String latLonStr = CoordinateUtil.dmToLatString(latLonDM[0], latLonDM[1], 5);
        latLonStr = latLonStr.replaceAll("[NSEW]", ".");
        String[] latLonParts = latLonStr.split("\\.");

        // fix dd size
        int sizeD = latLonParts[0].length();
        int insertPad = 3 - sizeD;
        String pad = isLatOrLon ? "0 " : "00";
        while (insertPad > 0) {
            latLonParts[0] = pad.charAt(2 - insertPad--) + latLonParts[0];
        }

        // fix mm size
        if (latLonParts[1].length() < 2)
            latLonParts[1] = "0" + latLonParts[1];

        // fix ss size
        sizeD = latLonParts[2].length();
        insertPad = 5 - sizeD;
        pad = "0000";
        while (insertPad > 0) {
            latLonParts[2] = latLonParts[2] + pad.charAt(2 - insertPad--);
        }

        String ret = latLonParts[0] + "." + latLonParts[1] + "." + latLonParts[2] + " " + letter; 
        return ret;
    }

    /**
     * Convert latitude degrees to the format "_dd.mm.xxxxx_N" dd = degrees, mm = minutes, xxxxx = decimal Minutes, _ = Space, N = North or S = South
     * @param latDegrees
     * @return
     */
    public static String latTo83PFormatWorker(double latDegrees) {
        return latLonTo83PFormatWorker(latDegrees, true);
    }

    /**
     * Convert longitude degrees to the format "ddd.mm.xxxxx_E" dd = degrees, mm = minutes, xxxxx = decimal Minutes, _ = Space, E = East or W = West
     * @param lonDegrees
     * @return
     */
    public static String lonTo83PFormatWorker(double lonDegrees) {
        return latLonTo83PFormatWorker(lonDegrees, false);
    }

    private static double latLonFrom83PFormatWorker(String latLonStr) {
        String[] parts = latLonStr.trim().split("[\\. ]");
        double sign = -1.0;
        if ("N".equalsIgnoreCase(parts[3].trim()) || "E".equalsIgnoreCase(parts[3].trim()))
            sign = 1.0;
        return sign * (Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim() + "." + parts[2].trim()) / 60d); 
    }

    public static double latFrom83PFormatWorker(String latStr) {
        return latLonFrom83PFormatWorker(latStr);
    }

    public static double lonFrom83PFormatWorker(String lonStr) {
        return latLonFrom83PFormatWorker(lonStr);
    }

    /**
     * Add an offset in meters(north, east) to a (lat,lon) in decimal degrees
     */
    public static double[] latLonAddNE2(double lat, double lon, double north, double east) {
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(lat);
        loc.setLongitudeDegs(lon);
        return WGS84displace(lat, lon, 0, north, east, 0);
    }

    /**
     * Computes the offset (north, east) in meters from (lat, lon) to (alat, alon) [both of these in decimal degrees].<br>
     * Subtract the two latlons and come up with the distance in meters N/S and E/W between them.
     */
    public static double[] latLonDiff(double lat, double lon, double alat, double alon) {

        double[] ret = WGS84displacement(lat, lon, 0, alat, alon, 0);
        return new double[] { ret[0], ret[1] };
    }

    /**
     * Changes a 3D point in the vehicle body reference frame to the inertial reference frame.
     * 
     * @param (x,y,z) are the point coodinates and (phi[rad],theta[rad],psi[rad]) represent the orientation on each axis from one reference frame to the other.
     *        Examples: If (1,1,1) is the point in one body frame and (0,0,pi/2) is the orientation of the body frame with respect to the the inertial frame,
     *        the same point in the inertial frame becomes (-1,1,1)
     * 
     * @return Is null a DoubleArray in the form of {x, y, z} which is the point in the inertial coordinates
     */
    public static double[] bodyFrameToInertialFrame(double x, double y, double z, double phi, double theta, double psi) {
        double[] result = { 0.0, 0.0, 0.0 };

        // euler angles transformation - generated automatically with maple - inertial.ms
        double cpsi = Math.cos(psi);
        double spsi = Math.sin(psi);
        double ctheta = Math.cos(theta);
        double stheta = Math.sin(theta);
        double cphi = Math.cos(phi);
        double sphi = Math.sin(phi);
        double t3 = y * cpsi;
        double t4 = stheta * sphi;
        double t6 = y * spsi;
        double t8 = z * cpsi;
        double t9 = stheta * cphi;
        double t11 = z * spsi;

        result[0] = cpsi * ctheta * x + t3 * t4 - t6 * cphi + t8 * t9 + t11 * sphi;
        result[1] = spsi * ctheta * x + t6 * t4 + t3 * cphi + t11 * t9 - t8 * sphi;
        result[2] = -stheta * x + ctheta * sphi * y + ctheta * cphi * z;

        return result;
    }

    /**
     * Changes a 3D point in the inertial reference frame to the vehicle body frame.
     * 
     * @param (x,y,z) are the point coodinates and (phi,theta,psi) represent the orientation on each axis from one reference frame to the other. Examples: If
     *        (-1,1,1) is the point in one inertial frame and (0,0,pi/2) is the orientation of the body frame with respect to the the body frame, the same point
     *        in the inertial frame becomes (1,1,1)
     * 
     * @return Is a DoubleArray in the form of {x, y, z} which is the point in the body coordinates
     */
    public static double[] inertialFrameToBodyFrame(double x, double y, double z, double phi, double theta, double psi) {
        double[] result = { 0.0, 0.0, 0.0 };

        // euler to body velocities transformation - generated automaticly with maple - inertial.ms
        double t1 = Math.cos(psi);
        double t2 = Math.cos(theta);
        double t5 = Math.sin(psi);
        double t8 = Math.sin(theta);
        result[0] = t1 * t2 * x + t5 * t2 * y - t8 * z;
        double t10 = x * t1;
        double t11 = Math.sin(phi);
        double t12 = t8 * t11;
        double t14 = x * t5;
        double t15 = Math.cos(phi);
        double t17 = y * t5;
        double t19 = y * t1;
        result[1] = t10 * t12 - t14 * t15 + t17 * t12 + t19 * t15 + t11 * t2 * z;
        double t23 = t8 * t15;
        result[2] = t10 * t23 + t14 * t11 + t17 * t23 - t19 * t11 + t15 * t2 * z;

        return result;
    }

    /**
     * This method transforms spherical coordinates to cartesian coordinates.
     * 
     * @param r Distance
     * @param theta Azimuth (\u00B0)
     * @param phi Zenith (\u00B0)
     * @return Array with x,y,z positions
     */
    public static double[] sphericalToCartesianCoordinates(double r, double theta, double phi) {
        double[] cartesian = new double[3];

        if (r == 0) {
            cartesian[0] = 0;
            cartesian[1] = 0;
            cartesian[2] = 0;
            return cartesian;
        }

        // converts degrees to rad
        theta = Math.toRadians(theta);
        phi = Math.toRadians(phi);

        double x = r * Math.cos(theta) * Math.sin(phi);
        double y = r * Math.sin(theta) * Math.sin(phi);
        double z = r * Math.cos(phi);

        cartesian[0] = x;
        cartesian[1] = y;
        cartesian[2] = z;

        return cartesian;
    }

    /**
     * This method transforms cylindrical coordinates to cartesian coordinates.
     * 
     * @param r Distance
     * @param theta Azimuth (\u00B0)
     * @param h Height
     * @return Array with x,y,z positions
     */
    public static double[] cylindricalToCartesianCoordinates(double r, double theta, double h) {
        double[] cartesian = new double[3];

        if (r == 0) {
            cartesian[0] = 0;
            cartesian[1] = 0;
            cartesian[2] = h;
            return cartesian;
        }

        // converts degrees to rad
        theta = Math.toRadians(theta);

        double x = r * Math.cos(theta);
        double y = r * Math.sin(theta);
        double z = h;

        cartesian[0] = x;
        cartesian[1] = y;
        cartesian[2] = z;

        return cartesian;
    }

    /**
     * This metho transforms cartesian coordinates to sherical coordinates
     * 
     * @param x
     * @param y
     * @param z
     * @return Array with r, theta (rad) and phi (rad) (distance, azimuth, zenith)
     */
    public static double[] cartesianToSphericalCoordinates(double x, double y, double z) {
        double[] polar = new double[3];
        double r = Math.sqrt(x * x + y * y + z * z);
        if (r >= 1e-6) {
            double theta = 0;
            if (Math.abs(x) > 1e-6) {
                if (x > 0)
                    theta = Math.atan(y / x);
                else
                    theta = Math.PI + Math.atan(y / x);
            }
            else {
                if (y == 0) {
                    theta = 0;
                }
                else if (y > 0) {
                    theta = Math.PI / 2;
                }
                else if (y < 0) {
                    theta = -Math.PI / 2;
                }
            }
            double phi = Math.acos(z / r);

            polar[0] = r;
            polar[1] = theta;
            polar[2] = phi;
        }
        else if (r < 1e-6) {
            polar[0] = 0;
            polar[1] = 0;
            polar[2] = 0;
        }
        return polar;
    }

    /**
     * This metho transforms cartesian coordinates to sherical coordinates
     * 
     * @param x
     * @param y
     * @param z
     * @return Array with r, theta (rad) and h (distance, azimuth, height)
     */
    public static double[] cartesianToCylindricalCoordinates(double x, double y, double z) {
        double[] cyl = new double[3];
        double r = Math.sqrt(x * x + y * y);
        double h = z;
        if (r >= 1e-6) {
            double theta = 0;
            if (Math.abs(x) > 1e-6) {
                if (x > 0)
                    theta = Math.atan(y / x);
                else
                    theta = Math.PI + Math.atan(y / x);
            }
            else {
                if (y == 0) {
                    theta = 0;
                }
                else if (y > 0) {
                    theta = Math.PI / 2;
                }
                else if (y < 0) {
                    theta = -Math.PI / 2;
                }
            }

            cyl[0] = r;
            cyl[1] = theta;
            cyl[2] = h;
        }
        else if (r < 1e-6) {
            cyl[0] = 0;
            cyl[1] = 0;
            cyl[2] = h;
        }
        return cyl;
    }

    /**
     * This method transforms spherical coordinates to cylindrical coordinates.
     * 
     * @param r Distance
     * @param theta Azimuth (\u00B0)
     * @param phi Zenith (\u00B0)
     * @return Array
     */
    public static double[] sphericalToCylindricalCoordinates(double r, double theta, double phi) {
        double[] rec = sphericalToCartesianCoordinates(r, theta, phi);
        double[] cyl = cartesianToCylindricalCoordinates(rec[0], rec[1], rec[2]);
        return cyl;
    }

    /**
     * This method transforms cylindrical coordinates to spherical coordinates.
     * 
     * @param r Distance
     * @param theta Azimuth (\u00B0)
     * @param phi Zenith (\u00B0)
     * @return Array
     */
    public static double[] cylindricalToShericalCoordinates(double r, double theta, double h) {
        double[] rec = cylindricalToCartesianCoordinates(r, theta, h);
        double[] sph = cartesianToSphericalCoordinates(rec[0], rec[1], rec[2]);
        return sph;
    }

    /**
     * @param r Distance
     * @param theta Azimuth (\u00B0)
     * @param phi Zenith (\u00B0)
     * @param x
     * @param y
     * @param z
     * @return r, theta (rads), h
     */
    public static double[] addSphericalToCartesianOffsetsAndGetAsCylindrical(double r, double theta, double phi, double x, double y, double z) {
        double[] sphRec = sphericalToCartesianCoordinates(r, theta, phi);
        double[] recSum = new double[] { x + sphRec[0], y + sphRec[1], z + sphRec[2] };
        double[] result = cartesianToCylindricalCoordinates(recSum[0], recSum[1], recSum[2]);
        return result;
    }

    private static double[] latLonDepthToGeocentricXYZ(double latitude, double longitude, double depth) {
        final double a = 6378137.0; // (metros do semi-eixo maior )
        final double b = 6356752.3142; // (metros do semi-eixo menor)

        double Hi = -depth;
        double Lat = Math.toRadians(latitude);
        double Lon = Math.toRadians(longitude);

        double CFi = Math.cos(Lat);
        double SFi = Math.sin(Lat);

        double N = a * a / Math.sqrt(a * a * CFi * CFi + b * b * SFi * SFi);

        double[] xyz = new double[3];

        xyz[0] = (N + Hi) * CFi * Math.cos(Lon);
        xyz[1] = (N + Hi) * CFi * Math.sin(Lon);
        xyz[2] = (b * b / a / a * N + Hi) * SFi;

        return xyz;
    }

    private static double[] geocentricXYZToLatLonDepth(double[] xyz) {

        // ned[2] = ned[2];

        final double a = 6378137.0; // (metros do semi-eixo maior )
        final double b = 6356752.3142; // (metros do semi-eixo menor)

        double XY2 = xyz[0] * xyz[0] + xyz[1] * xyz[1];
        double XY = Math.sqrt(XY2);
        double en2 = (a * a - b * b) / b;
        double ed2 = en2 * b / a;
        double den2 = xyz[2] * xyz[2] * (a * a) + XY2 * (b * b);
        double den = Math.sqrt(den2);

        double Lat = Math.atan2(xyz[2] + (en2 * a * a * a) * xyz[2] * xyz[2] * xyz[2] / (den2 * den), XY - (ed2 * b * b * b) * XY2 * XY / (den2 * den));
        double Lon = Math.atan2(xyz[1], xyz[0]);

        double CLat = Math.cos(Lat);
        double SLat = Math.sin(Lat);
        double N = (a * a) / Math.sqrt(a * a * CLat * CLat + b * b * SLat * SLat);
        double Hi = XY / CLat - N;

        double[] ret = new double[3];
        ret[0] = Math.toDegrees(Lat);
        ret[1] = Math.toDegrees(Lon);
        ret[2] = -Hi;

        return ret;
    }

    private static Matrix3d makeNedToEarthConversionMatrix(double[] latLonDepth) {
        double Lat = Math.toRadians(latLonDepth[0]);
        double Lon = Math.toRadians(latLonDepth[1]);

        Matrix3d m3d = new Matrix3d();

        // double out[][] = new double[3][3];

        m3d.m00 = -Math.sin(Lat) * Math.cos(Lon);
        m3d.m01 = -Math.sin(Lon);
        m3d.m02 = -Math.cos(Lat) * Math.cos(Lon);
        m3d.m10 = -Math.sin(Lat) * Math.sin(Lon);
        m3d.m11 = Math.cos(Lon);
        m3d.m12 = -Math.cos(Lat) * Math.sin(Lon);
        m3d.m20 = Math.cos(Lat);
        m3d.m21 = 0.0;
        m3d.m22 = -Math.sin(Lat);

        return m3d;
    }

    protected static double[] normalizeOffsetToLocation(double[] nedOffset, double[] LatLonDepth) {
        Matrix3d conversionMatrix = makeNedToEarthConversionMatrix(LatLonDepth);

        double v3dx = nedOffset[0];
        double v3dy = nedOffset[1];
        double v3dz = nedOffset[2];

        double x = conversionMatrix.m00 * v3dx + conversionMatrix.m01 * v3dx + conversionMatrix.m02 * v3dx;
        double y = conversionMatrix.m10 * v3dy + conversionMatrix.m11 * v3dx + conversionMatrix.m12 * v3dx;
        double z = conversionMatrix.m20 * v3dz + conversionMatrix.m21 * v3dz + conversionMatrix.m22 * v3dz;

        return new double[] { x, y, z };
    }

    /**
     * Gently taken from openmap UTMPoint class
     * 
     * @param lat A latitude in decimal degrees
     * @param lon A longitude in decimal degrees
     * @return An array a[2] with a[0] = northingMeters and a[1] = eastingMeters
     */
    protected double[] latLonToUTM(double lat, double lon) {
        double WGS84_radius = 6378137.0d;
        double WGS84_eccSqared = 0.00669438d;
        double a = WGS84_radius;
        double eccSquared = WGS84_eccSqared;
        double k0 = 0.9996;

        double LongOrigin;
        double eccPrimeSquared;
        double N, T, C, A, M;

        double LatRad = Math.toRadians(lat);
        double LongRad = Math.toRadians(lon);
        double LongOriginRad;
        int ZoneNumber;
        ZoneNumber = (int) ((lon + 180) / 6) + 1;

        // Make sure the longitude 180.00 is in Zone 60
        if (lon == 180) {
            ZoneNumber = 60;
        }

        // Special zone for Norway
        if (lat >= 56.0f && lat < 64.0f && lon >= 3.0f && lon < 12.0f) {
            ZoneNumber = 32;
        }

        // Special zones for Svalbard
        if (lat >= 72.0f && lat < 84.0f) {
            if (lon >= 0.0f && lon < 9.0f)
                ZoneNumber = 31;
            else if (lon >= 9.0f && lon < 21.0f)
                ZoneNumber = 33;
            else if (lon >= 21.0f && lon < 33.0f)
                ZoneNumber = 35;
            else if (lon >= 33.0f && lon < 42.0f)
                ZoneNumber = 37;
        }
        LongOrigin = (ZoneNumber - 1) * 6 - 180 + 3; // +3 puts origin
        // in middle of
        // zone
        LongOriginRad = Math.toRadians(LongOrigin);

        eccPrimeSquared = (eccSquared) / (1 - eccSquared);

        N = a / Math.sqrt(1 - eccSquared * Math.sin(LatRad) * Math.sin(LatRad));
        T = Math.tan(LatRad) * Math.tan(LatRad);
        C = eccPrimeSquared * Math.cos(LatRad) * Math.cos(LatRad);
        A = Math.cos(LatRad) * (LongRad - LongOriginRad);

        M = a
                * ((1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256) * LatRad
                        - (3 * eccSquared / 8 + 3 * eccSquared * eccSquared / 32 + 45 * eccSquared * eccSquared * eccSquared / 1024) * Math.sin(2 * LatRad)
                        + (15 * eccSquared * eccSquared / 256 + 45 * eccSquared * eccSquared * eccSquared / 1024) * Math.sin(4 * LatRad) - (35 * eccSquared
                                * eccSquared * eccSquared / 3072)
                        * Math.sin(6 * LatRad));

        double UTMEasting = (k0 * N * (A + (1 - T + C) * A * A * A / 6.0d + (5 - 18 * T + T * T + 72 * C - 58 * eccPrimeSquared) * A * A * A * A * A / 120.0d) + 500000.0d);

        double UTMNorthing = (float) (k0 * (M + N
                * Math.tan(LatRad)
        * (A * A / 2 + (5 - T + 9 * C + 4 * C * C) * A * A * A * A / 24.0d + (61 - 58 * T + T * T + 600 * C - 330 * eccPrimeSquared) * A * A * A * A
                * A * A / 720.0d)));
        if (lat < 0.0f) {
            UTMNorthing += 10000000.0d; // 10000000 meter offset for
            // southern hemisphere
        }

        return new double[] { UTMNorthing * 1000.0, UTMEasting * 1000.0 };
    }

    /**
     * @param latitudeDegs The value of lat
     * @param format The format, or null to use the {@link GeneralPreferences#latLonPrefFormat}
     * @return
     */
    public static String latitudeAsPrettyString(double latitudeDegs, LatLonFormatEnum format) {
        return latlongAsPrettyStringWorker(true, latitudeDegs, format);
    }

    /**
     * The format will be the {@link GeneralPreferences#latLonPrefFormat}
     *
     * @param latitudeDegs The value of lat
     * @return
     */
    public static String latitudeAsPrettyString(double latitudeDegs) {
        return latlongAsPrettyStringWorker(true, latitudeDegs, null);
    }

    /**
     * @param longitudeDegs The value of lon
     * @param format The format, or null to use the {@link GeneralPreferences#latLonPrefFormat}
     * @return
     */
    public static String longitudeAsPrettyString(double longitudeDegs, LatLonFormatEnum format) {
        return latlongAsPrettyStringWorker(false, longitudeDegs, format);
    }

    /**
     * The format will be the {@link GeneralPreferences#latLonPrefFormat}
     *
     * @param longitudeDegs The value of lon
     * @return
     */
    public static String longitudeAsPrettyString(double longitudeDegs) {
        return latlongAsPrettyStringWorker(false, longitudeDegs, null);
    }

    /**
     * @param isLat To denote if is lat or lon
     * @param latlongitudeDegs The value of lat or lon
     * @param format The format, or null to use the {@link GeneralPreferences#latLonPrefFormat}
     * @return
     */
    private static String latlongAsPrettyStringWorker(boolean isLat, double latlongitudeDegs, LatLonFormatEnum format) {
        if (format == null)
            format = GeneralPreferences.latLonPrefFormat;

        boolean showSeconds = true;
        switch (format) {
            case DECIMAL_DEGREES:
                NumberFormat nformat = DecimalFormat.getInstance(Locale.US);
                nformat.setMaximumFractionDigits(LAT_LON_DDEGREES_DECIMAL_PLACES);
                nformat.setMinimumFractionDigits(5);
                nformat.setGroupingUsed(false);
                return (Math.signum(latlongitudeDegs) >= 0 ? (isLat ? "N" : "E") : (isLat ? "S" : "W"))
                        + nformat.format(Math.abs(latlongitudeDegs));
            case DM:
                showSeconds = false;
            case DMS:
            default:
                return isLat
                        ? latitudeAsString(latlongitudeDegs, !showSeconds,
                                showSeconds ? LAT_LON_DMS_DECIMAL_PLACES : LAT_LON_DM_DECIMAL_PLACES)
                        : longitudeAsString(latlongitudeDegs, !showSeconds,
                                showSeconds ? LAT_LON_DMS_DECIMAL_PLACES : LAT_LON_DM_DECIMAL_PLACES);
        }
    }

    public static String latitudeAsString(double latitude) {
        return latitudeAsString(latitude, true);
    }

    public static String longitudeAsString(double longitude) {
        return longitudeAsString(longitude, true);
    }

    public static String latitudeAsString(double latitude, boolean minutesOnly) {
        return latitudeAsString(latitude, minutesOnly, -1);
    }

    public static String latitudeAsString(double latitude, boolean minutesOnly, int maxDecimalHouses) {
        if (!minutesOnly)
            return dmsToLatLonString(decimalDegreesToDMS(latitude), true, maxDecimalHouses);
        else
            return dmToLatLonString(decimalDegreesToDM(latitude), true, maxDecimalHouses);
    }

    public static String longitudeAsString(double longitude, boolean minutesOnly) {
        return longitudeAsString(longitude, minutesOnly, -1);
    }

    public static String longitudeAsString(double longitude, boolean minutesOnly, int maxDecimalHouses) {
        if (!minutesOnly)
            return dmsToLatLonString(decimalDegreesToDMS(longitude), false, maxDecimalHouses);
        else
            return dmToLatLonString(decimalDegreesToDM(longitude), false, maxDecimalHouses);
    }

    public static void copyToClipboard(LocationType lt) {
        LocationType lt2 = new LocationType();
        double[] absCoords = lt.getAbsoluteLatLonDepth();
        lt2.setLatitudeDegs(absCoords[0]);
        lt2.setLongitudeDegs(absCoords[1]);
        lt2.setDepth(absCoords[2]);
        ClipboardOwner owner = (clipboard, contents) -> {};
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(lt2.getClipboardText()), owner);
    }

    // ---------------------------------------------------------------------------------------------
    /**
     * Copied from Dune
     * 
     * @param loc
     * @return
     */
    private static double[] toECEF(double latDegrees, double lonDegrees, double depth) {

        double[] lld = {latDegrees, lonDegrees, depth};

        lld[0] = Math.toRadians(lld[0]);
        lld[1] = Math.toRadians(lld[1]);

        double cos_lat = Math.cos(lld[0]);
        double sin_lat = Math.sin(lld[0]);
        double cos_lon = Math.cos(lld[1]);
        double sin_lon = Math.sin(lld[1]);
        double rn = c_wgs84_a / Math.sqrt(1.0 - c_wgs84_e2 * sin_lat * sin_lat);
        double[] ned = new double[3];
        ned[0] = (rn - lld[2]) * cos_lat * cos_lon;
        ned[1] = (rn - lld[2]) * cos_lat * sin_lon;
        ned[2] = (((1.0 - c_wgs84_e2) * rn) - lld[2]) * sin_lat;

        return ned;
    }

    /**
     * Copied from Dune
     * 
     * @param lat
     * @return
     */
    private static double n_rad(double lat) {
        double lat_sin = Math.sin(lat);
        return c_wgs84_a / Math.sqrt(1 - c_wgs84_e2 * (lat_sin * lat_sin));
    }

    /**
     * Copied from Dune
     * 
     * @param x
     * @param y
     * @param z
     * @return
     */
    private static double[] toGeodetic(double x, double y, double z) {
        double[] lld = new double[3];

        double p = Math.sqrt(x * x + y * y);
        lld[1] = Math.atan2(y, x);
        lld[0] = Math.atan2(z / p, 0.01);
        double n = n_rad(lld[0]);
        lld[2] = p / Math.cos(lld[0]) - n;
        double old_hae = -1e-9;
        double num = z / p;

        while (Math.abs(lld[2] - old_hae) > 1e-4) {
            old_hae = lld[2];
            double den = 1 - c_wgs84_e2 * n / (n + lld[2]);
            lld[0] = Math.atan2(num, den);
            n = n_rad(lld[0]);
            lld[2] = p / Math.cos(lld[0]) - n;
        }

        lld[0] = Math.toDegrees(lld[0]);
        lld[1] = Math.toDegrees(lld[1]);

        return lld;
    }

    public static double[] WGS84displacement(LocationType loc1, LocationType loc2) {
        LocationType locTmp1 = loc1.getNewAbsoluteLatLonDepth();
        LocationType locTmp2 = loc2.getNewAbsoluteLatLonDepth();
        return WGS84displacement(locTmp1.getLatitudeDegs(), locTmp1.getLongitudeDegs(), locTmp1.getDepth(),
                locTmp2.getLatitudeDegs(), locTmp2.getLongitudeDegs(), locTmp2.getDepth());
    }

    /**
     * Copied from Dune
     * 
     * @param loc1
     * @param loc2
     * @return
     */
    public static double[] WGS84displacement(double latDegrees1, double lonDegrees1, double depth1,
            double latDegrees2, double lonDegrees2, double depth2) {

        double[] cs1;
        double[] cs2;

        cs1 = toECEF(latDegrees1, lonDegrees1, depth1);
        cs2 = toECEF(latDegrees2, lonDegrees2, depth2);

        double ox = cs2[0] - cs1[0];
        double oy = cs2[1] - cs1[1];
        double oz = cs2[2] - cs1[2];
        double[] lld1 = { latDegrees1, lonDegrees1, depth1 };

        double slat = Math.sin(Math.toRadians(lld1[0]));
        double clat = Math.cos(Math.toRadians(lld1[0]));
        double slon = Math.sin(Math.toRadians(lld1[1]));
        double clon = Math.cos(Math.toRadians(lld1[1]));

        double[] ret = new double[3];

        ret[0] = -slat * clon * ox - slat * slon * oy + clat * oz; // North
        ret[1] = -slon * ox + clon * oy; // East
        ret[2] = depth1 - depth2;

        return ret;
    }

    /**
     * Copied from Dune
     * 
     * @param loc
     * @param n
     * @param e
     * @param d
     */
    public static double[] WGS84displace(double latDegrees, double lonDegrees, double depth, double n, double e, double d) {
        // Convert reference to ECEF coordinates
        double[] xyz = toECEF(latDegrees, lonDegrees, depth);
        double[] lld = {latDegrees, lonDegrees, depth };
        // Compute Geocentric latitude
        double phi = Math.atan2(xyz[2], Math.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1]));

        // Compute all needed sine and cosine terms for conversion.
        double slon = Math.sin(Math.toRadians(lld[1]));
        double clon = Math.cos(Math.toRadians(lld[1]));
        double sphi = Math.sin(phi);
        double cphi = Math.cos(phi);

        // Obtain ECEF coordinates of displaced point
        // Note: some signs from standard ENU formula
        // are inverted - we are working with NED (= END) coordinates
        xyz[0] += -slon * e - clon * sphi * n - clon * cphi * d;
        xyz[1] += clon * e - slon * sphi * n - slon * cphi * d;
        xyz[2] += cphi * n - sphi * d;

        // Convert back to WGS-84 coordinates
        lld = toGeodetic(xyz[0], xyz[1], xyz[2]);

        if (d != 0d)
            lld[2] = depth + d;
        else
            lld[2] = depth;
        return lld;
    }

    /**
     * Copied from Dune
     * Get North-East bearing and range between two latitude/longitude coordinates.
     */
    public static double[] getNEBearingDegreesAndRange(LocationType loc1, LocationType loc2) {
        double[] ne = WGS84displacement(loc1, loc2);
        double n = ne[0];
        double e = ne[1];
        double bearing = Math.atan2(e, n);
        double range = Math.sqrt(n * n + e * e);
        return new double[] { Math.toDegrees(bearing), range };
    }

    /**
     * Traces a line between l1 and l2 and computes the distance
     * of point to this line. If the 3 locations are colinear this
     * distance will be = 0 + e, where e should be a small error.
     * */
    public static double distanceToLine(LocationType point, LocationType l1, LocationType l2) {
        double[] pt1 = l1.getOffsetFrom(point);
        double[] pt2 = l2.getOffsetFrom(point);
        Line2D line = new Line2D.Double(pt1[0], pt1[1], pt2[0], pt2[1]);
        return line.ptLineDist(new Point2D.Double());
    }

    /**
     * Compute the centroid of the given locations
     * */
    public static LocationType computeLocationsCentroid(List<LocationType> locations) {
        double sumLatDegs = 0;
        double sumLonDegs = 0;

        for(LocationType loc : locations) {
            LocationType tmp = loc.getNewAbsoluteLatLonDepth();
            sumLatDegs += tmp.getLatitudeDegs();
            sumLonDegs += tmp.getLongitudeDegs();
        }

        return new LocationType(sumLatDegs/locations.size(), sumLonDegs/locations.size());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * @deprecated Use {@link LocationType#getNewAbsoluteLatLonDepth()} that returns a copy of the location but without offsets, or
     *             {@link LocationType#convertToAbsoluteLatLonDepth()} that does a similar thing but it changes the location itself to be without offsets
     *             (returning the location itself, not a copy).
     * @param point
     * @return A new Location but without offsets. This calls {@link LocationType#getNewAbsoluteLatLonDepth()}.
     */
    @Deprecated
    public static LocationType getAbsoluteLatLonDepth(LocationType point) {
        return point.getNewAbsoluteLatLonDepth();
    }

    public static void main(String[] args) {

        NeptusLog.pub().info(heading3DigitsFormat.format(34));

        LocationType locA = new LocationType();
        locA.setLatitudeStr("41N10.6938");
        locA.setLongitudeStr("8W42.5051");

        LocationType locB = new LocationType();
        locB.setLatitudeStr("44N40.7312");
        locB.setLongitudeStr("63W32.2072");

        LocationType locC = new LocationType(locA);
        locC.translatePosition(100, 100, 0);
        locC.convertToAbsoluteLatLonDepth();

        LocationType locD = new LocationType(locB);
        locD.translatePosition(100, 100, 0);
        locD.convertToAbsoluteLatLonDepth();

        LocationType locE = new LocationType(locA);
        locE.translatePosition(10000, 10000, 0);
        locE.convertToAbsoluteLatLonDepth();

        LocationType locF = new LocationType(locB);
        locF.translatePosition(10000, 10000, 0);
        locF.convertToAbsoluteLatLonDepth();

        LocationType locG = new LocationType(locA);
        locG.setLatitudeDegs(locG.getLatitudeDegs() + 2);
        locG.setLongitudeDegs(locG.getLongitudeDegs() + 2);

        LocationType locH = new LocationType(locB);
        locH.setLatitudeDegs(locH.getLatitudeDegs() + 2);
        locH.setLongitudeDegs(locH.getLongitudeDegs() + 2);

        LocationType locI = new LocationType(locA);
        locI.setLatitudeDegs(locI.getLatitudeDegs() + 2);

        LocationType locJ = new LocationType(locB);
        locJ.setLatitudeDegs(locH.getLatitudeDegs() + 2);

        LocationType locK = new LocationType(locA);
        locK.setLongitudeDegs(locK.getLongitudeDegs() + 2);

        LocationType locL = new LocationType(locB);
        locL.setLongitudeDegs(locL.getLongitudeDegs() + 2);

        String[] obs = new String[] { "loc1 Leixões :: loc2 Halifax, Canada", "loc1 Leixões :: loc2=loc1 + N100m,E100m",
                "loc1 Halifax :: loc2=loc1 + N100m,E100m", "loc1 Leixões :: loc2=loc1 + N10000m,E10000m", "loc1 Halifax :: loc2=loc1 + N10000m,E10000m",
                "loc1 Leixões :: loc2=loc1 + N2\u00B0,E2\u00B0", "loc1 Halifax :: loc2=loc1 + N2\u00B0,E2\u00B0", "loc1 Leixões :: loc2=loc1 + N2\u00B0",
                "loc1 Halifax :: loc2=loc1 + N2\u00B0", "loc1 Leixões :: loc2=loc1 + E2\u00B0", "loc1 Halifax :: loc2=loc1 + E2\u00B0" };
        LocationType[] locations = new LocationType[] { locA, locB, locA, locC, locB, locD, locA, locE, locB, locF, locA, locG, locB, locH, locA, locI, locB,
                locJ, locA, locK, locB, locL };

        for (int i = 0; i < locations.length; i++) {
            LocationType loc1 = locations[i];
            LocationType loc2 = locations[++i];

            NeptusLog.pub().info("_______________________________________________________________________________");
            NeptusLog.pub().info(">>> " + obs[i / 2] + " <<<");
            System.out
            .println("loc1: {" + loc1.getLatitudeDegs() + "\u00B0, " + loc1.getLongitudeDegs() + "\u00B0, " + loc1.getDepth() + "]");
            System.out
            .println("loc2: {" + loc2.getLatitudeDegs() + "\u00B0, " + loc2.getLongitudeDegs() + "\u00B0, " + loc1.getDepth() + "]");

            double[] diff1 = loc1.getOffsetFrom(loc2);
            NeptusLog.pub().info("loc1.getOffsetFrom(loc2)\t\t[" + diff1[0] + ", " + diff1[1] + ", " + diff1[2] + "]");

            double[] neDiff = CoordinateUtil.latLonDiff(loc2.getLatitudeDegs(), loc2.getLongitudeDegs(), loc1.getLatitudeDegs(), loc1.getLongitudeDegs());
            NeptusLog.pub().info("loc1.getOffsetFromWorker(loc2)\t\t[" + neDiff[0] + ", " + neDiff[1] + ", " + 0 + "]");

            double[] locC1 = CoordinateUtil.latLonDepthToGeocentricXYZ(loc1.getLatitudeDegs(), loc1.getLongitudeDegs(), loc1.getDepth());
            NeptusLog.pub().info("latLonDepthToGeocentricXYZ:loc1 \t[[" + locC1[0] + ", " + locC1[1] + ", " + locC1[2] + "]]");
            double[] locC2 = CoordinateUtil.latLonDepthToGeocentricXYZ(loc2.getLatitudeDegs(), loc2.getLongitudeDegs(), loc2.getDepth());
            NeptusLog.pub().info("latLonDepthToGeocentricXYZ:loc2 \t[[" + locC2[0] + ", " + locC2[1] + ", " + locC2[2] + "]]");
            double[] diff2 = { locC1[0] - locC2[0], locC1[1] - locC2[1], locC1[2] - locC2[2] };
            NeptusLog.pub().info("diff of latLonDepthToGeocentricXYZ \t[" + diff2[0] + ", \t" + diff2[1] + ", \t" + diff2[2] + "]");
            double ox = locC1[0] - locC2[0];
            double oy = locC1[1] - locC2[1];
            double oz = locC1[2] - locC2[2];
            double slat = Math.sin(loc2.getLatitudeRads());
            double clat = Math.cos(loc2.getLatitudeRads());
            double slon = Math.sin(loc2.getLongitudeRads());
            double clon = Math.cos(loc2.getLongitudeRads());
            double n = -slat * clon * ox - slat * slon * oy + clat * oz; // North
            double e = -slon * ox + clon * oy; // East
            NeptusLog.pub().info("diff similar to dune \t\t\t[" + n + ", \t" + e + ", \t" + 0 + "]");

            double[] locC3 = CoordinateUtil.WGS84displacement(loc2, loc1);
            NeptusLog.pub().info("WGS84displacement(loc2, loc1) \t\t[" + locC3[0] + ", \t" + locC3[1] + ", \t" + locC3[2] + "]");
            double[] diffPx = MapTileUtil.getOffsetFrom(loc2.getLatitudeDegs(), loc2.getLongitudeDegs(), loc1.getLatitudeDegs(), loc1.getLongitudeDegs());
            NeptusLog.pub().info("MapTileUtil.getOffsetFrom(loc2, loc1) \t[" + diffPx[0] + ", \t" + diffPx[1] + ", \t" + 0 + "]");

            NeptusLog.pub().info("_______________________________________________________________________________");
        }        

        // 41N3.6117
        // 8W27.4009
//        String te = "41N3.6117";
//        String[] st = CoordinateUtil.parseLatitudeCoordToStringArray(te);
//        NeptusLog.pub().info(st[1] + st[0] + st[2] + " " + st[3]);
//        NeptusLog.pub().info(CoordinateUtil.parseLatitudeCoordToDoubleValue(te));
//        // NeptusLog.pub().info(CoordinateUtil.strtolat(te));

//        te = "8W27.4009";
//        st = CoordinateUtil.parseLongitudeCoordToStringArray(te);
//        NeptusLog.pub().info(st[1] + st[0] + st[2] + " " + st[3]);
//        NeptusLog.pub().info(CoordinateUtil.parseLongitudeCoordToDoubleValue(te));
////        NeptusLog.pub().info(CoordinateUtil.strtolon(te));

        NeptusLog.pub().info("rotation of pi/2 of point (1,1,1). The result should be (-1,1,1)");
        double[] teste = CoordinateUtil.bodyFrameToInertialFrame(1, 1, 1, 0, 0, Math.PI / 2);
        NeptusLog.pub().info(teste[0] + " " + teste[1] + " " + teste[2]);

        NeptusLog.pub().info("rotation of -pi/2 of point (-1,1,1). The result should be initial (1,1,1)");
        teste = CoordinateUtil.inertialFrameToBodyFrame(-1, 1, 1, 0, 0, Math.PI / 2);
        NeptusLog.pub().info(teste[0] + " " + teste[1] + " " + teste[2]);

        NeptusLog.pub().info(">>> Test latLonDepthToGeocentricXYZ and geocentricXYZToLatLonDepth:");
        double[] latLonDep = new double[] { 41.3433, -8.2334, 100 };
        double[] rev = latLonDepthToGeocentricXYZ(latLonDep[0], latLonDep[1], latLonDep[2]);
        double[] latLonDep2 = geocentricXYZToLatLonDepth(rev);
        NeptusLog.pub().info("Lat: " + latLonDep[0] + ", Lon: " + latLonDep[1] + ", Dep: " + latLonDep[2]);
        NeptusLog.pub().info("X: " + rev[0] + ", Y: " + rev[1] + ", Z: " + rev[2]);
        NeptusLog.pub().info("Lat: " + latLonDep2[0] + ", Lon: " + latLonDep2[1] + ", Dep: " + latLonDep2[2]);


        LocationType lt1 = new LocationType();
        lt1.setLatitudeDegs(41);
        lt1.setLongitudeDegs(-8);

        lt1.translatePosition(340, 234, 23);

        LocationType lt2 = new LocationType();
        lt2.setLatitudeDegs(42.655);
        lt2.setLongitudeDegs(-8.0012);

        lt2.translatePosition(23, 34, 435);

        lt2.setAzimuth(34);
        lt2.setOffsetDistance(456);

        double[] offs = lt2.getOffsetFrom(lt1);
        LocationType lt3 = new LocationType(lt1);
        lt3.translatePosition(offs[0], offs[1], offs[2]);

        NeptusLog.pub().info(lt3.getDistanceInMeters(lt2));

        // lt1.translatePosition(340, 45, -3);
        // lt1.translatePosition(-34, 450, 34);
        //
        //
        //
        // lt2.translatePosition(-34, 450, 34);
        //
        // LocationType lt3 = new LocationType();
        // lt3.setLatitude(41);
        // lt3.setLongitude(-8);
        //
        // lt3.translatePosition(340-34, 45+450, -3+34);
        //
        // System.err.println(lt3.getDistanceInMeters(lt2));
        //
        //
        /*
         * LocationType lt2 = new LocationType(); lt2.setLatitude(42.655); lt2.setLongitude(-8.0012); double offLong = lt2.getOffsetFrom(lt1)[1]; double offLat
         * = lt2.getOffsetFrom(lt1)[0]; double[] newCoords = latLonAddNE2(41, -8, 183749.21572828665, -2237.252129562752);
         * NeptusLog.pub().info("Offset lat: "+offLat+", Offset lon: "+offLong); lt2.setLatitude(newCoords[0]); lt2.setLongitude(newCoords[1]); offLong =
         * lt2.getOffsetFrom(lt1)[1]; offLat = lt2.getOffsetFrom(lt1)[0]; NeptusLog.pub().info("Offset lat: "+offLat+", Offset lon: "+offLong);
         * NeptusLog.pub().info("lat: "+newCoords[0]+", lon: "+newCoords[1]);
         */

        //NeptusLog.pub().info(latitudeAsPrettyString(39.543, false));
        NeptusLog.pub().info(latitudeAsPrettyString(39.543, LatLonFormatEnum.DM));

        double lat1 = 41.3456345678343434;
        String lat1Str = dmsToLatString(CoordinateUtil.decimalDegreesToDMS(lat1));
        double lat1M = CoordinateUtil.parseCoordString(lat1Str);
        NeptusLog.pub().info("--------------------------------------------------------");
        NeptusLog.pub().info(lat1);
        NeptusLog.pub().info(lat1Str);
        NeptusLog.pub().info(lat1M);
        //NeptusLog.pub().info(latitudeAsPrettyString(lat1, true));
        NeptusLog.pub().info(latitudeAsPrettyString(lat1, LatLonFormatEnum.DMS));

        NeptusLog.pub().info("_________________________________________________________");
        NeptusLog.pub().info(locC);

        NeptusLog.pub().info(CoordinateUtil.latitudeAsString(0.56, true));
        NeptusLog.pub().info(CoordinateUtil.latitudeAsString(1.06, true));
        NeptusLog.pub().info(CoordinateUtil.latitudeAsString(-0.56, true));
        NeptusLog.pub().info(CoordinateUtil.latitudeAsString(-1.06, true));

        NeptusLog.pub().info(CoordinateUtil.longitudeAsString(0.56, true));
        NeptusLog.pub().info(CoordinateUtil.longitudeAsString(1.06, true));
        NeptusLog.pub().info(CoordinateUtil.longitudeAsString(-0.56, true));
        NeptusLog.pub().info(CoordinateUtil.longitudeAsString(-1.06, true));

        NeptusLog.pub().info("_________________________________________________________");

        LocationType locA1 = new LocationType(0, 0);
        LocationType locA2 = new LocationType(0.000001, 0);
        NeptusLog.pub().info(locA1.getDistanceInMeters(locA2));

        NeptusLog.pub().info("_________________________________________________________");

        LocationType loc = new LocationType(41.73827393783, -9.783637266382);
        // NeptusLog.pub().info("latitudeAsPrettyString lat true " + latitudeAsPrettyString(loc.getLatitudeDegs(), true));
        // NeptusLog.pub().info("latitudeAsPrettyString lat false " + latitudeAsPrettyString(loc.getLatitudeDegs(), false));

        NeptusLog.pub().info("latitudeAsPrettyString lat DECIMAL_DEGREES " + latitudeAsPrettyString(loc.getLatitudeDegs(), LatLonFormatEnum.DECIMAL_DEGREES));
        NeptusLog.pub().info("latitudeAsPrettyString lat DM " + latitudeAsPrettyString(loc.getLatitudeDegs(), LatLonFormatEnum.DM));
        NeptusLog.pub().info("latitudeAsPrettyString lat DMS " + latitudeAsPrettyString(loc.getLatitudeDegs(), LatLonFormatEnum.DMS));

//        latitudeAsPrettyString(double latitude, boolean showSeconds)
//        latitudeAsString(latitude, !showSeconds, showSeconds ? 6 : 8);
//
//        longitudeAsPrettyString(double longitude, boolean showSeconds)
//        longitudeAsString(longitude, !showSeconds, showSeconds ? 6 : 8)

        NeptusLog.pub().info("-------------------------------------------------------");
        String lonMTestStr = dmsToLatLonString(new double[] { -9, 9, 0 }, false, 3);
        NeptusLog.pub().info(lonMTestStr + " == 9W9'0.000''  " + ("9W9'0.000''".equalsIgnoreCase(lonMTestStr)));
    }
}
