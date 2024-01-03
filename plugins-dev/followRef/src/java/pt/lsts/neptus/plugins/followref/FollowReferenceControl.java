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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Arrays;

import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

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
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="FollowReference Control")
public class FollowReferenceControl extends SimpleRendererInteraction implements IPeriodicUpdates {

    private static final long serialVersionUID = 1L;
    protected boolean frefActive = false;
    protected FollowRefState lastFrefState = null;
    protected EstimatedState lastState = null;
    protected Reference ref = null;
    protected boolean movingReference = false;
    protected double radius = 8;
    
    @NeptusProperty
    public double depth = 3;

    @NeptusProperty
    public double speed = 1.3;

    @NeptusProperty
    public boolean useAcousticModem = false;
    
    @NeptusProperty
    public boolean allowControlFromWebService = false;    

    public FollowReferenceControl(ConsoleLayout cl) {
        super(cl);
    }

    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    @Override
    public boolean update() {
        if (ref != null)
            send(ref);

        return true;
    }


    @Subscribe
    public void on(EstimatedState estimatedState) {
        if (estimatedState.getSourceName().equals(getConsole().getMainSystem()))            
            this.lastState = estimatedState;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        double z = depth;
        ZUnits units = ZUnits.DEPTH;
        if (z < 0) {
            units = ZUnits.ALTITUDE;
            z = -z;
        }
        if (ref != null) {
            ref.setZ(new DesiredZ((float)z, units));
            ref.setSpeed(new DesiredSpeed(speed, SpeedUnits.METERS_PS));
        }
    }

    @Subscribe
    public void on(PlanControlState controlState) {
        if (!controlState.getSourceName().equals(getConsole().getMainSystem()))
            return;

        if (lastState == null)
            return;

        boolean wasActive = frefActive;
        if (controlState.getPlanId().equals("follow_neptus") && controlState.getState() == PlanControlState.STATE.EXECUTING)
            frefActive = true;
        else
            frefActive = false;

        if (!wasActive && frefActive) {
            ref = new Reference();
            LocationType loc = new LocationType(Math.toDegrees(lastState.getLat()), Math.toDegrees(lastState.getLon()));
            loc.translatePosition(lastState.getX(), lastState.getY(), 0);

            loc.convertToAbsoluteLatLonDepth();
            ref.setLat(loc.getLatitudeRads());
            ref.setLon(loc.getLongitudeRads());
            double z = depth;
            ZUnits units = ZUnits.DEPTH;
            if (z < 0) {
                units = ZUnits.ALTITUDE;
                z = -z;
            }

            ref.setZ(new DesiredZ((float)z, units));
            ref.setSpeed(new DesiredSpeed(speed, SpeedUnits.METERS_PS));
            ref.setFlags((short)(Reference.FLAG_LOCATION | Reference.FLAG_SPEED | Reference.FLAG_Z));
        }           
        
        if (wasActive && !frefActive) {
            ref = null;
        }
    }

    @Subscribe
    public void on(FollowRefState frefState) {
        if (frefState.getSourceName().equals(getConsole().getMainSystem()))            
            this.lastFrefState = frefState;
    }

    public void on(ConsoleEventMainSystemChange event) {
        frefActive = false;
        lastState = null;
        lastFrefState = null;
        ref = null;        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleRendererInteraction#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

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
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        if (ref == null)
            super.mousePressed(event, source);
        
        LocationType pressed = source.getRealWorldLocation(event.getPoint());
        pressed.convertToAbsoluteLatLonDepth();
        LocationType refLoc = new LocationType(Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
        double dist = pressed.getPixelDistanceTo(refLoc, source.getLevelOfDetail());
        if (dist < radius) {
            movingReference = true;
            ref.setLat(pressed.getLatitudeRads());
            ref.setLon(pressed.getLongitudeRads());
            source.repaint();
        }        
        else {
            super.mousePressed(event, source);
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        movingReference = false;
        super.mouseReleased(event, source);        
    }
    
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (!movingReference)
            super.mouseDragged(event, source);
        else {
            LocationType pressed = source.getRealWorldLocation(event.getPoint());
            ref.setLat(pressed.getLatitudeRads());
            ref.setLon(pressed.getLongitudeRads());
            source.repaint();
        }
    }

    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        super.mouseClicked(event, source);

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            if (!frefActive) {
                popup.add("Activate Follow Reference for "+getConsole().getMainSystem()).addActionListener(new ActionListener() {

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

                        if (useAcousticModem)
                            man.setTimeout(60);
                        else
                            man.setTimeout(5);

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

                        send(startPlan);
                    }
                });
            }
            else {
                popup.add("Move Reference Here").addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        LocationType loc = source.getRealWorldLocation(event.getPoint());                        
                        loc.convertToAbsoluteLatLonDepth();
                        ref.setLat(loc.getLatitudeRads());
                        ref.setLon(loc.getLongitudeRads());
                        source.repaint();
                    }
                });    
                popup.add("Stop Follow Reference Control").addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PlanControl stop = new PlanControl();
                        stop.setType(TYPE.REQUEST);
                        stop.setOp(OP.STOP);
                        send(stop);
                    }
                });
            }

            popup.addSeparator();

            popup.add("Follow Reference Settings").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(FollowReferenceControl.this, getConsole(), true);
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
