/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 2005/06/22
 */
package pt.lsts.neptus.comm;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import gnu.io.CommPortIdentifier;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.PortRangeValidator;

/**
 * @author Paulo Dias
 */
public class CommUtil {

    /**
     * @param vehicle
     * @param protocol
     * @return
     */
    public static CommMean getActiveCommMeanForProtocol(VehicleType vehicle, String protocol) {
        boolean testForProtocol = false;
        CommMean activeCommMean = null;

        if (protocol == null)
            testForProtocol = false;
        else if (protocol.equalsIgnoreCase(""))
            testForProtocol = false;
        else
            testForProtocol = true;

        LinkedList<CommMean> activeCM = getActiveCommMeanList(vehicle);
        if (activeCM == null)
            return null;
        Iterator<CommMean> it = activeCM.iterator();
        // activeCommMean = (CommMean) it.next();
        activeCommMean = null;
        while (it.hasNext()) {
            CommMean tcm = (CommMean) it.next();
            try {
                if (testForProtocol)
                    if (!testCommMeanForProtocol(tcm, protocol))
                        continue;
                if (activeCommMean == null) {
                    activeCommMean = tcm;
                    continue;
                }
                if (MathMiscUtils.parseEngineeringModeToDouble(tcm.getLatency()) > MathMiscUtils
                        .parseEngineeringModeToDouble(activeCommMean.getLatency())) {
                    activeCommMean = tcm;
                }
            }
            catch (NumberFormatException e) {
                // e.printStackTrace();
                NeptusLog.pub().error(
                        "CommUtil.getActiveCommMeanForProtocol :: " + "Comparing latencies. " + e.getMessage());
            }
        }
        if (activeCommMean == null) {
            NeptusLog.pub().error(
                    "CommUtil.getActiveCommMeanForProtocol :: No "
                            + ((testForProtocol) ? (protocol + " protocol") : "active CommMean")
                            + " for vehicle with id: " + vehicle.getId());
        }
        return activeCommMean;
    }

    /**
     * @param vehicle
     * @return
     */
    public static CommMean getActiveCommMean(VehicleType vehicle) {
        return getActiveCommMeanForProtocol(vehicle, null);
    }

    /**
     * Verifies if a specific protocol is supported by a vehicle
     * 
     * @param vehicleID The vehicle's identifier
     * @param protocol The protocol to search for
     * @return <b>true</b> if the protocol is present or <b>false</b> otherwise.
     */
    public static boolean isProtocolSupported(String vehicleID, String protocol) {
        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleID);
        if (vehicle == null)
            return false;

        if (protocol == null || protocol.equals("")) {
            return false;
        }

        for (CommMean cm : vehicle.getCommunicationMeans().values()) {
            if (cm.getProtocols().contains(protocol))
                return true;
        }

        return false;
    }

    /**
     * @param vehicle
     * @return
     */
    public static LinkedList<CommMean> getActiveCommMeanList(VehicleType vehicle) {
        LinkedHashMap<String, CommMean> listCommMeans = vehicle.getCommunicationMeans();
        if (listCommMeans.isEmpty()) {
            NeptusLog.pub().error("CommMean.getActiveCommMean :: " + "No CommMean for vehicle " + vehicle.getId());
            return null;
        }
        Iterator<CommMean> it = listCommMeans.values().iterator();
        LinkedList<CommMean> activeCM = new LinkedList<CommMean>();
        while (it.hasNext()) {
            CommMean tcm = (CommMean) it.next();
            boolean ret = testCommMeanForActivity(tcm);
            if (ret)
                activeCM.add(tcm);
        }
        if (activeCM.isEmpty()) {
            NeptusLog.pub().error(
                    "CommMean.getActiveCommMean :: " + "No active CommMean for vehicle " + vehicle.getId());
            return null;
        }
        else {
            return activeCM;
        }
    }

    /**
     * @param cm
     * @return
     */
    public static boolean testCommMeanForActivity(CommMean cm) {
        // FIXME Test CommMean for activity!!!
        return true;
    }

    /**
     * @param cm
     * @param protocol
     * @return
     */
    public static boolean testCommMeanForProtocol(CommMean cm, String protocol) {
        LinkedList<?> listProtocols = cm.getProtocols();
        if (listProtocols.isEmpty())
            return false;
        else if (listProtocols.contains(protocol)) {
            // FIXME Testar se o serviço está activo
            return true;
        }
        else
            return false;
    }

    private final static int[] crc16_table = new int[] { 0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280,
            0xC241, 0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1, 0xC481, 0x0440, 0xCC01, 0x0CC0, 0x0D80, 0xCD41,
            0x0F00, 0xCFC1, 0xCE81, 0x0E40, 0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841, 0xD801,
            0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40, 0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0,
            0x1C80, 0xDC41, 0x1400, 0xD4C1, 0xD581, 0x1540, 0xD701, 0x17C0, 0x1680, 0xD641, 0xD201, 0x12C0, 0x1380,
            0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040, 0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281, 0x3240,
            0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441, 0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01,
            0x3FC0, 0x3E80, 0xFE41, 0xFA01, 0x3AC0, 0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840, 0x2800, 0xE8C1,
            0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41, 0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00, 0xEDC1, 0xEC81,
            0x2C40, 0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640, 0x2200, 0xE2C1, 0xE381, 0x2340,
            0xE101, 0x21C0, 0x2080, 0xE041, 0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240, 0x6600,
            0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441, 0x6C00, 0xACC1, 0xAD81, 0x6D40, 0xAF01, 0x6FC0,
            0x6E80, 0xAE41, 0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840, 0x7800, 0xB8C1, 0xB981,
            0x7940, 0xBB01, 0x7BC0, 0x7A80, 0xBA41, 0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40,
            0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640, 0x7200, 0xB2C1, 0xB381, 0x7340, 0xB101,
            0x71C0, 0x7080, 0xB041, 0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241, 0x9601, 0x56C0,
            0x5780, 0x9741, 0x5500, 0x95C1, 0x9481, 0x5440, 0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81,
            0x5E40, 0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880, 0x9841, 0x8801, 0x48C0, 0x4980, 0x8941,
            0x4B00, 0x8BC1, 0x8A81, 0x4A40, 0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41, 0x4400,
            0x84C1, 0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641, 0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1,
            0x8081, 0x4040 };

    public static int computeCrc16(byte[] data, int startPos, long length, int curCrc) {
        int crc = curCrc;

        for (int i = startPos; i < length; i++) {
            crc = (crc >> 8) ^ crc16_table[(crc ^ data[i]) & 0xff];
            // NeptusLog.pub().info("<###>crc calc index:"+i+" data:" + data[i] +" crc calc=" + crc);
        }

        return crc;
    }

    public static int computeCrc16(byte[] data, int startPos, int curCrc) {

        int crc = curCrc;

        for (int i = startPos; i < data.length; i++) {
            crc = (crc >> 8) ^ crc16_table[(crc ^ data[i]) & 0xff];
            // NeptusLog.pub().info("<###>crc calc index:"+i+" data:" + data[i] +" crc calc=" + crc);
        }

        return crc;

    }

    // Workaround to list virtual ports correctly
    // Found in "http://forum.java.sun.com/thread.jspa?threadID=575580&messageID=2874228"
    public static Vector<CommPortIdentifier> enumerateComPorts() {
        try {
            Field masterIdList_Field = CommPortIdentifier.class.getDeclaredField("masterIdList");
            masterIdList_Field.setAccessible(true);
            masterIdList_Field.set(null, null);

            String temp_string = System.getProperty("java.home") + File.separator + "lib" + File.separator
                    + "javax.comm.properties";
            Method loadDriver_Method = CommPortIdentifier.class.getDeclaredMethod("loadDriver",
                    new Class[] { String.class });
            loadDriver_Method.setAccessible(true); // unprotect it
            loadDriver_Method.invoke(null, new Object[] { temp_string });
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e);
        }

        Enumeration<?> e = CommPortIdentifier.getPortIdentifiers();
        Vector<CommPortIdentifier> ret = new Vector<CommPortIdentifier>();

        while (e.hasMoreElements()) {
            ret.add((CommPortIdentifier) e.nextElement());
        }

        return ret;
    }

    /**
     * To parse a list of ports from a range or list. e.g. "52000,52001" or "52000-52003"
     * 
     * @param rangeString
     * @return
     */
    public static int[] parsePortRangeFromString(String rangeString, int[] defaultValue) {
        PortRangeValidator valid = new PortRangeValidator();
        if (valid.validate(rangeString) != null)
            return defaultValue;
        StringTokenizer tokens = new StringTokenizer(rangeString, " ,");
        if (tokens.countTokens() == 0)
            return defaultValue;
        HashSet<Integer> list = new LinkedHashSet<Integer>();
        while (tokens.hasMoreElements()) {
            String str = tokens.nextToken();
            if (str.indexOf('-') != -1) {
                String[] ra = str.split("-");
                if (ra.length != 2) {
                    continue;
                }
                else {
                    try {
                        int v0 = Integer.parseInt(ra[0]);
                        int v1 = Integer.parseInt(ra[1]);
                        if (v0 > v1) {
                            int t = v0;
                            v0 = v1;
                            v1 = t;
                        }
                        for (int j = 0; j <= Math.min(10, v1 - v0); j++) {
                            list.add(v0 + j);
                        }
                    }
                    catch (NumberFormatException e) {
                        NeptusLog.pub().error(e.getMessage());
                        return defaultValue;
                    }
                }
            }
            else {
                try {
                    int vi = Integer.parseInt(str);
                    list.add(vi);
                }
                catch (NumberFormatException e) {
                    NeptusLog.pub().error(e.getMessage());
                    return defaultValue;
                }
            }
        }
        Integer[] retI = list.toArray(new Integer[list.size()]);
        int[] ret = new int[Math.min(10, retI.length)];
        for (int i = 0; i < Math.min(10, retI.length); i++) {
            ret[i] = retI[i];
        }
        return ret.length == 0 ? defaultValue : ret;
    }

    public static void main(String[] args) throws Exception {

        // byte[] arr =
        // {(byte)0x4c,(byte)0xc4,(byte)0x03,(byte)0x00,(byte)0x79,(byte)0x00,(byte)0xe2,(byte)0xcb,(byte)0x9f,(byte)0x29,(byte)0x7b,(byte)0xae,(byte)0xd1,(byte)0x41,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x40,(byte)0x37,(byte)0x15,(byte)0x40,(byte)0x3f,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x80,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x60,(byte)0x26,(byte)0x21,(byte)0xa9,(byte)0x3f,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
        // };
        //
        // byte[] foo =
        // {(byte)0x4c,(byte)0xc4,(byte)0x03,(byte)0x00,(byte)0x79,(byte)0x00,(byte)0xe2,(byte)0xcb,(byte)0x9f,(byte)0x29,(byte)0x7b,(byte)0xae,(byte)0xd1,(byte)0x41,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x40,(byte)0x37,(byte)0x15,(byte)0x40,(byte)0x3f,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x80,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x60,(byte)0x26,(byte)0x21,(byte)0xa9,(byte)0x3f,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
        // (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
        // ,(byte)0x27, (byte)0x53};
        //
        //
        //
        // NeptusLog.pub().info("<###>CRC should be = 21287 and is " + computeCrc16(arr, 0, 0));
        //
        // IMCMessage msg = MessageFactory.parseMessage(foo);
        //
        // msg.dump(System.out);

        int[] list = parsePortRangeFromString("52000", new int[]{10});
        for (int i : list) {
            System.out.print(i + " ");
        }
        System.out.println();

        list = parsePortRangeFromString("52000, 52001", new int[]{10});
        for (int i : list) {
            System.out.print(i + " ");
        }
        System.out.println();

        list = parsePortRangeFromString("52000, 52001-52004,3", new int[]{10});
        for (int i : list) {
            System.out.print(i + " ");
        }
        System.out.println();

        list = parsePortRangeFromString("52000, 52004-52001,3", new int[]{10});
        for (int i : list) {
            System.out.print(i + " ");
        }
        System.out.println();

        list = parsePortRangeFromString("52000, 52004-52001,3e", new int[]{10});
        for (int i : list) {
            System.out.print(i + " ");
        }
        System.out.println();

        list = parsePortRangeFromString("52000, 52004-52101,3", new int[]{10});
        for (int i : list) {
            System.out.print(i + " ");
        }
        System.out.println();

    }

}
