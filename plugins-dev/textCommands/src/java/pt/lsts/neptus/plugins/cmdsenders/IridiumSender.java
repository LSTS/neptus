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
 * Feb 27, 2014
 */
package pt.lsts.neptus.plugins.cmdsenders;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
public class IridiumSender implements ITextMsgSender {

    @Override
    public String getName() {
        return "Send via Iridium";
    }

    @Override
    public boolean available(String vehicleId) {
        return IridiumManager.getManager().isAvailable();
    }

    @Override
    public Future<String> sendToVehicle(String source, final String destination, String command) {
        final IridiumCommand cmd = new IridiumCommand();
        cmd.setCommand(command);
        cmd.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        ImcSystem dest = ImcSystemsHolder.getSystemWithName(destination);
        if (dest != null)
            cmd.setDestination(dest.getId().intValue());
        else
            cmd.setDestination(65535);
       
        return new Future<String>() {
            
            String result = "Sending to Iridium";
            boolean complete = false;
            
            {
                try {
                    IridiumManager.getManager().send(cmd);
                    result = "Send to Iridium ok";
                }
                catch (Exception e) {
                    e.printStackTrace();
                    result = e.getClass().getSimpleName()+": "+e.getMessage();
                }
                complete = true;
            }
            
             
            
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }
            
            @Override
            public String get() throws InterruptedException, ExecutionException {
                return result;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                return result;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return complete;
            }                
        };
    }
    
    private static long lastSuccess = -1;
    
    public static Future<Boolean> rockBlockIsReachable() {
        return new Future<Boolean>() {
            Boolean result = null;
            boolean canceled = false;
            long start = System.currentTimeMillis();
            {
                
                if (System.currentTimeMillis() - lastSuccess < 15000) {
                    result = true;            
                }
                
                try {
                    URL url = new URL("https://secure.rock7mobile.com/rockblock");
                    int len = url.openConnection().getContentLength();
                    if (len > 0)
                        lastSuccess = System.currentTimeMillis();
                    result = len > 0;
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    result = false;
                }        
            }
            @Override
            public Boolean get() throws InterruptedException, ExecutionException {
                while (result == null) {
                    Thread.sleep(100);                    
                }
                return result;
            }
            
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                canceled = true;
                return false;
            }
            
            @Override
            public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                while (result == null) {
                    Thread.sleep(100);      
                    if (System.currentTimeMillis() - start > unit.toMillis(timeout))
                        throw new TimeoutException("Time out while connecting");
                }
                return result;
            }
            
            @Override
            public boolean isCancelled() {
                return canceled;
            }
            
            @Override
            public boolean isDone() {
                return result != null;
            }
        };        
    }

    public static void main(String[] args) throws Exception {
        IridiumSender sender = new IridiumSender();
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));        
    }
}
