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
 * $Id:: GPSFixReplay.java 9952 2013-02-19 18:24:10Z jqcorreia                  $:
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.LinkedList;

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
@LayerPriority(priority=-40)
public class GPSFixReplay implements LogReplayLayer {

    protected LinkedList<LocationType> validFixes = new LinkedList<LocationType>();
    protected LinkedList<LocationType> invalidFixes = new LinkedList<LocationType>();
    protected IMCMessage fixToPaint = null;

    @Override
    public String getName() {
        return I18n.text("GPS Fixes");
    }

    
    @Override
    public void cleanup() {
        validFixes.clear();
        invalidFixes.clear();
        fixToPaint = null;
    }
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("GpsFix") != null;
    }

    @Override
    public void parse(IMraLogGroup source) {
        IMraLog log = source.getLog("GpsFix");

        IMCMessage m;
        while ((m = log.nextLogEntry()) != null) {            
            double lat = Math.toDegrees(m.getDouble("lat"));
            double lon = Math.toDegrees(m.getDouble("lon")) ;
            LinkedHashMap<String, Boolean> validity = m.getBitmask("validity");

            if (validity.get("VALID_POS"))
                validFixes.add(new LocationType(lat, lon));
            else if (lat != 0 && lon != 0) {
                while (lat > 90)
                    lat -= 90;                
                while (lat < 0)
                    lat += 90;

                while (lon > 180)
                    lon -= 180;

                while (lon < 0)
                    lon += 180;
                if (lat != 0 && lon != 0)
                    invalidFixes.add(new LocationType(lat, lon));
            }
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"GpsFix"};
    }

    @Override
    public void onMessage(IMCMessage message) {
        fixToPaint = message;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        g.setColor(Color.orange.darker());
        for (LocationType loc : validFixes) {
            Point2D pt = renderer.getScreenPosition(loc);
            g.drawLine((int)pt.getX()-3, (int)pt.getY(), (int)pt.getX()+3, (int)pt.getY());
            g.drawLine((int)pt.getX(), (int)pt.getY()-3, (int)pt.getX(), (int)pt.getY()+3);
        }

        if (fixToPaint == null)
            return;

        double lat = Math.toDegrees(fixToPaint.getDouble("lat"));
        double lon = Math.toDegrees(fixToPaint.getDouble("lon"));

        LinkedHashMap<String, Boolean> validity = fixToPaint.getBitmask("validity");
        LocationType loc = new LocationType(lat, lon);

        if (validity.get("VALID_POS")) {
            g.setColor(Color.green.darker());

            Point2D pt = renderer.getScreenPosition(loc);
            g.setColor(new Color(255,0,25));
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int)pt.getX()-5, (int)pt.getY(), (int)pt.getX()+5, (int)pt.getY());
            g.drawLine((int)pt.getX(), (int)pt.getY()-5, (int)pt.getX(), (int)pt.getY()+5);
            
            double radius = fixToPaint.getDouble("hacc") * renderer.getZoom();

            Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-radius, pt.getY()-radius, radius*2, radius*2);
            g.setColor(new Color(255,128,128,64));
            g.fill(ellis);

            g.setColor(new Color(255,128,25));
            g.draw(ellis);
        }
    } 
    @Override
    public boolean getVisibleByDefault() {
        return true;
    } 
}
