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
 * Nov 28, 2011
 * $Id:: Box2DInteraction.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.plugins.map.interactions;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.undo.UndoManager;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.map.edit.AddObjectEdit;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.ParallelepipedElement;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 */
@SuppressWarnings("serial")
@LayerPriority(priority = 60)
public class Box2DInteraction extends InteractionAdapter implements Renderer2DPainter {

    protected LocationType point1 = null, point2 = null;
    protected MapType pivot = null;
    protected UndoManager manager = null;
    
    public Box2DInteraction(MapType pivot, UndoManager manager, ConsoleLayout console) {
        super(console);
        this.pivot = pivot;
        this.manager = manager;
    }
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        if (point1 == null)
            point1 = source.getRealWorldLocation(event.getPoint());        
    }
    
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        super.mouseReleased(event, source);
        
        point2 = source.getRealWorldLocation(event.getPoint());
        
        point1.convertToAbsoluteLatLonDepth();
        point2.convertToAbsoluteLatLonDepth();
        
        double lat = (point1.getLatitudeAsDoubleValue() + point2.getLatitudeAsDoubleValue()) / 2;
        double lon = (point1.getLongitudeAsDoubleValue() + point2.getLongitudeAsDoubleValue()) / 2;
        
        double offsets[] = point1.getOffsetFrom(point2);
        double width = Math.abs(offsets[1]);
        double length = Math.abs(offsets[0]);
        
        ParallelepipedElement element = new ParallelepipedElement(pivot.getMapGroup(), pivot);
        element.setWidth(width);
        element.setHeight(0);
        element.setLength(length);
        element.setColor(Color.red);
        element.setCenterLocation(new LocationType(lat, lon));
        pivot.addObject(element);
        
        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        changeEvent.setChangedObject(element);
        changeEvent.setSourceMap(pivot);
        AddObjectEdit edit = new AddObjectEdit(element);
        
        manager.addEdit(edit);
        pivot.warnChangeListeners(changeEvent);
        
        if (getAssociatedSwitch() != null)
            getAssociatedSwitch().doClick();                
        
        point1 = point2 = null;
    }
    
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (point1 != null) {
            point2 = source.getRealWorldLocation(event.getPoint());
            source.repaint();
        }
    }    
    
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        if (point1 != null && point2 != null) {
            
            double offsets[] = point1.getOffsetFrom(point2);
            
            double w = Math.abs(offsets[1]);
            double h = Math.abs(offsets[0]);
            g.setColor(Color.red.darker());
            g.setFont(new Font("Helvetica", Font.BOLD, 14));
            g.drawString(I18n.textf("Width: %width m",GuiUtils.getNeptusDecimalFormat(1).format(w)), 55, 20);
            g.drawString(I18n.textf("Height: %height m",GuiUtils.getNeptusDecimalFormat(1).format(h)), 55, 35);
            
            Point2D pt1 = renderer.getScreenPosition(point1);
            Point2D pt2 = renderer.getScreenPosition(point2);
            
            double top = Math.min(pt1.getY(), pt2.getY());
            double left = Math.min(pt1.getX(), pt2.getX());
            
            double width = Math.abs(pt1.getX()-pt2.getX());
            double length = Math.abs(pt1.getY()-pt2.getY());
            
            Rectangle2D.Double rect = new Rectangle2D.Double(left, top, width, length);
            g.setColor(Color.red);
            g.draw(rect);
            g.setColor(new Color(255,0,0,200));
            g.fill(rect);
        }
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        if (mode)
            source.addPostRenderPainter(this, I18n.text("Box 2d painter"));
        else
            source.removePostRenderPainter(this);
    }
}