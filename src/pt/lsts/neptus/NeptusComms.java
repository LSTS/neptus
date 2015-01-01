/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Hugo Dias
 * Nov 7, 2012
 */
package pt.lsts.neptus;


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
