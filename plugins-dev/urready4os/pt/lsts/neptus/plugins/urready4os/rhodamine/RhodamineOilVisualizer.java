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
 * Author: pdias
 * Sep 24, 2014
 */
package pt.lsts.neptus.plugins.urready4os.rhodamine;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import pt.lsts.imc.CrudeOil;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FineOil;
import pt.lsts.imc.RhodamineDye;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.urready4os.rhodamine.importers.CSVDataParser;
import pt.lsts.neptus.plugins.urready4os.rhodamine.importers.MedslikDataParser;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

import com.google.common.eventbus.Subscribe;

/**
 * @author pdias
 *
 */
@PluginDescription(name="Rhodamine Oil Visualizer", author="Paulo Dias", version="0.2", icon = "pt/lsts/neptus/plugins/urready4os/urready4os.png")
@LayerPriority(priority = -50)
public class RhodamineOilVisualizer extends ConsoleLayer implements ConfigurationListener {

    @NeptusProperty(name = "Show rhodamine dye", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showRhodamine = true;

    @NeptusProperty(name = "Show crude oil", userLevel = LEVEL.REGULAR, category="Visibility", editable = false)
    public boolean showCrudeOil = false;

    @NeptusProperty(name = "Show refine oil", userLevel = LEVEL.REGULAR, category="Visibility", editable = false)
    public boolean showRefineOil = false;
    
    @NeptusProperty(name = "Minimum value", userLevel = LEVEL.REGULAR, category="Scale")
    public int minValue = 0;

    @NeptusProperty(name = "Maximum value", userLevel = LEVEL.REGULAR, category="Scale")
    public int maxValue = 10;

    @NeptusProperty(name = "Colormap", userLevel = LEVEL.REGULAR, category="Scale")
    private final ColorMap colorMap = ColorMapFactory.createJetColorMap();
    
    @NeptusProperty(name = "Clear data", userLevel = LEVEL.REGULAR, category="Reset")
    public boolean clearData = false;

    @NeptusProperty(name = "Pixel size data", userLevel = LEVEL.REGULAR, category="Scale")
    public int pixelSizeData = 4;
    
    @NeptusProperty(name = "Base folder for CSV files", userLevel = LEVEL.REGULAR, category = "Data Update")
    public File baseFolderForCSVFiles = new File("log/rhodamine");
    
    @NeptusProperty(name = "Period seconds to update", userLevel = LEVEL.REGULAR, category = "Data Update")
    private int periodSecondsToUpdate = 30;
    
    @NeptusProperty(userLevel = LEVEL.REGULAR, category = "Data Cleanup")
    private boolean autoCleanData = false;
    
    @NeptusProperty(userLevel = LEVEL.REGULAR, category = "Data Cleanup")
    private int dataAgeToCleanInMinutes = 120;
    
    @NeptusProperty(name = "Prediction file", userLevel = LEVEL.REGULAR, category = "Prediction")
    public File predictionFile = new File("log/rhodamine-prediction/current.tot");

//    @NeptusProperty(name = "Show Prediction", userLevel = LEVEL.REGULAR, category = "Prediction")
    public boolean showPrediction = false;

  @NeptusProperty(name = "Prediction scale factor", userLevel = LEVEL.REGULAR, category = "Prediction")
  public double predictionScaleFactor = 1;


    private final PrevisionRhodamineConsoleLayer previsionLayer = new PrevisionRhodamineConsoleLayer();

    
//    private EstimatedState lastEstimatedState = null;
//    private RhodamineDye lastRhodamineDye = null;
//    private CrudeOil lastCrudeOil = null;
//    private FineOil lastFineOil = null;
    
    private static final String csvFilePattern = ".\\.csv$";
    private static final String[] totFileExt = { "tot", "lv1" };

    // Cache image
    private BufferedImage cacheImg = null;
    private BufferedImage cachePredictionImg = null;
    private static int offScreenBufferPixel = 400;
    private Dimension dim = null;
    private int lastLod = -1;
    private LocationType lastCenter = null;
    private double lastRotation = Double.NaN;
    private BufferedImage cacheColorBarImg = null;

    private boolean clearImgCachRqst = false;
    private boolean clearColorBarImgCachRqst = false;
    
    private Ellipse2D circle = new Ellipse2D.Double(-4, -4, 8, 8);

    private ArrayList<BaseData> dataList = new ArrayList<>();
    private ArrayList<BaseData> dataPredictionList = new ArrayList<>();
    
    private HashMap<Integer, EstimatedState> lastEstimatedStateFromSystems = new HashMap<>();

    private long lastUpdatedValues = -1;

    public RhodamineOilVisualizer() {
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
//        try {
//            CSVDataParser csv = new CSVDataParser(new File("test.csv"));
//            CSVDataParser csv = new CSVDataParser(new File("log_2014-09-24_22-15.csv"));
//            csv.parse();
//            dataList.addAll(csv.getPoints());
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        
        getConsole().addMapLayer(previsionLayer, false);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        dataList.clear();
        lastEstimatedStateFromSystems.clear();

        getConsole().removeMapLayer(previsionLayer);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        clearImgCachRqst = true;
        clearColorBarImgCachRqst = true;
        
        if (clearData) {
            dataList.clear();
            lastEstimatedStateFromSystems.clear();
            clearData = false;
        }
        
        circle = new Ellipse2D.Double(-pixelSizeData / 2d, -pixelSizeData / 2d, pixelSizeData, pixelSizeData);
        
        if (minValue > maxValue)
            minValue = maxValue;
        
        if (maxValue < minValue)
            maxValue = minValue;
    }

    public String validatePixelSizeData(int value) {
        return new IntegerMinMaxValidator(2, 8).validate(value);
    }

    public String validateMinValue(int value) {
        return new IntegerMinMaxValidator(0, false).validate(value);
    }
    
    public String validatePeriodSecondsToUpdate(int value) {
        return new IntegerMinMaxValidator(5, false).validate(value);
    }
    
    public String validateDataAgeToCleanInMinutes(int value) {
        return new IntegerMinMaxValidator(1, false).validate(value);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }
    
    @Periodic(millisBetweenUpdates=1000)
    public void update() {
        long curTime = System.currentTimeMillis();
        if (curTime - lastUpdatedValues > periodSecondsToUpdate * 1000) {
            lastUpdatedValues = curTime;
            boolean ret = updateValues();
            if (ret) {
                invalidateCache();
            }
        }
    }

    private void invalidateCache() {
        clearImgCachRqst = true;
    }

    public boolean updateValues() {
        File[] fileList = FileUtil.getFilesFromDisk(baseFolderForCSVFiles, csvFilePattern);
        if (fileList != null && fileList.length > 0) {
            File csvFx = fileList[fileList.length -1];
            try {
                CSVDataParser csv = new CSVDataParser(csvFx);
                csv.parse();
                updateValues(dataList, csv.getPoints());
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        
        File[] folders = FileUtil.getFoldersFromDisk(baseFolderForCSVFiles, null);
        for (File folder : folders) {
            fileList = FileUtil.getFilesFromDisk(folder, csvFilePattern);
            if (fileList != null && fileList.length > 0) {
                File csvFx = fileList[fileList.length -1];
                try {
                    CSVDataParser csv = new CSVDataParser(csvFx);
                    csv.parse();
                    updateValues(dataList, csv.getPoints());
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        
        
        // Load prediction
        if (predictionFile.exists() && predictionFile.isFile()) {
            String fxExt = FileUtil.getFileExtension(predictionFile);
            if ("csv".equalsIgnoreCase(fxExt)) {
                try {
                    CSVDataParser csv = new CSVDataParser(predictionFile);
                    csv.parse();
                    dataPredictionList.clear();
                    updateValues(dataPredictionList, csv.getPoints());
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    MedslikDataParser totFile = new MedslikDataParser(predictionFile);
                    totFile.parse();
                    dataPredictionList.clear();
                    updateValues(dataPredictionList, totFile.getPoints());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        return true;
    }

    /**
     * @param list
     * @param points
     */
    private boolean updateValues(ArrayList<BaseData> list, ArrayList<BaseData> points) {
        boolean dataUpdated = false;
        
        if (autoCleanData) {
            long curTimeMillis = System.currentTimeMillis();
            for (BaseData bd : points.toArray(new BaseData[points.size()])) {
                if (curTimeMillis - bd.getTimeMillis() > dataAgeToCleanInMinutes * DateTimeUtil.MINUTE)
                    points.remove(bd);
            }
            for (BaseData bd : list.toArray(new BaseData[list.size()])) {
                if (curTimeMillis - bd.getTimeMillis() > dataAgeToCleanInMinutes * DateTimeUtil.MINUTE)
                    list.remove(bd);
            }
        }
        
        for (BaseData testPoint : points) {
            int counter = 0;
            boolean found = false;
            for (BaseData toTestPoint : list) {
                if (toTestPoint.equals(testPoint)) {
                    if (toTestPoint.getTimeMillis() < testPoint.getTimeMillis()) {
                        list.remove(counter);
                        list.add(counter, testPoint);
                        dataUpdated = true;
                    }
//                    System.out.println("######### " + counter);
                    found = true;
                    break;
                    
                }
                counter++;
            }
            if (!found) {
                list.add(testPoint);
                dataUpdated = true;
            }
        }
        System.out.println("List size: " + list.size());
        return dataUpdated;
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        
        if (!clearImgCachRqst) {
            if (isToRegenerateCache(renderer)) {
                cacheImg = null;
                cachePredictionImg = null;
            }
        }
        else {
            cacheImg = null;
            cachePredictionImg = null;
            clearImgCachRqst = false;
        }
        
        if (cacheImg == null) {
            dim = renderer.getSize(new Dimension());
            lastLod = renderer.getLevelOfDetail();
            lastCenter = renderer.getCenter();
            lastRotation = renderer.getRotation();

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            cacheImg = gc.createCompatibleImage((int) dim.getWidth() + offScreenBufferPixel * 2, (int) dim.getHeight() + offScreenBufferPixel * 2, Transparency.BITMASK); 
            Graphics2D g2 = cacheImg.createGraphics();
            
            g2.translate(offScreenBufferPixel, offScreenBufferPixel);
            
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            
            paintData(renderer, g2);
            
            g2.dispose();
        }

        if (cachePredictionImg == null) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            cachePredictionImg = gc.createCompatibleImage((int) dim.getWidth() + offScreenBufferPixel * 2, (int) dim.getHeight() + offScreenBufferPixel * 2, Transparency.BITMASK); 
            Graphics2D g2 = cachePredictionImg.createGraphics();
            
            g2.translate(offScreenBufferPixel, offScreenBufferPixel);
            
//            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            
            paintPredictionData(renderer, g2);
            
            g2.dispose();
        }

//        if (cachePredictionImg != null && showPrediction) {
//            //          System.out.println(".........................");
//            Graphics2D g3 = (Graphics2D) g.create();
//
//            double[] offset = renderer.getCenter().getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());
//            offset = AngleCalc.rotate(renderer.getRotation(), offset[0], offset[1], true);
//
//            g3.drawImage(cachePredictionImg, null, (int) offset[0] - offScreenBufferPixel, (int) offset[1] - offScreenBufferPixel);
//
//            g3.dispose();
//        }

        if (cacheImg != null) {
            //          System.out.println(".........................");
            Graphics2D g3 = (Graphics2D) g.create();

            double[] offset = renderer.getCenter().getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());
            offset = AngleCalc.rotate(renderer.getRotation(), offset[0], offset[1], true);

            g3.drawImage(cacheImg, null, (int) offset[0] - offScreenBufferPixel, (int) offset[1] - offScreenBufferPixel);

            g3.dispose();
        }
        
        paintColorBar(renderer, g);
        
        // Legend
        Graphics2D gl = (Graphics2D) g.create();
        gl.translate(10, 50);
        gl.setColor(Color.WHITE);
        gl.drawString(getName(), 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.dispose();
    }

    public void paintPrediction(Graphics2D g, StateRenderer2D renderer) {
        if (cachePredictionImg != null) {
            //          System.out.println(".........................");
            Graphics2D g3 = (Graphics2D) g.create();

            double[] offset = renderer.getCenter().getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());
            offset = AngleCalc.rotate(renderer.getRotation(), offset[0], offset[1], true);

            g3.drawImage(cachePredictionImg, null, (int) offset[0] - offScreenBufferPixel, (int) offset[1] - offScreenBufferPixel);

            g3.dispose();
        }

        paintColorBar(renderer, g);
        
        // Legend
        Graphics2D gl = (Graphics2D) g.create();
        gl.translate(10, 50);
        gl.setColor(Color.WHITE);
        gl.drawString(getName(), 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.dispose();
    }

    private void paintColorBar(StateRenderer2D renderer, Graphics2D g2) {
        if (clearColorBarImgCachRqst) {
            cacheColorBarImg = null;
            clearColorBarImgCachRqst = false;
        }
        
        if (cacheColorBarImg == null) {
//            BufferedImage img = new BufferedImage(100, 170, BufferedImage.TYPE_INT_ARGB);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            cacheColorBarImg = gc.createCompatibleImage(100, 170, Transparency.BITMASK); 

            Graphics2D g = (Graphics2D) cacheColorBarImg.getGraphics();
            g.setColor(new Color(255, 255, 255, 100));
            g.fillRect(5, 30, 70, 110);

            ColorMap cmap = colorMap;
            ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
            cb.setSize(15, 80);
            g.setColor(Color.WHITE);
            Font prev = g.getFont();
            g.setFont(new Font("Helvetica", Font.BOLD, 18));
            g.setFont(prev);
            g.translate(15, 45);
            cb.paint(g);
            g.translate(-10, -15);

            try {
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 28, 20);
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue / 2), 28, 60);
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 28, 100);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }
        }
        
        if (cacheColorBarImg != null) {
            Graphics2D g3 = (Graphics2D) g2.create();
//            double[] offset = renderer.getCenter().getDistanceInPixelTo(lastCenter, renderer.getLevelOfDetail());
//            offset = AngleCalc.rotate(renderer.getRotation(), offset[0], offset[1], true);
            g3.drawImage(cacheColorBarImg, null, 10, 20);
            g3.dispose();
        }
    }
    
    private void paintData(StateRenderer2D renderer, Graphics2D g2) {
        paintDataWorker(renderer, g2, false, dataList);
    }

    private void paintPredictionData(StateRenderer2D renderer, Graphics2D g2) {
        paintDataWorker(renderer, g2, true, dataPredictionList);
    }

    private void paintDataWorker(StateRenderer2D renderer, Graphics2D g2, boolean prediction, ArrayList<BaseData> dList) {
        LocationType loc = new LocationType();

        long curtime = System.currentTimeMillis();
        
        for (BaseData point : dList.toArray(new BaseData[dList.size()])) {
            double latV = point.getLat();
            double lonV = point.getLon();
            
            if (Double.isNaN(latV) || Double.isNaN(lonV))
                continue;
            
            loc.setLatitudeDegs(latV);
            loc.setLongitudeDegs(lonV);
            
            Point2D pt = renderer.getScreenPosition(loc);

            if (!isVisibleInRender(pt, renderer))
                continue;
            
            if (Double.isNaN(point.getRhodamineDyePPB()) || Double.isInfinite(point.getRhodamineDyePPB()))
                continue;
            
            if (point.getRhodamineDyePPB() < minValue)
                continue;
            
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(pt.getX(), pt.getY());
            Color color = colorMap.getColor((point.getRhodamineDyePPB() * (prediction ? predictionScaleFactor : 1) - minValue) / maxValue);
            if (!prediction) {
                if (curtime - point.getTimeMillis() > DateTimeUtil.MINUTE * 5)
                    color = ColorUtils.setTransparencyToColor(color, 150); // 128
            }
            else {
                color = ColorUtils.setTransparencyToColor(color, 128);
            }
            gt.setColor(color);
            
            // double rot =  -renderer.getRotation();
            // gt.rotate(rot);
            gt.fill(circle);
            //gt.rotate(-rot);
                        
            gt.dispose();
        }
    }

    /**
     * @return 
     * 
     */
    private boolean isToRegenerateCache(StateRenderer2D renderer) {
        if (dim == null || lastLod < 0 || lastCenter == null || Double.isNaN(lastRotation)) {
            Dimension dimN = renderer.getSize(new Dimension());
            if (dimN.height != 0 && dimN.width != 0)
                dim = dimN;
            return true;
        }
        LocationType current = renderer.getCenter().getNewAbsoluteLatLonDepth();
        LocationType last = lastCenter == null ? new LocationType(0, 0) : lastCenter;
        double[] offset = current.getDistanceInPixelTo(last, renderer.getLevelOfDetail());
        if (Math.abs(offset[0]) > offScreenBufferPixel || Math.abs(offset[1]) > offScreenBufferPixel) {
            return true;
        }

        if (!dim.equals(renderer.getSize()) || lastLod != renderer.getLevelOfDetail()
                /*|| !lastCenter.equals(renderer.getCenter())*/ || Double.compare(lastRotation, renderer.getRotation()) != 0) {
            return true;
        }
        
        return false;
    }

    @Subscribe
    public void on(EstimatedState msg) {
        // From any system
//        System.out.println(msg.asJSON());
    }

    @Subscribe
    public void on(RhodamineDye msg) {
        // From any system
//        System.out.println(msg.asJSON());
    }

    @Subscribe
    public void on(CrudeOil msg) {
        // From any system
//        System.out.println(msg.asJSON());
    }

    @Subscribe
    public void on(FineOil msg) {
        // From any system
//        System.out.println(msg.asJSON());
    }

    /**
     * @param sPos
     * @param renderer
     * @return
     */
    private boolean isVisibleInRender(Point2D sPos, StateRenderer2D renderer) {
        Dimension rendDim = renderer.getSize();
        if (sPos.getX() < 0 - offScreenBufferPixel && sPos.getY() < 0 - offScreenBufferPixel)
            return false;
        else if (sPos.getX() > rendDim.getWidth() + offScreenBufferPixel && sPos.getY() > rendDim.getHeight() + offScreenBufferPixel)
            return false;
        
        return true;
    }

//    private FileReader getFileReaderForFile(String fileName) {
//        // InputStreamReader
//        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
//        if (fxName == null)
//            fxName = fileName;
//        File fx = new File(fxName);
//        try {
//            FileReader freader = new FileReader(fx);
//            return freader;
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
    
    @PluginDescription(name="Rhodamine Oil Prevision Visualizer", icon = "pt/lsts/neptus/plugins/urready4os/urready4os.png")
    @LayerPriority(priority = -51)
    private class PrevisionRhodamineConsoleLayer extends ConsoleLayer {
        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
        }
        
        @Override
        public boolean userControlsOpacity() {
            return false;
        }

        @Override
        public void initLayer() {
        }

        @Override
        public void cleanLayer() {
        }
        
        /* (non-Javadoc)
         * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
         */
        @Override
        public void paint(Graphics2D g, StateRenderer2D renderer) {
            super.paint(g, renderer);
            
            paintPrediction(g, renderer);
        }
    }
}
