/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * 16/10/2024
 */
package pt.lsts.neptus.util.coord;

public class EPGS3857Utils {
    private static final int TILE_SIZE = 256;
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    private static double[] tileXYToPixelXY(int tileX, int tileY, boolean bottomRight) {
        int pixelX = tileX * TILE_SIZE + (bottomRight ? TILE_SIZE : 0);
        int pixelY = tileY * TILE_SIZE + (bottomRight ? TILE_SIZE : 0);
        return new double[] { pixelX, pixelY };
    }

    private static double[] pixelXYToLatLon(double pixelX, double pixelY, int zoom) {
        double mapSize = TILE_SIZE << zoom;
        double lon = (pixelX / mapSize - 0.5) * 360.0;
        double lat = 90.0 - 360.0 * Math.atan(Math.exp((0.5 - pixelY / mapSize) * 2.0 * Math.PI)) / Math.PI;
        return new double[] { lat, lon };
    }

    private static double[] latLonToEPSG3857(double lat, double lon) {
        double x = lon * ORIGIN_SHIFT / 180.0;
        double y = Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        y = y * ORIGIN_SHIFT / 180.0;
        y *= -1; // Invert the y-coordinate for EPSG:3857
        return new double[] { x, y };
    }

    public static double[][] getTileBoundingBoxEPSG3857(int tileX, int tileY, int zoom) {
        double[] topLeftPixel = tileXYToPixelXY(tileX, tileY, false);
        double[] bottomRightPixel = tileXYToPixelXY(tileX + 1, tileY + 1, false);

        double[] topLeftLatLon = pixelXYToLatLon(topLeftPixel[0], topLeftPixel[1], zoom);
        double[] bottomRightLatLon = pixelXYToLatLon(bottomRightPixel[0], bottomRightPixel[1], zoom);

        double[] topLeftEPSG3857 = latLonToEPSG3857(topLeftLatLon[0], topLeftLatLon[1]);
        double[] bottomRightEPSG3857 = latLonToEPSG3857(bottomRightLatLon[0], bottomRightLatLon[1]);

        return new double[][] { topLeftEPSG3857, bottomRightEPSG3857 };
    }

    public static void main(String[] args) {
        int tileX = 31182;
        int tileY = 24526;
        int zoom = 16;
        double[][] boundingBox = getTileBoundingBoxEPSG3857(tileX, tileY, zoom);
        System.out.println("Bounding Box EPSG:3857 Coordinates: ");
        System.out.println("Top-Left: " + boundingBox[0][0] + ", " + boundingBox[0][1]);
        System.out.println("Bottom-Right: " + boundingBox[1][0] + ", " + boundingBox[1][1]);
    }
}
