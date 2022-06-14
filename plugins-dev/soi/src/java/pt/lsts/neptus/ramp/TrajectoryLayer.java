/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: RicardoSantos
 * Aug 4,2021
 */
package pt.lsts.neptus.ramp;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.awt.geom.Point2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eclipsesource.json.JsonValue;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.jfree.data.json.impl.JSONValue;
import pt.lsts.imc.DevDataText;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.Goto;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.PlanTransition;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author RicardoSantos
 */
@PluginDescription(name = "Optimized Trajectories", description = "Adds trajectories and their calculated duration from JSON to the map console", icon="images/rampLogo.png")
public class TrajectoryLayer extends ConsoleLayer {

    @NeptusProperty(name = "Pollution markers endpoint", description = "Endpoint to GET pollution markers from Ripples")
    public String apiPollutionMarkers = "/pollution";

    @NeptusProperty(name = "Pollution markers status endpoint", description = "Endpoint to POST pollution markers status to Ripples")
    public String apiPollutionMarkersStatus = "/pollution";

    @NeptusProperty(name = "POST: Pollution sample", description = "Endpoint to POST samples to Ripples")
    public String aptToPostPollutionSample = "/pollution/sample";

    @NeptusProperty(name = "Pollution obstacles endpoint", description = "Endpoint to GET obstacles from Ripples")
    public String apiPollutionObstacles = "/pollution/obstacles";

    @NeptusProperty(name="Pollution samples endpoint", description = "Endpoint to GET samples from Ripples")
    public String apiPollutionSamples = "/pollution/sample";

    @NeptusProperty(name="Vehicle for sampling", description = "Vehicle that will do the pollution sample")
    public String vehicleForPollutionSample = "otter";

    @NeptusProperty(name="Vehicle to visit pollution area", description = "Vehicle that will visit pollution sample")
    public String vehicleToVisitPollution = "lauv-xplore-1";

    @NeptusProperty(name="Enable wifi", description = "Test wifi communications")
    protected boolean wifiEnable = false;

    protected List<PollutionMarker> pollutionMarkers;

    protected List<Obstacle> pollutionObstacles;

    protected List<Sample> pollutionSamples;

    public TrajectoryLayer(){
        init();
        fetchPollutionSamples();
        updatePollutionInfo();
    }

    public void init(){
        pollutionMarkers = Collections.synchronizedList(new ArrayList<PollutionMarker>());
        pollutionObstacles = Collections.synchronizedList(new ArrayList<Obstacle>());
        pollutionSamples = Collections.synchronizedList(new ArrayList<Sample>());
    }

    @Override
    public  boolean userControlsOpacity(){
        return false;
    }

    @Override
    public  void initLayer(){

        // Get Pollution markers
        try {
            String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionMarkers;
            // String serverRampApiUrl = "http://localhost:9090" + apiPollutionMarkers;
            Gson gson = new Gson();
            URL url = new URL(serverRampApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            PollutionMarker[] markers = gson.fromJson(new InputStreamReader(con.getInputStream()), PollutionMarker[].class);

            pollutionMarkers = Arrays.asList(markers);
            NeptusLog.pub().info("Fetched " + pollutionMarkers.size() + " pollution markers.");

        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        // Get Pollution obstacles
        try {
            String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionObstacles;
            // String serverRampApiUrl = "http://localhost:9090" + apiPollutionObstacles;
            URL url = new URL(serverRampApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            JsonArray obstacles = Json.parse(new InputStreamReader(con.getInputStream())).asArray();
            for(int i = 0 ; i < obstacles.size() ; i++) {
                JsonObject wpt = obstacles.get(i).asObject();

                long id = wpt.getLong("id",0);
                String description = wpt.getString("description", "");
                String timestamp = wpt.getString("timestamp", "");
                String user = wpt.getString("user", "");
                JsonArray points = wpt.get("positions").asArray();

                Obstacle obst = new Obstacle(id,description,timestamp,user,points);
                pollutionObstacles.add(obst);
                NeptusLog.pub().info("Fetched " + pollutionObstacles.size() + " pollution obstacles.");
            }
        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        // Get Sample markers
        try {
            String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionSamples;
            // String serverRampApiUrl = "http://localhost:9090" + apiPollutionSamples;
            Gson gson = new Gson();
            URL url = new URL(serverRampApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            Sample[] sampleMarkers = gson.fromJson(new InputStreamReader(con.getInputStream()), Sample[].class);

            pollutionSamples = Arrays.asList(sampleMarkers);
            NeptusLog.pub().info("Fetched " + pollutionSamples.size() + " pollution samples.");
        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }

    }

    @Override
    public  void cleanLayer(){

    }

    @Override
    public void paint(Graphics2D g0, StateRenderer2D renderer) {

        boolean displayLabel = renderer.getZoom() > 0.2;

        if(!pollutionMarkers.isEmpty()) {
            synchronized(pollutionMarkers){
                Graphics2D g = (Graphics2D) g0.create();
                for(PollutionMarker pollution : pollutionMarkers) {
                    LocationType loc = new LocationType(pollution.latitude, pollution.longitude);
                    Point2D pt2d = renderer.getScreenPosition(loc);
                    Color color;
                    switch (pollution.status) {
                        case "Synched":
                            color = new Color(1f,1f,0f,.5f );
                            break;
                        case "Exec":
                            color = new Color(1f,.6f,0f,.5f );
                            break;
                        case "Done":
                            color = new Color(0f,1f,0f,.5f );
                            break;
                        default:
                            color = new Color(1f, 0f, 0f, .5f);
                    }
                    String msg = pollution.description + " ("+ pollution.id+")";
                    pollution.paintCircle(pt2d, color, renderer.getZoom() * pollution.radius, msg, displayLabel, g);
                }
                g.dispose();
            }
        }

        if(!pollutionObstacles.isEmpty()) {
            synchronized(pollutionObstacles){
                Graphics2D g = (Graphics2D) g0.create();
                for(Obstacle o : pollutionObstacles) {
                    Vector<Point2D> locations = new Vector<>();
                    for(int i=0; i<o.obstaclePoints.size() ; i++) {
                        LocationType loc = new LocationType(o.obstaclePoints.get(i)[0], o.obstaclePoints.get(i)[1]);
                        Point2D pt2d = renderer.getScreenPosition(loc);
                        locations.add(pt2d);

                        // Close polygon
                        if(i == o.obstaclePoints.size()-1) {
                            LocationType first_loc = new LocationType(o.obstaclePoints.get(0)[0], o.obstaclePoints.get(0)[1]);
                            Point2D first_pt2d = renderer.getScreenPosition(first_loc);
                            locations.add(first_pt2d);
                        }
                    }
                    o.paintPolygon(locations, o.description, displayLabel, g);
                }
                g.dispose();
            }
        }

        if(!pollutionSamples.isEmpty() && displayLabel) {
            synchronized(pollutionSamples) {
                Graphics2D g = (Graphics2D) g0.create();
                for (Sample s : pollutionSamples) {
                    LocationType loc = new LocationType(s.latitude, s.longitude);
                    Point2D pt2d = renderer.getScreenPosition(loc);
                    s.paintIcon(pt2d, s.status, renderer.getZoom(), g);
                }
                g.dispose();
            }
        }

    }

    @Subscribe
    public void on(DevDataText msg) throws IOException {
        //System.out.println("DevDataText");
        if( wifiEnable && msg.getSourceName().equals(vehicleToVisitPollution) ) {

            HttpClient httpclient = HttpClients.createDefault();
            try {
                Pattern p = Pattern.compile("\\((.*)\\) (.*) / (.*) / (.*)");
                Matcher matcher = p.matcher(msg.getValue());
                matcher.matches();

                // Date timestamp = new Date(Double.valueOf(matcher.group(2)).longValue()*1000);
                // Parse timestamp
                double time = Double.parseDouble(matcher.group(2));
                Date timestamp = new Date(Double.valueOf(String.format("%.0f", time)).longValue() * 1000);

                if(matcher.group(1).equals("sample")) {
                    NeptusLog.pub().info("Sended pollution sample (" + matcher.group(4).toUpperCase() + ")");

                    // Parse coordinates
                    Pattern p_sample = Pattern.compile("(.*), (.*)");
                    Matcher matcher_sample = p_sample.matcher(matcher.group(3));
                    matcher_sample.matches();
                    String latMins = matcher_sample.group(1);
                    String lonMins = matcher_sample.group(2);
                    String latParts[] = latMins.split(" ");
                    String lonParts[] = lonMins.split(" ");
                    double lat = getCoords(latParts);
                    double lon = getCoords(lonParts);

                    System.out.println("*** Parsing pollution sample ***");
                    System.out.println("Timestamp: " + timestamp);
                    System.out.println("Lat: " + lat);
                    System.out.println("Lon: " + lon);
                    System.out.println("status: " + matcher.group(4).toUpperCase());

                    String pollutionStatusApiUrl = GeneralPreferences.ripplesUrl + aptToPostPollutionSample;
                    // String pollutionStatusApiUrl = "http://localhost:9090" + aptToPostPollutionSample;
                    HttpPost post = new HttpPost(pollutionStatusApiUrl);

                    Map obj = new HashMap<>();
                    obj.put("latitude", lat);
                    obj.put("longitude", lon);
                    obj.put("status", matcher.group(4).toUpperCase());
                    obj.put("timestamp", timestamp.getTime());

                    String json = JSONValue.toJSONString(obj);
                    StringEntity postingString = new StringEntity(json);
                    post.setHeader("Content-type", "application/json");
                    post.setEntity(postingString);

                    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(post);) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                        StringBuffer result = new StringBuffer();
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }
                        //System.out.println(result);
                    }
                    catch (Exception e) {
                        throw e;
                    }

                } else {
                    NeptusLog.pub().info("Changed pollution status: " + matcher.group(3) + " (" + matcher.group(4) + ")");

                    System.out.println("*** Parsing pollution status ***");
                    System.out.println(matcher.group(3));
                    System.out.println(matcher.group(4));

                    String pollutionStatusApiUrl = GeneralPreferences.ripplesUrl + apiPollutionMarkersStatus + "/" + matcher.group(3) + "/" + matcher.group(4);
                    // String pollutionStatusApiUrl = "http://localhost:9090" + apiPollutionMarkersStatus + "/" + matcher.group(3) + "/" + matcher.group(4);
                    HttpPost post = new HttpPost(pollutionStatusApiUrl);

                    try (CloseableHttpResponse response = (CloseableHttpResponse) httpclient.execute(post);) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                        StringBuffer result = new StringBuffer();
                        String line = "";
                        while ((line = rd.readLine()) != null) {
                            result.append(line);
                        }
                        //System.out.println(result);
                        initLayer();
                    }
                    catch (Exception e) {
                        //throw e;
                    }
                }
            }
            catch (Exception e) {
                //NeptusLog.pub().error(e);
            }
        }
    }

    private double getCoords(String[] coordParts) {
        double coord = Double.parseDouble(coordParts[0]);
        coord += (coord > 0) ? Double.parseDouble(coordParts[1]) / 60.0 : -Double.parseDouble(coordParts[1]) / 60.0;
        return coord;
    }


    private boolean triggerSample (Sample s) {
        for (Sample pollutionSample : pollutionSamples) {
            if(pollutionSample.id == s.id) {
                return false;
            }
        }
        return true;
    }

    @Periodic(millisBetweenUpdates = 10000)
    public void fetchPollutionSamples() {
        if(getConsole() != null) {

            // Get Sample markers
            try {
                String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionSamples;
                // String serverRampApiUrl = "http://localhost:9090" + apiPollutionSamples;
                Gson gson = new Gson();
                URL url = new URL(serverRampApiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                Sample[] sampleMarkers = gson.fromJson(new InputStreamReader(con.getInputStream()), Sample[].class);

                for(Sample s : sampleMarkers) {
                    // Trigger sample
                    if(triggerSample(s)) {
                        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(vehicleForPollutionSample);
                        if(sys != null) {

                            // Sample parameters
                            SetEntityParameters setParams = new SetEntityParameters();
                            setParams.setName("Ramp Sampler");
                            EntityParameter p1 = new EntityParameter();
                            p1.setName("Type of Sample");
                            if(s.status.equals("DIRTY"))
                                p1.setValue("Dirty");
                            else
                                p1.setValue("Clean");

                            Goto go = new Goto();
                            double lat = Math.toRadians(sys.getLocation().getLatitudeDegs());
                            double lon = Math.toRadians(sys.getLocation().getLongitudeDegs());
                            go.setLat(lat);
                            go.setLon(lon);
                            //go.setLat(Math.toRadians(40.642160));
                            //go.setLon(Math.toRadians(-8.749256));
                            go.setSpeedUnits(SpeedUnits.METERS_PS);
                            go.setSpeed(1.0);
                            go.setZUnits(ZUnits.DEPTH);
                            go.setZ(0);

                            // Create Plan
                            PlanSpecification ps = new PlanSpecification();
                            List<PlanManeuver> data = new ArrayList<PlanManeuver>();
                            List<PlanTransition> transitions = new ArrayList<PlanTransition>();
                            ps.setPlanId(p1.getValue() + "Sample_" + s.id);

                            PlanManeuver pm = new PlanManeuver();
                            pm.setManeuverId(p1.getValue() + "Sample_" + data.size());
                            pm.setData(go);

                            setParams.setParams(Arrays.asList(p1));
                            pm.setStartActions(Arrays.asList(setParams));

                            if(!data.isEmpty()) {
                                PlanTransition pt = new PlanTransition();
                                pt.setConditions("ManeuverIsDone");
                                pt.setSourceMan(data.get(data.size()-1).getManeuverId());
                                pt.setDestMan(pm.getManeuverId());
                                transitions.add(pt);
                            } else {
                                ps.setStartManId(pm.getManeuverId());
                            }
                            data.add(pm);

                            ps.setManeuvers(data);
                            ps.setTransitions(transitions);

                            PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(),ps);
                            plan.setVehicle(sys.getVehicle());
                            //plan.setVehicle("otter");
                            getConsole().getMission().addPlan(plan);
                            getConsole().warnMissionListeners();
                            getConsole().getMission().save(false);

                            // Send to vehicle
                            int reqId = IMCSendMessageUtils.getNextRequestId();
                            PlanControl pc = new PlanControl();
                            pc.setType(PlanControl.TYPE.REQUEST);
                            pc.setOp(PlanControl.OP.START);
                            pc.setRequestId(reqId);
                            pc.setPlanId(plan.getId());
                            pc.setInfo("Sample");
                            pc.setArg(ps);

                            ImcMsgManager.getManager().sendMessage(pc, sys.getId(), null);

                        } else {
                            NeptusLog.pub().error("Vehicle '" + vehicleForPollutionSample + "' not available: ");
                        }

                    } else {
                        //System.out.println("avoid trigger -> " + s.id);
                    }
                }
                pollutionSamples = Arrays.asList(sampleMarkers);

            } catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }

    }

    @Periodic(millisBetweenUpdates = 30000)
    public void updatePollutionInfo() {
        if (getConsole() != null) {
            // Get Pollution markers
            try {
                String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionMarkers;
                // String serverRampApiUrl = "http://localhost:9090" + apiPollutionMarkers;
                Gson gson = new Gson();
                URL url = new URL(serverRampApiUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                PollutionMarker[] markers = gson.fromJson(new InputStreamReader(con.getInputStream()), PollutionMarker[].class);

                pollutionMarkers = Arrays.asList(markers);

                NeptusLog.pub().info("Fetched " + pollutionMarkers.size() + " pollution markers.");

            } catch (Exception e) {
                NeptusLog.pub().error(e);
            }

        }
    }

    public static class PollutionMarker {
        public long id;
        public String description;
        public String status;
        public String user;
        public int radius;
        public float latitude;
        public float longitude;
        public String timestamp;

        public PollutionMarker(long pollutionId, String desc, float lat, float lng, int r, String st, String u, String time) {
            id = pollutionId;
            description = desc;
            latitude = lat;
            longitude = lng;
            radius = r;
            status = st;
            user = u;
            timestamp = time;
        }

        private void paintCircle(Point2D pt2d, Color color, float radius, String msg, Boolean display, Graphics2D g) {
            g.setPaint(new GradientPaint((float) pt2d.getX() - radius, (float) pt2d.getY(), color,
                    (float) pt2d.getX() + radius, (float) pt2d.getY()+radius, color.darker().darker().darker()));
            g.fill(new Ellipse2D.Double(pt2d.getX() - radius, pt2d.getY() - radius, radius*2, radius*2));
            g.setStroke(new BasicStroke(0.5f));
            g.setColor(color);
            if(display) {
                g.drawString(msg, (int) pt2d.getX() + 6, (int) pt2d.getY() - 3);
            }
            g.draw(new Ellipse2D.Double(pt2d.getX() - radius, pt2d.getY() - radius, radius*2, radius*2));
        }
    }

    public static class Obstacle {
        public long id;
        public String description, timestamp, user;
        public Vector<double[]> obstaclePoints;

        public Obstacle(long obstacleId, String desc, String time, String u, JsonArray wpts) {
            id = obstacleId;
            description = desc;
            timestamp = time;
            user = u;
            obstaclePoints = new Vector<double[]>();
            for (JsonValue p : wpts) {
                JsonArray point = p.asArray();
                float lat, lon;
                lat = point.get(0).asFloat();
                lon = point.get(1).asFloat();
                double[] position = {lat, lon};
                obstaclePoints.add(position);
            }
        }

        private void paintPolygon(Vector<Point2D> pts2d, String msg, Boolean display, Graphics2D g) {
            // Marker to display message
            double maxValue = 0;
            int maxIndex = 0;
            for (int n = 0; n < pts2d.size() - 1; n++) {
                if (pts2d.get(n).getX() > maxValue) {
                    maxValue = pts2d.get(n).getX();
                    maxIndex = n;
                }
            }

            List<Integer> list_x = new ArrayList<>();
            List<Integer> list_y = new ArrayList<>();
            for (Point2D p : pts2d) {
                list_x.add((int) p.getX());
                list_y.add((int) p.getY());
            }
            int[] xCoord = list_x.stream().mapToInt(i -> i).toArray();
            int[] yCoord = list_y.stream().mapToInt(i -> i).toArray();

            g.setColor(ColorUtils.setTransparencyToColor(Color.black, 140));
            Polygon polygon = new Polygon(xCoord, yCoord, pts2d.size());
            g.fillPolygon(polygon);
            if (display) {
                g.drawString(msg, (int) pts2d.get(maxIndex).getX() + 6, (int) pts2d.get(maxIndex).getY() - 3);
            }
        }
    }

    public static class Sample {
        public long id;
        public String status, timestamp;
        public double latitude, longitude;

        public Sample(long sampleId, String sta, String time, double lat, double lon) {
            id = sampleId;
            status = sta;
            timestamp = time;
            latitude = lat;
            longitude = lon;
        }

        private void paintIcon(Point2D pt2d, String sampleType, float zoom, Graphics2D g) {
            Image img = null;
            String msg = "";
            int size = (int) (zoom * 15);
            if(sampleType.equals("CLEAN")) {
                img = ImageUtils.getImage("images/droplet_clean.png");
                msg = "Clean sample";
            }
            else {
                img = ImageUtils.getImage("images/droplet_dirty.png");
                msg = "Dirty sample";
            }
            g.drawImage(img, (int) (pt2d.getX() - (size/2)), (int) (pt2d.getY() - (size/2)), size, size,null);
            g.setColor(Color.black);
            g.drawString(msg, (int) pt2d.getX() + (size/4), (int) pt2d.getY() - (size/4));
        }
    }
}