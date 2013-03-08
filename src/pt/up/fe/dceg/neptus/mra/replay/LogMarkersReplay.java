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
 * Dec 5, 2012
 * $Id:: LogMarkersReplay.java 9952 2013-02-19 18:24:10Z jqcorreia              $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;

/**
 * @author zp
 */
public class LogMarkersReplay implements LogReplayLayer {

    ArrayList<LogMarker> markers = new ArrayList<>();
    Vector<LocationType> locations = new Vector<>();

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {        
        
        for (int i = 0 ; i < markers.size(); i++) {
            Point2D pt = renderer.getScreenPosition(locations.get(i));

            Rectangle2D bounds = g.getFontMetrics().getStringBounds(markers.get(i).label, g);

            g.setColor(new Color(255,255,255,128));

            RoundRectangle2D r = new RoundRectangle2D.Double(pt.getX()-1, pt.getY()-21, bounds.getWidth()+2, bounds.getHeight()+2, 5, 5);
            GeneralPath gp = new GeneralPath();
            gp.moveTo(pt.getX(), pt.getY());
            gp.lineTo(pt.getX(), pt.getY()-15);
            gp.lineTo(pt.getX()+10, pt.getY()-15);
            gp.lineTo(pt.getX(), pt.getY());
            gp.closePath();
            Area a = new Area(gp);
            a.add(new Area(r));

            g.fill(a);
            g.setColor(Color.black);

            g.drawString(markers.get(i).label, (int)(pt.getX()+1), (int)(pt.getY()-8));
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {    
        return true;
    }

    @Override
    public String getName() {
        return "Log markers layer";
    }

    public void addMarker(LogMarker m) {
        synchronized (markers) {
            if (!markers.contains(m)) {
                markers.add(m);
                locations.add(m.getLocation());
            }
        }
        
        
    }


    public void removeMarker(LogMarker m) {
        synchronized (markers) {
            int ind = markers.indexOf(m);
            if (ind != -1) {
                markers.remove(m);
                locations.remove(ind);
            }
        }        
    }



    @Override
    public void parse(IMraLogGroup source) {
        synchronized (markers) {
            markers = LogUtils.getMarkersFromSource(source);
            for (LogMarker m : markers)
                locations.add(m.getLocation());
        }
    }

    @Override
    public String[] getObservedMessages() {
        return null;
    }

    @Override
    public void onMessage(IMCMessage message) {
    }

    @Override
    public boolean getVisibleByDefault() {
        return true;
    }

    @Override
    public void cleanup() {

    }

}
