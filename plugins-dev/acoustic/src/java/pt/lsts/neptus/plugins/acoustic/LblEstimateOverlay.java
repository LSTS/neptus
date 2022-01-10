/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Sep 3, 2013
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.LblEstimate;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "ZP", name = "LBL Estimates Overlay", icon = "pt/lsts/neptus/plugins/acoustic/lbl.png", description = "Displays LBL position estimations calculated by AUVs", category = CATEGORY.INTERFACE)
public class LblEstimateOverlay extends ConsolePanel implements Renderer2DPainter {

    private static final long serialVersionUID = -1669624418060388974L;
    protected LinkedHashMap<String, LblEstimate> lastEstimates = new LinkedHashMap<>();
    protected LinkedHashMap<String, Long> lastWarnings = new LinkedHashMap<>();
    protected static final long millisBetweenWarnings = 60 * 1000;

    @NeptusProperty(name = "Paint received estimates on the map")
    public boolean paintEstimates = true;
        
    @NeptusProperty
    public boolean simulation = true;
    
    @NeptusProperty(name="Distance above which user will be warned about estimations")
    public double minDistanceForWarning = 5;
    
    
    public LblEstimateOverlay(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void paint(Graphics2D g2d, StateRenderer2D renderer) {
        
        if (!paintEstimates)
            return;
        double zoom = renderer.getZoom();
        
        for (Entry<String, LblEstimate> estimateEntry : lastEstimates.entrySet()) {
            Graphics2D g = (Graphics2D) g2d.create();
            LocationType loc = new LocationType(Math.toDegrees(estimateEntry.getValue().getBeacon().getLat()),
                    Math.toDegrees(estimateEntry.getValue().getBeacon().getLon()));
            //loc.translatePosition(estimateEntry.getValue().getX(), estimateEntry.getValue().getY(), 0);

            Point2D center = renderer.getScreenPosition(loc);
            g.translate(center.getX(), center.getY());
            g.rotate(-renderer.getRotation());
            boolean outside = estimateEntry.getValue().getDistance() > Math.max(estimateEntry.getValue().getVarX(),
                    estimateEntry.getValue().getVarY());

            if (!outside)
                g.setColor(new Color(0, 255, 0, 128));
            else
                g.setColor(new Color(192, 64, 0, 128));

            double northVar = estimateEntry.getValue().getVarX() * zoom;
            double eastVar = estimateEntry.getValue().getVarY() * zoom;
            g.fill(new Ellipse2D.Double(-eastVar, -northVar, eastVar * 2, northVar * 2));
            if ((System.currentTimeMillis()/1000) %2 == 0) {
                if (outside)
                    g.setColor(Color.red.brighter());
                else
                    g.setColor(Color.green.brighter());
            }
            else
                g.setColor(Color.black);
            
            g.fill(new Ellipse2D.Double(-2.5, -2.5, 5, 5));
        }
    }

    @Subscribe
    public void on(LblEstimate estimate) {
        String beacon = estimate.getBeacon().getBeacon();
        lastEstimates.put(beacon, estimate);

        boolean warn = estimate.getDistance() > Math.max(estimate.getVarX(), estimate.getVarY());
        warn &= estimate.getDistance() > 5;
        
        if (warn && getConsole() != null) {
            if (!lastWarnings.containsKey(beacon)
                    || (System.currentTimeMillis() - lastWarnings.get(beacon)) > millisBetweenWarnings) {
                post(Notification.warning("Potential LBL mis-configuration", "The system " + estimate.getSourceName()
                        + " is estimating that "+beacon+" is " + estimate.getDistance()
                        + " meters away from current configuration."));
                lastWarnings.put(beacon, System.currentTimeMillis());
            }
        }
    }

}
