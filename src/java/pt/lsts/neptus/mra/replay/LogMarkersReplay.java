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
 * Dec 5, 2012
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.llf.LsfReportProperties;

/**
 * @author zp
 */
@PluginDescription(icon="images/menus/marker.png")
public class LogMarkersReplay implements LogReplayLayer, LogMarkerListener {

    ArrayList<LogMarker> markers = new ArrayList<>();
    Vector<LocationType> locations = new Vector<>();
    IMraLogGroup source = null;
    StateRenderer2D renderer = null;
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {    
        
        this.renderer = renderer;

        for (int i = 0 ; i < markers.size(); i++) {
            Point2D pt = renderer.getScreenPosition(locations.get(i));

            Rectangle2D bounds = g.getFontMetrics().getStringBounds(markers.get(i).getLabel(), g);

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

            g.drawString(markers.get(i).getLabel(), (int)(pt.getX()+1), (int)(pt.getY()-8));
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {    
        return true;
    }

    @Override
    public String getName() {
        return I18n.text("Log markers layer");
    }

    public void addMarker(LogMarker m) {
        synchronized (markers) {
            if (!markers.contains(m)) {
                markers.add(m);
                locations.add(m.getLocation());
            }
        }    
        if(renderer != null)
            renderer.repaint();
    }


    public void removeMarker(LogMarker m) {

        if (LsfReportProperties.generatingReport==true){
            //GuiUtils.infoMessage(getRootPane(), I18n.text("Can not remove Marks"), I18n.text("Can not remove Marks - Generating Report."));
            return;
        }

        synchronized (markers) {
            int ind = markers.indexOf(m);
            if (ind != -1) {
                markers.remove(m);
                locations.remove(ind);
            }
        }
    }
    
    @Override
    public void addLogMarker(LogMarker marker) {
        addMarker(marker);
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        removeMarker(marker);
    }
    

    @Override
    public void goToMarker(LogMarker marker) {
        //nothing
    }
    
    @Override
    public void parse(IMraLogGroup source) {
        Collection<LogMarker> sourceMarkers = LogMarker.load(source);
        for (LogMarker lm : sourceMarkers) {
            addMarker(lm); 
        }        
        this.source = source;
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
