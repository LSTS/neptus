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
 * Author: zp
 * Oct 19, 2015
 */
package org.necsave;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Future;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;

import info.necsave.msgs.AbortMission;
import info.necsave.msgs.AbortMission.TYPE;
import info.necsave.msgs.ActionIdle;
import info.necsave.msgs.ActionIdle.IDLE_AT_HOME;
import info.necsave.msgs.Area;
import info.necsave.msgs.BehaviorPatternFormation;
import info.necsave.msgs.BehaviorScanArea;
import info.necsave.msgs.Capabilities;
import info.necsave.msgs.CapabilityPlanMission;
import info.necsave.msgs.CapabilityScanArea;
import info.necsave.msgs.Contact;
import info.necsave.msgs.ContactList;
import info.necsave.msgs.Coordinate;
import info.necsave.msgs.Coordinate.TEMPORAL;
import info.necsave.msgs.Formation;
import info.necsave.msgs.Header.MEDIUM;
import info.necsave.msgs.Kinematics;
import info.necsave.msgs.MeshState;
import info.necsave.msgs.MissionArea;
import info.necsave.msgs.MissionCompleted;
import info.necsave.msgs.MissionGoal;
import info.necsave.msgs.MissionGoal.GOAL_TYPE;
import info.necsave.msgs.MissionReadyToStart;
import info.necsave.msgs.Plan;
import info.necsave.msgs.PlatformExit;
import info.necsave.msgs.PlatformFollower;
import info.necsave.msgs.PlatformInfo;
import info.necsave.msgs.PlatformPlan;
import info.necsave.msgs.PlatformPlanProgress;
import info.necsave.msgs.PlatformState;
import info.necsave.msgs.Resurface;
import info.necsave.msgs.SetHomeLocation;
import info.necsave.msgs.SweepPath;
import info.necsave.proto.Message;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.ManeuverWithSpeed;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * This class will show the states of NECSAVE platforms and allows interactions with them
 * 
 * @author zp
 * 
 */
@PluginDescription(name = "NECSAVE UI", icon="org/necsave/necsave.png")
public class NecsaveUI extends ConsoleLayer {

    @NeptusProperty(description="Select if the commands should be sent using TCP communications")
    public boolean sendCommandsReliably = true;

    @NeptusProperty(description="RFU - Safe Range in meters between vehicles in a formation mission")
    public double safeDistance = 25;

    @NeptusProperty(description = "Paint formation lines between vehicles on a formation")
    public boolean paintFormation = true;

    private NecsaveTransport transport = null;
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PlatformPlanProgress> planProgresses = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PlatformState> platfStates = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PlatformInfo> platfInfos = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Kinematics> platfKinematics = new LinkedHashMap<>();
    private LinkedHashMap<String, LocationType> contacts = new LinkedHashMap<>();
    private LinkedHashMap<Integer, Color> platformColors = new LinkedHashMap<>();
    private Vector<PlatformFollower> followers = new Vector<>();
    private Plan plan = null;
    private ParallelepipedElement elem = null;
    private ParallelepipedElement area;
    private LocationType corner = null; 
    private double width, height;
    private ConsoleInteraction interaction;
    private MissionArea sentArea = null;
    
    @Override
    public void initLayer() {
        
        try {
            transport = new NecsaveTransport(getConsole());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        
       setupInteraction();
       getConsole().addInteraction(interaction);
       area = new ParallelepipedElement();
    }

    private void setupInteraction() {
        interaction = new ConsoleInteraction() {

            @Override
            public void initInteraction() {

            }

            @Override
            public void cleanInteraction() {

            }
            
            @Override
            public void mouseClicked(MouseEvent event, StateRenderer2D source) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    for (Entry<Integer, String> plat : platformNames.entrySet()) {

                        JMenu cmdMenu = new JMenu(I18n.text("Command ")+ plat.getValue());
                        popup.add(cmdMenu);

                        cmdMenu.add(new JMenuItem(I18n.text("Set Home Position"))).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                LocationType loc = source.getRealWorldLocation(event.getPoint());
                                Coordinate homeLoc = new Coordinate();
                                homeLoc.setLatitude(loc.getLatitudeRads());
                                homeLoc.setLongitude(loc.getLongitudeRads());

                                SetHomeLocation home = new SetHomeLocation(homeLoc);

                                try {
                                    transport.sendMessage(home, plat.getKey());
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(getConsole(), ex);
                                    ex.printStackTrace();
                                }

                            }
                        });
                        cmdMenu.add(new JMenuItem(I18n.text("Go to Home Position"))).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {

                                ActionIdle goIdle = new ActionIdle();
                                goIdle.setIdleAtHome(ActionIdle.IDLE_AT_HOME.TRUE);

                                try {
                                    transport.sendMessage(goIdle, plat.getKey());
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(getConsole(), ex);
                                    ex.printStackTrace();
                                }

                            }
                        });
                        cmdMenu.add(new JMenuItem(I18n.text("Idle here"))).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                LocationType loc = source.getRealWorldLocation(event.getPoint());
                                Coordinate hereLoc = new Coordinate();
                                hereLoc.setLatitude(loc.getLatitudeRads());
                                hereLoc.setLongitude(loc.getLongitudeRads());

                                ActionIdle goIdle = new ActionIdle();
                                goIdle.setIdleAtHome(ActionIdle.IDLE_AT_HOME.FALSE);
                                goIdle.setWaypoint(hereLoc);

                                try {
                                    transport.sendMessage(goIdle, plat.getKey());
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(getConsole(), ex);
                                    ex.printStackTrace();
                                }

                            }
                        });

                        cmdMenu.add(new JMenuItem(I18n.text("Quit Platform"))).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {

                                PlatformExit exit = new PlatformExit();

                                try {
                                    transport.sendMessage(exit, plat.getKey());
                                }
                                catch (Exception ex) {
                                    GuiUtils.errorMessage(getConsole(), ex);
                                    ex.printStackTrace();
                                }

                            }
                        });
                    }
                    popup.show(source, event.getX(), event.getY());
                }
            }
        };
    }

    @NeptusMenuItem("Advanced>NECSAVE>Set Mission")
    public void setMission() {
        MapGroup mg = MapGroup.getMapGroupInstance(getConsole().getMission());

        ArrayList<String> goalsList = new ArrayList<>(Arrays.asList(getNames(GOAL_TYPE.class)));
        Collections.sort(goalsList);
        
        Object goal_ret = JOptionPane.showInputDialog(getConsole(), I18n.text("Select goal"),
                I18n.text("Select goal"), JOptionPane.QUESTION_MESSAGE, null,
                goalsList.toArray(), goalsList.toArray()[1]);
        if (goal_ret == null)
            return;

        GOAL_TYPE goal = GOAL_TYPE.valueOf((String) goal_ret);
        MissionGoal m_goal = new MissionGoal(goal);

        try {
            sendMessage(m_goal);
        }
        catch (Exception ex) {
            GuiUtils.errorMessage(getConsole(), ex);
            ex.printStackTrace();
        }

        if (goal.equals(GOAL_TYPE.ENV_ASSESSMENT)) {

            Vector<ParallelepipedElement> pps = mg.getAllObjectsOfType(ParallelepipedElement.class);

            if (pps.isEmpty()) {
                GuiUtils.errorMessage(getConsole(), I18n.text("Set mission area"),
                        I18n.text("Please add at least one rectangle to the map"));
                return;
            }

            Object ret = JOptionPane.showInputDialog(getConsole(), I18n.text("Select area"),
                    I18n.text("Select area"), JOptionPane.QUESTION_MESSAGE, null,
                    pps.toArray(new ParallelepipedElement[0]), pps.iterator().next());
            if (ret == null)
                return;

            elem = (ParallelepipedElement) ret;

            double a = elem.getYawRad();
            width = elem.getWidth();
            height = elem.getLength();

            corner = new LocationType(elem.getCenterLocation());
            corner.setOffsetDistance(-height / 2);
            corner.setAzimuth(Math.toDegrees(a));
            corner.convertToAbsoluteLatLonDepth();
            corner.setOffsetDistance(-width / 2);
            corner.setAzimuth(Math.toDegrees(a + Math.PI / 2));
            corner.convertToAbsoluteLatLonDepth();
            corner.convertToAbsoluteLatLonDepth(); //TODO he

            NecsaveUI.this.width = elem.getWidth();
            NecsaveUI.this.height = elem.getLength();
            Area area = new Area();
            area.setLatitude(corner.getLatitudeRads());
            area.setLongitude(corner.getLongitudeRads());
            area.setBearing(a);
            area.setWidth(elem.getWidth());
            area.setLength(elem.getLength());

            try {
                this.sentArea = new MissionArea(area);
                sendMessage(sentArea);                    
            }
            catch (Exception ex) {
                GuiUtils.errorMessage(getConsole(), ex);
                ex.printStackTrace();
            }
        } 
        else if (goal.equals(GOAL_TYPE.MINE_SWEEP) || goal.equals(GOAL_TYPE.AREA_SWEEP)) {
            
            Formation formation = new Formation();
            formation.setSafeDistance(safeDistance);

            ArrayList<PlatformFollower> followers_list = new ArrayList<>();

            if (platformNames.isEmpty()) {
                JOptionPane.showMessageDialog (null, "There are no platforms available.", "No platforms", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String leader = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Select formation leader"),
                    I18n.text("Formation leader"), JOptionPane.QUESTION_MESSAGE, null,
                    platformNames.values().toArray(), platformNames.values().iterator().next());
            if (leader == null)
                return;

            String[] typesOfFormation = new String[]{"Horizontal Line", "Vertical Line", "Triangle"};
            String formType = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Select formation type"),
                    I18n.text("Formation type"), JOptionPane.QUESTION_MESSAGE, null,
                    typesOfFormation, typesOfFormation[0]);
            if (formType == null)
                return;

            if (platformNames.containsValue(leader)) {
                for (final Integer id : platformNames.keySet()) {
                    if (platformNames.get(id).equals(leader)) {
                        formation.setLeaderPlatformId(id);
                        break;
                    }
                }
            }

            HashMap<Integer, String> platfAvailable = new HashMap<>();

            for (Entry<Integer, String> plat : platformNames.entrySet()) {
                if (!plat.getValue().equals(leader))
                    platfAvailable.put(plat.getKey(), plat.getValue());
            }

            String follower1 = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Select follower 1:"),
                    I18n.text("Follower 1"), JOptionPane.QUESTION_MESSAGE, null,
                    platfAvailable.values().toArray(), platfAvailable.values().iterator().next());
            
            platfAvailable.remove(getId(follower1));
            
            String follower2 = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Select follower 2:"),
                    I18n.text("Follower 2"), JOptionPane.QUESTION_MESSAGE, null,
                    platfAvailable.values().toArray(), platfAvailable.values().iterator().next());
            
            PlatformFollower pfLeader = new PlatformFollower();
            pfLeader.setPayload(info.necsave.msgs.PlatformFollower.PAYLOAD.SIDESCAN);
            pfLeader.setFollowerPlatformId(getId(leader));
            pfLeader.setRadius(0);
            pfLeader.setBearing(0);
            
            PlatformFollower pf1 = new PlatformFollower();
            pf1.setPayload(info.necsave.msgs.PlatformFollower.PAYLOAD.SIDESCAN);
            pf1.setFollowerPlatformId(getId(follower1));

            PlatformFollower pf2 = new PlatformFollower();
            pf2.setPayload(info.necsave.msgs.PlatformFollower.PAYLOAD.SIDESCAN);
            pf2.setFollowerPlatformId(getId(follower2));

            followers_list.add(pf1);
            followers_list.add(pf2);
            
            if (followers_list.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "No platforms were choosen to be followers.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            else {
                formation.setFollowersList(followers_list);
                double dist = safeDistance;
                if (formType.equals("Horizontal Line")) {
                    for (PlatformFollower f : followers_list) {
                        f.setBearing(Math.toRadians(270));
                        f.setRadius(dist);
                        dist += dist;

                    }
                } 
                else if (formType.equals("Vertical Line")) {
                    for (PlatformFollower f : followers_list) {
                        f.setBearing(Math.toRadians(0));
                        f.setRadius(dist);
                        dist += dist;

                    }
                }
                else if (formType.equals("Triangle")) {
                    if (followers_list.size() == 2) {
                        double[] bearing = {Math.toRadians(150), Math.toRadians(-150)};
                        for (int i=0; i < followers_list.size(); i++) {
                            PlatformFollower f = followers_list.get(i);
                            f.setBearing(bearing[i]);
                            f.setRadius(dist);
                        }
                    }
                    else {
                        JOptionPane.showMessageDialog(null,
                                "Must choose 2 platforms for triangle formation.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                followers_list.add(pfLeader);
                try {
                    sendMessage(formation);
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                    ex.printStackTrace();
                }
                
                //choose plan from available list to be considered as path
                Set<String> plans = getConsole().getMission().getIndividualPlansList().keySet();
                String plan = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Select plan"),
                        I18n.text("Sweep Path"), JOptionPane.QUESTION_MESSAGE, null,
                        plans.toArray(), plans.iterator().next());
                if (plan == null)
                    return;
                
                PlanType pt = getConsole().getMission().getIndividualPlansList().get(plan);
                Maneuver firstMan = pt.getGraph().getManeuver(pt.getGraph().getInitialManeuverId());

                double speed = 0;
                if (firstMan instanceof ManeuverWithSpeed) {
                    speed = ((ManeuverWithSpeed) firstMan).getSpeed().getMPS();                    
                }
                
                Collection<Coordinate> points_list = new ArrayList<>();
                for (Maneuver m : pt.getGraph().getManeuversSequence()) {
                    ManeuverLocation manLoc = ((LocatedManeuver) m).getManeuverLocation();
                    Z_UNITS zUnits = manLoc.getZUnits();
                    float alt = -1;
                    float depth = -1;
                    
                    if (zUnits.equals(Z_UNITS.ALTITUDE))
                        alt = (float) manLoc.getZ();
                    else if (zUnits.equals(Z_UNITS.DEPTH))
                        depth = (float) manLoc.getZ();
                    
                    manLoc = manLoc.getNewAbsoluteLatLonDepth();
                    Coordinate co = new Coordinate(manLoc.getLatitudeRads(), manLoc.getLongitudeRads(), alt, depth, 0, TEMPORAL.FALSE);
                    points_list.add(co);
                }
                SweepPath path = new SweepPath((float) speed, points_list);
                
                try {
                    sendMessage(path);
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                    ex.printStackTrace();
                }
            }
        }

        String description = JOptionPane.showInputDialog(getConsole(), I18n.text("Please enter test description"), I18n.text("Start Mission"), JOptionPane.OK_CANCEL_OPTION);

        if (description == null)
            return;
        
        MissionReadyToStart start = new MissionReadyToStart();
        start.setInfo(description);

        try {
            sendMessage(start);
        }
        catch (Exception ex) {
            GuiUtils.errorMessage(getConsole(), ex);
            ex.printStackTrace();
        }
    }

    private int getId(String platName) {
        for (Entry<Integer, String> e : platformNames.entrySet()) {
            if (e.getValue().equals(platName))
                return e.getKey();
        }
        
        return -1;
    }

    @NeptusMenuItem("Advanced>NECSAVE>Abort Mission")
    public void abortMission() {
        AbortMission abort = new AbortMission();
        abort.setType(TYPE.SYSTEM_WIDE);
        try {
            sendMessage(abort);
        }
        catch (Exception ex) {
            GuiUtils.errorMessage(getConsole(), ex);
            ex.printStackTrace();
        }
    }

    @NeptusMenuItem("Advanced>NECSAVE>Send MissionCompleted")
    public void missionCompleted() {
        MissionCompleted msg = new MissionCompleted();
        try {
            sendMessage(msg);
        }
        catch (Exception ex) {
            GuiUtils.errorMessage(getConsole(), ex);
            ex.printStackTrace();
        }
    }

    @NeptusMenuItem("Advanced>NECSAVE>Set All Platforms Idle")
    public void setPlatformsIdle() {
        for (Entry<Integer, String> plat : platformNames.entrySet()) {
            ActionIdle goIdle = new ActionIdle();
            goIdle.setIdleAtHome(IDLE_AT_HOME.FALSE);

            try {
                transport.sendMessage(goIdle, plat.getKey());
            }
            catch (Exception ex) {
                GuiUtils.errorMessage(getConsole(), ex);
                ex.printStackTrace();
            }
        }
    }
    
    @NeptusMenuItem("Advanced>NECSAVE>Clear Platforms")
    public void clearPlatforms() {
        platformNames.clear();
    }

    private static String[] getNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    private void sendMessageReliably(Message msg) throws Exception {
        msg.setMedium(MEDIUM.IP_RELIABLE);
        LinkedHashMap<String, Future<Boolean>> results = new LinkedHashMap<>();
        for (String platf : platformNames.values())
            results.put(platf,transport.sendMessage(msg, platf));                    

        int successful = 0;
        for (Entry<String, Future<Boolean> > f : results.entrySet()) {
            try {
                if (f.getValue().get())
                    successful++;      
                else
                    getConsole().post(Notification.error("NECSAVE", "Could not deliver the message to "+f.getKey()));
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        if (successful == results.size())
            getConsole().post(Notification.success("NECSAVE", "Message delivered to "+results.size()+" platforms."));            
    }

    private void sendMessageUnreliable(Message msg) throws Exception {
        msg.setMedium(MEDIUM.IP_BROADCAST);
        try {
            transport.broadcast(msg);
            getConsole().post(
                    Notification.info(I18n.text("NECSAVE"),
                            I18n.text("Sent " + msg.getAbbrev() + " to NECSAVE")));
        }
        catch (Exception ex) {
            getConsole().post(Notification.error(I18n.text("NECSAVE"),
                    I18n.textf("Could not send " + msg.getAbbrev() + " to NECSAVE: %error", ex.getMessage())));
            NeptusLog.pub().error(ex);
        }
    }

    private void sendMessage(Message msg) {
        Runnable send = new Runnable() {            
            @Override
            public void run() {
                try {
                    if (sendCommandsReliably)
                        sendMessageReliably(msg);
                    else
                        sendMessageUnreliable(msg);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NeptusLog.pub().error(e);
                }
            }
        };

        Thread t = new Thread(send, "NECSAVE send");
        t.setDaemon(true);
        t.start();        
    }

    @Subscribe
    public void on(PlatformState msg) {
        platfStates.put(msg.getSrc(), msg);
        
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(Math.toDegrees(msg.getLatitude()));
        loc.setLongitudeDegs(Math.toDegrees(msg.getLongitude()));
        loc.setDepth(msg.getZ());
        double radians = msg.getHeading() * ((Math.PI * 2) / 65535d);
        update(msg.getOriginPlatformId(), loc, Math.toDegrees(radians));
    }

    @Subscribe
    public void on(AbortMission msg) {
        getConsole().post(Notification.warning("NECSAVE AbortMission",
                "AbortMission message sent from " + platformNames.get(msg.getSrc())));
    }

    @Subscribe
    public void on(Resurface msg) {
        getConsole().post(Notification.warning("NECSAVE Resurface",
                "Resurface message sent from " + platformNames.get(msg.getSrc())));
    }


    private void update(int src, LocationType loc, double headingDegs) {
        try {
            if (!platformNames.containsKey(src))
                return;

            String name = platformNames.get(src);

            if (ImcSystemsHolder.lookupSystemByName(name) != null) {
                ImcSystemsHolder.lookupSystemByName(name).setLocation(loc);
                ImcSystemsHolder.lookupSystemByName(name).setAttitudeDegrees(headingDegs);
                
            }
            else if (ExternalSystemsHolder.lookupSystem(name) == null) {
                ExternalSystem es = new ExternalSystem(name);
                ExternalSystemsHolder.registerSystem(es);
                es.setActive(true);
                es.setType(SystemTypeEnum.UNKNOWN);
            }
            else {
                ExternalSystem extSys = ExternalSystemsHolder.lookupSystem(name);
                extSys.setLocation(loc, System.currentTimeMillis());
                extSys.setAttitudeDegrees(headingDegs);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(Kinematics msg) {
        try {
            if (msg.getWaypoint() != null) {
                LocationType loc = new LocationType(Math.toDegrees(msg.getWaypoint().getLatitude()),
                        Math.toDegrees(msg.getWaypoint().getLongitude()));
                loc.setDepth(msg.getWaypoint().getDepth());

                platfKinematics.put(msg.getSrc(), msg);
                update(msg.getSrc(), loc, Math.toDegrees(msg.getHeading()));
            }
            else {
                NeptusLog.pub().error(
                        I18n.textf("Kinematics message from %platform is not valid.", platformNames.get(msg.getSrc())));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(PlatformInfo msg) {
        platfInfos.put(msg.getPlatformId(), msg);
        platformNames.put(msg.getPlatformId(), msg.getPlatformName());
    }

    @Subscribe
    public void on(PlatformPlanProgress msg) {
        planProgresses.put(msg.getSrc(), msg);
    }

    @Subscribe
    public void on(ContactList msg) {
        for (Contact c : msg.getContactsList())
            on(c);
    }

    @Subscribe
    public void on(Contact msg) {
        getConsole().post(Notification.info("NECSAVE", "Contact detected by " + msg.getSrc()));
        LocationType loc = new LocationType();
        loc.setLatitudeRads(msg.getObject().getLatitude());
        loc.setLongitudeRads(msg.getObject().getLongitude());
        loc.setDepth(msg.getObject().getDepth());
        contacts.put(msg.getSrc() + "." + msg.getContactId(), loc);
    }

    @Subscribe
    public void on(Plan msg) {
        this.plan = msg;
    }
    
    @Subscribe
    public void on(MeshState msg) {
        MeshStateWrapper wrapper = new MeshStateWrapper(msg);
        if (sentArea != null)
            this.plan = wrapper.generatePlan(sentArea, 3);
    }
    
    private boolean isMaster(int platformId) {
        if (!platfInfos.containsKey(platformId))
            return false;

        Capabilities cap = platfInfos.get(platformId).getCapabilities();
        @SuppressWarnings("unchecked")
        Vector<Message> caps = (Vector<Message>) cap.getValue("capabilities"); 
        for (Message m : caps) {
            if (m.getMgid() == CapabilityPlanMission.ID_STATIC) {
                return m.getString("planner_mode").equals("MASTER");
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private boolean hasScanArea(int platformId) {
        if (!platfInfos.containsKey(platformId))
            return false;

        Capabilities cap = platfInfos.get(platformId).getCapabilities();
        @SuppressWarnings("unchecked")
        Vector<Message> caps = (Vector<Message>) cap.getValue("capabilities"); 
        for (Message m : caps) {
            if (m.getMgid() == CapabilityScanArea.ID_STATIC)
                return true;            
        }
        return false;
    }

    private String getDelta(int platformId) {
        if (!platfStates.containsKey(platformId))
            return "\u221E";

        return ""+(int)platfStates.get(platformId).getAgeInSeconds()+"s";
    }

    private String getProgress(int plataformId) {
        if (!platfStates.containsKey(plataformId))
            return "";
        int state = platfStates.get(plataformId).getInteger("state");

        switch (state) {
            case 200:
                return "Idle";
            case 201:
                return "Error";
            case 202:
                return "Surfacing";
            case 203:
                return "Aborted";
            case 255:
                return "";
            default:
                return ""+state;
        }
    }

    public String stateHtml() {
        String html = "<html><table>";

        for (int id : platformNames.keySet()) {
            html += "<tr><td>"+platformNames.get(id)+"</td><td>"+(isMaster(id)? "master" : "slave")+"</td><td>"+getDelta(id)+"</td><td>"+getProgress(id)+"</td></tr>";            
        }
        html+="</table></html>";
        return html;
    }


    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        JLabel lbl = new JLabel(stateHtml());
        lbl.setOpaque(true);
        lbl.setBackground(new Color(255,255,255,128));
        lbl.setSize(lbl.getPreferredSize());
        g.translate(10, 10);
        lbl.paint(g);
        g.translate(-10, -10);
        paintPlan(g, renderer);

        if (paintFormation)
            paintFormation(g, renderer);
    }
    
    private void paintPlan(Graphics2D g, StateRenderer2D source) {
        if (plan == null)
            return;

        if (platformNames.isEmpty()) {
            plan = null;
            return;
        }

        @SuppressWarnings("unchecked")
        Vector<Message> platfPlans = (Vector<Message>) plan.getValue("platform_plans"); 
        if (platfPlans.isEmpty())
            return;
        
        if (platfPlans.size() != platformColors.size()) {
            platformColors.clear();
            Vector<Color> colors = new Vector<>();
            colors.addAll(Arrays.asList(ColorUtils.generateVisuallyDistinctColors(platfPlans.size(), 0.5f, 0.5f)));
            
            for (int i = 0; i < platfPlans.size(); i++) {
                PlatformPlan p = (PlatformPlan) platfPlans.get(i);
                Color c = colors.get(i);

                if (platformNames.containsKey(p.getPlatformId())) {
                    VehicleType vt = VehiclesHolder.getVehicleById(platformNames.get(p.getPlatformId()));
                    if (vt != null)
                        c = vt.getIconColor();
                }
                
                platformColors.put(p.getPlatformId(), c);                
            }
        }
        
        Vector<Color> colors = new Vector<>();
        colors.addAll(Arrays.asList(ColorUtils.generateVisuallyDistinctColors(platfPlans.size(), 0.5f, 0.5f)));
        
        for (int i = 0; i < platfPlans.size(); i++) {
            PlatformPlan p = (PlatformPlan) platfPlans.get(i);
            @SuppressWarnings("unchecked")
            Vector<Message> behaviors = (Vector<Message>) p.getValue("behaviors"); 
            for (Message b : behaviors) {
                if (b.getMgid() == BehaviorScanArea.ID_STATIC) {
                    paintScanArea((BehaviorScanArea)b, platformColors.get(p.getPlatformId()), g, source);
                }
            }
        }        
    }

    private void paintFormation(Graphics2D g, StateRenderer2D renderer) {
        if (platfKinematics.isEmpty() || plan == null)
            return;

        if (platformNames.isEmpty()) {
            plan = null;
            return;
        }

        @SuppressWarnings("unchecked")
        Vector<Message> platfPlans = (Vector<Message>) plan.getValue("platform_plans");
        if (platfPlans.isEmpty())
            return;

        followers.clear();

        PlatformPlan p = (PlatformPlan) platfPlans.get(0);
        @SuppressWarnings("unchecked")
        Vector<Message> behaviors = (Vector<Message>) p.getValue("behaviors");
        for (Message b : behaviors) {
            if (b.getMgid() == BehaviorPatternFormation.ID_STATIC) {
                BehaviorPatternFormation form = (BehaviorPatternFormation) b;
                Formation f = form.getFormation();
                @SuppressWarnings("unchecked")
                Vector<PlatformFollower> a = (Vector<PlatformFollower>) f.getValue("followers_list");
                followers.addAll(a);
                break;
            }
        }

        AffineTransform old = g.getTransform();
        
        int j = 0;
        for (int i = 0; i < followers.size(); i++) {
            Kinematics kin0 = platfKinematics.get(followers.get(i).getFollowerPlatformId());

            if (i == followers.size() - 1)
                j = 0;
            else
                j = j + 1;

            Kinematics kin1 = platfKinematics.get(followers.get(j).getFollowerPlatformId());
            LocationType loc0 = new LocationType(Math.toDegrees(kin0.getWaypoint().getLatitude()),
                    Math.toDegrees(kin0.getWaypoint().getLongitude()));
            loc0.setDepth(kin0.getWaypoint().getDepth());
            LocationType loc1 = new LocationType(Math.toDegrees(kin1.getWaypoint().getLatitude()),
                    Math.toDegrees(kin1.getWaypoint().getLongitude()));
            loc1.setDepth(kin1.getWaypoint().getDepth());
            Point2D point0 = renderer.getScreenPosition(loc0);
            Point2D point1 = renderer.getScreenPosition(loc1);

            g.setColor(new Color(255, 255, 0));
            g.setStroke(new BasicStroke(3));
            g.drawLine((int) point0.getX(), (int) point0.getY(), (int) point1.getX(), (int) point1.getY());

            double angle = Math.atan2(point0.getY() - point1.getY(), point0.getX() - point1.getX());

            g.translate(point0.getX(), point0.getY());
            g.rotate(angle - Math.PI / 2);

            double distance = point0.distance(point1);
            g.translate(0, -distance / 2);
            g.rotate(Math.PI / 2);

            if (Math.abs(angle) > Math.PI / 2) {
                g.rotate(Math.PI);
            }

            String txt = GuiUtils.getNeptusDecimalFormat(0).format(loc0.getDistanceInMeters(loc1)) + " m";
            Font oldFont = g.getFont();
            g.setFont(new Font("Arial", Font.PLAIN, 11));

            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(txt, g);
            g.setColor(Color.BLACK);
            g.drawString(txt, -(int) stringBounds.getWidth() / 2 + 1, -2);
            g.setColor(Color.WHITE);
            g.drawString(txt, -(int) stringBounds.getWidth() / 2, -3);
            g.setFont(oldFont);

            g.setTransform(old);
        }

    }

    private void paintScanArea(BehaviorScanArea b, Color c, Graphics2D g, StateRenderer2D source) {

        
        //g.setTransform(source.getIdentity());
        AffineTransform old = g.getTransform();
        LocationType lt = new LocationType();
        lt.setLatitudeRads(b.getScanArea().getLatitude());
        lt.setLongitudeRads(b.getScanArea().getLongitude());
        
        area.setColor(c.brighter());
        area.setLength(b.getScanArea().getLength());
        area.setWidth(b.getScanArea().getWidth());
        area.setYaw(Math.toDegrees(b.getScanArea().getBearing())); 
        
        lt.setOffsetDistance(area.getLength() / 2);
        lt.setAzimuth(Math.toDegrees(b.getScanArea().getBearing()));
        lt.convertToAbsoluteLatLonDepth();
        lt.setOffsetDistance(area.getWidth() / 2);
        lt.setAzimuth(Math.toDegrees(b.getScanArea().getBearing() + Math.PI / 2));
        lt.convertToAbsoluteLatLonDepth();
        
        area.setCenterLocation(lt);
        area.paint(g, source, 0);
        g.setFont(new Font("Helvetica", Font.BOLD, 18));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString(""+b.getScanArea().getAreaId(), -5, 5);
        g.setTransform(old);        
    }
    
    @Override
    public void cleanLayer() {
        if (transport != null)
            transport.stop();
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }
}
