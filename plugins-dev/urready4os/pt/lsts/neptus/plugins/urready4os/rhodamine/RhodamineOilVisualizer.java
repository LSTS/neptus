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
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAccumulator;

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
import pt.lsts.neptus.colormap.ColorBarPainterUtil;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.notifications.Notification.NotificationType;
import pt.lsts.neptus.gui.editor.FolderAndFilePropertyEditor;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.gui.editor.SystemNameOrNullListEditor;
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
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.ThreadPoolExecuterUtil;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

/**
 * @author pdias
 *
 */
@PluginDescription(name="Rhodamine Oil Visualizer", author="Paulo Dias", version="0.8", 
    icon = "pt/lsts/neptus/plugins/urready4os/urready4os.png")
@LayerPriority(priority = -50)
public class RhodamineOilVisualizer extends ConsoleLayer implements ConfigurationListener {

    enum VisibleDataVariableEnum {
        RhodaminePPB,
        Temperature
    }

    @NeptusProperty(name = "Visible data vaiable", userLevel = LEVEL.REGULAR, category="Visibility")
    private VisibleDataVariableEnum visibleDataVar = VisibleDataVariableEnum.RhodaminePPB;

    @NeptusProperty(name = "PPB Minimum value", userLevel = LEVEL.REGULAR, category="Scale")
    private double minValue = 0;

    @NeptusProperty(name = "PPB Maximum value", userLevel = LEVEL.REGULAR, category="Scale")
    private double maxValue = 100;

    @NeptusProperty(name = "Temp Minimum value", userLevel = LEVEL.REGULAR, category="Scale")
    private double minTempValue = 10;

    @NeptusProperty(name = "Temp Maximum PPB value", userLevel = LEVEL.REGULAR, category="Scale")
    private double maxTempValue = 30;

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
    
    @NeptusProperty(name = "Prediction file or folder (tot or lv* files)", userLevel = LEVEL.REGULAR, category = "Prediction",
            editorClass = FolderAndFilePropertyEditor.class)
    private File predictionFile = new File("log/rhodamine-prediction");

    @NeptusProperty(name = "Prediction scale factor", userLevel = LEVEL.REGULAR, category = "Prediction")
    private double predictionScaleFactor = 100;

    @NeptusProperty(name = "Read all or last of ordered files", userLevel = LEVEL.REGULAR, category = "Data Update",
            description = "True to read all CSV files of just the last of ordered files in folder.")
    private boolean readAllOrLastOfOrderedFiles = true;
    
    @NeptusProperty(name = "Max delta time between EstimatedState and message data received in millis", 
            userLevel = LEVEL.REGULAR, category = "Data Update",
            description = "If the maximum time difference allowed between EstimatedState and message data to data to be accepted.")
    private long maxDeltaTimeBetweenEstimatedStateAndMessageDataReceivedMillis = 200;

    @NeptusProperty(name = "Print debug", userLevel = LEVEL.ADVANCED, category = "Debug")
    private boolean printDebug = false;

    @NeptusProperty(name = "Print paint debug", userLevel = LEVEL.ADVANCED, category = "Debug")
    private boolean printPaintDebug = false;
    
    @NeptusProperty(name = "Systems to ignore for rhodamine", userLevel = LEVEL.ADVANCED, category = "Filter",
            editorClass = SystemNameOrNullListEditor.class, description = "Comma separated list of systems to ignore.")
    private String systemsToIgnoreForRhodamine = "";

    @NeptusProperty(name = "Systems to ignore for IMC rhodamine", userLevel = LEVEL.ADVANCED, category = "Filter",
            editorClass = SystemNameOrNullListEditor.class, description = "Comma separated list of systems to ignore (all to ignore all).")
    private String systemsToIgnoreForIMCRhodamine = "";

    @NeptusProperty(name = "Systems to ignore for temperature", userLevel = LEVEL.ADVANCED, category = "Filter",
            editorClass = SystemNameOrNullListEditor.class, description = "Comma separated list of systems to ignore.")
    private String systemsToIgnoreForTemperature = "";

    @NeptusProperty(name = "Show popup for rhodamine reveived", userLevel = LEVEL.REGULAR, category = "Popup",
            description = "Will show a popup event.")
    private boolean popupForRhodamine = false;

    @NeptusProperty(name = "Show popup type", userLevel = LEVEL.ADVANCED, category = "Popup",
            description = "Will show a popup event.")
    private Notification.NotificationType popupForRhodamineType = NotificationType.WARNING;

    @NeptusProperty(name = "Threshold for rhodamine popup", userLevel = LEVEL.REGULAR, category = "Popup",
            description = "Will show a popup event only above the confirured value.")
    private int popupForRhodamineThreshold = 10;

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
    private OffScreenLayerImageControl offScreenImageControlColorBarPred = new OffScreenLayerImageControl();
    
    private ScheduledThreadPoolExecutor threadPool = ThreadPoolExecuterUtil.createScheduledThreadPool(this, 5);
    
    private boolean clearImgCachRqst = false;
    private long lastClearImgCacheRqstMillis = -1;
    private long minPeriodToRecreateCacheMillis = 2000;
    private boolean clearColorBarImgCachRqst = false;
    
    private Ellipse2D circle = new Ellipse2D.Double(-4, -4, 8, 8);
    private Rectangle2D diamond = new Rectangle2D.Double(-4, -4, 8, 8);

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
    private double oldestTemp = Double.MAX_VALUE;
    private double newestTemp = 0;
    
    private String rhodamineImcString = "";
    private long rhodamineImcStringMillis = -1;
    
    private long lastUpdatedValues = -1;
    
    private SimpleDateFormat dateTimeFmt = new SimpleDateFormat("MM-dd HH:mm");
    
    private boolean updatingFiles = false;
    private boolean updatingExtraGui = false;
    private StateRenderer2D curRenderer = null;

    // Extra GUI
    private String predictionTxt = I18n.text("Prediction");
    private String timeTxt = I18n.text("Time");
    private String depthTxt = I18n.text("Depth");
    private String valueTxt = I18n.text("value");
    private String minTxt = I18n.text("min");
    private String maxTxt = I18n.text("max");
    private String rhoTxt = I18n.text("Rhodamine Dye");
    private String tempTxt = I18n.text("Temperature");

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
    private JLabel tempRangeTxt;
    
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
        offScreenImageControlColorBarPred.setOffScreenBufferPixel(0);
        getConsole().addMapLayer(previsionLayer, false);
        initGUI();
        recalcMinMaxValues();
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
        tempRangeTxt = new JLabel();
        updateTempRangeTexts();
        
        // 3D
        button3D = new JButton(I18n.text("3D"));
        button3D.addActionListener(get3DAction());
        
        sliderPanel = new JPanel(new MigLayout("hidemode 3, wrap 6"));
        
        sliderPanel.add(rhodamineRangeTxt, "spanx 2");
        sliderPanel.add(predictionRangeTxt, "spanx 2");
        sliderPanel.add(tempRangeTxt, "spanx 1");

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
                button3D.setEnabled(false);
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        if (rhod3DPanel == null) {
                            rhod3DPanel = new Rhodamine3DPanel();
                            dialog3D = new JDialog(SwingUtilities.getWindowAncestor(RhodamineOilVisualizer.this.getConsole()));
                            dialog3D.setLayout(new BorderLayout());
                            dialog3D.add(rhod3DPanel);
                            dialog3D.setSize(600, 400);
                        }

                        ArrayList<BaseData> to3D = new ArrayList<>(dataList);
                        to3D.removeIf(p -> !validPoint(p, true) || !testIsValidInRenderer(p, RhodamineOilVisualizer.this.curRenderer));

                        ArrayList<BaseData> tmpLst = filterPrevisionBySelTime();
                        ArrayList<BaseData> to3DPrev = new ArrayList<>(tmpLst);
                        to3DPrev.removeIf(p -> !validPoint(p, false) || !testIsValidInRenderer(p, RhodamineOilVisualizer.this.curRenderer));

                        rhod3DPanel.setUseRange(new double[] { minValue, maxValue });
                        
                        PointCloudRhodamine[] newPointCloudRhod = RhodaminePointCloudLoader.loadRhodamineData(to3D, to3DPrev,
                                predictionScaleFactor);
                        rhod3DPanel.updatePointCloud(newPointCloudRhod[0], newPointCloudRhod[1]);

                        return null;
                    }
                    
                    /* (non-Javadoc)
                     * @see javax.swing.SwingWorker#done()
                     */
                    @Override
                    protected void done() {
                        dialog3D.setVisible(true);
                        dialog3D.requestFocus();
                        button3D.setEnabled(true);
                    }
                };
                worker.execute();
                
//                if (rhod3DPanel == null) {
//                    rhod3DPanel = new Rhodamine3DPanel();
//                    dialog3D = new JDialog(SwingUtilities.getWindowAncestor(RhodamineOilVisualizer.this.getConsole()));
//                    dialog3D.setLayout(new BorderLayout());
//                    dialog3D.add(rhod3DPanel);
//                    dialog3D.setSize(600, 400);
//                }
//
//                ArrayList<BaseData> to3D = new ArrayList<>(dataList);
//                to3D.removeIf(p -> !validPoint(p, true) || !testIsValidInRenderer(p, RhodamineOilVisualizer.this.curRenderer));
//
//                ArrayList<BaseData> tmpLst = filterPrevisionBySelTime();
//                ArrayList<BaseData> to3DPrev = new ArrayList<>(tmpLst);
//                to3DPrev.removeIf(p -> !validPoint(p, false) || !testIsValidInRenderer(p, RhodamineOilVisualizer.this.curRenderer));
//
//                rhod3DPanel.setUseRange(new double[] { minValue, maxValue });
//                
//                PointCloudRhodamine[] newPointCloudRhod = RhodaminePointCloudLoader.loadRhodamineData(to3D, to3DPrev,
//                        predictionScaleFactor);
//                rhod3DPanel.updatePointCloud(newPointCloudRhod[0], newPointCloudRhod[1]);
//                
//                dialog3D.setVisible(true);
//                dialog3D.requestFocus();
            }

            private boolean testIsValidInRenderer(BaseData p, StateRenderer2D renderer) {
                if (renderer == null)
                    return true;
                
                LocationType l = new LocationType();
                l.setLatitudeDegs(p.getLat());
                l.setLongitudeDegs(p.getLon());
                Point2D pt = renderer.getScreenPosition(l);
                return isVisibleInRender(pt, curRenderer);
            }
        };
        return al;
    }

    private void dataPanelSetEnabled(boolean b) {
        timeLabel.setEnabled(b);
        timeLabelValue.setEnabled(b);
        timeLabelMinValue.setEnabled(b);
        timeSlider.setEnabled(b);
        timeLabelMaxValue.setEnabled(b);
        sliderPanel.repaint();
    }

    private void predictionPanelSetEnabled(boolean b) {
        predictionLabel.setEnabled(b);
        predictionLabelValue.setEnabled(b);
        predictionLabelMinValue.setEnabled(b);
        predictionSlider.setEnabled(b);
        predictionLabelMaxValue.setEnabled(b);
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

    private void updateTempRangeTexts() {
        String valMin = "";
        String valMax = "";
        if (newestTemp >= oldestTemp) {
            valMin = "" + MathMiscUtils.round(oldestTemp, 2);
            valMax = "" + MathMiscUtils.round(newestTemp, 2);
        }
        tempRangeTxt.setText(tempTxt + "[" + valMin + "; " + valMax + "] \u00B0C");
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
        
        curRenderer = null;
    }

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
        diamond = new Rectangle2D.Double(-pixelSizeData / 2d, -pixelSizeData / 2d, pixelSizeData, pixelSizeData);
        
        if (minValue > maxValue)
            minValue = maxValue;
        
        if (maxValue < minValue)
            maxValue = minValue;

        if (minTempValue > maxTempValue)
            minTempValue = maxTempValue;
        
        if (maxTempValue < minTempValue)
            maxTempValue = minTempValue;

        if (sliderPanel != null) {
            @SuppressWarnings("unused")
            SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
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
                    invalidateCache();
                    clearColorBarImgCachRqst = true;
                }
            };
        }
        else {
            invalidateCache();
            clearColorBarImgCachRqst = true;
        }
        
        invalidateCache();
        clearColorBarImgCachRqst = true;
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
        long nowMillis = System.currentTimeMillis();
        long deltaMillis = nowMillis - lastClearImgCacheRqstMillis;
        if (deltaMillis < minPeriodToRecreateCacheMillis) {
            threadPool.schedule(() -> {
                if (System.currentTimeMillis() - lastClearImgCacheRqstMillis >= minPeriodToRecreateCacheMillis)
                    clearImgCachRqst = true;
            }, deltaMillis, TimeUnit.MILLISECONDS);
        }
        else {
            clearImgCachRqst = true;
        }
    }

    public boolean updateValues() {
        if (updatingFiles)
            return false;
        
        updatingFiles = true;
        long startmillis = System.currentTimeMillis();
        NeptusLog.pub().info("Start processing");
        
        int startDataSize = dataList.size();
        int startPredSise = dataPredictionList.size();

        boolean addedNewData = false;
        try {
            File[] fileList = FileUtil.getFilesFromDisk(baseFolderForCSVFiles, csvFilePattern);
            if (fileList != null && fileList.length > 0) {
                for (int i = (readAllOrLastOfOrderedFiles ? 0 : fileList.length -1); i < fileList.length; i++) {
                    File csvFx = fileList[i];
                    if (isLastModifiedDifferent(csvFx, dataReadFiles)) {
                        try {
                            addedNewData = loadDataFile(csvFx) || addedNewData;
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e.toString());
                        }
                    }
                }
            }
            
            File[] folders = FileUtil.getFoldersFromDisk(baseFolderForCSVFiles, null);
            if (folders != null && folders.length > 0) {
                for (File folder : folders) {
                    fileList = FileUtil.getFilesFromDisk(folder, csvFilePattern);
                    if (fileList != null && fileList.length > 0) {
                        for (int i = (readAllOrLastOfOrderedFiles ? 0 : fileList.length -1); i < fileList.length; i++) {
                            File csvFx = fileList[i];
                            if (isLastModifiedDifferent(csvFx, dataReadFiles)) {
                                try {
                                    addedNewData = loadDataFile(csvFx) || addedNewData;
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e.toString());
                                }
                            }
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
                if (fileList != null && fileList.length > 0) {
                    for (int i = 0; i < fileList.length; i++) {
                        File totFx = fileList[i];
                        if (isLastModifiedDifferent(totFx, dataPredictionReadFiles)) {
                            reload = true;
                        }
                    }
                }
                
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
            
            
            boolean updatedMinMaxValues = cleanupOldData();
            if (addedNewData && !updatedMinMaxValues)
                recalcMinMaxValues();
        }
        catch (Exception e) {
            // cleanData();
            NeptusLog.pub().warn(e.toString());
        }
        
        updatingFiles = false;
        NeptusLog.pub().info(String.format("End processing. Took %s  [data size %d from %d | prediction %d from %d]",
                        DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - startmillis),
                        dataList.size() - startDataSize, dataList.size(), dataPredictionList.size() - startPredSise,
                        dataPredictionList.size()));
        
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

        recalcMinMaxValues();
    }

    /**
     * @param csvFx
     */
    private boolean loadDataFile(File csvFx) {
        try {
            CSVDataParser csv = new CSVDataParser(csvFx);
            if (printDebug)
                System.out.println("Processing file " + csvFx.getAbsolutePath());
            
            if (autoCleanData) {
                long ageMillis = dataAgeToCleanInMinutes * DateTimeUtil.MINUTE;
                csv.setMillisMaxAge(ageMillis);
            }
            
            csv.parse();
            return updateValues(dataList, csv.getPoints(), true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    private boolean updateValues(ArrayList<BaseData> list, ArrayList<BaseData> points, boolean dataOrPrediction) {
        boolean dataUpdated = false;
        
        long st = System.currentTimeMillis();
        
        if (autoCleanData && dataOrPrediction) {
            boolean updateValues = false;
            long curTimeMillis = System.currentTimeMillis();
            long maxAgeMillis = dataAgeToCleanInMinutes * DateTimeUtil.MINUTE;
            updateValues = points.removeIf(dp -> curTimeMillis - dp.getTimeMillis() > maxAgeMillis);
            if (updateValues)
                recalcMinMaxValues();
        }
        
        AtomicBoolean res = new AtomicBoolean(false);
        points.stream().forEach(testPoint -> {
            list.add(testPoint);
            res.set(true);

            if (dataOrPrediction) {
                updateTimeValuesMinMax(testPoint);
                updateRhodamineValuesMinMax(testPoint);
            }
            else {
                updatePredictionValuesMinMax(testPoint);
            }
            updateTempValuesMinMax(testPoint);
            updateDepthValuesMinMax(testPoint);
        });
        dataUpdated = res.get();

        if (printDebug)
            System.out.println("List size: " + list.size() + " took: " + DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - st) + (dataOrPrediction ? "" : " (prediction)"));
        return dataUpdated;
    }

    private boolean cleanupOldData() {
        if (autoCleanData) {
            boolean updateValues = false;
            long curTimeMillis = System.currentTimeMillis();
            long maxAgeMillis = dataAgeToCleanInMinutes * DateTimeUtil.MINUTE;
            updateValues = dataList.removeIf(dp -> curTimeMillis - dp.getTimeMillis() > maxAgeMillis);

            if (updateValues)
                recalcMinMaxValues();
            
            return updateValues;
        }
        
        return false;
    }

    private void recalcMinMaxValues() {
        clearTimeAndDepthValues();
        updateTimeAndDepthDataValues();
        updateDepthPredictionValues();
    }
    
    private void clearTimeAndDepthValues() {
        // Reset values
        oldestTimestamp = new Date().getTime();
        newestTimestamp = 0;

        oldestDepth = Double.MAX_VALUE;
        newestDepth = 0;
        
        clearRhodPredMinMaxValues();
        clearTempPredMinMaxValues();
    }

    private void clearRhodPredMinMaxValues() {
        oldestRhod = Double.MAX_VALUE;
        newestRhod = 0;
        
        oldestPred = Double.MAX_VALUE;
        newestPred = 0;
    }

    private void clearTempPredMinMaxValues() {
        oldestTemp = Double.MAX_VALUE;
        newestTemp = 0;
        
        oldestTemp = Double.MAX_VALUE;
        newestTemp = 0;
    }

    private void triggerRhodPredMinMaxValuesCalc() {
        clearRhodPredMinMaxValues();
        
        for (BaseData pt : dataList.toArray(new BaseData[dataList.size()])) {
            updateRhodamineValuesMinMax(pt);
            updateTempValuesMinMax(pt);
        }

        for (BaseData pt : dataPredictionList.toArray(new BaseData[dataPredictionList.size()])) {
            updatePredictionValuesMinMax(pt);
        }
        
        updateRhodamineRangeTexts();
        updateTempRangeTexts();
        updatePredictionRangeTexts();
    }

    private void updateTimeAndDepthDataValues() {
        // Calc new values
        for (BaseData pt : dataList.toArray(new BaseData[dataList.size()])) {
            updateTimeValuesMinMax(pt);
            updateDepthValuesMinMax(pt);
            updateRhodamineValuesMinMax(pt);
            updateTempValuesMinMax(pt);
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
        }
        if (pt.getTimeMillis() > 0 && pt.getTimeMillis() > newestTimestamp) {
            newestTimestampSelection = newestTimestamp = pt.getTimeMillis();
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
        }
        if (pt.getRhodamineDyePPB() > newestRhod) {
            newestRhod = pt.getRhodamineDyePPB();
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
        }
        if (pt.getRhodamineDyePPB() * predictionScaleFactor > newestPred) {
            newestPred = pt.getRhodamineDyePPB() * predictionScaleFactor;
        }
    }

    private void updateDepthValuesMinMax(BaseData pt) {
        if (pt == null)
            return;
            
        // Depth
        if (!Double.isNaN(pt.getDepth()) && pt.getDepth() < oldestDepth) {// && pt.getTimestamp() > minDate) {
            oldestDepth = pt.getDepth();
        }
        if (!Double.isNaN(pt.getDepth())) {
            double testLowerDepth = (!Double.isNaN(pt.getDepthLower()) && pt.getDepthLower() > pt.getDepth()) ? pt
                    .getDepthLower() : pt.getDepth();
            if (testLowerDepth > newestDepth) {
                newestDepthSelection = newestDepth = testLowerDepth;
            }
        }
    }

    private void updateTempValuesMinMax(BaseData pt) {
        if (pt == null)
            return;
        
        if (!validPoint(pt, pt.getTemperature(), false, -1, true))
            return;

        // Time
        if (pt.getTemperature() < oldestTemp) {
            oldestTemp = pt.getTemperature();
        }
        if (pt.getTemperature() > newestTemp) {
            newestTemp = pt.getTemperature();
        }
    }

    @Periodic(millisBetweenUpdates = 500)
    public boolean updateExtraGUIValues() {
        try {
            boolean selTimeMinForce = false;
            boolean selTimeMaxForce = false;
            if (!timeSlider.getValueIsAdjusting() && timeSlider.getValue() == timeSlider.getMinimum())
                selTimeMinForce = true;
            if (!timeSlider.getValueIsAdjusting() && timeSlider.getUpperValue() == timeSlider.getMaximum())
                selTimeMaxForce = true;
            timeSlider.setMinimum((int)(oldestTimestamp / timeStampSliderScale));
            timeSlider.setMaximum((int)(newestTimestamp / timeStampSliderScale));
            updateSliderWithValues(timeSlider, oldestTimestamp, newestTimestamp, oldestTimestampSelection,
                    newestTimestampSelection, timeStampSliderScale);
            updateTimeSliderTime();
            if (selTimeMinForce)
                timeSlider.setValue(timeSlider.getMinimum());
            if (selTimeMaxForce)
                timeSlider.setUpperValue(timeSlider.getMaximum());

            updateRhodamineRangeTexts();
            updatePredictionRangeTexts();
            updateTempRangeTexts();
            
            boolean selDepthMinForce = false;
            boolean selDepthMaxForce = false;
            if (!depthSlider.getValueIsAdjusting() && depthSlider.getValue() == depthSlider.getMinimum())
                selDepthMinForce = true;
            if (!depthSlider.getValueIsAdjusting() && depthSlider.getUpperValue() == depthSlider.getMaximum())
                selDepthMaxForce = true;
            depthSlider.setMinimum((int)(oldestDepth / depthSliderScale));
            depthSlider.setMaximum((int)(newestDepth / depthSliderScale));
            updateSliderWithValues(depthSlider, oldestDepth, newestDepth, oldestDepthSelection,
                    newestDepthSelection, depthSliderScale);
            updateDepthSliderTime();
            if (selDepthMinForce)
                depthSlider.setValue(depthSlider.getMinimum());
            if (selDepthMaxForce)
                depthSlider.setUpperValue(depthSlider.getMaximum());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.toString());
        }
        
        return true;
    }

    /**
     * @param slider
     * @param oldestVal
     * @param newestVal
     * @param oldestValSelection
     * @param newestValSelection
     * @param scale
     */
    private void updateSliderWithValues(RangeSlider slider, double oldestVal, double newestVal,
            double oldestValSelection, double newestValSelection, double scale) {
        if (slider.getValueIsAdjusting())
            return;
        slider.setUpperValue((int)(newestValSelection / scale));
        slider.setValue((int)(oldestValSelection / scale));
        
        if(slider.getValue() == slider.getMaximum())
            slider.setValue(slider.getValue());
        if(slider.getUpperValue() == slider.getMinimum())
            slider.setUpperValue(slider.getUpperValue());
        
        if (slider.getUpperValue() == slider.getValue() && slider.getValue() == 0) {
           slider.setValue(slider.getMinimum());
           slider.setUpperValue(slider.getMaximum());
        }
    }

    @Periodic(millisBetweenUpdates = 500)
    public boolean updateExtraGUI() {
        if (System.currentTimeMillis() - lastPaintMillis > 2000) {
            setupExtraGui(false, null);
        }

        if (System.currentTimeMillis() - lastPaintDataMillis > 2000)
            dataPanelSetEnabled(false);
        else
            dataPanelSetEnabled(true);

        if (System.currentTimeMillis() - lastPaintPredictonMillis > 2000)
            predictionPanelSetEnabled(false);
        else
            predictionPanelSetEnabled(true);

        return true;
    }
    
    public void setupExtraGui(boolean mode, StateRenderer2D source) {
        if (updatingExtraGui)
            return;
        
        updatingExtraGui = true;

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
        
        updatingExtraGui = false;
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
        offScreenImageControlData.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);

        paintColorBar(offScreenImageControlColorBar, g, renderer, visibleDataVar);            

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
                g2.drawString(rhodamineImcString, 1, 123);
                g2.setColor(Color.WHITE);
                g2.drawString(rhodamineImcString, 2, 124);
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
            lastClearImgCacheRqstMillis = System.currentTimeMillis();
        }
        
        if (clearColorBarImgCachRqst) {
            offScreenImageControlColorBar.triggerImageRebuild();
            offScreenImageControlColorBarPred.triggerImageRebuild();
            clearColorBarImgCachRqst = false;
        }
    }

    /**
     * @param g
     */
    private void paintLegend(Graphics2D g) {
        String name = getName();

        // Legend
        Graphics2D gl = (Graphics2D) g.create();
        gl.translate(10-3, 35+5);
        gl.setColor(Color.WHITE);
        gl.drawString(name, 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.translate(1, 1);
        gl.setColor(Color.BLACK);
        gl.drawString(name, 0, 0); // (int)pt.getX()+17, (int)pt.getY()+2
        gl.dispose();
    }

    /**
     * @param g
     * @param renderer
     */
    private void paintColorBar(OffScreenLayerImageControl offScreenImageControlColorBar, Graphics2D g, StateRenderer2D renderer, VisibleDataVariableEnum visibleDataVar) {
        boolean recreateImageColorBar = offScreenImageControlColorBar.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImageColorBar) {
            String varName;
            String unit;
            double minVal;
            double maxVal;
            int ox = 0;
            int oy = 0;
            switch (visibleDataVar) {
                case Temperature:
                    varName = "Temperature";
                    unit = "\u00B0C";
                    minVal = this.minTempValue;
                    maxVal = this.maxTempValue;
                    ox = 0;
                    oy = 120;
                    break;
                case RhodaminePPB:
                default:
                    varName = "Rhodamine";
                    unit = "ppb";
                    minVal = this.minValue;
                    maxVal = this.maxValue;
                    break;
            }

            Graphics2D g2 = offScreenImageControlColorBar.getImageGraphics();
            g2 = (Graphics2D) g2.create();
            g2.translate(10 + ox, 50 + oy);
            ColorBarPainterUtil.paintColorBar(g2, colorMap, varName, unit, minVal, maxVal);
            g2.dispose();
        }
        offScreenImageControlColorBar.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
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
        
        tmpLst.removeIf(p -> {
            if (p.timeMillis != filterTime)
                return true;
            return false;
        });

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

    private void paintDataWorker(StateRenderer2D renderer, Graphics2D g, boolean prediction, ArrayList<BaseData> dList) {
        Graphics2D g2 = (Graphics2D) g.create();

        this.curRenderer = renderer;
        
        long curtime = System.currentTimeMillis();
        
        ArrayList<BaseData> toProcessPoints = new ArrayList<>(dList);
        int initialPointsNumber = toProcessPoints.size();
        LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
        LongAccumulator paintedPts = new LongAccumulator((r, i) -> r += i, 0);
        final int gridSpacing = pixelSizeData / 2;
        
        ArrayList<String> systemsToIgnoreRhodamineList = new ArrayList<>();
        String[] sti = systemsToIgnoreForRhodamine == null ? new String[0] : systemsToIgnoreForRhodamine.split(",");
        for (String str : sti) {
            if (str == null || str.trim().isEmpty())
                continue;
            systemsToIgnoreRhodamineList.add(str.trim().toLowerCase());
        }
        ArrayList<String> systemsToIgnoreTemperatureList = new ArrayList<>();
        sti = systemsToIgnoreForTemperature == null ? new String[0] : systemsToIgnoreForTemperature.split(",");
        for (String str : sti) {
            if (str == null || str.trim().isEmpty())
                continue;
            systemsToIgnoreTemperatureList.add(str.trim().toLowerCase());
        }
        
        Map<Point2D, Object[]> processedPoints = toProcessPoints.parallelStream()
                .collect(HashMap<Point2D, Object[]>::new, (r, bp) -> {
                    boolean isToIgnoreRhodamine = false;
                    boolean isToIgnoreTemperature = false;
                    if (bp.getSourceSystem() == null || bp.getSourceSystem().trim().isEmpty()) {
                        isToIgnoreRhodamine = systemsToIgnoreRhodamineList.contains("unknown");
                        isToIgnoreTemperature = systemsToIgnoreTemperatureList.contains("unknown");
                    }
                    else {
                        isToIgnoreRhodamine = systemsToIgnoreRhodamineList.contains(bp.getSourceSystem().toLowerCase());
                        isToIgnoreTemperature = systemsToIgnoreTemperatureList.contains(bp.getSourceSystem().toLowerCase());
                    }
                    if (isToIgnoreRhodamine && isToIgnoreTemperature)
                        return;
                    
                    double latV = bp.getLat();
                    double lonV = bp.getLon();

                    if (Double.isNaN(latV) || Double.isNaN(lonV))
                        return;

                    LocationType l = new LocationType();
                    l.setLatitudeDegs(latV);
                    l.setLongitudeDegs(lonV);

                    Point2D pt = renderer.getScreenPosition(l);
                    double x = pt.getX();
                    double y = pt.getY();
                    x = ((int) x) / gridSpacing * gridSpacing;
                    y = ((int) y) / gridSpacing * gridSpacing;
                    pt.setLocation(x, y);

                    if (!isVisibleInRender(pt, renderer))
                        return;
                    
                    if (!prediction && !isTimeValid(bp))
                        return;

                    boolean validPoint = validPoint(bp, !prediction ? true : false);
                    if (!validPoint)
                        return;
                    if (!isDepthValid(bp))
                        return;

//                    if (!Double.isFinite(bp.getRhodamineDyePPB()))
//                        return;

                    r.put(pt, new Object[] { bp.timeMillis, isToIgnoreRhodamine ? Double.NaN : bp.getRhodamineDyePPB(), 
                            isToIgnoreTemperature ? Double.NaN : bp.getTemperature() });
                    visiblePts.accumulate(1);
                }, (r, a) -> {
                    a.keySet().stream().forEach(p -> {
                        if (r.containsKey(p)) {
                            Object[] rVal = r.get(p);
                            Object[] aVal = a.get(p);

                            rVal[0] = Math.max((long) rVal[0], (long) aVal[0]);
                            
                            if (!Double.isFinite((double) rVal[1]))
                                rVal[1] = aVal[1];
                            else if (Double.isFinite((double) aVal[1]))
                                rVal[1] = ((double) rVal[1] + (double) aVal[1]) / 2.;

                            if (!Double.isFinite((double) rVal[2]))
                                rVal[2] = aVal[2];
                            else if (Double.isFinite((double) aVal[2]))
                                rVal[2] = ((double) rVal[2] + (double) aVal[2]) / 2.;
                        }
                        else {
                            r.put(p, a.get(p));
                        }
                    });
                });

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        processedPoints.keySet().parallelStream().forEach(pt -> {
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(pt.getX(), pt.getY());
            
            Object[] values = processedPoints.get(pt);
            long timeMillis = (long) values[0];  
            
            double value;  
            Color color;
            if (prediction) {
                value = (double) values[1];  
                if (!Double.isFinite((double) value))
                    return;
                color = colorMap.getColor((value * (prediction ? predictionScaleFactor : 1) - minValue) / maxValue);
            }
            else {
                switch (visibleDataVar) {
                    case Temperature:
                        value = (double) values[2];
                        if (!Double.isFinite((double) value))
                            return;
                        color = colorMap.getColor((value - minTempValue) / maxTempValue);
                        break;
                    case RhodaminePPB:
                    default:
                        value = (double) values[1];  
                        if (!Double.isFinite((double) value))
                            return;
                        color = colorMap.getColor((value * (prediction ? predictionScaleFactor : 1) - minValue) / maxValue);
                        break;
                }
            }
            
            if (!prediction) {
                if (curtime - timeMillis > DateTimeUtil.MINUTE * 5)
                    color = ColorUtils.setTransparencyToColor(color, 150); // 128
            }
            else {
                color = ColorUtils.setTransparencyToColor(color, 128);
            }
            gt.setColor(color);
            
            // double rot =  -renderer.getRotation();
            // gt.rotate(rot);
            if (!prediction) {
                gt.fill(circle);
            }
            else {
                double r= Math.PI / 4;
                gt.rotate(r);
                gt.fill(diamond);
                gt.rotate(-r);
            }
            //gt.rotate(-rot);
                        
            gt.dispose();
            
            paintedPts.accumulate(1);
        });
        
        g2.dispose();
        
        if (printPaintDebug) {
            System.out.println(String.format("Paint%s took %s for %d points ammoung %d visible and final merged %d and %d painted for %s", 
                    prediction ? " prediction" : "", DateTimeUtil.milliSecondsToFormatedString(
                            System.currentTimeMillis() - curtime), initialPointsNumber, visiblePts.intValue(),
                            processedPoints.size(), paintedPts.get(), visibleDataVar.toString()));
        }
    }

    private boolean validPoint(BaseData point, boolean validateTime) {
        return validPoint(point, point.getRhodamineDyePPB(), true, minValue, validateTime);
    }

    private boolean validPoint(BaseData point, double value, boolean validateMin, double minValue,
            boolean validateTime) {
        if (!Double.isFinite(value))
            return false;
        
        if (validateMin && value < minValue)
            return false;
        
        if (validateTime) {
            if(!isTimeValid(point))
                return false;
        }
        if (!isDepthValid(point))
            return false;
        
        return true;
    }

    /**
     * @param point
     * @return
     */
    private boolean isDepthValid(BaseData point) {
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


    /**
     * @param point
     * @return
     */
    private boolean isTimeValid(BaseData point) {
        if (point.getTimeMillis() / timeStampSliderScale * timeStampSliderScale < oldestTimestampSelection
                || point.getTimeMillis() / timeStampSliderScale * timeStampSliderScale > newestTimestampSelection)
            return false;
        else
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
        String sysName = msg.getSourceName();
        if (sysName.trim().toLowerCase().contains("unknown"))
            sysName = "unknown";
        
        showConsolePopupEvent(sysName, msg);

        if (systemsToIgnoreForIMCRhodamine != null && "all".equalsIgnoreCase(systemsToIgnoreForIMCRhodamine.trim()))
            return;
        ArrayList<String> systemsToIgnoreIMCRhodamineList = new ArrayList<>();
        String[] sti = systemsToIgnoreForIMCRhodamine == null ? new String[0] : systemsToIgnoreForIMCRhodamine.split(",");
        for (String str : sti) {
            if (str == null || str.trim().isEmpty())
                continue;
            systemsToIgnoreIMCRhodamineList.add(str.trim().toLowerCase());
        }
        boolean isToIgnoreRhodamine = systemsToIgnoreIMCRhodamineList.contains(sysName);
        if (isToIgnoreRhodamine)
            return;
        
        // From any system
//        System.out.println(msg.asJSON());
        String accepted = "";
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
                pt.setRhodamineDyePPB(msg.getValue());

                String sourceSystem = msg.getSourceName();
                if (sourceSystem.startsWith("unknown"))
                    sourceSystem = new ImcId16(msg.getSrc()).toPrettyString();
                pt.setSourceSystem(sourceSystem);

                ArrayList<BaseData> data = new ArrayList<>(); 
                data.add(pt);
                updateValues(dataList, data, true);
            }
            else {
                accepted = " " + delta + "<=" + maxDeltaTimeBetweenEstimatedStateAndMessageDataReceivedMillis
                        + " ? " + (delta <= maxDeltaTimeBetweenEstimatedStateAndMessageDataReceivedMillis);
            }
        }
        
        double valueReceived = msg.getValue();
        if (!Double.isNaN(valueReceived)) {
            rhodamineImcString = "" + MathMiscUtils.round(valueReceived, 2) + "ppb @ " 
                    + DateTimeUtil.timeFormatterUTC.format(new Date(rhodamineImcStringMillis))
                    + accepted;
            rhodamineImcStringMillis = msg.getTimestampMillis();
        }
    }

    /**
     * @param sysName
     * @param msg
     * @return
     */
    private void showConsolePopupEvent(String sysName, RhodamineDye msg) {
        if (!popupForRhodamine)
            return;
        
        double valueReceived = msg.getValue();
        if (!Double.isNaN(valueReceived)) {
            if (valueReceived >= popupForRhodamineThreshold) {
                String str = sysName + " :: " + MathMiscUtils.round(valueReceived, 2) + "ppb @ "
                        + DateTimeUtil.timeFormatterUTC.format(new Date(rhodamineImcStringMillis));
                getConsole().post(
                        Notification.newNotification(I18n.text("Rhodamine value received"), str, popupForRhodamineType)
                                .src(sysName));
            }
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
        if (sPos.getX() < 0 - offScreenBufferPixel || sPos.getY() < 0 - offScreenBufferPixel)
            return false;
        else if (sPos.getX() > rendDim.getWidth() + offScreenBufferPixel || sPos.getY() > rendDim.getHeight() + offScreenBufferPixel)
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
            offScreenImageControlPrediction.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);

            paintColorBar(offScreenImageControlColorBarPred, g, renderer, VisibleDataVariableEnum.RhodaminePPB);            

            paintLegend(g);
        }
    }
}
