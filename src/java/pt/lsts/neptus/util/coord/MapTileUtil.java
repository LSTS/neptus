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
 * 30 de Set de 2011
 */
package pt.lsts.neptus.util.coord;

import java.awt.geom.Point2D;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * See http://msdn.microsoft.com/en-us/library/bb259689.aspx
 * 
 * Copyright (c) 2006-2009 Microsoft Corporation. All rights reserved.
 */
public class MapTileUtil {

    public static final int LEVEL_OFFSET = 22;
    public static final int LEVEL_MIN = 1;
    public static final int LEVEL_MAX = 22;

    public static final double EARTH_RADIUS = 6378137;
    public static final double MIN_LATITUDE = -85.05112878;
    public static final double MAX_LATITUDE = 85.05112878;
    public static final double MIN_LONGITUDE = -180;
    public static final double MAX_LONGITUDE = 180;

    public static final boolean USE_CLIPPING = true;
    
    /**
     * Clips a number to the specified minimum and maximum values.
     * 
     * @param n The number to clip.
     * @param minValue Minimum allowable value.
     * @param maxValue Maximum allowable value.
     * @return The clipped value.
     */
    private static double clip(double n, double minValue, double maxValue) {
        if (USE_CLIPPING)
            return Math.min(Math.max(n, minValue), maxValue);
        else
            return n;
    }

    /**
     * Determines the map width and height (in pixels) at a specified level of detail.
     * 
     * @param levelOfDetail Level of detail, from {@value #LEVEL_MIN} (lowest detail) to {@value #LEVEL_MAX} (highest detail).
     * @return The map width and height in pixels.
     */
    public static int mapSize(int levelOfDetail) {
        return (int) 256 << levelOfDetail;
    }

    /**
     * Determines the ground resolution (in meters per pixel) at a specified latitude and level of detail.
     * 
     * @param latitude Latitude (in degrees) at which to measure the ground resolution.
     * @param levelOfDetail Level of detail, from 1 (lowest detail) to 23 (highest detail).
     * @return The ground resolution, in meters per pixel.
     */
    public static double groundResolution(double latitude, int levelOfDetail) {
        latitude = clip(latitude, MIN_LATITUDE, MAX_LATITUDE);
        return Math.cos(latitude * Math.PI / 180) * 2 * Math.PI * EARTH_RADIUS / mapSize(levelOfDetail);
    }

    /**
     * Determines the map scale at a specified latitude, level of detail, and screen resolution.
     * 
     * @param latitude Latitude (in degrees) at which to measure the map scale.
     * @param levelOfDetail Level of detail, from 1 (lowest detail) to 23 (highest detail).
     * @param screenDpi Resolution of the screen, in dots per inch.
     * @return The map scale, expressed as the denominator N of the ratio 1 : N.
     */
    public static double mapScale(double latitude, int levelOfDetail, int screenDpi) {
        return groundResolution(latitude, levelOfDetail) * screenDpi / 0.0254;
    }

    /**
     * Converts a point from latitude/longitude WGS-84 coordinates (in degrees) into pixel XY coordinates at a specified level of detail.
     * 
     * @param latitude Latitude of the point, in degrees.
     * @param longitude Longitude of the point, in degrees.
     * @param levelOfDetail Level of detail, from 1 (lowest detail) to 23 (highest detail).
     * @return XY coordinates.
     */
    public static Point2D degreesToXY(double latitude, double longitude, int levelOfDetail) {
        latitude = clip(latitude, MIN_LATITUDE, MAX_LATITUDE);
        longitude = clip(longitude, MIN_LONGITUDE, MAX_LONGITUDE);

        double x = (longitude + 180) / 360;
        double sinLatitude = Math.sin(latitude * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        int mapSize = mapSize(levelOfDetail);
        double pixelX = clip(x * mapSize + 0.5, 0, mapSize - 1);
        double pixelY = clip(y * mapSize + 0.5, 0, mapSize - 1);
        return new Point2D.Double(pixelX, pixelY);
    }

    /**
     * Converts a pixel from pixel XY coordinates at a specified level of detail into latitude/longitude WGS-84 coordinates (in degrees).
     * 
     * @param pixelX X coordinate of the point, in pixels.
     * @param pixelY Y coordinates of the point, in pixels.
     * @param levelOfDetail Level of detail, from 1 (lowest detail) to 23 (highest detail).
     * @return Lat Lon in degrees.
     */
    public static double[] xyToDegrees(double pixelX, double pixelY, int levelOfDetail) {
        int mapSize = mapSize(levelOfDetail);
        double x = (clip(pixelX, 0, mapSize - 1) / mapSize) - 0.5;
        double y = 0.5 - (clip(pixelY, 0, mapSize - 1) / mapSize);

        double latitude = 90 - 360 * Math.atan(Math.exp(-y * 2 * Math.PI)) / Math.PI;
        double longitude = 360 * x;
        return new double[] { latitude, longitude };
    }

    /**
     * Converts pixel XY coordinates into tile XY coordinates of the tile containing the specified pixel.
     * 
     * @param pixelX Pixel X coordinate.
     * @param pixelY Pixel Y coordinate.
     * @return tileX: Output parameter receiving the tile X coordinate. tileY: Output parameter receiving the tile Y coordinate.
     */
    public static int[] pixelXYToTileXY(int pixelX, int pixelY) {// TODO change to double ?
        int tileX, tileY;
        tileX = pixelX / 256;
        tileY = pixelY / 256;
        return new int[] { tileX, tileY };
    }

    /**
     * Converts tile XY coordinates into pixel XY coordinates of the upper-left pixel of the specified tile.
     * 
     * @param tileX Tile X coordinate.
     * @param tileY Tile Y coordinate.
     * @return pixelX: Output parameter receiving the pixel X coordinate. pixelY: Output parameter receiving the pixel Y coordinate.
     */
    public static int[] tileXYToPixelXY(int tileX, int tileY) {// TODO change to double ?
        int pixelX, pixelY;
        pixelX = tileX * 256;
        pixelY = tileY * 256;
        return new int[] { pixelX, pixelY };
    }

    /**
     * Converts tile XY coordinates into a QuadKey at a specified level of detail.
     * 
     * @param tileX Tile X coordinate.
     * @param tileY Tile Y coordinate.
     * @param levelOfDetail Level of detail, from 1 (lowest detail) to 23 (highest detail).
     * @return A string containing the QuadKey.
     */
    public static String tileXYToQuadKey(int tileX, int tileY, int levelOfDetail) {// TODO change to double ?
        StringBuilder quadKey = new StringBuilder();
        for (int i = levelOfDetail; i > 0; i--) {
            char digit = '0';
            int mask = 1 << (i - 1);
            if ((tileX & mask) != 0) {
                digit++;
            }
            if ((tileY & mask) != 0) {
                digit++;
                digit++;
            }
            quadKey.append(digit);
        }
        return quadKey.toString();
    }

    /**
     * Converts a QuadKey into tile XY coordinates.
     * 
     * @param quadKey QuadKey of the tile.
     * @return tileX: Output parameter receiving the tile X coordinate. tileY: Output parameter receiving the tile Y coordinate. levelOfDetail: Output parameter
     *         receiving the level of detail.
     * @throws Exception
     */
    public static int[] quadKeyToTileXY(String quadKey) throws Exception {// TODO change to double ?
        int tileX, tileY, levelOfDetail;
        tileX = tileY = 0;
        levelOfDetail = quadKey.length();
        for (int i = levelOfDetail; i > 0; i--) {
            int mask = 1 << (i - 1);
            String str = quadKey.substring(levelOfDetail - i, levelOfDetail - i + 1);
            if ("0".equalsIgnoreCase(str)) {
            }
            else if ("1".equalsIgnoreCase(str)) {
                tileX |= mask;
            }
            else if ("2".equalsIgnoreCase(str)) {
                tileY |= mask;
            }
            else if ("3".equalsIgnoreCase(str)) {
                tileX |= mask;
                tileY |= mask;
            }
            else
                throw new Exception("Invalid QuadKey digit sequence.");

        }
        return new int[] { tileX, tileY, levelOfDetail };
    }

    // ------------------- Added methods ------------------- //

    /**
     * This will return an north and east offsets using Mercator projection at levelOfDetail={@value #LEVEL_OFFSET}. Calls
     * {@link #getOffsetFrom(double, double, double, double, int)}.
     * 
     * @param latitudeRef
     * @param longitudeRef
     * @param latitude
     * @param longitude
     * @return
     */
    public static double[] getOffsetFrom(double latitudeRef, double longitudeRef, double latitude, double longitude) {
        return getOffsetFrom(latitudeRef, longitudeRef, latitude, longitude, LEVEL_OFFSET);
    }

    /**
     * This will return an north and east offsets using Mercator projection in an specific levelOfDetail.
     * 
     * @param latitudeRef
     * @param longitudeRef
     * @param latitude
     * @param longitude
     * @param levelOfDetail
     * @return
     */
    public static double[] getOffsetFrom(double latitudeRef, double longitudeRef, double latitude, double longitude, int levelOfDetail) {
        double pixelX, pixelY;
        latitude = clip(latitude, MIN_LATITUDE, MAX_LATITUDE);
        longitude = clip(longitude, MIN_LONGITUDE, MAX_LONGITUDE);

        double x = (longitude + 180) / 360;
        double sinLatitude = Math.sin(latitude * Math.PI / 180);
        double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        int mapSize = mapSize(levelOfDetail);
        pixelX = clip(x * mapSize + 0.5, 0, mapSize - 1);
        pixelY = clip(y * mapSize + 0.5, 0, mapSize - 1);
        double gRes = groundResolution(latitudeRef, levelOfDetail);
        double[] locD = new double[] { pixelX * gRes, pixelY * gRes };

        latitude = clip(latitudeRef, MIN_LATITUDE, MAX_LATITUDE);
        longitude = clip(longitudeRef, MIN_LONGITUDE, MAX_LONGITUDE);

        x = (longitude + 180) / 360;
        sinLatitude = Math.sin(latitude * Math.PI / 180);
        y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

        mapSize = mapSize(levelOfDetail);
        pixelX = clip(x * mapSize + 0.5, 0, mapSize - 1);
        pixelY = clip(y * mapSize + 0.5, 0, mapSize - 1);
        double[] locDC = new double[] { pixelX * gRes, pixelY * gRes };
        return new double[] { -(locD[1] - locDC[1]), locD[0] - locDC[0] };
    }

    /**
     * Gets offset in pixels with given level
     * 
     * @param start
     * @param end
     * @param level
     * @return
     */
    public static double[] getOffsetInPixels(LocationType start, LocationType end, int level) {
        level = (int) clip(level, LEVEL_MIN, LEVEL_MAX);
        
        Point2D pointStart = start.getPointInPixel(level);
        Point2D pointEnd = end.getPointInPixel(level);
        
        return new double[] { pointEnd.getX() - pointStart.getX(), pointEnd.getY() - pointStart.getY() };
    }

    /**
     * Gets offset in pixels with default level for creating shape only !!!!
     * 
     * @param start
     * @param end
     * @return
     */
    public static double[] getOffsetInPixels(LocationType start, LocationType end) {
        LocationType startWithOffset = start.getNewAbsoluteLatLonDepth();
        LocationType endWithOffset = end.getNewAbsoluteLatLonDepth();
        return getOffsetInPixels(startWithOffset, endWithOffset, LEVEL_OFFSET);
    }
    
    /**
     * Distance in meters between two locations using 6371 as average earth radius in meters
     * Harversine formula
     * {@link http://en.wikipedia.org/wiki/Haversine_formula#Java}
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    double getDistanceHaversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * 
           Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 6371) * 1000.0; // 6371 = average earth radius
    }
    

    public static void main(String[] args) throws Exception {
        NeptusLog.pub().info("<###> "+tileXYToQuadKey(3, 5, 3));
        int[] tlxy = quadKeyToTileXY("213");
        NeptusLog.pub().info("<###> "+tlxy[0] + "  " + tlxy[1]);
        int[] pxy = tileXYToPixelXY(3, 5);
        NeptusLog.pub().info("<###> "+pxy[0] + "  " + pxy[1]);
        NeptusLog.pub().info("<###> "+(3 * 256) + "  " + (5 * 256));

        LocationType loc1 = new LocationType();
        loc1.setLatitudeStr("41N10.6938");
        loc1.setLongitudeStr("8W42.5051");
        NeptusLog.pub().info("<###>{" + loc1.getLatitudeDegs() + "\u00B0, " + loc1.getLongitudeDegs() + "\u00B0, " + loc1.getDepth() + "]");

        LocationType loc2 = new LocationType();
        loc2.setLatitudeStr("44N40.7312");
        loc2.setLongitudeStr("63W32.2072");
        NeptusLog.pub().info("<###>{" + loc2.getLatitudeDegs() + "\u00B0, " + loc2.getLongitudeDegs() + "\u00B0, " + loc1.getDepth() + "]");

        double[] diff1 = getOffsetFrom(loc1.getLatitudeDegs(), loc1.getLongitudeDegs(), loc2.getLatitudeDegs(),
                loc2.getLongitudeDegs());
        NeptusLog.pub().info("<###>[" + diff1[0] + ", " + diff1[1] + "]");
        
        NeptusLog.pub().info("<###>\n--------------------------------------------------------");
        
        LocationType locS1 = new LocationType(loc1);
        NeptusLog.pub().info("<###> "+locS1);
        Point2D ptS1 = locS1.getPointInPixel(22);
        double[] kS1 = MapTileUtil.xyToDegrees(ptS1.getX(), ptS1.getY(), 22);
        LocationType locS2 = new LocationType();
        locS2.setLatitudeDegs(kS1[0]);
        locS2.setLongitudeDegs(kS1[1]);
        NeptusLog.pub().info("<###> "+locS2);

        kS1 = MapTileUtil.xyToDegrees((int)ptS1.getX(), (int)ptS1.getY(), 22);
        LocationType locS3 = new LocationType();
        locS3.setLatitudeDegs(kS1[0]);
        locS3.setLongitudeDegs(kS1[1]);
        NeptusLog.pub().info("<###> "+locS3);

    }
}
