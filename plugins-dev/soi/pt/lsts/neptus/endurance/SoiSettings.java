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
 * Author: zepinto
 * 24/01/2018
 */
package pt.lsts.neptus.endurance;

import pt.lsts.neptus.plugins.NeptusProperty;

public class SoiSettings {
    @NeptusProperty(description = "Nominal Speed")
    public double speed = 1;

    @NeptusProperty(description = "Maximum Depth")
    public double max_depth = 10;

    @NeptusProperty(description = "Minimum Depth")
    public double min_depth = 0.0;

    @NeptusProperty(description = "Maximum Speed")
    public double max_speed = 1.5;

    @NeptusProperty(description = "Minimum Speed")
    public double min_speed = 0.7;

    @NeptusProperty(description = "Maximum time underwater")
    public int mins_under = 10;

    @NeptusProperty(description = "Number where to send reports")
    public String sms_number = "+351914785889";

    @NeptusProperty(description = "Seconds to idle at each vertex")
    public int wait_secs = 60;

    @NeptusProperty(description = "SOI plan identifier")
    public String soi_plan_id = "soi_plan";
    
    @NeptusProperty(description = "Watchdog timeout, in minutes")
    public int mins_timeout = 600;
    

    @NeptusProperty(description = "Cyclic execution")
    public boolean cycle = false;
}