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
 * Author: manuel
 * Nov 14, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.Serializable;
import java.text.DecimalFormat;

import com.MAVLink.common.msg_param_value;

public class Parameter implements Comparable<Parameter>, Serializable {

    private static final long serialVersionUID = 1L;
    public String name;
    public double value;
    public int type;

    private final static DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
    static {
        format.applyPattern("0.###");
    }

    public Parameter(String name, double value, int type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public Parameter(msg_param_value m_value) {
        this(m_value.getParam_Id(), m_value.param_value, m_value.param_type);
    }

    public Parameter(String name, Double value) {
        this(name, value, 0); // TODO Setting type to Zero may cause an error
    }

    public Parameter(String name) {
        this(name, 0, 0); // TODO Setting type to Zero may cause an error
    }

    public String getValue() {
        return format.format(value);
    }

    public static void checkParameterName(String name) throws Exception {
        if (name.equals("SYSID_SW_MREV")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("WP_TOTAL")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("CMD_TOTAL")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("FENCE_TOTAL")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("SYS_NUM_RESETS")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("ARSPD_OFFSET")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("GND_ABS_PRESS")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("GcND_TEMP")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("CMD_INDEX")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("LOG_LASTFILE")) {
            throw new Exception("ExcludedName");
        } else if (name.contains("FORMAT_VERSION")) {
            throw new Exception("ExcludedName");
        } else {
        }
    }

    public static DecimalFormat getFormat() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parameter)) return false;

        Parameter parameter = (Parameter) o;

        return (name == null ? parameter.name == null : name.equals(parameter.name));
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public int compareTo(Parameter another) {
        return name.compareTo(another.name);
    }
}