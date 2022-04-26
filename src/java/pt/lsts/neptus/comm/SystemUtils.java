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
 * Author: pdias
 * 22/03/2016
 */
package pt.lsts.neptus.comm;

import pt.lsts.neptus.systems.external.ExternalSystem.ExternalTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author pdias
 *
 */
public class SystemUtils {

    // Constants for ImcSystem and ExternalSystem data store
    public final static String GROUND_SPEED_KEY = "Ground Speed";
    public final static String VERTICAL_SPEED_KEY = "Vertical Speed";
    public final static String TRUE_SPEED_KEY = "True Speed";
    public final static String INDICATED_SPEED_KEY = "Indicate Speed";
    public final static String RPM_MAP_ENTITY_KEY = "RPM";
    public final static String COURSE_DEGS_KEY = "Course";
    public final static String HEADING_DEGS_KEY = "Heading";
    public final static String FUEL_LEVEL_KEY = "Fuel Level";
    public final static String WEB_UPDATED_KEY = "Web Updated";
    public final static String LBL_CONFIG_KEY = "LblConfig";
    public final static String ACOUSTIC_SYSTEMS = "AcousticSystems";
    public final static String ENTITY_PARAMETERS = "EntityParameters";
    public static final String WIDTH_KEY = "Width";
    public static final String LENGHT_KEY = "Lenght";
    public static final String WIDTH_CENTER_OFFSET_KEY = "Width Center Offset";
    public static final String LENGHT_CENTER_OFFSET_KEY = "Lenght Center Offset";
    public static final String DRAUGHT_KEY = "Draught";
    public static final String MMSI_KEY = "MMSI";
    public static final String SHIP_TYPE_KEY = "AIS Ship Type";
    public static final String NAV_STATUS_KEY = "AIS Navigational Status";
    public static final String CALL_SIGN_KEY = "Call Sign";
    public static final String RATE_OF_TURN_DEGS_PER_MIN_KEY = "Rate of Turn (deg/min)";
    public static final String DISTRESS_MSG_KEY = "Distress Message";

    /** To avoid instantiation */
    private SystemUtils() {
    }

    /**
     * Gets a string and tries to translate it to {@link SystemTypeEnum}.
     * 
     * @param typeStr
     * @return
     */
    public static SystemTypeEnum getSystemTypeFrom(String typeStr) {
        SystemTypeEnum ret = SystemTypeEnum.UNKNOWN;
        
        for (SystemTypeEnum ste : SystemTypeEnum.values()) {
            if (ste == SystemTypeEnum.ALL || ste == SystemTypeEnum.UNKNOWN)
                continue;
            
            String str = ste.toString();
            if (str.equalsIgnoreCase(typeStr.trim()))
                return ste;
            else if (str.equalsIgnoreCase(typeStr.replace(" ", "").trim()))
                return ste;
            else if (str.equalsIgnoreCase(typeStr.replace("_", "").trim()))
                return ste;
        }

        // Try additional options
        if ("drifter".equalsIgnoreCase(typeStr.trim()))
            return SystemTypeEnum.MOBILESENSOR;
        else if ("WSN".equalsIgnoreCase(typeStr.trim()))
            return SystemTypeEnum.MOBILESENSOR;
        else if ("mooring".equalsIgnoreCase(typeStr.trim()))
            return SystemTypeEnum.STATICSENSOR;
        else if ("buoy".equalsIgnoreCase(typeStr.trim()))
            return SystemTypeEnum.STATICSENSOR;
        else if ("glider".equalsIgnoreCase(typeStr.trim()))
            return SystemTypeEnum.VEHICLE;
        else if (!"helicopter".equalsIgnoreCase(typeStr.trim()) && typeStr.trim().endsWith("copter"))
            return SystemTypeEnum.VEHICLE;

        return ret;
    }

    /**
     * Gets a string and tries to translate it to {@link VehicleTypeEnum}.
     * 
     * @param typeStr
     * @return
     */
    public static VehicleTypeEnum getVehicleTypeFrom(String typeStr) {
        VehicleTypeEnum ret = VehicleTypeEnum.UNKNOWN;
        
        for (VehicleTypeEnum ste : VehicleTypeEnum.values()) {
            if (ste == VehicleTypeEnum.ALL || ste == VehicleTypeEnum.UNKNOWN)
                continue;
            
            String str = ste.toString();
            if (str.equalsIgnoreCase(typeStr.trim()))
                return ste;
        }

        // Try additional options
        if ("AUV".equalsIgnoreCase(typeStr.trim()))
            return VehicleTypeEnum.UUV;
        else if ("ASV".equalsIgnoreCase(typeStr.trim()))
            return VehicleTypeEnum.USV;
        else if ("AGV".equalsIgnoreCase(typeStr.trim()))
            return VehicleTypeEnum.UGV;
        else if ("glider".equalsIgnoreCase(typeStr.trim()))
            return VehicleTypeEnum.UUV;
        else if (!"helicopter".equalsIgnoreCase(typeStr.trim()) && typeStr.trim().endsWith("copter"))
            return VehicleTypeEnum.UAV;

        return ret;
    }

    /**
     * Gets a string and tries to translate it to {@link ExternalTypeEnum}.
     * 
     * @param typeStr
     * @return
     */
    public static ExternalTypeEnum getExternalTypeFrom(String typeStr) {
        ExternalTypeEnum ret = ExternalTypeEnum.UNKNOWN;
        
        for (ExternalTypeEnum ste : ExternalTypeEnum.values()) {
            if (ste == ExternalTypeEnum.ALL || ste == ExternalTypeEnum.UNKNOWN)
                continue;
            
            String str = ste.toString();
            if (str.equalsIgnoreCase(typeStr.trim()))
                return ste;
            else if (str.equalsIgnoreCase(typeStr.replace(" ", "").trim()))
                return ste;
            else if (str.equalsIgnoreCase(typeStr.replace(" ", "_").trim()))
                return ste;
            
        }
        
        // Try UAx
        VehicleTypeEnum vt = getVehicleTypeFrom(typeStr);
        if (VehicleTypeEnum.UNKNOWN != vt && vt.toString().toUpperCase().startsWith("U"))
            return ExternalTypeEnum.VEHICLE;
        
        typeStr = typeStr.toLowerCase();
        
        // Try additional options
        if ("drifter".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MOBILESENSOR;
        else if ("WSN".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MOBILESENSOR;
        else if ("mooring".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.STATICSENSOR;
        else if ("buoy".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.STATICSENSOR;
        else if ("glider".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.VEHICLE;
        else if ("AIS".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MANNED_SHIP;
        else if ("ship".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MANNED_SHIP;
        else if (typeStr.trim().contains("ship") || typeStr.trim().contains("vessel"))
            return ExternalTypeEnum.MANNED_SHIP;
        else if (typeStr.trim().contains("(wig)") || typeStr.trim().contains("(hsc)")
                || typeStr.trim().contains("fishing") || typeStr.trim().contains("towing")
                || typeStr.trim().contains("dredging") || typeStr.trim().contains("sailing")
                || typeStr.trim().contains("port tender") || typeStr.trim().contains("craft")
                || typeStr.trim().contains("cargo") || typeStr.trim().contains("tanker")
                || typeStr.trim().contains("passenger")|| typeStr.trim().contains("tug"))
            return ExternalTypeEnum.MANNED_SHIP;
        else if ("car".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MANNED_CAR;
        else if ("automobile".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MANNED_CAR;
        else if ("airplane".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MANNED_AIRPLANE;
        else if ("helicopter".equalsIgnoreCase(typeStr.trim()))
            return ExternalTypeEnum.MANNED_AIRPLANE;
        else if (typeStr.trim().endsWith("copter") || typeStr.trim().contains("copter"))
            return ExternalTypeEnum.VEHICLE;
        
        return ret;
    }
    
    public static void main(String[] args) {
        String[] typeLst = { "ccu", "manned airplane", "airplane", "drifter", "glider", "AIS", "mooring", "ship",
                "AGV", "AUV", "car", "automobile", "quadcopter", "octacopter", "helicopter"};
        for (String type : typeLst) {
            System.out.printf("SystemType\t %s \t--> %s \n", type, getSystemTypeFrom(type));
            System.out.printf("VehicleType\t %s \t--> %s \n", type, getVehicleTypeFrom(type));
            System.out.printf("ExternalType\t %s \t--> %s \n\n", type, getExternalTypeFrom(type));
        }
    }
}
