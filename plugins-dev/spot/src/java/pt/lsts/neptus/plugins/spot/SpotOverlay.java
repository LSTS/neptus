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
 * Author: Margarida Faria
 * Mar 25, 2013
 */
package pt.lsts.neptus.plugins.spot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Margarida Faria
 *
 */
@PluginDescription(author = "Margarida", name = "SPOT Overlay", icon = "pt/lsts/neptus/plugins/spot/images/spotIcon.png")
public class SpotOverlay extends SimpleRendererInteraction implements IPeriodicUpdates, ConfigurationListener {
    private List<Spot> spotsOnMap;
    private boolean active = false;

    private static final long serialVersionUID = -4807939956933128721L;

    @NeptusProperty
    public int updateMinutes = 2;

    @NeptusProperty(name = "Visible", userLevel = LEVEL.ADVANCED)
    public boolean visible = true;

    @NeptusProperty(name = "Show only when interaction is active", userLevel = LEVEL.REGULAR)
    public boolean showOnlyWhenInteractionIsActive = true;
    @NeptusProperty(name = "Show names", userLevel = LEVEL.REGULAR)
    public boolean showNames = true;
    @NeptusProperty(name = "Show speed value", userLevel = LEVEL.REGULAR)
    public boolean showSpeedValue = true;
    @NeptusProperty(userLevel = LEVEL.REGULAR, description = "Set the time window (in hours) for considered positions. Will only consider positions in the last x hours.", name = "Time window (hours)")
    public int hours = 70;
    @NeptusProperty(userLevel = LEVEL.REGULAR, name = "Export to CSV")
    public boolean printCvsFile = false;
    @NeptusProperty(name = "SPOT Stream ID", description = "Identifier of SPOT stream to show", userLevel = LEVEL.REGULAR)
    public String streamID = "0eFbYotphiMKz9YiDOI7XqR76JJ010Z0X";

    protected GeneralPath gp = new GeneralPath();
    {
        gp.moveTo(-2, -8);
        gp.lineTo(2, -8);
        gp.lineTo(2, 2);
        gp.lineTo(5, 2);
        gp.lineTo(0, 8);
        gp.lineTo(-5, 2);
        gp.lineTo(-2, 2);
        gp.closePath();
    }

    /**
     * @param console
     */
    public SpotOverlay(ConsoleLayout console) {
        super(console);
        spotsOnMap = new ArrayList<>();
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMinutes * 60 * 1000;
    }

    @Override
    public boolean update() {
        // Called by Update thread
        updateFromPage();
        return true;
    }

    private void updateFromPage() {
        List<Spot> nextSpotsOnMap = new ArrayList<>();
        HashMap<String, TreeSet<SpotMessage>> msgBySpot;
        try {
            msgBySpot = SpotMsgFetcher.get(hours, streamID);
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            NeptusLog.pub().error("Exception while loading data from Spot website.", e);
            return;
        }
        if (printCvsFile) {
            DataExporter.exportToCsv(msgBySpot);
        }
        // if no messages were found do nothing
        if (msgBySpot.size() == 0) {
            return;
        }
        Collection<TreeSet<SpotMessage>> spotIds = msgBySpot.values();
        TreeSet<SpotMessage> msgTreeSet;
        SpotMessage firstMsg;
        Spot spot;
        // iterate over spots mentioned in messages
        for (Iterator<TreeSet<SpotMessage>> iterator = spotIds.iterator(); iterator.hasNext();) {
            msgTreeSet = iterator.next();
            // create spot
            firstMsg = msgTreeSet.first();
            spot = new Spot(firstMsg.id);
            spot.update(msgTreeSet);
            nextSpotsOnMap.add(spot);
        }
        spotsOnMap = nextSpotsOnMap;
        repaint();
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
        PeriodicUpdatesService.unregister(this);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        active = mode;
        if (active)
            update();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!visible || (showOnlyWhenInteractionIsActive && !active)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // For each spot paint all the known positions with dots in each position and a path connecting them
        for (Spot spot : spotsOnMap) {
            LocationType spotLoc = spot.getLastLocation();
            if (spotLoc == null) {
                continue;
            }
            Point2D pt = renderer.getScreenPosition(spotLoc);
            double xScreenPos = pt.getX();
            double yScreenPos = pt.getY();
            g.translate(xScreenPos, yScreenPos);

            if (showNames) {
                g.setColor(Color.red.darker().darker());
                g.drawString(spot.getName(), 5, 0);
            }

            double speedMps = spot.getSpeed();
            if (speedMps != -1) {
                if (showSpeedValue) {
                    g.setColor(Color.black);
                    g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(speedMps) + " m/s", 5, 10);
                }
            }
            g.translate(-xScreenPos, -yScreenPos);
            ArrayList<LocationType> lastLocations = spot.getLastLocations();
            LocationType location;
            GeneralPath spotPath = new GeneralPath();
            Iterator<LocationType> iterator = lastLocations.iterator();
            float shadeOfGreyInc = 0.05f;
            float shadeOfGrey = (lastLocations.size() * shadeOfGreyInc);
            float min;
            if (iterator.hasNext()) {
                location = iterator.next();
                pt = renderer.getScreenPosition(location);
                spotPath.moveTo(pt.getX(), pt.getY());
                while (iterator.hasNext()) {
                    location = iterator.next();
                    pt = renderer.getScreenPosition(location);
                    spotPath.lineTo(pt.getX(), pt.getY());
                    min = 1 - Math.min(shadeOfGrey, 0.8f);
                    g2.setColor(new Color(min, min, min));
                    g2.fillOval((int) (pt.getX() - 3), (int) (pt.getY() - 3), 7, 7);
                    shadeOfGrey -= shadeOfGreyInc;

                }
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(0.4f, 0.4f, 0.4f, 0.5f));
                g2.draw(spotPath);
            }
        }
    }

    @Override
    public void propertiesChanged() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateFromPage();
            }
        });
    }
}
