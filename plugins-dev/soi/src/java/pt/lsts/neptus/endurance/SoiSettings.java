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
 * Author: zepinto
 * 24/01/2018
 */
package pt.lsts.neptus.endurance;

import java.lang.reflect.Field;

import pt.lsts.neptus.plugins.NeptusProperty;

public class SoiSettings{
    @NeptusProperty(name = "Nominal Speed", description="Speed to used when planning (nominal)", units="m/s")
    public double speed = 1;

    @NeptusProperty(name = "Maximum Depth", description="Maximum depth for yoyo behavior", units="m")
    public double maxDepth = 10;

    @NeptusProperty(name = "Minimum Depth", description="Minimum depth for yoyo behavior. 0 will require vehicle to acquire GPS.", units="m")
    public double minDepth = 0.0;

    @NeptusProperty(name = "Maximum Speed", description="Maximum speed allowed for the vehicle", units="m/s")
    public double maxSpeed = 1.5;

    @NeptusProperty(name = "Minimum Speed", description="Minimum speed allowed for the vehicle", units="m/s")
    public double minSpeed = 0.7;

    @NeptusProperty(name = "Waypoint Wait Time", description = "Seconds to idle at each vertex", units="s")
    public int wptSecs = 60;

    @NeptusProperty(name = "Deadline", description = "Minutes before termination", units="minutes")
    int timeout = 600;

    @NeptusProperty(name = "Connection timeout", description = "Maximum time without reporting position", units="minutes")
    int minsOff = 15;
    
    @NeptusProperty(name = "GPS timeout", description = "Maximum time without GPS", units="minutes")
    int minsUnder = 3;  

    @NeptusProperty(name = "Cyclic execution", description = "If selected, after plan completion the same plan is executed again (repeatedly)")
    boolean cycle = false;
    
    @NeptusProperty(name = "Diving speed", description = "Speed to use when diving", units="RPM")
    int descRpm = 1300;
    
    @NeptusProperty(name = "Upload Temperature", description = "Upload temperature profiles when idle")
    boolean upTemp = false;
    
    @NeptusProperty(name = "Align before diving", description = "Align with destination waypoint before going underwater")
    boolean align = true;
    
    public static String abbrev(String name) {
        for (Field f : SoiSettings.class.getDeclaredFields()) {
            NeptusProperty np = f.getAnnotation(NeptusProperty.class);
            if (np == null || !np.name().equals(name))
                continue;
            return f.getName();
        }
        return null;
    }
}