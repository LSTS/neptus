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
 * Author: Frédéric Leishman
 * 12 juin 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Frédéric Leishman
 * 
 */
@PluginDescription(name = "Bathymetry Map Exporter")
public class BathyMapExporter implements MRAExporter {

    @SuppressWarnings("unused")
    private IMraLogGroup source;
    @SuppressWarnings("unused")
    private ProgressMonitor pmonitor;
    private BathymetryParser bparser;   

    @NeptusProperty(name = "Number point to ignore", description = "Number of points of multibeam measure to ignore")
    public double ptsToIgnore = 2.0;

    @NeptusProperty(name = "Resolution", description = "Resolution of the map")
    public double mapResolution = 1.0;

    @NeptusProperty(name = "Minimal point to validate a cell", description = "Number minimal of point by cell enables its validation")
    public int ptsMinByCells = 30;

    @NeptusProperty(name = "Minimal point for filters", description = "Number minimal of valid value of the gaussian kernel mask enables the interpolation of unvalidate cells")
    public int ptsMinFilter = 20;

    @NeptusProperty(name = "Matrix format", description = "Choose the final format. List of point representing each valid cells (val = false)(ex: for paraview importer) or matrix 2D of all cells (val = true) (ex: for matlab importer)")
    public boolean matriceFormat = false;

    @NeptusProperty(name = "Empty cell value ", description = "Value to set in the case of unvalidate cells")
    public String valueCellEmpty = "Nan";

    public BathyMapExporter(IMraLogGroup source) {
        this.source = source;
        bparser = null;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        // Bathymetry Parser generated, if ok (.83P exist)-> map generator can be used
        bparser = BathymetryParserFactory.build(source, "multibeam");
        if (bparser != null) {
            return true;
        }
        return false;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        // Ask setting map parameters
        PluginUtils.editPluginProperties(this, true);

        // Initialization of progression display
        int indexProgression = 1;
        this.pmonitor = pmonitor;
        pmonitor.setMinimum(0);
        pmonitor.setMaximum(10001);

        pmonitor.setNote(I18n.text("PointCloud Loading..."));
        pmonitor.setProgress(1);

        bparser = BathymetryParserFactory.build(source, "multibeam");

        int countPointsMB = 0;
        LocationType initLoc = null;

        // Get the reference location of estimated state
        IMCMessage map_ref = source.getLsfIndex().getMessage(
                source.getLsfIndex().getFirstMessageOfType("EstimatedState"));

        double maxX = -100000;
        double maxY = -100000;
        double minX = 100000;
        double minY = 100000;

        List<double[]> tableMB = new ArrayList<double[]>();

        //NED offset to the MBS acquisition beginning
        double initOffsetNorth = 0;
        double initOffsetEast = 0;
        double initOffsetDown = 0;

        IMraLog estimatedStateParser = source.getLog("EstimatedState");
        
        BathymetrySwath bs;
        while ((bs = bparser.nextSwath()) != null) {
            LocationType loc = bs.getPose().getPosition();

            if (initLoc == null) {
                initLoc = new LocationType(loc);
                
                // Offset Correction, we get the first location correspond to the Mbs acquisition beginning  
                initOffsetNorth = (double) estimatedStateParser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("x");
                initOffsetEast  = (double) estimatedStateParser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("y");
                initOffsetDown  = (double) estimatedStateParser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("z");
                
                initLoc.setOffsetNorth(-initOffsetNorth);
                initLoc.setOffsetEast(-initOffsetEast);                   
                initLoc.setOffsetDown(initOffsetDown);  
            }

            for (int c = 0; c < bs.getNumBeams(); c++) {
                
                if (pmonitor.isCanceled())
                    break;
                
                // Limitation of number of point (14M pts is too much)
                if (Math.random() > 1.0 / ptsToIgnore)
                    continue;

                BathymetryPoint p = bs.getData()[c];
                if (p == null) {
                    continue;
                }

                // Gets offset north and east and adds with bathymetry point
                LocationType tempLoc = new LocationType(loc);

                tempLoc.translatePosition(p.north, p.east, 0);

                // Add data to table
                double data[] = tempLoc.getOffsetFrom(initLoc);

                // Add normalized depth
                double ptsMB[] = {data[0], data[1], p.depth};
                tableMB.add(ptsMB);

                ++countPointsMB;

                // Map border limits
                if (ptsMB[0] > maxX) {
                    maxX = ptsMB[0];
                }
                if (ptsMB[0] < minX) {
                    minX = ptsMB[0];
                }
                if (ptsMB[1] > maxY) {
                    maxY = ptsMB[1];
                }
                if (ptsMB[1] < minY) {
                    minY = ptsMB[1];
                }
            }

            // Progress notification
            indexProgression = (int) ((double) (countPointsMB)
                    / (double) (bparser.getBathymetryInfo().totalNumberOfPoints) * 8000.0 * ptsToIgnore);
            if (indexProgression > 0 && indexProgression < 8000) {
                pmonitor.setProgress(indexProgression);
                pmonitor.setNote(I18n.textf("PointCloud Loading... %pt", indexProgression / 100 + "%"));
            }
            
            if (pmonitor.isCanceled())
                break;
        }

        pmonitor.setProgress(8000);
        pmonitor.setNote(I18n.text("Map building..."));

        // Height map initialization
        Map map = new Map(mapResolution, minX, maxX, minY, maxY);
        map.SetParameters(ptsMinByCells, ptsMinFilter);
        map.CreateMapWithPointCloud(tableMB, countPointsMB);

        pmonitor.setProgress(9000);
        pmonitor.setNote(I18n.text("Map exporting..."));

        // Map is generated inside the mra path
        File dir = source.getFile("mra");

        try {
            File out = new File(dir, "bathymetry_mb_map.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(out));

            if (matriceFormat == false) {
                // Map generates as cells position list

                // Map header
                bw.write("#LLH Offset: " + String.format(Locale.US, "%.8f", map_ref.getDouble("lat")) + ", "
                        + String.format(Locale.US, "%.8f", map_ref.getDouble("lon")) + ", "
                        + String.format(Locale.US, "%.8f", map_ref.getDouble("height")) + "\n" +

                        "#NED Offset: " + String.format(Locale.US, "%.3f", map_ref.getDouble("x")) + ", "
                        + String.format(Locale.US, "%.3f", map_ref.getDouble("y")) + ", "
                        + String.format(Locale.US, "%.3f", map_ref.getDouble("z")) + "\n" +

                        "#Num NED Cells: " + Integer.toString(map.num_cells_valid) + "\n");

                // Map body
                for (int i = 0; i < map.num_cells; i++) {
                    if (map.cells.get(i).IsValidated()) {
                        bw.write(String.format(Locale.US, "%.3f", map.cells.get(i).position_x) + ", "
                                + String.format(Locale.US, "%.3f", map.cells.get(i).position_y) + ", "
                                + String.format(Locale.US, "%.3f", map.cells.get(i).depth) + "\n");
                    }
                    
                    if (pmonitor.isCanceled())
                        break;
                }
            }
            else {
                // Map generates as cells matrix disposition

                // Map header
                bw.write("#LLH Offset: " + String.format(Locale.US, "%.8f", map_ref.getDouble("lat")) + ", "
                        + String.format(Locale.US, "%.8f", map_ref.getDouble("lon")) + ", "
                        + String.format(Locale.US, "%.8f", map_ref.getDouble("height")) + "\n" +

                        "#NED Offset: " + String.format(Locale.US, "%.3f", map_ref.getDouble("x")) + ", "
                        + String.format(Locale.US, "%.3f", map_ref.getDouble("y")) + ", "
                        + String.format(Locale.US, "%.3f", map_ref.getDouble("z")) + "\n" +

                        "#Start Cells Position: " + String.format(Locale.US, "%.3f", map.min_x) + ", "
                        + String.format(Locale.US, "%.3f", map.min_y) + "\n" +

                        "#Resolution: " + String.format(Locale.US, "%.2f", mapResolution) + "\n");

                // Map body
                int id = 0;
                for (int j = 0; j < map.num_j; j++) {
                    if (pmonitor.isCanceled())
                        break;

                    for (int i = 0; i < map.num_i; i++) {
                        id = i + j * map.num_i;
                        if (map.cells.get(id).IsValidated()) {
                            bw.write(String.format(Locale.US, "%.3f", map.cells.get(id).depth) + ", ");
                        }
                        else {
                            bw.write(valueCellEmpty + ", ");
                        }

                        if (pmonitor.isCanceled())
                            break;
                    }
                    bw.write("\n");
                }
            }

            bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (pmonitor.isCanceled())
            return I18n.text("Cancelled by user");

        // Map export completed
        pmonitor.setProgress(pmonitor.getMaximum());
        return I18n.text("Bathymetry map export has completed successfully");
    }
}
