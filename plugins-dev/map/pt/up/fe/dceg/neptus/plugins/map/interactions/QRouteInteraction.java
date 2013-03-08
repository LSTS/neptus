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
 * Nov 15, 2011
 * $Id:: QRouteInteraction.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.plugins.map.interactions;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.undo.UndoManager;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.map.edit.AddObjectEdit;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.QRouteElement;

/**
 * @author zp
 *
 */
public class QRouteInteraction extends InteractionAdapter {
    private static final long serialVersionUID = 1L;
    protected MapType pivot;
    protected QRouteElement element = null;
    protected UndoManager undoManager = null;
    
    public QRouteInteraction(MapType pivot, UndoManager undoManager, ConsoleLayout console) {
        super(console);
        this.pivot = pivot;
        this.undoManager = undoManager;
    }    
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        
        if (event.getButton() != MouseEvent.BUTTON1) {
            
            if (element != null) {
                MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                changeEvent.setChangedObject(element);
                changeEvent.setSourceMap(pivot);
                AddObjectEdit edit = new AddObjectEdit(element);
                element.finish();
                undoManager.addEdit(edit);
                pivot.warnChangeListeners(changeEvent);
                element = null;
                
                if (associatedSwitch != null)
                    associatedSwitch.doClick();
            }
            
            super.mousePressed(event, source);
            return;
        }
        
        
        LocationType lt = source.getRealWorldLocation(event.getPoint());

        if (element == null) {
            element = new QRouteElement(pivot.getMapGroup(), pivot);
            element.setCenterLocation(lt);
            element.otherPoints.add(new Point2D.Double(lt.getLatitudeAsDoubleValue(), lt.getLongitudeAsDoubleValue()));
            pivot.addObject(element);
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            pivot.warnChangeListeners(changeEvent);
        }
        else {
            element.otherPoints.add(new Point2D.Double(lt.getLatitudeAsDoubleValue(), lt.getLongitudeAsDoubleValue()));
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            pivot.warnChangeListeners(changeEvent);
        }
        
    }
    
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        
        if (element == null) {
            super.mouseMoved(event, source);
            return;
        }
        
        LocationType lt = source.getRealWorldLocation(event.getPoint());
        element.otherPoints.lastElement().setLocation(lt.getLatitudeAsDoubleValue(), lt.getLongitudeAsDoubleValue());        
        
        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        changeEvent.setChangedObject(element);
        changeEvent.setSourceMap(pivot);
        pivot.warnChangeListeners(changeEvent);
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        if (!mode && element != null) {
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            AddObjectEdit edit = new AddObjectEdit(element);
            undoManager.addEdit(edit);
            element.finish();
            pivot.warnChangeListeners(changeEvent);
        }        
    }
    
    @Override
    public boolean isExclusive() {
        return true;
    }
}
