/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: João Fortuna
 * Feb 13, 2015
 */
package pt.lsts.neptus.plugins.uavs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.JPopupMenu;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.PowerChannelControl;
import pt.lsts.imc.PowerChannelControl.OP;
import pt.lsts.imc.Target;
import pt.lsts.imc.Target.Z_UNITS;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.util.WGS84Utilities;

import com.google.common.eventbus.Subscribe;

/**
 * @author jfortuna
 *
 */
@PluginDescription(name = "DropMapLayer", icon = "pt/lsts/neptus/plugins/uavs/drop.png")
public class DropMapLayer extends SimpleRendererInteraction implements Renderer2DPainter, MainVehicleChangeListener {

    @NeptusProperty(name = "Target height [m]", description = "Height of location to hit.")
    public int dropHeight = 50;
    @NeptusProperty(name = "Drop radius [m]", description = "Circle inside which the drop should hit.")
    public int dropRadius = 20;

    private static final long serialVersionUID = 1L;
    private Maneuver lastManeuver = null;

    protected boolean surveyEdit = false;
    protected int surveyPos = 0;
    protected boolean active = false;

    protected boolean dropped = false;

    private LocationType targetPos, dropPos;

    /**
     * @param console
     */
    public DropMapLayer(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        active = mode;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            final LocationType loc = source.getRealWorldLocation(event.getPoint());

            addStartDropMenu(popup);
            addSetTargetMenu(popup, loc);
            addSettingMenu(popup);
            popup.show(source, event.getPoint().x, event.getPoint().y);
        }
    }

    private void addSettingMenu(JPopupMenu popup) {
        popup.add("Settings").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(DropMapLayer.this, getConsole(), true);
            }
        });
    }

    private void addStartDropMenu(JPopupMenu popup) {
        popup.add("Start drop plan").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Target target = new Target();
                target.setLat(targetPos.getLatitudeRads());
                target.setLon(targetPos.getLongitudeRads());
                target.setZ(dropHeight);
                target.setZUnits(Z_UNITS.HEIGHT);
                target.setLabel("neptus");

                send(target);

                dropped = false;
            }
        });
    }

    private void addSetTargetMenu(JPopupMenu popup, final LocationType loc) {
        popup.add("Set target here").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                targetPos = loc;
                dropped = false;
            }
        });
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Point2D pt = renderer.getScreenPosition(targetPos);
        g.translate(pt.getX(), pt.getY());
        if (!dropped)
            g.setColor(Color.orange);
        else
            g.setColor(Color.green);
        g.fill(new Ellipse2D.Double(-6, -6, 12, 12));

        g.setColor(Color.white);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(255, 255, 255, 128));
        double radius = Math.abs(dropRadius * renderer.getZoom());
        g.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));

        if (dropped) {
            Point2D pt2 = renderer.getScreenPosition(dropPos);
            g.setColor(Color.red);
            g.fill(new Ellipse2D.Double(pt2.getX() - pt.getX() - 5, pt2.getY() - pt.getY() - 5, 10, 10));
        }
    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
    }

    @Subscribe
    public void on(PowerChannelControl msg) {
        if (!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        if (msg.getOp() == OP.TURN_ON)
            dropped = true;
    }

    @Subscribe
    public void on(EstimatedState msg) {
        if (!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        if (dropped)
            return;

        double pos[] = WGS84Utilities.toLatLonDepth(msg);

        dropPos = new LocationType(pos[0], pos[1]);
    }
}
