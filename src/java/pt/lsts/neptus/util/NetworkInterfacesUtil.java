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
 * 26 de Out de 2010
 */
package pt.lsts.neptus.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias
 *
 */
public class NetworkInterfacesUtil {

	public static final boolean testForReachability(String host) {
		return testForReachability(host, 6002);
	}

	public static final boolean testForReachability(String host, int port) {
	    InetAddress ii = new  InetSocketAddress(host, port).getAddress();
	    boolean result = false;
	    try {
	        result = ii.isReachable(2000);
	    } catch (IOException e) {
//	        e.printStackTrace();
	        result = false;
	    }
	    if (!result) {
	       try {
            Socket sock = new Socket(host, port);
               result = sock.isConnected();
               if (result)
                   sock.close();
	       }
	       catch (Exception e) {
//	           e.printStackTrace();
	       }
	    }
	    return result;
	}

	/**
	 * BE CAREFUL this has a high cost if you call it very often. 
	 * @return the active network interfaces
	 */
	public static Vector<NInterface> getNetworkInterfaces() {
		Vector<NInterface> list = new Vector<NetworkInterfacesUtil.NInterface>();
		try {
			Enumeration<NetworkInterface> nintf = NetworkInterface.getNetworkInterfaces();
			while (nintf.hasMoreElements()) {
				NetworkInterface ni = nintf.nextElement();
				if (ni.isUp()) {
					list.add(new NInterface(ni));
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		catch (Error e) {
		    e.printStackTrace();
		}
		return list;
	}
	
	public static class NInterface {
		private String name = "";
		private String displayName = "";
		private boolean loopback = false;
		private boolean pointToPoint = false;
		private boolean supportsMulticast = false;
		private boolean supportsBroadcast = false;
		private boolean virtual = false;
		private int mtu = -1;
		private Vector<Inet4Address> addresses = new Vector<Inet4Address>();
		private Vector<Inet4Address> broadcastAddresses = new Vector<Inet4Address>();
		private Vector<Inet6Address> addresses6 = new Vector<Inet6Address>();
		private String stringDesc;
		
		public NInterface(NetworkInterface ni) {
			name = ni.getName();
			displayName = ni.getDisplayName();
			try {
				loopback = ni.isLoopback();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			try {
				pointToPoint = ni.isPointToPoint();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			try {
				supportsMulticast = ni.supportsMulticast();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			virtual = isVirtual();
			try {
				mtu = ni.getMTU();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			stringDesc = ni.toString();
			for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
				//NeptusLog.pub().info("<###>      "+"      "+ia.getAddress().getHostAddress()+" "+(ia.getBroadcast()!=null?ia.getBroadcast().getHostAddress():""));
			    try {
			        Inet4Address address = (Inet4Address) ia.getAddress();
			        Inet4Address broadcastAddress = (Inet4Address) ia.getBroadcast();
			        if (address != null) {
			            addresses.add(address);
			            broadcastAddresses.add(broadcastAddress);
			            supportsBroadcast = supportsBroadcast || ((broadcastAddress != null)?true:false);
			        }
			        continue;
			    } catch (Exception e) {
			    }
			    try {
			        Inet6Address address6 = (Inet6Address) ia.getAddress();
			        if (address6 != null)
			            addresses6.add(address6);
			        continue;
			    } catch (Exception e) {
			    }
			}
		}
		
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @return the displayName
		 */
		public String getDisplayName() {
			return displayName;
		}
		/**
		 * @return the loopback
		 */
		public boolean isLoopback() {
			return loopback;
		}
		/**
		 * @return the pointToPoint
		 */
		public boolean isPointToPoint() {
			return pointToPoint;
		}
		/**
		 * @return the supportsMulticast
		 */
		public boolean supportsMulticast() {
			return supportsMulticast;
		}
		/**
		 * @return the supportsBroadcast
		 */
		public boolean supportsBroadcast() {
			return supportsBroadcast;
		}
		/**
		 * @return the virtual
		 */
		public boolean isVirtual() {
			return virtual;
		}
		/**
		 * @return the mtu
		 */
		public int getMtu() {
			return mtu;
		}
		/**
		 * @return the address
		 */
		public Inet4Address[] getAddress() {
			return addresses.toArray(new Inet4Address[addresses.size()]);
		}
		/**
		 * @return the broadcastAddress
		 */
		public Inet4Address[] getBroadcastAddress() {
			return broadcastAddresses.toArray(new Inet4Address[broadcastAddresses.size()]);
		}
		/**
		 * @return the address6
		 */
		public Inet6Address[] getAddress6() {
			return addresses6.toArray(new Inet6Address[addresses6.size()]);
		}
		@Override
		public String toString() {
			return stringDesc;
		}
		/**
		 * 
		 */
		public boolean hasIpv4Address() {
			return (addresses.size() > 0)?true:false;
		}
		/**
		 * 
		 */
		public boolean hasIpv6Address() {
			return (addresses6.size() > 0)?true:false;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Enumeration<NetworkInterface> nintf = java.net.NetworkInterface.getNetworkInterfaces();
			while (nintf.hasMoreElements()) {
				NetworkInterface ni = nintf.nextElement();
				if (ni.isUp()) {
					NeptusLog.pub().info("<###>>>>>  "+ni);
					NeptusLog.pub().info("<###>      isLoopback "+ni.isLoopback());
					NeptusLog.pub().info("<###>      isPointToPoint "+ni.isPointToPoint());
					NeptusLog.pub().info("<###>      multicast "+ni.supportsMulticast());
					NeptusLog.pub().info("<###>      virtual "+ni.isVirtual());
					NeptusLog.pub().info("<###>      mtu "+ni.getMTU());
					NeptusLog.pub().info("<###>      n addresses "+ni.getInterfaceAddresses().size());
					Enumeration<InetAddress> iadde = ni.getInetAddresses();
					while (iadde.hasMoreElements()) {
						InetAddress inetAddress = (InetAddress) iadde
								.nextElement();
						NeptusLog.pub().info("<###>:      "+"      "+inetAddress.getHostAddress());
					}
					NeptusLog.pub().info("<###>      InterfaceAddresses:"+ni.getInterfaceAddresses());
					for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        NeptusLog.pub().info("<###>      " + "      Host:" + ia.getAddress().getHostAddress() + " Broadcast:"
                                + (ia.getBroadcast() != null ? ia.getBroadcast().getHostAddress() : "")
                                + " NetworkPrefixLength:" + ia.getNetworkPrefixLength());
					}
				}
			}
		} 
		catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		InetAddress ii = new  InetSocketAddress("192.168.106.30", 6002).getAddress();
		try {
			NeptusLog.pub().info("<###> "+ii.isReachable(2000));
			ii = new  InetSocketAddress("192.168.56.2", 6002).getAddress();
			NeptusLog.pub().info("<###> "+ii.isReachable(2000));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

