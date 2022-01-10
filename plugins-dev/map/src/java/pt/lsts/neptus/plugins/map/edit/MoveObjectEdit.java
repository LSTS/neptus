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
package pt.lsts.neptus.plugins.map.edit;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;

/**
 * @author zp
 *
 */
public class MoveObjectEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 1L;
    protected AbstractElement element;
    protected LocationType newLocation;
    protected LocationType oldLocation;
    
    public MoveObjectEdit(AbstractElement element, LocationType oldLocation) {
        this.element = element;
        this.oldLocation = new LocationType(oldLocation);
        if (element instanceof LocatedManeuver)
            this.newLocation = new LocationType(((LocatedManeuver)element).getManeuverLocation());
        else
            this.newLocation = new LocationType(oldLocation);
    }
    
    
    
    @Override
    public void undo() throws CannotUndoException {
        
        if (element instanceof LocatedManeuver)
            element.setCenterLocation(oldLocation);
        
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(element.getParentMap());
        mce.setChangedObject(element);
        mce.setMapGroup(element.getParentMap().getMapGroup());
        element.getParentMap().warnChangeListeners(mce);
    }
    
    @Override
    public void redo() throws CannotRedoException {
        
        if (element instanceof LocatedManeuver)
            element.setCenterLocation(newLocation);
        
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(element.getParentMap());
        mce.setChangedObject(element);
        mce.setMapGroup(element.getParentMap().getMapGroup());
        element.getParentMap().warnChangeListeners(mce);
    }
    
    @Override
    public boolean canRedo() {
        return true;
    }
    
    @Override
    public boolean canUndo() {
        return true;
    }
    
    @Override
    public String getPresentationName() {
        return I18n.textf("Move '%element'",element.getId());
    }
    
}
