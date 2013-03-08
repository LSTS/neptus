/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: CoverArea.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PolygonVertex;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.PlanElement;

/**
 * @author zp
 * 
 */
public class CoverArea extends AbstractImcManeuver<pt.up.fe.dceg.neptus.imc.CoverArea> implements StateRendererInteraction, IMCSerialization {

    protected InteractionAdapter adapter = new InteractionAdapter(null);

    @NeptusProperty(name="polygon", hidden=true)
    public String polygonPoints = "";

    protected Vector<LocationType> points = new Vector<LocationType>();

    public CoverArea() {
        super(new pt.up.fe.dceg.neptus.imc.CoverArea());
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
            vertices.add(new PolygonVertex("lat", pt.getLatitudeAsDoubleValueRads(), "lon", pt.getLongitudeAsDoubleValueRads()));
        
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
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);

        System.out.println("setActive: "+mode);
    }

    public static void main(String[] args) {
        CoverArea area = new CoverArea();

        System.out.println(area.asXML());
    }
}
