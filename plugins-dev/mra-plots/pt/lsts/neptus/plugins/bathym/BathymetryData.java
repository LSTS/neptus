/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 16, 2020
 */
package pt.lsts.neptus.plugins.bathym;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;

/**
 * This class is used to process data from one or more XYZ files into a raster (gridded file)
 * @author zp
 *
 */
public class BathymetryData implements Serializable {

    private static final long serialVersionUID = 2964953368936853050L;
    private double minX, maxX, stepX, minY, maxY, stepY;
    private String crs;
    private final boolean DEBUG = false;
    private DataPoint[][] data;
    private int ncols, nrows;

    private transient ConcurrentHashMap<String, CoordTranslator> inboundTranslators = new ConcurrentHashMap<String, CoordTranslator>();

    public BathymetryData(String crs, double minX, double maxX, double stepX, double minY, double maxY, double stepY) {

        this.minX = minX;
        this.maxX = maxX;
        this.stepX = stepX;

        this.minY = minY;
        this.maxY = maxY;
        this.stepY = stepY;

        this.crs = crs;

        ncols = (int) Math.ceil((maxX - minX) / stepX);
        nrows = (int) Math.ceil((maxY - minY) / stepY);
        data = new DataPoint[ncols][nrows];
        if (DEBUG) {
            NeptusLog.pub().debug("Created a structure of " + ncols + "x" + nrows + " entries");
        }
    }

    public double addSample(String crs, double x, double y, double z) {
        CoordTranslator trans = inboundTranslators.computeIfAbsent(crs, c -> new CoordTranslator(c, this.crs));
        double[] coord = trans.translate(x, y);
        if (DEBUG)
            System.out.println(coord[0] + "," + coord[1] + " = " + z);
        int col = (int) ((coord[0] - minX) / stepX);
        int row = (int) ((coord[1] - minY) / stepY);

        if (col > ncols) {
            if (DEBUG)
                System.err.println("X value (" + coord[0] + ") is bigger than " + maxX);
            return 0;
        }

        if (row > nrows) {
            if (DEBUG)
                System.err.println("Y value (" + coord[1] + ") is bigger than " + maxY);
            return 0;
        }

        if (data[col][row] == null)
            data[col][row] = new DataPoint();

        double ret = 0;
        if (data[col][row].count > 0) {
            ret = data[col][row].average - z;            
        }
        
        data[col][row].average = (data[col][row].average * data[col][row].count + z) / (++data[col][row].count);

        if (DEBUG)
            System.out.println((minX + stepX * row) + "," + (minY + stepY * row) + " :: " + data[col][row].count
                    + " := " + data[col][row].average);

        return ret;
    }

    public void store(File out) throws FileNotFoundException, IOException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(out));
        oos.writeObject(this);
        oos.close();
    }

    public static BathymetryData load(File in) throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(in));
        BathymetryData ret = (BathymetryData) ois.readObject();
        ret.inboundTranslators = new ConcurrentHashMap<String, CoordTranslator>();
        ois.close();
        return ret;
    }

    public void process(File f) {
        
        if (f.isDirectory()) {
            System.out.println("Processing folder " + f);
            for (File sub : f.listFiles()) {
                process(sub);
            }
            return;
        }
        else if (f.getName().toUpperCase().endsWith(".XYZ")) {
            System.out.println("Processing file " + f);
            double offset = 0;
            int count = 0;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(f));

                String line = reader.readLine();

                while (line != null) {
                    String[] parts = line.split("\\t");
                    line = reader.readLine();
                    double x = Double.valueOf(parts[0]);
                    double y = Double.valueOf(parts[1]);
                    double z = Double.valueOf(parts[2]);
                    try {
                        double off = addSample(CoordTranslator.CRS_WGS84, y, x, z+offset);
                        if (off > 3) {
                            offset = ((offset * count) + off) / (++count);
                            System.out.println("offset: "+offset);                            
                        }
                    }
                    catch (Exception e) {
                        System.err.println("Invalid line: '" + line + "' (" + e.getMessage() + ")");
                        e.printStackTrace();
                    }

                }
                reader.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void writeImage(ColorMap colormap, double minZ, double maxZ, boolean transparent, File out)
            throws IOException {
        BufferedImage img = new BufferedImage(ncols, nrows,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        double extent = (maxZ - minZ);
        for (int x = 0; x < ncols; x++) {
            System.out.println(x + "...");
            for (int y = 0; y < nrows; y++) {
                DataPoint dp = data[x][y];
                if (dp != null) {
                    double val = (dp.average - minZ) / extent;
                    img.setRGB(x, y, colormap.getColor(val).getRGB());
                }
            }
        }

        ImageIO.write(img, "PNG", out);
    }
    
    public void writeXyz(File out) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        
        for (int y = 0; y < nrows; y++) {
            for (int x = 0; x < ncols; x++) {
                DataPoint dp = data[x][y];
                if (dp != null) {
                    writer.write(String.format("%.0f %.0f %.2f\n", minX+x*stepX, minY+y*stepY, data[x][y].average));
                }
            }
        }
        
        writer.close();
    }

    static class DataPoint implements Serializable {
        private static final long serialVersionUID = -5034715092526074440L;
        int count = 0;
        double average = 0;
    }

    public static void main(String[] args) throws Exception {
        // BathymetryData omare = new BathymetryData(CoordTranslator.CRS_ETRS89, 2759000, 2769000, 5, 2235000, 2255000, 5);
        
        System.out.println("reading from disk");

        //BathymetryData d = BathymetryData.load(new File("/home/zp/Desktop/pnln5.bathym"));
        //d.writeXyz(new File("/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/bathymetry.xyz"));
        
        /*System.out.println("adding data");
        d.process(new File("/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Batimetria/XT2/"));
        System.out.println("writing to disk");
        d.store(new File("/home/zp/Desktop/pnln-all.bathym"));
        System.out.println("generating image");
        d.writeImage(ColorMapFactory.createRedGreenBlueColorMap(), -20, 50, true, new File("/home/zp/Desktop/pnln-all.png"));*/
        // OMARE_BATHYM.process(new File("/home/zp/Desktop/Bathymetry/kongsberg"));
        // OMARE_BATHYM.store(new File("/home/zp/Desktop/polis5.bathym"));
        // OMARE_BATHYM.writeImage(ColorMapFactory.createJetColorMap(), 0, 50, new File("/home/zp/Desktop/polis5.png"));
    }
}
