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
 * Jun 25, 2012
 * $Id:: PathControlLayer.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PathControlState;
import pt.up.fe.dceg.neptus.messages.MessageFilter;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
@PluginDescription(name="PathControlLayer")
public class PathControlLayer extends SimpleSubPanel implements Renderer2DPainter, MessageListener<MessageInfo, IMCMessage> {


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


    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        g.drawString(I18n.text("Path Control Layer"), 10, 16);
        g.setColor(new Color(0,0,0,128));
        g.setStroke(new BasicStroke(1.5f));
        
       // System.out.println(lastMsgs.values());
        
        for (PathControlState pcs : lastMsgs.values()) {
            
            //if (pcs.get_flags() != 0) {
                LocationType dest = new LocationType(Math.toDegrees(pcs.getEndLat()), Math.toDegrees(pcs.getEndLon()));
                ImcSystem system = ImcSystemsHolder.lookupSystem(pcs.getSrc());
                
                Point2D pt = renderer.getScreenPosition(dest);
                
                g.draw(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10));
                
                if (system != null) {
                    LocationType src = system.getLocation();
                    Point2D ptSrc = renderer.getScreenPosition(src);
                    g.draw(new Line2D.Double(ptSrc, pt));
                }
           // }
        }
    }

    @Override
    public void onMessage(MessageInfo arg0, IMCMessage msg) {
        if (msg.getMgid() == pcontrol_id) {
            try {
                lastMsgs.put(msg.getSrc(), new PathControlState(msg));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }       
    }        
}
