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
 * Jun 4, 2013
 */
package pt.lsts.neptus.plugins.followref;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.FollowReference;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.Reference;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "FollowReference Interaction", category = CATEGORY.PLANNING, icon = "pt/lsts/neptus/plugins/followref/geolocation.png")
public class FollowReferenceInteraction extends SimpleRendererInteraction implements IPeriodicUpdates,
        ConfigurationListener {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, FollowRefState> frefStates = new LinkedHashMap<>();
    protected LinkedHashMap<String, ReferencePlan> plans = new LinkedHashMap<>();
    protected LinkedHashMap<String, EstimatedState> states = new LinkedHashMap<>();
    protected HashSet<String> activeVehicles = new HashSet<>();
    protected ReferenceWaypoint movingWaypoint = null;
    protected ReferenceWaypoint focusedWaypoint = null;
    protected double radius = 8;
    protected int entity = 255;

    private String helpMsg;

    @NeptusProperty(name = "Use acoustic communications", description = "Setting to true will make all communications go through acoustic modem")
    public boolean useAcousticCommunications = false;

    @NeptusProperty(name = "Control loop latency", description = "Ammount of seconds between reference transmissions (per controller vehicle)")
    public long controlLoopLatencySecs = 1;

    @NeptusProperty(name = "Control timeout", description = "Ammount of seconds after which the controlled vehicle will timeout if no new reference updates are received")
    public long referenceTimeout = 30;

    public FollowReferenceInteraction(ConsoleLayout cl) {
        super(cl);
        setHelpMsg();
    }

    @Override
    public long millisBetweenUpdates() {
        return controlLoopLatencySecs * 1000;
    }

    @Override
    public void propertiesChanged() {

    }

    @Override
    public boolean update() {
        Vector<String> copy = new Vector<>();
        copy.addAll(activeVehicles);

        for (String v : copy) {
            if (useAcousticCommunications) {
                if (states.containsKey(v)) {
                    boolean prox = false;
                    LocationType loc = IMCUtils.getLocation(states.get(v));
                    prox = plans.get(v).currentWaypoint().getManeuverLocation().getDistanceInMeters(loc) < radius + 4;
                    if (prox) {
                        ReferenceWaypoint wpt = plans.get(v).popFirstWaypoint();
                        if (focusedWaypoint.equals(wpt))
                            focusedWaypoint = null;
                    }
                }
            }
            else if (frefStates.containsKey(v)) {
                int prox = frefStates.get(v).getProximity();
                ReferenceWaypoint wpt = plans.get(v).currentWaypoint();
                if ((prox & FollowRefState.PROX_XY_NEAR) != 0 && (prox & FollowRefState.PROX_Z_NEAR) != 0) {
                    if (wpt.time != -1) {
                        if (wpt.time > 0) {
                            if (Double.isNaN(wpt.timeLeft()))
                                wpt.setStartTime(System.currentTimeMillis() / 1000.0);
                            else if (wpt.timeLeft() <= 0)
                                plans.get(v).popFirstWaypoint();
                        }
                        else
                            plans.get(v).popFirstWaypoint();
                    }
                }
                else {
                    wpt.setStartTime(Double.NaN);
                }
            }
            if (plans.containsKey(v)) {
                if (useAcousticCommunications) {
                    AcousticOperation op = new AcousticOperation();
                    op.setOp(AcousticOperation.OP.MSG);
                    op.setSystem(v);
                    op.setMsg(plans.get(v).currentWaypoint().getReference());

                    ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                            SystemTypeEnum.ALL, true);

                    if (sysLst.length == 0) {
                        NeptusLog.pub().error("Cannot send reference acoustically because no system is capable of it");
                        return true;
                    }

                    int successCount = 0;

                    for (ImcSystem sys : sysLst) {
                        if (ImcMsgManager.getManager().sendMessage(op, sys.getId(), null)) {
                            successCount++;
                            NeptusLog.pub().warn("Sent reference to " + v + " acoustically via " + sys.getName());
                        }
                    }
                    if (successCount == 0) {
                        NeptusLog.pub().error("Cannot send reference acoustically because no system is capable of it");
                    }
                }
                else {
                    send(v, plans.get(v).currentWaypoint().getReference());
                }
            }
        }
        return true;
    }

    @Subscribe
    public void on(EstimatedState state) {
        states.put(state.getSourceName(), state);
    }

    @Subscribe
    public void on(PlanControlState controlState) {

        
        if (controlState.getPlanId() == null || controlState.getPlanId().isEmpty())
            return;
        
        // Check if we have already received a FollowReferenceState
        if (!frefStates.containsKey(controlState.getSourceName()))
            return;

        // Check if vehicle is being controlled by this console
        if (frefStates.get(controlState.getSourceName()).getControlSrc() != ImcMsgManager.getManager().getLocalId()
                .intValue()) {
            // we are not controlling anymore.
            if (activeVehicles.remove(controlState.getSourceName())) {
                post(Notification.warning("FollowReferenceInteraction", "The vehicle " + controlState.getSourceName()
                        + " is not being controlled anymore."));                
            }
            frefStates.remove(controlState.getSourceName());
            return;
        }

        boolean newActivation = false;
        if (controlState.getPlanId().equals("follow_neptus")
                && controlState.getState() == PlanControlState.STATE.EXECUTING) {

            newActivation = activeVehicles.add(controlState.getSourceName());

            if (newActivation) {
                Reference ref = new Reference();
                EstimatedState lastState = states.get(controlState.getSourceName());
                if (lastState == null)
                    return;
                LocationType loc = new LocationType(Math.toDegrees(lastState.getLat()), Math.toDegrees(lastState
                        .getLon()));
                loc.translatePosition(lastState.getX(), lastState.getY(), 0);

                loc.convertToAbsoluteLatLonDepth();
                ref.setLat(loc.getLatitudeRads());
                ref.setLon(loc.getLongitudeRads());
                ref.setFlags((Reference.FLAG_LOCATION /* | Reference.FLAG_SPEED | Reference.FLAG_Z */));

                ReferencePlan plan = new ReferencePlan(controlState.getSourceName());
                plan.addWaypointAtEnd(ref);
                plans.put(controlState.getSourceName(), plan);
            }
        }
        else if (activeVehicles.contains(controlState.getSourceName())) {
            activeVehicles.remove(controlState.getSourceName());
            plans.remove(controlState.getSourceName());
        }
    }

    @Subscribe
    public void on(FollowRefState frefState) {
        frefStates.put(frefState.getSourceName(), frefState);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        try {
            Vector<ReferenceWaypoint> wpts = new Vector<>();

            for (ReferencePlan p : plans.values()) {
                wpts.clear();
                wpts.addAll(p.getWaypoints());

                for (int i = 1; i < wpts.size(); i++) {
                    Reference prevRef = wpts.get(i - 1).getReference();
                    LocationType prevLoc = new LocationType(Math.toDegrees(prevRef.getLat()), Math.toDegrees(prevRef
                            .getLon()));
                    Point2D prevPt = renderer.getScreenPosition(prevLoc);
                    Reference ref = wpts.get(i).getReference();
                    LocationType loc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
                    Point2D pt = renderer.getScreenPosition(loc);
                    Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2,
                            radius * 2);
                    g.setColor(Color.blue);
                    g.setStroke(new BasicStroke(3f));
                    g.draw(new Line2D.Double(prevPt, pt));
                    g.fill(ellis);
                    if (ref.getZ() != null) {
                        g.setStroke(new BasicStroke(2f));
                        g.setColor(Color.white);
                        switch (ref.getZ().getZUnits()) {
                            case DEPTH:
                                g.draw(new Line2D.Double(pt.getX() - radius, pt.getY() - radius, pt.getX() + radius, pt
                                        .getY() - radius));
                                break;
                            case ALTITUDE:
                            case HEIGHT:
                                g.draw(new Line2D.Double(pt.getX() - radius, pt.getY() + radius, pt.getX() + radius, pt
                                        .getY() + radius));
                                break;
                            default:
                                break;
                        }
                    }
                    g.setColor(Color.black);
                }
            }

            for (ReferencePlan p : plans.values()) {
                ReferenceWaypoint wpt = p.currentWaypoint();
                Reference ref = wpt.getReference();
                FollowRefState lastFrefState = frefStates.get(p.system_id);
                if (ref != null && lastFrefState != null) {
                    Color c = Color.red;
                    if (lastFrefState != null) {
                        if (lastFrefState.getReference() != null && ref.getLat() == lastFrefState.getReference().getLat()
                                && ref.getLon() == lastFrefState.getReference().getLon())
                            c = Color.green;
                    }
                    LocationType loc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
                    Point2D pt = renderer.getScreenPosition(loc);
                    Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2,
                            radius * 2);
                    g.setColor(c);
                    g.fill(ellis);
                    if (ref.getZ() != null) {
                        g.setStroke(new BasicStroke(2f));
                        g.setColor(Color.white);
                        switch (ref.getZ().getZUnits()) {
                            case DEPTH:
                                g.draw(new Line2D.Double(pt.getX() - radius, pt.getY() - radius, pt.getX() + radius, pt
                                        .getY() - radius));
                                break;
                            case ALTITUDE:
                            case HEIGHT:
                                g.draw(new Line2D.Double(pt.getX() - radius, pt.getY() + radius, pt.getX() + radius, pt
                                        .getY() + radius));
                                break;
                            default:
                                break;
                        }
                    }
                    if (wpt.loiter) {
                        g.setStroke(new BasicStroke(1.5f));
                        g.setColor(new Color(255, 255, 255, 128));
                        double radius = Math.abs(wpt.loiterRadius * renderer.getZoom());
                        g.draw(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
                    }
                }
            }

            if (focusedWaypoint != null) {
                g.setStroke(new BasicStroke(2f));

                Reference ref = focusedWaypoint.getReference();
                LocationType loc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
                Point2D pt = renderer.getScreenPosition(loc);
                Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2);
                g.setColor(Color.white);
                g.draw(ellis);
                int pos = 5;
                if (ref.getZ() != null) {
                    g.drawString(ref.getZ().getZUnits().toString().toLowerCase() + ": " + ref.getZ().getValue() + " m",
                            (int) pt.getX() + 15, (int) pt.getY() + pos);
                    pos += 15;
                }
                if (ref.getSpeed() != null) {
                    g.drawString("speed: " + ref.getSpeed().getValue(), (int) pt.getX() + 15, (int) pt.getY() + pos);
                    pos += 15;
                }
                if (!Double.isNaN(focusedWaypoint.timeLeft())) {
                    g.drawString(
                            "time left: "
                                    + GuiUtils.getNeptusDecimalFormat(0)
                                            .format(Math.max(0, focusedWaypoint.timeLeft())), (int) pt.getX() + 15,
                            (int) pt.getY() + pos);
                    pos += 15;
                }
                else if (focusedWaypoint.time > 0) {
                    g.drawString("time: " + GuiUtils.getNeptusDecimalFormat(0).format(focusedWaypoint.time),
                            (int) pt.getX() + 15, (int) pt.getY() + pos);
                    pos += 15;
                }
                else if (focusedWaypoint.time == -1) {
                    g.drawString("time: \u221e", (int) pt.getX() + 15, (int) pt.getY() + pos);
                    pos += 15;
                }

                if (focusedWaypoint.loiter) {
                    g.setStroke(new BasicStroke(2f));
                    g.setColor(new Color(255, 255, 255, 128));
                    double radius = Math.abs(focusedWaypoint.loiterRadius * renderer.getZoom());
                    g.draw(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
                }

            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        if (plans.isEmpty())
            super.mousePressed(event, source);
        LocationType pressed = source.getRealWorldLocation(event.getPoint());
        pressed.convertToAbsoluteLatLonDepth();

        Vector<ReferenceWaypoint> wpts = new Vector<>();
        for (ReferencePlan p : plans.values())
            wpts.addAll(p.getWaypoints());

        ReferenceWaypoint wpt = waypointUnder(event.getPoint(), source);

        if (wpt != null) {
            if (event.isControlDown() && event.getButton() == MouseEvent.BUTTON1) {
                for (ReferencePlan p : plans.values()) {
                    if (p.getWaypoints().contains(wpt)) {
                        ReferenceWaypoint newWaypoint = p.cloneWaypoint(wpt);
                        newWaypoint.setHorizontalLocation(pressed);
                        movingWaypoint = newWaypoint;
                    }
                }
            }
            else if (event.getButton() == MouseEvent.BUTTON1) {
                movingWaypoint = wpt;
            }
            source.repaint();
        }
        else {
            super.mousePressed(event, source);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        movingWaypoint = null;
        super.mouseReleased(event, source);
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (movingWaypoint == null)
            super.mouseDragged(event, source);
        else {
            LocationType pressed = source.getRealWorldLocation(event.getPoint());
            pressed.convertToAbsoluteLatLonDepth();
            movingWaypoint.setHorizontalLocation(pressed);
            source.repaint();
        }
    }

    public ReferenceWaypoint waypointUnder(Point2D pt, StateRenderer2D source) {
        Vector<ReferenceWaypoint> wpts = new Vector<>();
        for (ReferencePlan p : plans.values())
            wpts.addAll(p.getWaypoints());

        LocationType pressed = source.getRealWorldLocation(pt);
        pressed.convertToAbsoluteLatLonDepth();

        for (ReferenceWaypoint wpt : wpts) {
            Reference ref = wpt.getReference();
            LocationType refLoc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
            double dist = pressed.getPixelDistanceTo(refLoc, source.getLevelOfDetail());
            if (dist < radius) {
                return wpt;
            }
        }
        return null;
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        focusedWaypoint = waypointUnder(event.getPoint(), source);
    }

    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        super.mouseClicked(event, source);

        final ReferenceWaypoint wpt = waypointUnder(event.getPoint(), source);

        if (wpt != null && event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() >= 2) {
            PluginUtils.editPluginProperties(wpt, true);
            if (focusedWaypoint.equals(wpt))
                focusedWaypoint = null;
        }

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            if (wpt != null) {
                popup.add("Remove waypoint").addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (ReferencePlan p : plans.values())
                            p.removeWaypoint(wpt);
                        if (focusedWaypoint.equals(wpt))
                            focusedWaypoint = null;
                    }
                });
                popup.add("Waypoint parameters").addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PluginUtils.editPluginProperties(wpt, true);
                    }
                });
                popup.addSeparator();
            }

            Vector<VehicleType> avVehicles = new Vector<VehicleType>();

            ImcSystem[] veh = ImcSystemsHolder.lookupActiveSystemVehicles();

            if (getConsole().getMainSystem() != null)
                if (!avVehicles.contains(VehiclesHolder.getVehicleById(getConsole().getMainSystem())))
                    avVehicles.add(0, VehiclesHolder.getVehicleById(getConsole().getMainSystem()));

            for (ImcSystem sys : veh) {
                final String sysName = sys.getName();
                if (!activeVehicles.contains(sysName)) {
                    popup.add("Activate Follow Reference for " + sysName).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PlanControl startPlan = new PlanControl();
                            startPlan.setType(TYPE.REQUEST);
                            startPlan.setOp(OP.START);
                            startPlan.setPlanId("follow_neptus");
                            FollowReference man = new FollowReference();
                            man.setControlEnt((short) entity);
                            man.setControlSrc(ImcMsgManager.getManager().getLocalId().intValue());
                            man.setAltitudeInterval(2);
                            man.setTimeout(referenceTimeout);
                            man.setLoiterRadius(15);
                            PlanSpecification spec = new PlanSpecification();
                            spec.setPlanId("follow_neptus");
                            spec.setStartManId("1");
                            PlanManeuver pm = new PlanManeuver();
                            pm.setData(man);
                            pm.setManeuverId("1");
                            spec.setManeuvers(Arrays.asList(pm));
                            startPlan.setArg(spec);
                            int reqId = 0;
                            startPlan.setRequestId(reqId);
                            startPlan.setFlags(0);
                            send(sysName, startPlan);
                        }
                    });
                }
                else {
                    popup.add("Stop Follow Reference Control for " + sysName).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PlanControl stop = new PlanControl();
                            stop.setType(TYPE.REQUEST);
                            stop.setOp(OP.STOP);
                            send(sysName, stop);
                        }
                    });
                }
            }
            if (veh.length > 0)
                popup.addSeparator();

            popup.add("Follow Reference Settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(FollowReferenceInteraction.this, getConsole(), true);
                }
            });
            popup.addSeparator();
            popup.add("Follow Reference Interaction Helper").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    GuiUtils.htmlMessage(ConfigFetch.getSuperParentFrame() == null ? FollowReferenceInteraction.this
                            : ConfigFetch.getSuperParentAsFrame(), I18n.text("Follow Reference Interaction Helper")
                            + ".", "", helpMsg);
                }
            });

            popup.show((Component) event.getSource(), event.getX(), event.getY());
        }
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

    /**
     * Help message to be shown
     */
    private void setHelpMsg() {
        helpMsg = "<html><font size='2'><br><div align='center'><table border='1' align='center'>" + "<tr><th>"
                + I18n.text("Type") + "</th><th>" + I18n.text("Description") + "</th></tr>" + "<tr><th>"
                + I18n.text("Opens reference waypoint properties") + "</th><th>"
                + I18n.text("Left Mouse double click on Reference") + "</th></tr>" + "<tr><th>"
                + I18n.text("Add multiple waypoints") + "</th><th>"
                + I18n.text("Press CTRL + Left Mouse Click + Drag Mouse") + "- ("
                + I18n.text("It will show a blue dot that is referent to a new waypoint") + ")." + "</th></tr>";
    }
}
