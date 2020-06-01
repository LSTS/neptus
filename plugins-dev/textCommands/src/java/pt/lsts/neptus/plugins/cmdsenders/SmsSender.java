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
 * Feb 27, 2014
 */
package pt.lsts.neptus.plugins.cmdsenders;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.lsts.imc.Sms;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MonitorIMCComms;
import pt.lsts.neptus.types.comm.protocol.GsmArgs;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SmsSender implements ITextMsgSender {

    @Override
    public String getName() {
        return "Send via SMS";
    }

    @Override
    public boolean available(String vehicleId) {
        
        try {
            VehicleType vt = VehiclesHolder.getVehicleById(vehicleId);
            GsmArgs gsmArgs = (GsmArgs) vt.getProtocolsArgs().get("gsm");
            ImcSystem[] systems = ImcSystemsHolder.lookupSystemByService("sms", SystemTypeEnum.ALL, true);
            if (systems.length == 0)
                throw new Exception("No visible system is capable of sending SMS messages");
            return gsmArgs != null;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    @Override
    public Future<String> sendToVehicle(String source, final String destination, String command) {
        try {
            VehicleType vt = VehiclesHolder.getVehicleById(destination);
            GsmArgs gsmArgs = (GsmArgs) vt.getProtocolsArgs().get("gsm");
            if (gsmArgs == null)
                throw new Exception("No known GSM number exists for this destination");
            
            ImcSystem[] systems = ImcSystemsHolder.lookupSystemByService("sms", SystemTypeEnum.ALL, true);
            if (systems.length == 0)
                throw new Exception("No visible system is capable of sending SMS messages");
            
            final String sender = systems[0].getName();
            
            System.out.println("Sending SMS to "+destination+" via "+sender);
            
            final Sms sms = new Sms(gsmArgs.getNumber(), 120, command);
            
            return new Future<String>() {
                
                Future<SendResult> result = ImcMsgManager.getManager().sendMessageReliably(sms, sender);
                
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    return result.cancel(mayInterruptIfRunning);
                }
                
                @Override
                public String get() throws InterruptedException, ExecutionException {
                    return result.get().toString();
                }

                @Override
                public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                        TimeoutException {
                    return result.get(timeout, unit).toString();
                }

                @Override
                public boolean isCancelled() {
                    return result.isCancelled();
                }

                @Override
                public boolean isDone() {
                    return result.isDone();
                }                
            };
        }
        catch (final Exception e) {
            e.printStackTrace();
            FutureTask<String> ft = new FutureTask<>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return "Error: "+e.getMessage();
                }
            });
            ft.run();
            return ft;
        }
    }
    
    public static void main(String[] args) throws Exception {
        ImcMsgManager.getManager().start();
        MonitorIMCComms monitor = new MonitorIMCComms(ImcMsgManager.getManager());
        GuiUtils.testFrame(monitor);
        SmsSender sender = new SmsSender();
        for (int i = 0; i < 15; i++) {
            Thread.sleep(1000);            
        }
        try {
            System.out.println(sender.sendToVehicle("neptus", "lauv-xtreme-2", "test").get());
            System.out.println(sender.sendToVehicle("neptus", "lauv-seacon-1", "test").get());            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        //System.exit(-1);
    }

}
