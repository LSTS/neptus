/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredZ;
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
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.util.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="FollowReference Interaction")
public class FollowReferenceInteraction extends SimpleRendererInteraction implements IPeriodicUpdates {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, FollowRefState> frefStates = new LinkedHashMap<>();
    protected LinkedHashMap<String, ReferencePlan> plans = new LinkedHashMap<>();
    protected LinkedHashMap<String, EstimatedState> states = new LinkedHashMap<>();
    protected HashSet<String> activeVehicles = new HashSet<>();
    protected ReferenceWaypoint movingWaypoint = null;
    protected double radius = 8;
    
    @NeptusProperty
    public ManeuverLocation.Z_UNITS z_units = ManeuverLocation.Z_UNITS.DEPTH;

    @NeptusProperty
    public double z = 0;

    @NeptusProperty
    public double speed = 1.1;
    
    @NeptusProperty
    public boolean useAcousticCommunications = false;
    
    @NeptusProperty
    public long controlLoopLatencySecs = 3;
    
    @NeptusProperty
    public long referenceTimeout = 30;
        
    public FollowReferenceInteraction(ConsoleLayout cl) {
        super(cl);
    }

    @Override
    public long millisBetweenUpdates() {
        return controlLoopLatencySecs * 1000;
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
                    if (prox)
                        plans.get(v).popFirstWaypoint();
                }
            }
            else if (frefStates.containsKey(v)) {
                int prox = frefStates.get(v).getProximity();
                if ((prox & FollowRefState.PROX_XY_NEAR) != 0 &&
                        (prox & FollowRefState.PROX_Z_NEAR) != 0)
                    plans.get(v).popFirstWaypoint();                
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
                            NeptusLog.pub().warn("Sent reference to "+v+" acoustically via "+ sys.getName());
                        }
                    }
                    if (successCount == 0) {
                        NeptusLog.pub().error("Cannot send reference acoustically because no system is capable of it");
                    }
                }
                else {
                    System.out.println("send reference");
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

        boolean newActivation = false;
        if (controlState.getPlanId().equals("follow_neptus") && controlState.getState() == PlanControlState.STATE.EXECUTING) {
            newActivation = activeVehicles.add(controlState.getSourceName());

            if (newActivation) {
                Reference ref = new Reference();
                EstimatedState lastState = states.get(controlState.getSourceName());
                LocationType loc = new LocationType(Math.toDegrees(lastState.getLat()), Math.toDegrees(lastState.getLon()));
                loc.translatePosition(lastState.getX(), lastState.getY(), 0);

                loc.convertToAbsoluteLatLonDepth();
                ref.setLat(loc.getLatitudeAsDoubleValueRads());
                ref.setLon(loc.getLongitudeAsDoubleValueRads());
                ref.setZ(new DesiredZ((float) z, DesiredZ.Z_UNITS.valueOf(z_units.name())));
                ref.setSpeed(new DesiredSpeed(speed, DesiredSpeed.SPEED_UNITS.METERS_PS));
                ref.setFlags((short)(Reference.FLAG_LOCATION | Reference.FLAG_SPEED | Reference.FLAG_Z));

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
                Reference prevRef = wpts.get(i-1).getReference();
                LocationType prevLoc = new LocationType(Math.toDegrees(prevRef.getLat()), Math.toDegrees(prevRef.getLon()));
                Point2D prevPt = renderer.getScreenPosition(prevLoc);
                Reference ref = wpts.get(i).getReference();
                LocationType loc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
                Point2D pt = renderer.getScreenPosition(loc);
                Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-radius, pt.getY()-radius, radius * 2, radius * 2);
                g.setColor(Color.blue);
                
                g.setStroke(new BasicStroke(3f));
                g.draw(new Line2D.Double(prevPt, pt));
                g.fill(ellis);
            }
        }
         
        
        for (ReferencePlan p : plans.values()) {
            Reference ref = p.currentWaypoint().getReference();
            FollowRefState lastFrefState = frefStates.get(p.system_id);
            if (ref != null && lastFrefState != null) {
                Color c = Color.red;

                if (lastFrefState != null) {
                    if (ref.getLat() == lastFrefState.getReference().getLat() && ref.getLon() == lastFrefState.getReference().getLon())
                        c = Color.green;                    
                }
                LocationType loc = new LocationType( Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
                Point2D pt = renderer.getScreenPosition(loc);
                Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-radius, pt.getY()-radius, radius * 2, radius * 2);
                g.setColor(c);
                g.fill(ellis);
            }
        }

        
        for (ReferencePlan p : plans.values()) {
            Reference ref = p.currentWaypoint().getReference();
            FollowRefState lastFrefState = frefStates.get(p.system_id);
            if (ref != null) {
                Color c = Color.red;

                if (lastFrefState != null) {
                    if (ref.getLat() == lastFrefState.getReference().getLat() && ref.getLon() == lastFrefState.getReference().getLon())
                        c = Color.green;                    
                }
                LocationType loc = new LocationType( Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
                Point2D pt = renderer.getScreenPosition(loc);
                Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-radius, pt.getY()-radius, radius * 2, radius * 2);
                g.setColor(c);
                g.fill(ellis);
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

        boolean gotOne = false;
        for (ReferenceWaypoint wpt : wpts) {
            Reference ref = wpt.getReference();
            LocationType refLoc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
            double dist = pressed.getPixelDistanceTo(refLoc, source.getLevelOfDetail());
            if (dist < radius) {
                movingWaypoint = wpt;
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
                    wpt.setHorizontalLocation(pressed);
                }
                else if (event.getButton() == MouseEvent.BUTTON3) {
                    for (ReferencePlan p : plans.values())
                        p.removeWaypoint(wpt);
                }
                source.repaint();
                gotOne = true;                    
                break;
            }
        }
        if (!gotOne) {
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

    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        super.mouseClicked(event, source);

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            Vector<VehicleType> avVehicles = new Vector<VehicleType>();

            ImcSystem[] veh = ImcSystemsHolder.lookupActiveSystemVehicles();

            if (getConsole().getMainSystem() != null)
                if (!avVehicles.contains(VehiclesHolder.getVehicleById(getConsole().getMainSystem())))
                    avVehicles.add(0, VehiclesHolder.getVehicleById(getConsole().getMainSystem()));

            for (ImcSystem sys : veh) {
                final String sysName = sys.getName();
                if (!activeVehicles.contains(sysName)) {
                    popup.add("Activate Follow Reference for "+sysName).addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PlanControl startPlan = new PlanControl();
                            startPlan.setType(TYPE.REQUEST);
                            startPlan.setOp(OP.START);
                            startPlan.setPlanId("follow_neptus");
                            FollowReference man = new FollowReference();
                            man.setControlEnt((short)255);
                            man.setControlSrc(65535);
                            man.setAltitudeInterval(2);
                            man.setTimeout(referenceTimeout);

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
                    popup.add("Stop Follow Reference Control for "+sysName).addActionListener(new ActionListener() {
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
            popup.addSeparator();

            popup.add("Follow Reference Settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(FollowReferenceInteraction.this, getConsole(), true);
                }
            });
            popup.show((Component)event.getSource(), event.getX(), event.getY());
        }
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }
}

