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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.SoiCommand;
import pt.lsts.imc.SoiCommand.COMMAND;
import pt.lsts.imc.SoiCommand.TYPE;
import pt.lsts.imc.SoiPlan;
import pt.lsts.imc.StateReport;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.endurance.AssetsManager;
import pt.lsts.neptus.endurance.CommMean;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.endurance.SoiSettings;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
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
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "SoiInteraction")
public class SoiInteraction extends SimpleRendererInteraction {

    private static final long serialVersionUID = 477322168507708457L;

    @NeptusProperty(name = "Communication Mean", description = "Communication mean to use to send commands", userLevel = LEVEL.REGULAR)
    public CommMean commMean = CommMean.WiFi;

    @NeptusProperty(name = "Schedule plan waypoints", description = "Schedule plan before transmission")
    public boolean scheduleWaypoints = false;

    @NeptusProperty(name = "Time (seconds) till first waypoint", description = "Time, in seconds, for the first waypoint ETA")
    public double timeToFirstWaypoint;

    @NeptusProperty(name = "Hide layer if inactive")
    public boolean hideIfInactive = true;

    @NeptusProperty(name = "Battery Entity Name", description = "Vehicle Battery entity name")
    public String batteryEntityName = "Batteries";

    private LinkedHashMap<String, Plan> plans = new LinkedHashMap<>();
    private LinkedHashMap<String, SoiSettings> settings = new LinkedHashMap<>();
    
    private AssetsManager assetsManager = AssetsManager.getInstance();

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
            
            Collection<PlanType> pps = getConsole().getMission().getIndividualPlansList().values();
            List<String> ps = pps.stream().map(p -> p.getId()).collect(Collectors.toList());
            
            if (ps.isEmpty()) {
                GuiUtils.errorMessage(getConsole(), "Send SOI plan", "Create a plan to define the SOI waypoints.");
                return;
            }
            
            String p = getConsole().getPlan() != null ? getConsole().getPlan().getId() : ps.get(0);
            final Object selection = JOptionPane.showInputDialog(getConsole(), "Select plan to be sent as SOI waypoints",
                    "Start plan", JOptionPane.QUESTION_MESSAGE, null, ps.toArray(), p);
            if (selection == null)
                return;
                
            PlanType ptype = getConsole().getMission().getIndividualPlansList().get(""+selection);
            plan = Plan.parse((PlanSpecification) ptype.asIMCPlan());

            if (scheduleWaypoints) {
                if (!settings.containsKey(system)) {
                    settings.put(system, new SoiSettings());
                }
                SoiSettings vehicleSettings = settings.get(system);
                plan.scheduleWaypoints(System.currentTimeMillis() + (long) (timeToFirstWaypoint * 1000l),
                        vehicleSettings.speed);
            }

            SoiCommand cmd = new SoiCommand();
            cmd.setCommand(COMMAND.EXEC);
            cmd.setType(TYPE.REQUEST);
            cmd.setPlan(plan.asImc());
            sendCommand(cmd);


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
        cmd.setPlan(new SoiPlan());
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
        assetsManager.setParams(vehicle, params);
        
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
        assetsManager.process(cmd, getConsole());
        
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
        new Thread(() -> {
            assetsManager.sendCommand(getConsole().getMainSystem(), cmd, commMean, getConsole());
        }).start();        
    }

     public String infoHtml(ImcSystem[] vehicles) {
        StringBuilder html = new StringBuilder();
        html.append("<html><table>");
        html.append("<tr><th>Vehicle</th><th>Distance</th><th>Last Comm.</th><th>Next Comm.</th><th>Fuel</th><th>Match Plan</th></tr>\n");

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
            String matchPlan = "?";

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
                IMCMessage voltage = state.get(Voltage.ID_STATIC, EntitiesResolver.resolveId(vehicle.getName(), batteryEntityName));
                String voltageStr = "";
                if (voltage != null)
                    voltageStr = " (Batt: " + MathMiscUtils.round(((Voltage) voltage).getValue(), 1) + "V)";
                
                if (stateReport != null && fuelLevel != null) {
                    if (stateReport.getTimestampMillis() > fuelLevel.getTimestampMillis())
                        fuel = stateReport.getInteger("fuel") + "%" + voltageStr;
                    else
                        fuel = fuelLevel.getInteger("value") + "%" + voltageStr;
                }
                else if (stateReport != null)
                    fuel = stateReport.getInteger("fuel") + "%" + voltageStr;
                else if (fuelLevel != null)
                    fuel = fuelLevel.getInteger("value") + "%" + voltageStr;
                
                if (stateReport != null) {
                    int pcsum = ((StateReport) stateReport).getPlanChecksum();
                    matchPlan = "";
                    for (String pl : getConsole().getMission().getIndividualPlansList().keySet()) {
                        PlanType planType = getConsole().getMission().getIndividualPlansList().get(pl);
                        PlanSpecification pSpec = (PlanSpecification) planType.asIMCPlan();
                        int localPcsum = Plan.parse(pSpec).checksum();
                        if (localPcsum == pcsum) {
                            matchPlan = plan + " (CS::" + pcsum + ")";
                            break;
                        }
                    }
                }
            }

            html.append("<tr><td>" + vehicle.getName() + "</td><td>" + distance + "</td><td>" + lastComm + "</td><td>"
                    + nextComm + "</td><td>" + fuel + "</td><td>" + matchPlan + "</td></tr>\n");
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
}
