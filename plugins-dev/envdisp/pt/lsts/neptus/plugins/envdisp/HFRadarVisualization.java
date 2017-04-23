/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 22, 2013
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="HF Radar Visualization", author="Paulo Dias", version="1.0", icon="pt/lsts/neptus/plugins/envdisp/hf-radar.png")
@LayerPriority(priority = -50)
public class HFRadarVisualization extends ConsolePanel implements Renderer2DPainter, IPeriodicUpdates, ConfigurationListener {

    /*
     * Currents, wind, waves, SST 
     */

    @NeptusProperty(name = "Show currents", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showCurrents = true;

    @NeptusProperty(name = "Show SST", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showSST = true;

    @NeptusProperty(name = "Show wind", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showWind = true;

    @NeptusProperty(name = "Show waves", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showWaves = false;

    @NeptusProperty(name = "Show currents legend", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showCurrentsLegend = true;

    @NeptusProperty(name = "Show currents legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category="Visibility")
    public int showCurrentsLegendFromZoomLevel = 13;

    @NeptusProperty(name = "Show SST legend", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showSSTLegend = true;

    @NeptusProperty(name = "Show SST legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category="Visibility")
    public int showSSTLegendFromZoomLevel = 11;

    @NeptusProperty(name = "Show waves legend", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean showWavesLegend = true;

    @NeptusProperty(name = "Show waves legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category="Visibility")
    public int showWavesLegendFromZoomLevel = 13;

    @NeptusProperty(name = "Minutes between file updates", category="Data Update")
    public long updateFileDataMinutes = 5;

    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR, category="Data Update")
    public int dateLimitHours = 12;
    
    @NeptusProperty(name = "Use data x hour in the future (hours)", userLevel = LEVEL.REGULAR, category="Data Update")
    public int dateHoursToUseForData = 1;
    
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.ADVANCED, category="Data Update")
    public boolean ignoreDateLimitToLoad = false;
    
    @NeptusProperty(name = "Request data from Web", editable = false, userLevel = LEVEL.ADVANCED, category="Test", description = "Don't use this (testing purposes).")
    public boolean requestFromWeb = false;

    @NeptusProperty(name = "Load data from file (hfradar.txt)", editable = false, userLevel = LEVEL.ADVANCED, category="Test", description = "Don't use this (testing purposes).")
    public boolean loadFromFile = false;
    
    @NeptusProperty(name = "Show currents as most recent (true) or mean (false) value", userLevel = LEVEL.REGULAR, category="Data Update")
    public boolean hfradarUseMostRecentOrMean = true;

    @NeptusProperty(name = "Use color map for wind", userLevel = LEVEL.REGULAR, category="Visibility")
    public boolean useColorMapForWind = true;

    @NeptusProperty(name = "Base Folder For Currents TUV or netCDF Files", userLevel = LEVEL.REGULAR, category = "Data Update", 
            description = "The folder to look for currents data. Admissible files '*.tuv' and '*.nc'. NetCDF variables used: lat, lon, time, u, v.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForCurrentsTUVFiles = new File("IHData/CODAR");

    @NeptusProperty(name = "Base Folder For Meteo netCDF Files", userLevel = LEVEL.REGULAR, category = "Data Update", 
            description = "The folder to look for meteo data (wind and SST). Admissible files '*.nc'. NetCDF variables used: lat, lon, time, u, v, sst.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForMeteoNetCDFFiles = new File("IHData/METEO");

    @NeptusProperty(name = "Base Folder For Waves netCDF Files", userLevel = LEVEL.REGULAR, category = "Data Update", 
            description = "The folder to look for waves (significant height, peak period and direction) data. Admissible files '*.nc'. NetCDF variables used: lat, lon, time, hs, tp, pdir.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForWavesNetCDFFiles = new File("IHData/WAVES");
    
    @NeptusProperty(name = "Show currents visible data date-time interval", userLevel = LEVEL.ADVANCED, category = "Test", 
            description = "Draws the string with visible curents data date-time interval.")
    public boolean showDataDebugLegend = false;
    
    private static final String tuvFilePattern = ".\\.tuv$";
    private static final String netCDFFilePattern = ".\\.nc$";
    private static final String currentsFilePatternTUV = tuvFilePattern; // "^TOTL_TRAD_\\d{4}_\\d{2}_\\d{2}_\\d{4}\\.tuv$";
    private static final String currentsFilePatternNetCDF = netCDFFilePattern; // "^CODAR_TRAD_\\d{4}_\\d{2}_\\d{2}_\\d{4}\\.nc$";
    private static final String meteoFilePattern = netCDFFilePattern; // "^meteo_\\d{8}\\.nc$";
    private static final String wavesFilePattern = netCDFFilePattern; // "^waves_[a-zA-Z]{1,2}_\\d{8}\\.nc$";

    private final Font font8Pt = new Font("Helvetica", Font.PLAIN, 8);

    static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    static final SimpleDateFormat dateTimeFormaterSpacesUTC = new SimpleDateFormat("yyyy MM dd  HH mm ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    private static final SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    private static final SimpleDateFormat timeFormaterUTC = new SimpleDateFormat("HH':'mm") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    private long updateSeconds = 60;
    private long lastMillisFileDataUpdated = -1;

    private final static int ARROW_RADIUS = 12;
    private final static Path2D.Double arrow = new Path2D.Double();
    static {
        arrow.moveTo(-5, 6);
        arrow.lineTo(0, -6);
        arrow.lineTo(5, 6);
        arrow.lineTo(0, 3);
        arrow.lineTo(-5, 6);
        arrow.closePath();
    }

    private final static int SST_RADIUS = 8;
    private final static Ellipse2D circle = new Ellipse2D.Double(-SST_RADIUS / 2., -SST_RADIUS / 2., SST_RADIUS, SST_RADIUS);
    
    private final static int WIND_RADIUS = 28;
    private final static Path2D.Double windPoleKnots = new Path2D.Double();
    static {
        windPoleKnots.moveTo(0, 0);
        windPoleKnots.lineTo(0, 14*2);
        windPoleKnots.closePath();
    }
    private final static Path2D.Double wind50Knots1 = new Path2D.Double();
    static {
        wind50Knots1.moveTo(0, 14*2);
        wind50Knots1.lineTo(-8*2, 14*2);
        wind50Knots1.lineTo(0, 12*2);
        wind50Knots1.closePath();
    }
    private final static Path2D.Double wind10Knots1 = new Path2D.Double();
    static {
        wind10Knots1.moveTo(0, 14*2);
        wind10Knots1.lineTo(-8*2, 14*2);
        wind10Knots1.closePath();
    }
    private final static Path2D.Double wind5Knots2 = new Path2D.Double();
    static {
        wind5Knots2.moveTo(0, 12*2);
        wind5Knots2.lineTo(-4*2, 12*2);
        wind5Knots2.closePath();
    }
    private final static Path2D.Double wind10Knots2 = new Path2D.Double();
    static {
        wind10Knots2.moveTo(0, 12*2);
        wind10Knots2.lineTo(-8*2, 12*2);
        wind10Knots2.closePath();
    }
    private final static Path2D.Double wind5Knots3 = new Path2D.Double();
    static {
        wind5Knots3.moveTo(0, 10*2);
        wind5Knots3.lineTo(-4*2, 10*2);
        wind5Knots3.closePath();
    }
    private final static Path2D.Double wind10Knots3 = new Path2D.Double();
    static {
        wind10Knots3.moveTo(0, 10*2);
        wind10Knots3.lineTo(-8*2, 10*2);
        wind10Knots3.closePath();
    }
    private final static Path2D.Double wind5Knots4 = new Path2D.Double();
    static {
        wind5Knots4.moveTo(0, 8*2);
        wind5Knots4.lineTo(-4*2, 8*2);
        wind5Knots4.closePath();
    }
    private final static Path2D.Double wind10Knots4 = new Path2D.Double();
    static {
        wind10Knots4.moveTo(0, 8*2);
        wind10Knots4.lineTo(-8*2, 8*2);
        wind10Knots4.closePath();
    }
    private final static Path2D.Double wind5Knots5 = new Path2D.Double();
    static {
        wind5Knots5.moveTo(0, 6*2);
        wind5Knots5.lineTo(-4*2, 6*2);
        wind5Knots5.closePath();
    }

    //  http://hfradar.ndbc.noaa.gov/tab.php?from=2013-06-22%2015:00:00&to=2013-06-22%2015:00:00&p=1&lat=38.324420427006515&lng=-119.94323730468749&lat2=35.69299463209881&lng2=-124.33776855468749
    //  http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2013-06-23%2009:34:00&to=2013-06-23%2021:34:00&lat=37.78799270017669&lng=-122.39269445535145&lat2=37.78781729434937&lng2=-122.39236722585163
    private final String noaaURL = "http://hfradar.ndbc.noaa.gov/tabdownload.php?" +
    		"from=#FROM_DATE#%20#FROM_TIME#:00&to=#TO_DATE#%20#TO_TIME#:00&lat=#LAT1#&lng=#LNG1#&lat2=#LAT2#&lng2=#LNG2#";
    // lat lon speed (cm/s)    degree  acquired    resolution (km) origin
    //private double noaaMaxLat=75.40885422846455, noaaMinLng=-42.1875, noaaMinLat=12.21118019150401, noaaMaxLng=177.1875;
    private final double noaaMaxLat=55.47885346331034, noaaMaxLng=-61.87500000000001, noaaMinLat=14.093957177836236, noaaMinLng=-132.1875;
    
    private static final String sampleNoaaFile = "hfradar-noaa-sample1.txt";
    // private static final String sampleTuvFile = "TOTL_TRAD_2013_07_04_1100.tuv";
    // private static final String sampleMeteoFile = "meteo_20130705.nc";
    // private static final String sampleWavesFile = "waves_S_20130704.nc";
    
    protected final double m_sToKnotConv = 1.94384449244;
    
    private final ColorMap colorMapCurrents = ColorMapFactory.createJetColorMap(); //new InterpolationColorMap("RGB", new double[] { 0.0, 0.1, 0.3, 0.5, 1.0 }, new Color[] {
            //new Color(0, 0, 255), new Color(0, 0, 255), new Color(0, 255, 0), new Color(255, 0, 0), new Color(255, 0, 0) });
    // private final double minCurrentCmS = 0;
    private final double maxCurrentCmS = 200;

    private final ColorMap colorMapSST = ColorMapFactory.createJetColorMap();
    private final double minSST = -10;
    private final double maxSST = 40;

    private final ColorMap colorMapWind = colorMapCurrents;
    // private final double minWind = 0;
    private final double maxWind = 65 / m_sToKnotConv;

    private final ColorMap colorMapWaves = ColorMapFactory.createJetColorMap();
    // private final double minWaves = 0;
    private final double maxWaves = 7;

    private OffScreenLayerImageControl offScreen = new OffScreenLayerImageControl();
    
    private final HttpClientConnectionHelper httpComm = new HttpClientConnectionHelper();
    private HttpGet getHttpRequest;
    
    // ID is lat/lon
    private final HashMap<String, HFRadarDataPoint> dataPointsCurrents = new HashMap<>();
    private final HashMap<String, SSTDataPoint> dataPointsSST = new HashMap<>();
    private final HashMap<String, WindDataPoint> dataPointsWind = new HashMap<>();
    private final HashMap<String, WavesDataPoint> dataPointsWaves = new HashMap<>();

    @PluginDescription(name="HF Radar Visualization Layer", icon="pt/lsts/neptus/plugins/envdisp/hf-radar.png")
    private class HFRadarConsoleLayer extends ConsoleLayer {
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
            
            paintWorker(g, renderer);
        }
    }
    
    private final HFRadarConsoleLayer consoleLayer = new HFRadarConsoleLayer();

    public HFRadarVisualization(ConsoleLayout console) {
        super(console);
        httpComm.initializeComm();
    }
    
    @Override
    public void initSubPanel() {
        getConsole().addMapLayer(consoleLayer, false);
        
//        HashMap<String, HFRadarDataPoint> tdp = processNoaaHFRadarTest();
//        mergeCurrentsDataToInternalDataList(tdp);
//        
//        tdp = processTuvHFRadarTest(sampleTuvFile);
//        if (tdp != null && tdp.size() > 0)
//            mergeCurrentsDataToInternalDataList(tdp);
//
//        HashMap<?, ?>[] meteodp = processMeteoFile(sampleMeteoFile);
//        HashMap<String, SSTDataPoint> sstdp = (HashMap<String, SSTDataPoint>) meteodp[0];
//        if (sstdp != null && sstdp.size() > 0)
//            mergeSSTDataToInternalDataList(sstdp);
//        HashMap<String, WindDataPoint> winddp = (HashMap<String, WindDataPoint>) meteodp[1];
//        if (winddp != null && winddp.size() > 0)
//            mergeWindDataToInternalDataList(winddp);
//
//        HashMap<String, WavesDataPoint> wavesdp = processWavesFile(sampleWavesFile);
//        if (wavesdp != null && wavesdp.size() > 0)
//            mergeWavesDataToInternalDataList(wavesdp);
//        
//        update();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        getConsole().removeMapLayer(consoleLayer);

        httpComm.cleanUp();
    }
    
    public String validateDataLimitHours(int value) {
        if (value < 3 && value > 24)
            return "Keep it between 3 and 24";
        return null;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return updateSeconds * 1000;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public synchronized boolean update() {
        NeptusLog.pub().info("Update");
        
//        if (false && requestFromWeb) {
//            HashMap<String, HFRadarDataPoint> dpLts = getNoaaHFRadarData();
//            if (dpLts != null) {
//                System.out.println(dpLts.size() + " ------------------------------");
//                
//            }
//        }
        if (lastMillisFileDataUpdated <= 0
                || System.currentTimeMillis() - lastMillisFileDataUpdated >= updateFileDataMinutes * 60 * 1000) {
            lastMillisFileDataUpdated = System.currentTimeMillis();
            loadCurrentsFromFiles();
            loadMeteoFromFiles();
            loadWavesFromFiles();
        }

        propertiesChanged();
        
        return true;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        lastMillisFileDataUpdated = -1;
        
        cleanDataPointsBeforeDate();
        updateValues();
        offScreen.triggerImageRebuild();
    }

    private Date createDateToMostRecent() {
        Date nowDate = new Date(System.currentTimeMillis() + dateHoursToUseForData * DateTimeUtil.HOUR);
        return nowDate;
    }
    
    private Date createDateLimitToRemove() {
        Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);
        return dateLimit;
    }
    
    private void updateValues() {
        Date nowDate = createDateToMostRecent();

        for (String dpID : dataPointsCurrents.keySet().toArray(new String[0])) {
            HFRadarDataPoint dp = dataPointsCurrents.get(dpID);
            if (dp == null)
                continue;
            updateHFRadarToUseMostRecentOrMean(nowDate, dp);
        }

        for (String dpID : dataPointsSST.keySet().toArray(new String[0])) {
            SSTDataPoint dp = dataPointsSST.get(dpID);
            if (dp == null)
                continue;
            dp.useMostRecent(nowDate);
        }

        for (String dpID : dataPointsWind.keySet().toArray(new String[0])) {
            WindDataPoint dp = dataPointsWind.get(dpID);
            if (dp == null)
                continue;
            dp.useMostRecent(nowDate);
        }

        for (String dpID : dataPointsWaves.keySet().toArray(new String[0])) {
            WavesDataPoint dp = dataPointsWaves.get(dpID);
            if (dp == null)
                continue;
            dp.useMostRecent(nowDate);
        }
    }

    /**
     * @param dp
     * @return
     */
    private void updateHFRadarToUseMostRecentOrMean(Date nowDate, HFRadarDataPoint dp) {
        if (hfradarUseMostRecentOrMean)
            dp.useMostRecent(nowDate);
        else
            dp.calculateMean(nowDate);
    }

    private void cleanDataPointsBeforeDate() {
        Date dateLimit = ignoreDateLimitToLoad ? null : createDateLimitToRemove();
        
        cleanCurrentsDataPointsBeforeDate(dateLimit);
        cleanSSTDataPointsBeforeDate(dateLimit);
        cleanWindDataPointsBeforeDate(dateLimit);
        cleanWavesDataPointsBeforeDate(dateLimit);
    }

    private void cleanCurrentsDataPointsBeforeDate(Date dateLimit) {
        if (dateLimit == null)
            return;
        
        for (String dpID : dataPointsCurrents.keySet().toArray(new String[0])) {
            HFRadarDataPoint dp = dataPointsCurrents.get(dpID);
            if (dp == null)
                continue;
            
            if (dp.getDateUTC().before(dateLimit))
                dataPointsCurrents.remove(dpID);
            else {
                // Cleanup historicalData
                dp.purgeAllBefore(dateLimit);
            }
        }
    }

    private void cleanSSTDataPointsBeforeDate(Date dateLimit) {
        if (dateLimit == null)
            return;
        
        for (String dpID : dataPointsSST.keySet().toArray(new String[0])) {
            BaseDataPoint<?> dp = dataPointsSST.get(dpID);
            if (dp == null)
                continue;
            
            if (dp.getDateUTC().before(dateLimit))
                dataPointsSST.remove(dpID);
            else {
                // Cleanup historicalData
                dp.purgeAllBefore(dateLimit);
            }
        }
    }

    private void cleanWindDataPointsBeforeDate(Date dateLimit) {
        if (dateLimit == null)
            return;
        
        for (String dpID : dataPointsWind.keySet().toArray(new String[0])) {
            BaseDataPoint<?> dp = dataPointsWind.get(dpID);
            if (dp == null)
                continue;
            
            if (dp.getDateUTC().before(dateLimit))
                dataPointsWind.remove(dpID);
            else {
                // Cleanup historicalData
                dp.purgeAllBefore(dateLimit);
            }
        }
    }

    private void cleanWavesDataPointsBeforeDate(Date dateLimit) {
        if (dateLimit == null)
            return;
        
        for (String dpID : dataPointsWaves.keySet().toArray(new String[0])) {
            BaseDataPoint<?> dp = dataPointsWaves.get(dpID);
            if (dp == null)
                continue;
            
            if (dp.getDateUTC().before(dateLimit))
                dataPointsWaves.remove(dpID);
            else {
                // Cleanup historicalData
                dp.purgeAllBefore(dateLimit);
            }
        }
    }

    public void mergeCurrentsDataToInternalDataList(HashMap<String, HFRadarDataPoint> toMergeData) {
        for (String dpId : toMergeData.keySet()) {
            HFRadarDataPoint dp = toMergeData.get(dpId);
            HFRadarDataPoint dpo = dataPointsCurrents.get(dpId);
            if (dpo == null) {
                dataPointsCurrents.put(dpId, dp);
                dpo = dp;
            }
            else {
                mergeDataPointsWorker(dp, dpo);
            }
        }
    }

    public void mergeSSTDataToInternalDataList(HashMap<String, SSTDataPoint> toMergeData) {
        for (String dpId : toMergeData.keySet()) {
            SSTDataPoint dp = toMergeData.get(dpId);
            SSTDataPoint dpo = dataPointsSST.get(dpId);
            if (dpo == null) {
                dataPointsSST.put(dpId, dp);
                dpo = dp;
            }
            else {
                mergeDataPointsWorker(dp, dpo);
            }
        }
        System.out.println(toMergeData.size() + " vs " + dataPointsSST.size());
    }

    public void mergeWindDataToInternalDataList(HashMap<String, WindDataPoint> toMergeData) {
        for (String dpId : toMergeData.keySet()) {
            WindDataPoint dp = toMergeData.get(dpId);
            WindDataPoint dpo = dataPointsWind.get(dpId);
            if (dpo == null) {
                dataPointsWind.put(dpId, dp);
                dpo = dp;
            }
            else {
                mergeDataPointsWorker(dp, dpo);
            }
        }
    }

    public void mergeWavesDataToInternalDataList(HashMap<String, WavesDataPoint> toMergeData) {
        for (String dpId : toMergeData.keySet()) {
            WavesDataPoint dp = toMergeData.get(dpId);
            WavesDataPoint dpo = dataPointsWaves.get(dpId);
            if (dpo == null) {
                dataPointsWaves.put(dpId, dp);
                dpo = dp;
            }
            else {
                mergeDataPointsWorker(dp, dpo);
            }
        }
    }

    /**
     * @param dpToMerge
     * @param dpOriginal
     */
    private void mergeDataPointsWorker(BaseDataPoint<?> dpToMerge, BaseDataPoint<?> dpOriginal) {
        @SuppressWarnings("unchecked")
        ArrayList<BaseDataPoint<?>> histToMergeData = (ArrayList<BaseDataPoint<?>>) dpToMerge.getHistoricalData();
        @SuppressWarnings("unchecked")
        ArrayList<BaseDataPoint<?>> histOrigData = (ArrayList<BaseDataPoint<?>>) dpOriginal.getHistoricalData();
        ArrayList<BaseDataPoint<?>> toAddDP = new ArrayList<>();
        BaseDataPoint<?> toRemove = null;
        for (BaseDataPoint<?> hdp : histToMergeData) {
            boolean foundMatch = false;
            for (BaseDataPoint<?> hodp : histOrigData) {
                if (hdp.getDateUTC().equals(hodp.getDateUTC())) {
                    foundMatch = true;
                    toRemove = hodp;
                    break;
                }
            }
            if (foundMatch) {
//                continue;
                if (!histOrigData.remove(toRemove)) {
                    NeptusLog.pub().warn("Not able to remove from historical data element:" + toRemove.toString());
                    continue;
                }
            }
            toAddDP.add(hdp);
        }
        if (toAddDP.size() > 0)
            histOrigData.addAll(toAddDP);
    }

    private void loadCurrentsFromFiles() {
        // TUV files
        File[] fileList = FileUtil.getFilesFromDisk(baseFolderForCurrentsTUVFiles, currentsFilePatternTUV);
        if (fileList == null)
            return;

        for (File fx : fileList) {
            HashMap<String, HFRadarDataPoint> tdp = processTuvHFRadarTest(fx.getAbsolutePath());
            if (tdp != null && tdp.size() > 0)
                mergeCurrentsDataToInternalDataList(tdp);
        }

        // NetCDF files
        fileList = FileUtil.getFilesFromDisk(baseFolderForCurrentsTUVFiles, currentsFilePatternNetCDF);
        if (fileList == null)
            return;

        for (File fx : fileList) {
            HashMap<String, HFRadarDataPoint> tdp = processNetCDFHFRadarTest(fx.getAbsolutePath());
            if (tdp != null && tdp.size() > 0)
                mergeCurrentsDataToInternalDataList(tdp);
        }

    }

    private void loadMeteoFromFiles() {
        File[] fileList = FileUtil.getFilesFromDisk(baseFolderForMeteoNetCDFFiles, meteoFilePattern);
        if (fileList == null)
            return;

        for (File fx : fileList) {
            try {
                HashMap<?, ?>[] meteodp = processMeteoFile(fx.getAbsolutePath());
                @SuppressWarnings("unchecked")
                HashMap<String, SSTDataPoint> sstdp = (HashMap<String, SSTDataPoint>) meteodp[0];
                if (sstdp != null && sstdp.size() > 0)
                    mergeSSTDataToInternalDataList(sstdp);
                @SuppressWarnings("unchecked")
                HashMap<String, WindDataPoint> winddp = (HashMap<String, WindDataPoint>) meteodp[1];
                if (winddp != null && winddp.size() > 0)
                    mergeWindDataToInternalDataList(winddp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadWavesFromFiles() {
        File[] fileList = FileUtil.getFilesFromDisk(baseFolderForWavesNetCDFFiles, wavesFilePattern);
        if (fileList == null)
            return;

        for (File fx : fileList) {
            HashMap<String, WavesDataPoint> wavesdp = processWavesFile(fx.getAbsolutePath());
            if (wavesdp != null && wavesdp.size() > 0)
                mergeWavesDataToInternalDataList(wavesdp);
        }
    }

    public HashMap<String, HFRadarDataPoint> getNoaaHFRadarData() {
        Date nowDate = new Date(System.currentTimeMillis());
        Date tillDate = new Date(nowDate.getTime() - dateLimitHours * DateTimeUtil.HOUR);
        LocationType lastCenter = offScreen.getLastCenter();
        if (lastCenter == null)
            return null;
        LocationType lc = lastCenter;//.getNewAbsoluteLatLonDepth();
        Dimension boxDim = offScreen.getCurDimentions(null); // dim.getSize();
        LocationType lTop = lc.getNewAbsoluteLatLonDepth();
        lTop.translateInPixel(-boxDim.getWidth() / 2, -boxDim.getHeight() / 2, 22);
        LocationType lBot = lc.getNewAbsoluteLatLonDepth();
        lBot.translateInPixel(boxDim.getWidth() / 2, boxDim.getHeight() / 2, 22);
        
        lTop = new LocationType(noaaMaxLat, noaaMaxLng);
        lBot = new LocationType(noaaMinLat, noaaMinLng);
        return getNoaaHFRadarData(tillDate, nowDate, lTop, lBot);
    }
    
    public HashMap<String, HFRadarDataPoint> getNoaaHFRadarData(Date tillDate, Date nowDate, LocationType lTop, LocationType lBot) {
        if (getHttpRequest != null)
            getHttpRequest.abort();
        getHttpRequest = null;
        try {
            String uri = noaaURL;
            uri = getNoaaURI(tillDate, nowDate, lTop, lBot);
            //uri = uri.replace("hfradar.ndbc.noaa.gov", "whale.fe.up.pt");
            
            System.out.println(uri);
            // http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2013-06-23%2007:56:00&to=2013-06-23%2019:56:00&lat=37.84130100297351&lng=-122.53766785260639&lat2=37.84112572375683&lng2=-122.53734062310657

            
            getHttpRequest = new HttpGet(uri);
            getHttpRequest.setHeader("Referer", "http://hfradar.ndbc.noaa.gov/tab.php");
            @SuppressWarnings("unused")
            long reqTime = System.currentTimeMillis();
            HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequest);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, null);
            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>getRemoteImcData [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (getHttpRequest != null) {
                    getHttpRequest.abort();
                }
                return null;
            }
            InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
            @SuppressWarnings("unused")
            long fullSize = iGetResultCode.getEntity().getContentLength();
            InputStreamReader isr = new InputStreamReader(streamGetResponseBody);
            HashMap<String, HFRadarDataPoint> lst = processNoaaHFRadar(isr);
            return lst;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (getHttpRequest != null) {
                getHttpRequest.abort();
                getHttpRequest = null;
            }
        }
        return null;
    }
    
    /**
     * @param tillDate
     * @param nowDate
     * @param lTop
     * @param lBot
     * @return
     */
    private String getNoaaURI(Date tillDate, Date nowDate, LocationType lTop, LocationType lBot) {
        // from=#FROM_DATE#%20#FROM_TIME#:00&to=#TO_DATE#%20#TO_TIME#:00&lat=#LAT1#&lng=#LNG1#&lat2=#LAT2#&lng2=#LNG2#"
        String uri = noaaURL;
        uri = uri.replace("#FROM_DATE#", dateFormaterUTC.format(tillDate));
        uri = uri.replace("#FROM_TIME#", timeFormaterUTC.format(tillDate));
        uri = uri.replace("#TO_DATE#", dateFormaterUTC.format(nowDate));
        uri = uri.replace("#TO_TIME#", timeFormaterUTC.format(nowDate));
        uri = uri.replace("#LAT1#", "" + lTop.getLatitudeDegs());
        uri = uri.replace("#LNG1#", "" + lTop.getLongitudeDegs());
        uri = uri.replace("#LAT2#", "" + lBot.getLatitudeDegs());
        uri = uri.replace("#LNG2#", "" + lBot.getLongitudeDegs());
        return uri;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D go, StateRenderer2D renderer) {
//        if (!visible)
//            return;
//        paintWorker(go, renderer);
    }
    
    public void paintWorker(Graphics2D go, StateRenderer2D renderer) {
        boolean recreateImage = offScreen.paintPhaseStartTestRecreateImageAndRecreate(go, renderer);
        if (recreateImage) {
            Graphics2D g2 = offScreen.getImageGraphics();

            Date dateColorLimit = new Date(System.currentTimeMillis() - 3 * DateTimeUtil.HOUR);
            Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);
            
            if (showCurrents)
                paintHFRadarInGraphics(renderer, g2, dateColorLimit, dateLimit);
            if (showSST)
                paintSSTInGraphics(renderer, g2, dateColorLimit, dateLimit);
            if (showWind)
                paintWindInGraphics(renderer, g2, dateColorLimit, dateLimit);
            if (showWaves)
                paintWavesInGraphics(renderer, g2, dateColorLimit, dateLimit);

            g2.dispose();
        }            
        offScreen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(go, renderer);
    }

    /**
     * @param renderer
     * @param g2
     * @param dateColorLimit
     * @param dateLimit
     */
    private void paintHFRadarInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        LocationType loc = new LocationType();
        
        Date fromDate = null;
        Date toDate = null;
        
        for (HFRadarDataPoint dp : dataPointsCurrents.values().toArray(new HFRadarDataPoint[0])) {
            if (dp.getDateUTC().before(dateLimit) && !ignoreDateLimitToLoad)
                continue;
            
            double latV = dp.getLat();
            double lonV = dp.getLon();
            double headingV = dp.getHeadingDegrees();
            
            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(headingV))
                continue;
            
            loc.setLatitudeDegs(latV);
            loc.setLongitudeDegs(lonV);
            
            Point2D pt = renderer.getScreenPosition(loc);

            if (!isVisibleInRender(pt, renderer))
                continue;
            
            if (fromDate == null) {
                fromDate = dp.getDateUTC();
            }
            else {
                if (dp.getDateUTC().before(fromDate))
                    fromDate = dp.getDateUTC();
            }
            if (toDate == null) {
                toDate = dp.getDateUTC();
            }
            else {
                if (dp.getDateUTC().after(toDate))
                    toDate = dp.getDateUTC();
            }
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(pt.getX(), pt.getY());
            Color color = Color.WHITE;
            color = colorMapCurrents.getColor(dp.getSpeedCmS() / maxCurrentCmS);
            if (dp.getDateUTC().before(dateColorLimit))
                color = ColorUtils.setTransparencyToColor(color, 128);
            gt.setColor(color);
            double rot = Math.toRadians(-headingV + 90) - renderer.getRotation();
            gt.rotate(rot);
            gt.fill(arrow);
            gt.rotate(-rot);
            
            if (showCurrentsLegend && renderer.getLevelOfDetail() >= showCurrentsLegendFromZoomLevel) {
                gt.setFont(font8Pt);
                gt.setColor(Color.WHITE);
                gt.drawString(MathMiscUtils.round(dp.getSpeedCmS(), 1) + "cm/s", 10, 2);
            }
            
            gt.dispose();
        }
        
        
        if (showDataDebugLegend) {
            String txtMsg = "Currents data from '" + fromDate + "' till '" + toDate + "'";
            Graphics2D gt = (Graphics2D) g2.create();
            gt.setFont(font8Pt);
            gt.setColor(Color.WHITE);
            gt.drawString(txtMsg, 10, 52);
            gt.dispose();
        }
    }

    /**
     * @param renderer
     * @param g2
     * @param dateColorLimit
     * @param dateLimit
     */
    private void paintSSTInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        try {
            List<SSTDataPoint> dest = new ArrayList<>(dataPointsSST.values());
            long stNanos = System.currentTimeMillis();
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            Map<Point2D, Pair<Double, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Pair<Double, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double sstV = dp.getSst();
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(sstV))
                                return;
                            
                            Date dateV = new Date(dp.getDateUTC().getTime());

                            LocationType loc = new LocationType();
                            loc.setLatitudeDegs(latV);
                            loc.setLongitudeDegs(lonV);
                            
                            Point2D pt = renderer.getScreenPosition(loc);
                            
                            if (!isVisibleInRender(pt, renderer))
                                return;
                            
                            visiblePts.accumulate(1);
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % SST_RADIUS;
                            y = y - y % SST_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, new Pair<>(sstV, dateV));
                            }
                            else {
                                Pair<Double, Date> pval = res.get(pt);
                                double val = pval.first();
                                val = (val + sstV) / 2d;
                                if (dateV.after(pval.second()))
                                    res.put(pt, new Pair<>(val, dateV));
                                else
                                    res.put(pt, new Pair<>(val, pval.second()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Pair<Double, Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Pair<Double, Date> s = res.get(k1);
                                    double val = (s.first() + sI.first()) / 2d;
                                    Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                            : s.second();
                                    res.put(k1, new Pair<Double, Date>(val, valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            System.out.println(String.format("stg 1 took %ss :: %d of %d from %d (%f%%)",
                    DateTimeUtil.formatTime(System.currentTimeMillis() - stNanos), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stNanos = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<Double,Date> pVal = ptFilt.get(pt);
                    
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());
                    Color color = Color.WHITE;
                    color = colorMapSST.getColor((pVal.first() - minSST) / (maxSST - minSST));
                    if (pVal.second().before(dateColorLimit)) //if (dp.getDateUTC().before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    gt.draw(circle);
                    gt.fill(circle);
                    
                    if (showSSTLegend && renderer.getLevelOfDetail() >= showSSTLegendFromZoomLevel) {
                        gt.setFont(font8Pt);
                        gt.setColor(Color.WHITE);
                        gt.drawString(MathMiscUtils.round(pVal.first(), 1) + "\u00B0C", -15, 15);
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().trace(e);
                }
                
                if (gt != null)
                    gt.dispose();
            });
            System.out.println(String.format("stg 2 took %ss",
                    DateTimeUtil.formatTime(System.currentTimeMillis() - stNanos)));
            System.out.println(ptFilt.size() + " of " + dest.size());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void paintWindInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        try {
            List<WindDataPoint> dest = new ArrayList<>(dataPointsWind.values());
            long stNanos = System.currentTimeMillis();
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            Map<Point2D, Triple<Double, Double, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Triple<Double, Double, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double speedV = dp.getSpeed();
                            double headingV = AngleUtils.nomalizeAngleDegrees360(dp.getHeading());
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(speedV)|| Double.isNaN(headingV))
                                return;
                            
                            Date dateV = new Date(dp.getDateUTC().getTime());

                            LocationType loc = new LocationType();
                            loc.setLatitudeDegs(latV);
                            loc.setLongitudeDegs(lonV);
                            
                            Point2D pt = renderer.getScreenPosition(loc);
                            
                            if (!isVisibleInRender(pt, renderer))
                                return;
                            
                            visiblePts.accumulate(1);
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % WIND_RADIUS;
                            y = y - y % WIND_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, Triple.of(speedV, headingV, dateV));
                            }
                            else {
                                Triple<Double, Double, Date> pval = res.get(pt);
                                double val = pval.getLeft();
                                val = (val + speedV) / 2d;
                                double val1 = pval.getMiddle();
                                val1 = (val1 + headingV) / 2d;
                                if (dateV.after(pval.getRight()))
                                    res.put(pt, Triple.of(val, val1, dateV));
                                else
                                    res.put(pt, Triple.of(val, val1, pval.getRight()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Triple<Double, Double, Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Triple<Double, Double, Date> s = res.get(k1);
                                    double val = (s.getLeft() + sI.getLeft()) / 2d;
                                    double val1 = (s.getMiddle() + sI.getMiddle()) / 2d;
                                    Date valDate = sI.getRight().after(s.getRight()) ? new Date(sI.getRight().getTime())
                                            : s.getRight();
                                    res.put(k1, Triple.of(val, val1, valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            System.out.println(String.format("stg 1 took %ss :: %d of %d from %d (%f%%)",
                    DateTimeUtil.formatTime(System.currentTimeMillis() - stNanos), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stNanos = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Triple<Double, Double, Date> pVal = ptFilt.get(pt);
                    double speedV = pVal.getLeft();
                    double headingV = AngleUtils.nomalizeAngleDegrees360(pVal.getMiddle());
                    Date dateV = pVal.getRight();
                    
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());

                    Color color = Color.BLACK;
                    if (useColorMapForWind)
                        color = colorMapWind.getColor(speedV / maxWind);
                    if (dateV.before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    
                    gt.rotate(Math.toRadians(headingV) - renderer.getRotation());
                    
                    double speedKnots = speedV * m_sToKnotConv;
                    if (speedKnots >= 2) {
                        gt.draw(windPoleKnots);
                    }
                    
                    if (speedKnots >= 5 && speedKnots < 10) {
                        gt.draw(wind5Knots2);
                    }
                    else if (speedKnots >= 10 && speedKnots < 15) {
                        gt.draw(wind10Knots1);
                    }
                    else if (speedKnots >= 15 && speedKnots < 20) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind5Knots2);
                    }
                    else if (speedKnots >= 20 && speedKnots < 25) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind10Knots2);
                    }
                    else if (speedKnots >= 25 && speedKnots < 30) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind10Knots2);
                        gt.draw(wind5Knots3);
                    }
                    else if (speedKnots >= 30 && speedKnots < 35) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind10Knots2);
                        gt.draw(wind10Knots3);
                    }
                    else if (speedKnots >= 35 && speedKnots < 40) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind10Knots2);
                        gt.draw(wind10Knots3);
                        gt.draw(wind5Knots4);
                    }
                    else if (speedKnots >= 40 && speedKnots < 45) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind10Knots2);
                        gt.draw(wind10Knots3);
                        gt.draw(wind10Knots4);
                    }
                    else if (speedKnots >= 45 && speedKnots < 50) {
                        gt.draw(wind10Knots1);
                        gt.draw(wind10Knots2);
                        gt.draw(wind10Knots3);
                        gt.draw(wind10Knots4);
                        gt.draw(wind5Knots5);
                    }
                    else if (speedKnots >= 50) {
                        gt.draw(wind50Knots1);
                        gt.fill(wind50Knots1);
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().trace(e);
                }
                
                if (gt != null)
                    gt.dispose();
            });
            System.out.println(String.format("stg 2 took %ss",
                    DateTimeUtil.formatTime(System.currentTimeMillis() - stNanos)));
            System.out.println(ptFilt.size() + " of " + dest.size());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param renderer
     * @param g2
     * @param dateColorLimit
     * @param dateLimit
     */
    private void paintWavesInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        try {
            List<WavesDataPoint> dest = new ArrayList<>(dataPointsWaves.values());
            long stNanos = System.currentTimeMillis();
            LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
            Map<Point2D, Pair<Triple<Double, Double, Double>, Date>> ptFilt = dest.parallelStream()
                    .collect(HashMap<Point2D, Pair<Triple<Double, Double, Double>, Date>>::new, (res, dp) -> {
                        try {
                            if (!ignoreDateLimitToLoad && dp.getDateUTC().before(dateLimit))
                                return;
                            
                            double latV = dp.getLat();
                            double lonV = dp.getLon();
                            double sigHeightV = dp.getSignificantHeight();
                            double headingV = AngleUtils.nomalizeAngleDegrees360(dp.getPeakDirection());
                            double periodV = dp.getPeakPeriod();
                            
                            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(sigHeightV) || Double.isNaN(headingV)
                                    || Double.isNaN(periodV))
                                return;
                            
                            Date dateV = new Date(dp.getDateUTC().getTime());

                            LocationType loc = new LocationType();
                            loc.setLatitudeDegs(latV);
                            loc.setLongitudeDegs(lonV);
                            
                            Point2D pt = renderer.getScreenPosition(loc);
                            
                            if (!isVisibleInRender(pt, renderer))
                                return;
                            
                            visiblePts.accumulate(1);
                            
                            double x = pt.getX();
                            double y = pt.getY();
                            x = x - x % ARROW_RADIUS;
                            y = y - y % ARROW_RADIUS;
                            pt.setLocation(x, y);
                            
                            if (!res.containsKey(pt)) {
                                res.put(pt, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(sigHeightV, headingV, periodV), dateV));
                            }
                            else {
                                Pair<Triple<Double, Double, Double>, Date> pval = res.get(pt);
                                double val = pval.first().getLeft();
                                val = (val + sigHeightV) / 2d;
                                double val1 = pval.first().getMiddle();
                                val1 = (val1 + headingV) / 2d;
                                double val2 = pval.first().getRight();
                                val2 = (val2 + periodV) / 2d;
                                if (dateV.after(pval.second()))
                                    res.put(pt, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(val, val1, val2), dateV));
                                else
                                    res.put(pt, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(val, val1, val2), pval.second()));
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    }, (res, resInt) -> {
                        resInt.keySet().stream().forEach(k1 -> {
                            try {
                                Pair<Triple<Double,Double,Double>,Date> sI = resInt.get(k1);
                                if (res.containsKey(k1)) {
                                    Pair<Triple<Double, Double, Double>, Date> s = res.get(k1);
                                    double val = (s.first().getLeft() + sI.first().getLeft()) / 2d;
                                    double val1 = (s.first().getMiddle() + sI.first().getMiddle()) / 2d;
                                    double val2 = (s.first().getRight() + sI.first().getRight()) / 2d;
                                    Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                            : s.second();
                                    res.put(k1, new Pair<Triple<Double, Double, Double>, Date>(Triple.of(val, val1, val2), valDate));
                                }
                                else {
                                    res.put(k1, sI);
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(e);
                            }
                        });
                    });
            
            System.out.println(String.format("stg 1 took %ss :: %d of %d from %d (%f%%)",
                    DateTimeUtil.formatTime(System.currentTimeMillis() - stNanos), ptFilt.size(), visiblePts.longValue(), dest.size(),
                    (ptFilt.size() * 1. / visiblePts.longValue()) * 100));
            stNanos = System.currentTimeMillis();
            
            ptFilt.keySet().parallelStream().forEach(pt -> {
                Graphics2D gt = null;
                try {
                    Pair<Triple<Double, Double, Double>, Date> pVal = ptFilt.get(pt);
                    double sigHeightV = pVal.first().getLeft();
                    double headingV = AngleUtils.nomalizeAngleDegrees360(pVal.first().getMiddle());
                    @SuppressWarnings("unused")
                    double periodV = pVal.first().getRight();
                    Date dateV = pVal.second();
                    
                    gt = (Graphics2D) g2.create();
                    gt.translate(pt.getX(), pt.getY());

                    Color color = Color.WHITE;
                    color = colorMapWaves.getColor(sigHeightV / maxWaves);
                    if (dateV.before(dateColorLimit))
                        color = ColorUtils.setTransparencyToColor(color, 128);
                    gt.setColor(color);
                    double rot = Math.toRadians(headingV) - renderer.getRotation();
                    gt.rotate(rot);
                    gt.fill(arrow);
                    gt.rotate(-rot);
                    
                    if (showWavesLegend && renderer.getLevelOfDetail() >= showWavesLegendFromZoomLevel) {
                        gt.setFont(font8Pt);
                        gt.setColor(Color.WHITE);
                        gt.drawString(MathMiscUtils.round(sigHeightV, 1) + "m", 10, -8);
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().trace(e);
                }
                
                if (gt != null)
                    gt.dispose();
            });
            System.out.println(String.format("stg 2 took %ss",
                    DateTimeUtil.formatTime(System.currentTimeMillis() - stNanos)));
            System.out.println(ptFilt.size() + " of " + dest.size());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param sPos
     * @param renderer
     * @return
     */
    private boolean isVisibleInRender(Point2D sPos, StateRenderer2D renderer) {
        Dimension rendDim = renderer.getSize();
        if (sPos.getX() < 0 - offScreen.getOffScreenBufferPixel()
                || sPos.getY() < 0 - offScreen.getOffScreenBufferPixel())
            return false;
        else if (sPos.getX() > rendDim.getWidth() + offScreen.getOffScreenBufferPixel()
                || sPos.getY() > rendDim.getHeight() + offScreen.getOffScreenBufferPixel())
            return false;

        return true;
    }

    // private HashMap<String, HFRadarDataPoint> processNoaaHFRadarTest() {
    // // InputStreamReader
    // String fxName = FileUtil.getResourceAsFileKeepName(sampleNoaaFile);
    // File fx = new File(fxName);
    // try {
    // FileReader freader = new FileReader(fx);
    // return processNoaaHFRadar(freader);
    // }
    // catch (FileNotFoundException e) {
    // e.printStackTrace();
    // }
    // return new HashMap<String, HFRadarDataPoint>();
    // }

    private FileReader getFileReaderForFile(String fileName) {
        // InputStreamReader
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        File fx = new File(fxName);
        try {
            FileReader freader = new FileReader(fx);
            return freader;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HashMap<String, HFRadarDataPoint> processTuvHFRadarTest(String fileName) {
        // InputStreamReader
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        FileReader freader = getFileReaderForFile(fileName);
        if (freader == null)
            return hfdp;
        
        HashMap<String, HFRadarDataPoint> ret = LoaderHelper.processTUGHFRadar(freader, ignoreDateLimitToLoad ? null : createDateLimitToRemove());
//        NeptusLog.pub().info("*** SUCCESS reading file " + fileName);
        return ret;
    }

    private HashMap<String, HFRadarDataPoint> processNetCDFHFRadarTest(String fileName) {
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        if (!new File(fxName).exists())
            return new HashMap<>();
        HashMap<String, HFRadarDataPoint> ret = LoaderHelper.processNetCDFHFRadar(fxName, ignoreDateLimitToLoad ? null : createDateLimitToRemove());
//        NeptusLog.pub().info("*** SUCCESS reading file " + fileName);
        return ret;
    }

    private HashMap<?,?>[] processMeteoFile(String fileName) {
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        if (!new File(fxName).exists())
            return new HashMap[] { new HashMap<>(), new HashMap<>() };
        return LoaderHelper.processMeteo(fxName, ignoreDateLimitToLoad ? null : createDateLimitToRemove());
    }

    private HashMap<String, WavesDataPoint> processWavesFile(String fileName) {
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        if (!new File(fxName).exists())
            return new HashMap<>();
        return LoaderHelper.processWavesFile(fxName, ignoreDateLimitToLoad ? null : createDateLimitToRemove());
    }

    private HashMap<String, HFRadarDataPoint> processNoaaHFRadar(Reader readerInput) {
        long deltaTimeToHFRadarHistoricalData = dateLimitHours * DateTimeUtil.HOUR;
        
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        BufferedReader reader = null;
        Date dateLimite = new Date(System.currentTimeMillis() - deltaTimeToHFRadarHistoricalData);
        try {
            reader = new BufferedReader(readerInput);
            String line = reader.readLine();
            for (/* int i = 0 */; line != null; /* i++ */) {
                if (line.startsWith("#")) {
                    line = reader.readLine();
                    continue;
                }

                String[] tokens = line.split("[\t ,]");
                try {
                    String dateStr = tokens[4];
                    String timeStr = tokens[5];
                    Date date = dateTimeFormaterUTC.parse(dateStr + " " + timeStr);
                    if (date.before(dateLimite) && !ignoreDateLimitToLoad) {
                        line = reader.readLine();
                        continue;
                    }
                    
                    double lat = Double.parseDouble(tokens[0]);
                    double lon = Double.parseDouble(tokens[1]);
                    HFRadarDataPoint dp = new HFRadarDataPoint(lat, lon);
                    dp.setSpeedCmS(Double.parseDouble(tokens[2]));
                    dp.setHeadingDegrees(Double.parseDouble(tokens[3]));
                    dp.setDateUTC(date);
                    if (tokens.length >= 7) {
                        dp.setResolutionKm(Double.parseDouble(tokens[6]));
                        for (int j = 7; j < tokens.length; j++) {
                            dp.setInfo(dp.getInfo() + tokens[j]);
                        }
                    }

                    HFRadarDataPoint dpo = hfdp.get(HFRadarDataPoint.getId(dp));
                    if (dpo == null) {
                        dpo = dp.getACopyWithoutHistory();
                        hfdp.put(HFRadarDataPoint.getId(dp), dp);
                    }

                    ArrayList<HFRadarDataPoint> lst = dpo.getHistoricalData();
                    boolean alreadyIn = false;
                    for (HFRadarDataPoint tmpDp : lst) {
                        if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                            alreadyIn = true;
                            break;
                        }
                    }
                    if (!alreadyIn) {
                        dpo.getHistoricalData().add(dp);
                    }
                    
//                    System.out.println(dp);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                line = reader.readLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        Date nowDate = createDateToMostRecent();
        for (HFRadarDataPoint elm : hfdp.values()) {
            updateHFRadarToUseMostRecentOrMean(nowDate, elm);
        }
        
        return hfdp;
    }
    
    @SuppressWarnings("unused")
    public static void main1(String[] args) {
        //InputStream is = HFRadarVisualization.class.getResourceAsStream(sampleNoaaFile);
        String fxName = FileUtil.getResourceAsFileKeepName(sampleNoaaFile);
        File fx = new File(fxName);
        BufferedReader reader = null;
        try {
            FileReader freader = new FileReader(fx);
            reader = new BufferedReader(freader);
            String line = reader.readLine();
            for (int i = 0; line != null; i++) {
//                System.out.println(line);
                String[] tokens = line.split("[\t ,]");
                
                try {
                    double lat = Double.parseDouble(tokens[0]);
                    double lon = Double.parseDouble(tokens[1]);
                    HFRadarDataPoint dp = new HFRadarDataPoint(lat, lon);
                    dp.setSpeedCmS(Double.parseDouble(tokens[2]));
                    dp.setHeadingDegrees(Double.parseDouble(tokens[3]));
                    String dateStr = tokens[4];
                    String timeStr = tokens[5];
                    dp.setDateUTC(dateTimeFormaterUTC.parse(dateStr + " " + timeStr));
                    dp.setResolutionKm(Double.parseDouble(tokens[6]));
                    for (int j = 7; j < tokens.length; j++) {
                        dp.setInfo(dp.getInfo() + tokens[j]);
                    }
                    
                    System.out.println(dp);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                

                line = reader.readLine();
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        Map<Double, String> st = new HashMap<>();
        st.put(1.0, "a");
        st.put(2.1, "b");
        st.put(1.3, "c");
        st.put(1.6, "d");
        st.put(1.9, "e");
        st.put(0.9, "f");
        st.put(0.4, "g");
        st.put(3.9, "h");
        st.put(3.5, "i");
        Map<Double, String> result = st.keySet().parallelStream().collect(HashMap<Double, String>::new, (r, v) -> {
            double v1 = Math.round(v);
            String s = st.get(v);
            for (Double d : r.keySet()) {
                if (d == v1) {
                    String str = r.get(v1);
                    System.out.println("1 size=" + r.size() + "  v=" +  v + "  s=" + st.get(v) + "  resI=" + r);
                    r.put(v1, str + s);
                    return;
                }
            }
            System.out.println("2 size=" + r.size() + "  v=" +  v + "  s=" + st.get(v) + "  resI=" + r);
            r.put(v1, s);
        }, (res, resInt) -> {
            System.out.println("3 size=" + res.size() + "  res=" +  res + "  resI=" + resInt);
            resInt.keySet().stream().forEach(k1 -> {
                String sI = resInt.get(k1);
                if (res.containsKey(k1)) {
                    String s = res.get(k1);
                    res.put(k1, s + sI);
                }
                else {
                    res.put(k1, sI);
                }
            });
        });
        System.out.println(result);
        
        System.out.println(59 - 59 %4);
        
        if (true)
            return;
        
        String currentsFilePattern = "TOTL_TRAD_\\d{4}_\\d{2}_\\d{2}_\\d{4}\\.tuv";
        // String meteoFilePattern = "meteo_\\d{8}\\.nc";
        // String wavesFilePattern = "waves_[a-zA-Z]{1,2}_\\d{8}\\.nc";

        Pattern pat = Pattern.compile(currentsFilePattern);
        Matcher m = pat.matcher("TOTL_TRAD_2013_07_11_0800.tuv");
        System.out.println(m.find());
        
        File[] fileList = FileUtil.getFilesFromDisk(new File("IHData/CODAR"), currentsFilePatternNetCDF);
        System.out.println(Arrays.toString(fileList));
        
        HashMap<String, HFRadarDataPoint> ret = LoaderHelper.processNetCDFHFRadar("IHData/CODAR/mola_his_z-20140512.nc", null);
        System.out.println("First :: " + ret.values().iterator().next());
        HFRadarDataPoint lastdp = null;
        for (HFRadarDataPoint dp : ret.values().iterator().next().getHistoricalData()) {
            lastdp = dp;
        }
        System.out.println("Last  :: " + lastdp);
        
        // Test update to the most recent
        HFRadarDataPoint dp = ret.values().iterator().next();
        System.out.println("" + dp);
        Date newDate = new Date(dp.dateUTC.getTime() + DateTimeUtil.HOUR * 3 + DateTimeUtil.MINUTE * 30);
        System.out.println(newDate);
        dp.useMostRecent(newDate);
        System.out.println("" + dp);
    }
}
