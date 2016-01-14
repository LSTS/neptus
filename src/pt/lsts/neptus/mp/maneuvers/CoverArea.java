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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.JPopupMenu;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PolygonVertex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanElement;

/**
 * @author zp
 * 
 */
public class CoverArea extends AbstractImcManeuver<pt.lsts.imc.CoverArea> implements StateRendererInteraction, IMCSerialization {

    protected InteractionAdapter adapter = new InteractionAdapter(null);

    @NeptusProperty(name = "polygon", editable = false)
    public String polygonPoints = "";

    protected Vector<LocationType> points = new Vector<LocationType>();

    public CoverArea() {
        super(new pt.lsts.imc.CoverArea());
    }

    @Override
    public void parseIMCMessage(IMCMessage msg) {
        super.parseIMCMessage(msg);
        
        try {
            points.clear();
            Vector<PolygonVertex> vertices = message.getMessageList("polygon", PolygonVertex.class);
            for (PolygonVertex v : vertices)
                points.add(new LocationType(Math.toDegrees(v.getLat()), Math.toDegrees(v.getLon())));            
        }        
        catch (Exception e) {
            NeptusLog.pub().error("Error parsing message of type "+message.getAbbrev(), e);
            return;
        }
    }

    @Override
    public IMCMessage serializeToIMC() {
        
        Vector<PolygonVertex> vertices = new Vector<PolygonVertex>();
        
        for (LocationType pt : points )
            vertices.add(PolygonVertex.create("lat", pt.getLatitudeRads(), "lon", pt.getLongitudeRads()));
        
        message.setMessageList(vertices, "polygon");
        
        return super.serializeToIMC();
    }

    @Override
    public String getName() {
        return "CoverArea maneuver";
    }

    @Override
    public Image getIconImage() {
        return adapter.getIconImage();
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    @Override
    public boolean isExclusive() {
        return false;
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        super.paintOnMap(g2d, planElement, renderer);
        
        LocationType man_loc = this.getStartLocation();
        Graphics2D g = (Graphics2D)g2d.create();
        g.translate(-renderer.getScreenPosition(man_loc).getX(), -renderer.getScreenPosition(man_loc).getY());

        List<Integer> x = new ArrayList<Integer>();
        List<Integer> y = new ArrayList<Integer>();
        
        x.add((int)renderer.getScreenPosition(man_loc).getX());
        y.add((int)renderer.getScreenPosition(man_loc).getY());
        
        for (LocationType loc : points) {
            Point2D pt = renderer.getScreenPosition(loc);
            Ellipse2D corners = new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10);
            x.add((int)pt.getX());
            y.add((int)pt.getY());
            g.setColor(Color.black);
            g.fill(corners);
            
        }
        
        int[] xx = new int[x.size()];
        int[] yy = new int[x.size()];
        
        for(int i = 0;i < xx.length;i++){
            xx[i] = x.get(i);
            yy[i] = y.get(i);
        }
        
        g.drawPolygon(xx, yy, x.size());
        g.dispose();
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        final StateRenderer2D r2d = source;
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            popup.add("Clear polygon").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    points.clear();
                    r2d.repaint();
                }
            });

            popup.add("Finish editing").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    //TODO
                }
            });

            popup.show(source, event.getX(), event.getY());
        }
        else {
            if (event.getClickCount() == 1) {
                Point2D clicked = event.getPoint();
                LocationType curLoc = source.getRealWorldLocation(clicked);
                points.add(curLoc);
                source.repaint();
            }
        }    
        adapter.mouseClicked(event, source);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        adapter.mousePressed(event, source);
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        adapter.mouseDragged(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        adapter.setAssociatedSwitch(tswitch);
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);

        NeptusLog.pub().info("<###>setActive: "+mode);
    }
    
    
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);
        
    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }
    
    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        return Collections.singleton(getStartLocation());
    }

    public static void main(String[] args) {
        CoverArea area = new CoverArea();

        NeptusLog.pub().info("<###> "+area.asXML());
        
        CoverArea compc = new CoverArea();
        String ccmanXML = compc.getManeuverAsDocument("CoverArea").asXML();
        System.out.println(ccmanXML);
        CoverArea compc1 = new CoverArea();
        compc1.loadFromXML(ccmanXML);
        ccmanXML = compc.getManeuverAsDocument("CoverArea").asXML();
        System.out.println(ccmanXML);
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
    }
}
