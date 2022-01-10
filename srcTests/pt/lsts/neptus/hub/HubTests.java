/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 13, 2014
 */
package pt.lsts.neptus.hub;

import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.comm.iridium.ActivateSubscription;
import pt.lsts.neptus.comm.iridium.DeactivateSubscription;
import pt.lsts.neptus.comm.iridium.DeviceUpdate;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author zp
 *
 */
public class HubTests {

    @Test
    public void messagePollingTest() throws Exception {
        HubIridiumMessenger messenger = new HubIridiumMessenger();
        Collection<IridiumMessage> msgs1 = messenger.pollMessages(new Date(System.currentTimeMillis() - 3600 * 1000));
        ImcIridiumMessage msg = new ImcIridiumMessage();
        msg.setMsg(new TextMessage("HubTest", "This is a test message"));
        messenger.sendMessage(msg);
        Collection<IridiumMessage> msgs2 = messenger.pollMessages(new Date(System.currentTimeMillis() - 3600 * 1000));
        Assert.assertEquals("New message is visible when polling", 1, msgs2.size() - msgs1.size()); 
    }
    
    @Test
    public void deviceUpdatesTest() throws Exception {
        HubIridiumMessenger messenger = new HubIridiumMessenger();
        DeviceUpdate updates = messenger.pollActiveDevices();
        System.out.println(updates.getPositions());
    }
    
    @Test
    public void updatesSubscriptionTest() throws Exception {
        HubIridiumMessenger messenger = new HubIridiumMessenger();
        messenger.sendMessage(new ActivateSubscription());
        Thread.sleep(1000);
        messenger.sendMessage(new DeactivateSubscription());
    }
    
    @Test
    public void sendCommand() throws Exception {
        HubIridiumMessenger messenger = new HubIridiumMessenger();
        IridiumCommand cmd = new IridiumCommand();
        cmd.setCommand("This is a test command");
        cmd.setDestination(VehiclesHolder.getVehicleById("lauv-xtreme-2").getImcId().intValue());
        messenger.sendMessage(cmd);               
    }
    
    
}
