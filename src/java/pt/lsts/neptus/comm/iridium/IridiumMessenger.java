/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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

import java.util.Collection;
import java.util.Date;

/**
 * @author zp
 *
 */
public interface IridiumMessenger {

    /**
     * Send this message across
     */
    public void sendMessage(IridiumMessage msg) throws Exception;

    /**
     * Send this message across
     *
     * @param destinationName The name of the destination
     *                        (e.g. the name of the vehicle that should receive the message)
     * @param destinationAddr The address of the destination, this depends on the messenger
     *                        (e.g. the IMC address of the vehicle that should receive the message,
     *                        or the imei of the Iridium device that should receive the message)
     *                        This can be empty or null, the messenger will try its best to find the
     *                        missing information.
     * @param data            The data to be sent
     * @return
     */
    public void sendMessageRaw(String destinationName, String destinationAddr, byte[] data) throws Exception;

    /**
     * Retrieve any messages that were received since given time
     * It is expected that the messenger sends a IridiumManager to send an IridiumMsgTx message to the internal bus
     */
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception;
    
    /**
     * Is this messenger able to receive / send messages 
     */
    public boolean isAvailable();
    
    /**
     * Retrieve messenger's name
     */
    public String getName();
    
    /**
     * Add listener that will be notified when new messages are received
     */
    public void addListener(IridiumMessageListener listener);
    
    /** 
     * Remove existing message listener
     */
    public void removeListener(IridiumMessageListener listener);

    /**
     * Method that is called when messengers are removed
     */
    public void cleanup();    
}
