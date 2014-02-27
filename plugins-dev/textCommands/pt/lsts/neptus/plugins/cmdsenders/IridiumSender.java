/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 27, 2014
 */
package pt.lsts.neptus.plugins.cmdsenders;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.comm.protocol.IridiumArgs;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;

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
        Future<Boolean> isRockBlockReachable = rockBlockIsReachable();
        
        try {
            VehicleType vt = VehiclesHolder.getVehicleById(vehicleId);
            IridiumArgs args = (IridiumArgs) vt.getProtocolsArgs().get("iridium");
            ImcSystem[] iridiumSenders = ImcSystemsHolder.lookupSystemByService("iridium", SystemTypeEnum.ALL, true);
            return args != null && (isRockBlockReachable.get() || iridiumSenders.length > 0);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    @Override
    public Future<String> sendToVehicle(String source, final String destination, String command) {
        //FIXME
        return null;
        
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
    
    private static int req_id = (int)(System.currentTimeMillis() / 1000) % 255;
    
    public static String sendViaIridiumGateway(String destination, byte[] data) {
        IridiumMsgTx tx = new IridiumMsgTx(req_id, 6000, destination, data);
        req_id = (req_id+1)%255;
        ImcSystem[] iridiumSenders = ImcSystemsHolder.lookupSystemByService("iridium", SystemTypeEnum.ALL, true);
        if (iridiumSenders.length == 0)
            return "Error: There is no available Iridium gateway";
        
        Future<SendResult> result = ImcMsgManager.getManager().sendMessageReliably(tx, iridiumSenders[0].getName());
        try {
            return "Message sent via "+iridiumSenders[0].getName()+": "+result.get();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return "Error sending message via "+iridiumSenders[0].getName()+": "+e.getMessage();
        }
    }
    
    public static String sendToRockBlockHttp(String destImei, String username, String password, byte[] data) throws HttpException, IOException{
        
        HttpClient client = new HttpClient();
        HttpMethod post = new PostMethod("https://secure.rock7mobile.com/rockblock/MT");
        post.getParams().setParameter("imei", destImei);
        post.getParams().setParameter("username", username);
        post.getParams().setParameter("password", password);
        post.getParams().setParameter("data", ByteUtil.encodeToHex(data));
        try {
            client.executeMethod(post);
        }
        catch (Exception e) {
            return "Error: "+e.getClass().getSimpleName()+": "+e.getMessage();
        }
        return post.getResponseBodyAsString();
    }
    
    public static void main(String[] args) throws Exception {
        IridiumSender sender = new IridiumSender();
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sender.available("lauv-xtreme-2"));
        System.out.println(sendToRockBlockHttp("234234234", "a", "b", new byte[] {100, 100, 100, 100, 0, 0, 0, 0, 0,0,100, -34, 89, 124}));
    }
}
