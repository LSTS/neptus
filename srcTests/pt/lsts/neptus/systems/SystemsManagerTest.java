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
 * Author: guga
 * 2 de Nov de 2012
 */
package pt.lsts.neptus.systems;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.junit.Test;

import pt.lsts.neptus.NeptusLog;

/**
 * @author guga
 * 
 */
public class SystemsManagerTest {

    /**
     * Test method for {@link pt.lsts.neptus.systems.SystemsManager#buildSelf()}.
     */
    @Test
    public void testBuildSelf() {

        String hostadr = "";
        System.getProperty("os.name");
        if (true) {
            // if (osName.toLowerCase().indexOf("linux") != -1) {
            try {
                Enumeration<NetworkInterface> netInt = NetworkInterface.getNetworkInterfaces();
                while (netInt.hasMoreElements()) {
                    NetworkInterface ni = netInt.nextElement();
                    Enumeration<InetAddress> iAddress = ni.getInetAddresses();
                    while (iAddress.hasMoreElements()) {
                        InetAddress ia = iAddress.nextElement();
                        if (!ia.isLoopbackAddress()) {
                            if (ia instanceof Inet4Address) {
                                hostadr = ia.getHostAddress();
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        String[] sl2 = hostadr.split("\\.");
        NeptusLog.pub().info("<###> "+sl2[0] + " " + sl2[1] + " " + sl2[2] + " " + sl2[3]);
    }

    @Test
    public void testLocalIp() {
        NeptusLog.pub().info("<###> "+SystemsManagerTest.getLocalIP());
    }

    public static String getLocalIP() {
        String ip = "";
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
                NetworkInterface ni = NetworkInterface.getByName("eth0");
                if (ni != null) {
                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {
                        InetAddress add = (InetAddress) ias.nextElement();
                        if (add instanceof Inet4Address && !add.isLinkLocalAddress() && add != null) {
                            return add.getHostAddress();
                        }
                    }
                }

                if (ip.isEmpty()) {
                    Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
                    while (nifs.hasMoreElements()) {
                        NetworkInterface nif = nifs.nextElement();
                        if (!nif.isLoopback() && nif.isUp() && !nif.isVirtual()) {
                            Enumeration<InetAddress> adrs = nif.getInetAddresses();
                            while (adrs.hasMoreElements()) {
                                InetAddress adr = adrs.nextElement();
                                if (adr instanceof Inet4Address && adr != null && !adr.isLoopbackAddress()
                                        && !adr.isLinkLocalAddress()) {
                                    return adr.getHostAddress();
                                }
                            }
                        }
                    }
                }
                return ip;
            }
            else {
                return InetAddress.getLocalHost().getHostAddress();
            }
        }
        catch (SocketException e) {
            e.printStackTrace();
            return "";
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return "";
        }

    }
}
