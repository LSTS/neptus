/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * May 21, 2013
 */
package pt.up.fe.dceg.neptus.plugins.params.editor.custom;

import java.beans.PropertyChangeEvent;
import java.util.Map;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.up.fe.dceg.neptus.gui.editor.ValidationEnableInterface;
import pt.up.fe.dceg.neptus.plugins.params.SystemProperty;

/**
 * @author pdias
 *
 */
public class Edgetech2205CustomEditor extends CustomEditor {

    /*
        The sidescan section will have an additional attribute called 'editor' telling Neptus 
        to use a custom parameter editor (see attached file). This custom editor should behave 
        like the generic editor with the following additional constraints:
        
        IF 'High-Frequency Channels' != 'None' AND 'Low-Frequency Channels' != 'None'
        ENABLE 'Trigger Divisor'
        DISABLE 'High-Frequency Range'
        DISABLE VALIDATION OF 'High-Frequency Range'
        COMPUTE 'High-Frequency Range' AS ('Low-Frequency Range' / 'Trigger Divisor')
        ELSE
        DISABLE 'Trigger Divisor'
        ENABLE 'High-Frequency Range'
        ENABLE VALIDATION OF 'High-Frequency Range'     
     */

    /**
     * @param paramList
     */
    public Edgetech2205CustomEditor(Map<String, SystemProperty> paramList) {
        super(paramList);
    }

    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        System.out.println("#############################################################");
        if(evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            String sectionId = sp.getCategoryId();
            
            if (!paramList.values().iterator().next().getCategoryId().equalsIgnoreCase(sectionId))
                return;
            
            if (!"None".equalsIgnoreCase(paramList.get("High-Frequency Channels").getValue().toString()) &&
                    !"None".equalsIgnoreCase(paramList.get("Low-Frequency Channels").getValue().toString())) {
                paramList.get("Trigger Divisor").setEditable(true);
                paramList.get("High-Frequency Range").setEditable(false);
                AbstractPropertyEditor editor = paramList.get("High-Frequency Range").getEditor();
                if (editor instanceof ValidationEnableInterface)
                    ((ValidationEnableInterface) editor).setEnableValidation(false);
                paramList.get("High-Frequency Range").setValue(
                        ((Number) paramList.get("Low-Frequency Range").getValue()).doubleValue()
                                / ((Number) paramList.get("Trigger Divisor").getValue()).doubleValue());
            }
            else {
                paramList.get("Trigger Divisor").setEditable(false);
                paramList.get("High-Frequency Range").setEditable(true);
                AbstractPropertyEditor editor = paramList.get("High-Frequency Range").getEditor();
                if (editor instanceof ValidationEnableInterface)
                    ((ValidationEnableInterface) editor).setEnableValidation(true);
            }
        }
    }
}
