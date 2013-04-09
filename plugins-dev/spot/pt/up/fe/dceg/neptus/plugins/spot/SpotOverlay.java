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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.plugins.update.PeriodicUpdatesService;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Margarida Faria
 *
 */
@PluginDescription(author = "Margarida", name = "SPOT Overlay")
public class SpotOverlay extends SimpleRendererInteraction implements IPeriodicUpdates {
    private final ArrayList<Spot> spotsOnMap;
    private boolean active = false;

    private static final long serialVersionUID = -4807939956933128721L;
    private final int updateMillis;
    private Timer timer;
    private final Image arrow;

    @NeptusProperty
    public boolean showOnlyWhenInteractionIsActive = true;
    @NeptusProperty
    public boolean showNames = true;
    @NeptusProperty
    public boolean showSpeedValue = true;

    /**
     * @param console
     */
    public SpotOverlay(ConsoleLayout console) {
        super(console);
        updateMillis = 60000;
        arrow = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/spot/images/spotArrow.png");
        spotsOnMap = new ArrayList<Spot>();
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

        return true;
    }

    @Override
    public void initSubPanel() {
        Spot.log.debug("Init SpotOverlay");
        spotsOnMap.add(new Spot(SpotPageKeys.LSTSSPOT));
        timer = new Timer();
        int delayToStart = 0;// 1 * 60 * 10000;
        int delayBetweenTasks = 10 * 60 * 10000;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Spot.log.debug("Gonna run updates on SPOTS. There are " + spotsOnMap.size() + " spots registered");
                for (Spot spot : spotsOnMap) {
                    spot.update();
                    Spot.log.debug("Repaint asked for SpotOverlay");
                    repaint();
                }
            }
        }, delayToStart, delayBetweenTasks);
    }

    @Override
    public void cleanSubPanel() {
        PeriodicUpdatesService.unregister(this);// TODO stop using this
        timer.cancel();
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
        super.paint(g, renderer);
        // Graphics2D graphicsClone;
//        Graphics2D graphicsClone = (Graphics2D)g.create();
//        graphicsClone.dispose();
        
        // System.out.println("showOnlyWhenInteractionIsActive && !active:" + (showOnlyWhenInteractionIsActive &&
        // !active)
        // + "; showOnlyWhenInteractionIsActive " + showOnlyWhenInteractionIsActive + " active" + active);
        //
        // if (showOnlyWhenInteractionIsActive && !active)
        // return;

        // if (lastThread != null) {
        // g.drawString("Updating AIS layer...", 10, 15);
        // }
        Spot.log.debug(spotsOnMap.size() + " spots:");
        for (Spot spot : spotsOnMap) {
            LocationType spotLoc = spot.getLastLocation();
            Spot.log.debug(spot.getName() + " at " + spotLoc);
            if (spotLoc == null) {
                continue;
            }
            Point2D pt = renderer.getScreenPosition(spotLoc);
            double xScreenPos = pt.getX();
            double yScreenPos = pt.getY();
            g.translate(xScreenPos, yScreenPos);
            Spot.log.debug("; rendered at (" + xScreenPos + ", " + yScreenPos + ")");

            if (showNames) {
                g.setColor(Color.red.darker().darker());
                g.drawString(spot.getName(), 5, 5);
            }

            double speedMps = spot.getSpeed();
            if (showSpeedValue) {
                g.setColor(Color.black);
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(speedMps) + " m/s", 5, 15);
            }

            int xArrowScreenCoord = 0;// arrow.getWidth(renderer);
            int yArrowScreenCoord = 0; // arrow.getHeight(renderer);
            int widthArrow = arrow.getWidth(null);
            int heightArrow = arrow.getHeight(null);
            g.drawImage(arrow, xArrowScreenCoord, yArrowScreenCoord, widthArrow, heightArrow, null);
            Spot.log.debug(", arrow: coords(" + xArrowScreenCoord + "," + yArrowScreenCoord + ") dimensions:("
                    + widthArrow + ", " + heightArrow + ")");
            // g.rotate(spot.getDirection());

            // g.setColor(Color.red);
            // if (speedMps == 0) {
            // g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            // }
            // else {
            // g.rotate(spot.getHeadingRads());
            // g.fill(path);
            // g.rotate(-spot.getHeadingRads());
            // }

            g.translate(-xScreenPos, -yScreenPos);

        }
        
    }


}
