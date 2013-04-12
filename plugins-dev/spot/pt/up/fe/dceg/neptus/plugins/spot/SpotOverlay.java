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
 * Author: Margarida Faria
 * Mar 25, 2013
 */
package pt.up.fe.dceg.neptus.plugins.spot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.plugins.update.PeriodicUpdatesService;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author Margarida Faria
 *
 */
@PluginDescription(author = "Margarida", name = "SPOT Overlay")
public class SpotOverlay extends SimpleRendererInteraction implements IPeriodicUpdates {
    private Vector<Spot> spotsOnMap;
    private boolean active = false;

    private static final long serialVersionUID = -4807939956933128721L;
    private final int updateMillis;

    @NeptusProperty
    public boolean showOnlyWhenInteractionIsActive = true;
    @NeptusProperty
    public boolean showNames = true;
    @NeptusProperty
    public boolean showSpeedValue = true;
    @NeptusProperty(userLevel = LEVEL.REGULAR, description = "Set the time window (in hours) for considered positions. Will only consider positions in the last x hours.", name = "Time window (hours)")
    public int hours = 1;

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
        updateMillis = 2 * 60 * 1000;
        spotsOnMap = new Vector<Spot>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }

    @Override
    public boolean update() {
        // Called by Update thread
        updateFromPage();
        return true;
    }


    private void updateFromPage() {
        Vector<Spot> nextSpotsOnMap = new Vector<Spot>();
        HashMap<String, TreeSet<SpotMessage>> msgBySpot;
        try {
            msgBySpot = SpotMsgFetcher.get(hours);
            // if no messages were found or could not reach page do nothing
            if (msgBySpot.size() == 0) {
                Spot.log.debug("No messages in the last " + hours + " hours.");
                return;
            }
            Collection<TreeSet<SpotMessage>> spotIds = msgBySpot.values();
            Spot.log.debug("Gonna run updates on SPOTS. There are " + spotIds.size() + " spots registered");
            TreeSet<SpotMessage> msgTreeSet;
            // iterate over spots mentioned in messages
            for (Iterator<TreeSet<SpotMessage>> iterator = spotIds.iterator(); iterator.hasNext();) {
                msgTreeSet = iterator.next();
                // create spot
                SpotMessage firstMsg = msgTreeSet.first();
                Spot spot = new Spot(firstMsg.id);
                spot.update(msgTreeSet);
                nextSpotsOnMap.add(spot);
            }
            spotsOnMap = nextSpotsOnMap;
            Spot.log.debug("Repaint asked for SpotOverlay");
            repaint();
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            Spot.log.debug("Ran into exception while getting messages from page: " + e.getStackTrace());
            e.printStackTrace();
        }
    }

    @Override
    public void initSubPanel() {
        Spot.log.debug("Init SpotOverlay");
    }

    @Override
    public void cleanSubPanel() {
        PeriodicUpdatesService.unregister(this);// TODO stop using this
        Spot.log.debug("--------- end of session ------------");
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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
                g.drawString(spot.getName(), 12, 0);
            }

            double speedMps = spot.getSpeed();
            if (speedMps != -1) {
                if (showSpeedValue) {
                    g.setColor(Color.black);
                    g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(speedMps) + " m/s", 12, 10);
                }
                g.rotate(spot.direction);
                g.setColor(Color.black);
                g.fill(gp);
                g.setStroke(new BasicStroke(0.9f));
                g.setColor(Color.white);
                g.draw(gp);
            }

        }
        
    }


}
