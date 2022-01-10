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
 * Author: pdias
 * Jun 22, 2013
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.Font;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBarPainterUtil;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.envdisp.datapoints.BaseDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.ChlorophyllDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.HFRadarDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.SSTDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.WavesDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.WindDataPoint;
import pt.lsts.neptus.plugins.envdisp.painter.EnvDataPaintHelper;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.UnitsUtil;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Environmental Data Visualization", author="Paulo Dias", version="2.0", icon="pt/lsts/neptus/plugins/envdisp/hf-radar.png")
@LayerPriority(priority = -50)
public class EnvironmentalDataVisualization extends ConsolePanel implements Renderer2DPainter, IPeriodicUpdates, ConfigurationListener {

    private static final String CATEGORY_TEST = "Test";
    private static final String CATEGORY_DATA_UPDATE = "Data Update";
    private static final String CATEGORY_VISIBILITY_CURRENTS = "Visibility Currents";
    private static final String CATEGORY_VISIBILITY_SST = "Visibility SST";
    private static final String CATEGORY_VISIBILITY_WIND = "Visibility Wind";
    private static final String CATEGORY_VISIBILITY_WAVES = "Visibility Waves";
    private static final String CATEGORY_VISIBILITY_CHLOROPHILL = "Visibility Chlorophyll";

    /*
     * Currents, wind, waves, SST 
     */

    @NeptusProperty(name = "Show currents", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CURRENTS)
    public boolean showCurrents = true;
    @NeptusProperty(name = "Show SST", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST)
    public boolean showSST = true;
    @NeptusProperty(name = "Show wind", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WIND)
    public boolean showWind = true;
    @NeptusProperty(name = "Show waves", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WAVES)
    public boolean showWaves = false;
    @NeptusProperty(name = "Show chlorophyll", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL)
    public boolean showChlorophyll = false;

    @NeptusProperty(name = "Show currents legend", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CURRENTS)
    public boolean showCurrentsLegend = true;
    @NeptusProperty(name = "Show currents legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CURRENTS)
    public int showCurrentsLegendFromZoomLevel = 13;
    @NeptusProperty(name = "Show SST legend", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST)
    public boolean showSSTLegend = true;
    @NeptusProperty(name = "Show SST legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST)
    public int showSSTLegendFromZoomLevel = 11;
    @NeptusProperty(name = "Show waves legend", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WAVES)
    public boolean showWavesLegend = true;
    @NeptusProperty(name = "Show waves legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WAVES)
    public int showWavesLegendFromZoomLevel = 13;
    @NeptusProperty(name = "Show currents as most recent (true) or mean (false) value", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public boolean hfradarUseMostRecentOrMean = true;
    @NeptusProperty(name = "Use color map for wind", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WIND)
    public boolean useColorMapForWind = true;
    @NeptusProperty(name = "Show chlorophyll legend", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL)
    public boolean showChlorophyllLegend = true;
    @NeptusProperty(name = "Show chlorophyll legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL)
    public int showChlorophyllLegendFromZoomLevel = 13;

    @NeptusProperty(name = "Show currents colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CURRENTS,
            description = "Show the color scale bar. Only one will show.")
    public boolean showCurrentsColorbar = false;
    @NeptusProperty(name = "Show SST colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST,
            description = "Show the color scale bar. Only one will show.")
    public boolean showSSTColorbar = false;
    @NeptusProperty(name = "Show wind colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WIND,
            description = "Show the color scale bar. Only one will show.")
    public boolean showWindColorbar = false;
    @NeptusProperty(name = "Show waves colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WAVES,
            description = "Show the color scale bar. Only one will show.")
    public boolean showWavesColorbar = false;
    @NeptusProperty(name = "Show chlorophyll colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL,
            description = "Show the color scale bar. Only one will show.")
    public boolean showChlorophyllColorbar = false;

    @NeptusProperty(name = "Colormap for currents", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CURRENTS)
    private ColorMap colorMapCurrents = ColorMapFactory.createJetColorMap();
    @NeptusProperty(name = "Colormap for SST", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST)
    private ColorMap colorMapSST = ColorMapFactory.createJetColorMap();
    @NeptusProperty(name = "Colormap for wind", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WIND)
    private ColorMap colorMapWind = colorMapCurrents;
    @NeptusProperty(name = "Colormap for waves", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WAVES)
    private ColorMap colorMapWaves = ColorMapFactory.createJetColorMap();
    @NeptusProperty(name = "Colormap for chlorophyll", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL)
    private ColorMap colorMapChlorophyll = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Minutes between updates", category = CATEGORY_DATA_UPDATE)
    public int updateFileDataMinutes = 5;
    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public int dateLimitHours = 12;
    @NeptusProperty(name = "Use data x hour in the future (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public int dateHoursToUseForData = 1;
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.ADVANCED, category = CATEGORY_DATA_UPDATE)
    public boolean ignoreDateLimitToLoad = false;

    @NeptusProperty(name = "Base Folder For Currents TUV or netCDF Files", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, 
            description = "The folder to look for currents data. Admissible files '*.tuv' and '*.nc'. NetCDF variables used: lat, lon, time, u, v.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForCurrentsTUVFiles = new File("IHData/CODAR");
    @NeptusProperty(name = "Base Folder For Meteo netCDF Files", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, 
            description = "The folder to look for meteo data (wind and SST). Admissible files '*.nc'. NetCDF variables used: lat, lon, time, u, v, sst.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForMeteoNetCDFFiles = new File("IHData/METEO");
    @NeptusProperty(name = "Base Folder For Waves netCDF Files", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, 
            description = "The folder to look for waves (significant height, peak period and direction) data. Admissible files '*.nc'. NetCDF variables used: lat, lon, time, hs, tp, pdir.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForWavesNetCDFFiles = new File("IHData/WAVES");
    @NeptusProperty(name = "Base Folder For Chlorophyll netCDF Files", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, 
            description = "The folder to look for chlorophyll data. Admissible files '*.nc'. NetCDF variables used: lat, lon, time, chlorophyll.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForChlorophyllNetCDFFiles = new File("IHData/CHLOROPHYL");
    @NeptusProperty(name = "Request HF_Radar data from NOOA", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public boolean requestFromNooaWeb = false;
    
    @NeptusProperty(name = "Show visible data date-time interval", userLevel = LEVEL.ADVANCED, category = CATEGORY_TEST, 
            description = "Draws the string with visible curents data date-time interval.")
    public boolean showDataDebugLegend = false;
    @NeptusProperty(name = "Load data from file (hfradar.txt)", editable = false, userLevel = LEVEL.ADVANCED, category=CATEGORY_TEST, description = "Don't use this (testing purposes).")
    public boolean loadFromFile = false;
    
    // private final double minCurrentCmS = 0;
    @NeptusProperty(name = "Currents max cm/s", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CURRENTS)
    private double maxCurrentCmS = 200;

    @NeptusProperty(name = "SST min \u00B0C", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST)
    private double minSST = -10;
    @NeptusProperty(name = "SST max \u00B0C", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_SST)
    private double maxSST = 40;

    // private final double minWind = 0;
    @NeptusProperty(name = "Wind max m/s", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WIND)
    private double maxWind = 65 / UnitsUtil.MS_TO_KNOT;

    // private final double minWaves = 0;
    @NeptusProperty(name = "Waves max m", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_WAVES)
    private double maxWaves = 7;

    @NeptusProperty(name = "Chlorophyll min mg/m\u00B3", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL)
    private double minChlorophyll = 0.01; //mg/m3 log
    @NeptusProperty(name = "Chlorophyll max mg/m\u00B3", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_CHLOROPHILL)
    private double maxChlorophyll = 60; //mg/m3

    private static final String tuvFilePattern = ".\\.tuv$";
    private static final String netCDFFilePattern = ".\\.nc(\\.gz)?$";
    private static final String currentsFilePatternTUV = tuvFilePattern; // "^TOTL_TRAD_\\d{4}_\\d{2}_\\d{2}_\\d{4}\\.tuv$";
    private static final String currentsFilePatternNetCDF = netCDFFilePattern; // "^CODAR_TRAD_\\d{4}_\\d{2}_\\d{2}_\\d{4}\\.nc$";
    private static final String meteoFilePattern = netCDFFilePattern; // "^meteo_\\d{8}\\.nc$";
    private static final String wavesFilePattern = netCDFFilePattern; // "^waves_[a-zA-Z]{1,2}_\\d{8}\\.nc$";
    private static final String chlorophyllFilePattern = netCDFFilePattern;

    private final Font font8Pt = new Font("Helvetica", Font.PLAIN, 9);

    static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    static final SimpleDateFormat dateTimeFormaterSpacesUTC = new SimpleDateFormat("yyyy MM dd  HH mm ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    private static final SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    private static final SimpleDateFormat timeFormaterUTC = new SimpleDateFormat("HH':'mm") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    private int updateSeconds = 30;
    private long lastMillisFileDataUpdated = System.currentTimeMillis() + 60000; // To defer the first run on start

    //  http://hfradar.ndbc.noaa.gov/tab.php?from=2013-06-22%2015:00:00&to=2013-06-22%2015:00:00&p=1&lat=38.324420427006515&lng=-119.94323730468749&lat2=35.69299463209881&lng2=-124.33776855468749
    //  http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2013-06-23%2009:34:00&to=2013-06-23%2021:34:00&lat=37.78799270017669&lng=-122.39269445535145&lat2=37.78781729434937&lng2=-122.39236722585163
    private final String noaaURL = "http://hfradar.ndbc.noaa.gov/tabdownload.php?" +
    		"from=#FROM_DATE#%20#FROM_TIME#:00&to=#TO_DATE#%20#TO_TIME#:00&lat=#LAT1#&lng=#LNG1#&lat2=#LAT2#&lng2=#LNG2#&uom=cms&fmt=tsv";
    // lat lon speed (cm/s)    degree  acquired    resolution (km) origin
    @SuppressWarnings("unused")
    private final double noaaMaxLat = 55.47885346331034, noaaMaxLng = -61.87500000000001, noaaMinLat = 14.093957177836236, noaaMinLng = -132.1875;

    private static final String sampleNoaaFile = "hfradar-noaa-sample1.txt";
    // private static final String sampleTuvFile = "TOTL_TRAD_2013_07_04_1100.tuv";
    // private static final String sampleMeteoFile = "meteo_20130705.nc";
    // private static final String sampleWavesFile = "waves_S_20130704.nc";
    
    private OffScreenLayerImageControl offScreen = new OffScreenLayerImageControl();
    
    private final HttpClientConnectionHelper httpComm = new HttpClientConnectionHelper();
    private HttpGet getHttpRequest;
    
    // ID is lat/lon
    private final HashMap<String, HFRadarDataPoint> dataPointsCurrents = new HashMap<>();
    private final HashMap<String, SSTDataPoint> dataPointsSST = new HashMap<>();
    private final HashMap<String, WindDataPoint> dataPointsWind = new HashMap<>();
    private final HashMap<String, WavesDataPoint> dataPointsWaves = new HashMap<>();
    private final HashMap<String, ChlorophyllDataPoint> dataPointsChlorophyll = new HashMap<>();

    private Thread painterThread = null;
    private AtomicBoolean abortIndicator = null;

    @PluginDescription(name="Environmental Data Visualization Layer", icon="pt/lsts/neptus/plugins/envdisp/hf-radar.png")
    @LayerPriority(priority = -300)
    private class EnvironmentalDataConsoleLayer extends ConsoleLayer {
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
    
    private final EnvironmentalDataConsoleLayer consoleLayer = new EnvironmentalDataConsoleLayer();

    public EnvironmentalDataVisualization(ConsoleLayout console) {
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
    
    public String validateUpdateFileDataMinutes(int value) {
        if (value < 1 || value > 10)
            return "Keep it between 1 and 10";
        return null;
    }

    public String validateDateLimitHours(int value) {
        if (value < 3 || value > 24 * 5)
            return "Keep it between 3 and 24*5=120";
        return null;
    }

    public String validateDateHoursToUseForData(int value) {
        if (value < 0)
            return "Keep it above 0";
        return null;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return updateSeconds * 1000L;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public synchronized boolean update() {
        if (lastMillisFileDataUpdated <= 0
                || System.currentTimeMillis() - lastMillisFileDataUpdated >= updateFileDataMinutes * 60 * 1000) {
            
            NeptusLog.pub().info("Update Data");

            lastMillisFileDataUpdated = System.currentTimeMillis();
            
            try {
                if (requestFromNooaWeb) {
                    HashMap<String, HFRadarDataPoint> dpLts = getNoaaHFRadarData();
                    if (dpLts != null && dpLts.size() > 0) {
                        mergeDataToInternalDataList(dataPointsCurrents, dpLts);
                        System.out.println(dpLts.size() + " ------------------------------");
                    }
                }

                loadCurrentsFromFiles();
                loadMeteoFromFiles();
                loadWavesFromFiles();
                loadChlorophyllFromFiles();
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
            }

            
            try {
                cleanUpData();
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
            }
        }

        return true;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (updateFileDataMinutes < 1)
            updateFileDataMinutes = 1;
        if (updateFileDataMinutes > 10)
            updateFileDataMinutes = 10;
        
        if (dateLimitHours < 3)
            dateLimitHours = 3;
        if (dateLimitHours > 24 * 5)
            dateLimitHours = 24 * 5;

        if (dateHoursToUseForData < 0)
            dateHoursToUseForData = 0;

        if (maxCurrentCmS <= 0)
            maxCurrentCmS = 1;

        if (minSST >= maxSST)
            minSST = maxSST - 10;

        if (maxWind <= 0)
            maxWind = 1;

        if (maxWaves <= 0)
            maxWaves = 1;

        if (minChlorophyll >= maxChlorophyll)
            minChlorophyll = maxChlorophyll - 10;

        lastMillisFileDataUpdated = -1;
        cleanUpData();
    }

    private void cleanUpData() {
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
        
        for (String dpID : dataPointsChlorophyll.keySet().toArray(new String[0])) {
            ChlorophyllDataPoint dp = dataPointsChlorophyll.get(dpID);
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
        
        BaseDataPoint.cleanDataPointsBeforeDate(dataPointsCurrents, dateLimit);
        BaseDataPoint.cleanDataPointsBeforeDate(dataPointsSST, dateLimit);
        BaseDataPoint.cleanDataPointsBeforeDate(dataPointsWind, dateLimit);
        BaseDataPoint.cleanDataPointsBeforeDate(dataPointsWaves, dateLimit);
        BaseDataPoint.cleanDataPointsBeforeDate(dataPointsChlorophyll, dateLimit);
    }

    static <Bp extends BaseDataPoint<?>> void mergeDataToInternalDataList(HashMap<String, Bp> originalData,
            HashMap<String, Bp> toMergeData) {
        for (String dpId : toMergeData.keySet()) {
            Bp dp = toMergeData.get(dpId);
            Bp dpo = originalData.get(dpId);
            if (dpo == null) {
                originalData.put(dpId, dp);
                dpo = dp;
            }
            else {
                mergeDataPointsWorker(dp, dpo);
            }
        }
        System.out.println(toMergeData.size() + " vs " + originalData.size());
    }

    /**
     * @param dpToMerge
     * @param dpOriginal
     */
    static void mergeDataPointsWorker(BaseDataPoint<?> dpToMerge, BaseDataPoint<?> dpOriginal) {
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
                mergeDataToInternalDataList(dataPointsCurrents, tdp);
        }

        // NetCDF files
        fileList = FileUtil.getFilesFromDisk(baseFolderForCurrentsTUVFiles, currentsFilePatternNetCDF);
        if (fileList == null)
            return;

        for (File fx : fileList) {
            HashMap<String, HFRadarDataPoint> tdp = processNetCDFHFRadarTest(fx.getAbsolutePath());
            if (tdp != null && tdp.size() > 0)
                mergeDataToInternalDataList(dataPointsCurrents, tdp);
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
                    mergeDataToInternalDataList(dataPointsSST, sstdp);
                @SuppressWarnings("unchecked")
                HashMap<String, WindDataPoint> winddp = (HashMap<String, WindDataPoint>) meteodp[1];
                if (winddp != null && winddp.size() > 0)
                    mergeDataToInternalDataList(dataPointsWind, winddp);
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
                mergeDataToInternalDataList(dataPointsWaves, wavesdp);
        }
    }

    private void loadChlorophyllFromFiles() {
        File[] fileList = FileUtil.getFilesFromDisk(baseFolderForChlorophyllNetCDFFiles, chlorophyllFilePattern);
        if (fileList == null)
            return;

        for (File fx : fileList) {
            HashMap<String, ChlorophyllDataPoint> chlorophylldp = processChlorophyllFile(fx.getAbsolutePath());
            if (chlorophylldp != null && chlorophylldp.size() > 0)
                mergeDataToInternalDataList(dataPointsChlorophyll, chlorophylldp);
        }
    }

    public HashMap<String, HFRadarDataPoint> getNoaaHFRadarData() {
        Date nowDate = new Date(System.currentTimeMillis());
        Date tillDate = new Date(nowDate.getTime() - dateLimitHours * DateTimeUtil.HOUR);
        LocationType lastCenter = offScreen.getLastCenter();
        if (lastCenter == null)
            return null;
        LocationType[] cornersLocs = offScreen.getLastCorners();
        if (cornersLocs == null)
            return null;
        
        return getNoaaHFRadarData(tillDate, nowDate, cornersLocs[3], cornersLocs[1]);
    }
    
    public HashMap<String, HFRadarDataPoint> getNoaaHFRadarData(Date tillDate, Date nowDate, LocationType lTop, LocationType lBot) {
        if (getHttpRequest != null)
            getHttpRequest.abort();
        getHttpRequest = null;
        try {
            String uri = noaaURL;
            uri = getNoaaURI(tillDate, nowDate, lTop, lBot);
            
            debugOut(uri);
            // http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2013-06-23%2007:56:00&to=2013-06-23%2019:56:00&lat=37.84130100297351&lng=-122.53766785260639&lat2=37.84112572375683&lng2=-122.53734062310657
            
            getHttpRequest = new HttpGet(uri);
            getHttpRequest.setHeader("Referer", uri /*"http://hfradar.ndbc.noaa.gov/tab.php"*/);
            @SuppressWarnings("unused")
            long reqTime = System.currentTimeMillis();
            HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequest);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, null);
            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>getRemoteImcData [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                getHttpRequest.abort();
                return null;
            }
            @SuppressWarnings("unused")
            long fullSize = iGetResultCode.getEntity().getContentLength();
            try (InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
                 InputStreamReader isr = new InputStreamReader(streamGetResponseBody);) {
                HashMap<String, HFRadarDataPoint> lst = processNoaaHFRadar(isr);
                return lst;
            }
            catch (Exception e) {
                throw e;
            }
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e);
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
    }
    
    public void paintWorker(Graphics2D go, StateRenderer2D renderer) {
        boolean recreateImage = offScreen.paintPhaseStartTestRecreateImageAndRecreate(go, renderer);
        if (recreateImage) {
            if (painterThread != null) {
                try {
                    abortIndicator.set(true);
                    painterThread.interrupt();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            final MapTileRendererCalculator rendererCalculator = new MapTileRendererCalculator(renderer);
            abortIndicator = new AtomicBoolean();
            painterThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Graphics2D g2 = offScreen.getImageGraphics();

                        Date dateColorLimit = new Date(System.currentTimeMillis() - 3 * DateTimeUtil.HOUR);
                        Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);

                        if (showCurrents) {
                            try {
                                EnvDataPaintHelper.paintHFRadarInGraphics(rendererCalculator, g2, 128, dateColorLimit, dateLimit, dataPointsCurrents,
                                        ignoreDateLimitToLoad, offScreen.getOffScreenBufferPixel(), colorMapCurrents, 0, maxCurrentCmS,
                                        showCurrentsLegend, showCurrentsLegendFromZoomLevel, font8Pt, showDataDebugLegend, abortIndicator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }
                        if (showSST) {
                            try {
                                EnvDataPaintHelper.paintSSTInGraphics(rendererCalculator, g2, 128, dateColorLimit, dateLimit, dataPointsSST, ignoreDateLimitToLoad,
                                        offScreen.getOffScreenBufferPixel(), colorMapSST, minSST, maxSST, showSSTLegend,
                                        showSSTLegendFromZoomLevel, font8Pt, showDataDebugLegend, abortIndicator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }
                        if (showChlorophyll) {
                            try {
                                EnvDataPaintHelper.paintChlorophyllInGraphics(rendererCalculator, g2, 128, dateColorLimit, dateLimit, dataPointsChlorophyll, ignoreDateLimitToLoad,
                                        offScreen.getOffScreenBufferPixel(), colorMapChlorophyll, minChlorophyll, maxChlorophyll, showChlorophyllLegend,
                                        showChlorophyllLegendFromZoomLevel, font8Pt, showDataDebugLegend, abortIndicator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }
                        if (showWind) {
                            try {
                                EnvDataPaintHelper.paintWindInGraphics(rendererCalculator, g2, 128, dateColorLimit, dateLimit, dataPointsWind, ignoreDateLimitToLoad,
                                        offScreen.getOffScreenBufferPixel(), useColorMapForWind, colorMapWind, 0, maxWind, font8Pt,
                                        showDataDebugLegend, abortIndicator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }
                        if (showWaves) {
                            try {
                                EnvDataPaintHelper.paintWavesInGraphics(rendererCalculator, g2, 128, dateColorLimit, dateLimit, dataPointsWaves, ignoreDateLimitToLoad,
                                        offScreen.getOffScreenBufferPixel(), colorMapWaves, 0, maxWaves, showWavesLegend,
                                        showWavesLegendFromZoomLevel, font8Pt, showDataDebugLegend, abortIndicator);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                                offScreen.triggerImageRebuild();
                            }
                        }

                        g2.dispose();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        offScreen.triggerImageRebuild();
                    }
                    catch (Error e) {
                        e.printStackTrace();
                        offScreen.triggerImageRebuild();
                    }
                }
            }, "EnvironDisp::Painter");
            painterThread.setDaemon(true);
            painterThread.start();
        }
        offScreen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRendererNoGraphicDispose(go, renderer);

        paintColorbars(go, renderer);
    }

    /**
     * @param go
     * @param renderer
     */
    private void paintColorbars(Graphics2D go, StateRenderer2D renderer) {
        int offsetHeight = 130;
        int offsetWidth = 5;
        int offsetDelta = 130;
        int counter = 2;
        if (showCurrents && showCurrentsColorbar) { // no need for "&& counter > 0" here
            counter--;
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapCurrents, I18n.text("Currents"), "cm/s", 0, maxCurrentCmS);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
        if (showSST && showSSTColorbar) {  // no need for "&& counter > 0" here
            counter--;
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapSST, I18n.text("SST"), "\u00B0C", minSST, maxSST);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
        if (showWind && showWindColorbar && counter > 0) {
            counter--;
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapWind, I18n.text("Wind"), "kn", 0, maxWind);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
        if (showWaves && showWavesColorbar && counter > 0) {
            counter--;
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapWaves, I18n.text("Waves"), "m", 0, maxWaves);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
        if (showChlorophyll && showChlorophyllColorbar && counter > 0) {
            counter--;
            Graphics2D gl = (Graphics2D) go.create();
            gl.translate(offsetWidth, offsetHeight);
            ColorBarPainterUtil.paintColorBar(gl, colorMapChlorophyll, I18n.text("Chlorophyll"), "mg/m\u00B3", minChlorophyll, maxChlorophyll);
            gl.dispose();
            offsetHeight += offsetDelta;
        }
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

    private HashMap<String, ChlorophyllDataPoint> processChlorophyllFile(String fileName) {
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        if (!new File(fxName).exists())
            return new HashMap<>();
        return LoaderHelper.processChlorophyllFile(fxName, ignoreDateLimitToLoad ? null : createDateLimitToRemove());
    }

    private HashMap<String, HFRadarDataPoint> processNoaaHFRadar(Reader readerInput) {
        long deltaTimeToHFRadarHistoricalData = dateLimitHours * DateTimeUtil.HOUR;
        
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        BufferedReader reader = null;
        Date dateLimite = new Date(System.currentTimeMillis() - deltaTimeToHFRadarHistoricalData);
        try {
            reader = new BufferedReader(readerInput);
            String line = reader.readLine();
            line = reader.readLine(); // ignoring header line
            for (/* int i = 0 */; line != null; /* i++ */) {
                if (line.startsWith("#")) {
                    line = reader.readLine();
                    continue;
                }

                String[] tokens = line.split("[\t ,]");
                try {
                    String dateStr = tokens[4].replaceAll("\"", "");
                    String timeStr = tokens[5].replaceAll("\"", "");
                    Date date = dateTimeFormaterUTC.parse(dateStr + " " + timeStr);
                    if (date.before(dateLimite) && !ignoreDateLimitToLoad) {
                        line = reader.readLine();
                        continue;
                    }
                    
                    double lat = AngleUtils.nomalizeAngleDegrees180(Double.parseDouble(tokens[0]));
                    double lon =  AngleUtils.nomalizeAngleDegrees180(Double.parseDouble(tokens[1]));
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
                    
//                    debugOut(dp);
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

    private void debugOut(Object message) {
        if (showDataDebugLegend)
            System.out.println(message);
        else
            NeptusLog.pub().debug(message);
    }

    @SuppressWarnings("unused")
    private void debugOut(Object message, Throwable t) {
        if (showDataDebugLegend) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.out.println(message + "\n" + sw.toString());
        }
        else {
            NeptusLog.pub().debug(message, t);
        }
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
//                debugOut(line);
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
        
        String url = "http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2017-04-23%2023:40:00&to=2017-04-24%2011:40:00&lat=24.72893474935707&lng=-130.71583135132266&lat2=46.951810314247766&lng2=-85.89161260132266";
        HttpClientConnectionHelper httpComm = new HttpClientConnectionHelper("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
        httpComm.initializeComm();
        HttpGet hget = new HttpGet(url);
        hget.setHeader("Referer", url);
        try {
            CloseableHttpResponse iGetResultCode = httpComm.getClient().execute(hget);
            InputStream ris = iGetResultCode.getEntity().getContent();
            System.out.println(StringEscapeUtils.escapeCsv(StreamUtil.copyStreamToString(ris)));
        }
        catch (ClientProtocolException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

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
        Date newDate = new Date(dp.getDateUTC().getTime() + DateTimeUtil.HOUR * 3 + DateTimeUtil.MINUTE * 30);
        System.out.println(newDate);
        dp.useMostRecent(newDate);
        System.out.println("" + dp);
    }
}
