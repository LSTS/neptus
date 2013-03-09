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
 */
package pt.up.fe.dceg.neptus.plugins.map.interactions;

import java.awt.event.MouseEvent;

import javax.swing.undo.UndoManager;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.map.edit.AddObjectEdit;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.PathElement;

/**
 * @author zp
 *
 */
public class DrawPathInteraction extends InteractionAdapter {

    private static final long serialVersionUID = 1L;
    protected MapType pivot;
    protected PathElement curDrawing = null;
    protected UndoManager undoManager = null;
    
    public DrawPathInteraction(MapType pivot, UndoManager undoManager, ConsoleLayout console) {
        super(console);
        this.pivot = pivot;
        this.undoManager = undoManager;
    }    
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        
        if (event.getButton() != MouseEvent.BUTTON1) {
            super.mousePressed(event, source);
            return;
        }
        
        LocationType lt = source.getRealWorldLocation(event.getPoint());

        curDrawing = new PathElement(pivot.getMapGroup(), pivot, lt);
        curDrawing.addPoint(0, 0, 0, false);
        pivot.addObject(curDrawing);
        source.setFastRendering(true);
        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
        changeEvent.setChangedObject(curDrawing);
        changeEvent.setSourceMap(pivot);
        pivot.warnChangeListeners(changeEvent);
        
    }
    
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() != MouseEvent.BUTTON1) {
            super.mouseReleased(event, source);
            return;
        }

        curDrawing.setFinished(true);
        source.setFastRendering(false);
        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        changeEvent.setChangedObject(curDrawing);
        changeEvent.setSourceMap(pivot);
        AddObjectEdit edit = new AddObjectEdit(curDrawing);
        
        //System.out.println("Adding edit "+edit.getPresentationName());
        undoManager.addEdit(edit);

        pivot.warnChangeListeners(changeEvent);

        curDrawing = null;
        
        if (associatedSwitch != null)
            associatedSwitch.doClick();
    }
    
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        
        if (curDrawing == null) {
            super.mouseDragged(event, source);
            return;
        }
        
        double[] offsets = curDrawing.getCenterLocation().getOffsetFrom(
                source.getRealWorldLocation(event.getPoint()));
        curDrawing.addPoint(-offsets[1], -offsets[0], 0, false);

        MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        changeEvent.setChangedObject(curDrawing);
        changeEvent.setSourceMap(pivot);

        pivot.warnChangeListeners(changeEvent);
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        if (!mode && curDrawing != null) {
            curDrawing.setFinished(true);
            source.setFastRendering(false);
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(curDrawing);
            changeEvent.setSourceMap(pivot);
            AddObjectEdit edit = new AddObjectEdit(curDrawing);
            undoManager.addEdit(edit);

            pivot.warnChangeListeners(changeEvent);
        }        
    }
    
    @Override
    public boolean isExclusive() {
        return true;
    }
}
