/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 26, 2014
 */
package pt.lsts.neptus.plugins.cmdsenders;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MonitorIMCComms;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
public class AcousticModemSender implements ITextMsgSender {

    @Override
    public String getName() {
        return "Send acoustically";
    }

    @Override
    public boolean available(String vehicleId) {
        return ImcSystemsHolder.lookupSystemByService("acoustic/operation", SystemTypeEnum.ALL, true).length > 0;
    }

    @Override
    public Future<String> sendToVehicle(String source, String destination, String command) {
        TextMessage tm = new TextMessage();
        tm.setText(command);
        tm.setOrigin(source);
        final boolean result = IMCSendMessageUtils.sendMessageByAcousticModem(tm, destination, true,
                ImcSystemsHolder.lookupSystemByService("acoustic/operation", SystemTypeEnum.ALL, true));
        
        return new Future<String>() {
            @Override
            public String get() throws InterruptedException ,java.util.concurrent.ExecutionException {
                return result? "Sent" : "Error sending";
            };
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
            @Override
            public boolean isCancelled() {
                return false;
            }
            @Override
            public boolean isDone() {
                return true;
            }
            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                return result? "Sent" : "Error sending";
            }
        };
    }

    public static void main(String[] args) throws Exception {
        ImcMsgManager.getManager().start();
        MonitorIMCComms monitor = new MonitorIMCComms(ImcMsgManager.getManager());
        GuiUtils.testFrame(monitor);
        AcousticModemSender sender = new AcousticModemSender();
        for (int i = 0; i < 15; i++) {
            Thread.sleep(1000);            
        }
        try {
            System.out.println(sender.sendToVehicle("neptus", "lauv-xtreme-2", "yoyo").get());
            System.out.println(sender.sendToVehicle("neptus", "lauv-seacon-1", "yoyo").get());            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //System.exit(-1);
    }

}
