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
import java.nio.file.FileSystems;
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

import pt.lsts.imc.Goto;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.PlanTransition;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import  pt.lsts.neptus.endurance.Waypoint;

@PluginDescription(description = "Adds trajectories and their calculated duration from JSON to the map console",name = "Optimized Trajectories")
public class TrajectoryLayer extends ConsoleLayer {

    @NeptusProperty(description = "Path to JSON file with optimized trajectories info", name = "JSON Source")
    public String source  = "conf/trajectories/trajectory_raveiro1.json"; //TODO receive from Ripples
    // JSON example in https://drive.google.com/file/d/1ZhYdk7CuoHPo83OfZyk8fFF3oPCTM48t/view?usp=sharing

    @NeptusProperty(description = "Generated plan speed", name = "Speed", units = "m/s")
    public float speed = 1.0f;

    @NeptusProperty(description = "Generated plan depth or altitude if signal is inverted", name = "Depth", units = "m")
    public float depth = 0.0f;

    private double totalTime;

    protected List<Trajectory> trajectories;

    private File input;

    public TrajectoryLayer(){
        init();
    }

    public void init(){
        input  = new File (source);
        trajectories = Collections.synchronizedList(new ArrayList<Trajectory>());
        totalTime = 0.0;
    }


    @NeptusMenuItem("Tools>Trajectories>Generate Plans")
    public void generatePlan() {
        int index = 0;
        for(Trajectory traj: trajectories){
            PlanSpecification ps = new PlanSpecification();
            List<PlanManeuver> data = new ArrayList<PlanManeuver>();
            List<PlanTransition> transitions = new ArrayList<PlanTransition>();
            ps.setPlanId("trajectory_" + index);
            for(Waypoint wp: traj.waypoints){
                Goto go = new Goto();
                go.setLat(Math.toRadians(wp.getLatitude()));
                go.setLon(Math.toRadians(wp.getLongitude()));
                go.setSpeedUnits(SpeedUnits.METERS_PS);
                go.setSpeed(speed);
                if(depth >= 0.0)
                    go.setZUnits(ZUnits.DEPTH);
                else
                    go.setZUnits(ZUnits.ALTITUDE);
                go.setZ(Math.abs(depth));
                PlanManeuver pm = new PlanManeuver();
                pm.setManeuverId("wp_"+ data.size());
                pm.setData(go);
                if(!data.isEmpty()){
                    PlanTransition pt = new PlanTransition();
                    pt.setConditions("ManeuverIsDone"); 
                    pt.setSourceMan(data.get(data.size()-1).getManeuverId());
                    pt.setDestMan(pm.getManeuverId());
                    transitions.add(pt);
                }
                else{
                    ps.setStartManId(pm.getManeuverId());
                }
                data.add(pm);
                
                
            }
            ps.setManeuvers(data);
            ps.setTransitions(transitions);
            PlanType plan = IMCUtils.parsePlanSpecification(this.getConsole().getMission(),ps);
            plan.setVehicle(getConsole().getMainSystem());
            this.getConsole().getMission().addPlan(plan);
            this.getConsole().warnMissionListeners(); 
            this.getConsole().getMission().save(false);
            index++;
        }       
    }

    @Override
    public  boolean userControlsOpacity(){
        return false;

    }

    @Override
    public  void initLayer(){
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
        String[] path = source.split(FileSystems.getDefault().getSeparator());
        if(input != null)
            if(!path[path.length-1].equals(input.getName())){
                trajectories.clear();
                init();
                initLayer();
            }
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
            // verify is the same coordinate that previous WP - avoid duplicated
            if(!waypoints.isEmpty()){
                Waypoint waypoint = waypoints.get(waypoints.size()-1);
                if(waypoint.compareTo(wp) == 0 && waypoint.getLatitude() == lat && waypoint.getLongitude() == lon)
                    return;
                else
                    waypoints.add(wp); 

            }
            else
                waypoints.add(wp); 
        }

    }
    
}