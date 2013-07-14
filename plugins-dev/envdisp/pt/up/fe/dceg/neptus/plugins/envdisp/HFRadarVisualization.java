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
 * Author: pdias
 * Jun 22, 2013
 */
package pt.up.fe.dceg.neptus.plugins.envdisp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
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
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.ColormapOverlay;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.ColorUtils;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="HF Radar Visualization", author="Paulo Dias", version="0.1")
@LayerPriority(priority = -50)
public class HFRadarVisualization extends SimpleSubPanel implements Renderer2DPainter, IPeriodicUpdates, ConfigurationListener {

    /*
     * Currents, wind, waves, SST 
     */
    
    @NeptusProperty(name = "Visible", userLevel = LEVEL.REGULAR)
    public boolean visible = true;

    @NeptusProperty(name = "Show currents", userLevel = LEVEL.REGULAR)
    public boolean showCurrents = true;

    @NeptusProperty(name = "Show SST", userLevel = LEVEL.REGULAR)
    public boolean showSST = true;

    @NeptusProperty(name = "Show wind", userLevel = LEVEL.REGULAR)
    public boolean showWind = true;

    @NeptusProperty(name = "Show waves", userLevel = LEVEL.REGULAR)
    public boolean showWaves = true;

    @NeptusProperty(name = "Seconds between updates")
    public long updateSeconds = 60;

    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR)
    public int dateLimitHours = 12;
    
    @NeptusProperty(name = "Use data x hour in the future (hours)", userLevel = LEVEL.REGULAR)
    public int dateHoursToUseForData = 1;
    
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.ADVANCED)
    public boolean ignoreDateLimitToLoad = true;
    
    @NeptusProperty(name = "Request data from Web", hidden=true)
    public boolean requestFromWeb = false;

    @NeptusProperty(name = "Load data from file (hfradar.txt)")
    public boolean loadFromFile = false;
    
    @NeptusProperty(name = "Show currents as most recent (true) or mean (false) value", userLevel = LEVEL.REGULAR)
    public boolean hfradarUseMostRecentOrMean = true;
    
    private boolean clearImgCachRqst = false;

    public static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'SS");
    public static final SimpleDateFormat dateTimeFormaterSpacesUTC = new SimpleDateFormat("yyyy MM dd  HH mm SS");
    public static final SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat timeFormaterUTC = new SimpleDateFormat("HH':'mm");
    {
        dateTimeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateTimeFormaterSpacesUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    private final static Path2D.Double arrow = new Path2D.Double();
    static {
        arrow.moveTo(-5, 6);
        arrow.lineTo(0, -6);
        arrow.lineTo(5, 6);
        arrow.lineTo(0, 6 * 1 / 20);
        arrow.closePath();
    }

    private final static Ellipse2D circle = new Ellipse2D.Double(-4, -4, 8, 8);
    
    private final static Path2D.Double windPoleKnots = new Path2D.Double();
    static {
        windPoleKnots.moveTo(0, 0);
        windPoleKnots.lineTo(0, 14);
        windPoleKnots.closePath();
    }
    private final static Path2D.Double wind50Knots1 = new Path2D.Double();
    static {
        wind50Knots1.moveTo(0, 14);
        wind50Knots1.lineTo(-8, 14);
        wind50Knots1.lineTo(0, 12);
        wind50Knots1.closePath();
    }
    private final static Path2D.Double wind10Knots1 = new Path2D.Double();
    static {
        wind10Knots1.moveTo(0, 14);
        wind10Knots1.lineTo(-8, 14);
        wind10Knots1.closePath();
    }
    private final static Path2D.Double wind5Knots2 = new Path2D.Double();
    static {
        wind5Knots2.moveTo(0, 12);
        wind5Knots2.lineTo(-4, 12);
        wind5Knots2.closePath();
    }
    private final static Path2D.Double wind10Knots2 = new Path2D.Double();
    static {
        wind10Knots2.moveTo(0, 12);
        wind10Knots2.lineTo(-8, 12);
        wind10Knots2.closePath();
    }
    private final static Path2D.Double wind5Knots3 = new Path2D.Double();
    static {
        wind5Knots3.moveTo(0, 10);
        wind5Knots3.lineTo(-4, 10);
        wind5Knots3.closePath();
    }
    private final static Path2D.Double wind10Knots3 = new Path2D.Double();
    static {
        wind10Knots3.moveTo(0, 10);
        wind10Knots3.lineTo(-8, 10);
        wind10Knots3.closePath();
    }
    private final static Path2D.Double wind5Knots4 = new Path2D.Double();
    static {
        wind5Knots4.moveTo(0, 8);
        wind5Knots4.lineTo(-4, 8);
        wind5Knots4.closePath();
    }
    private final static Path2D.Double wind10Knots4 = new Path2D.Double();
    static {
        wind10Knots4.moveTo(0, 8);
        wind10Knots4.lineTo(-8, 8);
        wind10Knots4.closePath();
    }
    private final static Path2D.Double wind5Knots5 = new Path2D.Double();
    static {
        wind5Knots5.moveTo(0, 6);
        wind5Knots5.lineTo(-4, 6);
        wind5Knots5.closePath();
    }

    //  http://hfradar.ndbc.noaa.gov/tab.php?from=2013-06-22%2015:00:00&to=2013-06-22%2015:00:00&p=1&lat=38.324420427006515&lng=-119.94323730468749&lat2=35.69299463209881&lng2=-124.33776855468749
    //  http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2013-06-23%2009:34:00&to=2013-06-23%2021:34:00&lat=37.78799270017669&lng=-122.39269445535145&lat2=37.78781729434937&lng2=-122.39236722585163
    private String noaaURL = "http://hfradar.ndbc.noaa.gov/tabdownload.php?" +
    		"from=#FROM_DATE#%20#FROM_TIME#:00&to=#TO_DATE#%20#TO_TIME#:00&lat=#LAT1#&lng=#LNG1#&lat2=#LAT2#&lng2=#LNG2#";
    // lat lon speed (cm/s)    degree  acquired    resolution (km) origin
    //private double noaaMaxLat=75.40885422846455, noaaMinLng=-42.1875, noaaMinLat=12.21118019150401, noaaMaxLng=177.1875;
    private double noaaMaxLat=55.47885346331034, noaaMaxLng=-61.87500000000001, noaaMinLat=14.093957177836236, noaaMinLng=-132.1875;
    
    private static final String sampleNoaaFile = "hfradar-noaa-sample1.txt";
    private static final String sampleTuvFile = "TOTL_TRAD_2013_07_04_1100.tuv";
    private static final String sampleMeteoFile = "meteo_20130705.nc";
    private static final String sampleWavesFile = "waves_S_20130704.nc";
    
    protected final double m_sToKnotConv = 1.94384449244;
    
    private ColorMap colorMapCurrents = new InterpolationColorMap("RGB", new double[] { 0.0, 0.1, 0.3, 0.5, 1.0 }, new Color[] {
            new Color(0, 0, 255), new Color(0, 0, 255), new Color(0, 255, 0), new Color(255, 0, 0), new Color(255, 0, 0) });
    private double minCurrentCmS = 0;
    private double maxCurrentCmS = 200;

    private ColorMap colorMapSST = ColorMapFactory.createJetColorMap();
    private double minSST = -50;
    private double maxSST = 40;

    private ColorMap colorMapWind = colorMapCurrents;
    private double minWind = 0;
    private double maxWind = 65 / m_sToKnotConv;

    private ColorMap colorMapWaves = ColorMapFactory.createJetColorMap();
    private double minWaves = 0;
    private double maxWaves = 7;

    private BufferedImage cacheImg = null;
    private Dimension dim = null;
    private int lastLod = -1;
    private LocationType lastCenter = null;
    private double lastRotation = Double.NaN;
    
    private HttpClientConnectionHelper httpComm = new HttpClientConnectionHelper();
    private HttpGet getHttpRequest;
    
    // ID is lat/lon
    private HashMap<String, HFRadarDataPoint> dataPointsCurrents = new HashMap<>();
    private HashMap<String, SSTDataPoint> dataPointsSST = new HashMap<>();
    private HashMap<String, WindDataPoint> dataPointsWind = new HashMap<>();
    private HashMap<String, WavesDataPoint> dataPointsWaves = new HashMap<>();
    
    public HFRadarVisualization(ConsoleLayout console) {
        super(console);
        httpComm.initializeComm();
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @SuppressWarnings("unchecked")
    @Override
    public void initSubPanel() {
        HashMap<String, HFRadarDataPoint> tdp = processNoaaHFRadarTest();
        mergeCurrentsDataToInternalDataList(tdp);
        
        tdp = processTuvHFRadarTest(sampleTuvFile);
        if (tdp != null && tdp.size() > 0)
            mergeCurrentsDataToInternalDataList(tdp);

        HashMap<?, ?>[] meteodp = processMeteoFile(sampleMeteoFile);
        HashMap<String, SSTDataPoint> sstdp = (HashMap<String, SSTDataPoint>) meteodp[0];
        if (sstdp != null && sstdp.size() > 0)
            mergeSSTDataToInternalDataList(sstdp);
        HashMap<String, WindDataPoint> winddp = (HashMap<String, WindDataPoint>) meteodp[1];
        if (winddp != null && winddp.size() > 0)
            mergeWindDataToInternalDataList(winddp);

        HashMap<String, WavesDataPoint> wavesdp = processWavesFile(sampleWavesFile);
        if (wavesdp != null && wavesdp.size() > 0)
            mergeWavesDataToInternalDataList(wavesdp);
        
        update();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        httpComm.cleanUp();
    }
    
    public String validateDataLimitHours(int value) {
        if (value < 3 && value > 24)
            return "Keep it between 3 and 24";
        return null;
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return updateSeconds * 1000;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public synchronized boolean update() {
//        System.out.println("######");
//        if (false && requestFromWeb) {
//            HashMap<String, HFRadarDataPoint> dpLts = getNoaaHFRadarData();
//            if (dpLts != null) {
//                System.out.println(dpLts.size() + " ------------------------------");
//                
//            }
//        }
        
        cleanDataPointsBeforeDate();
        updateValues();
        clearImgCachRqst = true;
        
        return true;
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    public void propertiesChanged() {
        updateValues();
        clearImgCachRqst = true;
    }

    private Date createDateToMostRecent() {
        Date nowDate = new Date(System.currentTimeMillis() + dateHoursToUseForData * DateTimeUtil.HOUR);
        return nowDate;
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
        if (ignoreDateLimitToLoad)
            return;
        
        Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);
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
//                ArrayList<HFRadarDataPoint> histToMergeData = dp.getHistoricalData();
//                ArrayList<HFRadarDataPoint> histOrigData = dpo.getHistoricalData();
//                ArrayList<HFRadarDataPoint> toAddDP = new ArrayList<>();
//                for (HFRadarDataPoint hdp : histToMergeData) {
//                    boolean foundMatch = false;
//                    for (HFRadarDataPoint hodp : histOrigData) {
//                        if (hdp.getDateUTC().equals(hodp.getDateUTC())) {
//                            foundMatch = true;
//                            break;
//                        }
//                    }
//                    if (foundMatch)
//                        continue;
//                    toAddDP.add(hdp);
//                }
//                if (toAddDP.size() > 0)
//                    histOrigData.addAll(toAddDP);
            }
//            dpo.calculateMean();
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
        for (BaseDataPoint<?> hdp : histToMergeData) {
            boolean foundMatch = false;
            for (BaseDataPoint<?> hodp : histOrigData) {
                if (hdp.getDateUTC().equals(hodp.getDateUTC())) {
                    foundMatch = true;
                    break;
                }
            }
            if (foundMatch)
                continue;
            toAddDP.add(hdp);
        }
        if (toAddDP.size() > 0)
            histOrigData.addAll(toAddDP);
    }

    
    public HashMap<String, HFRadarDataPoint> getNoaaHFRadarData() {
        Date nowDate = new Date(System.currentTimeMillis());
        Date tillDate = new Date(nowDate.getTime() - dateLimitHours * DateTimeUtil.HOUR);
        if (lastCenter == null)
            return null;
        LocationType lc = lastCenter.getNewAbsoluteLatLonDepth();
        Dimension boxDim = dim.getSize();
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
        uri = uri.replace("#LAT1#", "" + lTop.getLatitudeAsDoubleValue());
        uri = uri.replace("#LNG1#", "" + lTop.getLongitudeAsDoubleValue());
        uri = uri.replace("#LAT2#", "" + lBot.getLatitudeAsDoubleValue());
        uri = uri.replace("#LNG2#", "" + lBot.getLongitudeAsDoubleValue());
        return uri;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D go, StateRenderer2D renderer) {
        if (!visible)
            return;
        
        if (!clearImgCachRqst) {
            if (dim == null || lastLod < 0 || lastCenter == null || Double.isNaN(lastRotation)) {
                Dimension dimN = renderer.getSize(new Dimension());
                if (dimN.height != 0 && dimN.width != 0)
                    dim = dimN;
                cacheImg = null;
            }
            else if (!dim.equals(renderer.getSize()) || lastLod != renderer.getLevelOfDetail()
                    || !lastCenter.equals(renderer.getCenter()) || Double.compare(lastRotation, renderer.getRotation()) != 0) {
                cacheImg = null;
            }
        }
        else {
            cacheImg = null;
        }
        
        if (cacheImg == null) {
            dim = renderer.getSize(new Dimension());
            lastLod = renderer.getLevelOfDetail();
            lastCenter = renderer.getCenter();
            lastRotation = renderer.getRotation();

//            System.out.println("#########################");
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            cacheImg = gc.createCompatibleImage((int) dim.getWidth(), (int) dim.getHeight(), Transparency.BITMASK); 
            Graphics2D g2 = cacheImg.createGraphics();
            
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
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

        if (cacheImg != null) {
            //          System.out.println(".........................");
            Graphics2D g3 = (Graphics2D) go.create();
//            if (renderer.getRotation() != 0) {
//                g3.rotate(-renderer.getRotation(), renderer.getWidth() / 2, renderer.getHeight() / 2);
//            }
            // g3.drawImage(cacheImg, 0, 0, null);
            g3.drawImage(cacheImg, 0, 0, cacheImg.getWidth(), cacheImg.getHeight(), 0, 0, cacheImg.getWidth(),
                    cacheImg.getHeight(), null);
            g3.dispose();
        }
    }


    /**
     * @param renderer
     * @param g2
     * @param dateColorLimit
     * @param dateLimit
     */
    private void paintHFRadarInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        LocationType loc = new LocationType();
        for (HFRadarDataPoint dp : dataPointsCurrents.values()) {
            if (dp.getDateUTC().before(dateLimit) && !ignoreDateLimitToLoad)
                continue;
            
            double latV = dp.getLat();
            double lonV = dp.getLon();
            double headingV = dp.getHeadingDegrees();
            
            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(headingV))
                continue;
            
            loc.setLatitude(latV);
            loc.setLongitude(lonV);
            
            Point2D pt = renderer.getScreenPosition(loc);

            if (!isVisibleInRender(pt, renderer))
                continue;
            
            Graphics2D gt = (Graphics2D) g2.create();
            
            gt.translate(pt.getX(), pt.getY());
            Color color = Color.WHITE;
            color = colorMapCurrents.getColor(dp.getSpeedCmS() / maxCurrentCmS);
            if (dp.getDateUTC().before(dateColorLimit))
                color = ColorUtils.setTransparencyToColor(color, 128);
            gt.setColor(color);
            gt.rotate(-Math.toRadians(headingV) - renderer.getRotation());
            gt.fill(arrow);
            
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
        LocationType loc = new LocationType();
        ColormapOverlay overlay = new ColormapOverlay("SST", 20, false, 0);
        for (SSTDataPoint dp : dataPointsSST.values()) {
            if (dp.getDateUTC().before(dateLimit) && !ignoreDateLimitToLoad)
                continue;
            
            double latV = dp.getLat();
            double lonV = dp.getLon();
            double sstV = dp.getSst();
            
            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(sstV))
                continue;
            
            loc.setLatitude(latV);
            loc.setLongitude(lonV);
            
            Point2D pt = renderer.getScreenPosition(loc);

            if (!isVisibleInRender(pt, renderer))
                continue;
            
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(pt.getX(), pt.getY());
            Color color = Color.WHITE;
            color = colorMapSST.getColor((dp.getSst() - minSST) / (maxSST - minSST));
            if (dp.getDateUTC().before(dateColorLimit))
                color = ColorUtils.setTransparencyToColor(color, 128);
            gt.setColor(color);
            gt.draw(circle);
            gt.fill(circle);
            
//            overlay.addSample(loc.getNewAbsoluteLatLonDepth(), dp.getSst());
            
            gt.dispose();
        }

//        Graphics2D gt = (Graphics2D) g2.create();
//        overlay.paint(gt, renderer);
//        gt.dispose();
    }

    /**
     * @param renderer
     * @param g2
     * @param dateColorLimit
     * @param dateLimit
     */
    private void paintWindInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        LocationType loc = new LocationType();
        for (WindDataPoint dp : dataPointsWind.values()) {
            if (dp.getDateUTC().before(dateLimit) && !ignoreDateLimitToLoad)
                continue;
            
            double latV = dp.getLat();
            double lonV = dp.getLon();
            double speedV = dp.getSpeed();
            double headingV = dp.getHeading();
            
            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(speedV)|| Double.isNaN(headingV))
                continue;
            
            loc.setLatitude(latV);
            loc.setLongitude(lonV);
            
            Point2D pt = renderer.getScreenPosition(loc);

            if (!isVisibleInRender(pt, renderer))
                continue;
            
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(pt.getX(), pt.getY());
            Color color = Color.WHITE;
            color = colorMapWind.getColor(dp.getSpeed() / maxWind);
            if (dp.getDateUTC().before(dateColorLimit))
                color = ColorUtils.setTransparencyToColor(color, 128);
            gt.setColor(color);
            gt.rotate(-Math.toRadians(headingV) - renderer.getRotation());
            // gt.fill(arrow);
            
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
            
            gt.dispose();
        }
    }

    /**
     * @param renderer
     * @param g2
     * @param dateColorLimit
     * @param dateLimit
     */
    private void paintWavesInGraphics(StateRenderer2D renderer, Graphics2D g2, Date dateColorLimit, Date dateLimit) {
        LocationType loc = new LocationType();
        for (WavesDataPoint dp : dataPointsWaves.values()) {
            if (dp.getDateUTC().before(dateLimit) && !ignoreDateLimitToLoad)
                continue;
            
            double latV = dp.getLat();
            double lonV = dp.getLon();
            double sigHeightV = dp.getSignificantHeight();
            double headingV = dp.getPeakDirection();
            double periodV = dp.getPeakPeriod();
            
            if (Double.isNaN(latV) || Double.isNaN(lonV) || Double.isNaN(sigHeightV) || Double.isNaN(headingV)
                    || Double.isNaN(periodV))
                continue;
            
            loc.setLatitude(latV);
            loc.setLongitude(lonV);
            
            Point2D pt = renderer.getScreenPosition(loc);

            if (!isVisibleInRender(pt, renderer))
                continue;
            
            Graphics2D gt = (Graphics2D) g2.create();
            gt.translate(pt.getX(), pt.getY());
            Color color = Color.WHITE;
            color = colorMapWaves.getColor(dp.getSignificantHeight() / maxWaves);
            if (dp.getDateUTC().before(dateColorLimit))
                color = ColorUtils.setTransparencyToColor(color, 128);
            gt.setColor(color);
            gt.rotate(-Math.toRadians(headingV) - renderer.getRotation());
            gt.fill(arrow);
            
            gt.dispose();
        }
    }

    /**
     * @param sPos
     * @param renderer
     * @return
     */
    private boolean isVisibleInRender(Point2D sPos, StateRenderer2D renderer) {
        Dimension rendDim = renderer.getSize();
        if (sPos.getX() < 0 && sPos.getY() < 0)
            return false;
        else if (sPos.getX() > rendDim.getWidth() && sPos.getY() > rendDim.getHeight())
            return false;
        
        return true;
    }

    private HashMap<String, HFRadarDataPoint> processNoaaHFRadarTest() {
        // InputStreamReader
        String fxName = FileUtil.getResourceAsFileKeepName(sampleNoaaFile);
        File fx = new File(fxName);
        try {
            FileReader freader = new FileReader(fx);
            return processNoaaHFRadar(freader);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new HashMap<String, HFRadarDataPoint>(); 
    }

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
        
        return LoaderHelper.processTUGHFRadar(freader, ignoreDateLimitToLoad ? null : createDateToMostRecent());
    }

    
    private HashMap<?,?>[] processMeteoFile(String fileName) {
        // InputStreamReader
        HashMap<String, SSTDataPoint> sstdp = new HashMap<>();
        HashMap<String, WindDataPoint> winddp = new HashMap<>();

        FileReader freader = getFileReaderForFile(fileName);
        if (freader == null)
            return new HashMap[] { sstdp, winddp };
        
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        return LoaderHelper.processMeteo(fxName, ignoreDateLimitToLoad ? null : createDateToMostRecent());
    }

    private HashMap<String, WavesDataPoint> processWavesFile(String fileName) {
        // InputStreamReader
        HashMap<String, WavesDataPoint> wavesddp = new HashMap<>();

        FileReader freader = getFileReaderForFile(fileName);
        if (freader == null)
            return wavesddp;
        
        String fxName = FileUtil.getResourceAsFileKeepName(fileName);
        if (fxName == null)
            fxName = fileName;
        return LoaderHelper.processWavesFile(fxName, ignoreDateLimitToLoad ? null : createDateToMostRecent());
    }

    private HashMap<String, HFRadarDataPoint> processNoaaHFRadar(Reader readerInput) {
        long deltaTimeToHFRadarHistoricalData = dateLimitHours * DateTimeUtil.HOUR;
        
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        BufferedReader reader = null;
        Date dateLimite = new Date(System.currentTimeMillis() - deltaTimeToHFRadarHistoricalData);
        try {
            reader = new BufferedReader(readerInput);
            String line = reader.readLine();
            for (int i = 0; line != null; i++) {
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
    public static void main(String[] args) {
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
}
