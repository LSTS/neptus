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
 * 06/03/2017
 */
package pt.lsts.neptus.firers.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;
import pt.lsts.neptus.comm.transports.tcp.TCPMessageListener;
import pt.lsts.neptus.comm.transports.tcp.TCPNotification;
import pt.lsts.neptus.comm.transports.tcp.TCPTransport;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 *
 */
public class ConnectionTest {

    private ConnectionTest() {
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void mainWriter(String[] args) throws Exception {
        int bindPort = 6001;
        String curTime = DateTimeUtil.dateTimeFileNameFormatter.format(new Date(System.currentTimeMillis()));
        File fx = new File("FireRs-raw-log-" + curTime + ".log");
        FileOutputStream fos = new FileOutputStream(fx);
        
        TCPTransport tcp = new TCPTransport(bindPort);
        if (tcp.isOnBindError()) {
            System.out.println("On bind error on port " + bindPort);
            System.exit(1);
        }
            
        TCPMessageListener listener = new TCPMessageListener() {
            @Override
            public void onTCPMessageNotification(TCPNotification req) {
                byte[] buf = req.getBuffer();
                try {
                    fos.write(buf);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        tcp.addListener(listener );
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shutting down ...");
                    //some cleaning up code...
                    fos.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        boolean exit = false;
        while (!exit) {
            try { Thread.sleep(200); } catch (Exception e) { }
            Thread.yield();
        }
    }
    
    public static void mainProducerTest(String[] args) throws Exception {
        int bindPort = 6009;
        String curTime = DateTimeUtil.dateTimeFileNameFormatter.format(new Date(System.currentTimeMillis()));

        TCPTransport tcp = new TCPTransport(bindPort);
        if (tcp.isOnBindError()) {
            System.out.println("On bind error on port " + bindPort);
            System.exit(1);
        }
            
        TCPMessageListener listener = new TCPMessageListener() {
            @Override
            public void onTCPMessageNotification(TCPNotification req) {
                ResultEnum res = req.getOperationResult();
                System.out.println("Producer test sent " + res);
            }
        };
        tcp.addListener(listener );
                
        boolean exit = false;
        while (!exit) {
            try { Thread.sleep(2000); } catch (Exception e) { }
            Thread.yield();
            tcp.sendMessage("127.0.0.1", 6001, curTime.getBytes());
        }
    }

    public static void main(String[] args) throws Exception {
        new Thread() {
            @Override
            public void run() {
                try {
                    mainWriter(null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                try {
//                    mainProducerTest(null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
