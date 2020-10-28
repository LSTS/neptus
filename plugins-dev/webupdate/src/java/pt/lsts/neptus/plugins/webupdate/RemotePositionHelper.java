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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 4 de Jul de 2012
 */
package pt.lsts.neptus.plugins.webupdate;

import java.util.Date;
import java.util.LinkedHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author pdias
 *
 */
class RemotePositionHelper {

    private RemotePositionHelper() {
    }
    
    /**
     * 
     */
    public static long getRemoteState(LinkedHashMap<String, Long> timeSysList, LinkedHashMap<String, CoordinateSystem> locSysList,
            Document docProfiles) {

        long lastCalcPosTimeMillis = -1;

        Element root = docProfiles.getDocumentElement();

        CoordinateSystem loc = null;
        double phi = Double.NaN, theta = Double.NaN, psi = Double.NaN;
        // time="21:02:58.233" date="2011-07-07"
        try {
            String dateS = root.getAttributes().getNamedItem("date").getTextContent();
            String timeS = root.getAttributes().getNamedItem("time").getTextContent();
            Date dateTimeS = DateTimeUtil.dateTimeFormatterUTC.parse(dateS + " " + timeS);
            lastCalcPosTimeMillis  = dateTimeS.getTime();
        }
        catch (Exception e1) {
            e1.printStackTrace();
            lastCalcPosTimeMillis = -1;
        }
        Node bn = root.getFirstChild();
        while (bn != null) {
            if ("VehicleState".equalsIgnoreCase(bn.getNodeName())) {
                try {
                    // <VehicleState id="lauv-seacon-1" time="16:31:52.044"
                    // date="2011-07-07" publisher="178.166.107.33">
                    String id = bn.getAttributes().getNamedItem("id").getTextContent();
                    String date = bn.getAttributes().getNamedItem("date").getTextContent();
                    String time = bn.getAttributes().getNamedItem("time").getTextContent();
                    Date dateTime = DateTimeUtil.dateTimeFormatterUTC.parse(date + " " + time);
                    NodeList clst = bn.getChildNodes();
                    for (int i = 0; i < clst.getLength(); i++) {
                        Node node = clst.item(i);
                        if ("coordinate".equalsIgnoreCase(node.getNodeName())) {
                            String str = XMLUtil.nodeToString(node);
                            loc = new CoordinateSystem(str);
                        }
                        else if ("attitude".equalsIgnoreCase(node.getNodeName())) {
                            try {
                                NodeList anode = node.getChildNodes();
                                for (int j = 0; j < anode.getLength(); j++) {
                                    Node nodep = anode.item(j);
                                    if ("phi".equalsIgnoreCase(nodep.getNodeName())) {
                                        String pName = nodep.getTextContent();
                                        phi = Double.parseDouble(pName);
                                    }
                                    else if ("theta".equalsIgnoreCase(nodep.getNodeName())) {
                                        String pName = nodep.getTextContent();
                                        theta = Double.parseDouble(pName);
                                    }
                                    else if ("psi".equalsIgnoreCase(nodep.getNodeName())) {
                                        String pName = nodep.getTextContent();
                                        psi = Double.parseDouble(pName);
                                    }
                                }
                            }
                            catch (Exception e) {
                                NeptusLog.pub().debug(ReflectionUtil.getCallerStamp()
                                        + e.getMessage());
                                phi = Double.NaN;
                                theta = Double.NaN;
                                psi = Double.NaN;
                            }
                        }
                    }
                    if (loc != null && !Double.isNaN(phi) && !Double.isNaN(theta)
                            && !Double.isNaN(psi)) {
                        loc.setRoll(Math.toDegrees(phi));
                        loc.setPitch(Math.toDegrees(theta));
                        loc.setYaw(Math.toDegrees(psi));
                    }
                    timeSysList.put(id, dateTime.getTime());
                    locSysList.put(id, loc);
                }
                catch (Exception e) {
                    NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
                }
            }
            bn = bn.getNextSibling();
        }
        return lastCalcPosTimeMillis;
    }

    /**
     * @param timeSysList
     * @param locSysList
     */
    static void publishRemoteStatesLocally(LinkedHashMap<String, Long> timeSysList,
            LinkedHashMap<String, CoordinateSystem> locSysList) {
        try {
            String[] ids = timeSysList.keySet().toArray(new String[0]);
            Long[] timeMillis = timeSysList.values().toArray(new Long[0]);
            CoordinateSystem[] posAndAttitude = locSysList.values().toArray(new CoordinateSystem[0]);
            
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i];
                long time = timeMillis[i];
                CoordinateSystem coordinateSystem = posAndAttitude[i];
                
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);
                if (sys != null) {
                    if ((System.currentTimeMillis() - time < DateTimeUtil.MINUTE * 10) && sys.getLocationTimeMillis() < time) {
                        if (coordinateSystem.getLatitudeDegs() != 0d && coordinateSystem.getLongitudeDegs() != 0d) {
                            sys.setLocation(coordinateSystem, time);
                            
                            if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                    && coordinateSystem.getYaw() != 0d) {
                                sys.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                        coordinateSystem.getYaw(), time);
                            }
                            sys.storeData(SystemUtils.WEB_UPDATED_KEY, true, time, true);
                        }
                    }
                }
                else {
                    ExternalSystem ext = ExternalSystemsHolder.lookupSystem(id);
                    boolean registerNewExternal = false;
                    if (ext == null) {
                        ext = new ExternalSystem(id);
                        registerNewExternal = true;
                    }
                    if ((System.currentTimeMillis() - time < DateTimeUtil.MINUTE * 10) && ext.getLocationTimeMillis() < time) {
                        if (coordinateSystem.getLatitudeDegs() != 0d && coordinateSystem.getLongitudeDegs() != 0d) {
                            ext.setLocation(coordinateSystem, time);
                            
                            if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                    && coordinateSystem.getYaw() != 0d) {
                                ext.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                        coordinateSystem.getYaw(), time);
                            }

                            if (registerNewExternal)
                                ExternalSystemsHolder.registerSystem(ext);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
