/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.vecmath.Matrix3d;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author Zé Pinto
 * @author Paulo Dias
 * @author Sergio Fraga
 * @author Rui Gomes
 */
public class CoordinateUtil {

    public static final char CHAR_DEGREE = '\u00B0'; // º Unicode

    public final static float MINUTE = 1 / 60.0f;
    public final static double MINUTE_D = 1 / 60.0d;
    public final static float SECOND = 1 / 3600.0f;
    public final static double SECOND_D = 1 / 3600.0d;

    public static final double c_wgs84_a = 6378137.0;
    public static final double c_wgs84_e2 = 0.00669437999013;
    public static final double c_wgs84_f = 0.0033528106647475;

    private final static String DELIM = "NnSsEeWwº'\": \t\n\r\f\u00B0";
    
    public final static NumberFormat heading3DigitsFormat = new DecimalFormat("000");


    /**
     * @param coord Is expeted to be in the form of:
     * 
     *            <pre>
     * "[NSEW]ddd mm' ss.sss''", "ddd[NSEW]mm ss.sss"
     * </pre>
     * 
     *            Examples: "N41 36' 3.333''", "41N36 3.333", "N41 36' 3,333''", "41N36.21"
     * @return Is null if some error occurs or an StringArray in the form of {"N", "0", "0", "0"} (all elements will allways exist).
     */
    public static String[] parseCoordToStringArray(String coord) {
        String[] result = { "N", "0", "0", "0" }; // new String[4];
        StringTokenizer strt;

        if (coord == null)
            return null;

        coord = coord.replace(',', '.');

        if (coord.toUpperCase().indexOf('N') != -1)
            result[0] = "N";
        else if (coord.toUpperCase().indexOf('S') != -1)
            result[0] = "S";
        else if (coord.toUpperCase().indexOf('E') != -1)
            result[0] = "E";
        else if (coord.toUpperCase().indexOf('W') != -1)
            result[0] = "W";
        else
            return null;

        strt = new StringTokenizer(coord, DELIM);
        // NeptusLog.pub().info(strt.countTokens());
        if ((strt.countTokens() < 1) | (strt.countTokens() > 3))
            return null;
        for (int i = 1; strt.hasMoreTokens(); i++) {
            // NeptusLog.pub().info(strt.nextToken());
            result[i] = strt.nextToken();
            // Tries to see if the value is a valid double number
            try {
                Double.parseDouble(result[i]);
            }
            catch (NumberFormatException e) {
                return null;
            }
        }

        return result;
    }

    /**
     * @see #parseCoordToStringArray(String)
     * @param coord
     * @return
     */
    public static String[] parseLatitudeCoordToStringArray(String coord) {
        String[] result = parseCoordToStringArray(coord);

        if (result == null)
            return null;

        else if (result[0].equalsIgnoreCase("N"))
            return result;
        else if (result[0].equalsIgnoreCase("S"))
            return result;

        return null;
    }

    /**
     * @see #parseCoordToStringArray(String)
     * @param coord
     * @return
     */
    public static String[] parseLongitudeCoordToStringArray(String coord) {
        String[] result = parseCoordToStringArray(coord);
        if (result == null)
            return null;
        else if (result[0].equalsIgnoreCase("E"))
            return result;
        else if (result[0].equalsIgnoreCase("W"))
            return result;

        return null;
    }

    /**
     * @see #parseCoordToStringArray(String)
     * @param coord
     * @return
     */
    public static double parseLatitudeCoordToDoubleValue(String coord) {
        
        try { return Double.parseDouble(coord); }
        catch (Exception e){ }
        
        double result = Double.NaN;
        String[] latStr = parseLatitudeCoordToStringArray(coord);
        if (latStr == null)
            return Double.NaN;

        double latD = Double.NaN;
        double latM = Double.NaN;
        double latS = Double.NaN;
        try {
            latD = Double.parseDouble(latStr[1]);
            latM = Double.parseDouble(latStr[2]);
            latS = Double.parseDouble(latStr[3]);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        double latDouble = latD;
        latDouble += latM * MINUTE_D;
        latDouble += latS * SECOND_D;

        if (!latStr[0].equalsIgnoreCase("N"))
            latDouble = -latDouble;
        while (latDouble > 90)
            latDouble -= 180d;
        while (latDouble < -90)
            latDouble += 180d;

        result = latDouble;
        return result;
    }

    /**
     * @see #parseCoordToStringArray(String)
     * @param coord
     * @return
     */
    public static double parseLongitudeCoordToDoubleValue(String coord) {
       
        try { return Double.parseDouble(coord); }
        catch (Exception e){ }
        
        double result = Double.NaN;
        String[] lonStr = parseLongitudeCoordToStringArray(coord);
        if (lonStr == null)
            return Double.NaN;

        double lonD = Double.NaN;
        double lonM = Double.NaN;
        double lonS = Double.NaN;
        try {
            lonD = Double.parseDouble(lonStr[1]);
            lonM = Double.parseDouble(lonStr[2]);
            lonS = Double.parseDouble(lonStr[3]);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }

        double lonDouble = lonD;
        lonDouble += lonM * MINUTE_D;
        lonDouble += lonS * SECOND_D;

        if (!lonStr[0].equalsIgnoreCase("E"))
            lonDouble = -lonDouble;
        while (lonDouble > 180)
            lonDouble -= 360d;
        while (lonDouble < -180)
            lonDouble += 360d;

        result = lonDouble;
        return result;
    }

    /**
     * 
     * @param latitude
     * @return
     */
    public static double[] parseLatitudeStringToDMS(String latitude) {
        double[] dms = new double[3];

        String[] latStr = parseLatitudeCoordToStringArray(latitude);
        // if (la)
        try {
            if (latStr[0].equalsIgnoreCase("S"))
                dms[0] = -Double.parseDouble(latStr[1]);
            else
                dms[0] = Double.parseDouble(latStr[1]);

            dms[1] = Double.parseDouble(latStr[2]);
            dms[2] = Double.parseDouble(latStr[3]);
        }
        catch (Exception e) {
            NeptusLog.pub().error("parseLatitudeStringToDMS(String latitude)", e);
        }

        return dms;
    }

    /**
     * 
     * @param latRads
     * @return
     */
    public static double[] parseLongitudeStringToDMS(String longitude) {
        double[] dms = new double[3];

        String[] lonStr = parseLongitudeCoordToStringArray(longitude);

        try {
            if (lonStr[0].equalsIgnoreCase("W"))
                dms[0] = -Double.parseDouble(lonStr[1]);
            else
                dms[0] = Double.parseDouble(lonStr[1]);

            dms[1] = Double.parseDouble(lonStr[2]);
            dms[2] = Double.parseDouble(lonStr[3]);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return dms;
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

        double res[] = new double[2];
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

        double remainder = decimalDegress - ((int) decimalDegress);

        dms[0] = (double) Math.floor((double) decimalDegress);

        dms[1] = (double) Math.floor((double) (remainder * 60.0d));

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
        double[] dms = new double[3];
        double[] dm = new double[2];

        dms = decimalDegreesToDMS(decimalDegress);
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
            maxDecimalHouses = 3;
        String l = "N";
        if (!isLat)
            l = "E";
        double d = dms[0];
        double m = dms[1];
        double s = 0d;
        if (!dmonly)
            s = dms[2];

        if ((d < 0 || "-0.0".equalsIgnoreCase("" + d)) && (d + m + s != 0)) {
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
            return nformat.format(d) + l;// +"0' 0''";
        }

        if (dmonly) {
            return new String((int) d + l + nformat.format(m));
        }

        if (hasFracPart(m)) {
            nformat.setMaximumFractionDigits(maxDecimalHouses); // 10
            return (int) Math.floor(d) + l + nformat.format(m);// +"' 0''";
        }

        nformat.setMaximumFractionDigits(maxDecimalHouses); // 8

        return new String((int) Math.floor(d) + l + (int) Math.floor(m) + "'" + nformat.format(s) + "''");
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
        return dmsToLatLonString(dms, true, -1);
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
        return dmToLatLonString(dm, true, -1);
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
        return dmsToLatLonString(dms, true, -1);
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
        return dmToLatLonString(dm, true, -1);
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
        return dmsToLatLonString(dms, false, -1);
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
        return dmToLatLonString(dm, false, -1);
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
        return dmsToLatLonString(dms, false, -1);
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
        return dmToLatLonString(dm, false, -1);
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

        double[] latLonDM = CoordinateUtil.decimalDegreesToDM(AngleCalc.nomalizeAngleDegrees180(latLonDegrees));
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

    
//    /**
//     * Converts a latitude string to double usage (string format to pass to the method): gg[N/S]mm.mmmm
//     * 
//     * @deprecated
//     */
//    private static double strtolat(String string) {
//        // double multiplier = 1;
//        String str = string.toUpperCase();
//        double lat = 0;
//        int index;
//        if (str.indexOf('S') == -1)
//            index = str.indexOf('N');
//        else
//            index = str.indexOf('S');
//        lat = new Double(str.substring(0, index)).doubleValue();
//        lat += new Double(str.substring(index + 1)).doubleValue() / 60;
//        if (str.indexOf('N') == -1)
//            lat = -lat;
//        while (lat > 90)
//            lat -= 180;
//        while (lat < -90)
//            lat += 180.0;
//        return lat;
//    }

//    /**
//     * Converts a latitude string to double usage (string format to pass to the method): gg[N/S]mm.mmmm
//     * 
//     * @deprecated
//     */
//    private static double strtolon(String string) {
//        // double multiplier = 1;
//        String str = string.toUpperCase();
//        double lon = 0;
//        int index;
//        str.toLowerCase();
//        if (str.indexOf('W') == -1)
//            index = str.indexOf('E');
//        else
//            index = str.indexOf('W');
//        lon = new Double(str.substring(0, index)).doubleValue();
//        lon += new Double(str.substring(index + 1)).doubleValue() / 60;
//        if (str.indexOf('E') == -1)
//            lon = -lon;
//
//        while (lon > 180)
//            lon -= 360;
//        while (lon < -180)
//            lon += 360;
//        return lon;
//    }

//    private static double[] LatLonOffset(double latitude, double longitude, double eastOffset, double northOffset) {
//        LocationType lt1 = new LocationType();
//        LocationType lt2 = new LocationType();
//
//        lt1.setLatitude(latitude);
//        lt1.setLongitude(longitude);
//
//        lt2.setLatitude(latitude + 1.0);
//        lt2.setLongitude(longitude);
//
//        double metersPerLatitudeDegree = lt2.getDistanceInMeters(lt1);
//        // System.err.println("LT1: "+lt1.getDebugString());
//        // System.err.println("LT2: "+lt2.getDebugString());
//
//        // System.err.println("metersPerLatitudeDegree ="+metersPerLatitudeDegree);
//        lt2 = new LocationType();
//        lt2.setLatitude(latitude);
//        lt2.setLongitude(longitude + 1);
//
//        double metersPerLongitudeDegree = lt2.getDistanceInMeters(lt1);
//        // NeptusLog.pub().info("metersPerLongitudeDegree="+metersPerLongitudeDegree);
//        double lat = latitude + northOffset / metersPerLatitudeDegree;
//        double lon = longitude + eastOffset / metersPerLongitudeDegree;
//
//        // NeptusLog.pub().info("Result: "+lat+", "+lon);
//
//        return new double[] { lat, lon };
//    }

    /**
     * Add an offset in meters(north, east) to a (lat,lon) in decimal degrees
     */
    public static double[] latLonAddNE2(double lat, double lon, double north, double east) {
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(lat);
        loc.setLongitudeDegs(lon);
        return WGS84displace(lat, lon, 0, north, east, 0);
        
//        final double meterToFeet = 3.2808399;
//
//        if (north == 0 && east == 0)
//            return new double[] {lat, lon};
//        
//        GISCoordinate coord = new GISCoordinate(lat, lon, false);
//        try {
//            double angRad = Math.atan2(east, north);
//            double dist = Math.sqrt(north * north + east * east);
//
//            coord.move(dist * meterToFeet, Math.toDegrees(angRad), GISCoordinate.WGS84);
//
//            double rest[] = CoordinateUtil.latLonDiff(lat, lon, coord.getLatInDecDeg(), coord.getLonInDecDeg());
//            rest[0] = 0;
//            rest[1] = -rest[1] + east;
//            angRad = Math.atan2(rest[1], rest[0]);
//            dist = Math.sqrt(rest[0] * rest[0] + rest[1] * rest[1]);
//            coord.move(dist * meterToFeet, Math.toDegrees(angRad), GISCoordinate.WGS84);
//
//            rest = CoordinateUtil.latLonDiff(lat, lon, coord.getLatInDecDeg(), coord.getLonInDecDeg());
//            rest[0] = -rest[0] + north;
//            rest[1] = 0;
//            angRad = Math.atan2(rest[1], rest[0]);
//            dist = Math.sqrt(rest[0] * rest[0] + rest[1] * rest[1]);
//            coord.move(dist * meterToFeet, Math.toDegrees(angRad), GISCoordinate.WGS84);
//        }
//        catch (Exception e) {
//            NeptusLog.pub().error("latLonAddNE()", e);
//        }
//
//        return new double[] { coord.getLatInDecDeg(), coord.getLonInDecDeg() };
    }

//    /**
//     * Add an offset in meters(north, east) to a (lat,lon) in decimal degrees
//     * 
//     * @deprecated
//     */
//    private static double[] latLonAddNE(double lat, double lon, double north, double east) {
//        /*
//         * //double newOffset[] = normalizeOffsetToLocation(new double[] {north, east, 0}, new double[] {lat, lon, 0}); double absXYZ[] =
//         * latLonDepthToGeocentricXYZ(lat, lon, 0); absXYZ[0] += north; absXYZ[1] += east; //absXYZ[2] += newOffset[2]; // Só para ficar bonito :P
//         * 
//         * return geocentricXYZToLatLonDepth(absXYZ);
//         */
//
//        double lat_rad = lat * Math.PI / 180;
//        lat += north / (111132.92 - 559.82 * Math.cos(2 * lat_rad) + 1.175 * Math.cos(4 * lat_rad));
//        lon += east / (111412.84 * Math.cos(lat_rad) - 93.5 * Math.cos(3 * lat_rad));
//
//        while (lon > 180)
//            lon -= 360;
//        while (lon < -180)
//            lon += 360;
//        while (lat > 90)
//            lat -= 180;
//        while (lat < -90)
//            lat += 180.0;
//
//        double[] latLon = new double[2];
//        latLon[0] = lat;
//        latLon[1] = lon;
//        return latLon;
//
//    }

//    /**
//     * Computes the offset (north, east) in meters from (lat, lon) to (alat, alon) [both of these in decimal degrees].<br>
//     * Subtract the two latlons and come up with the distance in meters N/S and E/W between them.
//     * 
//     * @deprecated
//     */
//    private static double[] latLonDiff_(double lat, double lon, double alat, double alon) {
//        /*
//         * double abs1[] = latLonDepthToGeocentricXYZ(lat, lon, 0); double abs2[] = latLonDepthToGeocentricXYZ(alat, alon, 0);
//         * 
//         * return new double[] {abs1[0]-abs2[0], abs1[1]-abs2[1], 0};
//         */
//
//        double lat_rad = lat * Math.PI / 180;
//        double n = (alat - lat) * (111132.92 - 559.82 * Math.cos(2 * lat_rad) + 1.175 * Math.cos(4 * lat_rad));
//        double e = (alon - lon) * (111412.84 * Math.cos(lat_rad) - 93.5 * Math.cos(3 * lat_rad));
//        // NeptusLog.pub().info("n/e: " + n + " ;" + e);
//        double[] NE = new double[2];
//        NE[0] = n;
//        NE[1] = e;
//        return NE;
//    }

    /**
     * Computes the offset (north, east) in meters from (lat, lon) to (alat, alon) [both of these in decimal degrees].<br>
     * Subtract the two latlons and come up with the distance in meters N/S and E/W between them.
     */
    public static double[] latLonDiff(double lat, double lon, double alat, double alon) {
        
        double[] ret = WGS84displacement(lat, lon, 0, alat, alon, 0);
        return new double[] { ret[0], ret[1] };

//        if (lat == alat && lon == alon)
//            return new double[] { 0, 0 };
//
//        UTMCoordinates coords1 = new UTMCoordinates(lat, lon);
//        UTMCoordinates coords2 = new UTMCoordinates(alat, alon);
//
//        double diff[] = new double[2];
//
//        diff[0] = coords2.getNorthing() - coords1.getNorthing();
//        diff[1] = coords2.getEasting() - coords1.getEasting();
//        return diff;
    }

//    /**
//     * Transforms a spherical location to the respective cartesian one.
//     * 
//     * @param coord Is expeted to be in the form of: Examples: Input-> "41N3.6117" "8W27.4009" 10 Output: (0,0,10) "N41 36' 3.333''", "41N36 3.333",
//     *            "N41 36' 3,333''", "41N36.21"
//     * @return Is null if some error occurs or an StringArray in the form of {"N", "0", "0", "0"} (all elements will allways exist).
//     * @see #parseCoordToStringArray(String)
//     */
//    private static double[] latLonHeightCoordToXYZCoord(String lat, String lon, double height) {
//        // TODO NOT FINISHED!!!!!!!!!!!!!!!!!!!
//        Double latVal = parseLatitudeCoordToDouble(lat);
//        Double lonVal = parseLongitudeCoordToDouble(lon);
//
//        if ((latVal == null) || (lonVal == null))
//            return null;
//
//        double[] xYCoordinates = CoordinateUtil.latLonAddNE(latVal.doubleValue(), lonVal.doubleValue(), 0.0, 0.0);
//
//        double[] latLonHeight = new double[3];
//        latLonHeight[0] = xYCoordinates[0];
//        latLonHeight[1] = xYCoordinates[1];
//        latLonHeight[2] = height;
//
//        return latLonHeight;
//    }

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

//    /**
//     * function [dphi,dtheta,dpsi]=inertial_eq(p,q,r,phi,theta,psi) ctheta = cos(theta); stheta = sin(theta); cphi = cos(phi); sphi = sin(phi);
//     * 
//     * t4 = stheta*sphi; t9 = stheta*cphi; t28 = 1/ctheta;
//     * 
//     * dphi = (p*ctheta+t4*q+t9*r)*t28; dtheta = cphi*q-sphi*r; dpsi = (sphi*q+cphi*r)*t28;
//     * 
//     * @param p
//     * @param q
//     * @param r
//     * @param phi
//     * @param theta
//     * @param psi
//     * @return
//     */
//    private static double[] pqrToInertialFrame(double p, double q, double r, double phi, double theta, double psi) {
//        double[] result = { 0.0, 0.0, 0.0 };
//
//        double ctheta = Math.cos(theta);
//        double stheta = Math.sin(theta);
//        double cphi = Math.cos(phi);
//        double sphi = Math.sin(phi);
//
//        double t4 = stheta * sphi;
//        double t9 = stheta * cphi;
//        double t28 = 1 / ctheta;
//
//        double dphi = (p * ctheta + t4 * q + t9 * r) * t28;
//        double dtheta = cphi * q - sphi * r;
//        double dpsi = (sphi * q + cphi * r) * t28;
//
//        result[0] = dphi;
//        result[1] = dtheta;
//        result[2] = dpsi;
//        return result;
//    }
//
//    /**
//     * 
//     * function [dx,dy,dz]=inertial_eq(u,v,w,phi,theta,psi)
//     * 
//     * cpsi = cos(psi); spsi = sin(psi); ctheta = cos(theta); stheta = sin(theta); cphi = cos(phi); sphi = sin(phi);
//     * 
//     * t3 = v*cpsi; t4 = stheta*sphi; t6 = v*spsi; t8 = w*cpsi; t9 = stheta*cphi; t11 = w*spsi;
//     * 
//     * dx = cpsi*ctheta*u+t3*t4-t6*cphi+t8*t9+t11*sphi; dy = spsi*ctheta*u+t6*t4+t3*cphi+t11*t9-t8*sphi; dz = -stheta*u+ctheta*sphi*v+ctheta*cphi*w;
//     * 
//     * @param u
//     * @param v
//     * @param w
//     * @param phi
//     * @param theta
//     * @param psi
//     * @return
//     */
//    private static double[] uvwToInertialFrame(double u, double v, double w, double phi, double theta, double psi) {
//        double[] result = { 0.0, 0.0, 0.0 };
//
//        double cpsi = Math.cos(psi);
//        double spsi = Math.sin(psi);
//        double ctheta = Math.cos(theta);
//        double stheta = Math.sin(theta);
//        double cphi = Math.cos(phi);
//        double sphi = Math.sin(phi);
//
//        double t3 = v * cpsi;
//        double t4 = stheta * sphi;
//        double t6 = v * spsi;
//        double t8 = w * cpsi;
//        double t9 = stheta * cphi;
//        double t11 = w * spsi;
//
//        double dx = cpsi * ctheta * u + t3 * t4 - t6 * cphi + t8 * t9 + t11 * sphi;
//        double dy = spsi * ctheta * u + t6 * t4 + t3 * cphi + t11 * t9 - t8 * sphi;
//        double dz = -stheta * u + ctheta * sphi * v + ctheta * cphi * w;
//
//        result[0] = dx;
//        result[1] = dy;
//        result[2] = dz;
//        return result;
//    }

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
        // double[] sph = new double[] {r, theta, phi};
        // double[] rec = new double[] {x, y, z};
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

        double xyz[] = new double[3];

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

        double ret[] = new double[3];
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

    protected static double[] normalizeOffsetToLocation(double nedOffset[], double LatLonDepth[]) {

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

    public static String latitudeAsString(double latitude) {
        return latitudeAsString(latitude, true);
    }

    public static String longitudeAsString(double longitude) {
        return longitudeAsString(longitude, true);
    }

    public static String latitudeAsPrettyString(double latitude, boolean showSeconds) {
        return latitudeAsString(latitude, !showSeconds, showSeconds ? 6 : 8);
    }

    public static String longitudeAsPrettyString(double longitude, boolean showSeconds) {
        return longitudeAsString(longitude, !showSeconds, showSeconds ? 6 : 8);
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
        ClipboardOwner owner = new ClipboardOwner() {
            public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable contents) {
            }
        };
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

        double lld[] = {latDegrees, lonDegrees, depth};

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

        double cs1[];
        double cs2[];

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
        double xyz[] = toECEF(latDegrees, lonDegrees, depth);
        double lld[] = {latDegrees, lonDegrees, depth };
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
//        LocationType loc2 = new LocationType();
//        loc2.setLatitude(lld[0]);
//        loc2.setLongitude(lld[1]);
//        loc2.setDepth(lld[2]);
//        loc.setLocation(loc2);
        
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
        double bearing = 0, range = 0;
        double n, e;
        double[] ne = WGS84displacement(loc1, loc2);
        n = ne[0];
        e = ne[1];
        bearing = Math.atan2(e, n);
        range = Math.sqrt(n * n + e * e);
        return new double[] { Math.toDegrees(bearing), range };
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
        String te = "41N3.6117";
        String[] st = CoordinateUtil.parseLatitudeCoordToStringArray(te);
        NeptusLog.pub().info(st[1] + st[0] + st[2] + " " + st[3]);
        NeptusLog.pub().info(CoordinateUtil.parseLatitudeCoordToDoubleValue(te));
        // NeptusLog.pub().info(CoordinateUtil.strtolat(te));

        te = "8W27.4009";
        st = CoordinateUtil.parseLongitudeCoordToStringArray(te);
        NeptusLog.pub().info(st[1] + st[0] + st[2] + " " + st[3]);
        NeptusLog.pub().info(CoordinateUtil.parseLongitudeCoordToDoubleValue(te));
//        NeptusLog.pub().info(CoordinateUtil.strtolon(te));

        NeptusLog.pub().info("\nVandalizado por RG em 20/1/2005");
        NeptusLog.pub().info("rotation of pi/2 of point (1,1,1). The result should be (-1,1,1)");
        double[] teste = CoordinateUtil.bodyFrameToInertialFrame(1, 1, 1, 0, 0, Math.PI / 2);
        NeptusLog.pub().info(teste[0] + " " + teste[1] + " " + teste[2]);

        NeptusLog.pub().info("rotation of -pi/2 of point (-1,1,1). The result should be initial (1,1,1)");
        teste = CoordinateUtil.inertialFrameToBodyFrame(-1, 1, 1, 0, 0, Math.PI / 2);
        NeptusLog.pub().info(teste[0] + " " + teste[1] + " " + teste[2]);

        NeptusLog.pub().info(">>> Test latLonDepthToGeocentricXYZ and geocentricXYZToLatLonDepth:");
        double latLonDep[] = new double[] { 41.3433, -8.2334, 100 };
        double rev[] = latLonDepthToGeocentricXYZ(latLonDep[0], latLonDep[1], latLonDep[2]);
        double latLonDep2[] = geocentricXYZToLatLonDepth(rev);
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

        double offs[] = lt2.getOffsetFrom(lt1);
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
        NeptusLog.pub().info(latitudeAsPrettyString(39.543, false));

        double lat1 = 41.3456345678343434;
        String lat1Str = dmsToLatString(CoordinateUtil.decimalDegreesToDMS(lat1));
        double lat1M = CoordinateUtil.parseLatitudeCoordToDoubleValue(lat1Str);
        NeptusLog.pub().info("--------------------------------------------------------");
        NeptusLog.pub().info(lat1);
        NeptusLog.pub().info(lat1Str);
        NeptusLog.pub().info(lat1M);
        NeptusLog.pub().info(latitudeAsPrettyString(lat1, true));
        
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
    }
}
