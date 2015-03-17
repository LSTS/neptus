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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jul 3, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@IridiumProvider(id = "sim", name = "Simulated Messenger", description = "This messenger posts the Iridium message "
        + "directly in the bus of the destination via IMC. Used only for debug / simulation purposes")
public class SimulatedMessenger implements IridiumMessenger {

    protected Vector<IridiumMessage> messagesReceived = new Vector<>();

    protected HashSet<IridiumMessageListener> listeners = new HashSet<>();

    @Override
    public void addListener(IridiumMessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IridiumMessageListener listener) {
        listeners.remove(listener);
    }

    @Subscribe
    public void on(IridiumMsgTx tx) {
        try {
            IridiumMessage m = IridiumMessage.deserialize(tx.getData());
            for (IridiumMessageListener listener : listeners)
                listener.messageReceived(m);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
        IridiumMsgRx rx = new IridiumMsgRx();
        rx.setOrigin("Iridium simulated messenger");
        rx.setDst(msg.getDestination());
        rx.setSrc(msg.getSource());
        rx.setData(msg.serialize());
        rx.setHtime(msg.timestampMillis / 1000.0);
        ImcMsgManager.getManager().sendMessage(rx);
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {
        return new Vector<>();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getName() {
        return "Simulated messenger";
    }
    
    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void cleanup() {
        listeners.clear();
        messagesReceived.clear();
    }
}
