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
 * Oct 20, 2015
 */
package org.necsave;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import info.necsave.msgs.ActionStop;
import info.necsave.msgs.Header.MEDIUM;
import info.necsave.msgs.PlatformInfo;
import info.necsave.proto.Message;
import info.necsave.proto.ProtoDefinition;
import info.necsave.proto.ProtoInputStream;
import info.necsave.proto.ProtoOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;

/**
 * @author zp
 *
 */
public class NecsaveTransport {

    @NeptusProperty(description="Port where platforms broadcast their state")
    public int broadcastPort = 17650;

    private DatagramSocket serverSocket = null;
    boolean stopped = false;
    private ConsoleLayout console;
    private byte[] receiveData = new byte[64 * 1024];
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    private LinkedHashMap<Integer, InetSocketAddress> platformAddrs = new LinkedHashMap<>();


    public NecsaveTransport(ConsoleLayout console) throws Exception {
        this.console = console;
        boolean bound = false;
        for (int port = broadcastPort; port < broadcastPort+3; port++) {
            try {
                serverSocket = new DatagramSocket(null);
                serverSocket.setReuseAddress(true);
                serverSocket.setBroadcast(true);
                serverSocket.bind(new InetSocketAddress(port));
                NeptusLog.pub().info(I18n.textf("Bound to port %d", port));
                bound = true;
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                NeptusLog.pub()
                .error(I18n.textf("Unable to bind to port %port: %error",  port, e.getMessage()));
            }
        }
        if (!bound)
            throw new RuntimeException("Unable to bind to any broadcast port.");
            
        receiverThread.setDaemon(true);
        receiverThread.start();        
    }

    private Thread receiverThread = new Thread("NMP Receiver") {
        public void run() {
            while (!stopped) {
                try {
                    Message msg = readMessage();
                    
                    if (console != null)
                        console.post(msg);                    
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        };
    };

    private void process(PlatformInfo msg, String host, int port) {
        if (msg.getPlatformId() != msg.getSrc())
            return;
        platformNames.put(msg.getPlatformId(), msg.getPlatformName());
        platformAddrs.put(msg.getPlatformId(), new InetSocketAddress(host, msg.getPort()));
    }

    private Message readMessage() throws Exception {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);

        ProtoInputStream pis = new ProtoInputStream(new ByteArrayInputStream(receiveData),
                ProtoDefinition.getInstance());

        Message msg = ProtoDefinition.getInstance().nextMessage(pis);

        if (msg instanceof PlatformInfo)
            process((PlatformInfo)msg, receivePacket.getAddress().getHostAddress(), receivePacket.getPort());        

        NeptusLog.pub().debug("Received message of type '" + msg.getAbbrev() + "' from " + receivePacket.getAddress()
                + " - Platform " + platformNames.get(msg.getSrc()));
        
        return msg;
    }   

    public void broadcast(Message msg) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ProtoOutputStream pos = new ProtoOutputStream(baos);
        msg.setMedium(MEDIUM.IP_BROADCAST);
        int length = msg.serialize(pos);
        DatagramPacket packet = new DatagramPacket(baos.toByteArray(), length);
        for (int i = 0; i < 3; i++)
            packet.setSocketAddress(new InetSocketAddress("255.255.255.255", broadcastPort+i));
        serverSocket.send(packet);
    }

    public Future<Boolean> sendMessage(final Message msg, final int platf) {

        if (platformAddrs.containsKey(platf)) {
            InetSocketAddress addr = platformAddrs.get(platf);
            return sendMessage(msg, addr.getHostName(), addr.getPort());
        }
        else
            return ConcurrentUtils.constantFuture(Boolean.FALSE);        
    }

    public Future<Boolean> sendMessage(final Message msg, final String platform) {
        int platf = -1;
        if (platformNames.containsValue(platform)) {
            for (Entry<Integer, String> e : platformNames.entrySet()) {
                if (e.getValue().equals(platform)) {
                    platf = e.getKey();
                    break;
                }
            }
        }
        return sendMessage(msg, platf);   
    }

    public Future<Boolean> sendMessage(final Message msg, final String host, final int port) {
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                msg.setMedium(MEDIUM.IP_RELIABLE);
                try {
                    Socket socket = new Socket(host, port);
                    msg.serialize(new ProtoOutputStream(socket.getOutputStream()));
                    socket.close();
                    return true;
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    return false;
                }
            } 
        });
    }

    public void stop() {
        stopped = true;
        receiverThread.interrupt();
    }
    
    public InetSocketAddress addressOf(int id) {
        return platformAddrs.get(id);
    }
    
    public void clearPlatforms() {
        platformAddrs.clear();
        platformNames.clear();
    }

    public static void main(String[] args) throws Exception {
        NecsaveTransport transport = new NecsaveTransport(null);
        transport.broadcast(new ActionStop());
        Thread.sleep(100000);        
    }
}
