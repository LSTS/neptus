/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 26/08/2016
 */
package pt.lsts.neptus.util.coord;

import java.awt.image.BufferedImage;

/**
 * @author zp
 *
 */
/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 26/08/2016
 */

import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.sanselan.Sanselan;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.UTMCoordinates;
import pt.lsts.neptus.types.map.ImageElement;

/**
 * @author zp
 *
 */
public class GdalDataSet {

    private Dataset hDataset = null;
    private int utmZone = -1;
    private boolean northernHemisphere = false;
    private File imageFile = null;
    private boolean wgs84 = false;

    public GdalDataSet(File f) {
        GdalUtilities.loadNativeLibraries();
        hDataset = gdal.Open(f.getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
        this.imageFile = f;

        String pszProjection = hDataset.GetProjectionRef();

        if (!pszProjection.isEmpty()) {
            SpatialReference ref = new SpatialReference(pszProjection);
            wgs84 = ref.IsGeographic() != 0;
            if (ref.IsProjected() != 0) {
                utmZone = ref.GetUTMZone();
                northernHemisphere = true;
                if (utmZone < 0) {
                    utmZone = -utmZone;
                    northernHemisphere = false;
                }
            }
        }
    }

    public boolean isWgs84() {
        return wgs84;
    }

    public boolean isUtm() {
        return !isWgs84();
    }

    public boolean isUtmZoneKnown() {
        return utmZone != -1;
    }

    public boolean isUtmHemisphereKnown() {
        return utmZone != -1;
    }

    public void setUtmZone(int zone, char letter) {
        setUtmZone(zone, Character.toUpperCase(letter) > 'M');
    }

    public void setUtmZone(int zone, boolean northernHemisphere) {
        utmZone = zone;
        this.northernHemisphere = northernHemisphere;
    }

    public final boolean isNorthernHemisphere() {
        return northernHemisphere;
    }

    public BufferedImage getGroundOverlay() throws Exception {
        BufferedImage img = ImageIO.read(imageFile);
        if (img != null)
            return img;
        else
            return Sanselan.getBufferedImage(imageFile);
    }

    public LocationType getCenterCoordinates() {
        double center[] = getCenter();

        if (isWgs84()) {
            return new LocationType(center[1], center[0]);
        }

        if (!isWgs84() && isUtmZoneKnown() && isUtmHemisphereKnown()) {
            UTMCoordinates coords = new UTMCoordinates(center[0], center[1], utmZone, northernHemisphere ? 'N' : 'D');
            return new LocationType(coords.getLatitudeDegrees(), coords.getLongitudeDegrees());
        }

        return null;
    }
    
    public LocationType[] getCornerCoordinates() {
        double[] adfGeoTransform = new double[6];
        double dfGeoX, dfGeoY;

        ArrayList<double[]> coordinates = new ArrayList<>();
        ArrayList<LocationType> locations = new ArrayList<>();
        
        coordinates.add(new double[] {0,0});
        coordinates.add(new double[] {0,hDataset.getRasterYSize()});
        coordinates.add(new double[] {hDataset.getRasterXSize(),0});
        coordinates.add(new double[] {hDataset.getRasterXSize(),hDataset.getRasterYSize()});
        hDataset.GetGeoTransform(adfGeoTransform);
        
        for (double[] coord : coordinates) {
            dfGeoX = adfGeoTransform[0] + adfGeoTransform[1] * coord[0] + adfGeoTransform[2] * coord[1];
            dfGeoY = adfGeoTransform[3] + adfGeoTransform[4] * coord[0] + adfGeoTransform[5] * coord[1];
            
            if (isWgs84()) {
                locations.add(new LocationType(dfGeoY, dfGeoX));
            }

            if (!isWgs84() && isUtmZoneKnown() && isUtmHemisphereKnown()) {
                UTMCoordinates utmCoords = new UTMCoordinates(dfGeoX, dfGeoY, utmZone, northernHemisphere ? 'N' : 'D');
                locations.add(new LocationType(utmCoords.getLatitudeDegrees(), utmCoords.getLongitudeDegrees()));
            }
        }
        
        return locations.toArray(new LocationType[0]);     
    }
    
    public double getRotationRads() {
        LocationType[] corners = getCornerCoordinates();
        if (corners.length == 0)
            return 0;
        return Math.PI - corners[0].getXYAngle(corners[1]);
    }    

    private double[] getCenter() {
        double[] adfGeoTransform = new double[6];
        double dfGeoX, dfGeoY;
        double x = hDataset.getRasterXSize() / 2.0;
        double y = hDataset.getRasterYSize() / 2.0;

        hDataset.GetGeoTransform(adfGeoTransform);
        dfGeoX = adfGeoTransform[0] + adfGeoTransform[1] * x + adfGeoTransform[2] * y;
        dfGeoY = adfGeoTransform[3] + adfGeoTransform[4] * x + adfGeoTransform[5] * y;

        return new double[] { dfGeoX, dfGeoY };
    }

    private double[] getMetersPerPixel() {
        double[] adfGeoTransform = new double[6];
        hDataset.GetGeoTransform(adfGeoTransform);
        if (!wgs84)
            return new double[] { adfGeoTransform[1], adfGeoTransform[5] };
        else {
            LocationType loc = getCenterCoordinates();
            LocationType loc2 = new LocationType(loc.getLatitudeDegs() + 1, loc.getLongitudeDegs());
            double ySize = loc2.getDistanceInMeters(loc);
            return new double[] { adfGeoTransform[1] * ySize, adfGeoTransform[5] * ySize };
        }
    }
    
    public ImageElement asImageElement(File outputDir) throws Exception {
        ImageElement el = new ImageElement();
        if (!outputDir.isDirectory())
            outputDir = outputDir.getParentFile();
        File file = new File(outputDir, el.getId()+".png");
        ImageIO.write(getGroundOverlay(), "png", file);
        
        el.setImageFileName(file.getAbsolutePath());
        
        el.setImageScale(getMetersPerPixel()[0]);
        el.setYaw(getRotationRads());
        el.setCenterLocation(getCenterCoordinates());
        
        return el;
    }

    public static void main(String[] args) throws Exception {
        GdalDataSet dataSet1 = new GdalDataSet(new File("/home/zp/Desktop/jpgw_exmple/TC_NG_Madrid_ES_Geo.tif"));
        GdalDataSet dataSet2 = new GdalDataSet(new File("/home/zp/Desktop/jpgw_exmple/O44121a1.jpg"));
        GdalDataSet dataSet3 = new GdalDataSet(new File("/home/zp/Desktop/jpgw_exmple/m30dem.tif"));

        System.out.println(dataSet1.getCenterCoordinates());
        System.out.println(dataSet1.getCornerCoordinates());
        System.out.println(dataSet1.getRotationRads());
        System.out.println(Arrays.toString(dataSet1.getMetersPerPixel()));

        System.out.println(dataSet2.getCenterCoordinates());
        System.out.println(dataSet2.getCornerCoordinates());
        System.out.println(dataSet2.getRotationRads());
        System.out.println(Arrays.toString(dataSet2.getMetersPerPixel()));

        System.out.println(dataSet2.getCenterCoordinates());
        System.out.println(dataSet2.getCornerCoordinates());
        System.out.println(dataSet2.getRotationRads());
        System.out.println(Arrays.toString(dataSet3.getMetersPerPixel()));
    }

}
