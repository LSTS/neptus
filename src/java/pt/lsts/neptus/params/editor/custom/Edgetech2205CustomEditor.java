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
 * May 21, 2013
 */
package pt.lsts.neptus.params.editor.custom;

import java.beans.PropertyChangeEvent;
import java.util.Map;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.lsts.neptus.gui.editor.ValidationEnableInterface;
import pt.lsts.neptus.params.SystemProperty;

/**
 * @author pdias
 *
 */
public class Edgetech2205CustomEditor extends CustomSystemPropertyEditor {

    /*
        The sidescan section will have an additional attribute called 'editor' telling Neptus
        to use a custom parameter editor (see attached file). This custom editor should behave
        like the generic editor with the following additional constraints:

        IF 'High-Frequency Channels' != 'None' AND 'Low-Frequency Channels' != 'None'
        ENABLE 'Trigger Divisor'
        DISABLE 'Low-Frequency Range'
        DISABLE VALIDATION OF 'Low-Frequency Range'
        COMPUTE 'Low-Frequency Range' AS ('High-Frequency Range' * 'Range Multiplier')
        ELSE
        DISABLE 'Range Multiplier'
        ENABLE 'Low-Frequency Range'
        ENABLE VALIDATION OF 'Low-Frequency Range'
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
//        System.out.println("#############################################################");
        if(evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            String sectionId = sp.getCategoryId();

            if (!paramList.values().iterator().next().getCategoryId().equalsIgnoreCase(sectionId))
                return;

            if (!"None".equalsIgnoreCase(paramList.get("High-Frequency Channels").getValue().toString()) &&
                    !"None".equalsIgnoreCase(paramList.get("Low-Frequency Channels").getValue().toString())) {
                paramList.get("Range Multiplier").setEditable(true);
                paramList.get("Low-Frequency Range").setEditable(false);
                AbstractPropertyEditor editor = paramList.get("Low-Frequency Range").getEditor();
                if (editor instanceof ValidationEnableInterface)
                    ((ValidationEnableInterface) editor).setEnableValidation(false);
                paramList.get("Low-Frequency Range").setValue(Double.valueOf(
                        ((Number) paramList.get("High-Frequency Range").getValue()).doubleValue()
                                * ((Number) paramList.get("Range Multiplier").getValue()).doubleValue()).intValue());
            }
            else {
                paramList.get("Range Multiplier").setEditable(false);
                paramList.get("Low-Frequency Range").setEditable(true);
                AbstractPropertyEditor editor = paramList.get("Low-Frequency Range").getEditor();
                if (editor instanceof ValidationEnableInterface)
                    ((ValidationEnableInterface) editor).setEnableValidation(true);
            }
        }
    }
}
