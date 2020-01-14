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
 * Author: dronekit.io
 * Nov 22, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.Serializable;
import java.util.HashMap;

public class ParameterMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int RANGE_LOW = 0;
    public static final int RANGE_HIGH = 1;

    private String name;
    private String humanName;
    private String documentation;

    private String range;
    private String increment;
    private String units;
    private String bitmask;
    private HashMap<String, String> values = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return humanName;
    }

    public void setDisplayName(String displayName) {
        this.humanName = displayName;
    }

    public String getDescription() {
        return documentation;
    }

    public void setDescription(String description) {
        this.documentation = description;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public HashMap<String, String> getValues() {
        return values;
    }

    public void setValues(HashMap<String, String> values) {
        this.values = values;
    }

    public boolean hasInfo() {
        return (documentation != null && !documentation.isEmpty())
                || (values != null && !values.isEmpty());
    }

    /**
     * @return the increment
     */
    public String getIncrement() {
        return increment;
    }

    /**
     * @param increment the increment to set
     */
    public void setIncrement(String increment) {
        this.increment = increment;
    }

    /**
     * @param attribute
     * @param textContent
     */
    public void parseValues(String codeAttribute, String value) {
        values.put(codeAttribute.trim(), value.trim());
    }

    /**
     * @return bitmask
     */
    public String getBitmask() {
        return bitmask;
    }
    
    /**
     * @param bitmask
     */
    public void setBitmask(String bitmask) {
        this.bitmask = bitmask;
    }

    class Item {
        private String value;
        private String text;

        public Item(String value, String text) {
            this.setValue(value);
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }

        /**
         * @param value the value to set
         */
        public void setValue(String value) {
            this.value = value;
        }

    }
}
