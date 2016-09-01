/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Braga
 * Set 1, 2016
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
 * @author jb
 *
 */
@LayerPriority(priority=-40)
@PluginDescription(icon="pt/lsts/neptus/mra/replay/geolocation-usbl.png")
public class USBLFixReplay implements LogReplayLayer {
    protected LinkedList<LocationType> fixes = new LinkedList<LocationType>();
    protected LinkedHashMap<String, IMCMessage> fixesToPaint = new LinkedHashMap<>();

    @Override
    public String getName() {
        return I18n.text("USBL Fixes");
    }

    @Override
    public void cleanup() {
        fixes.clear();
        fixesToPaint.clear();
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("UsblFixExtended") != null;
    }

    @Override
    public void parse(IMraLogGroup source) {
        IMraLog log = source.getLog("UsblFixExtended");

        IMCMessage m;
        while ((m = log.nextLogEntry()) != null) {
            if (!source.getSystemName(m.getSrc()).equals(m.getString("target")))
                break;

            double lat = Math.toDegrees(m.getDouble("lat"));
            double lon = Math.toDegrees(m.getDouble("lon"));
            fixes.add(new LocationType(lat, lon));
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"UsblFixExtended"};
    }

    @Override
    public void onMessage(IMCMessage message) {
        fixesToPaint.put(message.getSourceName(), message);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(new Color(153,0,76));
        for (LocationType loc : fixes) {
            Point2D pt = renderer.getScreenPosition(loc);
            g.drawLine((int)pt.getX()-3, (int)pt.getY(), (int)pt.getX()+3, (int)pt.getY());
            g.drawLine((int)pt.getX(), (int)pt.getY()-3, (int)pt.getX(), (int)pt.getY()+3);
        }

        if (fixesToPaint.isEmpty())
            return;

        Vector<IMCMessage> fixes = new Vector<>();
        fixes.addAll(fixesToPaint.values());
        for (IMCMessage fixToPaint : fixes) {
            double lat = Math.toDegrees(fixToPaint.getDouble("lat"));
            double lon = Math.toDegrees(fixToPaint.getDouble("lon"));
            LocationType loc = new LocationType(lat, lon);
    
            g.setColor(Color.blue.darker());

            Point2D pt = renderer.getScreenPosition(loc);
            g.setColor(new Color(153,0,76));
            g.setStroke(new BasicStroke(2f));
            g.drawLine((int)pt.getX()-5, (int)pt.getY(), (int)pt.getX()+5, (int)pt.getY());
            g.drawLine((int)pt.getX(), (int)pt.getY()-5, (int)pt.getX(), (int)pt.getY()+5);
                
            double radius = fixToPaint.getDouble("accuracy") * renderer.getZoom();
    
            Ellipse2D ellis = new Ellipse2D.Double(pt.getX()-radius, pt.getY()-radius, radius*2, radius*2);
            g.setColor(new Color(255,102,178,64));
            g.fill(ellis);
    
            g.setColor(new Color(153,0,76));
            g.draw(ellis);
        }
    } 

    @Override
    public boolean getVisibleByDefault() {
        return true;
    } 
}
