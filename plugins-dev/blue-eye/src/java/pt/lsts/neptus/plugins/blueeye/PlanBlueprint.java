/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Nov 15, 2013
 */
package pt.lsts.neptus.plugins.blueeye;

import java.util.Collection;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
public class PlanBlueprint {

    private String planId;
    private String systemId;
    private Vector<Waypoint> trajectory = new Vector<>();

    public PlanBlueprint(int imcid, String planId) {
        this.systemId = ImcSystemsHolder.lookupSystem(new ImcId16(imcid)).getName();
        this.planId = planId;
    }

    public void addPoint(double latDegrees, double lonDegrees, double depthMeters) {
        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeDegs(latDegrees);
        loc.setLongitudeDegs(lonDegrees);
        if (depthMeters >= 0) {
            loc.setZ(depthMeters);
            loc.setZUnits(Z_UNITS.DEPTH);
        }
        else {
            loc.setZ(-depthMeters);
            loc.setZUnits(Z_UNITS.ALTITUDE);
        }
        trajectory.add(new Waypoint(loc));
    }

    public PlanType generate() {
        PlanType plan = new PlanType(null);
        plan.setId(planId);
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(systemId);
        plan.setVehicle(sys.getName());

        Vector<Maneuver> maneuvers = new Vector<>();
        FollowPath current = null;
        ManeuverLocation lastLoc = null;

        int count = 1;
        for (Waypoint wpt : trajectory) {
            lastLoc = wpt.coordinates;
            if (current == null || wpt.coordinates.getZUnits() != current.getStartLocation().getZUnits()) {
                current = (FollowPath) plan.getVehicleType().getManeuverFactory().getManeuver("FollowPath");
                current.setId("" + count);
                count++;
                Vector<double[]> points = new Vector<>();
                points.add(new double[] { 0, 0, 0, 0 });
                current.setOffsets(points);
                maneuvers.add(current);
                current.setManeuverLocation(wpt.coordinates);
            }
            else {
                double[] offsets = wpt.coordinates.getOffsetFrom(current.getManeuverLocation());
                Vector<double[]> points = new Vector<>();
                points.addAll(current.getPathPoints());
                double[] point = new double[4];
                point[0] = offsets[0];
                point[1] = offsets[1];
                point[2] = wpt.coordinates.getZ() - current.getManeuverLocation().getZ();

                points.add(point);
                current.setOffsets(points);
            }
        }
        StationKeeping sk = (StationKeeping) plan.getVehicleType().getManeuverFactory().getManeuver("StationKeeping");
        sk.setId("" + count);
        ManeuverLocation loc = lastLoc.clone();
        loc.setZ(0);
        loc.setZUnits(Z_UNITS.DEPTH);
        sk.setManeuverLocation(loc);
        maneuvers.add(sk);

        String lastMan = null;
        for (Maneuver m : maneuvers) {
            plan.getGraph().addManeuver(m);
            if (lastMan != null)
                plan.getGraph().addTransition(lastMan, m.getId(), "true");
            lastMan = m.getId();
        }
        return plan;
    }

    public void addPoint(Waypoint wpt) {
        trajectory.add(wpt);
    }

    public void addPoints(Collection<Waypoint> wpts) {
        trajectory.addAll(wpts);
    }

    /**
     * @return the planId
     */
    public String getPlanId() {
        return planId;
    }

    /**
     * @param planId the planId to set
     */
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    class Waypoint {
        private ManeuverLocation coordinates;

        public Waypoint(ManeuverLocation loc) {
            this.coordinates = loc.clone();
        }
    }

    public static void main(String[] args) {
        NeptusLog.init();
        int imcid = ImcSystemsHolder.getSystemWithName("lauv-xtreme-2").getId().intValue();
        PlanBlueprint plan = new PlanBlueprint(imcid, "test1");
        plan.addPoint(41.18547713427995, -8.70566725730896, 3);
        plan.addPoint(41.18456472894702, -8.704508543014526, 3);
        plan.addPoint(41.18441131441246, -8.704723119735718, 3);
        plan.addPoint(41.18518645785469, -8.705699443817139, 3);
        plan.addPoint(41.185081491050695, -8.705892562866211, -2);
        plan.addPoint(41.18432249530712, -8.704948425292969, -2);
        plan.addPoint(41.18417715469308, -8.705259561538696, -4);
        plan.addPoint(41.1850330447767, -8.706353902816772, 3);
        plan.addPoint(41.18548117144345, -8.705696761608124, 3);

        IMCMessage msg = plan.generate().asIMCPlan();
        GuiUtils.testFrame(new JScrollPane(new JLabel(IMCUtils.getAsHtml(msg))));
    }
}
