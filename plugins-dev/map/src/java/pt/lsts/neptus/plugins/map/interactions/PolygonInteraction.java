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
 * Nov 15, 2011
 */
package pt.lsts.neptus.plugins.map.interactions;

import java.awt.event.MouseEvent;

import javax.swing.undo.UndoManager;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.plugins.map.edit.AddObjectEdit;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PathElement;

/**
 * @author zp
 *
 */
public class PolygonInteraction extends InteractionAdapter {

    private static final long serialVersionUID = 1L;
    protected MapType pivot;
    protected PathElement element = null;
    protected UndoManager undoManager = null;
    protected boolean fill = false;
    
    public PolygonInteraction(MapType pivot, UndoManager undoManager, boolean fill, ConsoleLayout console) {
        super(console);
        this.pivot = pivot;
        this.fill = fill;
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
                element.setFinished(true);
                
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
            element = new PathElement(pivot.getMapGroup(), pivot, lt);
            element.setFilled(fill);
            element.setShape(fill);
            pivot.addObject(element);
            element.addPoint(0,0,0, false);
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_ADDED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            pivot.warnChangeListeners(changeEvent);
        }
        else {
            double[] offsets = element.getCenterLocation().getOffsetFrom(
                    source.getRealWorldLocation(event.getPoint()));
            element.addPoint(-offsets[1], -offsets[0], 0, false);
           
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            pivot.warnChangeListeners(changeEvent);
        }
        
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        if (!mode && element != null) {
            MapChangeEvent changeEvent = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            changeEvent.setChangedObject(element);
            changeEvent.setSourceMap(pivot);
            AddObjectEdit edit = new AddObjectEdit(element);
            undoManager.addEdit(edit);
            element.setFinished(true);
            pivot.warnChangeListeners(changeEvent);
        }        
    }
    
    @Override
    public boolean isExclusive() {
        return true;
    }
}
