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
 * Author: Manuel R.
 * 16/02/2018
 */
package pt.lsts.neptus.plugins.flirptucommand;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Reference;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author Manuel R.
 *
 */
@PluginDescription(name = "FLIR PTU Control", icon = "pt/lsts/neptus/plugins/flirptucommand/target.png", description = "PTU Control Interaction", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 70)
public class PTUCommandInteraction extends SimpleRendererInteraction implements IPeriodicUpdates {
    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Enable Target system")
    public boolean enableTarget = false;

    @NeptusProperty(name = "Target System Name", description = "Target system name to point the PTU at")
    public String targetSysName = "lauv-xplore-2";

    @NeptusProperty(name = "Gateway Name", description = "Gateway system name")
    public String gatewaySystem = "manta-3";

    @NeptusProperty(name = "Height", description = "Target height (WGS84)")
    public double height = 0.0;

    private EstimatedState es = new EstimatedState();
    private Reference ref = null;
    private boolean movingReference = false;
    private double radius = 8;
    
    public PTUCommandInteraction(ConsoleLayout console) {
        super(console);
    }


    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        if (ref != null) {

            LocationType loc = new LocationType( Math.toDegrees(ref.getLat()), Math.toDegrees(ref.getLon()));
            Point2D pt = renderer.getScreenPosition(loc);
            double radiusAlt = radius;
            if (enableTarget) {
                radiusAlt = radiusAlt * 2;

                Ellipse2D ellisOuter = new Ellipse2D.Double(pt.getX()-(radiusAlt+2), pt.getY()-(radiusAlt+2), (radiusAlt+2) * 2, (radiusAlt+2) * 2);
                g.setColor(Color.green.brighter());
                g.fill(ellisOuter);
            }

            if (movingReference) {
                Ellipse2D ellisOuter = new Ellipse2D.Double(pt.getX()-(radiusAlt+2), pt.getY()-(radiusAlt+2), (radiusAlt+2) * 2, (radiusAlt+2) * 2);
                g.setColor(Color.green.brighter());
                g.fill(ellisOuter);
            }


            Ellipse2D ellisInner = new Ellipse2D.Double(pt.getX()-radiusAlt, pt.getY()-radiusAlt, radiusAlt * 2, radiusAlt * 2);
            g.setColor(Color.red);
            g.fill(ellisInner);
            
            Ellipse2D ellisInner2 =  new Ellipse2D.Double(pt.getX()-0.25-(radiusAlt-2), pt.getY()-(radiusAlt-2), (radiusAlt-2) * 2, (radiusAlt-2) * 2);
            g.setColor(Color.white);
            g.fill(ellisInner2);
            
            Ellipse2D ellisInner3 = new Ellipse2D.Double(pt.getX()-0.25-(radiusAlt-5), pt.getY()-(radiusAlt-5), (radiusAlt-5) * 2, (radiusAlt-5) * 2);
            g.setColor(Color.red);
            g.fill(ellisInner3);

        }
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        if (ref == null || !SwingUtilities.isLeftMouseButton(event)) {
            super.mousePressed(event, source);
            return;
        }

        if (enableTarget)
            enableTarget = false;

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

            es.setLat(ref.getLat());
            es.setLon(ref.getLon());
            es.setHeight(height);

            ImcMsgManager.getManager().sendMessageToSystem(es, gatewaySystem);

            source.repaint();
        }
    }

    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        super.mouseClicked(event, source);

        if (SwingUtilities.isRightMouseButton(event)) {
            JPopupMenu popup = new JPopupMenu();
            if (!enableTarget) {

                JMenu systemsMenu = new JMenu("Set Target Reference as...");

                ImcSystem[] veh = ImcSystemsHolder.lookupSystemVehicles();
                for (ImcSystem sys : veh) {
                    final VehicleType vehS = VehiclesHolder.getVehicleById(sys.getName());
                    JMenuItem menuItem = vehS != null ? new JMenuItem(vehS.getId(), vehS.getIcon()) : new JMenuItem(sys.getName());
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            targetSysName = sys.getName();
                            if (!enableTarget)
                                enableTarget = true;
                        }
                    });
                    systemsMenu.add(menuItem);
                }

                

                ImcSystem[] mobile = ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.MOBILESENSOR);
                
                if (mobile.length > 0)
                    systemsMenu.addSeparator();
                
                for (ImcSystem sys : mobile) {
                    JMenuItem menuItem = new JMenuItem(sys.getName());
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            targetSysName = sys.getName();
                            if (!enableTarget)
                                enableTarget = true;
                        }
                    });
                    systemsMenu.add(menuItem);
                }

                

                ImcSystem[] ccu = ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.CCU);
                if (ccu.length > 0)
                    systemsMenu.addSeparator();
                
                List<ImcSystem> vecLst = Arrays.asList(veh);
                for (ImcSystem sys : ccu) {
                    if (vecLst.contains(sys))
                        continue;

                    JMenuItem menuItem = new JMenuItem(sys.getName());
                    menuItem.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            targetSysName = sys.getName();
                            if (!enableTarget)
                                enableTarget = true;
                        }
                    });
                    systemsMenu.add(menuItem);
                }

                popup.add(systemsMenu);

                if (enableTarget)
                    popup.addSeparator();
            }

            if (enableTarget) {

                popup.add("Disable target").addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        enableTarget = false;
                        ref = null;
                    }
                });
            }

            popup.show((Component)event.getSource(), event.getX(), event.getY());

        }

        if (SwingUtilities.isLeftMouseButton(event)) {

            ref = new Reference();
            LocationType loc = source.getRealWorldLocation(event.getPoint());

            loc.convertToAbsoluteLatLonDepth();
            ref.setLat(loc.getLatitudeRads());
            ref.setLon(loc.getLongitudeRads());

            ref.setFlags((short)(Reference.FLAG_LOCATION));

            es.setLat(ref.getLat());
            es.setLon(ref.getLon());
            es.setHeight(height);

            ImcMsgManager.getManager().sendMessageToSystem(es, gatewaySystem);
        }
    }

    @Override
    public boolean isExclusive() {
        return true;
    }


    @Override
    public void cleanSubPanel() {
        ref = null;

    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }


    @Override
    public boolean update() {
        if (!active)
            ref = null;

        if (enableTarget) {
            ImcSystem sys = ImcSystemsHolder.getSystemWithName(targetSysName);
            LocationType loc = null;

            if (sys != null)
                loc = sys.getLocation();

            if (loc != null && active) {
                if (ref == null)
                    ref = new Reference();

                loc.convertToAbsoluteLatLonDepth();
                ref.setLat(loc.getLatitudeRads());
                ref.setLon(loc.getLongitudeRads());

                es.setLat(ref.getLat());
                es.setLon(ref.getLon());
                es.setHeight(height);

                ImcMsgManager.getManager().sendMessageToSystem(es, gatewaySystem);
            } 
        }

        return true;
    }
}
