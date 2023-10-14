/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 25, 2012
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PathControlState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.messages.MessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="PathControlLayer")
@LayerPriority(priority=-5)
public class PathControlLayer extends ConsolePanel implements Renderer2DPainter {

    
    @NeptusProperty(name="Consider start of path to be the vehicle's position")
    public boolean startFromCurrentLocation = true;

    private static final long serialVersionUID = 1L;
    protected final int pcontrol_id = IMCDefinition.getInstance().getMessageId("PathControlState");

    protected Map<Integer, PathControlState> lastMsgs = Collections.synchronizedMap(new LinkedHashMap<Integer, PathControlState>());

    /**
     * @param console
     */
    public PathControlLayer(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
        ImcMsgManager.getManager().addListener(this, new MessageFilter<MessageInfo, IMCMessage>() {            
            @Override
            public boolean isMessageToListen(MessageInfo info, IMCMessage msg) {
                return msg.getMgid() == pcontrol_id;
            }
        });
    }

    @Override
    public void cleanSubPanel() {
        ImcMsgManager.getManager().removeListener(this);
    }

    @Periodic(millisBetweenUpdates=5000)
    public void gc() {
        Vector<Integer> keys = new Vector<Integer>();
        keys.addAll(lastMsgs.keySet());
        for (Integer key : keys) {
            PathControlState pcs = lastMsgs.get(key);
            if (System.currentTimeMillis() - pcs.getTimestampMillis() > 5000)
                lastMsgs.remove(key);
        }
    }
    

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Vector<PathControlState> states = new Vector<PathControlState>();
        states.addAll(lastMsgs.values());
        for (PathControlState pcs : states) {
            LocationType dest = new LocationType(Math.toDegrees(pcs.getEndLat()), Math.toDegrees(pcs.getEndLon()));
            LocationType src = new LocationType(Math.toDegrees(pcs.getStartLat()), Math.toDegrees(pcs.getStartLon()));
            ImcSystem system = ImcSystemsHolder.lookupSystem(pcs.getSrc());

            Point2D pt = renderer.getScreenPosition(dest);
            g.setColor(Color.black);
            g.draw(new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10));

            if (system != null) {
                if (startFromCurrentLocation) {
                    src = system.getLocation();
                }
                Point2D ptSrc = renderer.getScreenPosition(src);
                g.draw(new Line2D.Double(ptSrc, pt));
            }
        }
    }

    @Subscribe
    public void consume(PathControlState pcs) {
        lastMsgs.put(pcs.getSrc(), pcs);
    }        
}
