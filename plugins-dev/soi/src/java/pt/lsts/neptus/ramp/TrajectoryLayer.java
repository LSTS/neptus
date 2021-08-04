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
 * Author: Sheila
 * Aug 4,2021
 */

package pt.lsts.neptus.ramp;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;


import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

import  pt.lsts.neptus.endurance.Waypoint;

@PluginDescription(description = "Adds trajectories and their calculated duration from JSON to the map console",name = "Optimized Trajectories")
public class TrajectoryLayer extends ConsoleLayer {

    @NeptusProperty(description = "Path to JSON file with optimized trajectories info", name = "JSON Source")
    public String source  = "conf/trajectories/trajectory_raveiro.json"; //TODO receive from Ripples

    @NeptusProperty(description = "Color Map Max Limit", name = "max limit", units = "m/s")
    public float max = 1.5f;

    @NeptusProperty(description = "Color Map Min Limit", name = "min limit", units = "m/s")
    public float min = 0.1f;

    @NeptusProperty(description = "Generate timed plan from trajectory", name = "timed plan")
    public boolean timed = false;

    

    protected double totalTime = 0.0;

    protected List<Trajectory> trajectories = Collections.synchronizedList(new ArrayList<Trajectory>());


    @NeptusMenuItem("Tools>RaMP>Generate Plans")
    public void generatePlan() {
        //TODO
        if(timed){ //Timed GO-TO's

        }
        else{ //Regular Go-To's

        }
    }

    @Override
    public  boolean userControlsOpacity(){
        return false;

    }

    @Override
    public  void initLayer(){
        File input = new File(source);
        if(input.exists() && input.isFile()){
            try{
                JsonObject json = Json.parse(new FileReader(input)).asObject();
                JsonArray trajs = json.get("trajectories").asArray();
                totalTime = json.get("total_duration").asDouble();
                trajs.values().forEach(entry -> addTrajectory(entry.asObject()));
            }
            catch(Exception e){
                NeptusLog.pub().error(I18n.text("Error parsing trajectories file:  "+input.getAbsolutePath()),e);
            }

        }
        else{
            NeptusLog.pub().error(I18n.text("Error opening trajectories file:  "+input.getAbsolutePath()));
        }

    }

    @Override
    public  void cleanLayer(){

    }

    @Override
    public void paint(Graphics2D g0, StateRenderer2D renderer){
        Color c = VehiclesHolder.getVehicleById(super.getConsole().getMainSystem()).getIconColor();
        if(!trajectories.isEmpty()){
            synchronized(trajectories){
            Graphics2D g = (Graphics2D) g0.create();
                for(Trajectory traj: trajectories) {
                    for (Waypoint wpt: traj.waypoints) {
                        LocationType loc = new LocationType(wpt.getLatitude(), wpt.getLongitude());
                        Point2D pt2d = renderer.getScreenPosition(loc);
                        wpt.paint(g, wpt, pt2d, c, 4, true);
                    }
                }
                g.dispose();
            }
        }
    }

    protected void addTrajectory(JsonObject element){
        String id = element.get("id").asString();
        long duration = Float.valueOf(element.get("duration").asFloat()).longValue();
        Trajectory traj = new Trajectory(id, duration, element.get("waypoints").asArray());
        trajectories.add(traj);
    }

    public class Trajectory {
        public final String id;
        public final long duration;
        public Vector<Waypoint> waypoints;

        public Trajectory(String name, long t, JsonArray wpts){
            id = name;
            duration = t;
            waypoints = new Vector<Waypoint>();
            wpts.values().forEach(wp -> addWaypoint(wp.asArray()) );
        }

        protected void addWaypoint(JsonArray data){
            float lat,lon,t;
            t   = data.get(0).asFloat();
            long arrival = Float.valueOf(t).longValue(); //UNIX Timestamp in seconds
            lat = data.get(1).asFloat();
            lon = data.get(2).asFloat();
            Waypoint wp = new Waypoint(waypoints.size(), lat,lon);
            wp.setArrivalTime(new Date(1000 * arrival));
            //wp.setDuration();
            waypoints.add(wp);
        }

    }
    
}