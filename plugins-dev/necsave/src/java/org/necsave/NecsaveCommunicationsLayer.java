/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 12/10/2016
 */
package org.necsave;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import com.google.common.eventbus.Subscribe;

import info.necsave.msgs.Header.MEDIUM;
import info.necsave.msgs.PlatformInfo;
import info.necsave.proto.Message;
import info.necsave.proto.ProtoDefinition;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author zp
 *
 */
@PluginDescription(name="Necsave Communications Layer", icon="org/necsave/necsave.png")
public class NecsaveCommunicationsLayer extends ConsoleLayer {

    @NeptusProperty
    public long displayMilliseconds = 4000;
    
    private StateRenderer2D renderer = null;
    
    HashSet<Communication> communications = new HashSet<>();
    LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    {
        platformNames.put(11, "lauv-noptilus-1");
        platformNames.put(12, "lauv-noptilus-2");
        platformNames.put(13, "lauv-noptilus-3");
        platformNames.put(14, "lauv-xplore-1");
    }
    
    LinkedHashMap<String, Color> msgColors = new LinkedHashMap<>();
    {
        msgColors.put("PlatformInfo", Color.green.darker());
        msgColors.put("Plan", Color.red.darker());
        msgColors.put("AllPlatformInfo", Color.orange.darker());
        msgColors.put("MeshState", Color.magenta.darker());
        msgColors.put("Abort", Color.red);
    }
    
    
    @Override
    public boolean userControlsOpacity() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void initLayer() {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanLayer() {
        // TODO Auto-generated method stub

    }
    
    @Subscribe
    public void on(Message msg) {
        if (msg.getMgid() == PlatformInfo.ID_STATIC) {
            synchronized (platformNames) {
                PlatformInfo pinfo = (PlatformInfo) msg;
                platformNames.put(pinfo.getPlatformId(), pinfo.getPlatformName());    
            }            
        }
        if (msg.getMedium() != MEDIUM.UNKNOWN && msg.getDst() != 0xFFFF)
            communications.add(new Communication(msg));        
    }
    
    private String nameOf(int platformId) {
        synchronized (platformNames) {
            if (platformNames.containsKey(platformId))
                return platformNames.get(platformId);
        }        
        return ""+platformId;
    }
    
    @Periodic(millisBetweenUpdates=50)
    private void purgeComms() {
        HashSet<Communication> copy = new HashSet<>();
        synchronized (communications) {
            communications.forEach(c -> {
                if (System.currentTimeMillis() - c.timestamp < displayMilliseconds)
                    copy.add(c);
            });
            communications = copy;
        }
        if (!communications.isEmpty() && renderer != null)
            renderer.repaint();
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.renderer = renderer;
        super.paint(g, renderer);
        ArrayList<Communication> copy = new ArrayList<>();
        synchronized (communications) {
            copy.addAll(communications);    
        }
        copy.forEach(c -> {
            ImcSystem src = ImcSystemsHolder.getSystemWithName(c.src);
            ImcSystem dst = ImcSystemsHolder.getSystemWithName(c.dst);
            if (src == null || dst == null || src.getLocation() == null || dst.getLocation() == null)
                return;
            
            double ang = src.getLocation().getXYAngle(dst.getLocation());
            
            Point2D pt1 = renderer.getScreenPosition(src.getLocation()), pt2 = renderer.getScreenPosition(dst.getLocation());
            double distance = pt1.distance(pt2);
            AffineTransform transform = g.getTransform();
            g.translate(pt1.getX(), pt1.getY());
            g.rotate(ang-Math.PI/2 - renderer.getRotation());
            if (distance > 10) {
                g.translate(5, 0);
                distance -= 10;
            }
            
            Color color = Color.white;
            
            if (msgColors.containsKey(c.msg))
                color = msgColors.get(c.msg);
            
            double pos = ((System.currentTimeMillis()-c.timestamp)/(double)displayMilliseconds);
            pos = distance * Math.min(1.0, pos);
            
            g.setColor(color);
            
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(0, 0, distance, 0));
            g.fill(new Ellipse2D.Double(pos-4, -4, 8, 8));
            g.setStroke(new BasicStroke(1f));
            g.setTransform(transform);
            
            int width = g.getFontMetrics().stringWidth(c.msg);
            
            g.translate((pt1.getX()+pt2.getX())/2.0, (pt1.getY()+pt2.getY())/2.0);
            if (ang > Math.PI)
                ang -= Math.PI;
            
            g.rotate(ang-Math.PI/2 - renderer.getRotation());
            
            g.setColor(Color.white);            
            g.drawString(c.msg, -width/2, 5);
            
            g.setTransform(transform);
        });
    }
    
    
    
    class Communication {
        public String src, dst, msg;
        public long timestamp;
        
        public Communication(Message msg) {
            src = nameOf(msg.getSrc());
            dst = nameOf(msg.getDst());
            this.msg = msg.getAbbrev();
            if (msg.getAbbrev().equals("CompressedMsg"))
                this.msg = ProtoDefinition.getInstance().getMessageName(msg.getInteger("msg_type"));
            timestamp = msg.getTimestampMillis();
        }
        
        public Communication(String src, String dst, String msg) {
            this.timestamp = System.currentTimeMillis();
            this.src = src;
            this.dst = dst;
            this.msg = msg;
        }
        
        @Override
        public int hashCode() {
            return (src+" "+dst+" "+msg).hashCode();
        }
        
        @Override
        public String toString() {
            return src +" -> " + dst + " ("+msg+")";
        }
    }
}
