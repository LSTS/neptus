/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 04/12/2017
 */
package pt.lsts.neptus.firers.test;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.ImcTcpTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
public class TcpImcTest {
    /**
     * @param args
     * @throws MiddlewareException 
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        ConfigFetch.initialize();

        int portServer = Integer.parseInt(args[0]);

        String server = args[1];
        int destServer = Integer.parseInt(args[2]);

        ImcTcpTransport tcpT = new ImcTcpTransport(portServer, IMCDefinition.getInstance());
        
        tcpT.addListener(new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                info.dump(System.out);
                msg.dump(System.out);
            }
        });

        MessageDeliveryListener mdlT = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliveryUnreacheable: "+ message.getAbbrev());
            }
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliveryTimeOut: "+ message.getAbbrev());
            }
            @Override
            public void deliverySuccess(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliverySuccess: "+ message.getAbbrev());
            }
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                NeptusLog.pub().info("<###>>>> deliveryError: "+ message.getAbbrev() + " " + error);
            }
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                NeptusLog.pub().info("<###>>>> deliveryUncertain: "+ message.getAbbrev() + " " + msg);                
            }
        };

        IMCMessage msg = new Heartbeat();;
        IMCMessage msgES = new EstimatedState();
     
        msg.setSrc(0x3c22);
        msgES.setSrc(0x3c22);
        
        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT.sendMessage(server, destServer, msg, mdlT);
        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT.sendMessage(server, destServer, msgES, mdlT);
        
        while (true) {
            msg.setTimestampMillis(System.currentTimeMillis());
            tcpT.sendMessage(server, destServer, msg, mdlT);
            try { Thread.sleep(1000); } catch (InterruptedException e1) { }
        }
    }
}
