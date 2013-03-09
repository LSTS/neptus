/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Nov 7, 2012
 */
package pt.up.fe.dceg.neptus;


/**
 * @author Hugo
 * 
 */
public class NeptusComms {
//    private boolean multicastEnabled;
//    private String multicastAddress;
//    private int[] multicastPorts = new int[] { 6969 };
//    private boolean broadcastEnabled;
//    private IMCDefinition imcDefinition;
    
    // Transports
//    private ImcUdpTransport multicastUdpTransport = null;
//
//    public NeptusComms(IMCDefinition imcDefinition) {
//        this.imcDefinition = imcDefinition;
//        this.broadcastEnabled = GeneralPreferences.getPropertyBoolean(GeneralPreferences.IMC_BROADCAST_ENABLE, false);
//        this.multicastEnabled = GeneralPreferences.getPropertyBoolean(GeneralPreferences.IMC_MULTICAST_ENABLE, false);
//        this.multicastAddress = GeneralPreferences.getProperty(GeneralPreferences.IMC_MULTICAST_ADDRESS);
//        this.multicastPorts = CommUtil.parsePortRangeFromString(
//                GeneralPreferences.getProperty(GeneralPreferences.IMC_MULTICAST_PORT), new int[] { 6969 });
//    }
//
//    public NeptusComms start() {
//
//        return this;
//    }
//
//    private void createMulticastUDPTransport() {
//        if (!(multicastEnabled || broadcastEnabled)) {
//            throw new IllegalArgumentException(
//                    "tried to create multicast udp transport but multicast or broadcast are disable in the preferences");
//        }
//        multicastUdpTransport = new ImcUdpTransport(multicastPorts[0], multicastAddress, imcDefinition);
//    }
}
