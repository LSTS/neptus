/*
 * Copyright (c) 2004-2015 Norwegian University of Science and Technology (NTNU)
 * Centre for Autonomous Marine Operations and Systems (AMOS)
 * Department of Engineering Cybernetics (ITK)
 * All rights reserved.
 * O.S. Bragstads plass 2D, 7034 Trondheim, Norway
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
 * Author: João Fortuna
 * Feb 13, 2015
 */
package pt.lsts.neptus.plugins.drop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PowerChannelControl;
import pt.lsts.imc.PowerChannelControl.OP;
import pt.lsts.imc.PowerChannelState;
import pt.lsts.imc.PowerChannelState.STATE;
import pt.lsts.imc.Target;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author jfortuna
 *
 */
@PluginDescription(name = "DropMapLayer", icon = "pt/lsts/neptus/plugins/drop/drop.png")
public class DropMapLayer extends SimpleRendererInteraction implements Renderer2DPainter, MainVehicleChangeListener {

    @NeptusProperty(name = "Target height [m]", description = "Height of location to hit.")
    public int dropHeight = 50;

    @NeptusProperty(name = "Drop radius [m]", description = "Circle inside which the drop should hit.")
    public int dropRadius = 20;

    @NeptusProperty(name = "Drop PowerChannel Name", description = "Vehicle Drop control power channel name")
    public String dropPowerChannel = "drop";

    private static final long serialVersionUID = 1L;
    private LocationType targetPos = null, dropPos = null;
    private boolean manualDrop = false;

    /**
     * @param console
     */
    public DropMapLayer(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
    }

    /**
     * On right-click, show popup menu on the map with plug-in options
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            final LocationType loc = source.getRealWorldLocation(event.getPoint());

            addStartDropMenu(popup);
            addSetTargetMenu(popup, loc);
            addSettingMenu(popup);
            popup.addSeparator();
            addDropNowMenu(popup);
            popup.show(source, event.getPoint().x, event.getPoint().y);
        }
    }

    private void addSettingMenu(JPopupMenu popup) {
        popup.add(I18n.text("Settings")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(DropMapLayer.this, getConsole(), true);
            }
        });
    }

    private void addStartDropMenu(JPopupMenu popup) {
        JMenuItem item = popup.add(I18n.text("Start drop plan"));
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Target target = new Target();
                target.setLat(targetPos.getLatitudeRads());
                target.setLon(targetPos.getLongitudeRads());
                target.setZ(dropHeight);
                target.setZUnits(ZUnits.HEIGHT);
                target.setLabel("neptus");
                send(target);
                dropPos = null;
            }
        });
        item.setEnabled(targetPos != null);
    }

    private void addSetTargetMenu(JPopupMenu popup, final LocationType loc) {
        popup.add(I18n.text("Set target here")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                targetPos = loc;
                dropPos = null;
            }
        });
    }

    private void addDropNowMenu(JPopupMenu popup) {
        popup.add(I18n.text("Drop now!")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PowerChannelControl pcc = new PowerChannelControl();
                pcc.setName(dropPowerChannel);
                pcc.setOp(OP.TURN_ON);
                send(pcc);
                manualDrop = true;
            }
        });
    }

    @Override
    /**
     * Always returns true
     */
    public boolean isExclusive() {
        return true;
    }

    /**
     * Paints filled circles on the current target and drop positions.
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        // If the target position has not been set, there is nothing to paint
        if (targetPos == null)
            return;

        Point2D pt = renderer.getScreenPosition(targetPos);
        g.translate(pt.getX(), pt.getY());
        if (dropPos == null)
            g.setColor(Color.orange);
        else
            g.setColor(Color.green);
        g.fill(new Ellipse2D.Double(-6, -6, 12, 12));

        g.setColor(Color.white);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(255, 255, 255, 128));
        double radius = Math.abs(dropRadius * renderer.getZoom());
        g.draw(new Ellipse2D.Double(-radius, -radius, radius * 2, radius * 2));

        if (dropPos != null) {
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

    /**
     * When a {@link PowerChannelState} with state ON is received, sets the drop position to current location. If the
     * release was manually requested, a TURN_OFF command is issued to the drop mechanism.
     *
     * @param msg Message to check for the current drop state. Will become "dropped" if <code>msg.state == ON</code> .
     */
    @Subscribe
    public void on(PowerChannelState msg) {
        if (!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        // check if the power channel matches the dropped power channel
        if (!dropPowerChannel.isEmpty() && !dropPowerChannel.equals(msg.getName()))
            return;

        if (msg.getState() == STATE.ON) {
            dropPos = ImcSystemsHolder.getSystemWithName(getMainVehicleId()).getLocation();

            if (manualDrop) {
                PowerChannelControl pcc = new PowerChannelControl();
                pcc.setName(dropPowerChannel);
                pcc.setOp(OP.TURN_OFF);
                send(pcc);
                manualDrop = false;
            }
        }
    }
}
