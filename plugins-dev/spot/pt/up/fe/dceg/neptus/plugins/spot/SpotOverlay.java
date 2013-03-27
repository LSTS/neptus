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
import java.io.IOException;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

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
    private static final long serialVersionUID = -4807939956933128721L;
    private final int updateMillis;
    private final String getUrl;
    private final String postUrl;
    private SpotUpdater spotUpdater;
    protected boolean active = false;

    protected final Vector<SpotInfo> spotsOnMap;
    protected StateRenderer2D renderer = null;
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
        getUrl = "http://tiny.cc/spot1";
        postUrl = "http://whale.fe.up.pt/neptleaves/state";
        updateMillis = 60000;
        arrow = ImageUtils.getImage("images/spotArrow.png");
        spotsOnMap = new Vector<SpotInfo>();
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

        // lancar thread para ir buscar dados, processa-los e dp pedir o repaint
        try {
            spotUpdater.update(getUrl, postUrl);
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void initSubPanel() {
        spotUpdater = new SpotUpdater();
    }

    @Override
    public void cleanSubPanel() {
        PeriodicUpdatesService.unregister(this);
        spotsOnMap.clear();
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
        this.renderer = renderer;
        Graphics2D graphicsClone;
//        Graphics2D graphicsClone = (Graphics2D)g.create();
//        graphicsClone.dispose();
        
        if (showOnlyWhenInteractionIsActive && !active)
            return;

        // if (lastThread != null) {
        // g.drawString("Updating AIS layer...", 10, 15);
        // }

        for (SpotInfo spot : spotsOnMap) {
            LocationType spotLoc = spot.getLastLocation();

            // g.translate(pt.getX(), pt.getY());

            if (showNames) {
                g.setColor(Color.red.darker().darker());
                g.drawString(spot.getName(), 5, 5);
            }

            double speedMps = spot.getSpeed();
            if (showSpeedValue) {
                g.setColor(Color.black);
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(speedMps) + " m/s", 5, 15);
            }

            graphicsClone = (Graphics2D)g.create();
            graphicsClone.drawImage(arrow, arrow.getWidth(renderer), arrow.getHeight(renderer), arrow.getWidth(null), arrow.getHeight(null), null);
            graphicsClone.rotate(spot.getDirection());
            Point2D pt = renderer.getScreenPosition(spotLoc);
            graphicsClone.translate(pt.getX(), pt.getY());
            // g.setColor(Color.red);
            // if (speedMps == 0) {
            // g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            // }
            // else {
            // g.rotate(spot.getHeadingRads());
            // g.fill(path);
            // g.rotate(-spot.getHeadingRads());
            // }

            // g.translate(-pt.getX(), -pt.getY());

        }
        
    }


}
