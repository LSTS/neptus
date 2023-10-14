/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 15/12/2015
 */
package pt.lsts.neptus.mra.api;

import java.awt.image.BufferedImage;

import com.google.common.primitives.UnsignedLong;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * This class holds utility methods for sidescan
 * 
 * @author Paulo Dias
 *
 */
public class SidescanUtil {

    /** Avoid instantiation */
    private SidescanUtil() {
    }

    /**
     * Method to convert from sidescan x point to mouse click x point in the image.
     * 
     * @param sidescanLineX The x index of the sidescan (middle is half of the data size)
     * @param sidescanLine The sidescan line
     * @param image The full image for sidescan line as painted in the viewer.
     * @return
     */
    public static int convertSidescanLinePointXToImagePointX(int sidescanLineX, SidescanLine sidescanLine,
            BufferedImage image) {
        int sidescanLineXSize = sidescanLine.getXSize();
        if (!sidescanLine.isImageWithSlantCorrection()) {
            return (int) (sidescanLineX / (sidescanLineXSize / (float) image.getWidth()));
        }
        else {
            int imgWidth = image.getWidth();
            int sspoints = sidescanLine.getData().length;
            double ximg = sidescanLineX * imgWidth / sspoints;
            double hInImg = sidescanLine.getState().getAltitude() * (imgWidth / (sidescanLine.getRange() * 2));
            double dInImg = imgWidth / 2 - ximg;
            double d = imgWidth / 2 - Math.signum(dInImg) * Math.sqrt(dInImg * dInImg - hInImg * hInImg);
            return (int) d;
        }
    }

    /**
     * Method to convert from mouse click x point in the image to sidescan x point.
     * 
     * @param imageMouseX The image x index from image
     * @param sidescanLine The sidescan line
     * @param image The full image for sidescan line as painted in the viewer.
     * @return
     */
    public static int convertImagePointXToSidescanLinePointX(int imageMouseX, SidescanLine sidescanLine,
            BufferedImage image) {
        int sidescanLineXSize = sidescanLine.getXSize();
        if (!sidescanLine.isImageWithSlantCorrection()) {
            return (int) (imageMouseX * (sidescanLineXSize / (float) image.getWidth()));
        }
        else {
            int imgWidth = image.getWidth();
            int sspoints = sidescanLine.getData().length;
            double hInImg = sidescanLine.getState().getAltitude() * (imgWidth / (sidescanLine.getRange() * 2));
            double d1 = Math.signum(imageMouseX - imgWidth / 2)
                    * Math.sqrt(Math.pow(imageMouseX - imgWidth / 2, 2) + hInImg * hInImg);
            double x1 = d1 + imgWidth / 2;
            double valCalcSSpx = x1 * sspoints / imgWidth;
            return (int) valCalcSSpx;
        }
    }

    /**
     * Method to convert from mouse click x point in the image to sidescan x point.
     * 
     * @param imageMouseX The image x index from image
     * @param sidescanLine The sidescan line
     * @param image The full image for sidescan line as painted in the viewer.
     * @return
     */
    public static SidescanPoint convertImagePointXToSidescanPoint(int imageMouseX, SidescanLine sidescanLine,
            boolean slantRangeCorrection, BufferedImage image) {
        return sidescanLine.calcPointFromIndex(convertImagePointXToSidescanLinePointX(imageMouseX, sidescanLine, image),
                slantRangeCorrection);
    }

    /**
     * Method to convert from mouse click x point in the image to sidescan x point.
     * 
     * @param imageMouseX The image x index from image
     * @param sidescanLine The sidescan line
     * @param slantRangeCorrection To overwrite what is on sidescanLine
     * @param image The full image for sidescan line as painted in the viewer.
     * @return
     */
    public static LocationType convertImagePointXToLocation(int imageMouseX, SidescanLine sidescanLine,
            boolean slantRangeCorrection, BufferedImage image) {
        return convertImagePointXToSidescanPoint(imageMouseX, sidescanLine, slantRangeCorrection, image).location;
    }

    /**
     * Calculates the horizontal distance from two x indexes ({@link SidescanLine#getData()}) of two
     * {@link SidescanLine}s.
     * 
     * @param xIndexLine1
     * @param line1
     * @param xIndexLine2
     * @param line2
     * @return
     */
    public static double calcHorizontalDistanceFrom2XIndexesOf2SidescanLines(int xIndexLine1, SidescanLine line1,
            int xIndexLine2, SidescanLine line2) {
        return calcDistanceFrom2XIndexesOf2SidescanLines(xIndexLine1, line1, xIndexLine2, line2, true);
    }

    /**
     * Calculates the slant distance (2D) from two x indexes ({@link SidescanLine#getData()}) of two
     * {@link SidescanLine}s.
     * 
     * @param xIndexLine1
     * @param line1
     * @param xIndexLine2
     * @param line2
     * @return
     */
    public static double calcSlantDistanceFrom2XIndexesOf2SidescanLines(int xIndexLine1, SidescanLine line1,
            int xIndexLine2, SidescanLine line2) {
        return calcDistanceFrom2XIndexesOf2SidescanLines(xIndexLine1, line1, xIndexLine2, line2, false);
    }

    /**
     * Calculates the horizontal or slant distance from two x indexes ({@link SidescanLine#getData()}) of two
     * {@link SidescanLine}s.
     * 
     * @param xIndexLine1
     * @param line1
     * @param xIndexLine2
     * @param line2
     * @param slantCorrected
     * @return
     */
    private static double calcDistanceFrom2XIndexesOf2SidescanLines(int xIndexLine1, SidescanLine line1,
            int xIndexLine2, SidescanLine line2, boolean slantCorrected) {
        SidescanPoint pt2 = line2.calcPointFromIndex(xIndexLine2, slantCorrected);
        SidescanPoint pt1 = line1.calcPointFromIndex(xIndexLine1, slantCorrected);
        double dist = pt1.location.getHorizontalDistanceInMeters(pt2.location);
        return dist;
    }

    /**
     * Calculates the height of an object by the two indexes of the shadow.
     * 
     * @param xIndex1
     * @param xIndex2
     * @param line
     * @return
     */
    public static double calcHeightFrom2XIndexesOfSidescanLine(int xIndex1, int xIndex2, SidescanLine line) {
        double p1 = line.getDistanceFromIndex(xIndex1, false);
        double p2 = line.getDistanceFromIndex(xIndex2, false);

        // Shadow length
        double l = Math.abs(p2 - p1);
        // Altitude
        double a = line.getState().getAltitude();
        // Distance of shadow
        double r = Math.abs(Math.max(p1, p2));
        // Height
        double h = l * a / r;
        return h;
    }
    
    /**
     * Transform a byte array into long (little-endian) according with bitsPerPoint.
     * 
     * @param data
     * @param bitsPerPoint
     * @return
     */
    public static long[] transformData(byte[] data, short bitsPerPoint) {
        if (bitsPerPoint % 8 != 0 || bitsPerPoint > 64 || bitsPerPoint < 8)
            return null;
        
        int bytesPerPoint = bitsPerPoint < 8 ? 1 : (bitsPerPoint / 8);
        long[] fData = new long[data.length / bytesPerPoint];
        
        int k = 0;
        for (int i = 0; i < data.length; /* i incremented inside the 2nd loop */) {
            long val = 0;
            for (int j = 0; j < bytesPerPoint; j++) {
                int v = data[i] & 0xFF;
                v = (v << 8 * j);
                val |= v;
                i++; // progressing index of data
            }
            fData[k++] = val;
        }
        return fData;
    }
    
    /**
     * Takes the data byte array transforms it to a double array applying the scale factor. 
     * 
     * @param data
     * @param scaleFactor
     * @param bitsPerPoint
     * @return
     */
    public static double[] getData(byte[] data, double scaleFactor, short bitsPerPoint) {
        long[] longData = transformData(data, bitsPerPoint);
        if (longData == null)
            return null;
        
        double[] fData = new double[longData.length];
        
        // Lets apply scaling
        for (int i = 0; i < fData.length; i++) {
            if (fData[i] > 0) {
                 fData[i] = longData[i] * scaleFactor;
            }
            else {
                // To account for 64bit unsigned long
                UnsignedLong ul = UnsignedLong.valueOf(Long.toUnsignedString(longData[i]));
                fData[i] = ul.doubleValue() * scaleFactor;
            }
        }
        
        return fData;
    }

    /**
     * Converts a {@link SonarData} into a {@link SidescanLine} and applies the {@link SidescanParameters}.
     * 
     * MRA to be able to use a larger range of IMC versions should not use typed messages,
     * call {@link SidescanUtil#getSidescanLine(SonarData, SystemPositionAndAttitude, SidescanParameters)}
     * or {@link SidescanUtil#getSidescanLine(IMCMessage, SystemPositionAndAttitude)}.
     * 
     * @param sonarData
     * @param pose
     * @param sidescanParams
     * @return
     */
    public static SidescanLine getSidescanLine(SonarData sonarData, SystemPositionAndAttitude pose,
            SidescanParameters sidescanParams) {
        return getSidescanLine((IMCMessage) sonarData, pose,sidescanParams);
    }

    /**
     * Converts a SonarData {@link IMCMessage} into a {@link SidescanLine} and applies the {@link SidescanParameters}.
     * 
     * @param sonarData
     * @param pose
     * @param sidescanParams
     * @return
     */
    public static SidescanLine getSidescanLine(IMCMessage sonarData, SystemPositionAndAttitude pose,
            SidescanParameters sidescanParams) {
        SidescanLine line = null;
        if (sonarData instanceof SonarData)
            line = getSidescanLine((SonarData) sonarData, pose);
        else
            line = getSidescanLine(sonarData, pose);
        
        if (line == null)
            return null;
        
        if (sidescanParams != null) {
            float range = line.getRange();
            double[] sData = line.getData();
            sData = applyNormalizationAndTVG(sData, range, sidescanParams);
            for (int i = 0; i < sData.length; i++) {
                line.getData()[i] = sData[i];
            }
        }

        return line;
    }

    /**
     * Converts a {@link SonarData} into a {@link SidescanLine} without any extra conversion.
     * 
     * MRA to be able to use a larger range of IMC versions should not use typed messages,
     * call {@link SidescanUtil#getSidescanLine(SonarData, SystemPositionAndAttitude, SidescanParameters)}
     * or {@link SidescanUtil#getSidescanLine(IMCMessage, SystemPositionAndAttitude)}.
     * 
     * @param sonarData
     * @param pose
     * @return
     */
    public static SidescanLine getSidescanLine(SonarData sonarData, SystemPositionAndAttitude pose) {
        if (sonarData.getType() != SonarData.TYPE.SIDESCAN) {
            return null;
        }
 
        int range = sonarData.getMaxRange();
        byte[] data = sonarData.getData();
        double scaleFactor = sonarData.getScaleFactor();
        short bitsPerPoint = sonarData.getBitsPerPoint();
        double[] sData = getData(data, scaleFactor, bitsPerPoint);
        
        long timeMillis = sonarData.getTimestampMillis();
        long freq = sonarData.getFrequency();
        SidescanLine line = new SidescanLine(timeMillis, range, pose, freq, sData);
        return line;
    }

    /**
     * Converts a SonarData {@link IMCMessage} into a {@link SidescanLine} without any extra conversion.
     * 
     * @param sonarData
     * @param pose
     * @return
     */
    public static SidescanLine getSidescanLine(IMCMessage sonarData, SystemPositionAndAttitude pose) {
        if (!"SonarData".equalsIgnoreCase(sonarData.getAbbrev()) 
                || sonarData.getInteger("type") != SonarData.TYPE.SIDESCAN.value()) {
            return null;
        }
 
        int range = sonarData.getInteger("range");
        if (range == 0)
            range = sonarData.getInteger("max_range");
        byte[] data = sonarData.getRawData("data");
        double scaleFactor = sonarData.getDouble("scale_factor");
        short bitsPerPoint = (short) sonarData.getInteger("bits_per_point");
        double[] sData = getData(data, scaleFactor, bitsPerPoint);
        
        long timeMillis = sonarData.getTimestampMillis();
        long freq = sonarData.getLong("frequency");
        SidescanLine line = new SidescanLine(timeMillis, range, pose, freq, sData);
        return line;
    }

    /**
     * Applies normalization and TVG to data.
     * This does not touch the input data. 
     * 
     * @param data
     * @param range
     * @param sidescanParams
     * @return
     */
    public static double[] applyNormalizationAndTVG(double[] data, double range, SidescanParameters sidescanParams) {
        return applyNormalizationAndTVGMethod1(data, range, sidescanParams);
    }
    
    private static double[] applyNormalizationAndTVGMethod1(double[] data, double range,
            SidescanParameters sidescanParams) {
        int middle = data.length / 2;
        double[] outData = new double[data.length]; 

        double avgSboard = 0;
        double avgPboard = 0;
        for (int c = 0; c < data.length; c++) {
            double r = data[c];
            if (c < middle)
                avgPboard += r;
            else
                avgSboard += r;                        
        }
        
        avgPboard /= (double) middle * sidescanParams.getNormalization();
        avgSboard /= (double) middle * sidescanParams.getNormalization();

        // applying slide window
        double minVal = MathMiscUtils.round(Math.min(1, Math.max(0, sidescanParams.getMinValue())), 2);
        double maxVal = MathMiscUtils.round(Math.min(1 - minVal, Math.max(0, minVal + sidescanParams.getWindowValue())), 2);

        for (int c = 0; c < data.length; c++) {
            double r;
            double avg;
            if (c < middle) {
                r =  c / (double) middle;
                avg = avgPboard;
            }
            else {
                r =  1 - (c - middle) / (double) middle;
                avg = avgSboard;
            }
            double gain = Math.abs(30.0 * Math.log(r));
            double pb = data[c] * Math.pow(10, gain / sidescanParams.getTvgGain());
            double v = pb / avg;

            if ((minVal > 0 || maxVal < 1) && !Double.isNaN(v) && Double.isFinite(v)) {
                v = (v - minVal) / (maxVal - minVal);
            }
            outData[c] = v;
        }
        
        return outData;
    }

    @SuppressWarnings("unused")
    private static double[] applyNormalizationAndTVGMethod2(double[] data, double range,
            SidescanParameters sidescanParams) {
        double max = 0;
        double[] outData = new double[data.length]; 

        for (int i = 0; i < data.length; i++) {
            max = Math.max(max, data[i]);
        }

        for (int i = 0; i < data.length; i++) {
            outData[i] = data[i] / max;
        }
        
        return outData;
    }
}
