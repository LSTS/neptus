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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import pt.lsts.imc.DevDataBinary;
import pt.lsts.imc.DevDataText;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Goto;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Loiter;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.ImcTcpTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.FileUtil;
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

        int req_id = 666;

        int portServer = 8888;// Integer.parseInt(args[0]);

        String server = "127.0.0.1";// args[1];
        int destServer = 8000;// Integer.parseInt(args[2]);

        ImcTcpTransport tcpT = new ImcTcpTransport(portServer, IMCDefinition.getInstance());

        tcpT.addListener(new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                // info.dump(System.out);
                // msg.dump(System.out);
                // System.err.println("Received "+msg.getAbbrev());
            }
        });

        MessageDeliveryListener mdlT = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliveryUnreacheable: " + message.getAbbrev());
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliveryTimeOut: " + message.getAbbrev());
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliverySuccess: " + message.getAbbrev());
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                NeptusLog.pub().info("<###>>>> deliveryError: " + message.getAbbrev() + " " + error);
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                NeptusLog.pub().info("<###>>>> deliveryUncertain: " + message.getAbbrev() + " " + msg);
            }
        };

        // build plan specification to send
        IMCMessage msg = new Heartbeat();
        ;
        IMCMessage msgES = new EstimatedState();
        PlanControl pc = new PlanControl();
        PlanSpecification ps = new PlanSpecification();
        PlanManeuver pm = new PlanManeuver();
        Goto goto_ = new Goto();
        goto_.setLat(LocationType.FEUP.getLatitudeRads());
        goto_.setLon(LocationType.FEUP.getLongitudeRads());
        goto_.setZ(20.0);
        goto_.setZUnitsStr("HEIGHT");
        Loiter loiter = new Loiter();
        loiter.setZ(20.0);
        loiter.setZUnitsStr("HEIGHT");
        loiter.setRadius(150.0);
        List<PlanManeuver> mans = new ArrayList<>();
        pm.setData(goto_);
        mans.add(pm);
        pm.setData(loiter);
        pc.setPlanId("plan" + req_id);
        ps.setPlanId("plan" + req_id);
        ps.setManeuvers(mans);
        pc.setRequestId(req_id);
        pc.setOp(OP.LOAD);
        pc.setArg(ps);
        pc.setSrc(0x3c22);
        pc.setDst(0x0c0c);
        msg.setSrc(0x3c22);
        msgES.setSrc(0x3c22);

        String last = pc.getPlanId();
        while (true) {
            // Send IMC Messages periodically
            for (int i = 1; i <= 2; i++) {
                DevDataBinary dataB = getDevBinaryMsg(i);
                dataB.setTimestampMillis(System.currentTimeMillis());
                dataB.setSrc(0x3c22);
                dataB.setDst(0x0c0c);
                msg.setTimestampMillis(System.currentTimeMillis());
                tcpT.sendMessage(server, destServer, dataB, mdlT);
            }
            DevDataText dataT = getDevTextMsg();
            dataT.setTimestampMillis(System.currentTimeMillis());
            dataT.setSrc(0x3c22);
            dataT.setDst(0x0c0c);
            msg.setTimestampMillis(System.currentTimeMillis());
            tcpT.sendMessage(server, destServer, dataT, mdlT);

            try {
                Thread.sleep(15_000);
            }
            catch (InterruptedException e1) {
            }
            /*
             * pc.setTimestampMillis(System.currentTimeMillis()); pc.setRequestId(req_id); if
             * (pc.getOp().equals(OP.LOAD)) { pc.setArg(null); pc.setOp(OP.START); pc.setPlanId(last);
             * sendIMCMsg(server, destServer, tcpT, mdlT, pc); }
             * 
             * else if (pc.getOp().equals(OP.START)) { pc.setArg(null); pc.setOp(OP.STOP); pc.setPlanId(last);
             * 
             * try { Thread.sleep(15_000); } catch (InterruptedException e1) { } sendIMCMsg(server, destServer, tcpT,
             * mdlT, pc); req_id++; pc.setRequestId(req_id); pc.setPlanId("plan"+req_id); ps.setPlanId("plan"+req_id); }
             * 
             * 
             * else if (pc.getOp().equals(OP.STOP)) { pc.setArg(ps); pc.setOp(OP.LOAD); pc.setPlanId(last);
             * 
             * try { Thread.sleep(15_000); } catch (InterruptedException e1) { } sendIMCMsg(server, destServer, tcpT,
             * mdlT, pc); }
             */
        }

    }

    /**
     * @param server
     * @param destServer
     * @param tcpT
     * @param mdlT
     * @param pc
     */
    private static void sendIMCMsg(String server, int destServer, ImcTcpTransport tcpT, MessageDeliveryListener mdlT,
            PlanControl pc) {
        System.err.println("SENT " + pc.getOpStr() + " with planID: " + pc.getPlanId());
        tcpT.sendMessage(server, destServer, pc, mdlT);
    }

    /**
     * Read Raster data from file
     */
    private static DevDataBinary getDevBinaryMsg(int j) {
        DevDataBinary data = new DevDataBinary();
        FileInputStream inputFile = null;
        try {
            inputFile = new FileInputStream("plugins-dev/fire-rs/pt/lsts/neptus/firers/test/rasterSample" + j);
            BufferedReader inputBuf = new BufferedReader(new InputStreamReader(inputFile));
            String inputStr = inputBuf.readLine();
            int len = inputStr.length();
            byte[] input = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                input[i / 2] = (byte) ((Character.digit(inputStr.charAt(i), 16) << 4)
                        + Character.digit(inputStr.charAt(i + 1), 16));
                data.setValue(input);
            }
            data.setValue(input);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * Read Raster data from file
     */
    private static DevDataText getDevTextMsg() {
        DevDataText data = new DevDataText();
        String input = FileUtil.getFileAsString("plugins-dev/fire-rs/pt/lsts/neptus/firers/test/contourLines");
        data.setValue(input);
        return data;
    }
}
