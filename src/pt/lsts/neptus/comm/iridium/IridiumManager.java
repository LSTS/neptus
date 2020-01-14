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
 * Mar 11, 2014
 */
package pt.lsts.neptus.comm.iridium;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

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
    private RipplesIridiumMessenger ripplesMessenger;
    private ScheduledExecutorService service = null;
    //private IridiumMessenger currentMessenger;
    
    public static final int IRIDIUM_MTU = 270;
    public static final int IRIDIUM_HEADER = 6;
    
    public enum IridiumMessengerEnum {
        DuneIridiumMessenger,
        RockBlockIridiumMessenger,
        HubIridiumMessenger,
        SimulatedMessenger,
        RipplesMessenger
    }
    
    private IridiumManager() {
        duneMessenger = new DuneIridiumMessenger();
        rockBlockMessenger = new RockBlockIridiumMessenger();
        hubMessenger = new HubIridiumMessenger();
        simMessenger = new SimulatedMessenger();
        ripplesMessenger = new RipplesIridiumMessenger();
    }
    
    public IridiumMessenger getCurrentMessenger() {
        switch (GeneralPreferences.iridiumMessenger) {
            case DuneIridiumMessenger:
                return duneMessenger;
            case HubIridiumMessenger:
                return hubMessenger;
            case RockBlockIridiumMessenger:
                return rockBlockMessenger;
            case RipplesMessenger:
                return ripplesMessenger;
            default:
                return simMessenger;
        }
    }
    
    private Runnable pollMessages = new Runnable() {
        
        Date lastTime = new Date(System.currentTimeMillis() - 3600 * 1000);
        @Override
        public void run() {
            try {
                Date now = new Date();
                Collection<IridiumMessage> msgs = getCurrentMessenger().pollMessages(lastTime);
                for (IridiumMessage m : msgs)
                    processMessage(m);
                
                lastTime = now;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                
            }
        }
    };
    
    public boolean isAvailable() {
        return getCurrentMessenger().isAvailable();
    }
    
    public synchronized boolean isActive() {
        return service != null;
    }
    
    public void processMessage(IridiumMessage msg) {
        
        try {
            IridiumMsgTx transmission = new IridiumMsgTx();
            transmission.setData(msg.serialize());
            transmission.setSrc(msg.getSource());
            transmission.setDst(msg.getDestination());
            transmission.setTimestamp(msg.timestampMillis/1000.0);
            ImcMsgManager.getManager().postInternalMessage("IridiumManager", transmission);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        
        Collection<IMCMessage> msgs = msg.asImc();
        
        for (IMCMessage m : msgs) {
            NeptusLog.pub().info("Posting resulting "+m.getAbbrev()+" message to bus.");
            ImcMsgManager.getManager().postInternalMessage("iridium", m);            
        }
    }

    public void selectMessenger(Component parent) {
        Object op = JOptionPane.showInputDialog(parent, "Select Iridium provider", "Iridium Provider",
                JOptionPane.QUESTION_MESSAGE, ImageUtils.createImageIcon("images/satellite.png"), IridiumMessengerEnum.values(),
                GeneralPreferences.iridiumMessenger);

        if (op != null) {
            GeneralPreferences.iridiumMessenger = (IridiumMessengerEnum) op;
            GeneralPreferences.saveProperties();
        }
    }
    
    public synchronized void start() {
        if (service != null)
            stop();
        
        ImcMsgManager.getManager().registerBusListener(this);        
        service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(pollMessages, 0, 5, TimeUnit.MINUTES);
    }
    
    public synchronized void stop() {
        if (service != null) {
            service.shutdownNow();           
            service = null;
        }
        ImcMsgManager.getManager().unregisterBusListener(this);        
    }

    public static IridiumManager getManager() {
        if (instance == null)
            instance = new IridiumManager();
        return instance;
    }

    public static Collection<ImcIridiumMessage> iridiumEncode(IMCMessage msg) throws Exception {
        if (msg.getPayloadSize() < ImcIridiumMessage.MaxPayloadSize) {
            ImcIridiumMessage m = new ImcIridiumMessage();
            m.setSource(msg.getSrc());
            m.setDestination(msg.getDst());
            m.timestampMillis = msg.getTimestampMillis();
            m.msg = msg;
            return Arrays.asList(m);
        }
        else {
            MessagePart[] parts = new IMCFragmentHandler(IMCDefinition.getInstance()).fragment(msg, ImcIridiumMessage.MaxPayloadSize+IMCDefinition.getInstance().headerLength());
            
            ArrayList<ImcIridiumMessage> ret = new ArrayList<ImcIridiumMessage>();
            for (MessagePart mp : parts) {
                ImcIridiumMessage m = new ImcIridiumMessage();
                m.setSource(msg.getSrc());
                m.setDestination(msg.getDst());
                m.timestampMillis = msg.getTimestampMillis();
                m.msg = mp;
                ret.add(m);
            }
            return ret;
        }
    }

    public static void testMessageSerialization() {
        IMCDefinition defs = IMCDefinition.getInstance();

        for (String abbrev: defs.getMessageNames()) {
            IMCMessage m = defs.create(abbrev);
            IMCUtil.fillWithRandomData(m);
            System.out.println("Message of type "+m.getAbbrev()+" and size "+(m.getPayloadSize()));
            System.out.println(m);
            try {
                Collection<ImcIridiumMessage> msgs = iridiumEncode(m);
                System.out.println(" ==> "+msgs.size()+" messages");
                for (ImcIridiumMessage msg : msgs) {
                    ByteUtil.dumpAsHex("Iridium message of type "+msg.getMessageType(), msg.serialize(), System.out);
                    for (byte b : msg.serialize()) {
                        System.out.printf("%02X",b);
                    }
                    System.out.println();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    

    /**
     * This method will send the given message using the currently selected messenger
     * 
     * @param msg
     * @return
     */
    public void send(IridiumMessage msg) throws Exception {
        NeptusLog.pub().info("Sending iridum message via "+getCurrentMessenger().getName()+": "+ByteUtil.encodeToHex(msg.serialize()));
        getCurrentMessenger().sendMessage(msg);
    }
    
    public static void main(String[] args) throws Exception {
       String hexText = "ffff0000db07002247545653000000000000000041002147545653000000000000000042000147545653000000000000000031000047545653000000000000000032000047545653000000000000000030";
       byte[] data = Hex.decodeHex(hexText.toCharArray());
       
       ExtendedDeviceUpdate devupd = (ExtendedDeviceUpdate) IridiumMessage.deserialize(data);
       for (Position p : devupd.positions.values()) {
           System.out.println(p.posType);
       }
    }
}
