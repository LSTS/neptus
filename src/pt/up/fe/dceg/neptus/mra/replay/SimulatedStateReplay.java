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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;


/**
 * @author zp
 *
 */
@LayerPriority(priority=100)
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
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("SimulatedState") != null && source.getLog("NavigationStartupPoint") != null;
    }

    @Override
    public void parse(IMraLogGroup source) {
        IMraLog log = source.getLog("NavigationStartupPoint");
        
//        System.out.println(log.getNumberOfEntries());
        
        IMCMessage m = log.firstLogEntry();
        
        navStartup = new LocationType();
        
        navStartup.setLatitude(Math.toDegrees(m.getDouble("lat")));
        navStartup.setLongitude(Math.toDegrees(m.getDouble("lon")));
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
