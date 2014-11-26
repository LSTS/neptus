/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Frédéric Leishman
 * 12 juin 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Frédéric Leishman
 * 
 */
@PluginDescription(name = "Bathy Map Exporter")
public class BathyMapExporter implements MRAExporter {

    IMraLogGroup source;
    ProgressMonitor pmonitor;
    BathymetryParser bparser;

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
    public String getName() {
        return PluginUtils.getPluginDescription(getClass());
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        // Ask setting map parameters
        PluginUtils.editPluginProperties(this, true);

        // Initialization of progression display
        int index_progression = 1;
        this.pmonitor = pmonitor;
        pmonitor.setMinimum(0);
        pmonitor.setMaximum(10001);

        pmonitor.setNote("PointCloud Loading... ");
        pmonitor.setProgress(1);

        bparser = BathymetryParserFactory.build(source, "multibeam");

        int countPoints_MB = 0;
        LocationType initLoc = null;

        // Get the reference location of estimated state
        IMCMessage map_ref = source.getLsfIndex().getMessage(
                source.getLsfIndex().getFirstMessageOfType("EstimatedState"));

        double max_x = -100000;
        double max_y = -100000;
        double min_x = 100000;
        double min_y = 100000;

        List<double[]> table_MB = new ArrayList<double[]>();

        BathymetrySwath bs;
        while ((bs = bparser.nextSwath()) != null) {
            LocationType loc = bs.getPose().getPosition();

            if (initLoc == null) {
                initLoc = new LocationType(loc);
            }

            for (int c = 0; c < bs.getNumBeams(); c++) {
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
                double offset[] = tempLoc.getOffsetFrom(initLoc);

                // Add normalized depth
                double pts_mb[] = { offset[0] + initLoc.getOffsetNorth(), offset[1] + initLoc.getOffsetEast(),
                        p.depth + initLoc.getOffsetDown() };
                table_MB.add(pts_mb);

                ++countPoints_MB;

                // Map border limits
                if (pts_mb[0] > max_x) {
                    max_x = pts_mb[0];
                }
                if (pts_mb[0] < min_x) {
                    min_x = pts_mb[0];
                }
                if (pts_mb[1] > max_y) {
                    max_y = pts_mb[1];
                }
                if (pts_mb[1] < min_y) {
                    min_y = pts_mb[1];
                }
            }

            // Progress notification
            index_progression = (int) ((double) (countPoints_MB)
                    / (double) (bparser.getBathymetryInfo().totalNumberOfPoints) * 8000.0 * ptsToIgnore);
            if (index_progression > 0 && index_progression < 8000) {
                pmonitor.setProgress(index_progression);
                pmonitor.setNote("PointCloud Loading... " + index_progression / 100 + "%");
            }
        }

        pmonitor.setProgress(8000);
        pmonitor.setNote("Map building ... 80%");

        // Height map initialization
        Map map = new Map(mapResolution, min_x, max_x, min_y, max_y);
        map.SetParameters(ptsMinByCells, ptsMinFilter);
        map.CreateMapWithPointCloud(table_MB, countPoints_MB);

        pmonitor.setProgress(9000);
        pmonitor.setNote("Map exporting ... 90%");

        // Map is generated inside the mra path
        File dir = new File(source.getFile("mra"), "mbp");
        dir.mkdirs();

        try {
            File out = new File(dir, "carte_mbp.txt");
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
                    for (int i = 0; i < map.num_i; i++) {
                        id = i + j * map.num_i;
                        if (map.cells.get(id).IsValidated()) {
                            bw.write(String.format(Locale.US, "%.3f", map.cells.get(id).depth) + ", ");
                        }
                        else {
                            bw.write(valueCellEmpty + ", ");
                        }
                    }
                    bw.write("\n");
                }
            }

            bw.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // Map export completed
        pmonitor.setProgress(pmonitor.getMaximum());
        return "Bathy map export is successfully completed";
    }
}
