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
 * Feb 26, 2014
 */
package pt.lsts.neptus.plugins.cmdsenders;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import pt.lsts.imc.Abort;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;

/**
 * @author zp
 * 
 */
public class WiFiSender implements ITextMsgSender {

    @Override
    public String getName() {
        return "Send via Wi-Fi";
    }

    @Override
    public boolean available(String vehicleId) {
        return ImcMsgManager.getManager().getCommInfoById(vehicleId) != null
                && ImcMsgManager.getManager().getCommInfoById(vehicleId).isActive();
    }

    @Override
    public Future<String> sendToVehicle(String source, String destination, String command) {
        TextMessage tm = new TextMessage();
        tm.setText(command);
        tm.setOrigin(source);
        final Future<SendResult> res = ImcMsgManager.getManager().sendMessageReliably(tm, destination);

        return new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return res.get().toString();
            }
        });
    }

    public static void main(String[] args) throws Exception {
        ImcMsgManager.getManager().start();
        
        WiFiSender sender = new WiFiSender();
        for (int i = 0; i < 15; i++) {
            Thread.sleep(1000);
            System.out.println(sender.available("lauv-xtreme-2"));
            ImcMsgManager.getManager().sendMessageToVehicle(new Heartbeat(), "lauv-xtreme-2", null);
        }
        try {
            System.out.println(ImcMsgManager.getManager().sendMessageReliably(new Abort(), "lauv-xtreme-2").get(10, TimeUnit.SECONDS));
            System.out.println(ImcMsgManager.getManager().sendMessageReliably(new Abort(), "lauv-seacon-2").get(10, TimeUnit.SECONDS));
            System.out.println(ImcMsgManager.getManager().sendMessageReliably(new Abort(), "lauv-xtreme-3").get(10, TimeUnit.SECONDS));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(-1);
    }

}
