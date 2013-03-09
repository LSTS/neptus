/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 12, 2011
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;

/**
 * @author zp
 * 
 */
@LayerPriority(priority = -10)
public class EstimatedStateReplay implements LogReplayLayer {

    protected HashMap<Integer, Vector<LocationType>> positions = new LinkedHashMap<Integer, Vector<LocationType>>();
    protected Vector<Double> timestamps = new Vector<Double>();
    protected HashMap<Integer, VehiclePaths> pathsList = new LinkedHashMap<Integer, VehiclePaths>();
    
    protected int currentPos = 0;
    protected double lastZoom = -1;
    protected double lastRotation = 0;

    @Override
    public String getName() {
        return I18n.text("EstimatedState path");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.mra.replay.LLFReplayLayer#cleanup()
     */
    @Override
    public void cleanup() {
        positions.clear();
        timestamps.clear();
        pathsList.clear();
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
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
                paths.setColor(VehiclesHolder.getVehicleWithImc(new ImcId16(src)).getIconColor());
                pathsList.put(src, paths);
                positions.put(src, pos);
            }
            
            LocationType loc = LogUtils.getLocation(m);
            loc.convertToAbsoluteLatLonDepth();
            pos.add(loc);
            timestamps.add(m.getTimestamp());
            log.advance(500);
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    @Override
    public void onMessage(IMCMessage message) {
        double curTime = message.getTimestamp();
        while (currentPos < timestamps.size() && timestamps.get(currentPos) < curTime)
            currentPos++;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        // If zoom changed then recalculate paths
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
}
