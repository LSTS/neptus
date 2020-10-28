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
 * Author: zp
 * Mar 31, 2015
 */
package com.inovaworks;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;


/**
 * @author zp
 *
 */
public class ObservedProperty {

    public String property;
    public String propertyName;
    public Result result = new Result();

    static class Result {
        public String value = "n/a";
        public String uom = "n/a";
    }

    public ObservedProperty(String property, Object value, String units) {
        this.propertyName = property;
        this.property = StringUtils.deleteWhitespace(property);
        
        if (value != null)
            this.result.value = ""+value;
        if (units != null)
            this.result.uom = units;
    }
    
    public static ObservedProperty position(double latDegs, double lonDegs, double height) {
        return new ObservedProperty("position", String.format(Locale.US, "%.8f,%.8f,%.3f", latDegs, lonDegs, height), "position");
    }
    
    public static ObservedProperty speed(double speedMps) {
        return new ObservedProperty("speed", String.format(Locale.US, "%.2f", speedMps * 3.6), "km/h");
    }
    
    public static ObservedProperty verticalSpeed(double speedMps) {
        return new ObservedProperty("verticalSpeed", String.format(Locale.US, "%.2f", speedMps * 3.6), "km/h");
    }
    
    public static ObservedProperty heading(double headingDegs) {
        return new ObservedProperty("heading", String.format(Locale.US, "%.1f", headingDegs), "degrees");
    }
}
