/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.CrudeOil;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FineOil;
import pt.lsts.imc.RhodamineDye;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.gui.swing.RangeSlider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.urready4os.rhodamine.importers.CSVDataParser;
import pt.lsts.neptus.plugins.urready4os.rhodamine.importers.MedslikDataParser;
import pt.lsts.neptus.plugins.urready4os.vtk.PointCloudRhodamine;
import pt.lsts.neptus.plugins.urready4os.vtk.Rhodamine3DPanel;
import pt.lsts.neptus.plugins.urready4os.vtk.RhodaminePointCloudLoader;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

/**
 * @author pdias
 *
 */
@PluginDescription(name="Rhodamine Oil Visualizer", author="Paulo Dias", version="0.8", 
    icon = "pt/lsts/neptus/plugins/urready4os/urready4os.png")
@LayerPriority(priority = -50)
public class RhodamineOilVisualizer extends ConsoleLayer implements ConfigurationListener {

//    @NeptusProperty(name = "Show rhodamine dye", userLevel = LEVEL.REGULAR, category="Visibility", editable = false)
//    private boolean showRhodamine = true;
//
//    @NeptusProperty(name = "Show crude oil", userLevel = LEVEL.REGULAR, category="Visibility", editable = false)
//    private boolean showCrudeOil = false;
//
//    @NeptusProperty(name = "Show refine oil", userLevel = LEVEL.REGULAR, category="Visibility", editable = false)
//    private boolean showRefineOil = false;
    
    @NeptusProperty(name = "Minimum value", userLevel = LEVEL.REGULAR, category="Scale")
    private double minValue = 0;

    @NeptusProperty(name = "Maximum value", userLevel = LEVEL.REGULAR, category="Scale")
    private double maxValue = 100;

    @NeptusProperty(name = "Colormap", userLevel = LEVEL.REGULAR, category="Scale")
    private final ColorMap colorMap = ColorMapFactory.createJetColorMap();
    
    @NeptusProperty(name = "Clear data", userLevel = LEVEL.REGULAR, category="Reset")
    private boolean clearData = false;

    @NeptusProperty(name = "Pixel size data", userLevel = LEVEL.REGULAR, category="Scale")
    private int pixelSizeData = 4;
    
    @NeptusProperty(name = "Base folder for CSV files", userLevel = LEVEL.REGULAR, category = "Data Update",
            description = "Defines the base folder fo CSV lookup. The children folders will also be considered. (Be aware of the \"Read all or last of ordered files\" flag.)",
            editorClass = FolderPropertyEditor.class)
    private File baseFolderForCSVFiles = new File("log/rhodamine");
    
    @NeptusProperty(name = "Period seconds to update", userLevel = LEVEL.REGULAR, category = "Data Update")
    private int periodSecondsToUpdate = 60;
    
    @NeptusProperty(userLevel = LEVEL.REGULAR, category = "Data Cleanup")
    private boolean autoCleanData = false;
    
    @NeptusProperty(userLevel = LEVEL.REGULAR, category = "Data Cleanup")
    private int dataAgeToCleanInMinutes = 120;
    
    @NeptusProperty(name = "Prediction file or folder (tot or lv* files)", userLevel = LEVEL.REGULAR, category = "Prediction")
    private File predictionFile = new File("log/rhodamine-prediction");

//    @NeptusProperty(name = "Show Prediction", userLevel = LEVEL.REGULAR, category = "Prediction")
//    private boolean showPrediction = false;

    @NeptusProperty(name = "Prediction scale factor", userLevel = LEVEL.REGULAR, category = "Prediction")
    private double predictionScaleFactor = 100;

    @NeptusProperty(name = "Read all or last of ordered files", userLevel = LEVEL.REGULAR, category = "Data Update",
            description = "True to read all CSV files of just the last of ordered files in folder.")
    private boolean readAllOrLastOfOrderedFiles = true;
    
    @NeptusProperty(name = "Max delta time between EstimatedState and message data received in millis", 
            userLevel = LEVEL.REGULAR, category = "Data Update",
            description = "If the maximum time difference allowed between EstimatedState and message data to data to be accepted.")
    private long maxDeltaTimeBetweenEstimatedStateAndMessageDataReceivedMillis = 200;
    
    private final PrevisionRhodamineConsoleLayer previsionLayer = new PrevisionRhodamineConsoleLayer();

    private long lastPaintMillis = -1;
    private long lastPaintDataMillis = -1;
    private long lastPaintPredictonMillis = -1;
    
    private HashMap<Integer, EstimatedState> lastEstimatedStateList = new HashMap<>();
    private HashMap<Integer, ArrayList<BaseData>> lastRhodamineDyeList = new HashMap<>();
    
    private static final String csvFilePattern = ".\\.csv$";
    private static final String totFilePattern = "(.\\.tot$)|(.\\.lv\\d$)";

    // Cache image
    private OffScreenLayerImageControl offScreenImageControlData = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenImageControlPrediction = new OffScreenLayerImageControl();
    private OffScreenLayerImageControl offScreenImageControlColorBar = new OffScreenLayerImageControl();
    
    private boolean clearImgCachRqst = false;
    private boolean clearColorBarImgCachRqst = false;
    
    private Ellipse2D circle = new Ellipse2D.Double(-4, -4, 8, 8);

    private ArrayList<BaseData> dataList = new ArrayList<>(); //Collections.synchronizedList(new ArrayList<>());
    private ArrayList<BaseData> dataPredictionList = new ArrayList<>();
    private long dataPredictionMillisPassedFromSpillMax = 0;
    private ArrayList<Long> dataPredictionValues = new ArrayList<>();
    
    private ArrayList<File> dataReadFiles = new ArrayList<File>();
    private ArrayList<File> dataPredictionReadFiles = new ArrayList<File>();
    
    private long timeStampSliderScale = 1000;
    private long oldestTimestamp = new Date().getTime();
    private long newestTimestamp = 0;
    private long oldestTimestampSelection = new Date().getTime();
    private long newestTimestampSelection = 0;

    private double depthSliderScale = 0.01;
    private double oldestDepth = Double.MAX_VALUE;
    private double newestDepth = 0;
    private double oldestDepthSelection = Double.MAX_VALUE;
    private double newestDepthSelection = 0;
    
    private double oldestRhod = Double.MAX_VALUE;
    private double newestRhod = 0;
    private double oldestPred = Double.MAX_VALUE;
    private double newestPred = 0;
    
    private String rhodamineImcString = "";
    private long rhodamineImcStringMillis = -1;
    
    private long lastUpdatedValues = -1;
    
    private SimpleDateFormat dateTimeFmt = new SimpleDateFormat("MM-dd HH:mm");
    
    // Extra GUI
    private String predictionTxt = I18n.text("Prediction");
    private String timeTxt = I18n.text("Time");
    private String depthTxt = I18n.text("Depth");
    private String valueTxt = I18n.text("value");
    private String minTxt = I18n.text("min");
    private String maxTxt = I18n.text("max");
    private String rhoTxt = I18n.text("Rhodamine Dye");

    private JPanel sliderPanel;

    private JSlider predictionSlider;
    private JLabel predictionLabel;
    private JLabel predictionLabelValue;
    private JLabel predictionLabelMinValue;
    private JLabel predictionLabelMaxValue;

    private RangeSlider timeSlider;
    private JLabel timeLabel;
    private JLabel timeLabelValue;
    private JLabel timeLabelMinValue;
    private JLabel timeLabelMaxValue;

    private RangeSlider depthSlider;
    private JLabel depthLabel;
    private JLabel depthLabelValue;
    private JLabel depthLabelMinValue;
    private JLabel depthLabelMaxValue;
    
    private JLabel rhodamineRangeTxt;
    private JLabel predictionRangeTxt;
    
    // 3D GUI
    private JButton button3D;
    private Rhodamine3DPanel rhod3DPanel;
    private JDialog dialog3D;

    public RhodamineOilVisualizer() {
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        offScreenImageControlColorBar.setOffScreenBufferPixel(0);
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
        
        initGUI();
        
        clearTimeAndDepthValues();
        updateTimeAndDepthDataValues();
        updateDepthPredictionValues();
    }

    private void initGUI() {
        predictionSlider = new JSlider(0, 0, 0);
        predictionSlider.setUI(new BasicSliderUI(predictionSlider) {
            @Override
            public void paintThumb(Graphics g) {
                Rectangle knobBounds = thumbRect;
                int w = knobBounds.width;
                int h = knobBounds.height;      
                
                Graphics2D g2d = (Graphics2D) g.create();
                Shape thumbShape = new Ellipse2D.Double(0, 0, w - 1, h - 1);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.translate(knobBounds.x, knobBounds.y);

                g2d.setColor(Color.CYAN);
                g2d.fill(thumbShape);

                g2d.setColor(Color.BLUE);
                g2d.draw(thumbShape);
                
                g2d.dispose();
            }
        });
        predictionLabel = new JLabel(predictionTxt);
        predictionLabelValue = new JLabel("");
        predictionLabelMinValue = new JLabel(minTxt + "=" + DateTimeUtil.milliSecondsToFormatedString(0));
        predictionLabelMaxValue = new JLabel();
        updatePredictionTimeSliderTime();
        predictionSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!((JSlider) e.getSource()).getValueIsAdjusting()) {
                    updatePredictionTimeSliderTime();
                    invalidateCache();
                    triggerRhodPredMinMaxValuesCalc();
                }
            }
        });

        timeSlider = new RangeSlider(0, 0);
        timeLabel = new JLabel(timeTxt);
        timeLabelValue = new JLabel("");
        timeLabelMinValue = new JLabel(minTxt + "=0");
        timeLabelMaxValue = new JLabel();
        updateTimeSliderTime();
        timeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!((JSlider) e.getSource()).getValueIsAdjusting()) {
                    updateTimeSliderTime();
                    invalidateCache();
                    oldestTimestampSelection = timeSlider.getValue() * timeStampSliderScale;
                    newestTimestampSelection = (timeSlider.getValue() + timeSlider.getExtent()) * timeStampSliderScale;
                    triggerRhodPredMinMaxValuesCalc();
                }
            }
        });

        depthSlider = new RangeSlider(0, 0);
        depthLabel = new JLabel(depthTxt);
        depthLabelValue = new JLabel("");
        depthLabelMinValue = new JLabel(minTxt + "=0m");
        depthLabelMaxValue = new JLabel(maxTxt + "=0m");
        updateDepthSliderTime();
        depthSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!((JSlider) e.getSource()).getValueIsAdjusting()) {
                    updateDepthSliderTime();
                    invalidateCache();
                    oldestDepthSelection = depthSlider.getValue() * depthSliderScale;
                    newestDepthSelection = (depthSlider.getValue() + depthSlider.getExtent()) * depthSliderScale;
                    triggerRhodPredMinMaxValuesCalc();
                }
            }
        });

        rhodamineRangeTxt = new JLabel();
        updateRhodamineRangeTexts();
        predictionRangeTxt = new JLabel();
        updatePredictionRangeTexts();
        
        // 3D
        button3D = new JButton(I18n.text("3D"));
        button3D.addActionListener(get3DAction());
        
        sliderPanel = new JPanel(new MigLayout("hidemode 3, wrap 6"));
        
        sliderPanel.add(rhodamineRangeTxt, "spanx 2");
        sliderPanel.add(predictionRangeTxt, "spanx 3");

        sliderPanel.add(button3D, "tag right, spany 3, wrap");
        
        sliderPanel.add(predictionLabel);
        sliderPanel.add(predictionLabelValue, "gapleft 10, width :100:");
        sliderPanel.add(predictionLabelMinValue, "gapleft 10, , width :100:");
        sliderPanel.add(predictionSlider, "width :100%:");
        sliderPanel.add(predictionLabelMaxValue, "width :100:");
        
        
        sliderPanel.add(timeLabel);
        sliderPanel.add(timeLabelValue, "gapleft 10, width :100:");
        sliderPanel.add(timeLabelMinValue, "gapleft 10, , width :100:");
        sliderPanel.add(timeSlider, "width :100%:");
        sliderPanel.add(timeLabelMaxValue, "width :100:");
        
        sliderPanel.add(depthLabel);
        sliderPanel.add(depthLabelValue, "gapleft 10, width :100:");
        sliderPanel.add(depthLabelMinValue, "gapleft 10, , width :100:");
        sliderPanel.add(depthSlider, "width :100%:");
        sliderPanel.add(depthLabelMaxValue, "width :100:");
    }

    /**
     * @return
     */
    private ActionListener get3DAction() {
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rhod3DPanel == null) {
                    rhod3DPanel = new Rhodamine3DPanel();
                    dialog3D = new JDialog(SwingUtilities.getWindowAncestor(RhodamineOilVisualizer.this.getConsole()));
                    dialog3D.setLayout(new BorderLayout());
                    dialog3D.add(rhod3DPanel);
                    dialog3D.setSize(600, 400);
                }

                ArrayList<BaseData> to3D = new ArrayList<>();
                for (BaseData point : dataList.toArray(new BaseData[dataList.size()])) {
                    if (validPoint(point, true))
                        to3D.add(point);
                }

                ArrayList<BaseData> to3DPrev = new ArrayList<>();
                ArrayList<BaseData> tmpLst = filterPrevisionBySelTime();
                for (BaseData point : tmpLst) {
                    if (validPoint(point, false))
                        to3DPrev.add(point);
                }

                rhod3DPanel.setUseRange(new double[] { minValue, maxValue });
                
                PointCloudRhodamine[] newPointCloudRhod = RhodaminePointCloudLoader.loadRhodamineData(to3D, to3DPrev,
                        predictionScaleFactor);
                rhod3DPanel.updatePointCloud(newPointCloudRhod[0], newPointCloudRhod[1]);
                
                dialog3D.setVisible(true);
                dialog3D.requestFocus();
            }
        };
        return al;
    }


    private void dataPanelSetVisible(boolean b) {
        timeLabel.setVisible(b);
        timeLabelValue.setVisible(b);
        timeLabelMinValue.setVisible(b);
        timeSlider.setVisible(b);
        timeLabelMaxValue.setVisible(b);
        sliderPanel.repaint();
    }

    private void predictionPanelSetVisible(boolean b) {
        predictionLabel.setVisible(b);
        predictionLabelValue.setVisible(b);
        predictionLabelMinValue.setVisible(b);
        predictionSlider.setVisible(b);
        predictionLabelMaxValue.setVisible(b);
        sliderPanel.repaint();
    }

    /**
     * @param dataPredictionMillisPassedFromSpillMax the dataPredictionMillisPassedFromSpillMax to set
     */
    public void setDataPredictionMillisPassedFromSpillMax(long dataPredictionMillisPassedFromSpillMax) {
        this.dataPredictionMillisPassedFromSpillMax = dataPredictionMillisPassedFromSpillMax;
        
        if (predictionSlider != null) {
            int oldValue = this.predictionSlider.getValue();
            this.predictionSlider.setMaximum((int)dataPredictionMillisPassedFromSpillMax);
            this.predictionSlider.setValue(Math.min(oldValue, this.predictionSlider.getMaximum()));
        }
    }
    
    private void updatePredictionTimeSliderTime() {
        predictionLabelMaxValue.setText(maxTxt + "="
                + DateTimeUtil.milliSecondsToFormatedString(dataPredictionMillisPassedFromSpillMax));
        predictionLabelValue.setText(valueTxt + "=" + DateTimeUtil.milliSecondsToFormatedString(predictionSlider.getValue()));
    }
    
    private void updateTimeSliderTime() {
        timeLabelMinValue.setText(minTxt + "="
                + dateTimeFmt.format(new Date(timeSlider.getMinimum() * timeStampSliderScale)));
        timeLabelMaxValue.setText(maxTxt + "="
                + dateTimeFmt.format(new Date(timeSlider.getMaximum() * timeStampSliderScale)));
        timeLabelValue.setText(valueTxt + "=[" 
                + dateTimeFmt.format(new Date(timeSlider.getValue() * timeStampSliderScale))
                + "; "
                + dateTimeFmt.format(new Date(timeSlider.getUpperValue() * timeStampSliderScale))
                + "]");
    }

    private void updateDepthSliderTime() {
        depthLabelMinValue.setText(minTxt + "="
                + MathMiscUtils.round(depthSlider.getMinimum() * depthSliderScale, 2) + "m");
        depthLabelMaxValue.setText(maxTxt + "="
                + MathMiscUtils.round(depthSlider.getMaximum() * depthSliderScale, 2) + "m");
        depthLabelValue.setText(valueTxt + "=[" 
                + MathMiscUtils.round(depthSlider.getValue() * depthSliderScale, 2)
                + "; "
                + MathMiscUtils.round(depthSlider.getUpperValue() * depthSliderScale, 2)
                + "] m");
    }

    private void updateRhodamineRangeTexts() {
        String valMin = "";
        String valMax = "";
        if (newestRhod >= oldestRhod) {
            valMin = "" + MathMiscUtils.round(oldestRhod, 2);
            valMax = "" + MathMiscUtils.round(newestRhod, 2);
        }
        rhodamineRangeTxt.setText(rhoTxt + "[" + valMin + "; " + valMax + "] ppb");
    }

    private void updatePredictionRangeTexts() {
        String valMin = "";
        String valMax = "";
        if (newestPred >= oldestPred) {
            valMin = "" + MathMiscUtils.round(oldestPred, 2);
            valMax = "" + MathMiscUtils.round(newestPred, 2);
        }
        predictionRangeTxt.setText(predictionTxt + "[" + valMin + "; " + valMax + "] ppb");
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        cleanData();

        getConsole().removeMapLayer(previsionLayer);
        
        if (dialog3D != null) {
            dialog3D.setVisible(false);
            dialog3D.dispose();
        }
    }


    /**
     * 
     */
    private void cleanData() {
        dataList.clear();
        dataReadFiles.clear();
        
        clearDataPredictionList();
        lastEstimatedStateList.clear();
        lastRhodamineDyeList.clear();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (clearData) {
            cleanData();
            clearData = false;
        }
        
        circle = new Ellipse2D.Double(-pixelSizeData / 2d, -pixelSizeData / 2d, pixelSizeData, pixelSizeData);
        
        if (minValue > maxValue)
            minValue = maxValue;
        
        if (maxValue < minValue)
            maxValue = minValue;

        if (sliderPanel != null) {
            @SuppressWarnings("unused")
            SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
//                    while (!updateValues()) {
//                        Thread.yield();
//                    }
                    updateValues();
                    return true;
                }
                @Override
                protected void done() {
                    try {
                        get();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    clearImgCachRqst = true;
                    clearColorBarImgCachRqst = true;
                }
            };
        }
        else {
            clearImgCachRqst = true;
            clearColorBarImgCachRqst = true;
        }
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
        try {
            long curTime = System.currentTimeMillis();
            if (curTime - lastUpdatedValues > periodSecondsToUpdate * 1000) {
                lastUpdatedValues = curTime;
                boolean ret = updateValues();
                if (ret) {
                    invalidateCache();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void invalidateCache() {
        clearImgCachRqst = true;
    }

    private boolean updatingFiles = false;
    public boolean updateValues() {
        if (updatingFiles)
            return false;
        
        updatingFiles = true;
        System.out.println("#########################S");

        try {
            
            File[] fileList = FileUtil.getFilesFromDisk(baseFolderForCSVFiles, csvFilePattern);
            if (fileList != null && fileList.length > 0) {
                for (int i = (readAllOrLastOfOrderedFiles ? 0 : fileList.length -1); i < fileList.length; i++) {
                    File csvFx = fileList[i];
                    if (isLastModifiedDifferent(csvFx, dataReadFiles))
                        loadDataFile(csvFx);
                }
            }
            
            File[] folders = FileUtil.getFoldersFromDisk(baseFolderForCSVFiles, null);
            if (folders != null && folders.length > 0) {
                for (File folder : folders) {
                    fileList = FileUtil.getFilesFromDisk(folder, csvFilePattern);
                    if (fileList != null && fileList.length > 0) {
                        for (int i = (readAllOrLastOfOrderedFiles ? 0 : fileList.length -1); i < fileList.length; i++) {
                            File csvFx = fileList[i];
                            if (isLastModifiedDifferent(csvFx, dataReadFiles))
                                loadDataFile(csvFx);
                        }
                    }
                }
            }
            
            // Load prediction
            if (predictionFile.exists() && predictionFile.isFile()) {
                if (isLastModifiedDifferent(predictionFile, dataPredictionReadFiles)) {
                    dataPredictionMillisPassedFromSpillMax = -1;
                    dataPredictionList.clear();
                    dataPredictionValues.clear();
                    loadPredictionFile(predictionFile);
                }
            }
            else if (predictionFile.exists() && predictionFile.isDirectory()) {
                boolean reload = false;
                fileList = FileUtil.getFilesFromDisk(predictionFile, totFilePattern);
    //            ArrayList<File> fileAListCopy = new ArrayList<File>(Arrays.asList(fileList));
                if (fileList != null && fileList.length > 0) {
                    for (int i = 0; i < fileList.length; i++) {
                        File totFx = fileList[i];
                        if (isLastModifiedDifferent(totFx, dataPredictionReadFiles)) {
                            reload = true;
                        }
    //                    else {
    //                        fileAListCopy.remove(totFx);
    //                    }
                    }
                }
                //fileList = fileAListCopy.toArray(new File[fileAListCopy.size()]);
                
                if (reload) {
                    dataPredictionMillisPassedFromSpillMax = -1;
                    dataPredictionList.clear();
                    dataPredictionValues.clear();
                    if (fileList != null && fileList.length > 0) {
                        for (int i = 0; i < fileList.length; i++) {
                            File totFx = fileList[i];
                            loadPredictionFile(totFx);
                        }
                    }
                }
            }
            setDataPredictionMillisPassedFromSpillMax(dataPredictionMillisPassedFromSpillMax);
        }
        catch (Exception e) {
            cleanData();
        }
        
        updatingFiles = false;
        System.out.println("#########################E");
        
        return true;
    }


    /**
     * @param fx
     */
    private boolean isLastModifiedDifferent(File fx, ArrayList<File> fileList) {
        if (fileList.contains(fx)) {
            int idx = fileList.indexOf(fx);
            File rFx = fileList.get(idx);
            if (rFx.lastModified() != fx.lastModified())
                return true;
            else
                return false;
        }
        else {
            fileList.add(new File(fx.getPath()));
            return true;
        }
    }

    private void clearDataPredictionList() {
        dataPredictionList.clear();
        dataPredictionValues.clear();
        dataPredictionReadFiles.clear();
        setDataPredictionMillisPassedFromSpillMax(0);

        clearTimeAndDepthValues();
        updateTimeAndDepthDataValues();
        updateDepthPredictionValues();
    }

    /**
     * @param csvFx
     */
    private void loadDataFile(File csvFx) {
        try {
            CSVDataParser csv = new CSVDataParser(csvFx);
            csv.parse();
            updateValues(dataList, csv.getPoints(), true);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * @param predictionFile 
     * 
     */
    private void loadPredictionFile(File predictionFile) {
        String fxExt = FileUtil.getFileExtension(predictionFile);
        if ("csv".equalsIgnoreCase(fxExt)) {
            try {
                CSVDataParser csv = new CSVDataParser(predictionFile);
                csv.parse();
                updateValues(dataPredictionList, csv.getPoints(), true);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                MedslikDataParser totFile = new MedslikDataParser(predictionFile);
                totFile.parse();
                dataPredictionMillisPassedFromSpillMax = Math.max(dataPredictionMillisPassedFromSpillMax,
                        totFile.getMillisPassedFromSpill());
                if (!dataPredictionValues.contains(totFile.getMillisPassedFromSpill()))
                    dataPredictionValues.add(totFile.getMillisPassedFromSpill());
                updateValues(dataPredictionList, totFile.getPoints(), false);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param list
     * @param points
     */
    private boolean updateValues(ArrayList<BaseData> list, ArrayList<BaseData> points, boolean dataOrPrediction) {
        boolean dataUpdated = false;
        
        long st = System.currentTimeMillis();
        
        if (autoCleanData && dataOrPrediction) {
            boolean updateValues = false;
            long curTimeMillis = System.currentTimeMillis();
            for (BaseData bd : points.toArray(new BaseData[points.size()])) {
                if (curTimeMillis - bd.getTimeMillis() > dataAgeToCleanInMinutes * DateTimeUtil.MINUTE) {
                    points.remove(bd);
                    updateValues = true;
                }
            }
            for (BaseData bd : list.toArray(new BaseData[list.size()])) {
                if (curTimeMillis - bd.getTimeMillis() > dataAgeToCleanInMinutes * DateTimeUtil.MINUTE) {
                    list.remove(bd);
                    updateValues = true;
                }
            }
            
            if (updateValues) {
                clearTimeAndDepthValues();
                updateTimeAndDepthDataValues();
                updateDepthPredictionValues();
            }
        }
        
        for (BaseData testPoint : points) {
//            int counter = 0;
            boolean found = false;
            
//            for (BaseData toTestPoint : list.toArray(new BaseData[list.size()])) {
//                if (toTestPoint.equals(testPoint)) {
//                    if (toTestPoint.getTimeMillis() < testPoint.getTimeMillis()) {
//                        list.remove(counter);
//                        list.add(counter, testPoint);
//                        dataUpdated = true;
//                        
//                        if (dataOrPrediction) {
//                            updateTimeValuesMinMax(testPoint);
//                            updateRhodamineValuesMinMax(testPoint);
//                        }
//                        else {
//                            updatePredictionValuesMinMax(testPoint);
//                        }
//                        updateDepthValuesMinMax(testPoint);
//                    }
////                    System.out.println("######### " + counter);
//                    found = true;
//                    break;
//                }

//            if (list.contains(testPoint))
//                found = true;

//            for (BaseData toTestPoint : list) {
//                if (toTestPoint.equals(testPoint)) {
//                    found = true;
//                    break;
//                }
//
////              counter++;
//            }
//            System.out.println("taking " +  (System.currentTimeMillis() - st) + "ms");
            
//            if (list.contains(testPoint))
//                found = true;
////            for (BaseData toTestPoint : list) {
////                if (toTestPoint.equals(testPoint)) {
////                    found = true;
////                    break;
////                }
////            }
            if (!found) {
//                counter++;
                list.add(testPoint);
                dataUpdated = true;

                if (dataOrPrediction) {
                    updateTimeValuesMinMax(testPoint);
                    updateRhodamineValuesMinMax(testPoint);
                }
                else {
                    updatePredictionValuesMinMax(testPoint);
                }
                updateDepthValuesMinMax(testPoint);
            }
        }
        System.out.println("List size: " + list.size() + " took: " + (System.currentTimeMillis() - st) + "ms" + (dataOrPrediction ? "" : " (prediction)"));
        return dataUpdated;
    }

    private void clearTimeAndDepthValues() {
        // Reset values
        oldestTimestamp = new Date().getTime();
        newestTimestamp = 0;

        oldestDepth = Double.MAX_VALUE;
        newestDepth = 0;
        
        clearRhodPredMinMaxValues();
    }

    /**
     * 
     */
    private void clearRhodPredMinMaxValues() {
        oldestRhod = Double.MAX_VALUE;
        newestRhod = 0;
        
        oldestPred = Double.MAX_VALUE;
        newestPred = 0;
    }

    private void triggerRhodPredMinMaxValuesCalc() {
        clearRhodPredMinMaxValues();
        
        for (BaseData pt : dataList.toArray(new BaseData[dataList.size()])) {
            updateRhodamineValuesMinMax(pt);
        }

        for (BaseData pt : dataPredictionList.toArray(new BaseData[dataPredictionList.size()])) {
            updatePredictionValuesMinMax(pt);
        }
        
        updateRhodamineRangeTexts();
        updatePredictionRangeTexts();
    }

    private void updateTimeAndDepthDataValues() {
        // Calc new values
        for (BaseData pt : dataList.toArray(new BaseData[dataList.size()])) {
            updateTimeValuesMinMax(pt);
            updateDepthValuesMinMax(pt);
            updateRhodamineValuesMinMax(pt);
        }
    }

    private void updateDepthPredictionValues() {
        // Calc new values
        for (BaseData pt : dataPredictionList.toArray(new BaseData[dataPredictionList.size()])) {
            updateDepthValuesMinMax(pt);
            updatePredictionValuesMinMax(pt);
        }
    }

    private void updateTimeValuesMinMax(BaseData pt) {
        if (pt == null)
            return;
            
        // Time
        if (pt.getTimeMillis() > 0 && pt.getTimeMillis() < oldestTimestamp) {// && pt.getTimestamp() > minDate) {
            oldestTimestamp = pt.getTimeMillis();
            timeSlider.setMinimum((int)(oldestTimestamp / timeStampSliderScale));
            updateTimeSliderTime();
        }
        if (pt.getTimeMillis() > 0 && pt.getTimeMillis() > newestTimestamp) {
            newestTimestampSelection = newestTimestamp = pt.getTimeMillis();
            timeSlider.setMaximum((int)(newestTimestamp / timeStampSliderScale));
            timeSlider.setUpperValue((int)(newestTimestamp / timeStampSliderScale));
            updateTimeSliderTime();
        }
    }

    private void updateRhodamineValuesMinMax(BaseData pt) {
        if (pt == null)
            return;
        
        if (!validPoint(pt, true))
            return;

        // Time
        if (pt.getRhodamineDyePPB() < oldestRhod) {
            oldestRhod = pt.getRhodamineDyePPB();
            updateRhodamineRangeTexts();
        }
        if (pt.getRhodamineDyePPB() > newestRhod) {
            newestRhod = pt.getRhodamineDyePPB();
            updateRhodamineRangeTexts();
        }
    }

    private void updatePredictionValuesMinMax(BaseData pt) {
        if (pt == null)
            return;
        
        if (!validPoint(pt, false))
            return;
        long filterTime = calcPredictionFlterTime(dataPredictionValues);
        if (pt.getTimeMillis() != filterTime)
            return;
        
        // Time
        if (pt.getRhodamineDyePPB() * predictionScaleFactor < oldestPred) {
            oldestPred = pt.getRhodamineDyePPB() * predictionScaleFactor;
            updatePredictionRangeTexts();
        }
        if (pt.getRhodamineDyePPB() * predictionScaleFactor > newestPred) {
            newestPred = pt.getRhodamineDyePPB() * predictionScaleFactor;
            updatePredictionRangeTexts();
        }
    }

    private void updateDepthValuesMinMax(BaseData pt) {
        if (pt == null)
            return;
            
        // Depth
        if (!Double.isNaN(pt.getDepth()) && pt.getDepth() < oldestDepth) {// && pt.getTimestamp() > minDate) {
            oldestDepth = pt.getDepth();
            depthSlider.setMinimum((int)(oldestDepth / depthSliderScale));
            updateDepthSliderTime();
        }
        if (!Double.isNaN(pt.getDepth())) {
            double testLowerDepth = (!Double.isNaN(pt.getDepthLower()) && pt.getDepthLower() > pt.getDepth()) ? pt
                    .getDepthLower() : pt.getDepth();
            if (testLowerDepth > newestDepth) {
                newestDepthSelection = newestDepth = testLowerDepth;
                depthSlider.setMaximum((int)(newestDepth / depthSliderScale));
                depthSlider.setUpperValue((int)(newestDepth / depthSliderScale));
                updateDepthSliderTime();
            }
        }
    }


    @Periodic(millisBetweenUpdates = 500)
    public boolean updateExtraGUI() {
        if (System.currentTimeMillis() - lastPaintMillis > 2000) {
            setupExtraGui(false, null);
        }

        if (System.currentTimeMillis() - lastPaintDataMillis > 2000)
            dataPanelSetVisible(false);
        else
            dataPanelSetVisible(true);

        if (System.currentTimeMillis() - lastPaintPredictonMillis > 2000)
            predictionPanelSetVisible(false);
        else
            predictionPanelSetVisible(true);

        return true;
    }
    
    private boolean updating = false; 
    public void setupExtraGui(boolean mode, StateRenderer2D source) {
        if (updating)
            return;
        
        updating = true;

        boolean repaint = false;
        Container parent = source != null ? source.getParent() : null;
        if (source != null && mode && sliderPanel.getParent() == null) {
            while (parent != null && !(parent.getLayout() instanceof BorderLayout)) { 
                parent = parent.getParent();
            }
            parent.add(sliderPanel, BorderLayout.SOUTH);
            repaint = true;
        }
        else if (!mode && sliderPanel != null && sliderPanel.getParent() != null) {
            parent = sliderPanel.getParent();
            sliderPanel.getParent().remove(sliderPanel);
            repaint = true;
        }
        
        if (repaint) {
            parent.invalidate();
            parent.validate();
            parent.repaint();
        }
        
        updating = false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        lastPaintMillis = System.currentTimeMillis();
        lastPaintDataMillis = lastPaintMillis;

        super.paint(g, renderer);
        
        setupExtraGui(true, renderer);
        
        checkIfClearCache();

        boolean recreateImageData = offScreenImageControlData.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImageData) {
            Graphics2D g2 = offScreenImageControlData.getImageGraphics();
            paintData(renderer, g2);
            g2.dispose();
        }            
        offScreenImageControlData.paintPhaseEndFinishImageRecreateAndPainImageCacheToRenderer(g, renderer);

        paintColorBar(g, renderer);            

        paintLegend(g);
        
        if (rhodamineImcStringMillis - System.currentTimeMillis() < 5000) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.WHITE);
            Font prev = g2.getFont();
            g2.setFont(new Font("Helvetica", Font.BOLD, 18));
            g2.setFont(prev);
            g2.translate(15, 45);
            
            try {
                g2.setColor(Color.BLACK);
                g2.drawString(rhodamineImcString, 1, 120);
                g2.setColor(Color.WHITE);
                g2.drawString(rhodamineImcString, 2, 121);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }
            g2.dispose();
        }

    }

    private void checkIfClearCache() {
        if (clearImgCachRqst) {
            offScreenImageControlData.triggerImageRebuild();
            offScreenImageControlPrediction.triggerImageRebuild();
            clearImgCachRqst = false;
        }
        
        if (clearColorBarImgCachRqst) {
            offScreenImageControlColorBar.triggerImageRebuild();
            clearColorBarImgCachRqst = false;
        }
    }

    /**
     * @param g
     */
    private void paintLegend(Graphics2D g) {
        // Legend
        Graphics2D gl = (Graphics2D) g.create();
        gl.translate(10, 35);
        gl.setColor(Color.BLACK);
        gl.drawString(getName(), 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.translate(1, 1);
        gl.setColor(Color.WHITE);
        gl.drawString(getName(), 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.dispose();
    }

    /**
     * @param g
     * @param renderer
     */
    private void paintColorBar(Graphics2D g, StateRenderer2D renderer) {
        boolean recreateImageColorBar = offScreenImageControlColorBar.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImageColorBar) {
            Graphics2D g2 = offScreenImageControlColorBar.getImageGraphics();
            g2.setColor(new Color(250, 250, 250, 100));
            g2.fillRect(5, 30, 70, 110);

            ColorMap cmap = colorMap;
            ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
            cb.setSize(15, 80);
            g2.setColor(Color.WHITE);
            Font prev = g2.getFont();
            g2.setFont(new Font("Helvetica", Font.BOLD, 18));
            g2.setFont(prev);
            g2.translate(15, 45);
            cb.paint(g2);
            g2.translate(-10, -15);

            try {
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 28, 20);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue), 29, 21);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue / 2), 28, 60);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(maxValue / 2), 29, 61);
                g2.setColor(Color.BLACK);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 28, 100);
                g2.setColor(Color.WHITE);
                g2.drawString(GuiUtils.getNeptusDecimalFormat(1).format(minValue), 29, 101);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }
            g2.dispose();
        }
        offScreenImageControlColorBar.paintPhaseEndFinishImageRecreateAndPainImageCacheToRenderer(g, renderer);
    }
    
    private void paintData(StateRenderer2D renderer, Graphics2D g2) {
        paintDataWorker(renderer, g2, false, dataList);
    }

    private void paintPredictionData(StateRenderer2D renderer, Graphics2D g2) {
        ArrayList<BaseData> tmpLst = filterPrevisionBySelTime();
        paintDataWorker(renderer, g2, true, tmpLst);
    }

    /**
     * @return
     */
    private ArrayList<BaseData> filterPrevisionBySelTime() {
        ArrayList<BaseData> tmpLst = new ArrayList<>(dataPredictionList);
        List<Long> tmpValLst = new ArrayList<>(dataPredictionValues);
        //Collections.sort(tmpValLst);
        long filterTime = calcPredictionFlterTime(tmpValLst);
        
        BaseData[] arr = tmpLst.toArray(new BaseData[tmpLst.size()]);
        for (BaseData dpt : arr) {
            if (dpt.timeMillis != filterTime)
                tmpLst.remove(dpt);
        }
        return tmpLst;
    }


    /**
     * @param tmpValLst
     * @return
     */
    private long calcPredictionFlterTime(List<Long> tmpValLst) {
        long curSelTime = predictionSlider.getValue();
        long filterTime = -1;
        for (long tm : tmpValLst) {
            if (filterTime == -1) {
                filterTime = tm;
                continue;
            }
            
            if (tm < curSelTime)
                filterTime = Math.max(filterTime, tm);
            else if (tm == curSelTime)
                filterTime = Math.max(filterTime, tm);
            else
                filterTime = Math.min(filterTime, tm);
        }
        return filterTime;
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
            
            boolean validPoint = validPoint(point, !prediction ? true : false);
            if (!validPoint)
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

    private boolean validPoint(BaseData point, boolean validateTime) {
        if (Double.isNaN(point.getRhodamineDyePPB()) || Double.isInfinite(point.getRhodamineDyePPB()))
            return false;
        
        if (point.getRhodamineDyePPB() < minValue)
            return false;
        
        if (validateTime) {
            if (point.getTimeMillis() < oldestTimestampSelection
                    || point.getTimeMillis() > newestTimestampSelection)
                return false;
        }
        if (Double.isNaN(point.getDepthLower())) {
            if (point.getDepth() < oldestDepthSelection
                    || point.getDepth() > newestDepthSelection)
                return false;
        }
        else {
            if (point.getDepthLower() < oldestDepthSelection
                    || point.getDepth() > newestDepthSelection)
                return false;
        }
        
        return true;
    }


    @Subscribe
    public void on(EstimatedState msg) {
        // From any system
//        System.out.println(msg.asJSON());
        lastEstimatedStateList.put(msg.getSrc(), msg);
    }

    @Subscribe
    public void on(RhodamineDye msg) {
        // From any system
//        System.out.println(msg.asJSON());
        EstimatedState lastSystemES = lastEstimatedStateList.get(msg.getSrc());
        if (lastSystemES != null) {
            long rdmTs = msg.getTimestampMillis();
            long lastSystemESTs = lastSystemES.getTimestampMillis();
            long delta = Math.abs(rdmTs - lastSystemESTs);
            if (delta <= maxDeltaTimeBetweenEstimatedStateAndMessageDataReceivedMillis) {
                LocationType loc = IMCUtils.getLocation(lastSystemES);
                loc.convertToAbsoluteLatLonDepth();
                BaseData pt = new BaseData(loc.getLatitudeDegs(), loc.getLongitudeDegs(), 
                        loc.getDepth(), rdmTs);
                pt.setRefineOilPPB(msg.getValue());
                // TODO store this data
                ArrayList<BaseData> data = new ArrayList<>(); 
                data.add(pt);
                updateValues(dataList, data, true);
            }
        }
        
        double valueReceived = msg.getValue();
        if (!Double.isNaN(valueReceived)) {
            rhodamineImcString = "" + MathMiscUtils.round(valueReceived, 2) + "ppb @ " 
                    + DateTimeUtil.timeFormatterUTC.format(new Date(rhodamineImcStringMillis));
            rhodamineImcStringMillis = msg.getTimestampMillis();
        }
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
        int offScreenBufferPixel = offScreenImageControlData.getOffScreenBufferPixel();
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
            lastPaintMillis = System.currentTimeMillis();
            lastPaintPredictonMillis = lastPaintMillis;

            super.paint(g, renderer);

            setupExtraGui(true, renderer);

            checkIfClearCache();

            boolean recreateImagePrediction = offScreenImageControlPrediction.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
            if (recreateImagePrediction) {
                Graphics2D g2 = offScreenImageControlPrediction.getImageGraphics();
                paintPredictionData(renderer, g2);
                g2.dispose();
            }            
            offScreenImageControlPrediction.paintPhaseEndFinishImageRecreateAndPainImageCacheToRenderer(g, renderer);

            paintColorBar(g, renderer);            

            paintLegend(g);
        }
    }
}
