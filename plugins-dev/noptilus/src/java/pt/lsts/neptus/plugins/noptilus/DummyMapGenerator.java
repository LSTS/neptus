/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 5, 2013
 */
package pt.lsts.neptus.plugins.noptilus;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.vecmath.Point3d;

import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.colormap.DataDiscretizer;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class DummyMapGenerator {

    protected int rows = 200, cols = 200;
    
    protected double resolution = 0.5;

    protected Vector<Point3d> lowResData = new Vector<>();
    protected Vector<Point3d> highResData = new Vector<>();
    protected Vector<Point3d> noiseData = new Vector<>();
    
    public Vector<Point3d> generateRandomData(int numPoints) {
        Vector<Point3d> data = new Vector<>();
        Random rnd = new java.util.Random(System.currentTimeMillis());
        for (int i = 0; i < 50; i++) {
            int x = rnd.nextInt(rows);
            int y = rnd.nextInt(cols);
            double val = rnd.nextGaussian() * 20;
            data.add(new Point3d(x, y, val));  
        }
        return data;
    }
    
    public Vector<Point3d> generatePathData(int numPoints) {
        Vector<Point3d> data = new Vector<>();
        Random rnd = new java.util.Random(System.currentTimeMillis());
        for (int i = 0; i < 50; i++) {
            int x = rnd.nextInt(rows);
            int y = x;
            double val = rnd.nextGaussian() * 20;
            data.add(new Point3d(x, y, val));  
        }
        
        return data;
    }
    
    public void firstPhase() {
        noiseData = generateRandomData(25);
        lowResData = generatePathData(50);
        highResData = generatePathData(100);
        DataDiscretizer dd = new DataDiscretizer(20);
        for (Point3d pt : lowResData)
            dd.addPoint(pt.x, pt.y, pt.z);
        for (Point3d pt : noiseData)
            dd.addPoint(pt.x, pt.y, pt.z);
        
        BufferedImage img = new BufferedImage(rows, cols, BufferedImage.TYPE_INT_ARGB);
        ColorMapUtils.generateColorMap(dd.getDataPoints(), img.createGraphics(), rows, cols, 0, ColorMapFactory.createGrayScaleColorMap());
        JLabel lbl = new JLabel(new ImageIcon(img));
        
        GuiUtils.testFrame(lbl);
        highResData = generatePathData(50);
        
        dd = new DataDiscretizer(1);
        
        for (Point3d pt : noiseData)
            dd.addPoint(pt.x, pt.y, pt.z);
        
        for (Point3d pt : lowResData)
            dd.addPoint(pt.x, pt.y, pt.z);

        for (Point3d pt : highResData)
            dd.addPoint(pt.x, pt.y, pt.z);
        
        BufferedImage img2 = new BufferedImage(rows, cols, BufferedImage.TYPE_INT_ARGB);
        ColorMapUtils.generateColorMap(dd.getDataPoints(), img2.createGraphics(), rows, cols, 0, ColorMapFactory.createGrayScaleColorMap());
        JLabel lbl2 = new JLabel(new ImageIcon(img2));
        
        GuiUtils.testFrame(lbl2);

        try {
            PathToFile(lowResData, new File("path_lowres.txt"));
            highResData.addAll(lowResData);
            PathToFile(highResData, new File("path_highres.txt"));
            ImageToFile(img, new File("lowres.txt"), "Low resolution using 50 data points and cell width of 20 meters");
            ImageIO.write(img, "png", new File("lowres.png"));
            ImageToFile(img2, new File("highres.txt"), "High resolution using 150 data points and cell width of 1 meters");
            ImageIO.write(img2, "png", new File("highres.png"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void PathToFile(Vector<Point3d> points, File out) throws Exception {
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));
        bw.write("# Path generated by Neptus Dummy Map Generator\n");
        bw.write("200 200 0.5 0 0\n\n");

        int path[] = new int[cols];
        for (Point3d pt : points) {
            int i = (int)pt.x;
            path[i] = 1;
        }
        
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (x == y && path[x] == 1)
                    bw.write("1 ");
                else
                    bw.write("0 ");
            }   
            bw.write("\n");
            System.out.println();
        }
        
        bw.close();
    }
    
    public void ImageToFile(BufferedImage img, File out, String desc) throws Exception {
        BufferedWriter bw = new BufferedWriter(new FileWriter(out));
        bw.write("# Map generated by Neptus Dummy Map Generator\n");
        bw.write("# "+desc+"\n");
        bw.write("200 200 0.5 0 0\n\n");
        
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
    
    public static void main(String[] args) {
        new DummyMapGenerator().firstPhase();
    }
}
