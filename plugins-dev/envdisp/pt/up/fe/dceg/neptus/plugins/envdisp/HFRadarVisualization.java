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
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.InterpolationColorMap;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="HF Radar Visualization", author="Paulo Dias", version="0.1")
@LayerPriority(priority = -50)
public class HFRadarVisualization extends SimpleSubPanel implements Renderer2DPainter {

    public boolean requestFromWeb = false;
    
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
        arrow.moveTo(-20 / 2, 14 / 2);
        arrow.lineTo(0, -14 / 2);
        arrow.lineTo(20 / 2, 14 / 2);
        arrow.lineTo(0, 14 / 2 * 3 / 5);
        arrow.closePath();
    }
    
    //  http://hfradar.ndbc.noaa.gov/tab.php?from=2013-06-22%2015:00:00&to=2013-06-22%2015:00:00&p=1&lat=38.324420427006515&lng=-119.94323730468749&lat2=35.69299463209881&lng2=-124.33776855468749
    private String noaaURL = "http://hfradar.ndbc.noaa.gov/tabdownload.php?" +
    		"from=#FROM_DATE#%20#FROM_TIME#:00&to=#TO_DATE#%20#TO_TIME#:00&lat=#LAT1#&lng=#LNG1#&lat2=#LAT2#&lng2=#LNG2#";
    // lat lon speed (cm/s)    degree  acquired    resolution (km) origin
    //private double noaaMaxLat=75.40885422846455, noaaMinLng=-42.1875, noaaMinLat=12.21118019150401, noaaMaxLng=177.1875;
    private double noaaMaxLat=55.47885346331034, noaaMaxLng=-61.87500000000001, noaaMinLat=14.093957177836236, noaaMinLng=-132.1875;
    
    private static final String sampleNoaaFile = "hfradar-noaa-sample.txt";
    
    private ColorMap colorMap = new InterpolationColorMap("RGB",
                    new double[] {0.0, 0.5, 1.0},
                    new Color[] {new Color(0,0,255),new Color(0,255,0),new Color(255,0,0)} 
                    );
    private double minCmS = 0;
    private double maxCmS = 200;
    
    private HttpClientConnectionHelper httpComm = new HttpClientConnectionHelper();
    
    private ArrayList<HFRadarDataPoint> dataPoints = new ArrayList<>();
    
    public HFRadarVisualization(ConsoleLayout console) {
        super(console);
        httpComm.initializeComm();
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        ArrayList<HFRadarDataPoint> tdp = processNoaaHFRadarTest();
        if (tdp.size() > 0)
            dataPoints.addAll(tdp);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        httpComm.cleanUp();
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        Graphics2D g = (Graphics2D) g2.create();
        
        //renderer.getCenter();
        //renderer.getTopLeftLocationType();
        //renderer.getBottomRightLocationType();
        LocationType loc = new LocationType(); 
        for (HFRadarDataPoint dp : dataPoints) {
            g = (Graphics2D) g2.create();
            
            loc.setLatitude(dp.lat);
            loc.setLongitude(dp.lon);
            
            Point2D pt = renderer.getScreenPosition(loc);
            //if (pt.getX() < 0 || )
            
            g.translate(pt.getX(), pt.getY());
            Color color = Color.WHITE;
            color = colorMap.getColor(dp.speedCmS / maxCmS);
            g.setColor(color);
            g.rotate(-Math.toRadians(dp.headingDegrees));
            g.fill(arrow);
            
            g.dispose();
        }
        
    }
    
    private ArrayList<HFRadarDataPoint> processNoaaHFRadarTest() {
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
        return new ArrayList<HFRadarDataPoint>(); 
    }
    
    private ArrayList<HFRadarDataPoint> processNoaaHFRadar(Reader readerInput) {
        ArrayList<HFRadarDataPoint> hfdp = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(readerInput);
            String line = reader.readLine();
            for (int i = 0; line != null; i++) {
                String[] tokens = line.split("[\t ,]");
                try {
                    HFRadarDataPoint dp = new HFRadarDataPoint();
                    dp.lat = Double.parseDouble(tokens[0]);
                    dp.lon = Double.parseDouble(tokens[1]);
                    dp.speedCmS = Double.parseDouble(tokens[2]);
                    dp.headingDegrees = Double.parseDouble(tokens[3]);
                    String dateStr = tokens[4];
                    String timeStr = tokens[5];
                    dp.dateUTC = dateTimeFormaterUTC.parse(dateStr + " " + timeStr);
                    dp.resolutionKm = Double.parseDouble(tokens[6]);
                    for (int j = 7; j < tokens.length; j++) {
                        dp.info += tokens[j];
                    }
                    hfdp.add(dp);
                    System.out.println(dp);
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
        
        return hfdp;
    }
    
    public static class HFRadarDataPoint {
        //lat lon speed (cm/s)    degree  acquired (Date+Time)    resolution (km) origin
        public double lat;
        public double lon;
        public double speedCmS;
        public double headingDegrees;
        public Date dateUTC;
        public double resolutionKm = -1;
        public String info = "";
        
        @Override
        public String toString() {
            return "Lat:\t" + lat +
            		"\tLon:\t" + lon +
            		"\tSpeed:\t" + speedCmS + 
            		"\tHeading:\t" + headingDegrees + 
            		"\tDate:\t" + dateUTC +
            		"\tResolution:\t" + resolutionKm +
            		"\tInfo:\t" + info;
        }
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
                    HFRadarDataPoint dp = new HFRadarDataPoint();
                    dp.lat = Double.parseDouble(tokens[0]);
                    dp.lon = Double.parseDouble(tokens[1]);
                    dp.speedCmS = Double.parseDouble(tokens[2]);
                    dp.headingDegrees = Double.parseDouble(tokens[3]);
                    String dateStr = tokens[4];
                    String timeStr = tokens[5];
                    dp.dateUTC = dateTimeFormaterUTC.parse(dateStr + " " + timeStr);
                    dp.resolutionKm = Double.parseDouble(tokens[6]);
                    for (int j = 7; j < tokens.length; j++) {
                        dp.info += tokens[j];
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
