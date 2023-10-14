/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 10/06/2016
 */
package pt.lsts.neptus.util;

/**
 * @author pdias
 *
 */
public class AISUtil {

    private AISUtil() {
    }

    /**
     * Translate ship type code to human-readable message.
     * 
     * @param code
     * @return
     */
    public static  String translateShipType(int code) {
        String res;
        switch (code) {
            case 0:
                res = "Not available (default)";
                break;
            case 1:
                res = "Reserved for future use";
                break;
            case 2:
                res = "Reserved for future use";
                break;
            case 3:
                res = "Reserved for future use";
                break;
            case 4:
                res = "Reserved for future use";
                break;
            case 5:
                res = "Reserved for future use";
                break;
            case 6:
                res = "Reserved for future use";
                break;
            case 7:
                res = "Reserved for future use";
                break;
            case 8:
                res = "Reserved for future use";
                break;
            case 9:
                res = "Reserved for future use";
                break;
            case 10:
                res = "Reserved for future use";
                break;
            case 11:
                res = "Reserved for future use";
                break;
            case 12:
                res = "Reserved for future use";
                break;
            case 13:
                res = "Reserved for future use";
                break;
            case 14:
                res = "Reserved for future use";
                break;
            case 15:
                res = "Reserved for future use";
                break;
            case 16:
                res = "Reserved for future use";
                break;
            case 17:
                res = "Reserved for future use";
                break;
            case 18:
                res = "Reserved for future use";
                break;
            case 19:
                res = "Reserved for future use";
                break;
            case 20:
                res = "Wing in ground (WIG), all ships of this type";
                break;
            case 21:
                res = "Wing in ground (WIG), Hazardous category A";
                break;
            case 22:
                res = "Wing in ground (WIG), Hazardous category B";
                break;
            case 23:
                res = "Wing in ground (WIG), Hazardous category C";
                break;
            case 24:
                res = "Wing in ground (WIG), Hazardous category D";
                break;
            case 25:
                res = "Wing in ground (WIG), Reserved for future use";
                break;
            case 26:
                res = "Wing in ground (WIG), Reserved for future use";
                break;
            case 27:
                res = "Wing in ground (WIG), Reserved for future use";
                break;
            case 28:
                res = "Wing in ground (WIG), Reserved for future use";
                break;
            case 29:
                res = "Wing in ground (WIG), Reserved for future use";
                break;
            case 30:
                res = "Fishing";
                break;
            case 31:
                res = "Towing";
                break;
            case 32:
                res = "Towing: length exceeds 200m or breadth exceeds 25m";
                break;
            case 33:
                res = "Dredging or underwater ops";
                break;
            case 34:
                res = "Diving ops";
                break;
            case 35:
                res = "Military ops";
                break;
            case 36:
                res = "Sailing";
                break;
            case 37:
                res = "Pleasure Craft";
                break;
            case 38:
                res = "Reserved";
                break;
            case 39:
                res = "Reserved";
                break;
            case 40:
                res = "High speed craft (HSC), all ships of this type";
                break;
            case 41:
                res = "High speed craft (HSC), Hazardous category A";
                break;
            case 42:
                res = "High speed craft (HSC), Hazardous category B";
                break;
            case 43:
                res = "High speed craft (HSC), Hazardous category C";
                break;
            case 44:
                res = "High speed craft (HSC), Hazardous category D";
                break;
            case 45:
                res = "High speed craft (HSC), Reserved for future use";
                break;
            case 46:
                res = "High speed craft (HSC), Reserved for future use";
                break;
            case 47:
                res = "High speed craft (HSC), Reserved for future use";
                break;
            case 48:
                res = "High speed craft (HSC), Reserved for future use";
                break;
            case 49:
                res = "High speed craft (HSC), No additional information";
                break;
            case 50:
                res = "Pilot Vessel";
                break;
            case 51:
                res = "Search and Rescue vessel";
                break;
            case 52:
                res = "Tug";
                break;
            case 53:
                res = "Port Tender";
                break;
            case 54:
                res = "Anti-pollution equipment";
                break;
            case 55:
                res = "Law Enforcement";
                break;
            case 56:
                res = "Spare - Local Vessel";
                break;
            case 57:
                res = "Spare - Local Vessel";
                break;
            case 58:
                res = "Medical Transport";
                break;
            case 59:
                res = "Noncombatant ship according to RR Resolution No. 18";
                break;
            case 60:
                res = "Passenger, all ships of this type";
                break;
            case 61:
                res = "Passenger, Hazardous category A";
                break;
            case 62:
                res = "Passenger, Hazardous category B";
                break;
            case 63:
                res = "Passenger, Hazardous category C";
                break;
            case 64:
                res = "Passenger, Hazardous category D";
                break;
            case 65:
                res = "Passenger, Reserved for future use";
                break;
            case 66:
                res = "Passenger, Reserved for future use";
                break;
            case 67:
                res = "Passenger, Reserved for future use";
                break;
            case 68:
                res = "Passenger, Reserved for future use";
                break;
            case 69:
                res = "Passenger, No additional information";
                break;
            case 70:
                res = "Cargo, all ships of this type";
                break;
            case 71:
                res = "Cargo, Hazardous category A";
                break;
            case 72:
                res = "Cargo, Hazardous category B";
                break;
            case 73:
                res = "Cargo, Hazardous category C";
                break;
            case 74:
                res = "Cargo, Hazardous category D";
                break;
            case 75:
                res = "Cargo, Reserved for future use";
                break;
            case 76:
                res = "Cargo, Reserved for future use";
                break;
            case 77:
                res = "Cargo, Reserved for future use";
                break;
            case 78:
                res = "Cargo, Reserved for future use";
                break;
            case 79:
                res = "Cargo, No additional information";
                break;
            case 80:
                res = "Tanker, all ships of this type";
                break;
            case 81:
                res = "Tanker, Hazardous category A";
                break;
            case 82:
                res = "Tanker, Hazardous category B";
                break;
            case 83:
                res = "Tanker, Hazardous category C";
                break;
            case 84:
                res = "Tanker, Hazardous category D";
                break;
            case 85:
                res = "Tanker, Reserved for future use";
                break;
            case 86:
                res = "Tanker, Reserved for future use";
                break;
            case 87:
                res = "Tanker, Reserved for future use";
                break;
            case 88:
                res = "Tanker, Reserved for future use";
                break;
            case 89:
                res = "Tanker, No additional information";
                break;
            case 90:
                res = "Other Type, all ships of this type";
                break;
            case 91:
                res = "Other Type, Hazardous category A";
                break;
            case 92:
                res = "Other Type, Hazardous category B";
                break;
            case 93:
                res = "Other Type, Hazardous category C";
                break;
            case 94:
                res = "Other Type, Hazardous category D";
                break;
            case 95:
                res = "Other Type, Reserved for future use";
                break;
            case 96:
                res = "Other Type, Reserved for future use";
                break;
            case 97:
                res = "Other Type, Reserved for future use";
                break;
            case 98:
                res = "Other Type, Reserved for future use";
                break;
            case 99:
                res = "Other Type, no additional information";
                break;
            default:
                res = "Unknown ship type " + code;
                break;
        }
        return res;
    }
    
    public static String translateNavigationalStatus(int code) {
        String res;
        switch (code) {
            case 0:
                res = "under way using engine";
                break;
            case 1:
                res = "at anchor";
                break;
            case 2:
                res = "not under command ";
                break;
            case 3:
                res = "restricted maneuverability";
                break;
            case 4:
                res = "constrained by her draught";
                break;
            case 5:
                res = "moored";
                break;
            case 6:
                res = "aground ";
                break;
            case 7:
                res = "engaged in fishing";
                break;
            case 8:
                res = "under way sailing";
                break;
            case 9:
                res = "reserved for future"; // amendment of navigational status for ships carrying DG, HS, or MP, or
                                             // IMO hazard or pollutant category C, high-speed craft (HSC)";
                break;
            case 10:
                res = "reserved for future"; // amendment of navigational status for ships carrying dangerous goods
                                             // (DG), harmful substances (HS) or marine pollutants (MP), or IMO hazard
                                             // or pollutant ";category A, wing in ground (WIG)
                break;
            case 11:
                res = "power-driven vessel towing astern (regional use)";
                break;
            case 12:
                res = "power-driven vessel pushing ahead or towing alongside (regional use)";
                break;
            case 13:
                res = "reserved for future use";
                break;
            case 14:
                res = "AIS-SART (active), MOB-AIS, EPIRB-AIS";
                break;
            case 15:
            default:
                res = "undefined"; // = default (also used by AIS-SART, MOB-AIS and EPIRB-AIS under test)";
                break;
        }
        return res;
    }
}
