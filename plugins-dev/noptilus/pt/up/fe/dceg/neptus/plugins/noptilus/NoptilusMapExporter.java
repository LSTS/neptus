/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Sep 13, 2013
 */
package pt.up.fe.dceg.neptus.plugins.noptilus;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.ColorMapUtils;
import pt.up.fe.dceg.neptus.colormap.DataDiscretizer;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIterator;
import pt.up.fe.dceg.neptus.mra.api.BathymetryPoint;
import pt.up.fe.dceg.neptus.mra.api.BathymetrySwath;
import pt.up.fe.dceg.neptus.mra.exporters.MraExporter;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.importers.deltat.DeltaTParser;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 *
 */
public class NoptilusMapExporter implements MraExporter, PropertiesProvider {

    @NeptusProperty(name="Map center location")
    public LocationType mapCenter = new LocationType();

    @NeptusProperty(name="Map width in meters")
    public double mapWidth = 100;

    @NeptusProperty(name="Map height in meters")
    public double mapHeight = 100;

    @NeptusProperty(name="Cell size in meters")
    public double cellSize = 1;

    protected IMraLogGroup source = null;

    public NoptilusMapExporter(IMraLogGroup source) {
        this.source = source;
        PluginUtils.loadProperties(this, "default");
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    @Override
    public String process() {
        PropertiesEditor.editProperties(NoptilusMapExporter.this, true);
        mapCenter.convertToAbsoluteLatLonDepth();
        PluginUtils.saveProperties(this, "default");
        LocationType sw = new LocationType(mapCenter);
        sw.translatePosition(-mapWidth/2, -mapHeight/2, 0);

        DataDiscretizer highRes = new DataDiscretizer(cellSize);
        DataDiscretizer lowRes = new DataDiscretizer(cellSize * 10);
        int numCols = (int)(mapWidth / cellSize);
        int numRows = (int)(mapHeight / cellSize);
        boolean pathHigh[][] = new boolean[numCols][numRows];
        boolean pathLow[][] = new boolean[numCols/10 + 1][numRows/10 + 1];
        BufferedImage imgHigh = new BufferedImage(numRows, numCols, BufferedImage.TYPE_INT_ARGB);
        BufferedImage imgLow = new BufferedImage(numRows/10, numCols/10, BufferedImage.TYPE_INT_ARGB);

        
        if (source.getFile("multibeam.83P") == null) {
            NeptusLog.pub().info(I18n.text("no multibeam data has been found... using DVL"));
            LsfIterator<EstimatedState> it = source.getLsfIndex().getIterator(EstimatedState.class);

            while(it.hasNext()) {
                EstimatedState state = it.next();
                LocationType loc = IMCUtils.parseLocation(state);
                double[] offsets = loc.getOffsetFrom(sw);
                if (offsets[0] < 0 || offsets[0] > mapHeight)
                    continue;
                if (offsets[1] < 0 || offsets[1] > mapWidth)
                    continue;

                int col = (int)(offsets[1] / cellSize);
                int row = (int)(offsets[0] / cellSize);
                pathHigh[col][row] = true;            
                pathLow[col/10][row/10] = true;
                highRes.addPoint(offsets[1], offsets[0], state.getAlt()+state.getDepth());
                lowRes.addPoint(offsets[1], offsets[0], state.getAlt()+state.getDepth());                        
            }
        }
        else {
            DeltaTParser parser = new DeltaTParser(source);
            parser.rewind();
            
            LocationType topLeft = new LocationType(mapCenter);
            topLeft.translatePosition(mapHeight/2, -mapWidth/2, 0);
            LocationType bottomRight = new LocationType(mapCenter);
            bottomRight.translatePosition(-mapHeight/2, mapWidth/2, 0);
            
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

                    int col = (int)(offsets[1] / cellSize);
                    int row = (int)(offsets[0] / cellSize);
                    pathHigh[col][row] = true;            
                    pathLow[col/10][row/10] = true;
                    highRes.addPoint(offsets[1], offsets[0], bp.depth);
                    lowRes.addPoint(offsets[1], offsets[0], bp.depth); 
                }
            }
        }
        
        for (int row = numRows-1; row >= 0; row--) {
            for (int col = 0; col < numCols; col++) {
                if (pathHigh[col][row])
                    System.out.print("*");
                else
                    System.out.print(" ");
            }
            System.out.println();
        }
        
        String dirOut = source.getFile("mra").getAbsolutePath()+"/noptilus";
        
        
        ColorMapUtils.generateColorMap(highRes.getDataPoints(), imgHigh.createGraphics(), numRows, numCols, 0, ColorMapFactory.createGrayScaleColorMap());
        JLabel lbl = new JLabel(new ImageIcon(imgHigh));
        ColorMapUtils.generateColorMap(lowRes.getDataPoints(), imgLow.createGraphics(), numRows/10, numCols/10, 0, ColorMapFactory.createGrayScaleColorMap());
        JLabel lbl2 = new JLabel(new ImageIcon(imgLow));

        GuiUtils.testFrame(lbl, "High resolution");
        GuiUtils.testFrame(lbl2, "Low resolution");
        
        try {            
            String desc = "Map centered in "+mapCenter.getLatitudeAsPrettyString()+" / "+mapCenter.getLongitudeAsPrettyString();
            ImageToFile(imgHigh, (float)cellSize, new File(dirOut+"/highres.txt"), desc);
            ImageToFile(imgLow, (float)cellSize*10, new File(dirOut+"/lowres.txt"), desc);
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass().getSimpleName()+"  while generating maps: "+e.getMessage();
        }
        return "Map files saved in "+dirOut;
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
                System.out.print(val / 255.0+" ");
                double value = (val * 20) / 255.0;
                bw.write((float)value+" ");                                
            }   
            bw.write("\n");
            System.out.println();
        }
        
        bw.close();
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "Normalized map export settings";
    }


    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getName() {
        return "Normalized map";
    }

}
