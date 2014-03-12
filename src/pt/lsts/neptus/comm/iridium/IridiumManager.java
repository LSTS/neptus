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
 * Mar 11, 2014
 */
package pt.lsts.neptus.comm.iridium;

import java.awt.Component;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.ImageUtils;

/**
 * This class will handle Iridium communications
 * 
 * @author zp
 */
public class IridiumManager {

    private static IridiumManager instance = null;
    private DuneIridiumMessenger duneMessenger;
    private RockBlockIridiumMessenger rockBlockMessenger;
    private HubIridiumMessenger hubMessenger;
    private SimulatedMessenger simMessenger;
    private ScheduledExecutorService service = null;
    
    public static final int IRIDIUM_MTU = 270;
    public static final int IRIDIUM_HEADER = 6;
    
    private IridiumManager() {
        duneMessenger = new DuneIridiumMessenger();
        rockBlockMessenger = new RockBlockIridiumMessenger();
        hubMessenger = new HubIridiumMessenger();
        simMessenger = new SimulatedMessenger();
    }
    
    private Runnable pollMessages = new Runnable() {
        
        Date lastTime = new Date(System.currentTimeMillis() - 3600 * 1000);
        @Override
        public void run() {
            if (currentMessenger == null)
                return;

            try {
                Date now = new Date();
                Collection<IridiumMessage> msgs = currentMessenger.pollMessages(lastTime);
                for (IridiumMessage m : msgs)
                    processMessage(m);
                
                lastTime = now;
            }
            catch (Exception e) {
                e.printStackTrace();
                NeptusLog.pub().error(e);                
            }
        }
    };
    
    public synchronized boolean isActive() {
        return service != null;
    }
    
    public void processMessage(IridiumMessage msg) {
        Collection<IMCMessage> msgs = msg.asImc();
        
        for (IMCMessage m : msgs) {
            ImcMsgManager.getManager().postInternalMessage("iridium", m);
            
        }
    }

    public void selectMessenger(Component parent) {

        IridiumMessenger[] messengers = new IridiumMessenger[] { duneMessenger, rockBlockMessenger, hubMessenger, simMessenger };

        Object op = JOptionPane.showInputDialog(parent, "Select Iridium provider", "Iridium Provider",
                JOptionPane.QUESTION_MESSAGE, ImageUtils.createImageIcon("images/satellite.png"), messengers,
                currentMessenger);

        if (op != null)
            currentMessenger = (IridiumMessenger) op;
    }
    
    public synchronized void start() {
        if (service != null)
            stop();
        
        ImcMsgManager.registerBusListener(this);        
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(pollMessages, 0, 5, TimeUnit.MINUTES);
    }
    
    public synchronized void stop() {
        if (service != null) {
            service.shutdownNow();           
            service = null;
        }
        ImcMsgManager.unregisterBusListener(this);        
    }

    public static IridiumManager getManager() {
        if (instance == null)
            instance = new IridiumManager();
        return instance;
    }

    private IridiumMessenger currentMessenger;

    /**
     * This method will send the given message using the currently selected messenger
     * 
     * @param msg
     * @return
     */
    public void send(IridiumMessage msg) throws Exception {
        if (currentMessenger == null) {
            throw new Exception("A messenger needs to be selected first");
        }
        System.out.println(ByteUtil.dumpAsHexToString(msg.serialize()));
        currentMessenger.sendMessage(msg);
    }
    
    public static void main(String[] args) {
        IridiumManager.getManager().selectMessenger(null);
        IridiumManager.getManager().start();
        IridiumManager.getManager().stop();
    }
}
