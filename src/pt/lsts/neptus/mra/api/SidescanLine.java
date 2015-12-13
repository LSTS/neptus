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
 * Author: José Correia
 * Oct 26, 2012
 */
package pt.lsts.neptus.mra.api;

import java.awt.image.BufferedImage;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 * @author pdias
 */
public class SidescanLine {
    /** The sonar data timestamp (milliseconds) */
    public long timestampMillis;

    /** The sonar data size (size of {@link #data}) */
    public int xsize;
    /** The sonar y data size (1 for normal and >1 for speed correction, which means line data extends more then one line) */
    public int ysize;

    /** The sonar y pos in relation to an external list (for the next line pos one can add to this the {@link #ysize}) */
    public int ypos;

    /** The sonar frequency */
    public float frequency;
    /** The sonar range */
    public float range;
    /** The state of the sensor */
    public SystemPositionAndAttitude state;

    /** The image created from data */
    public BufferedImage image;
    /** Holds information if the image has slant correction */
    public boolean imageWithSlantCorrection = false;
    /** The sonar data */
    public double data[];

    /**
     * Initializes the sidescan line
     * @param timestamp The timestamp.
     * @param range The range.
     * @param state the sensor state (see {@link SystemPositionAndAttitude}).
     * @param frequency The sonar frequency.
     * @param data The array with collected data.
     */
    public SidescanLine(long timestamp, float range, SystemPositionAndAttitude state, float frequency, double data[]) {
        super();
        this.timestampMillis = timestamp;
        this.xsize = data.length;
        this.range = range;
        this.state = state;
        this.data = data;
        this.frequency = frequency;
    }

    /**
     * Calculates the distance (horizontal (true) or slant (false)) from nadir.
     * @param x The sidescan x index (nadir is the half of total points {@link #xsize}).
     * @param slantCorrection Indicates if distance is horizontal (true) or slant (false).
     * @return Distance from nadir (negative means port-side).
     */
    public double getDistanceFromIndex(int x, boolean slantCorrection) {
        double distance = x * (range * 2 / xsize) - (range);
        if (slantCorrection) {
            double alt = state.getAltitude();
            alt = Math.max(alt, 0);
            double distanceG = Math.signum(distance) * Math.sqrt(distance * distance - alt * alt);
            distance = Double.isNaN(distanceG) ? 0 : distanceG;
        }
        return distance;
    }

    /**
     * Get the sidescan x from the distance in meters from nadir.
     * @param distance Distance from nadir (negative means port-side).
     * @param slantCorrection Indicates if distance is horizontal (true) or slant (false).
     * @return The sidescan x index (nadir is the half of total points {@link #xsize}).
     */
    public int getIndexFromDistance(double distance, boolean slantCorrection) {
        double r = distance;
        if (slantCorrection) {
            if (Double.isNaN(distance))
                return xsize / 2;
            double alt = state.getAltitude();
            double rG = Math.signum(distance) * Math.sqrt(distance * distance + alt * alt);
            r = rG;
        }
        int x =  (int) ((r + range) / (range * 2 / xsize));
        return x;
    }
    
    /**
     * Based on a 'x' position within a scan line calculate the proper location
     * @param x The sidescan x index (nadir is the half of total points {@link #xsize}).
     * @param slantCorrection Indicates if distance is horizontal (true) or slant (false).
     * @return a LocationType object containing the absolute GPS location of the 
     * point (wrapped into {@link SidescanPoint}.
     */
    public SidescanPoint calcPointFromIndex(int x, boolean slantCorrection) {
        LocationType location = new LocationType();
        // Set the System lat/lon as the center point
        location.setLatitudeStr(state.getPosition().getLatitudeStr());
        location.setLongitudeStr(state.getPosition().getLongitudeStr());
        
        double distance = getDistanceFromIndex(x, slantCorrection);
        
        double angle = -state.getYaw() + (x < (xsize / 2) ? Math.PI : 0);
        double offsetNorth = Math.abs(distance) * Math.sin(angle);
        double offsetEast = Math.abs(distance) * Math.cos(angle);
        // Add the original vehicle offset to the calculated offset
        location.setOffsetNorth(state.getPosition().getOffsetNorth() + offsetNorth);
        location.setOffsetEast(state.getPosition().getOffsetEast() + offsetEast);

//        System.out.printf("LineConv: ssX=%d  loc=%s    %s\n", x, location.getNewAbsoluteLatLonDepth(),
//                CoordinateUtil.latitudeAsString(location.getNewAbsoluteLatLonDepth().getLatitudeDegs(), true, 4) + " "
//                + CoordinateUtil.longitudeAsString(location.getNewAbsoluteLatLonDepth().getLongitudeDegs(), true, 4));
        
        // Return new absolute location        
        return new SidescanPoint(x, ypos, xsize, location.getNewAbsoluteLatLonDepth(), this);
    }
    
    public static void main(String[] args) {
        double rangeMax = 40;
        System.out.printf("rangeMax=%.6f (m)  \n", rangeMax);
        double h = 10;
        double r = -20;
        double d = Math.signum(r) * Math.sqrt(r * r - h * h);
        System.out.printf("h=%.6f (m)  r=%.6f (m signed) d=%.6f (m signed) \n", h, r, d);
        
        double pc = (rangeMax + r) / (rangeMax * 2);
        
        int sspoints = 2000; // xSize
        int xPoint = (int) (sspoints * pc); //500;
        double dssFromSSPoint = xPoint * (rangeMax * 2 / sspoints) - (rangeMax);
        double dssFromSSPointNoSlant = Math.signum(dssFromSSPoint) * Math.sqrt(dssFromSSPoint * dssFromSSPoint - h * h);
        System.out.printf("sspoints=%d (sspx)  xPoint=%d (sspx)\n", sspoints, xPoint);
        System.out.printf("dssFromSSPoint=%.6f (m signed)  dssFromSSPointNoSlant=%.6f (m signed)   result=%s %s\n", 
                dssFromSSPoint, dssFromSSPointNoSlant, 
                Double.compare(r, dssFromSSPoint) == 0 ? "OK" : "Wrong",
                Double.compare(d, dssFromSSPointNoSlant) == 0 ? "OK" : "Wrong");

        double hInSSPx = h * (sspoints / (rangeMax * 2));
        double dssFromSSPointSSPx = xPoint - sspoints / 2;
        double dssFromSSPointNoSlantSSPx = sspoints / 2 + Math.signum(dssFromSSPointSSPx) * Math.sqrt(dssFromSSPointSSPx * dssFromSSPointSSPx - hInSSPx * hInSSPx);
        System.out.printf("hInSSPx=%.6f (sspx)  dssFromSSPointSSPx=%.6f (sspx signed)  dssFromSSPointNoSlantSSPx=%.6f (sspx)   result=%s %s\n", 
                hInSSPx, dssFromSSPointSSPx, dssFromSSPointNoSlantSSPx,
                Double.compare(Math.round(r * sspoints/2 / rangeMax), Math.round(dssFromSSPointSSPx)) == 0 ? "OK" : "Wrong",
                Double.compare(Math.round(d * sspoints/2 / rangeMax + sspoints / 2), Math.round(dssFromSSPointNoSlantSSPx)) == 0 ? "OK" : "Wrong");
        
        int imgWidth = 1000;
        int ximg = (int) (imgWidth * pc); //250;
        System.out.printf("imgWidth=%d (px)  ximg=%d (px)\n", imgWidth, ximg);
        double hInImg = h * (imgWidth / (rangeMax * 2));
        double dInImg = imgWidth / 2 - ximg;
        double dInImgNoSlant;
        if (ximg < imgWidth / 2)
            dInImgNoSlant = imgWidth / 2 - Math.sqrt(dInImg * dInImg + hInImg * hInImg);
        else
            dInImgNoSlant = imgWidth / 2 + Math.sqrt(dInImg * dInImg + hInImg * hInImg);
        System.out.printf("hInImg=%.6f (px) dInImg=%.6f (px signed) dInImgNoSlant=%.6f (px)   result=%s %s\n", 
                hInImg, dInImg, dInImgNoSlant,
                Double.compare(Math.round(r * imgWidth/2 / rangeMax), Math.round(dInImg)) == 0 ? "OK" : "Wrong",
                Double.compare(Math.round(d * imgWidth/2 / rangeMax + imgWidth / 2), Math.round(dInImgNoSlant)) == 0 ? "OK" : "Wrong");
        dInImg = ximg - imgWidth / 2;
        dInImgNoSlant = imgWidth / 2 + Math.signum(dInImg) * Math.sqrt(dInImg * dInImg - hInImg * hInImg);
        System.out.printf("hInImg=%.6f (px) dInImg=%.6f (px signed) dInImgNoSlant=%.6f (px)   result=%s %s\n", 
                hInImg, dInImg, dInImgNoSlant,
                Double.compare(Math.round(r * imgWidth/2 / rangeMax), Math.round(dInImg)) == 0 ? "OK" : "Wrong",
                Double.compare(Math.round(d * imgWidth/2 / rangeMax + imgWidth / 2), Math.round(dInImgNoSlant)) == 0 ? "OK" : "Wrong");
        
        
        // Conv img px (slant correction) to sspx
        double d1 =  Math.signum(dInImgNoSlant - imgWidth / 2) * Math.sqrt(Math.pow(dInImgNoSlant - imgWidth / 2, 2) + hInImg * hInImg);
        double x1 = d1 + imgWidth / 2;
        double valCalcSSpx = x1 * sspoints / imgWidth;
        System.out.println(d1 + "   " + x1 + "   " + valCalcSSpx);
    }
}
