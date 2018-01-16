/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * 27/04/2017
 */
package pt.lsts.neptus.params.editor.custom;

import java.beans.PropertyChangeEvent;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.params.SystemProperty;

/**
 * Minimum value of lower gate depends on the upper gate value (min = [Value of upper gate] m).
 * 
 * @author pdias
 *
 */
public class NorbitWBMSCustomEditor extends CustomSystemPropertyEditor {

    private static final String UPPER_GATE = "Upper Gate";
    private static final String LOWER_GATE = "Lower Gate";
    
    /**
     * @param paramList
     */
    public NorbitWBMSCustomEditor(Map<String, SystemProperty> paramList) {
        super(paramList);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.params.editor.custom.CustomSystemPropertyEditor#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            String sectionId = sp.getCategoryId();

            if (!paramList.values().iterator().next().getCategoryId().equalsIgnoreCase(sectionId))
                return;

            SystemProperty upperGateProp = paramList.get(UPPER_GATE);
            if (upperGateProp == null)
                upperGateProp = paramList.get(UPPER_GATE.replace(" ", ""));
            if (upperGateProp == null)
                upperGateProp = paramList.get(UPPER_GATE.toUpperCase());
            if (upperGateProp == null) {
                NeptusLog.pub().error(String.format("Parameter %s was not found! Skipping.", UPPER_GATE));
                return;
            }

            SystemProperty lowerGateProp = paramList.get(LOWER_GATE);
            if (lowerGateProp == null)
                lowerGateProp = paramList.get(LOWER_GATE.replace(" ", ""));
            if (lowerGateProp == null)
                lowerGateProp = paramList.get(LOWER_GATE.toUpperCase());
            if (lowerGateProp == null) {
                NeptusLog.pub().error(String.format("Parameter %s was not found! Skipping.", LOWER_GATE));
                return;
            }

            try {
                long upperGateVal = (long) upperGateProp.getValue();
                long lowerGateVal = (long) lowerGateProp.getValue();
                
                if (lowerGateVal < upperGateVal)
                    lowerGateProp.setValue(upperGateVal);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }
}
