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
 * Apr 5, 2018
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Right-Click Planner")
public class RightClickPlanner extends ConsolePanel implements IEditorMenuExtension {

    private static final long serialVersionUID = -2286768761365602657L;
    private final HashMap<String, ImageIcon> vehIconPool = new HashMap<String, ImageIcon>();
    private final LinkedHashMap<Integer, Long> registerRequestIdsTime = new LinkedHashMap<Integer, Long>();

    @NeptusProperty(category="Behavior", name = "End Behavior", userLevel = LEVEL.REGULAR)
    public BEHAVIOR endBehavior = BEHAVIOR.StationKeeping;

    @NeptusProperty(name = "Depth", description = "Use 0 for going at surface", userLevel = LEVEL.REGULAR)
    public double depth = 0;

    @NeptusProperty(name = "Speed", userLevel = LEVEL.REGULAR)
    public SpeedType speed = new SpeedType(1.3, Units.MPS);

    @NeptusProperty(category="Loiter Specific", name = "Loiter Duration", description = "Duration, in seconds, for loitering behavior", userLevel = LEVEL.REGULAR)
    public int duration = 300;

    @NeptusProperty(category="Loiter Specific", name = "Loiter Radius", description = "Loitering radius, in meters", userLevel = LEVEL.REGULAR)
    public int radius = 20;

    @NeptusProperty(category="Behavior", name = "Ignore errors", description = "Ignore any errors during execution", userLevel = LEVEL.REGULAR)
    public boolean ignoreErrors = false;

    enum BEHAVIOR {
        Stop,
        StationKeeping,
        Loiter
    }

    public RightClickPlanner(ConsoleLayout console) {
        super(console);
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

        for (VehicleType v : avVehicles) {
            JMenuItem menu = new JMenuItem(I18n.textf("Command %vehicle", v.getId()));
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
            menu.addActionListener(a -> commandVehicle(v, loc, source.getRenderer()));
            items.add(menu);
        }

        return items;

    }

    private void commandVehicle(VehicleType v, LocationType loc, StateRenderer2D renderer) {
        PointRenderer r = new PointRenderer(ImcSystemsHolder.lookupSystemByName(v.getName()), loc);
        renderer.addPostRenderPainter(r, "point renderer");

        boolean cancelled = PropertiesEditor.editProperties(new PropertiesProvider() {
            public void setProperties(Property[] properties) {
                PluginUtils.setPluginProperties(RightClickPlanner.this, properties);
            }

            public String[] getPropertiesErrors(Property[] properties) {
                return null;
            }

            public String getPropertiesDialogTitle() {
                return "Command " + v.getId();
            }

            public DefaultProperty[] getProperties() {
                return PluginUtils.getPluginProperties(RightClickPlanner.this, true);
            }
        }, getConsole(), true);

        if (!cancelled) {
            PlanCreator creator = new PlanCreator(getConsole().getMission());
            creator.setLocation(loc);
            creator.setZ(depth, Z_UNITS.DEPTH);
            creator.setSpeed(speed);
            creator.addManeuver("Goto");

            switch (endBehavior) {
                case Loiter:
                    creator.setSpeed(speed);
                    creator.addManeuver("Loiter", "loiterDuration", duration,
                            "radius", radius);
                    /* FALLTHROUGH */
                case StationKeeping:
                    creator.setZ(0, Z_UNITS.DEPTH);
                    creator.setSpeed(speed);
                    creator.addManeuver("StationKeeping", "duration", 0);
                    break;
                default:
                    break;
            }

            PlanType plan = creator.getPlan();
            plan.setVehicle(v);
            plan.setId("cmd-" + v);
            plan = addPlanToMission(plan);
            startPlan(plan, false, ignoreErrors);
        }

        renderer.removePostRenderPainter(r);
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

        startPlan.setFlags(
                (calibrate ? PlanControl.FLG_CALIBRATE : 0) | (ignoreErrors ? PlanControl.FLG_IGNORE_ERRORS : 0));

        boolean ret = IMCSendMessageUtils.sendMessage(startPlan, RightClickPlanner.this,
                "Error starting " + plan.getId() + " plan", false, true, true, plan.getVehicle());
        if (ret)
            registerPlanControlRequest(reqId);
    }

    /**
     * @param reqId
     */
    private void registerPlanControlRequest(int reqId) {
        registerRequestIdsTime.put(reqId, System.currentTimeMillis());
    }

    private class PointRenderer implements Renderer2DPainter {

        private LocationType loc;
        private ImcSystem v;
        public PointRenderer(ImcSystem v, LocationType loc) {
            this.loc = loc;
            this.v = v;
        }

        @Override
        public void paint(Graphics2D g2, StateRenderer2D renderer) {
            Point2D pt = renderer.getScreenPosition(loc);
            Graphics2D g = (Graphics2D) g2.create();
            g.setStroke(new BasicStroke(2.0f));
            
            if (v.getLocation() != null && v.getLocationTimeMillis() != -1) {
                Point2D origin = renderer.getScreenPosition(v.getLocation());
                g.setColor(Color.GREEN.darker());
                g.draw(new Line2D.Double(origin, pt));
                
                g.translate(pt.getX(), pt.getY());
                g.rotate(loc.getXYAngle(v.getLocation()));
                GeneralPath gp = new GeneralPath();
                gp.moveTo(-7, -10);
                gp.lineTo(0, 0);
                gp.lineTo(7, -10);
                gp.closePath();
                g.fill(gp);
            }
            
            g = (Graphics2D) g2.create();
            g.setColor(Color.red.darker());
            g.setStroke(new BasicStroke(2.0f));
            g.draw(new Line2D.Double(pt.getX() - 5, pt.getY() - 5, pt.getX() + 5, pt.getY() + 5));
            g.draw(new Line2D.Double(pt.getX() - 5, pt.getY() + 5, pt.getX() + 5, pt.getY() - 5));
        }
    }

    @Subscribe
    public void on(PlanControl pc) {
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
