/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 09/10/2017
 */
package pt.lsts.neptus.soi;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Announce;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.SoiCommand;
import pt.lsts.imc.SoiCommand.COMMAND;
import pt.lsts.imc.SoiCommand.TYPE;
import pt.lsts.imc.SoiPlan;
import pt.lsts.imc.StateReport;
import pt.lsts.imc.VerticalProfile;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.endurance.AssetsManager;
import pt.lsts.neptus.endurance.CommMean;
import pt.lsts.neptus.endurance.DripSettings;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.endurance.SoiSettings;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.speech.SpeechUtil;

/**
 * @author zp
 *
 */
@PluginDescription(name = "SOI Interaction", icon="pt/lsts/neptus/soi/icons/soi_interaction.png")
public class SoiInteraction extends SimpleRendererInteraction {

    private static final long serialVersionUID = 477322168507708457L;

    @NeptusProperty(name = "Communication Mean", userLevel = LEVEL.REGULAR,
            description = "Communication mean to use to send commands")
    public CommMean commMean = CommMean.WiFi;

    @NeptusProperty(name = "Schedule plan waypoints", description = "Schedule plan before transmission")
    public boolean scheduleWaypoints = false;

    @NeptusProperty(name = "Time (seconds) till first waypoint", description = "Time, in seconds, for the first waypoint ETA")
    public double timeToFirstWaypoint;

    @NeptusProperty(name = "Hide layer if inactive", userLevel = LEVEL.REGULAR)
    public boolean hideIfInactive = true;

    @NeptusProperty(name = "Audio Notifications", userLevel = LEVEL.REGULAR)
    public boolean audioNotifications = true;
    
    @NeptusProperty(name = "Maximum profile age (hours)", userLevel = LEVEL.REGULAR,
            description = "Profiles older than this age will be hidden")
    public int oldestProfiles = 24;
        
    @NeptusProperty(name = "Use salinity colormap for profiles", userLevel = LEVEL.REGULAR)
    public boolean colorizeSalinity = false;
    
    @NeptusProperty(name = "Show profile values", userLevel = LEVEL.REGULAR)
    public boolean profileValues = true;
    
    
    private VerticalProfileViewer profileView = new VerticalProfileViewer();
    
    /**
     * @param console
     */
    public SoiInteraction(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @NeptusMenuItem("Tools>SOI>Send Resume")
    public void sendResume() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.RESUME);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd, getConsole().getMainSystem());
    }

    @NeptusMenuItem("Tools>SOI>Send Stop")
    public void sendStop() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.STOP);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd, getConsole().getMainSystem());
    }

    @NeptusMenuItem("Tools>SOI>Send Plan")
    public void sendPlan() {
        String system = getConsole().getMainSystem();
        Plan plan;
        try {

            Collection<PlanType> pps = getConsole().getMission().getIndividualPlansList().values();
            List<String> ps = pps.stream().map(p -> p.getId()).collect(Collectors.toList());

            if (ps.isEmpty()) {
                GuiUtils.errorMessage(getConsole(), "Send SOI plan", "Create a plan to define the SOI waypoints.");
                return;
            }

            String p = getConsole().getPlan() != null ? getConsole().getPlan().getId() : ps.get(0);
            final Object selection = JOptionPane.showInputDialog(getConsole(),
                    "Select plan to be sent to "+system, "Send plan", JOptionPane.QUESTION_MESSAGE, null,
                    ps.toArray(), p);
            if (selection == null)
                return;

            PlanType ptype = getConsole().getMission().getIndividualPlansList().get("" + selection);
            plan = Plan.parse((PlanSpecification) ptype.asIMCPlan());

            if (scheduleWaypoints) {
                SoiSettings vehicleSettings = (SoiSettings) AssetsManager.getInstance().getSettings().getOrDefault(system, new SoiSettings());
                plan.scheduleWaypoints(System.currentTimeMillis() + (long) (timeToFirstWaypoint * 1000l),
                        vehicleSettings.speed);
            }

            SoiCommand cmd = new SoiCommand();
            cmd.setCommand(COMMAND.EXEC);
            cmd.setType(TYPE.REQUEST);
            cmd.setPlan(plan.asImc());
            sendCommand(cmd, system);

            AssetsManager.getInstance().getPlans().put(system, plan);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error translating plan", e);
        }
    }

    @NeptusMenuItem("Tools>SOI>Clear Plan")
    public void clearPlan() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.EXEC);
        cmd.setType(TYPE.REQUEST);
        cmd.setPlan(new SoiPlan());
        sendCommand(cmd, getConsole().getMainSystem());
    }

    @NeptusMenuItem("Tools>SOI>Request Settings")
    public void getSettings() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.GET_PARAMS);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd, getConsole().getMainSystem());
    }

    @NeptusMenuItem("Tools>SOI>Request Plan")
    public void getPlan() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.GET_PLAN);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd, getConsole().getMainSystem());
    }

    @NeptusMenuItem("Tools>SOI>Change SOI Settings")
    public void sendSOISettings() {

        String system = getConsole().getMainSystem();

        AssetsManager.getInstance().getSettings().putIfAbsent(system, new SoiSettings());

        if (PluginUtils.editPluginProperties(AssetsManager.getInstance().getSettings().get(system), true))
            return;
        List<PluginProperty> after = Arrays.asList(PluginUtils.getPluginProperties(AssetsManager.getInstance().getSettings().get(system)));
        SoiCommand cmd = new SoiCommand();
        cmd.setType(TYPE.REQUEST);
        cmd.setCommand(COMMAND.SET_PARAMS);
        String settingsStr = "";
        for (PluginProperty p : after) {
            settingsStr += SoiSettings.abbrev(p.getName()) + "=" + p.getValue() + ";";
        }
        cmd.setSettings(settingsStr.substring(0, settingsStr.length() - 1));
        sendCommand(cmd, system);
    }
    
    @NeptusMenuItem("Tools>SOI>Change Drip Settings")
    public void sendDripSettings() {

        String system = getConsole().getMainSystem();

        AssetsManager.getInstance().getDripSettings().putIfAbsent(system, new DripSettings());

        if (PluginUtils.editPluginProperties(AssetsManager.getInstance().getDripSettings().get(system), true))
            return;
        List<PluginProperty> after = Arrays.asList(PluginUtils.getPluginProperties(AssetsManager.getInstance().getDripSettings().get(system)));
        SoiCommand cmd = new SoiCommand();
        cmd.setType(TYPE.REQUEST);
        cmd.setCommand(COMMAND.SET_PARAMS);
        String settingsStr = "";
        for (PluginProperty p : after) {
            settingsStr += DripSettings.abbrev(p.getName()) + "=" + p.getValue() + ";";
        }
        cmd.setSettings(settingsStr.substring(0, settingsStr.length() - 1));
        sendCommand(cmd, system);
    }
    
    protected void setParams(String vehicle, LinkedHashMap<String, String> params) {
        try {
            AssetsManager.getInstance().setParams(vehicle, params);

            AssetsManager.getInstance().getSettings().putIfAbsent(vehicle, new SoiSettings());

            PluginProperty[] props = PluginUtils.getPluginProperties(AssetsManager.getInstance().getSettings().get(vehicle));

            LinkedHashMap<String, String> fieldToName = new LinkedHashMap<>();

            // Translate between field names and property names
            for (Field f : SoiSettings.class.getDeclaredFields()) {
                NeptusProperty prop = f.getAnnotation(NeptusProperty.class);
                if (prop != null)
                    fieldToName.put(prop.name(), f.getName());
            }

            for (PluginProperty p : props) {

                String name = fieldToName.get(p.getName());
                if (name == null || !params.containsKey(name))
                    continue;

                switch (p.getType().getSimpleName()) {
                    case "String":
                        p.setValue(params.get(name));
                        break;
                    case "double":
                        p.setValue(Double.parseDouble(params.get(name)));
                        break;
                    case "float":
                        p.setValue(Float.parseFloat(params.get(name)));
                        break;
                    case "int":
                        p.setValue(Integer.parseInt(params.get(name)));
                        break;
                    case "boolean":
                        p.setValue(Boolean.parseBoolean(params.get(name)));
                        break;
                    default:
                        System.out.println("Class not recognized: " + p.getType());
                        break;
                }
            }

            PluginUtils.setPluginProperties(AssetsManager.getInstance().getSettings().get(vehicle), props);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(VerticalProfile msg) {
        profileView.addProfile(msg);
        getConsole().post(Notification.success(I18n.text("Profile from "+msg.getSourceName()),
                I18n.textf("Received %param profile from %vehicle.", msg.getParameter().name().toLowerCase(), msg.getSourceName())));
        
        if (audioNotifications) {
            VehicleType v = VehiclesHolder.getVehicleById(msg.getSourceName());
            String vName = "Vehicle";
            if (v != null)
                vName = v.getNickname();
            
            say(vName+ " profile");
            
        }
    }
    
    private void say(String text) {
        if (audioNotifications) {
            SpeechUtil.removeStringsFromQueue(text);
            SpeechUtil.readSimpleText(text);
            NeptusLog.pub().info("Saying \""+text+"\"");
        }
    }
    
    @Subscribe
    public void on(StateReport cmd) {
        try {
            AssetsManager.getInstance().process(cmd);    
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        if (audioNotifications) {
            VehicleType v = VehiclesHolder.getVehicleById(cmd.getSourceName());
            String vName = "Vehicle";
            if (v != null)
                vName = v.getNickname();
            
            say(vName+ " update");
            
        }
        
    }
    
    @Subscribe
    public void on(Announce cmd) {
        try { 
            AssetsManager.getInstance().process(cmd);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(SoiCommand cmd) {
        try {
            AssetsManager.getInstance().process(cmd, getConsole());
    
            if (cmd.getType() != SoiCommand.TYPE.SUCCESS)
                return;
    
            NeptusLog.pub().info("Processing SoiCommand: " + cmd.asJSON() + ", " + Thread.currentThread().getName() + ", "
                    + cmd.hashCode());
    
            VehicleType v = VehiclesHolder.getVehicleById(cmd.getSourceName());
            String vName = "Vehicle";
            if (v != null)
                vName = v.getNickname();
            
            switch (cmd.getCommand()) {
                case GET_PARAMS:
                case SET_PARAMS:
                    setParams(cmd.getSourceName(), cmd.getSettings());
                    say(vName+" params");
                    break;
                case GET_PLAN:
                case EXEC:
                    Plan plan = Plan.parse(cmd.getPlan());
                    if (plan != null) 
                        AssetsManager.getInstance().getPlans().put(cmd.getSourceName(), Plan.parse(cmd.getPlan()));
                    else
                        AssetsManager.getInstance().getPlans().remove(cmd.getSourceName());
                    say(vName+" plan");
                    break;
                default:
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            for (final Method m : getClass().getDeclaredMethods()) {
                if (m.getAnnotation(NeptusMenuItem.class) != null) {
                    String path = m.getAnnotation(NeptusMenuItem.class).value();
                    String name = path.substring(path.lastIndexOf(">") + 1);

                    popup.add(name).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                m.invoke(SoiInteraction.this);
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }
            }

            popup.addSeparator();

            popup.add("Change plug-in settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PluginUtils.editPluginProperties(SoiInteraction.this, true);
                    profileView.setOldestProfiles(oldestProfiles);
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else {
            profileView.mouseClicked(event, source);
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        profileView.mouseMoved(event, source);
    }

    private void sendCommand(SoiCommand cmd, final String system) {
        new Thread(() -> {
            AssetsManager.getInstance().sendCommand(system, cmd, commMean, getConsole());
        }).start();
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        if (!active && hideIfInactive)
            return;

        SoiStateRenderer.paintStatic(g, renderer);        
        profileView.setColorizeSalinity(colorizeSalinity);
        profileView.setValuesTable(profileValues);
        profileView.paint(g, renderer);
    }
}
