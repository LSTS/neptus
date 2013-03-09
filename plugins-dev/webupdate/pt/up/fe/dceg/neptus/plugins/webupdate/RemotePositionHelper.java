/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 4 de Jul de 2012
 */
package pt.up.fe.dceg.neptus.plugins.webupdate;

import java.util.Date;
import java.util.LinkedHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystem;
import pt.up.fe.dceg.neptus.systems.external.ExternalSystemsHolder;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;
import pt.up.fe.dceg.neptus.util.XMLUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

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
            Date dateTimeS = DateTimeUtil.dateTimeFormaterUTC.parse(dateS + " " + timeS);
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
                    Date dateTime = DateTimeUtil.dateTimeFormaterUTC.parse(date + " " + time);
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
                    if (loc != null && phi != Double.NaN && theta != Double.NaN
                            && psi != Double.NaN) {
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
                        if (coordinateSystem.getLatitudeAsDoubleValue() != 0d && coordinateSystem.getLongitudeAsDoubleValue() != 0d) {
                            sys.setLocation(coordinateSystem, time);
                            
                            if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                    && coordinateSystem.getYaw() != 0d) {
                                sys.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                        coordinateSystem.getYaw(), time);
                            }
                            sys.storeData(ImcSystem.WEB_UPDATED_KEY, true, time, true);
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
                        if (coordinateSystem.getLatitudeAsDoubleValue() != 0d && coordinateSystem.getLongitudeAsDoubleValue() != 0d) {
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
