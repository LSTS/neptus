/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Oct 19, 2015
 */
package org.necsave;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;

import info.necsave.msgs.AbortMission;
import info.necsave.msgs.AbortMission.TYPE;
import info.necsave.msgs.Header.MEDIUM;
import info.necsave.msgs.Area;
import info.necsave.msgs.Contact;
import info.necsave.msgs.ContactList;
import info.necsave.msgs.Kinematics;
import info.necsave.msgs.MissionArea;
import info.necsave.msgs.MissionCompleted;
import info.necsave.msgs.MissionGoal;
import info.necsave.msgs.MissionGoal.GOAL_TYPE;
import info.necsave.msgs.MissionReadyToStart;
import info.necsave.msgs.PlatformInfo;
import info.necsave.msgs.PlatformPlanProgress;
import info.necsave.proto.Message;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.GuiUtils;

/**
 * This class will show the states of NECSAVE platforms and allows interactions with them
 * 
 * @author zp
 * 
 */
@PluginDescription(name = "NECSAVE UI")
public class NecsaveUI extends ConsoleInteraction {

    @NeptusProperty(description="Select if the commands should be sent using TCP communications")
    public boolean sendCommandsReliably = true;

    private NecsaveTransport transport = null;
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PlatformPlanProgress> planProgresses = new LinkedHashMap<>();
    private LinkedHashMap<String, LocationType> contacts = new LinkedHashMap<>();
    private ParallelepipedElement elem = null;
    private LocationType corner = null; 
    private double width, height;

    @Override
    public void initInteraction() {
        try {
            transport = new NecsaveTransport(getConsole());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        addActions();
    }

    public void addActions() {
        getConsole().addMenuItem("Advanced>NECSAVE>Set Mission", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                MapGroup mg = MapGroup.getMapGroupInstance(getConsole().getMission());
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
                corner.convertToAbsoluteLatLonDepth();

                NecsaveUI.this.width = elem.getWidth();
                NecsaveUI.this.height = elem.getLength();
                Area area = new Area();
                area.setLatitude(corner.getLatitudeRads());
                area.setLongitude(corner.getLongitudeRads());
                area.setBearing(a);
                area.setWidth(elem.getWidth());
                area.setLength(elem.getLength());

                try {
                    sendMessage(new MissionArea(area));                    
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                    ex.printStackTrace();
                }

                String[] goals = getNames(GOAL_TYPE.class);

                Object goal_ret = JOptionPane.showInputDialog(getConsole(), I18n.text("Select goal"),
                        I18n.text("Select goal"), JOptionPane.QUESTION_MESSAGE, null,
                        goals, goals[0]);
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
        });

        getConsole().addMenuItem("Advanced>NECSAVE>Abort Mission", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
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
        });

        getConsole().addMenuItem("Advanced>NECSAVE>Send MissionCompleted", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                MissionCompleted msg = new MissionCompleted();
                try {
                    sendMessage(msg);
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                    ex.printStackTrace();
                }
            }
        });

        getConsole().addMenuItem("Advanced>NECSAVE>Clear Platforms", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                platformNames.clear();
            }
        });
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
    public void on(Kinematics msg) {
        try {
            if (!platformNames.containsKey(msg.getSrc()))
                return;

            String name = platformNames.get(msg.getSrc());

            if (ExternalSystemsHolder.lookupSystem(name) == null) {
                ExternalSystem es = new ExternalSystem(name);
                ExternalSystemsHolder.registerSystem(es);
                es.setActive(true);
                es.setType(SystemTypeEnum.UNKNOWN);
            }
            ExternalSystem extSys = ExternalSystemsHolder.lookupSystem(name);
            if (msg.getWaypoint() != null) {
                LocationType loc = new LocationType(Math.toDegrees(msg.getWaypoint().getLatitude()),
                        Math.toDegrees(msg.getWaypoint().getLongitude()));
                loc.setDepth(msg.getWaypoint().getDepth());
                extSys.setLocation(loc, System.currentTimeMillis());
                extSys.setAttitudeDegrees(Math.toDegrees(msg.getHeading()));
            }
            else {
                NeptusLog.pub().error(I18n.textf("Kinematics message from %platform is not valid.", name));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(PlatformInfo msg) {
        platformNames.put(msg.getSrc(), msg.getPlatformName());
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

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        super.paintInteraction(g, source);
        g.setColor(Color.black);
        int x = 50;
        for (int id : platformNames.keySet()) {
            g.drawString(platformNames.get(id)+": "+transport.addressOf(id), 50, x);
            x += 20;
        }
    }

    @Override
    public void cleanInteraction() {
        if (transport != null)
            transport.stop();
    }
}
