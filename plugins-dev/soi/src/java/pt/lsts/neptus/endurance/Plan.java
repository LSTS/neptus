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
 * Author: zepinto
 * 09/01/2018
 */
package pt.lsts.neptus.endurance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import pt.lsts.imc.Goto;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.ScheduledGoto;
import pt.lsts.imc.SoiPlan;
import pt.lsts.imc.SoiWaypoint;
import pt.lsts.neptus.soi.SoiUtils;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.CRC16Util;
import pt.lsts.util.PlanUtilities;

public class Plan {

    private final String planId;
    private boolean cyclic = false;
    private ArrayList<Waypoint> waypoints = new ArrayList<>();

    /**
     * @return the planId
     */
    public final String getPlanId() {
        return planId;
    }

    /**
     * @return the cyclic
     */
    public final boolean isCyclic() {
        return cyclic;
    }

    /**
     * @param cyclic the cyclic to set
     */
    public final void setCyclic(boolean cyclic) {
        this.cyclic = cyclic;
    }

    public Plan(String id) {
        this.planId = id;
    }

    public static Plan parse(String spec) throws Exception {
        IMCMessage msg = null;
        try {
            msg = FormatConversion.fromJson(spec);
        }
        catch (Exception e) {
        }

        if (msg == null) {
            try {
                JsonObject json = Json.parse(spec).asObject();
                Plan p = new Plan(json.getString("id", ""));
                JsonArray arr = json.get("waypoints").asArray();

                for (int i = 0; i < arr.size(); i++) {
                    JsonObject wpt = arr.get(i).asObject();
                    float lat = wpt.getFloat("latitude", 0);
                    float lon = wpt.getFloat("longitude", 0);
                    Waypoint waypoint = new Waypoint(i, lat, lon);
                    waypoint.setDuration(wpt.getInt("duration", 0));
                    double time = wpt.getDouble("eta", 0);
                    if (time != 0)
                        waypoint.setArrivalTime(new Date((long) (time * 1000)));
                    p.addWaypoint(waypoint);
                }
                return p;
            }
            catch (Exception e) {
                throw new Exception("Unrecognized plan format.", e);
            }

        }
        if (msg instanceof SoiPlan) {
            return parse((SoiPlan) msg);
        }
        else if (msg instanceof PlanSpecification) {
            return parse((PlanSpecification) msg);
        }
        else {
            throw new Exception("Message not recognized: " + msg.getAbbrev());
        }
    }

    public static Plan parse(PlanSpecification spec) {
        Plan plan = new Plan(spec.getPlanId());
        int id = 1;
        LocationType lastLocation = null;

        for (Maneuver m : SoiUtils.getFirstManeuverSequence(spec)) {
            LocationType thisLocation = Waypoint.locationOf(m);
            try {

                if (lastLocation != null && lastLocation.getDistanceInMeters(thisLocation) > 40_000) {
                    double times = Math.ceil(lastLocation.getDistanceInMeters(thisLocation) / 40_000);
                    double intermediate = lastLocation.getDistanceInMeters(thisLocation) / times;
                    double ang = lastLocation.getXYAngle(thisLocation);
                    for (int i = 0; i <= times; i++) {
                        LocationType loc = new LocationType(lastLocation);
                        loc.translatePosition(Math.cos(ang) * intermediate * i, Math.sin(ang) * intermediate * i, 0);
                        loc.convertToAbsoluteLatLonDepth();
                        plan.addWaypoint(
                                new Waypoint(id++, (float) loc.getLatitudeDegs(), (float) loc.getLongitudeDegs()));
                    }
                }
                else
                    plan.addWaypoint(new Waypoint(id++, m));
                lastLocation = Waypoint.locationOf(m);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (PlanUtilities.isCyclic(spec))
            plan.cyclic = true;

        return plan;
    }

    public static Plan parse(SoiPlan spec) {
        // empty plan
        if (spec == null) {
            return null;
        }
        
        Plan plan = new Plan("soi_" + spec.getPlanId());
        int id = 1;
        for (SoiWaypoint wpt : spec.getWaypoints()) {
            Waypoint soiWpt = new Waypoint(id++, (float) wpt.getLat(), (float) wpt.getLon());
            soiWpt.setDuration(wpt.getDuration());
            if (wpt.getEta() > 0)
                soiWpt.setArrivalTime(new Date(1000 * wpt.getEta()));
            plan.addWaypoint(soiWpt);
        }
        return plan;
    }

    public SoiPlan asImc() {
        SoiPlan plan = new SoiPlan();
        if (waypoints != null) {
            Vector<SoiWaypoint> wps = new Vector<>(waypoints.size());
            for (Waypoint wpt : waypoints) {
                SoiWaypoint waypoint = new SoiWaypoint();
                if (wpt.getArrivalTime() != null)
                    waypoint.setEta(wpt.getArrivalTime().getTime() / 1000);
                else
                    waypoint.setEta(0);
                waypoint.setLat(wpt.getLatitude());
                waypoint.setLon(wpt.getLongitude());
                waypoint.setDuration(wpt.getDuration());

                wps.add(waypoint);
            }
            plan.setWaypoints(wps);
        }

        ByteBuffer destination = ByteBuffer.allocate(plan.getPayloadSize());
        int dataLength = plan.serializePayload(destination, 0);
        plan.setPlanId(CRC16Util.crc16(destination, 2, dataLength - 2));
        return plan;
    }

    public int checksum() {
        try {
            SoiPlan plan = asImc();
            // ByteBuffer destination = ByteBuffer.allocate(plan.getPayloadSize() * 4);
            ByteArrayOutputStream bfos = new ByteArrayOutputStream(plan.getPayloadSize());
            IMCOutputStream out = new IMCOutputStream(bfos);
            int dataLength = IMCDefinition.getInstance().serializeFields(plan, out);
            return CRC16Util.crc16(bfos.toByteArray(), 2, dataLength - 2);
        }
        catch (IOException e) {
            e.printStackTrace();
        } // plan.serializePayload(destination, 0);
        return 0;
    }

    public void addWaypoint(Waypoint waypoint) {
        synchronized (waypoints) {
            waypoints.add(waypoint);
        }
    }

    public Waypoint waypoint(int index) {
        if (index < 0 || index >= waypoints.size())
            return null;
        return waypoints.get(index);
    }

    public ArrayList<Waypoint> waypoints() {
        ArrayList<Waypoint> ret = new ArrayList<>();
        synchronized (waypoints) {
            for (Waypoint wpt : waypoints)
                ret.add(wpt.clone());
        }
        return ret;
    }

    public void remove(int index) {
        synchronized (waypoints) {
            waypoints.remove(index);
        }
    }

    public void scheduleWaypoints(long startTime, double lat, double lon, double speed) {
        long curTime = startTime;
        synchronized (waypoints) {
            for (Waypoint waypoint : waypoints) {
                double[] offsets = CoordinateUtil.WGS84displacement(lat, lon, 0,
                        Float.valueOf(waypoint.getLatitude()).doubleValue(),
                        Float.valueOf(waypoint.getLongitude()).doubleValue(), 0);
                double distance = Math.hypot(offsets[0], offsets[1]);
                double timeToReach = distance / speed;
                curTime += (long) (1000.0 * (timeToReach + waypoint.getDuration()));
                waypoint.setArrivalTime(new Date(curTime));
                lat = waypoint.getLatitude();
                lon = waypoint.getLongitude();
            }
        }
    }

    public void scheduleWaypoints(long startTime, double speed) {
        if (waypoints.isEmpty())
            return;

        Waypoint start = waypoints.get(0);
        scheduleWaypoints(startTime, start.getLatitude(), start.getLongitude(), speed);
    }

    public String toString() {
        JsonObject pp = new JsonObject();
        pp.add("id", getPlanId());
        JsonArray waypoints = new JsonArray();
        for (Waypoint wpt : waypoints()) {
            JsonObject waypoint = new JsonObject();
            waypoint.add("latitude", wpt.getLatitude());
            waypoint.add("longitude", wpt.getLongitude());
            if (wpt.getDuration() != 0)
                waypoint.add("duration", wpt.getDuration());
            if (wpt.getArrivalTime() != null)
                waypoint.add("eta", wpt.getArrivalTime().getTime() / 1000);
            waypoints.add(waypoint);
        }
        pp.add("waypoints", waypoints);
        return pp.toString();
    }

    public void remove(Waypoint waypoint) {
        remove(waypoint.getId());
    }

    
    public Date planEta() {
        if (waypoints == null || waypoints.size() == 0)
            return null;
        return  waypoints.get(waypoints.size()-1).getArrivalTime();
    }
    
    public boolean scheduledInTheFuture() {
        long present = System.currentTimeMillis();
        if (waypoints == null)
            return false;

        synchronized (waypoints) {
            for (Waypoint wpt : waypoints) {
                if (wpt.getArrivalTime() == null || wpt.getArrivalTime().getTime() < present)
                    return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        Plan plan = new Plan("test");
        ScheduledGoto goto1 = new ScheduledGoto();
        goto1.setLat(Math.toRadians(41));
        goto1.setLon(Math.toRadians(-8));
        goto1.setArrivalTime(new Date().getTime() / 1000.0 + 3600);

        ScheduledGoto goto2 = new ScheduledGoto();
        goto2.setLat(Math.toRadians(41.5));
        goto2.setLon(Math.toRadians(-8.5));
        goto2.setArrivalTime(new Date().getTime() / 1000.0 + 1800);

        Goto goto3 = new Goto();
        goto3.setLat(Math.toRadians(41.2));
        goto3.setLon(Math.toRadians(-8.2));

        Goto goto4 = new Goto();
        goto4.setLat(Math.toRadians(41.4));
        goto4.setLon(Math.toRadians(-8.4));

        plan.addWaypoint(new Waypoint(1, goto1));
        plan.addWaypoint(new Waypoint(2, goto2));
        plan.addWaypoint(new Waypoint(3, goto3));
        plan.addWaypoint(new Waypoint(4, goto4));

        System.out.println(plan.toString());

        Plan plan2 = Plan.parse(plan.toString());
        System.out.println(plan2.toString());
    }
}
