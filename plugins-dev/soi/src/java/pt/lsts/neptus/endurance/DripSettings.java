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
 * Version 1.1 only (the "Licence", units=""), appearing in the file LICENSE.md
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
 * Author: keila
 * 09/10/2018
 */
package pt.lsts.neptus.endurance;

import java.lang.reflect.Field;

import pt.lsts.neptus.plugins.NeptusProperty;

/**
 * @author keila
 *
 */
public class DripSettings{
    @NeptusProperty(name="River Mouth Latitude",description = "Latitude, in degrees, of river mouth")
    double river_lat = 41.144789;

    @NeptusProperty(name="River Mouth Longitude", description = "Longitude, in degrees, of river mouth")
    double river_lon = -8.679689;

    @NeptusProperty(name="Number of YoYo",description = "Number of yoyos to perform on each side of the plume")
    int yoyo_count = 5;

    @NeptusProperty(name="Start angle",description = "Start angle, in degrees", units="degrees")
    double start_ang = -180;

    @NeptusProperty(name="End angle",description = "End angle, in degrees", units="degrees")
    double end_ang = -45;

    @NeptusProperty(name="Angle increment",description = "Variation, in degrees, between survey angles", units="degrees")
    double angle_inc = 10;

    @NeptusProperty(name="Minimum distance",description = "Minimum distance from river mouth", units="meters")
    double min_dist = 750;

    @NeptusProperty(name="Maximum distance",description = "Maximum distance from river mouth", units="meters")
    double max_dist = 15000;

    @NeptusProperty(name="Simulated",description = "Use Simulated Plume")
    boolean simulated_plume = false;

    @NeptusProperty(name="Plume distance",description = "Distance of simulated plume", units="meters")
    double plume_dist = 1000;
    
    @NeptusProperty(name="Gradient",description = "Plume Gradient")
    double plume_gradient = 5;

    
    public static String abbrev(String name) {
        for (Field f : DripSettings.class.getDeclaredFields()) {
            NeptusProperty np = f.getAnnotation(NeptusProperty.class);
            if (np == null || !np.name().equals(name))
                continue;
            return f.getName();
        }
        return null;
    }
}
