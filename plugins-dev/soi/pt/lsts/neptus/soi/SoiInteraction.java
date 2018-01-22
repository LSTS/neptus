/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;

import com.google.common.eventbus.Subscribe;

import pt.lsts.autonomy.soi.Plan;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.SoiCommand;
import pt.lsts.imc.SoiCommand.COMMAND;
import pt.lsts.imc.SoiCommand.TYPE;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "SoiInteraction")
public class SoiInteraction extends SimpleRendererInteraction {

    private static final long serialVersionUID = 477322168507708457L;
    private LinkedHashMap<String, SoiSettings> settings = new LinkedHashMap<>();

    @NeptusProperty(name = "Communication Mean", description = "Communication mean to use to send commands")
    public CommMean commMean = CommMean.WiFi;

    @NeptusProperty(name = "Soi Plan ID", description = "Identifier for SOI plan")
    public String soiPlanId = "soi_plan";

    @NeptusProperty(name = "Schedule plan waypoints", description = "Schedule plan before transmission")
    public boolean scheduleWaypoints = false;

    @NeptusProperty(name = "Time (seconds) till first waypoint", description = "Time, in seconds, for the first waypoint ETA")
    public double timeToFirstWaypoint;

    @NeptusProperty(name = "Hide layer if inactive")
    public boolean hideIfInactive = true;

    private LinkedHashMap<String, Plan> plans = new LinkedHashMap<>();

    @NeptusMenuItem("Tools>SOI>Send Resume")
    public void sendResume() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.RESUME);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd);
    }

    @NeptusMenuItem("Tools>SOI>Send Stop")
    public void sendStop() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.STOP);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd);
    }

    @NeptusMenuItem("Tools>SOI>Send Plan")
    public void sendPlan() {
        String system = getConsole().getMainSystem();
        Plan plan;
        try {
            PlanType ptype = getConsole().getMission().getIndividualPlansList().get(soiPlanId);
            plan = Plan.parse((PlanSpecification) ptype.asIMCPlan());
            SoiCommand cmd = new SoiCommand();
            cmd.setCommand(COMMAND.EXEC);
            cmd.setType(TYPE.REQUEST);
            cmd.setPlan(plan.asImc());
            sendCommand(cmd);

            if (scheduleWaypoints) {

                if (!settings.containsKey(system)) {
                    settings.put(system, new SoiSettings());
                }
                SoiSettings vehicleSettings = settings.get(system);
                plan.scheduleWaypoints(System.currentTimeMillis() + (long) (timeToFirstWaypoint * 1000l),
                        vehicleSettings.speed);
            }

            plans.put(system, plan);
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
        sendCommand(cmd);
    }

    @NeptusMenuItem("Tools>SOI>Request Settings")
    public void getSettings() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.GET_PARAMS);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd);
    }

    @NeptusMenuItem("Tools>SOI>Request Plan")
    public void getPlan() {
        SoiCommand cmd = new SoiCommand();
        cmd.setCommand(COMMAND.GET_PLAN);
        cmd.setType(TYPE.REQUEST);
        sendCommand(cmd);
    }

    @NeptusMenuItem("Tools>SOI>Change Settings")
    public void sendSettings() {

        String system = getConsole().getMainSystem();

        if (!settings.containsKey(system)) {
            settings.put(system, new SoiSettings());
        }

        if (PluginUtils.editPluginProperties(settings.get(system), getConsole(), true))
            return;
        @SuppressWarnings("unchecked")
        List<PluginProperty> after = Arrays.asList(PluginUtils.getPluginProperties(settings.get(system)));
        SoiCommand cmd = new SoiCommand();
        cmd.setType(TYPE.REQUEST);
        cmd.setCommand(COMMAND.SET_PARAMS);
        String settingsStr = "";
        for (PluginProperty p : after) {
            settingsStr += p.getName() + "=" + p.getValue() + ";";
        }
        cmd.setSettings(settingsStr.substring(0, settingsStr.length() - 1));
        sendCommand(cmd);
    }

    protected void setParams(String vehicle, LinkedHashMap<String, String> params) {
        if (!settings.containsKey(vehicle))
            settings.put(vehicle, new SoiSettings());

        PluginProperty[] props = PluginUtils.getPluginProperties(settings.get(vehicle));

        for (PluginProperty p : props) {

            if (!params.containsKey(p.getName()))
                continue;

            switch (p.getType().getSimpleName()) {
                case "String":
                    p.setValue(params.get(p.getName()));
                    break;
                case "double":
                    p.setValue(Double.parseDouble(params.get(p.getName())));
                    break;
                case "float":
                    p.setValue(Float.parseFloat(params.get(p.getName())));
                    break;
                case "int":
                    p.setValue(Integer.parseInt(params.get(p.getName())));
                    break;
                case "boolean":
                    p.setValue(Boolean.parseBoolean(params.get(p.getName())));
                    break;
                default:
                    System.out.println("Class not recognized: " + p.getType());
                    break;
            }
        }

        PluginUtils.setPluginProperties(settings.get(vehicle), props);
    }

    @Subscribe
    public void on(SoiCommand cmd) {
        if (cmd.getType() != SoiCommand.TYPE.SUCCESS)
            return;

        NeptusLog.pub().info("Processing SoiCommand: " + cmd.asJSON() + ", " + Thread.currentThread().getName() + ", "
                + cmd.hashCode());

        switch (cmd.getCommand()) {
            case GET_PARAMS:
                getConsole().post(Notification.success(I18n.text("SOI Settings"),
                        I18n.textf("Received settings from %vehicle.", cmd.getSourceName())));
                setParams(cmd.getSourceName(), cmd.getSettings());
                break;
            case GET_PLAN:
            case EXEC:
                getConsole().post(Notification.success(I18n.text("SOI Plan"),
                        I18n.textf("Received plan from %vehicle.", cmd.getSourceName())));
                plans.put(cmd.getSourceName(), Plan.parse(cmd.getPlan()));
                break;
            default:
                break;
        }
    }

    private void paintPlans(Graphics2D g, StateRenderer2D renderer) {
        for (Entry<String, Plan> p : plans.entrySet()) {
            SoiPlanRenderer prenderer = new SoiPlanRenderer();
            try {
                prenderer.setColor(VehiclesHolder.getVehicleById(p.getKey()).getIconColor());
                prenderer.setPlan(p.getValue());
                prenderer.paint(g, renderer);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            
            for (final Method m : getClass().getDeclaredMethods()) {
                if (m.getAnnotation(NeptusMenuItem.class) != null) {
                    String path = m.getAnnotation(NeptusMenuItem.class).value();
                    String name = path.substring(path.lastIndexOf(">")+1);
                    
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
                }
            });
            
            popup.show(source, event.getX(), event.getY());
        }
    }

    private void sendCommand(SoiCommand cmd) {

        if (commMean == CommMean.WiFi) {
            send(cmd);
            getConsole().post(Notification.success(I18n.text("Command sent"),
                    I18n.textf("%cmd sent over UDP to %vehicle.", cmd.getCommandStr(), getConsole().getMainSystem())));
        }
        else if (commMean == CommMean.Iridium) {
            try {
                ImcSystem system = ImcSystemsHolder.lookupSystemByName(getConsole().getMainSystem());
                ImcIridiumMessage msg = new ImcIridiumMessage();
                msg.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                msg.setMsg(cmd);
                msg.setDestination(system.getId().intValue());
                IridiumManager.getManager().send(msg);
                getConsole().post(Notification.success("Iridium message sent", "1 Iridium messages were sent using "
                        + IridiumManager.getManager().getCurrentMessenger().getName()));
            }
            catch (Exception e) {
                GuiUtils.errorMessage(getConsole(), e);
            }
        }
    }

    /**
     * @param console
     */
    public SoiInteraction(ConsoleLayout console) {
        super(console);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }

    enum CommMean {
        WiFi,
        Iridium
    }

    public String infoHtml(ImcSystem[] vehicles) {
        StringBuilder html = new StringBuilder();
        html.append("<html><table>");
        html.append("<tr><th>Vehicle</th><th>Distance</th><th>Last Comm.</th><th>Next Comm.</th><th>Fuel</th></tr>\n");

        for (ImcSystem vehicle : vehicles) {
            if (vehicle.getLocation() == null)
                continue;

            Plan plan = plans.get(vehicle.getName());
            if (plan == null)
                continue;

            SystemPositionAndAttitude lastState = new SystemPositionAndAttitude(vehicle.getLocation(), 0, 0, 0);
            lastState.setTime(vehicle.getLocationTimeMillis());

            SystemPositionAndAttitude estimatedState = SoiUtils.estimatedState(vehicle, plan);

            SystemPositionAndAttitude futureState = SoiUtils.futureState(vehicle, plan);

            String distance = "?";
            String lastComm = "?";
            String nextComm = "?";
            String fuel = "?";

            if (estimatedState != null)
                distance = String.format(Locale.US, "%.0f m",
                        MyState.getLocation().getDistanceInMeters(estimatedState.getPosition()));

            if (lastState != null)
                lastComm = DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - lastState.getTime());

            if (futureState != null)
                nextComm = DateTimeUtil
                        .milliSecondsToFormatedString(futureState.getTime() - System.currentTimeMillis());

            ImcSystemState state = ImcMsgManager.getManager().getState(vehicle.getName());
            if (state != null) {
                IMCMessage fuelLevel = state.get("FuelLevel");
                IMCMessage stateReport = state.get("StateReport");
                if (stateReport != null && fuelLevel != null) {
                    if (stateReport.getTimestampMillis() > fuelLevel.getTimestampMillis())
                        fuel = stateReport.getInteger("fuel") + "%";
                    else
                        fuel = fuelLevel.getInteger("value") + "%";
                }
                else if (stateReport != null)
                    fuel = stateReport.getInteger("fuel") + "%";
                else if (fuelLevel != null)
                    fuel = fuelLevel.getInteger("value") + "%";

            }

            html.append("<tr><td>" + vehicle.getName() + "</td><td>" + distance + "</td><td>" + lastComm + "</td><td>"
                    + nextComm + "</td><td>" + fuel + "</td></tr>\n");
        }

        html.append("</table></html>");

        return html.toString();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        if (!active && hideIfInactive)
            return;

        // String sys = getConsole().getMainSystem();

        JLabel label = new JLabel(infoHtml(ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.VEHICLE)));
        Dimension d = label.getPreferredSize();

        int x = (int) ((renderer.getWidth() - d.getWidth()) / 2.0);

        label.setBounds(x, 0, (int) d.getWidth(), (int) d.getHeight());
        label.setBackground(new Color(255, 255, 255, 128));
        label.setForeground(Color.BLACK);
        label.setOpaque(true);

        g.translate(x, 0);
        label.paint(g);
        g.translate(-x, 0);

        paintPlans(g, renderer);
    }

    class SoiSettings {
        @NeptusProperty(description = "Nominal Speed")
        double speed = 1;

        @NeptusProperty(description = "Maximum Depth")
        double max_depth = 10;

        @NeptusProperty(description = "Minimum Depth")
        double min_depth = 0.0;

        @NeptusProperty(description = "Maximum Speed")
        double max_speed = 1.5;

        @NeptusProperty(description = "Minimum Speed")
        double min_speed = 0.7;

        @NeptusProperty(description = "Maximum time underwater")
        int mins_under = 10;

        @NeptusProperty(description = "Number where to send reports")
        String sms_number = "+351914785889";

        @NeptusProperty(description = "Seconds to idle at each vertex")
        int wait_secs = 60;

        @NeptusProperty(description = "SOI plan identifier")
        String soi_plan_id = "soi_plan";

        @NeptusProperty(description = "Cyclic execution")
        boolean cycle = false;
    }
}
