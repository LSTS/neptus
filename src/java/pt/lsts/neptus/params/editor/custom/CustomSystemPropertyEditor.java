/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;

import pt.lsts.neptus.params.SystemProperty;

/**
 * @author pdias
 *
 */
public class CustomSystemPropertyEditor implements PropertyChangeListener, Cloneable {

    protected Map<String, SystemProperty> paramList = null;
    
    public CustomSystemPropertyEditor(Map<String, SystemProperty> paramList) {
        this.paramList = paramList;
    }
    
    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent evt) {
    }

    public Map<String, SystemProperty> getSystemPropertiesList() {
        return this.paramList;
    }

    public void setSystemPropertiesList(Map<String, SystemProperty> paramList) {
        this.paramList = paramList;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public CustomSystemPropertyEditor clone() throws CloneNotSupportedException {
        CustomSystemPropertyEditor clone = (CustomSystemPropertyEditor) super.clone();
        Map<String, SystemProperty> pl = clone.getSystemPropertiesList();
        LinkedHashMap<String, SystemProperty> cl = new LinkedHashMap<>();
        for (String key : pl.keySet()) {
            if (key == null)
                continue;
            cl.put(key, pl.get(key));
        }
        clone.setSystemPropertiesList(cl);
        return clone;
    }
}
