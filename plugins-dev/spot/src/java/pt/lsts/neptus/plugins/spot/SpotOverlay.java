/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.mp.SpeedType;
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
import pt.lsts.neptus.util.UnitsUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author Margarida Faria
 *
 */
@PluginDescription(author = "Margarida", name = "SPOT Overlay", icon = "pt/lsts/neptus/plugins/spot/images/spotIcon.png")
public class SpotOverlay extends SimpleRendererInteraction implements IPeriodicUpdates, ConfigurationListener {
    public static final Color COLOR_SPOT_A128 = new Color(255, 148, 143, 128);
    public static final Color COLOR_SPOT = new Color(255, 148, 143);

    private List<Spot> spotsOnMap;
    private boolean active = false;

    private static final long serialVersionUID = -4807939956933128721L;

    @NeptusProperty(name = "Update every (minutes)", userLevel = LEVEL.ADVANCED,
            description = "Time between updates of the SPOT positions. Default is 2 minutes.")
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
    @NeptusProperty(name = "Filter by SPOT ID", userLevel = LEVEL.REGULAR,
            description = "Filter the SPOT messages by the SPOT ID. Leave empty to show all SPOTs. Use comma to separate multiple SPOT IDs.")
    public String spotIDs = "";
    @NeptusProperty(name = "Case Sensitive Filter by SPOT ID", userLevel = LEVEL.REGULAR,
            description = "Case sensitive filter the SPOT messages by the SPOT ID.")
    public boolean spotIDsCaseSensitive = false;

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

    private final List<String> spotIDsToShow = new ArrayList<>();

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
            if (!spotIDsToShow.isEmpty() && spotIDsToShow.stream().noneMatch(
                    s -> spotIDsCaseSensitive ? spot.getName().equals(s) : spot.getName().equalsIgnoreCase(s))) {
                continue;
            }
            LocationType spotLoc = spot.getLastLocation();
            if (spotLoc == null) {
                continue;
            }
            Point2D pt = renderer.getScreenPosition(spotLoc);
            double xScreenPos = pt.getX();
            double yScreenPos = pt.getY();
            g.translate(xScreenPos, yScreenPos);

            boolean hasTime = false;
            if (showNames) {
                g.setColor(Color.BLACK);
                g.drawString(spot.getName(), 7, 1);
                g.setColor(COLOR_SPOT);
                g.drawString(spot.getName(), 6, 0);

                Long timestampMillis = spot.getLastLocationTimestampMillis();
                if (timestampMillis != null) {
                    hasTime = true;
                    String dateStr = new Date(timestampMillis).toString();
                    g.setColor(Color.BLACK);
                    g.drawString(dateStr, 7, 13);
                    g.setColor(COLOR_SPOT);
                    g.drawString(dateStr, 6, 12);
                }
            }

            double speedMps = spot.getSpeed();
            if (speedMps != -1) {
                if (showSpeedValue) {
                    SpeedType.Units speedUnits = GeneralPreferences.speedUnits;
                    Pair<Double, SpeedType.Units> convSpeed = UnitsUtil.convertSpeed(speedMps, speedUnits);
                    String speedTxt = GuiUtils.getNeptusDecimalFormat(1).format(convSpeed.getLeft()) + " " + convSpeed.getRight();
                    g.setColor(Color.BLACK);
                    int offset = hasTime ? 12 : 0;
                    g.drawString(speedTxt, 7, 13 + offset);
                    g.setColor(COLOR_SPOT);
                    g.drawString(speedTxt, 6, 12 + offset);
                }
            }
            g.translate(-xScreenPos, -yScreenPos);
            List<LocationType> lastLocations = spot.getLastLocations();
            List<Long> lastLocationsTimestampMillis = spot.getLastLocationsTimestampMillis();
            LocationType location;
            Long locationTimestampMillis;
            GeneralPath spotPath = new GeneralPath();
            float shadeOfGreyInc = 0.05f;
            float shadeOfGrey = (lastLocations.size() * shadeOfGreyInc);
            float min;
            if (!lastLocations.isEmpty()) {
                location = lastLocations.get(0);
                locationTimestampMillis = lastLocationsTimestampMillis.get(0);
                pt = renderer.getScreenPosition(location);
                spotPath.moveTo(pt.getX(), pt.getY());
                min = 1 - Math.min(shadeOfGrey, 0.8f);
                g2.setColor(new Color(min, min, min));
                g2.fillOval((int) (pt.getX() - 4), (int) (pt.getY() - 4), 8, 8);

                for (int i = 1; i < lastLocations.size(); i++) {
                    location = lastLocations.get(i);
                    locationTimestampMillis = lastLocationsTimestampMillis.get(i);
                    pt = renderer.getScreenPosition(location);
                    spotPath.lineTo(pt.getX(), pt.getY());
                    min = 1 - Math.min(shadeOfGrey, 0.8f);
                    g2.setColor(new Color(min, min, min));
                    g2.fillOval((int) (pt.getX() - 4), (int) (pt.getY() - 4), 8, 8);
                    shadeOfGrey -= shadeOfGreyInc;
                }

                g2.setStroke(new BasicStroke(3f));
                g2.setColor(COLOR_SPOT_A128);
                g2.draw(spotPath);

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(Color.darkGray);
                g2.drawOval((int) (pt.getX() - 5), (int) (pt.getY() - 5), 9, 9);
                g2.setColor(new Color(255, 148, 143, 128));
                g2.fillOval((int) (pt.getX() - 5), (int) (pt.getY() - 5), 10, 10);
                g2.setColor(Color.darkGray);
                g2.fillOval((int) (pt.getX() - 2), (int) (pt.getY() - 2), 4, 4  );
            }
        }
    }

    @Override
    public void propertiesChanged() {
        spotIDsToShow.clear();
        if (!spotIDs.isEmpty()) {
            String[] ids = spotIDs.split(",");
            for (String id : ids) {
                spotIDsToShow.add(id.trim());
            }
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateFromPage();
            }
        });
    }
}
