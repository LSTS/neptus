/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by guga
 * 2 de Nov de 2012
 * $Id::                                                                        $:
 */
package pt.up.fe.dceg.neptus.systems;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.junit.Test;

/**
 * @author guga
 * 
 */
public class SystemsManagerTest {

    /**
     * Test method for {@link pt.up.fe.dceg.neptus.systems.SystemsManager#buildSelf()}.
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
        System.out.println(sl2[0] + " " + sl2[1] + " " + sl2[2] + " " + sl2[3]);
    }

    @Test
    public void testLocalIp() {
        System.out.println(SystemsManagerTest.getLocalIP());
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
            return "";
        }
        catch (UnknownHostException e) {
            return "";
        }

    }
}
