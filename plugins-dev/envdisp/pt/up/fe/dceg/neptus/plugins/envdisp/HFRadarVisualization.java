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
import java.awt.geom.GeneralPath;
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
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
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
     * Currents, wind, waves, SST, bathy 
     */
    
    @NeptusProperty(name = "Visible", userLevel = LEVEL.REGULAR)
    public boolean visible = true;

    @NeptusProperty(name = "Seconds between updates")
    public long updateSeconds = 60;

    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR)
    public int dateLimitHours = 12;
    
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.ADVANCED)
    public boolean ignoreDateLimitToLoad = true;
    
    @NeptusProperty(name = "Request data from Web", hidden=true)
    public boolean requestFromWeb = false;

    @NeptusProperty(name = "Load data from file (hfradar.txt)")
    public boolean loadFromFile = false;
    
    @NeptusProperty(name = "HF-Radar most recent (true) or mean (false)", userLevel = LEVEL.REGULAR)
    public boolean hfradarUseMostRecentOrMean = true;
    
    private boolean clearImgCachRqst = false;

    public static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'SS");
    public static final SimpleDateFormat dateFormaterUTC = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat timeFormaterUTC = new SimpleDateFormat("HH':'mm");
    {
        dateTimeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        timeFormaterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    private final static GeneralPath arrow = new GeneralPath();
    static {
        arrow.moveTo(-5, 6);
        arrow.lineTo(0, -6);
        arrow.lineTo(5, 6);
        arrow.lineTo(0, 6 * 1 / 20);
        arrow.closePath();
    }
    
    //  http://hfradar.ndbc.noaa.gov/tab.php?from=2013-06-22%2015:00:00&to=2013-06-22%2015:00:00&p=1&lat=38.324420427006515&lng=-119.94323730468749&lat2=35.69299463209881&lng2=-124.33776855468749
    //  http://hfradar.ndbc.noaa.gov/tabdownload.php?from=2013-06-23%2009:34:00&to=2013-06-23%2021:34:00&lat=37.78799270017669&lng=-122.39269445535145&lat2=37.78781729434937&lng2=-122.39236722585163
    private String noaaURL = "http://hfradar.ndbc.noaa.gov/tabdownload.php?" +
    		"from=#FROM_DATE#%20#FROM_TIME#:00&to=#TO_DATE#%20#TO_TIME#:00&lat=#LAT1#&lng=#LNG1#&lat2=#LAT2#&lng2=#LNG2#";
    // lat lon speed (cm/s)    degree  acquired    resolution (km) origin
    //private double noaaMaxLat=75.40885422846455, noaaMinLng=-42.1875, noaaMinLat=12.21118019150401, noaaMaxLng=177.1875;
    private double noaaMaxLat=55.47885346331034, noaaMaxLng=-61.87500000000001, noaaMinLat=14.093957177836236, noaaMinLng=-132.1875;
    
    private static final String sampleNoaaFile = "hfradar-noaa-sample1.txt";
    
    private ColorMap colorMap = new InterpolationColorMap("RGB", new double[] { 0.0, 0.1, 0.3, 0.5, 1.0 }, new Color[] {
            new Color(0, 0, 255), new Color(0, 0, 255), new Color(0, 255, 0), new Color(255, 0, 0), new Color(255, 0, 0) });
    private double minCmS = 0;
    private double maxCmS = 200;
    
    private BufferedImage cacheImg = null;
    private Dimension dim = null;
    private int lastLod = -1;
    private LocationType lastCenter = null;
    private double lastRotation = Double.NaN;
    
    private HttpClientConnectionHelper httpComm = new HttpClientConnectionHelper();
    private HttpGet getHttpRequest;
    
    private HashMap<String, HFRadarDataPoint> dataPoints = new HashMap<>();
    
    public HFRadarVisualization(ConsoleLayout console) {
        super(console);
        httpComm.initializeComm();
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        HashMap<String, HFRadarDataPoint> tdp = processNoaaHFRadarTest();
        mergeDataToInternalDataList(tdp);
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

    private void updateValues() {
        for (String dpID : dataPoints.keySet().toArray(new String[0])) {
            HFRadarDataPoint dp = dataPoints.get(dpID);
            if (dp == null)
                continue;
            
            updateHFRadarToUseMostRecentOrMean(dp);
        }
    }

    /**
     * @param dp
     * @return
     */
    private void updateHFRadarToUseMostRecentOrMean(HFRadarDataPoint dp) {
        Date nowDate = new Date();
        if (hfradarUseMostRecentOrMean)
            dp.useMostRecent(nowDate);
        else
            dp.calculateMean(nowDate);
    }

    private void cleanDataPointsBeforeDate() {
        if (ignoreDateLimitToLoad)
            return;
        
        Date dateLimit = new Date(System.currentTimeMillis() - dateLimitHours * DateTimeUtil.HOUR);
        for (String dpID : dataPoints.keySet().toArray(new String[0])) {
            HFRadarDataPoint dp = dataPoints.get(dpID);
            if (dp == null)
                continue;
            
            if (dp.getDateUTC().before(dateLimit))
                dataPoints.remove(dpID);
            else {
                // Cleanup historicalData
                dp.purgeAllBefore(dateLimit);
            }
        }
    }

    public void mergeDataToInternalDataList(HashMap<String, HFRadarDataPoint> toMergeData) {
        for (String dpId : toMergeData.keySet()) {
            HFRadarDataPoint dp = toMergeData.get(dpId);
            HFRadarDataPoint dpo = dataPoints.get(dpId);
            if (dpo == null) {
                dataPoints.put(dpId, dp);
                dpo = dp;
            }
            else {
                ArrayList<HFRadarDataPoint> histToMergeData = dp.getHistoricalData();
                ArrayList<HFRadarDataPoint> histOrigData = dpo.getHistoricalData();
                ArrayList<HFRadarDataPoint> toAddDP = new ArrayList<>();
                for (HFRadarDataPoint hdp : histToMergeData) {
                    boolean foundMatch = false;
                    for (HFRadarDataPoint hodp : histOrigData) {
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
//            dpo.calculateMean();
        }
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
            LocationType loc = new LocationType();
            for (HFRadarDataPoint dp : dataPoints.values()) {
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
                color = colorMap.getColor(dp.getSpeedCmS() / maxCmS);
                if (dp.getDateUTC().before(dateColorLimit))
                    color = ColorUtils.setTransparencyToColor(color, 128);
                gt.setColor(color);
                gt.rotate(-Math.toRadians(headingV) - renderer.getRotation());
                gt.fill(arrow);
                
                gt.dispose();
            }

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
        
        for (HFRadarDataPoint elm : hfdp.values()) {
            updateHFRadarToUseMostRecentOrMean(elm);
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
