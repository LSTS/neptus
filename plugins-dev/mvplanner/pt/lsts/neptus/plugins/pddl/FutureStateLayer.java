/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 12, 2017
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventFutureState;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author zp
 *
 */
@PluginDescription(description="This layer shows the estimated locations where the vehicles will pop-up")
public class FutureStateLayer extends ConsoleLayer {

    private ConcurrentHashMap<String, ConsoleEventFutureState> futureStates = new ConcurrentHashMap<>();
    
    @Subscribe
    public void on(ConsoleEventFutureState futureState) {
        futureStates.put(futureState.getVehicle(), futureState);
    }    
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        
    }

    @Override
    public void cleanLayer() {
        futureStates.clear();
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        
        GeneralPath gp = new GeneralPath();
        gp.moveTo(-5, 0);
        gp.lineTo(0, 20);
        gp.lineTo(5, 0);
        gp.closePath();
        
        for (ConsoleEventFutureState state : futureStates.values()) {
            if (state.getState() == null || state.getDate() == null)
                continue;
            
            Point2D pt = renderer.getScreenPosition(state.getState().getPosition());
            
            String date = DateTimeUtil.milliSecondsToFormatedString(state.getDate().getTime()-System.currentTimeMillis());
            String txt = "["+state.getVehicle()+"] "+date;
            Rectangle2D rect = g.getFontMetrics().getStringBounds(txt, g);
            
            double tx = pt.getX()-rect.getWidth()/2;
            double ty = pt.getY()-rect.getHeight()/2 - 30;
            
            g.setColor(new Color(255,255,255,128));
            g.fill(new Rectangle2D.Double(tx-3, ty-3, rect.getWidth()+6, rect.getHeight()+6));
            
            AffineTransform at = new AffineTransform();
            at.translate(pt.getX(), pt.getY()-rect.getHeight()-6);
            GeneralPath gp2 = new GeneralPath(gp);
            gp2.transform(at);
            g.fill(gp2);
            
            g.setColor(Color.black);
            g.drawString(txt, (int)tx, (int)(ty+rect.getHeight()));
            
            
        }
    }
}
