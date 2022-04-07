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
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.awt.geom.Point2D;

import com.eclipsesource.json.JsonValue;
import com.google.gson.Gson;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author RicardoSantos
 */
@PluginDescription(name = "Optimized Trajectories", description = "Adds trajectories and their calculated duration from JSON to the map console", icon="images/rampLogo.png")
public class TrajectoryLayer extends ConsoleLayer {

    @NeptusProperty(name = "Pollution markers endpoint", description = "Endpoint to GET pollution markers from Ripples")
    public String apiPollutionMarkers = "/pollution";

    @NeptusProperty(name = "Pollution obstacles endpoint", description = "Endpoint to GET obstacles from Ripples")
    public String apiPollutionObstacles = "/pollution/obstacles";

    protected List<PollutionMarker> pollutionMarkers;

    protected List<Obstacle> pollutionObstacles;

    public TrajectoryLayer(){
        init();
    }

    public void init(){
        pollutionMarkers = Collections.synchronizedList(new ArrayList<PollutionMarker>());
        pollutionObstacles = Collections.synchronizedList(new ArrayList<Obstacle>());
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
            Gson gson = new Gson();
            URL url = new URL(serverRampApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            PollutionMarker[] markers = gson.fromJson(new InputStreamReader(con.getInputStream()), PollutionMarker[].class);

            pollutionMarkers = Arrays.asList(markers);

        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        // Get Pollution obstacles
        try {
            String serverRampApiUrl = GeneralPreferences.ripplesUrl + apiPollutionObstacles;
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
            }
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
                            color = new Color(1f,1f,1f,.5f );
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
    
}