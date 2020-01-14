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
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author zp
 * 
 */
@LayerPriority(priority = -10)
@PluginDescription(icon="pt/lsts/neptus/mra/replay/globe.png")
public class EstimatedStateReplay implements LogReplayLayer {

    protected HashMap<Integer, Vector<LocationType>> positions = new LinkedHashMap<Integer, Vector<LocationType>>();
    protected HashMap<Integer, VehiclePaths> pathsList = new LinkedHashMap<Integer, VehiclePaths>();
    protected HashMap<Integer, Double> lastPositionTime = new HashMap<>(); 
    protected double lastZoom = -1;
    protected double lastRotation = 0;
    
    @Override
    public String getName() {
        return I18n.text("EstimatedState path");
    }

    @Override
    public void cleanup() {
        positions.clear();
        pathsList.clear();
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("EstimatedState") != null;
    }

    @Override
    public void parse(IMraLogGroup source) {
        IMraLog log = source.getLog("EstimatedState");
        IMCMessage m;
        int src;
        
        while ((m = log.nextLogEntry()) != null) {
            src = m.getSrc();
            Vector<LocationType> pos;
            if((pos = positions.get(src)) == null) {
                pos = new Vector<LocationType>();
                VehiclePaths paths = new VehiclePaths();
                VehicleType vt = VehiclesHolder.getVehicleById(m.getSourceName());
                if (vt != null)
                    paths.setColor(VehiclesHolder.getVehicleById(m.getSourceName()).getIconColor());
                else 
                    paths.setColor(new Color(255, 255, 255, 128));
                
                pathsList.put(src, paths);
                positions.put(src, pos);
                lastPositionTime.put(src, 0d);                
            }
            
            if (m.getTimestamp() - lastPositionTime.get(src) >= 1.0) {
                LocationType loc = LogUtils.getLocation(m);
                loc.convertToAbsoluteLatLonDepth();
                pos.add(loc);
                lastPositionTime.put(src, m.getTimestamp());
            }
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {  };
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (renderer.getZoom() != lastZoom) {
            for (int i : positions.keySet()) {
                VehiclePaths paths = pathsList.get(i);
                
                // Initialize paths
                Point2D pivot = renderer.getScreenPosition(positions.get(i).firstElement());
                paths.path1 = new GeneralPath();
                paths.path2 = new GeneralPath();
                paths.path1.moveTo(0, 0);
                paths.path2.moveTo(0, 0);

                // Generate
                for (LocationType loc : positions.get(i)) {
                    Point2D pt = renderer.getScreenPosition(loc);
                    double diffX = pt.getX() - pivot.getX();
                    double diffY = pt.getY() - pivot.getY();
                    paths.path1.lineTo(diffX, diffY);
                    paths.path2.moveTo(diffX, diffY);
                    paths.path2.lineTo(diffX, diffY);
                }
            }
            lastZoom = renderer.getZoom();
            lastRotation = renderer.getRotation();
        }
        
        for(int i : positions.keySet()) {
            Point2D pivot = renderer.getScreenPosition(positions.get(i).firstElement());
            VehiclePaths paths = pathsList.get(i);
            
            g.translate(pivot.getX(), pivot.getY());
            g.rotate(-renderer.getRotation() + lastRotation);
            
            g.setColor(paths.color.darker().darker());
            g.draw(paths.path1);
            
            g.setStroke(new BasicStroke(2f));
            g.setColor(paths.color);
            g.draw(paths.path2);
            
            g.translate(-pivot.getX(), -pivot.getY());
        }
    }

    private class VehiclePaths {
        public GeneralPath path1 = new GeneralPath();
        public GeneralPath path2 = new GeneralPath();
        public Color color;
        
        public void setColor(Color c) {
            this.color = c;
        }
    }
    
    @Override
    public boolean getVisibleByDefault() {
        return true;
    }

    @Override
    public void onMessage(IMCMessage message) {
        // TODO Auto-generated method stub
        
    }
}
