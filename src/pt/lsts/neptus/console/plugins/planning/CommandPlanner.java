/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 28/04/2010
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.editor.SpeedUnitsEnumEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Command Planner", author = "zp, pdias", icon = "pt/lsts/neptus/console/plugins/kenolaba.png", version = "1.5", category = CATEGORY.PLANNING)
public class CommandPlanner extends ConsolePanel implements IEditorMenuExtension, NeptusMessageListener {

    @NeptusProperty(name = "AUV travelling depth", category = "AUV", description = "Use 0 for going at surface", userLevel = LEVEL.REGULAR)
    public double auvDepth = 0;

    @NeptusProperty(name = "AUV travelling speed", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvSpeed = 1000;

    @NeptusProperty(name = "AUV travelling speed units", category = "AUV", editorClass = SpeedUnitsEnumEditor.class, userLevel = LEVEL.REGULAR)
    public Maneuver.SPEED_UNITS auvSpeedUnits = SPEED_UNITS.RPM;

    @NeptusProperty(name = "AUV loiter depth", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvLtDepth = 3;
    
    @NeptusProperty(name = "AUV loiter duration in seconds", category = "AUV", userLevel = LEVEL.REGULAR)
    public int auvLtDuration = 300;
        
    @NeptusProperty(name = "AUV loiter radius", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvLoiterRadius = 20;

    @NeptusProperty(name = "AUV Station Keeping radius", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvSkRadius = 15;
    
    @NeptusProperty(name = "UAV Z", category = "UAV", userLevel = LEVEL.REGULAR)
    public double uavZ = 200;

    @NeptusProperty(name = "UAV Z Reference", category = "UAV", userLevel = LEVEL.REGULAR)
    private pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS uavZUnits = ManeuverLocation.Z_UNITS.HEIGHT;

    @NeptusProperty(name = "UAV flying speed", category = "UAV", userLevel = LEVEL.REGULAR)
    public double uavSpeed = 18;

    @NeptusProperty(name = "UAV flying speed units", category = "UAV", editorClass = SpeedUnitsEnumEditor.class, userLevel = LEVEL.REGULAR)
    public Maneuver.SPEED_UNITS uavSpeedUnits = SPEED_UNITS.METERS_PS;

    @NeptusProperty(name = "UAV loiter radius", category = "UAV", userLevel = LEVEL.REGULAR)
    public double uavLoiterRadius = 180;

    @NeptusProperty(name = "ASV speed", category = "ASV", userLevel = LEVEL.REGULAR)
    public double asvSpeed = 1000;

    @NeptusProperty(name = "ASV speed units", category = "ASV", editorClass = SpeedUnitsEnumEditor.class, userLevel = LEVEL.REGULAR)
    public Maneuver.SPEED_UNITS asvSpeedUnits = SPEED_UNITS.RPM;

    @NeptusProperty(name = "ASV Station Keeping radius", category = "ASV", description = "Radius of the circle where the ASV should keep its position", userLevel = LEVEL.REGULAR)
    public double asvSkRadius = 20;

    @NeptusProperty(name = "ASV loiter radius", category = "ASV", userLevel = LEVEL.REGULAR)
    public double asvLoiterRadius = 30;

    private final HashMap<String, ImageIcon> vehIconPool = new HashMap<String, ImageIcon>();

    private final LinkedHashMap<Integer, Long> registerRequestIdsTime = new LinkedHashMap<Integer, Long>();
    private final String[] messagesToObserve = new String[] { "PlanControl" };

    public CommandPlanner(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {
        Vector<IMapPopup> r = getConsole().getSubPanelsOfInterface(IMapPopup.class);
        for (IMapPopup str2d : r) {
            str2d.addMenuExtension(this);
        }

    }

    @Override
    public void cleanSubPanel() {
        Vector<IMapPopup> r = getConsole().getSubPanelsOfInterface(IMapPopup.class);
        for (IMapPopup str2d : r) {
            str2d.removeMenuExtension(this);
        }
    }

    @Override
    public String[] getObservedMessages() {
        return messagesToObserve;
    }

    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {

        Vector<VehicleType> avVehicles = new Vector<VehicleType>();

        ImcSystem[] veh = ImcSystemsHolder.lookupActiveSystemVehicles();

        // ordenar com auth
        Arrays.sort(veh, new Comparator<ImcSystem>() {
            @Override
            public int compare(ImcSystem o1, ImcSystem o2) {
                // Comparison if authority option and only one has it
                if ((o1.isWithAuthority() ^ o2.isWithAuthority()))
                    return o1.isWithAuthority() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

                // Comparison if authority option and the levels are different
                if ((o1.getAuthorityState() != o2.getAuthorityState()))
                    return o2.getAuthorityState().ordinal() - o1.getAuthorityState().ordinal();

                return o1.compareTo(o2);
            }
        });

        for (int i = 0; i < veh.length; i++) {
            VehicleType vehicle = VehiclesHolder.getVehicleWithImc(veh[i].getId());
            if (vehicle != null) {
                if (getConsole().getMainSystem() != null
                        && getConsole().getMainSystem().equalsIgnoreCase(vehicle.getId()))
                    avVehicles.add(0, vehicle);
                else
                    avVehicles.add(vehicle);
            }
        }

        if (getConsole().getMainSystem() != null)
            if (!avVehicles.contains(VehiclesHolder.getVehicleById(getConsole().getMainSystem())))
                avVehicles.add(0, VehiclesHolder.getVehicleById(getConsole().getMainSystem()));

        Vector<JMenuItem> items = new Vector<JMenuItem>();
        final LocationType target = loc;
        for (final VehicleType v : avVehicles) {
            JMenu menu = new JMenu(I18n.textf("Command %vehicle", v.getId()));
            ImageIcon vicon;
            if (vehIconPool.containsKey(v.getId())) {
                vicon = vehIconPool.get(v.getId());
            }
            else {
                String imgFile;
                if (!v.getPresentationImageHref().equalsIgnoreCase(""))
                    imgFile = v.getPresentationImageHref();
                else
                    imgFile = v.getSideImageHref();
                Image vimg = new ImageIcon(imgFile).getImage();
                vicon = new ImageIcon(vimg.getScaledInstance(40, -1, Image.SCALE_SMOOTH));
                vehIconPool.put(v.getId(), vicon);
            }

            menu.setIcon(vicon);

            String loiterSettings = "(";
            String settings = "(";
            NumberFormat nf = GuiUtils.getNeptusDecimalFormat(1);
            switch (v.getType().toLowerCase()) {
                case "auv":
                case "uuv":
                    loiterSettings += "D="+nf.format(auvLtDepth);
                    loiterSettings += " / S="+nf.format(auvSpeed)+" "+auvSpeedUnits.getString()+")";
                    settings += "D="+nf.format(auvDepth);
                    settings += " / S="+nf.format(auvSpeed)+" "+auvSpeedUnits.getString()+")";                    
                    break;
                case "asv":
                case "usv":
                    settings += " / S="+nf.format(asvSpeed)+" "+asvSpeedUnits.getString()+")";                    
                    break;
                case "uav":
                    settings += uavZUnits.name().substring(0, 1)+"="+nf.format(uavZ) ;
                    settings += " S="+nf.format(uavSpeed)+" "+uavSpeedUnits.getString()+")";     
                    break;
                default:
                    break;
            }
            
            
            boolean ok = false;
            if (v.getFeasibleManeuvers().containsValue(Goto.class.getName())) {
                menu.add(new AbstractAction(I18n.textf("Go here %settings", settings)) {
                    @Override
                    public void actionPerformed(final ActionEvent arg0) {
                        new Thread() {
                            @Override
                            public void run() {
                                double speed = 0;
                                double z = 0;
                                ManeuverLocation.Z_UNITS zunits = Z_UNITS.NONE;
                                Maneuver.SPEED_UNITS speedUnit = SPEED_UNITS.RPM;

                                PlanCreator creator = new PlanCreator(getConsole().getMission());
                                creator.setLocation(target);
                                if ("auv".equalsIgnoreCase(v.getType()) || "uuv".equalsIgnoreCase(v.getType())) {
                                    speed = auvSpeed;
                                    z = auvDepth;
                                    zunits = ManeuverLocation.Z_UNITS.DEPTH;
                                    speedUnit = auvSpeedUnits;
                                    creator.setZ(z, zunits);
                                }
                                else if ("uav".equalsIgnoreCase(v.getType())) {
                                    speed = uavSpeed;
                                    z = uavZ;
                                    zunits = uavZUnits;
                                    speedUnit = uavSpeedUnits;
                                    creator.setZ(z, zunits);
                                }
                                else if ("asv".equalsIgnoreCase(v.getType()) || "usv".equalsIgnoreCase(v.getType())) {
                                    speed = asvSpeed;
                                    z = 0;
                                    zunits = ManeuverLocation.Z_UNITS.DEPTH;
                                    speedUnit = asvSpeedUnits;
                                    creator.setZ(z, zunits);
                                }
                                else {
                                    NeptusLog.pub().error("error sending goto ");
                                    return;
                                }
                                creator.setZ(z, zunits);
                                creator.addManeuver("Goto", "speed", speed, "speedUnits", speedUnit.getString());
                                PlanType plan = creator.getPlan();
                                plan.setVehicle(v);
                                plan.setId("cmd-"+v);
                                plan = addPlanToMission(plan);
                                startPlan(plan, false, (arg0.getModifiers() & ActionEvent.CTRL_MASK) != 0);
                            }
                        }.start();
                    }
                });
                ok = true;
            }

            if (v.getFeasibleManeuvers().containsValue(StationKeeping.class.getName())) {
                menu.add(new AbstractAction(I18n.textf("Surface here %settings", settings)) {
                    @Override
                    public void actionPerformed(final ActionEvent arg0) {
                        new Thread() {
                            @Override
                            public void run() {

                                double speed = 0;
                                Maneuver.SPEED_UNITS speedUnit = SPEED_UNITS.RPM;
                                double radius = 10;
                                int duration = 0;
                                if ("auv".equalsIgnoreCase(v.getType()) || "uuv".equalsIgnoreCase(v.getType())) {
                                    speed = auvSpeed;
                                    speedUnit = auvSpeedUnits;
                                    radius = auvSkRadius;
                                }
                                else if ("asv".equalsIgnoreCase(v.getType()) || "usv".equalsIgnoreCase(v.getType())) {
                                    speed = asvSpeed;
                                    speedUnit = asvSpeedUnits;
                                    radius = asvSkRadius;
                                }
                                else {
                                    return;
                                }

                                PlanCreator creator = new PlanCreator(getConsole().getMission());
                                creator.setLocation(target);
                                creator.setZ(0, Z_UNITS.DEPTH);
                                creator.addManeuver("StationKeeping", "speed", speed, "speedUnits", speedUnit.getString(),
                                        "duration", duration, "radius", radius);
                                PlanType plan = creator.getPlan();
                                plan.setVehicle(v);
                                plan.setId("cmd-"+v);
                                plan = addPlanToMission(plan);
                                startPlan(plan, false, (arg0.getModifiers() & ActionEvent.CTRL_MASK) != 0);
                            }
                        }.start();
                    }
                });
                ok = true;
            }

            if (v.getFeasibleManeuvers().containsValue(Loiter.class.getName())) {
                menu.add(new AbstractAction(I18n.textf("Loiter here, %settings", loiterSettings)) {
                    @Override
                    public void actionPerformed(final ActionEvent arg0) {
                        new Thread() {
                            @Override
                            public void run() {

                                double speed = 0;
                                Maneuver.SPEED_UNITS speedUnit = SPEED_UNITS.RPM;
                                double radius = 10;
                                int duration = 0;
                                double z = 0;
                                Z_UNITS zunits = Z_UNITS.NONE;

                                if ("auv".equalsIgnoreCase(v.getType()) || "uuv".equalsIgnoreCase(v.getType())) {
                                    speed = auvSpeed;
                                    speedUnit = auvSpeedUnits;
                                    radius = auvLoiterRadius;
                                    z = auvLtDepth;
                                    duration = auvLtDuration;
                                    zunits = Z_UNITS.DEPTH;
                                }
                                else if ("asv".equalsIgnoreCase(v.getType()) || "usv".equalsIgnoreCase(v.getType())) {
                                    speed = asvSpeed;
                                    speedUnit = asvSpeedUnits;
                                    radius = asvLoiterRadius;
                                    z = 0;
                                    zunits = Z_UNITS.DEPTH;
                                }
                                else if ("uav".equalsIgnoreCase(v.getType())) {
                                    speed = uavSpeed;
                                    speedUnit = uavSpeedUnits;
                                    radius = uavLoiterRadius;
                                    z = uavZ;
                                    zunits = uavZUnits;
                                }
                                else {
                                    return;
                                }

                                PlanCreator creator = new PlanCreator(getConsole().getMission());
                                creator.setLocation(target);
                                creator.setZ(z, zunits);
                                creator.addManeuver("Loiter", "speed", speed, "speedUnits", speedUnit.getString(),
                                        "loiterDuration", duration, "radius", radius);
                                PlanType plan = creator.getPlan();
                                plan.setVehicle(v);
                                plan.setId("cmd-"+v);
                                plan = addPlanToMission(plan);
                                startPlan(plan, false, (arg0.getModifiers() & ActionEvent.CTRL_MASK) != 0);
                            }
                        }.start();
                    }
                });
                ok = true;
            }

            if (ok) {
                menu.addSeparator();
                menu.add(new AbstractAction(I18n.text("Change settings")) {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PluginUtils.editPluginProperties(CommandPlanner.this, getConsole(), true);
                    }
                });
                items.add(menu);
            }

        }

        return items;
    }

    private PlanType addPlanToMission(PlanType plan) {
        final String planId = plan.getId();
        getConsole().getMission().addPlan(plan);
        getConsole().getMission().save(true);
        getConsole().updateMissionListeners();
        PlanType result = getConsole().getMission().getIndividualPlansList().get(planId);
        getConsole().setPlan(result);

        return result;
    }

    protected void startPlan(PlanType plan, boolean calibrate, boolean ignoreErrors) {

        PlanControl startPlan = new PlanControl();
        startPlan.setType(TYPE.REQUEST);
        startPlan.setOp(OP.START);
        startPlan.setPlanId(plan.getId());
        startPlan.setArg(plan.asIMCPlan(true));
        int reqId = IMCSendMessageUtils.getNextRequestId();
        startPlan.setRequestId(reqId);

        startPlan.setFlags((calibrate ? PlanControl.FLG_CALIBRATE : 0)
                | (ignoreErrors ? PlanControl.FLG_IGNORE_ERRORS : 0));

        boolean ret = IMCSendMessageUtils.sendMessage(startPlan, CommandPlanner.this, "Error starting " + plan.getId()
                + " plan", false, true, true, plan.getVehicle());
        if (ret)
            registerPlanControlRequest(reqId);
    }

    /**
     * @param reqId
     */
    private void registerPlanControlRequest(int reqId) {
        registerRequestIdsTime.put(reqId, System.currentTimeMillis());
    }

    @Override
    public void messageArrived(IMCMessage message) {

        if (message.getMgid() == PlanControl.ID_STATIC) {
            PlanControl pc = (PlanControl) message;
            try {
                PlanControl.TYPE typeId = pc.getType();
                if (typeId != TYPE.REQUEST) {
                    int reqId = pc.getRequestId();
                    if (registerRequestIdsTime.containsKey(reqId)) {
                        boolean cleanReg = false;

                        if (typeId == TYPE.SUCCESS)
                            cleanReg = true;
                        else if (typeId == TYPE.FAILURE) {
                            cleanReg = true;
                        }
                        if (cleanReg)
                            registerRequestIdsTime.remove(reqId);

                    }
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }
}