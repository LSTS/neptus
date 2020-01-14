/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 04/10/2017
 */
package pt.lsts.neptus.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
public class ZUnitsEditor extends AbstractPropertyEditor {

    protected JComboBox<ManeuverLocation.Z_UNITS> unitsCombo = new JComboBox<>();
    protected ManeuverLocation.Z_UNITS zUnits = Z_UNITS.NONE;
    protected ArrayList<ManeuverLocation.Z_UNITS> validZUnits = new ArrayList<>();
    
    public ZUnitsEditor() {
        this((ManeuverLocation.Z_UNITS[]) null);
    }

    public ZUnitsEditor(ManeuverLocation.Z_UNITS... validUnits) {
        this(true, validUnits);
    }

    private ZUnitsEditor(boolean useNone, ManeuverLocation.Z_UNITS... validUnits) {
        if (validUnits != null && validUnits.length != 0 && validUnits[0] != null) {
            for (Z_UNITS u : validUnits)
                validZUnits.add(u);
            
            if (useNone && !validZUnits.contains(ManeuverLocation.Z_UNITS.NONE))
                validZUnits.add(0, ManeuverLocation.Z_UNITS.NONE);
            if (!useNone && validZUnits.contains(ManeuverLocation.Z_UNITS.NONE))
                validZUnits.remove(ManeuverLocation.Z_UNITS.NONE);
        }
        else {
            if (GeneralPreferences.validZUnits != null && GeneralPreferences.validZUnits.length != 0
                    && GeneralPreferences.validZUnits[0] != null) {
                for (Z_UNITS u : GeneralPreferences.validZUnits)
                    validZUnits.add(u);
                if (useNone && !validZUnits.contains(ManeuverLocation.Z_UNITS.NONE))
                    validZUnits.add(0, ManeuverLocation.Z_UNITS.NONE);
                if (!useNone && validZUnits.contains(ManeuverLocation.Z_UNITS.NONE))
                    validZUnits.remove(ManeuverLocation.Z_UNITS.NONE);
            }
            else {
                for (Z_UNITS u : ManeuverLocation.Z_UNITS.values())
                    validZUnits.add(u);
                if (!useNone && validZUnits.contains(ManeuverLocation.Z_UNITS.NONE))
                    validZUnits.remove(ManeuverLocation.Z_UNITS.NONE);
            }
        }
        
        for (ManeuverLocation.Z_UNITS u : ManeuverLocation.Z_UNITS.values()) {
            if (validZUnits.contains(u))
                unitsCombo.addItem(u);
        }
        editor = unitsCombo;
        unitsCombo.setSelectedItem(zUnits);
        
        unitsCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ManeuverLocation.Z_UNITS oldValue = zUnits;
                ManeuverLocation.Z_UNITS u = (ManeuverLocation.Z_UNITS) unitsCombo.getSelectedItem();
                zUnits = u;
                firePropertyChange(oldValue, zUnits);
            }
        });
    }
    
    public Object getValue() {
        return zUnits;
    }

    public void setValue(Object value) {
        if (value instanceof ManeuverLocation.Z_UNITS) {
            ManeuverLocation.Z_UNITS uTmp = (ManeuverLocation.Z_UNITS) value;
            if (validZUnits.contains(uTmp))
                this.zUnits = uTmp;
            else
                this.zUnits = validZUnits.get(0);
        }
    }
}
