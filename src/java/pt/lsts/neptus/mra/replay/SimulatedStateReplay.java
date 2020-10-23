/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Dec 12, 2011
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;


/**
 * @author zp
 *
 */
@LayerPriority(priority=100)
@PluginDescription
public class SimulatedStateReplay implements LogReplayLayer {

    protected Vector<LocationType> positions = new Vector<LocationType>();    
    protected Vector<Double> timestamps = new Vector<Double>();
    protected int currentPos = 0;
    protected GeneralPath path1 = null;
    protected GeneralPath path2 = null;
    protected double lastZoom = -1;
    protected double lastRotation = 0;
    protected LocationType navStartup = null;
    protected LocationType pos = null;
    
    @Override
    public String getName() {
        return I18n.text("SimulatedState path");
    }

    @Override
    public void cleanup() {
        positions.clear();
        timestamps.clear();
        path1 = path2 = null;
    }
    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("SimulatedState") != null && source.getLog("NavigationStartupPoint") != null;
    }

    @Override
    public void parse(IMraLogGroup source) {
        IMraLog log = source.getLog("NavigationStartupPoint");
        
//        NeptusLog.pub().info("<###> "+log.getNumberOfEntries());
        
        IMCMessage m = log.firstLogEntry();
        
        navStartup = new LocationType();
        
        navStartup.setLatitudeDegs(Math.toDegrees(m.getDouble("lat")));
        navStartup.setLongitudeDegs(Math.toDegrees(m.getDouble("lon")));
        navStartup.setDepth(m.getDouble("depth"));
        
        log = source.getLog("SimulatedState");
        
        while ((m = log.nextLogEntry()) != null) {
            LocationType loc = new LocationType(navStartup);
            loc.setOffsetNorth(m.getDouble("x"));
            loc.setOffsetEast(m.getDouble("y"));
            loc.setOffsetDown(m.getDouble("z"));
            loc.convertToAbsoluteLatLonDepth();
            positions.add(loc);
            timestamps.add(m.getTimestamp());
            log.advance(500);
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"SimulatedState"};
    }

    @Override
    public void onMessage(IMCMessage message) {
        /*double curTime = message.getTimestamp();
        while (currentPos < timestamps.size() && timestamps.get(currentPos) < curTime)
            currentPos++;*/
        if (!message.getAbbrev().equals("SimulatedState"))
            return;
        
        if (navStartup == null)
            return;
        
        pos = new LocationType(navStartup);
        
        pos.setOffsetNorth(message.getDouble("x"));
        pos.setOffsetEast(message.getDouble("y"));
        pos.setOffsetDown(message.getDouble("z"));
        pos.convertToAbsoluteLatLonDepth();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
       if (pos != null) {
            Point2D pt = renderer.getScreenPosition(pos);
            g.setColor(Color.white);
            g.fill(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10));
            g.setColor(Color.blue.brighter().brighter());
            g.draw(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10));            
        }
        
        Point2D pivot = renderer.getScreenPosition(positions.firstElement());
        
        if (renderer.getZoom() != lastZoom) {
            path1 = new GeneralPath();
            path2 = new GeneralPath();
            path1.moveTo(0, 0);
            path2.moveTo(0, 0);
            for (LocationType loc : positions) {
                Point2D pt = renderer.getScreenPosition(loc);
                double diffX = pt.getX()-pivot.getX();
                double diffY = pt.getY()-pivot.getY();
                path1.lineTo(diffX, diffY);
                path2.moveTo(diffX, diffY);
                path2.lineTo(diffX, diffY);
                path2.lineTo(diffX, diffY);
            }            
            lastZoom = renderer.getZoom();
            lastRotation = renderer.getRotation();
        }
        g.translate(pivot.getX(), pivot.getY());
        g.rotate(-renderer.getRotation()+lastRotation);
        
        
        g.setColor(Color.gray);
        g.draw(path1);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(Color.blue.darker());
        g.draw(path2);
    } 

    @Override
    public boolean getVisibleByDefault() {
        return true;
    }

}
