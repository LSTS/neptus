/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Aug 21, 2013
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;

import pt.lsts.imc.Announce;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(icon="pt/lsts/neptus/mra/replay/lighthouse.png")
public class AnnouncesReplay implements LogReplayLayer {

    protected LinkedHashMap<String, LocationType> systemsPositions = new LinkedHashMap<>();
    protected LinkedHashMap<String, Double> lastAnnounces = new LinkedHashMap<>(); 
    protected double curTime = 0;
    
    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLsfIndex().containsMessagesOfType("Announce");
    }
    
    @Override
    public void cleanup() {
        
    }
    
    @Override
    public String getName() {
        return "Announces replay";
    }
    
    @Override
    public String[] getObservedMessages() {
        return new String[] {"Announce", "EstimatedState"};
    }
    
    @Override
    public boolean getVisibleByDefault() {
        return false;
    }
    
    @Override
    public void onMessage(IMCMessage message) {
        if (message.getMgid() == Announce.ID_STATIC) {
            LocationType loc = new LocationType();
            loc.setLatitudeRads(message.getDouble("lat"));
            loc.setLongitudeRads(message.getDouble("lon"));
            systemsPositions.put(message.getString("sys_name"), loc);
            lastAnnounces.put(message.getString("sys_name"), message.getTimestamp());
        }
        else {
            curTime = message.getTimestamp();
        }
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        for (String name : systemsPositions.keySet()) {
            Graphics2D g2 = (Graphics2D) g.create();
          Point2D pt = renderer.getScreenPosition(systemsPositions.get(name));
          
          double lastTime = lastAnnounces.get(name);
          if (curTime - lastTime > 20)
              g2.setColor(Color.gray);
          else
              g2.setColor(Color.orange);
          
          g2.translate(pt.getX(), pt.getY());
          g2.fill(new Rectangle2D.Double(-3, -3, 6, 6));
          g2.setColor(Color.black);
          g2.drawString(name, 5, 0);
        }
    }
    
    @Override
    public void parse(IMraLogGroup source) {
        //LsfIterator<Announce> announces = source.getLsfIndex().getIterator(Announce.class);
        
        //for (Announce ann : announces) {
            
        //}
    }
}
