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
 * Author: Manuel R.
 * Nov 14, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import com.MAVLink.common.msg_param_request_list;
import com.MAVLink.common.msg_param_request_read;
import com.MAVLink.common.msg_param_set;

import pt.lsts.neptus.plugins.uavparameters.connection.MAVLinkConnection;

/**
 * @author Manuel R.
 *
 */
public class MAVLinkParameters {
    public static void requestParametersList(MAVLinkConnection con) {
        msg_param_request_list msg = new msg_param_request_list();
        msg.target_system = 1;
        msg.target_component = 1;
        con.sendMavPacket(msg.pack());
    }

    public static void sendParameter(MAVLinkConnection con, Parameter parameter) {
        msg_param_set msg = new msg_param_set();
        msg.target_system = 1;
        msg.target_component = 1;
        msg.setParam_Id(parameter.name);
        msg.param_type = (byte) parameter.type;
        msg.param_value = (float) parameter.value;
        con.sendMavPacket(msg.pack());
    }

    public static void readParameter(MAVLinkConnection con, String name) {
        msg_param_request_read msg = new msg_param_request_read();
        msg.param_index = -1;
        msg.target_system = 1;
        msg.target_component = 1;
        msg.setParam_Id(name);
        con.sendMavPacket(msg.pack());
    }

    public static void readParameter(MAVLinkConnection con, int index) {
        msg_param_request_read msg = new msg_param_request_read();
        msg.target_system = 1;
        msg.target_component = 1;
        msg.param_index = (short) index;
        con.sendMavPacket(msg.pack());
    }
}
