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
 * 18 de Jun de 2011
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.UTMCoordinates;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author pdias
 *
 */
public class XYZUtils {

    
    /**
     * @param xyzData
     * @return
     */
    static double[][] invertDataSet(double[][] dataSet) {
        double[][] kdkdinv = new double[dataSet.length][dataSet[0].length];
        for (int i = 0; i < dataSet.length; i++) {
            for (int j = 0; j < dataSet.length; j++) {
                if (!Double.isNaN(dataSet[i][j]))
                    kdkdinv[i][j] = 1 - dataSet[i][j];
                else
                    kdkdinv[i][j] = Double.NaN;
            }
        }
        return kdkdinv;
    }

    /**
     * 
     */
    static double[] calcMinMaxOfVector(Vector<Double> xvec) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        for (double xt : xvec) {
            if (Double.isNaN(xt))
                continue;
            if (xt < minX)
                minX = xt;
            if (xt > maxX)
                maxX = xt;
        }
        return new double[] {minX, maxX};
    }
    
    /**
     * 
     */
    public static double[] calcWidthHeightScale(double dimX, double dimY, int targetImageWidth,
            int targetImageHeight) {
        double scale = 1;
        int width = targetImageWidth, height = targetImageHeight;
//        if (dimY < dimX) {
//            height = targetImageWidth;
//            width = (int) (dimY * height / dimX);
//            scale = dimX / height;
//        }
//        else {
//            width = targetImageHeight;
//            height = (int) (dimX * width / dimY);
//            scale = dimY / width;
//        }
//        return new double[] { width, height, scale };
        
        double ratio1 = (double)targetImageWidth/(double)targetImageHeight;
        double ratio2 = dimX/dimY;

        if (ratio2 < ratio1)        
            width = (int) (width / ratio1);
        else
            height = (int) (height * ratio1);
        scale = dimX / height;
        return new double[] { width, height, scale };
    }
    
    /**
     * Distribute values among cells.
     * 
     * @param gridSize
     * @param bounds
     * @param points
     * @param values
     * @return
     */
    static double[][] discretizeQuadMatrix(int gridSize, Rectangle2D bounds, Point2D[] points, Double[] values) {
        // scale to grid size
        double dx = bounds.getWidth() / gridSize;
        double dy = bounds.getHeight() / gridSize;
        
        Double[] vals = new Double[gridSize * gridSize]; // the mean of the values
        int numPoints[] = new int[gridSize * gridSize]; // number of measurements for that spot in grid
                
        for (int i = 0; i < points.length; i++) {
            Point2D pt = points[i];
            // calc index of point
            int indX = (int) Math.floor((pt.getX() - bounds.getMinX()) / dx);
            int indY = (int) Math.floor((pt.getY() - bounds.getMinY()) / dy);
            int ind = indX * gridSize + indY;
            ind = Math.min(ind, gridSize * gridSize - 1);
            
            if (vals[ind] == null)
                vals[ind] = values[i]; // if there is no value, initialize
            else
                vals[ind] = (numPoints[ind] * vals[ind] + values[i]) / (numPoints[ind] + 1); // Calculate mean
            numPoints[ind]++;
        }
        
        int sizeG = (int)Math.floor(Math.sqrt(vals.length));
        double[][] ret = new double[sizeG][sizeG];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret.length; j++) {
                ret[i][j] = (vals[i*ret.length+j]!=null)?vals[i*ret.length+j]:Double.NaN;
            }
        }

        return ret;
    }       

    public static void getInterpolatedData(double[][] data, ColorMap colormap, BufferedImage image, int alpha) {
        //BufferedImage tmp = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_ARGB);
        BufferedImage tmp = new BufferedImage(data[0].length, data.length, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                if (Double.isNaN(data[i][j]))
                    continue;
                Color c = colormap.getColor(data[i][j]);
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);

                //tmp.setRGB(i, j, c.getRGB());
                tmp.setRGB(j, data.length-1-i, c.getRGB());
            }
        }

        Graphics2D bg = image.createGraphics();
        //bg.scale(1,-1);
        bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        bg.scale((double)image.getWidth()/(double)tmp.getWidth(),
                (double)image.getHeight()/(double)tmp.getHeight());

        bg.drawImage(tmp, 0, 0, null);
        //bg.scale(1,-1);
    }

    /**
     * 
     */
    public static boolean saveImageToPNG(BufferedImage image, File destFile) {
        try {
            return ImageIO.write(image , "png", destFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean saveImageToJPG(BufferedImage image, File destFile) {
        try {
            return ImageIO.write(image, "jpg", destFile);
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param targetImageWidth 
     * @param targetImageHeight 
     * @param gridSize 
     * @return 
     * 
     */
    public static XYZDataType getInterpolatedData(LocationType baseLoc, Vector<Double> xvec,
            Vector<Double> yvec, Vector<Double> zvec, int targetImageWidth, int targetImageHeight, int gridSize) {
        // NeptusLog.pub().info("<###>Number of points: " + xvec.size());

        double[] xMinMaxVal = calcMinMaxOfVector(xvec);
        double minX = xMinMaxVal[0];
        double maxX = xMinMaxVal[1];

        double[] yMinMaxVal = calcMinMaxOfVector(yvec);
        double minY = yMinMaxVal[0];
        double maxY = yMinMaxVal[1];

        double[] zMinMaxVal = calcMinMaxOfVector(zvec);
        double minZ = zMinMaxVal[0];
        double maxZ = zMinMaxVal[1];

        // NeptusLog.pub().info("<###>x = ["+minX+","+maxX+"]"+"  \tdelta x = "+(Math.round(maxX-minX)));
        // NeptusLog.pub().info("<###>y = ["+minY+","+maxY+"]"+"  \tdelta y = "+(Math.round(maxY-minY)));
        // NeptusLog.pub().info("<###>z = ["+minZ+","+maxZ+"]"+"  \t\tdelta z = "+(Math.round(maxZ-minZ)));
        //
        double[] dim = {maxX-minX, maxY-minY};
        double[] widthHeightScaleVal = calcWidthHeightScale(dim[0], dim[1], targetImageWidth, targetImageHeight);
        int width = (int) widthHeightScaleVal[0];
        int height = (int) widthHeightScaleVal[1];
        double scale = widthHeightScaleVal[2];
        
        // NeptusLog.pub().info("<###>Scale: " + scale);
        
        LocationType topCorner = new LocationType(baseLoc);
        topCorner.translatePosition(maxX, minY, 0);
        // NeptusLog.pub().info("<###>Top Left Corner location = " + topCorner);

        LocationType centerLocation = new LocationType(baseLoc);
        centerLocation.translatePosition(maxX-((maxX-minX)/2), minY+((maxY-minY)/2), 0);
        // NeptusLog.pub().info("<###>Center location = " + centerLocation);

        Rectangle2D bounds = new Rectangle2D.Double(0, 0, maxX - minX, maxY - minY);
        // NeptusLog.pub().info("<###>BoundingBox: " + (maxX - minX) + " x " + (maxY - minY));

        Point2D[] points = new Point2D[xvec.size()];
        for (int i = 0; i < xvec.size(); i++) {
            points[i] = new Point2D.Double(xvec.get(i)-minX, yvec.get(i)-minY);
        }

        double[][] quadMatrixArray = discretizeQuadMatrix(gridSize, bounds, points, zvec.toArray(new Double[0]));
        
        // find maximum and minimum values in array
        double minZ1 = Double.POSITIVE_INFINITY;
        double maxZ1 = Double.NEGATIVE_INFINITY;
        for (double[] zt1 : quadMatrixArray) {
            for (double zt : zt1) {
                if (Double.isNaN(zt))
                    continue;
                if (zt < minZ1)
                    minZ1 = zt;
                if (zt > maxZ1)
                    maxZ1 = zt;
            }
        }

        // transform into values from 0 to 1 for colorMap
        for (int i = 0; i < quadMatrixArray.length; i++) {
            for (int j = 0; j < quadMatrixArray.length; j++) {
                if (!Double.isNaN(quadMatrixArray[i][j]))
                    quadMatrixArray[i][j] = /* 1 - */(quadMatrixArray[i][j] - minZ1) / (maxZ1 - minZ1);
                else
                    quadMatrixArray[i][j] = Double.NaN;
            }
        }
        
        XYZDataType xyzDataType = new XYZDataType();
        xyzDataType.topCornerLoc = topCorner;
        xyzDataType.centerLoc = centerLocation;
        xyzDataType.width = width;
        xyzDataType.height = height;
        xyzDataType.scale = scale;
        xyzDataType.dataSet = quadMatrixArray;
        xyzDataType.minX = minX;
        xyzDataType.maxX = maxX;
        xyzDataType.minY = minY;
        xyzDataType.maxY = maxY;
        xyzDataType.minZ = minZ;
        xyzDataType.maxZ = maxZ;
        
        return xyzDataType;
    }
    
    /**
     * 
     */
    static MapType getAsMapType(BufferedImage image, BufferedImage heightImage,
            String imageNameId, String mapBaseDir, LocationType centerLocation, double scale, double maxHeight, double maxDepth) {
        
        File baseMapDir = new File(mapBaseDir);
        baseMapDir.mkdirs();
        
        String imageFilePath = new File(baseMapDir, imageNameId + ".png").getAbsolutePath();
        String heightImageFilePath = new File(baseMapDir, imageNameId + "-height-map.png").getAbsolutePath();
        String mapFilePath = new File(baseMapDir, "map-" + imageNameId + ".nmap").getAbsolutePath();
        
        saveImageToPNG(image, new File(imageFilePath));
        saveImageToPNG(heightImage, new File(heightImageFilePath));
        
        MapType mapT = new MapType();
        mapT.setCenterLocation(centerLocation);
        CoordinateSystem cs = new CoordinateSystem();
        cs.setLocation(centerLocation);
        
        ImageElement imageElement = new ImageElement(MapGroup.getNewInstance(cs), mapT);
        imageElement.setId(imageNameId);
        imageElement.setCenterLocation(centerLocation);
        imageElement.setImageScale(scale);
        imageElement.setImage(image);
        imageElement.setImageFileName(imageFilePath);
        imageElement.setOriginalFilePath(imageElement.getImageFileName());
        imageElement.setBathymetric(true);
        imageElement.setHeightImage(heightImage);
        imageElement.setBathymetricImageFileName(heightImageFilePath); //No depth deve ser a inv-gray pois queremos valores mais altos sejam mais baixos
        imageElement.setMaxHeight(maxHeight);
        imageElement.setMaxDepth(maxDepth);
        imageElement.setTransparency(99);

        mapT.setId("map-" + imageNameId);
        mapT.setName("map-" + imageNameId);
        mapT.setOriginalFilePath(mapFilePath);
        mapT.setHref(mapFilePath);

        imageElement.setParentMap(mapT);
        
        mapT.addObject(imageElement);
        mapT.saveFile(mapFilePath);
        
        return mapT;
    }
    
    static void drawLegend(Graphics2D g, ColorMap cmap, String name, double scale,
            String unit, double min, double max) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setTransform(new AffineTransform());

        g.setColor(new Color(255,255,255,100));
        g.fillRoundRect(10, 10, 100, 170, 10, 10);

        ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
        cb.setSize(15, 80);
        g.setColor(Color.black);
        Font prev = g.getFont();
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        g.drawString(name, 15, 25);
        g.setFont(prev);

        g.translate(15, 45);

        cb.paint(g);

        g.translate(-10, -15);

        try {
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(max) + unit, 28, 20);
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format((max + min) / 2) + unit, 28, 60);
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(min) + unit, 28, 100);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        g.translate(10, 120);
        
        g.drawLine(0, -3, 0, 3);
        g.drawLine(0, 0, 90, 0);
        g.drawLine(90, -3, 90, 3);
        
        //double meters = scaleX * 90;
        g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(90d/scale) + " " + unit, 25, 15);
    }

    
    static void drawPath(Graphics2D g, double scaleX, double scaleY, double minX, double minY,
            double timeStep, IMraLogGroup logSource) {

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setTransform(new AffineTransform());
        g.setColor(new Color(0,0,0,10));

        IMraLog stateParser = logSource.getLog("EstimatedState");
        IMCMessage stateEntry;
        Point2D lastPt = null;
        stateEntry = stateParser.nextLogEntry();
        
        LocationType refLoc = LogUtils.getLocation(stateEntry).convertToAbsoluteLatLonDepth();
        
        boolean isImc5 = false;
        if (logSource.getLsfIndex().getDefinitions().getVersion().compareTo("5.0.0") >= 0)
            isImc5 = true;
        
        while (stateEntry != null) {
            LocationType loc = LogUtils.getLocation(stateEntry).convertToAbsoluteLatLonDepth();
            double[] xyz = loc.getOffsetFrom(refLoc);
            
            double north = isImc5 ? xyz[0] : stateEntry.getDouble("x");
            double east = isImc5 ? xyz[1] : stateEntry.getDouble("y");
            // NeptusLog.pub().info("<###>x=" + xyz[0] + "\t" + "y=" + xyz[1] + "\t\t" + "x=" + stateEntry.getDouble("x") + "\t" + "y=" + stateEntry.getDouble("y"));
            Point2D pt = new Point2D.Double((east - minY) * scaleY, (-minX-north) * scaleX);

            if (timeStep == 0)
                g.setColor(new Color(0,0,0,20));
            else
                g.setColor(Color.black);

            if (lastPt != null && pt != null)               
                g.draw(new Line2D.Double(lastPt, pt));
            lastPt = pt;
            
            if (timeStep == 0)
                stateEntry = stateParser.nextLogEntry();
            else {
                stateParser.advance((long)(timeStep*1000));
                stateEntry = stateParser.nextLogEntry();
            }
        }

    }
    
    public static void bathymCadiz() throws Exception {
        FileInputStream fis = new FileInputStream("/home/zp/Desktop/grid_5_cadizsub_UTM-29N.xyz");
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        Vector<Double> xvec = new Vector<Double>(), yvec = new Vector<Double>(), zvec = new Vector<Double>();

        LocationType ref = null;

        String line = br.readLine();
        while (line != null) {
            if (line.startsWith("#")) {}                
            else {
                String[] xt = line.split("[\t ,]");
                if (xt.length == 3) {
                    try {
                        double xx = Double.parseDouble(xt[0]);
                        double yy = Double.parseDouble(xt[1]);
                        double zz = Double.parseDouble(xt[2]);
                        
                        if (ref == null) {
                            UTMCoordinates utm = new UTMCoordinates(xx, yy, 29, 'N');
                            utm.UTMtoLL();
                            ref = new LocationType(utm.getLatitudeDegrees(), utm.getLongitudeDegrees());                            
                        }
                        
                        UTMCoordinates utm = new UTMCoordinates(xx, yy, 29, 'N');
                        utm.UTMtoLL();
                        LocationType tmp = new LocationType(utm.getLatitudeDegrees(), utm.getLongitudeDegrees());
                        
                        System.out.println(tmp);
                        double[] offsets = tmp.getOffsetFrom(ref);
                                
                        xvec.add(offsets[0]);
                        yvec.add(offsets[1]);
                        zvec.add(-zz);
                        
                    } catch (NumberFormatException e) {
                        e.getMessage();
                    }
                }
            }           
            line = br.readLine();
        }
        br.close();
        
        int targetImageWidth = 800;
        int targetImageHeight = 600;
        int gridSize = 297;
        XYZDataType xyzData = getInterpolatedData(ref, xvec, yvec, zvec, targetImageWidth,
                targetImageHeight, gridSize);

        BufferedImage destination = new BufferedImage(xyzData.width,
                xyzData.height, BufferedImage.TYPE_INT_ARGB);

        BufferedImage destination1 = new BufferedImage(xyzData.width,
                xyzData.height, BufferedImage.TYPE_INT_ARGB);

        BufferedImage destination2 = new BufferedImage(xyzData.width,
                xyzData.height, BufferedImage.TYPE_INT_ARGB);


        double[][] kdkdinv = invertDataSet(xyzData.dataSet);


        getInterpolatedData(xyzData.dataSet, ColorMapFactory.createJetColorMap(), destination, 255);
//        drawLegend((Graphics2D) destination.getGraphics(), ColorMapFactory.createJetColorMap(), 
//                "bathymetry", xyzData.scale, "m", xyzData.minZ, xyzData.maxZ);
        JLabel lblw = new JLabel(new ImageIcon(destination));
        GuiUtils.testFrame(lblw, "", 800, 600);

        getInterpolatedData(xyzData.dataSet, ColorMapFactory.createGrayScaleColorMap(), destination1, 255);
        JLabel lblw1 = new JLabel(new ImageIcon(destination1));
        GuiUtils.testFrame(lblw1, "", 800, 600);

        getInterpolatedData(kdkdinv, ColorMapFactory.createGrayScaleColorMap(), destination2, 255);
        JLabel lblw2 = new JLabel(new ImageIcon(destination2));
        GuiUtils.testFrame(lblw2, "", 800, 600);


        ImageIO.write(destination , "png", new File("cadiz-jet.png"));
        ImageIO.write(destination1, "png", new File("cadiz-gray.png"));
        ImageIO.write(destination2, "png", new File("cadiz-inv-gray.png"));

        MapType mapT = getAsMapType(destination, destination2, "Cadiz-Bat", ".", xyzData.centerLoc,
                xyzData.scale, xyzData.maxZ, xyzData.minZ);

        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(mapT.asXML()));
    }
    
    public static void main(String[] args) throws Exception {
        
        
        bathymCadiz();
        System.exit(0);
        
        double[] vec = calcWidthHeightScale(417.4842522414401, 417.05859590325053, 800, 600);
        NeptusLog.pub().info("<###> "+vec[0]);
        NeptusLog.pub().info("<###> "+vec[1]);
        NeptusLog.pub().info("<###> "+vec[2]);
        
        
        //41N09'35.293'' 08W41'35.721''
        FileInputStream fis = new FileInputStream("..\\Batimetria\\batimetria_leixoes.xyz");
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        Vector<Double> xvec = new Vector<Double>(), yvec = new Vector<Double>(), zvec = new Vector<Double>();
        String line = br.readLine();
        while (line != null) {
            if (line.startsWith("#")) {}                
            else {
                //NeptusLog.pub().info("<###> "+line);
                String[] xt = line.split("[\t ,]");
                if (xt.length == 3) {
                    try {
                        double xx = Double.parseDouble(xt[1]);
                        double yy = Double.parseDouble(xt[0]);
                        double zz = Double.parseDouble(xt[2]);
                        
                        
                        
                        
                        xvec.add(xx);
                        yvec.add(yy);
                        zvec.add(-zz);
                    } catch (NumberFormatException e) {
                        e.getMessage();
                    }
                }
            }           
            line = br.readLine();
        }
        br.close();

        LocationType baseLoc = new LocationType();
        baseLoc.setLatitudeStr("41N09'35.293''");// 41º09'35.293"N
        baseLoc.setLongitudeStr("08W41'35.721''");
        baseLoc.translatePosition(-465778.48, -152987.42, 0);
        //baseLoc = (LocationType) baseLoc.convertToAbsoluteLatLonDepth();
        NeptusLog.pub().info("<###>Base location = " + baseLoc);

        int targetImageWidth = 800;
        int targetImageHeight = 600;
        int gridSize = 297;
        XYZDataType xyzData = getInterpolatedData(baseLoc, xvec, yvec, zvec, targetImageWidth,
                targetImageHeight, gridSize);

        BufferedImage destination = new BufferedImage(xyzData.width,
                xyzData.height, BufferedImage.TYPE_INT_ARGB);

        BufferedImage destination1 = new BufferedImage(xyzData.width,
                xyzData.height, BufferedImage.TYPE_INT_ARGB);

        BufferedImage destination2 = new BufferedImage(xyzData.width,
                xyzData.height, BufferedImage.TYPE_INT_ARGB);


        double[][] kdkdinv = invertDataSet(xyzData.dataSet);


        getInterpolatedData(xyzData.dataSet, ColorMapFactory.createJetColorMap(), destination, 255);
//        drawLegend((Graphics2D) destination.getGraphics(), ColorMapFactory.createJetColorMap(), 
//                "bathymetry", xyzData.scale, "m", xyzData.minZ, xyzData.maxZ);
        JLabel lblw = new JLabel(new ImageIcon(destination));
        GuiUtils.testFrame(lblw, "", 800, 600);

        getInterpolatedData(xyzData.dataSet, ColorMapFactory.createGrayScaleColorMap(), destination1, 255);
        JLabel lblw1 = new JLabel(new ImageIcon(destination1));
        GuiUtils.testFrame(lblw1, "", 800, 600);

        getInterpolatedData(kdkdinv, ColorMapFactory.createGrayScaleColorMap(), destination2, 255);
        JLabel lblw2 = new JLabel(new ImageIcon(destination2));
        GuiUtils.testFrame(lblw2, "", 800, 600);


        ImageIO.write(destination , "png", new File("bathyLeixoes-jet.png"));
        ImageIO.write(destination1, "png", new File("bathyLeixoes-gray.png"));
        ImageIO.write(destination2, "png", new File("bathyLeixoes-inv-gray.png"));

        MapType mapT = getAsMapType(destination, destination2, "APDL-Bat", ".", xyzData.centerLoc,
                xyzData.scale, xyzData.maxZ, xyzData.minZ);

        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(mapT.asXML()));
    }
}