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
 * Author: hfq
 * Apr 26, 2013
 */
package pt.lsts.neptus.vtk.mravisualizer;

import java.util.Date;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.api.BathymetryInfo;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.lsf.DVLBathymetryParser;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.bathymetry.TidePredictionFinder;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import vtk.vtkPoints;
import vtk.vtkShortArray;

/**
 * @author hfq
 *
 */
public class LoadToPointCloud {

    public IMraLogGroup source;
    public IMraLog state;

    public BathymetryInfo batInfo;

    public BathymetryParser parser;
    public APointCloud<?> pointCloud;
    
    private TidePredictionFinder finder;
    private boolean alreadyTriedToLoadTideFinder = false;
    
    private vtkPoints points;
    private vtkShortArray intensities;


    // private int countIntens = 0;
    // private int countIntensZero = 0;

    /**
     * @param log
     * @param pointCloud
     */
    public LoadToPointCloud(IMraLogGroup log, APointCloud<?> pointCloud) {
        this.source = log;
        this.pointCloud = pointCloud;
    }

    private double getTideOffset(long timestampMillis) {
        try {
            if (finder == null)
                return 0;
            Float ret = finder.getTidePrediction(new Date(timestampMillis), false);
            return ret == null || ret.isNaN() || ret.isInfinite() ? 0 : ret;
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage(), e);
            return 0;
        }
    }

    public void parseMultibeamPointCloud () {
        parser = BathymetryParserFactory.build(this.source, "multibeam");

        getOrCreateTideDataProvider();

        parser.rewind();

        BathymetrySwath bs;

        setPoints(new vtkPoints());
        setIntensities(new vtkShortArray());

        int countPoints = 0;
        LocationType initLoc = null;

        while ((bs = parser.nextSwath()) != null) {
            LocationType loc = bs.getPose().getPosition();

            if(initLoc == null)
                initLoc = new LocationType(loc);

            double tideOffset = getTideOffset(bs.getTimestamp());

            if (!MRAProperties.approachToIgnorePts) {
                for (int c = 0; c < bs.getNumBeams(); c += MRAProperties.ptsToIgnore) {
                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;

                    // gets offset north and east and adds with bathymetry point p.north and p.east respectively
                    LocationType tempLoc = new LocationType(loc);

                    tempLoc.translatePosition(p.north, p.east, 0);

                    // add data to pointcloud
                    double offset[] = tempLoc.getOffsetFrom(initLoc);

                    getPoints().InsertNextPoint(offset[0],
                            offset[1],
                            p.depth - tideOffset);

                    if (parser.getHasIntensity()) {
                        getIntensities().InsertValue(c, p.intensity);
                        // pointCloud.setHasIntensities(true);
                    }

                    ++countPoints;
                }
            }
            else {
                for (int c = 0; c < bs.getNumBeams(); c++) {
                    if (Math.random() > 1.0 / MRAProperties.ptsToIgnore)
                        continue;

                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;
                    // gets offset north and east and adds with bathymetry point p.north and p.east respectively
                    LocationType tempLoc = new LocationType(loc);

                    tempLoc.translatePosition(p.north, p.east, 0);

                    // add data to pointcloud
                    double offset[] = tempLoc.getOffsetFrom(initLoc);
                    //System.out.println(offset[0] + " " + offset[1]);
                    getPoints().InsertNextPoint(offset[0],
                            offset[1],
                            p.depth - tideOffset);

                    //                    if (multibeamDeltaTParser.getHasIntensity()) {
                    //                        ++countIntens;
                    //                        getIntensities().InsertValue(c, p.intensity);
                    //                        pointCloud.setHasIntensities(true);
                    //
                    //                        if (p.intensity == 0)
                    //                            ++countIntensZero;
                    //                        //NeptusLog.pub().info("intensity: " + p.intensity);
                    //                        //NeptusLog.pub().info("intensity from array: " + getIntensities().GetValue(c));
                    //                    }

                    ++countPoints;
                }
            }
        }

        //        NeptusLog.pub().info("Number of intensity values: " + countIntens);
        //        NeptusLog.pub().info("Number of intensity zero: " + countIntensZero);

        parser.getBathymetryInfo().totalNumberOfPoints = countPoints;
        batInfo = parser.getBathymetryInfo();

        pointCloud.setNumberOfPoints(parser.getBathymetryInfo().totalNumberOfPoints);

        NeptusLog.pub().info("Total number of points: " + parser.getBathymetryInfo().totalNumberOfPoints);
        NeptusLog.pub().info("Number of points on multibeamtopointcloud: " + getPoints().GetNumberOfPoints());
        //}
    }

    public void parseDVLPointCloud() {
        parser = BathymetryParserFactory.build(this.source, "dvl");

        if (parser instanceof DVLBathymetryParser) {
            getOrCreateTideDataProvider();

            NeptusLog.pub().info("Parsing dvl points to vtk points");

            parser.rewind();
            BathymetrySwath bs;

            setPoints(new vtkPoints());

            int countPoints = 0;
            LocationType initLoc = null;

            //int countSwaths = 0;
            while ((bs = parser.nextSwath()) != null)  {
                //++countSwaths;
                LocationType loc = bs.getPose().getPosition();
                if(initLoc == null) {
                    initLoc = new LocationType(loc);
                }

                double tideOffset = getTideOffset(bs.getTimestamp());

                for (int c = 0; c < bs.getNumBeams(); ++c) {
                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;

                    LocationType tempLoc = new LocationType(loc);

                    tempLoc.translatePosition(p.north, p.east, 0);

                    // add data to pointcloud
                    double offset[] = tempLoc.getOffsetFrom(initLoc);
                    getPoints().InsertNextPoint(offset[0],
                            offset[1],
                            p.depth - tideOffset);

                    ++countPoints;
                }

            }
            NeptusLog.pub().info("number of points: " + countPoints);
            parser.getBathymetryInfo().totalNumberOfPoints = countPoints;
            batInfo = parser.getBathymetryInfo();

            pointCloud.setNumberOfPoints(parser.getBathymetryInfo().totalNumberOfPoints);
        }
    }

    private void getOrCreateTideDataProvider() {
        if (!alreadyTriedToLoadTideFinder && finder == null)
            finder = TidePredictionFactory.create(this.source.getLsfIndex());
        if (finder == null)
            NeptusLog.pub().warn("No tides data found!!");
        alreadyTriedToLoadTideFinder = true;
    }

    /**
     * @return the points
     */
    public vtkPoints getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(vtkPoints points) {
        this.points = points;
    }

    /**
     * @return the intensities
     */
    public vtkShortArray getIntensities() {
        return intensities;
    }

    /**
     * @param intensities the intensities to set
     */
    public void setIntensities(vtkShortArray intensities) {
        this.intensities = intensities;
    }

    public void showIntensities() {
        NeptusLog.pub().info("Number of intensities values: " + getIntensities().GetSize());

        for (int i = 0; i < getIntensities().GetSize(); ++i) {
            //NeptusLog.pub().info("Intensity value: " + getIntensities().GetValue(i));
        }
    }

    //    /**
    //     *
    //     */
    //    private void getMyDeltaTHeader() {
    //        file = source.getFile("multibeam.83P");
    //        //System.out.println("print parent: " + file.toString());
    //        try {
    //            fileInputStream = new FileInputStream(file);
    //        }
    //        catch (FileNotFoundException e) {
    //            NeptusLog.pub().info("File not found: " + e);
    //            e.printStackTrace();
    //        }
    //        catch (IOException ioe) {
    //            NeptusLog.pub().info("Exception while reading the file: " + ioe);
    //            ioe.printStackTrace();
    //        }
    //
    //        channel = fileInputStream.getChannel();
    //        long posOnFile = 0;
    //        long sizeOfRegionToMap = 256;   // 256 bytes currespondent to the header of each ping
    //        try {
    //            buf = channel.map(MapMode.READ_ONLY, posOnFile, sizeOfRegionToMap);
    //        }
    //        catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //
    //        MultibeamDeltaTHeader deltaTHeader = new MultibeamDeltaTHeader(buf);
    //        deltaTHeader.parseHeader();
    //    }
}
