/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: dronekit.io
 * Nov 22, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParameterMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int RANGE_LOW = 0;
    public static final int RANGE_HIGH = 1;

    private String name;
    private String displayName;
    private String description;

    private String units;
    private String range;
    private String values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getValues() {
        return values;
    }

    public void setValues(String values) {
        this.values = values;
    }

    public boolean hasInfo() {
        return (description != null && !description.isEmpty())
                || (values != null && !values.isEmpty());
    }

    public double[] parseRange() throws ParseException {
        final DecimalFormat format = Parameter.getFormat();

        final String[] parts = this.range.split(" ");
        if (parts.length != 2) {
            throw new IllegalArgumentException();
        }

        final double[] outRange = new double[2];
        outRange[RANGE_LOW] = format.parse(parts[RANGE_LOW]).doubleValue();
        outRange[RANGE_HIGH] = format.parse(parts[RANGE_HIGH]).doubleValue();

        return outRange;
    }

    public Map<Double, String> parseValues() throws ParseException {
        final DecimalFormat format = Parameter.getFormat();

        final Map<Double, String> outValues = new LinkedHashMap<Double, String>();
        if (values != null) {
            final String[] tparts = this.values.split(",");
            for (String tpart : tparts) {
                final String[] parts = tpart.split(":");
                if (parts.length != 2)
                    throw new IllegalArgumentException();
                outValues.put(format.parse(parts[0].trim()).doubleValue(), parts[1].trim());
            }
        }
        return outValues;
    }
}
