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
 * Author: zp
 * Apr 7, 2014
 */
package pt.lsts.neptus.comm.iridium;

import java.util.HashMap;
import java.util.Map;

public class Position {

    public int id;
    public double latRads, lonRads, timestamp;
    public PosType posType = PosType.Unknown;

    public enum PosType {
        Unknown (000, 0, "Unknown / Other"),
        AUV     (021, 0, "Autonomous Underwater Vehicle"),
        UAV     (022, 0, "Unmanned Aerial Vehicle"),
        ASV     (023, 0, "Autonomous Surface Vehicle"),
        CCU     (101, 0, "Command and Control Unit"),
        SpotTag (103, 0, "SPOT satellite tag"),
        Argos0  (0x30, 3000, "Argos tag position of class 0 (more than 1500 m of uncertainty radius)"),
        Argos1  (0x31, 1500, "Argos tag position of class 1 (500 m < uncertainty radius < 1500 m)"),
        Argos2  (0x32, 500, "Argos tag position of class 2 (250 m < uncertainty radius < 500 m)"),
        Argos3  (0x33, 250, "Argos tag position of class 3 (uncertainty radius < 250 m)"),
        ArgosA  (0x41, 3000, "Argos tag position of class A (uncertainty radius > 1500 m, 3 transmissions)"),
        ArgosB  (0x42, 3000, "Argos tag position of class B (uncertainty radius > 1500 m, 2 transmissions)"),
        ArgosG  (0x47, 100, "Argos tag position of class G (GPS, uncertainty radius < 100 m)"),
        ArgosZ  (0x5A, 0, "Argos tag position of class Z (no position obtained)");    

        protected long value;
        protected long uncertainty;
        protected String desc;

        public long value() {
            return value;
        }

        public long uncertainty() {
            return uncertainty;
        }

        public String desc() {
            return desc;
        }

        PosType(long value, long uncertainty, String desc) {
            this.value = value;
            this.desc = desc;
        }
    }

    private static final Map<Long, PosType> intToTypeMap = new HashMap<Long, PosType>();
    static {
        for (PosType type : PosType.values()) {
            intToTypeMap.put(type.value, type);
        }
    }

    public static PosType fromImcId(int imcId) {
        int sys_selector = 0xE000;
        int vtype_selector = 0x1800;

        int sys_type = (imcId & sys_selector) >> 13;
        
        switch (sys_type) {
            case 0:
            case 1:
                switch ((imcId & vtype_selector) >> 11) {
                    case 0:
                        return PosType.AUV;
                    case 1:
                        return PosType.AUV;
                    case 2:
                        return PosType.ASV;
                    case 3:
                        return PosType.UAV;
                    default:
                        return PosType.Unknown;
                }
            case 2:
                return PosType.CCU;
            default:
                return PosType.Unknown;
        }
    }

    public static PosType fromInt(int i) {
        PosType type = intToTypeMap.get(Long.valueOf(i));
        if (type == null) 
            return PosType.Unknown;
        return type;
    }
}