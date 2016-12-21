/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 \* Alternatively, this file may be used under the terms of the Modified EUPL,
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

import pt.lsts.neptus.types.coord.LocationType;

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
}
