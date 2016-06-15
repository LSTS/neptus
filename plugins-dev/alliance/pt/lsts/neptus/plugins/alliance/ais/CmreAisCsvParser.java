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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 15/06/2016
 */
package pt.lsts.neptus.plugins.alliance.ais;

import java.util.Arrays;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.plugins.alliance.AisContactDb;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AISUtil;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.NMEAUtils;

/**
 * @author pdias
 *
 */
public class CmreAisCsvParser {

    private CmreAisCsvParser() {
    }

    public static String getType(String sentence) {
        String[] tk = sentence.trim().split(",");
        if (tk.length < 1)
            return "";
        return tk[0].trim();
    }
    
    /**
     * @param sentence
     * @param contactDb 
     */
    public static void process(String sentence, AisContactDb contactDb) {
        String type = getType(sentence);
        if (type.isEmpty())
            return;
        
        switch (type.toUpperCase()) {
            case "AIS":
                parseAIS(sentence, contactDb);
                break;
            case "DISTRESS_CALL":
                parseDistressCall(sentence, contactDb);
                break;
            default:
                NeptusLog.pub().error("Type not known (" + type + ")");
                break;
        }
    }

    /**
     * Example:
     * 
     * AIS,Node_Name=211212500,Node_Type=ship,Latitude=38.889712,Longitude=­
     * 77.008934,Depth=0,Speed=0,Heading=52.5,Course=n.a.,RateOfTurn=n.a.,Timestamp=n.a.,
     * Navigation_Status=1,Number_Contacts=1,
     * Node_Name=221212500,Node_Type=ship,
     * Latitude=38.887712,Longitude=­77.018934,Depth=0,Speed=0.5,Heading=62.5,
     * Course=n.a.,RateOfTurn=n.a.,Timestamp=n.a.,Navigation_Status=0\r\n
     * 
     * @param sentence
     * @param contactDb 
     */
    private static void parseAIS(String sentence, AisContactDb contactDb) {
        final int AIS_ELM = 11;
        final int EXTRA_COUNTER_IDX = 12;
        final int MIN_ELMS = 13;
        String[] tk = sentence.split(",");
        if (tk.length < MIN_ELMS || !"AIS".equalsIgnoreCase(tk[0].trim()))
            return;
        
        String[] msg = Arrays.copyOfRange(tk, 1, EXTRA_COUNTER_IDX);
        @SuppressWarnings("unused")
        boolean res = parseOneAISWorker(msg, contactDb);
        int extraElements = 0;
        try {
            extraElements = Integer.parseInt(tk[EXTRA_COUNTER_IDX].split("=")[1].trim());
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }
        
        if (extraElements == 0)
            return;
        if (tk.length != MIN_ELMS + AIS_ELM * extraElements)
            return;
        
        for (int i = 0; i < extraElements; i++) {
            msg = Arrays.copyOfRange(tk, MIN_ELMS + AIS_ELM * i, MIN_ELMS + AIS_ELM * (i + 1));
            res |= parseOneAISWorker(msg, contactDb);
        }
    }

    /**
     * @param msg
     * @param contactDb
     * @return
     */
    private static boolean parseOneAISWorker(String[] msg, AisContactDb contactDb) {
        if (msg.length < 11)
            return false;
        
        String name = null;
        String type = "";
        double latDegs = 0;
        double lonDegs = 0;
        double depth = 0;
        double speedKnots = 0;
        double headingDegs = 0;
        double courseDegs = 0;
        double rateOfTurnDegsPerMin = Double.NaN;
        double timestamp = Double.NaN;
        int navStatus = -1;
        
        try {
            for (String st : msg) {
                String[] tk = st.split("=");
                String v;
                switch (tk[0].trim().toLowerCase()) {
                    case "node_name":
                        name = tk[1].trim();
                        break;
                    case "node_type":
                        v = tk[1].trim();
                        if (v.toLowerCase().startsWith("n.a"))
                            type = "";
                        else
                            type = tk[1].trim();
                        break;
                    case "latitude":
                        latDegs = Double.parseDouble(tk[1].trim());
                        break;
                    case "longitude":
                        lonDegs = Double.parseDouble(tk[1].trim());
                        break;
                    case "depth":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            depth = Double.parseDouble(v);
                        break;
                    case "speed":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            speedKnots = Double.parseDouble(v);
                        break;
                    case "heading":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            headingDegs = AngleUtils.nomalizeAngleDegrees360(Double.parseDouble(v));
                        if (headingDegs > 360)
                            headingDegs = 0;
                        break;
                    case "course":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            courseDegs = AngleUtils.nomalizeAngleDegrees360(Double.parseDouble(v));
                        if (courseDegs > 360)
                            courseDegs = 0;
                        break;
                    case "rateofturn":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            rateOfTurnDegsPerMin = Double.parseDouble(v);
                        break;
                    case "timestamp":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            timestamp = Double.parseDouble(v);
                        break;
                    case "navigation_status":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            navStatus = Integer.parseInt(v);
                        break;
                    default:
                        NeptusLog.pub().warn("Token not known (" + st + ")!");
                        break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        long timeMillis = System.currentTimeMillis();
        if (Double.isFinite(timestamp)) // assuming millis since epoch
            timeMillis = Double.valueOf(timestamp).longValue();
        
        int mmsi = -1;
        try {
            mmsi = Integer.parseInt(name);
            String shipName = contactDb.getNameForMMSI(mmsi);
            if (shipName != null && !shipName.isEmpty())
                name = shipName;
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        
        ExternalSystem sys = NMEAUtils.getAndRegisterExternalSystem(mmsi, name);
        if (sys == null)
            return false;
        
        // OK, let us fill the data
        LocationType loc = new LocationType(latDegs, lonDegs);
        loc.setDepth(depth);
        sys.setLocation(loc, timeMillis);
        sys.setAttitudeDegrees(headingDegs > 360 ? courseDegs : headingDegs, timeMillis);
        
        if (mmsi > 0)
            sys.storeData(SystemUtils.MMSI_KEY, mmsi, timeMillis, true);

        sys.storeData(SystemUtils.GROUND_SPEED_KEY, speedKnots / AisContactDb.MPS_TO_KNOT_CONV, timeMillis, true);
        sys.storeData(SystemUtils.COURSE_KEY, courseDegs, timeMillis, true);

        sys.storeData(SystemUtils.NAV_STATUS_KEY, AISUtil.translateNavigationalStatus(navStatus), timeMillis, true);

        if (!type.isEmpty()) {
            sys.storeData(SystemUtils.SHIP_TYPE_KEY, type, timeMillis, true);
            sys.setType(SystemUtils.getSystemTypeFrom(type));
            sys.setTypeExternal(SystemUtils.getExternalTypeFrom(type));
            sys.setTypeVehicle(SystemUtils.getVehicleTypeFrom(type));
        }
        
        if (Double.isFinite(rateOfTurnDegsPerMin)) {
            sys.storeData(SystemUtils.RATE_OF_TURN_DEGS_PER_MIN_KEY, rateOfTurnDegsPerMin, timeMillis, true);
        }
        
        return true;
    }
    
    /**
     * @param sentence
     * @param contactDb
     */
    private static void parseDistressCall(String sentence, AisContactDb contactDb) {
        final int MIN_ELMS = 11;
        final int MIN_BASE_ELMS = 7;
        String[] tk = sentence.split(",");
        if (tk.length < MIN_ELMS || !"DISTRESS_CALL".equalsIgnoreCase(tk[0].trim()))
            return;
        
        String[] msg = Arrays.copyOfRange(tk, 1, MIN_ELMS);
        
        int countBaseElm = 0;
        String name = null;
        String type = "";
        double latDegs = 0;
        double lonDegs = 0;
        double depth = 0;
        double speedKnots = 0;
        double headingDegs = 0;
        StringBuilder distressSb = new StringBuilder();

        try {
            boolean firstDistressParcel = true;
            for (String st : msg) {
                tk = st.split("=");
                String v;
                switch (tk[0].trim().toLowerCase()) {
                    case "node_name":
                        name = tk[1].trim();
                        countBaseElm++;
                        break;
                    case "node_type":
                        v = tk[1].trim();
                        if (v.toLowerCase().startsWith("n.a"))
                            type = "";
                        else
                            type = tk[1].trim();
                        countBaseElm++;
                        break;
                    case "latitude":
                        latDegs = Double.parseDouble(tk[1].trim());
                        countBaseElm++;
                        break;
                    case "longitude":
                        lonDegs = Double.parseDouble(tk[1].trim());
                        countBaseElm++;
                        break;
                    case "depth":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            depth = Double.parseDouble(v);
                        countBaseElm++;
                        break;
                    case "speed":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            speedKnots = Double.parseDouble(v);
                        countBaseElm++;
                        break;
                    case "heading":
                        v = tk[1].trim();
                        if (!v.toLowerCase().startsWith("n.a"))
                            headingDegs = AngleUtils.nomalizeAngleDegrees360(Double.parseDouble(v));
                        if (headingDegs > 360)
                            headingDegs = 0;
                        countBaseElm++;
                        break;
                    default:
                        // NeptusLog.pub().warn("Token not known (" + st + ")!");
                        if (!firstDistressParcel)
                            distressSb.append(", ");
                        else
                            firstDistressParcel = false;
                        distressSb.append(st);
                        break;
                }
            }
            if (countBaseElm < MIN_BASE_ELMS)
                return;
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        long timeMillis = System.currentTimeMillis();
        
        int mmsi = -1;
        try {
            mmsi = Integer.parseInt(name);
            String shipName = contactDb.getNameForMMSI(mmsi);
            if (shipName != null && !shipName.isEmpty())
                name = shipName;
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        
        ExternalSystem sys = NMEAUtils.getAndRegisterExternalSystem(mmsi, name);
        if (sys == null)
            return;
        
        // OK, let us fill the data
        LocationType loc = new LocationType(latDegs, lonDegs);
        loc.setDepth(depth);
        sys.setLocation(loc, timeMillis);
//        sys.setAttitudeDegrees(headingDegs > 360 ? courseDegs : headingDegs, timeMillis);
        if (headingDegs >= 0 && headingDegs < 360)
            sys.setAttitudeDegrees(headingDegs, timeMillis);
        
        if (mmsi > 0)
            sys.storeData(SystemUtils.MMSI_KEY, mmsi, timeMillis, true);

        sys.storeData(SystemUtils.GROUND_SPEED_KEY, speedKnots / AisContactDb.MPS_TO_KNOT_CONV, timeMillis, true);
//        sys.storeData(SystemUtils.COURSE_KEY, courseDegs, timeMillis, true);

        if (!type.isEmpty()) {
            sys.storeData(SystemUtils.SHIP_TYPE_KEY, type, timeMillis, true);
            sys.setType(SystemUtils.getSystemTypeFrom(type));
            sys.setTypeExternal(SystemUtils.getExternalTypeFrom(type));
            sys.setTypeVehicle(SystemUtils.getVehicleTypeFrom(type));
        }

        if (distressSb.length() > 0)
            distressSb.insert(0, " :: ");
        distressSb.insert(0, "DISTRESS");
        String distress = distressSb.toString();
        sys.storeData(SystemUtils.DISTRESS_MSG_KEY, distress, timeMillis, true);
    }

    public static void main(String[] args) {
        AisContactDb contactDb = new AisContactDb();
        
        String sentenceAIS = "AIS,Node_Name=211212500,Node_Type=ship,Latitude=38.889712,"
                + "Longitude=-77.008934,Depth=0,Speed=0,Heading=52.5,Course=n.a.,"
                + "RateOfTurn=n.a.,Timestamp=n.a.,Navigation_Status=1,Number_Contacts=1," 
                + "Node_Name=221212501,Node_Type=ship," 
                + "Latitude=38.99999,Longitude=-77.000000,Depth=0,Speed=0.5,Heading=62.5," 
                + "Course=n.a.,RateOfTurn=n.a.,Timestamp=n.a.,Navigation_Status=0\r\n";
        
        parseAIS(sentenceAIS, contactDb);
        
        System.out.println(Arrays.toString(ExternalSystemsHolder.lookupAllActiveSystems()));
        
        String sentenceDistressCall = "DISTRESS_CALL,Node_Name=211342500,Node_Type=conventional sub,"
                + "Latitude=38.889712,Longitude=-77.008934,Depth=100,Speed=0,Heading=52.5,"
                + "Oxygen=15.4,Battery=23.2,People=76\r\n";

        parseDistressCall(sentenceDistressCall, contactDb);
        
        System.out.println(Arrays.toString(ExternalSystemsHolder.lookupAllActiveSystems()));
    }
}
