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
 * Author: zp
 * 26/08/2016
 */
package pt.lsts.neptus.util.coord;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.apache.sanselan.Sanselan;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author zp
 *
 */
public class GdalDataSet {

    private Dataset hDataset = null;
    private File imageFile = null;
    
    public GdalDataSet(File f) {
        GdalUtilities.loadNativeLibraries();
        hDataset = gdal.Open(f.getAbsolutePath(), gdalconstConstants.GA_ReadOnly);
        this.imageFile = f;
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
        return new LocationType(center[0], center[1]);
        
    }
    
    public LocationType[] getCornerCoordinates() {
        
        ArrayList<double[]> coordinates = new ArrayList<>();
        ArrayList<LocationType> locations = new ArrayList<>();
        
        coordinates.add(new double[] {0,0});
        coordinates.add(new double[] {0,hDataset.getRasterYSize()});
        coordinates.add(new double[] {hDataset.getRasterXSize(),hDataset.getRasterYSize()});
        coordinates.add(new double[] {hDataset.getRasterXSize(),0});
        
        for (double[] coord : coordinates) {
            double[] point = toLatLong(hDataset, coord[0], coord[1]);
            locations.add(new LocationType(point[0], point[1]));            
        }
        
        return locations.toArray(new LocationType[0]);     
    }
    
    public double getRotationRads() {
        LocationType[] corners = getCornerCoordinates();
        // check angle between most distant corners
        if (corners[0].getDistanceInMeters(corners[1]) > corners[0].getDistanceInMeters(corners[3]))
            return corners[1].getXYAngle(corners[0]);
        else
            return corners[0].getXYAngle(corners[1]) - Math.PI/2;
    }    

    private double[] getCenter() {
        return toLatLong(hDataset, hDataset.getRasterXSize() / 2.0, hDataset.getRasterYSize() / 2.0);                
    }
    
    private SpatialReference getSpatialReference() {
        String pszProjection;
        pszProjection = hDataset.GetProjectionRef();
        if (pszProjection != null && pszProjection.length() > 0) {
            return  new SpatialReference(pszProjection);
        }
        return new SpatialReference();
    }

    private double[] getMetersPerPixel() {
        double[] adfGeoTransform = new double[6];
        hDataset.GetGeoTransform(adfGeoTransform);
        
        if (getSpatialReference().IsGeographic() == 0 && adfGeoTransform[2] == 0.0 && adfGeoTransform[4] == 0.0)
            return new double[] {adfGeoTransform[1], -adfGeoTransform[5]};
            
        LocationType corners[] = getCornerCoordinates();
        double distY = corners[0].getHorizontalDistanceInMeters(corners[1]);
        double distX = corners[0].getHorizontalDistanceInMeters(corners[3]);
        
        return new double[] {distX / hDataset.GetRasterXSize(), distY / hDataset.GetRasterYSize() };        
    }
    
    public ImageElement asImageElement(File outputDir) throws Exception {
        ImageElement el = new ImageElement();
        if (!outputDir.isDirectory())
            outputDir = outputDir.getParentFile();
        File file = new File(outputDir, el.getId()+".png");
        try {
            ImageIO.write(getGroundOverlay(), "png", file);
        }
        catch (IOException e) {
            throw new Exception("Unable to read source image: " + e.getMessage(), e);
        }
        
        el.setImageFileName(file.getAbsolutePath());
        double[] mppx = getMetersPerPixel();
        el.setImageScale(mppx[0]);
        el.setImageScaleV(mppx[1]);
        el.setYaw(AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(getRotationRads()), 1)));
        el.setCenterLocation(getCenterCoordinates());
        
        return el;
    }
    
    static double[] toLatLong(Dataset hDataset, double x, double y) {
        double dfGeoX, dfGeoY;
        String pszProjection;
        double[] adfGeoTransform = new double[6];
        CoordinateTransformation hTransform = null;

        /* -------------------------------------------------------------------- */
        /* Transform the point into georeferenced coordinates. */
        /* -------------------------------------------------------------------- */
        hDataset.GetGeoTransform(adfGeoTransform);

        // FIXME check htransform

        {
            pszProjection = hDataset.GetProjectionRef();
            
            dfGeoX = adfGeoTransform[0] + adfGeoTransform[1] * x + adfGeoTransform[2] * y;
            dfGeoY = adfGeoTransform[3] + adfGeoTransform[4] * x + adfGeoTransform[5] * y;
        }

        if (adfGeoTransform[0] == 0 && adfGeoTransform[1] == 0 && adfGeoTransform[2] == 0 && adfGeoTransform[3] == 0
                && adfGeoTransform[4] == 0 && adfGeoTransform[5] == 0) {
            System.out.println("(" + x + "," + y + ")");
            return new double[] { x, y };
        }

        /* -------------------------------------------------------------------- */
        /* Setup transformation to lat/long. */
        /* -------------------------------------------------------------------- */
        if (pszProjection != null && pszProjection.length() > 0) {
            SpatialReference hProj, hLatLong = null;

            hProj = new SpatialReference(pszProjection);
            
            
            if (hProj != null)
                hLatLong = hProj.CloneGeogCS();

            if (hLatLong != null) {
                /* New in GDAL 1.10. Before was "new CoordinateTransformation(srs,dst)". */
                hTransform = new CoordinateTransformation(hProj, hLatLong);
            }

            if (hProj != null)
                hProj.delete();
        }

        /* -------------------------------------------------------------------- */
        /* Transform to latlong */
        /* -------------------------------------------------------------------- */
        if (hTransform != null) {
            double[] transPoint = new double[3];
            hTransform.TransformPoint(transPoint, dfGeoX, dfGeoY, 0);
            if (hTransform != null)
                hTransform.delete();
            return new double[] { transPoint[1], transPoint[0] };
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        
        File f = new File("/home/zp/Downloads");
        
        for (File file : f.listFiles()) {
            if (file.getName().endsWith(".tif")) {
                System.out.println(file.getName() + ":");
                GdalDataSet dataSet = new GdalDataSet(file);
                LocationType loc = new LocationType(dataSet.getCenterCoordinates());
                loc.convertToAbsoluteLatLonDepth();
                System.out.println("\tCenter: " + loc.getLatitudeAsPrettyString() + ", " + loc.getLongitudeAsPrettyString());
                System.out.println("\tRotation: " + Math.toDegrees(dataSet.getRotationRads()));
                System.out.println("\tScale: " + Arrays.toString(dataSet.getMetersPerPixel()));

                @SuppressWarnings("resource")
                Scanner s = new Scanner(
                        Runtime.getRuntime().exec(new String[]{"gdalinfo", file.getAbsolutePath()}).getInputStream())
                            .useDelimiter("\\A");
                System.out.println("GDALINFO:");
                System.out.println(s.next());
                s.close();
            }
        }
    }
}
