/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Sep 13, 2013
 */
package pt.lsts.neptus.plugins.noptilus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import convcao.com.agent.NoptilusCoords;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.colormap.DataDiscretizer;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="Noptilus Normalized map", experimental=true)
public class NoptilusMapExporter implements MRAExporter, PropertiesProvider {

    NoptilusCoords coords = new NoptilusCoords();

    protected IMraLogGroup source = null;

    public NoptilusMapExporter(IMraLogGroup source) {
        this.source = source;
        PluginUtils.loadProperties(this, "default");
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        // return source.getLsfIndex().containsMessagesOfType("EstimatedState");
        return false;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        PropertiesEditor.editProperties(NoptilusMapExporter.this, true);
        coords.squareCenter.convertToAbsoluteLatLonDepth();
        PluginUtils.saveProperties(this, "default");
        double mapWidth = coords.numCols * coords.cellWidth;
        double mapHeight = coords.numRows * coords.cellWidth;
        double cellSize = coords.cellWidth;
        LocationType sw = new LocationType(coords.squareCenter);
        sw.translatePosition(-mapWidth / 2, -mapHeight / 2, 0);

        DataDiscretizer highRes = new DataDiscretizer(cellSize);
        DataDiscretizer lowRes = new DataDiscretizer(cellSize * 10);
        int numCols = (int) (mapWidth / cellSize);
        int numRows = (int) (mapHeight / cellSize);
        boolean pathHigh[][] = new boolean[numCols][numRows];
        boolean pathLow[][] = new boolean[numCols / 10 + 1][numRows / 10 + 1];
        BufferedImage imgHigh = new BufferedImage(numRows, numCols, BufferedImage.TYPE_INT_ARGB);
        BufferedImage imgLow = new BufferedImage(numRows / 10, numCols / 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage pathImg = new BufferedImage(numRows, numCols, BufferedImage.TYPE_INT_ARGB);
        // if (source.getFile("data.83P") == null) {
        NeptusLog.pub().info(I18n.text("no multibeam data has been found... using DVL"));
        LsfIterator<EstimatedState> it = source.getLsfIndex().getIterator(EstimatedState.class);

        while (it.hasNext()) {
            EstimatedState state = it.next();
            LocationType loc = IMCUtils.parseLocation(state);
            double[] offsets = loc.getOffsetFrom(sw);
            if (offsets[0] < 0 || offsets[0] > mapHeight)
                continue;
            if (offsets[1] < 0 || offsets[1] > mapWidth)
                continue;

            int col = (int) (offsets[1] / cellSize);
            int row = (int) (offsets[0] / cellSize);
            pathHigh[col][row] = true;
            pathLow[col / 10][row / 10] = true;
            highRes.addPoint(offsets[1], offsets[0], state.getAlt() + state.getDepth());
            lowRes.addPoint(offsets[1], offsets[0], state.getAlt() + state.getDepth());
            pathImg.setRGB(col, row, Color.blue.darker().getRGB());
        }
        //}
        if (DeltaTParser.findDataSource(source) != null) {
            highRes = new DataDiscretizer(cellSize);
            DeltaTParser parser = new DeltaTParser(source);
            parser.rewind();

            LocationType topLeft = new LocationType(coords.squareCenter);
            topLeft.translatePosition(mapHeight / 2, -mapWidth / 2, 0);
            LocationType bottomRight = new LocationType(coords.squareCenter);
            bottomRight.translatePosition(-mapHeight / 2, mapWidth / 2, 0);

            BathymetrySwath swath;
            while ((swath = parser.nextSwath(0.3)) != null) {

                LocationType loc = swath.getPose().getPosition();

                for (BathymetryPoint bp : swath.getData()) {
                    if (Math.random() < 0.2)
                        continue;
                    LocationType loc2 = new LocationType(loc);
                    if (bp == null)
                        continue;
                    loc2.translatePosition(bp.north, bp.east, 0);
                    double offsets[] = loc2.getOffsetFrom(sw);
                    if (offsets[0] < 0 || offsets[0] > mapHeight)
                        continue;
                    if (offsets[1] < 0 || offsets[1] > mapWidth)
                        continue;

                    // int col = (int)(offsets[1] / cellSize);
                    // int row = (int)(offsets[0] / cellSize);
                    //pathHigh[col][row] = true;
                    //pathLow[col/10][row/10] = true;
                    highRes.addPoint(offsets[1], offsets[0], bp.depth);
                    //lowRes.addPoint(offsets[1], offsets[0], bp.depth);
                    //pathImg.setRGB(col, row, Color.red.darker().getRGB());
                }
            }
        }

        String dirOut = source.getFile("mra").getAbsolutePath()+"/noptilus";

        ColorMapUtils.generateColorMap(highRes.getDataPoints(), imgHigh.createGraphics(), numRows, numCols, 0, ColorMapFactory.createGrayScaleColorMap());
        ColorMapUtils.generateColorMap(lowRes.getDataPoints(), imgLow.createGraphics(), numRows/10, numCols/10, 0, ColorMapFactory.createGrayScaleColorMap());

        try {
            String desc = "Map centered in "+coords.squareCenter.getLatitudeAsPrettyString(LatLonFormatEnum.DMS)+" / "+coords.squareCenter.getLongitudeAsPrettyString(LatLonFormatEnum.DMS);
            pathToFile(pathHigh, (float)cellSize, new File(dirOut+"/path_highres.txt"), desc);
            pathToFile(pathLow, (float)cellSize*10, new File(dirOut+"/path_lowres.txt"), desc);
            ImageToFile(imgHigh, (float)cellSize, new File(dirOut+"/highres.txt"), desc);
            ImageToFile(imgLow, (float)cellSize*10, new File(dirOut+"/lowres.txt"), desc);
            ImageIO.write(pathImg, "PNG", new File(dirOut+"/path.png"));
            ImageIO.write(imgHigh, "PNG", new File(dirOut+"/highres.png"));
            ImageIO.write(imgLow, "PNG", new File(dirOut+"/lowres.png"));
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass().getSimpleName()+"  while generating maps: "+e.getMessage();
        }
        return "Map files saved in "+dirOut;
    }

    public void pathToFile(boolean[][] path, float resolution, File out, String desc) throws Exception {
        out.getParentFile().mkdirs();
        int numRows = path.length;
        int numCols = path[0].length;
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));
        bw.write("# Path generated by Neptus MRA\n");
        bw.write("# "+desc+"\n");
        bw.write("# <num cols> <num rows> <cell size> <?> <?>\n");
        bw.write(numCols+ " "+numRows+" "+resolution+" 0 0\n\n");
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                if (path[col][row])
                    bw.write('1');
                else
                    bw.write('0');

                if (col == numCols-1)
                    bw.write('\n');
                else
                    bw.write(' ');
            }
        }
        bw.close();
    }

    public void ImageToFile(BufferedImage img, float resolution, File out, String desc) throws Exception {
        out.getParentFile().mkdirs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));
        bw.write("# Map generated by Neptus MRA\n");
        bw.write("# "+desc+"\n");
        bw.write("# <num cols> <num rows> <cell size> <?> <?>\n");
        bw.write(img.getWidth()+ " " + img.getHeight()+" "+resolution+" 0 0\n\n");

        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int val = (img.getRGB(x, y) & 0x0000FF00) >> 8;
            double value = (val * 20) / 255.0;
            bw.write((float)value+" ");
            }
            bw.write("\n");
        }

        bw.close();
    }

    @Override
    public DefaultProperty[] getProperties() {
        return coords.getProperties();
    }

    @Override
    public String getPropertiesDialogTitle() {
        return coords.getPropertiesDialogTitle();
    }


    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(coords, properties);
    }

}
