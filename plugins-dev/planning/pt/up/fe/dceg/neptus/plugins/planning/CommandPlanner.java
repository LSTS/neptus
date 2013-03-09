/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 28/04/2010
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Image;
import java.awt.event.ActionEvent;
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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.editor.SpeedUnitsEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PlanControl;
import pt.up.fe.dceg.neptus.imc.PlanControl.OP;
import pt.up.fe.dceg.neptus.imc.PlanControl.TYPE;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.mp.maneuvers.StationKeeping;
import pt.up.fe.dceg.neptus.mp.templates.PlanCreator;
import pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension;
import pt.up.fe.dceg.neptus.planeditor.IMapPopup;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.IMCSendMessageUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem.IMCAuthorityState;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Command Planner", author = "zp, pdias", icon = "pt/up/fe/dceg/neptus/plugins/kenolaba.png", version = "1.5", category = CATEGORY.PLANNING)
public class CommandPlanner extends SimpleSubPanel implements IEditorMenuExtension, NeptusMessageListener {

    private static final String PLAN_PREFIX = "";

    // private static final String[] SPEED_UNITS = new String[] {"RPM", "m/s", "%"};

    @NeptusProperty(name = "AUV travelling depth", category = "AUV", description = "Use 0 for going at surface", userLevel = LEVEL.REGULAR)
    public double auvDepth = 0;

    @NeptusProperty(name = "AUV travelling speed", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvSpeed = 1000;

    @NeptusProperty(name = "AUV travelling speed units", category = "AUV", editorClass = SpeedUnitsEditor.class, userLevel = LEVEL.REGULAR)
    public String auvSpeedUnits = "RPM";

    @NeptusProperty(name = "AUV loiter radius", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvLoiterRadius = 20;

    @NeptusProperty(name = "AUV Station Keeping radius", category = "AUV", userLevel = LEVEL.REGULAR)
    public double auvSkRadius = 15;

    @NeptusProperty(name = "UAV flying altitude", category = "UAV", userLevel = LEVEL.REGULAR)
    public double uavAltitude = 200;

    @NeptusProperty(name = "UAV flying speed", category = "UAV", userLevel = LEVEL.REGULAR)
    public double uavSpeed = 18;

    @NeptusProperty(name = "UAV flying speed units", category = "UAV", editorClass = SpeedUnitsEditor.class, userLevel = LEVEL.REGULAR)
    public String uavSpeedUnits = "m/s";

    @NeptusProperty(name = "UAV loiter radius", category = "UAV", userLevel = LEVEL.REGULAR)
    public double uavLoiterRadius = 180;

    @NeptusProperty(name = "ASV speed", category = "ASV", userLevel = LEVEL.REGULAR)
    public double asvSpeed = 1000;

    @NeptusProperty(name = "ASV speed units", category = "ASV", editorClass = SpeedUnitsEditor.class, userLevel = LEVEL.REGULAR)
    public String asvSpeedUnits = "RPM";

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
            if (veh[i].getAuthorityState().ordinal() < IMCAuthorityState.SYSTEM_FULL.ordinal())
                continue;

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

            boolean ok = false;
            if (v.getFeasibleManeuvers().containsValue(Goto.class.getName())) {
                menu.add(new AbstractAction(I18n.text("Go here")) {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        new Thread() {
                            @Override
                            public void run() {
                                double speed = 0;
                                double z = 0;
                                ManeuverLocation.Z_UNITS zunits = Z_UNITS.NONE;
                                String speedUnit = "RPM";

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
                                    z = uavAltitude;
                                    zunits = ManeuverLocation.Z_UNITS.ALTITUDE;
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
                                creator.addManeuver("Goto", "speed", speed, "speedUnits", speedUnit);
                                PlanType plan = creator.getPlan();
                                plan.setVehicle(v);
                                plan.setId(PLAN_PREFIX + "go_" + plan.getId());
                                getConsole().getMission().addPlan(plan);
                                getConsole().updateMissionListeners();
                                getConsole().setPlan(plan);

                                startPlan(plan, false);
                            }
                        }.start();
                    }
                });
                ok = true;
            }

            if (v.getFeasibleManeuvers().containsValue(StationKeeping.class.getName())) {
                menu.add(new AbstractAction(I18n.text("Surface here")) {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        new Thread() {
                            @Override
                            public void run() {

                                double speed = 0;
                                String speedUnit = "RPM";
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
                                creator.addManeuver("StationKeeping", "speed", speed, "speedUnits", speedUnit,
                                        "duration", duration, "radius", radius);
                                PlanType plan = creator.getPlan();
                                plan.setVehicle(v);
                                plan.setId(PLAN_PREFIX + "sk_" + plan.getId());
                                getConsole().getMission().addPlan(plan);
                                getConsole().updateMissionListeners();
                                getConsole().setPlan(plan);

                                startPlan(plan, false);
                            }
                        }.start();
                    }
                });
                ok = true;
            }

            if (v.getFeasibleManeuvers().containsValue(Loiter.class.getName())) {
                menu.add(new AbstractAction(I18n.text("Loiter here")) {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        new Thread() {
                            @Override
                            public void run() {

                                double speed = 0;
                                String speedUnit = "RPM";
                                double radius = 10;
                                int duration = 0;
                                double z = 0;
                                Z_UNITS zunits = Z_UNITS.NONE;

                                if ("auv".equalsIgnoreCase(v.getType()) || "uuv".equalsIgnoreCase(v.getType())) {
                                    speed = auvSpeed;
                                    speedUnit = auvSpeedUnits;
                                    radius = auvLoiterRadius;
                                    z = auvDepth;
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
                                    z = uavAltitude;
                                    zunits = Z_UNITS.ALTITUDE;
                                }
                                else {
                                    return;
                                }

                                PlanCreator creator = new PlanCreator(getConsole().getMission());
                                creator.setLocation(target);
                                creator.setZ(z, zunits);
                                creator.addManeuver("Loiter", "speed", speed, "speedUnits", speedUnit, "loiterDuration",
                                        duration, "radius", radius);
                                PlanType plan = creator.getPlan();
                                plan.setVehicle(v);
                                plan.setId(PLAN_PREFIX + "lt_" + plan.getId());
                                getConsole().getMission().addPlan(plan);
                                getConsole().updateMissionListeners();
                                getConsole().setPlan(plan);

                                startPlan(plan, false);
                            }
                        }.start();
                    }
                });
                ok = true;
            }

            if (ok)
                items.add(menu);

        }

        return items;
    }

    protected void startPlan(PlanType plan, boolean calibrate) {
        
        PlanControl startPlan = new PlanControl();
        startPlan.setType(TYPE.REQUEST);
        startPlan.setOp(OP.START);
        startPlan.setArg(IMCUtils.generatePlanSpecification(plan));
        startPlan.setPlanId(plan.getId());
        int reqId = IMCSendMessageUtils.getNextRequestId();
        startPlan.setRequestId(reqId);
        if (calibrate)
            startPlan.setFlags(PlanControl.FLG_CALIBRATE);
        else
            startPlan.setFlags(0);
        
        boolean ret = IMCSendMessageUtils.sendMessage(startPlan, CommandPlanner.this,
                "Error starting " + plan.getId() + " plan", false, true, plan.getVehicle());
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