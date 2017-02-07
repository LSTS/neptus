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
 * Author: Marcus Frølich
 * Feb 17, 2015
 */
package pt.lsts.neptus.plugins.land;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Abort;
import pt.lsts.imc.DeviceState;
import pt.lsts.imc.PlanGeneration;
import pt.lsts.imc.PlanGeneration.CMD;
import pt.lsts.imc.PlanGeneration.OP;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author marcusf
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Land Map Layer", icon = "pt/lsts/neptus/plugins/land/land_icon.png", 
    author = "Marcus Frølich", version = "0.8", description = "This plugin needs the land plan generator in the vehicle.")
public class LandMapLayer extends SimpleRendererInteraction implements Renderer2DPainter, MainVehicleChangeListener {

    // Simple settings
    @NeptusProperty(name = "Net Height", description = "Height of the actual net (m).", userLevel = LEVEL.REGULAR)
    public double netHeight = 3;

    @NeptusProperty(name = "Net Orientation", description = "Heading for UAV to enter net (deg | N=0, E=90).", 
            userLevel = LEVEL.REGULAR)
    public double netHeading = 66.5;

    @NeptusProperty(name = "Net Latitude", description = "Position of landing net (decimal deg).", 
            userLevel = LEVEL.REGULAR, editable = false)
    public double netLat = 63.628600;

    @NeptusProperty(name = "Net Longitude", description = "Position of landing net (decimal deg).", 
            userLevel = LEVEL.REGULAR, editable = false)
    public double netLon = 9.727570;

    @NeptusProperty(name = "Ground Level", description = "Height from \"ground\" to bottom of net (m).", 
            userLevel = LEVEL.REGULAR)
    public double groundLevel = 30;

    // Advanced settings
    @NeptusProperty(name = "Minimum Turn Radius", description = "Lateral turning radius of UAV (m).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double minTurnRadius = 150;

    @NeptusProperty(name = "Attack Angle", description = "Vertical angle of attack into the net (deg).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double attackAngle = 4;

    @NeptusProperty(name = "Descend Angle", description = "Vertical angle of UAV when descending (deg).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double descendAngle = 4;

    @NeptusProperty(name = "Speed WP1-2", description = "Speed of waypoints WP1 and WP2 (m/s).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double speed12 = 18;

    @NeptusProperty(name = "Speed WP3-5", description = "Speed of waypoints WP3, WP4 and WP5 (m/s).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double speed345 = 16;

    @NeptusProperty(name = "Distance in Front", description = "Distance from net to WP before (should be negative) (m).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double distInFront = -100;

    @NeptusProperty(name = "Distance Behind", description = "Distance from net to aimingpoint (WP) after net (m).", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public double distBehind = 100;

    @NeptusProperty(name = "Ignore Evasive", description = "If true: Force landing despite error demanding evasive.", 
            userLevel = LEVEL.ADVANCED, category = "Advanced")
    public boolean ignoreEvasive = false;

    private LocationType landPos = null;

    private int[] arrX = { -8, -12, -12, 12, 12, 8, 0 };
    private int[] arrY = { 6, 6, 10, 10, 6, 6, -10 };
    private Polygon poly = new Polygon(arrX, arrY, 7);

    /**
     * @param console
     */
    public LandMapLayer(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
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

            addStartLandMenu(popup);
            addSetNetMenu(popup, loc);
            addSettingMenu(popup);
            popup.show(source, event.getPoint().x, event.getPoint().y);
        }
    }

    private void addSettingMenu(JPopupMenu popup) {
        popup.add(I18n.text("Settings")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(LandMapLayer.this, getConsole(), true);
                updateNetArrow();
            }
        });
    }

    private void addStartLandMenu(JPopupMenu popup) {
        JMenuItem item = popup.add(I18n.text("Start land plan"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PlanGeneration pg = new PlanGeneration();
                String params = "land_lat=" + landPos.getLatitudeDegs() + ";";
                params += "land_lon=" + landPos.getLongitudeDegs() + ";";
                params += "land_heading=" + netHeading + ";";
                params += "net_height=" + netHeight / 2 + ";";
                params += "min_turn_radius=" + minTurnRadius + ";";
                params += "attack_angle=" + attackAngle + ";";
                params += "descend_angle=" + descendAngle + ";";

                params += "dist_behind=" + distBehind + ";";
                params += "dist_infront=" + distInFront + ";";
                params += "speed12=" + speed12 + ";";
                params += "speed345=" + speed345 + ";";

                params += "z_unit=height;"; // "height" or "altitude"
                params += "ground_level=" + groundLevel + ";";

                params += "ignore_evasive=" + ignoreEvasive + ";";

                pg.setParams(params);
                pg.setCmd(CMD.EXECUTE); // CMD.GENERATE
                pg.setOp(OP.REQUEST);
                pg.setPlanId("land");

                if (pg.getCmd() == CMD.EXECUTE) {
                    send(new Abort());
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    }
                    catch (InterruptedException err) {
                        // Handle exception
                    }
                }

                send(pg);
            }
        });
        item.setEnabled(landPos != null);
    }

    private void addSetNetMenu(JPopupMenu popup, final LocationType loc) {
        popup.add(I18n.text("Set net here")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                landPos = loc;
                netLat = landPos.getLatitudeDegs();
                netLon = landPos.getLongitudeDegs();
                updateNetArrow();
            }
        });
    }

    /**
     * Always returns true
     */
    @Override
    public boolean isExclusive() {
        return true;
    }

    /**
     * Paints filled circles on the current target and drop positions.
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        // If the land position has not been set, there is nothing to paint
        if (landPos == null)
            return;

        Point2D pt = renderer.getScreenPosition(landPos);
        g.translate(pt.getX(), pt.getY());

        // Draws the "arrow"
        g.setColor(Color.green);
        g.fillPolygon(poly);

        // Draws the "aiming point" in the middle
        g.setColor(Color.red);
        g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
    }

    @Subscribe
    public void on(DeviceState state) {
        // Consumes changes to the net
        netHeading = Math.toDegrees(state.getPsi());
        double[] displaced = CoordinateUtil.WGS84displace(netLat, netLon, 0.0, state.getY(), state.getX(), 0.0);
        netLat = displaced[0];
        netLon = displaced[1];
        landPos.setLatitudeDegs(netLat);
        landPos.setLongitudeDegs(netLon);
        updateNetArrow();
    }

    private void updateNetArrow() {
        double angle = Math.toRadians(netHeading);
        for (int i = 0; i < poly.npoints; i++) {
            int x = arrX[i];
            int y = arrY[i];

            // Apply rotation
            double temp_x = x * Math.cos(angle) - y * Math.sin(angle);
            double temp_y = x * Math.sin(angle) + y * Math.cos(angle);

            poly.xpoints[i] = (int) Math.round(temp_x);
            poly.ypoints[i] = (int) Math.round(temp_y);
        }
    }
}
