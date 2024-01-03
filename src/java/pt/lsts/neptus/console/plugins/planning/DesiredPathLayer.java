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
 * Author: José Pinto
 * Jun 25, 2012
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PathControlState;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="DesiredPath Layer", icon="images/icons/carrot.png")
@LayerPriority(priority=-5)
public class DesiredPathLayer extends ConsoleLayer {

    @NeptusProperty(name="Consider start of path to be the vehicle's position")
    public boolean startFromCurrentLocation = true;

    protected Map<Integer, PathControlState> lastMsgs = Collections.synchronizedMap(new LinkedHashMap<Integer, PathControlState>());

    @Periodic(millisBetweenUpdates=5000)
    public void gc() {
        Vector<Integer> keys = new Vector<Integer>();
        keys.addAll(lastMsgs.keySet());
        for (Integer key : keys) {
            PathControlState pcs = lastMsgs.get(key);
            if (System.currentTimeMillis() - pcs.getTimestampMillis() > 5000)
                lastMsgs.remove(key);
        }
    }
    

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Vector<PathControlState> states = new Vector<PathControlState>();
        states.addAll(lastMsgs.values());
        for (PathControlState pcs : states) {
            LocationType dest = new LocationType(Math.toDegrees(pcs.getEndLat()), Math.toDegrees(pcs.getEndLon()));
            LocationType src = new LocationType(Math.toDegrees(pcs.getStartLat()), Math.toDegrees(pcs.getStartLon()));
            ImcSystem system = ImcSystemsHolder.lookupSystem(pcs.getSrc());

            if (system!= null && startFromCurrentLocation)
                src = system.getLocation();

            double lradius = pcs.getLradius() * renderer.getZoom();
            LocationType destCenter = new LocationType(dest);
            
            if (lradius > 0) {
                dest.setAzimuth(Math.toDegrees(dest.getXYAngle(src)));
                dest.setOffsetDistance(pcs.getLradius());
            }
            
            Point2D pt = renderer.getScreenPosition(dest);
            Point2D ptCenter = renderer.getScreenPosition(destCenter);
            g.setStroke(new BasicStroke(3));
            g.setColor(Color.black.darker());
            
            if (lradius == 0)
                g.fill(new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10));
            else                 
                g.draw(new Ellipse2D.Double(ptCenter.getX() - lradius, ptCenter.getY()-lradius, lradius*2, lradius*2));
            
            Point2D ptSrc = renderer.getScreenPosition(src);
            
            g.draw(new Line2D.Double(ptSrc, pt));
            g.setColor(Color.yellow);
            g.setStroke(new BasicStroke(1.5f));                
            g.draw(new Line2D.Double(ptSrc, pt));
            if (lradius == 0)
                g.fill(new Ellipse2D.Double(pt.getX() - 4, pt.getY() - 4, 8, 8));
            else
                g.draw(new Ellipse2D.Double(ptCenter.getX() - lradius, ptCenter.getY()-lradius, lradius*2, lradius*2));

        }
    }

    @Subscribe
    public void consume(PathControlState pcs) {
        lastMsgs.put(pcs.getSrc(), pcs);
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        
    }

    @Override
    public void cleanLayer() {
        lastMsgs.clear();
    }        
}
