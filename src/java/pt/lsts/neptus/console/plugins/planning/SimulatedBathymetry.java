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
 * Author: zp
 * Aug 6, 2013
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class SimulatedBathymetry implements Renderer2DPainter, Serializable {

    private static final long serialVersionUID = -2381547139906945494L;
    protected double defaultDepth = 10;
    protected double minDistToSounding = 200;
    protected LinkedHashMap<LocationType, Double> soundings = new LinkedHashMap<>();
    private static final File file = new File("conf/bathymetry.obj");
    
    private SimulatedBathymetry() {
        
    }
    
    private static SimulatedBathymetry instance = null;
    
    public static SimulatedBathymetry getInstance() {
        if (instance == null)
            instance = load();
        return instance;
    }
    
    public void addSoundings(Map<LocationType, Double> data) {
        soundings.putAll(data);
        store(this);
    }
    
    public void addSounding(LocationType loc, double depth) {
        soundings.put(loc, depth);
        store(this);
    }
    
    public void clearSoundings() {
        soundings.clear();
        store(this);
    }
    
    public Map<LocationType, Double> getSoundings() {
        return Collections.unmodifiableMap(soundings);
    }
    
    /**
     * @return the defaultDepth
     */
    public double getDefaultDepth() {
        return defaultDepth;
    }

    /**
     * @param defaultDepth the defaultDepth to set
     */
    public void setDefaultDepth(double defaultDepth) {
        this.defaultDepth = defaultDepth;
        store(this);
    }

    public double getSimulatedDepth(LocationType loc) {
        if (soundings.size() == 0)
            return defaultDepth;
        else if (soundings.size() == 1)
            return soundings.values().iterator().next();
        else {
            double dTotal = 0;
            double valTotal = 0;
            for (Entry<LocationType, Double> sounding : soundings.entrySet()) {
                double dist = loc.getHorizontalDistanceInMeters(sounding.getKey());
                
                dist = Math.pow(dist, 3);
                if (dist > 0.0) {
                    dist = 1.0 / dist;
                }
                else { // if d is real small set the inverse to a large number 
                    // to avoid INF
                    dist = 1.e20;
                }
                valTotal += dist * sounding.getValue();
                dTotal += dist;
            }
            
            return valTotal / dTotal;
        }       
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(Color.orange);
        for (Entry<LocationType, Double> sounding : soundings.entrySet()) {
            Point2D pt = renderer.getScreenPosition(sounding.getKey());
            g.draw(new Line2D.Double(pt.getX()-3, pt.getY(), pt.getX()+3, pt.getY()));
            g.draw(new Line2D.Double(pt.getX(), pt.getY()-3, pt.getX(), pt.getY()+3));            
            g.drawString(String.format("%.1f m", sounding.getValue()), (int)pt.getX()+5, (int)pt.getY()+5);
        }
    }
    
    private static void store(SimulatedBathymetry obj) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(obj);
            oos.close();            
        }
        catch (Exception e) {
            NeptusLog.pub().error("Could not store bathymetry to file", e);            
        }
    }
    
    private static SimulatedBathymetry load() {
        if (!file.canRead())
            return new SimulatedBathymetry();
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            SimulatedBathymetry obj = (SimulatedBathymetry) ois.readObject();
            ois.close();
            return obj;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Could not load bathymetry from file", e);
            return new SimulatedBathymetry();
        }        
    }
}
