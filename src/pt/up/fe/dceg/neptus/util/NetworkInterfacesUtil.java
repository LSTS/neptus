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
 * 26 de Out de 2010
 */
package pt.up.fe.dceg.neptus.util;

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
		} catch (Exception e) {
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
				//System.out.println("      "+"      "+ia.getAddress().getHostAddress()+" "+(ia.getBroadcast()!=null?ia.getBroadcast().getHostAddress():""));
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
					System.out.println(">>>>  "+ni);
					System.out.println("      isLoopback "+ni.isLoopback());
					System.out.println("      isPointToPoint "+ni.isPointToPoint());
					System.out.println("      multicast "+ni.supportsMulticast());
					System.out.println("      virtual "+ni.isVirtual());
					System.out.println("      mtu "+ni.getMTU());
					System.out.println("      n addresses "+ni.getInterfaceAddresses().size());
					Enumeration<InetAddress> iadde = ni.getInetAddresses();
					while (iadde.hasMoreElements()) {
						InetAddress inetAddress = (InetAddress) iadde
								.nextElement();
						System.out.println(":      "+"      "+inetAddress.getHostAddress());
					}
					System.out.println("      InterfaceAddresses:"+ni.getInterfaceAddresses());
					for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        System.out.println("      " + "      Host:" + ia.getAddress().getHostAddress() + " Broadcast:"
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
			System.out.println(ii.isReachable(2000));
			ii = new  InetSocketAddress("192.168.56.2", 6002).getAddress();
			System.out.println(ii.isReachable(2000));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

