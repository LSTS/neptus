/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jun 28, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.IridiumTxStatus;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.messages.TypedMessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * @author zp
 * 
 */
@IridiumProvider(id = "imc", name = "IMC Iridium messenger", description = "Uses visible IMC systems capable "+
 "of sending Iridium messages and processes incoming Iridium messages transmitted via IMC")
public class DuneIridiumMessenger implements IridiumMessenger, MessageListener<MessageInfo, IMCMessage> {

    boolean available = false;

    protected int req_id = (int) (Math.random() * 65535);

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

    public DuneIridiumMessenger() {
        ImcMsgManager.getManager().addListener(this,
                new TypedMessageFilter(IridiumMsgRx.class.getSimpleName(), IridiumTxStatus.class.getSimpleName()));
    }

    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {
        if (msg.getMgid() == IridiumMsgRx.ID_STATIC) {
            try {
                IridiumMessage m = IridiumMessage.deserialize(msg.getRawData("data"));
                messagesReceived.add(m);
                NeptusLog.pub().info("Received a " + m.getClass().getSimpleName() + " from " + msg.getSourceName());
                for (IridiumMessageListener listener : listeners)
                    listener.messageReceived(m);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }

            IMCInputStream iis = new IMCInputStream(new ByteArrayInputStream(msg.getRawData("data")), IMCDefinition.getInstance());
            iis.setBigEndian(false);
            PlainTextMessage txtIridium = new PlainTextMessage();
            try {
                txtIridium.deserializeFields(iis);
                NeptusLog.pub().info("Received a plain text from " + msg.getSourceName());
                for (IMCMessage m : txtIridium.asImc()) {
                    ImcMsgManager.getManager().postInternalMessage("iridium txt", m);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        else if (msg.getMgid() == IridiumTxStatus.ID_STATIC) {
            // TODO
        }
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {

        ArrayList<String> providers = new ArrayList<String>();
        providers.addAll(getIridiumServiceProviders());
        
        if (providers.isEmpty()) {
            throw new Exception("No Iridium service providers are available");
        }
        
        providers.sort(Collections.reverseOrder());
        ImcSystem system = ImcSystemsHolder.lookupSystemByName(providers.iterator().next());
        
        System.out.println("Subscribed to Iridium Device Updates through "+system.getName());
        
        // Activate and deactivate subscriptions should use the id of the used gateway
        //if (msg instanceof ActivateSubscription || msg instanceof DeactivateSubscription) {
        //    ImcSystem system = ImcSystemsHolder.lookupSystemByName(messengerName);
        //    if (system != null)
        //        msg.setSource(system.getId().intValue());

        msg.setSource(system.getId().intValue());
        IridiumMsgTx tx = new IridiumMsgTx();
        tx.setReqId((++req_id % 65535));
        tx.setTtl(3600);
        tx.setData(msg.serialize());
        if (!ImcMsgManager.getManager().sendMessageToSystem(tx, system.getName()))
            throw new Exception("Error while sending message to " + system.getName() + " via IMC.");
    }
    
    public Collection<String> getIridiumServiceProviders() {
        ArrayList<String> names = new ArrayList<>();
        ImcSystem[] providers = ImcSystemsHolder.lookupSystemByService("iridium", SystemTypeEnum.ALL, true);
        
        if (providers != null)
            for (ImcSystem s : providers)
                names.add(s.getName());
        
        return names;
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {
        return new Vector<>();
    }

    @Override
    public String getName() {
        return "DUNE Iridium Messenger";
    }
    
    @Override
    public boolean isAvailable() {
        return !getIridiumServiceProviders().isEmpty();
    }

    @Override
    public void cleanup() {
        listeners.clear();
        messagesReceived.clear();
    }
    
    @Override
    public String toString() {
        return getName();                
    }

}
