/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 17, 2015
 */
package pt.lsts.neptus.plugins.trex;

import java.io.ByteArrayOutputStream;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.TrexToken;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.DuneIridiumMessenger;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.DeliveryListener;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 */
@PluginDescription(name="Europtus Interface")
public class Europtus extends ConsoleInteraction implements MessageDeliveryListener, DeliveryListener {

    @NeptusProperty
    public String auv1 = "lauv-xplore-1";
    
    @NeptusProperty
    public String auv2 = "lauv-xplore-2";
    
    @NeptusProperty
    public String europtus = "127.0.0.1:8800";

    @NeptusProperty(description="IP and port of first simulated DUNE instace")
    public String sim_auv1 = "127.0.0.1:6002";
    
    @NeptusProperty(description="IP and port of second simulated DUNE instace")
    public String sim_auv2 = "127.0.0.1:6003";
    
    private String europtus_host = null, auv1_host = null, auv2_host = null;
    private int europtus_port = -1, auv1_port = -1, auv2_port = -1;
    
    private HubIridiumMessenger hubMessenger = null;
    private DuneIridiumMessenger duneMessenger = null;
    private UDPTransport imcTransport = null;
    
    public enum Connection {
        MantaIridium,
        HubIridium,
        IMC
    }
    
    @NeptusProperty(description="Connection to be used to send Goals")
    public Connection connection_type = Connection.IMC;
        
    void sendToEuroptus(TrexToken msg) throws Exception {
        if (europtus_host == null)
            throw new Exception("Europtus host and port not correctly set.");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.serialize(new IMCOutputStream(baos));
        getUdpTransport().sendMessage(europtus_host, europtus_port, baos.toByteArray(), this);
    }
    
    ImcIridiumMessage wrap(IMCMessage msg) {
        ImcIridiumMessage m = new ImcIridiumMessage();
        m.setMsg(msg);        
        m.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        return m;
    }
    
    void sendToVehicle1(IMCMessage msg) throws Exception {
        switch (connection_type) {
            case IMC:
                ImcMsgManager.getManager().sendMessageToSystem(msg, auv1, this);
                break;
            case HubIridium:
                ImcIridiumMessage m = wrap(msg);
                m.setDestination(ImcSystemsHolder.getSystemWithName(auv1).getId().intValue());
                getHubMessenger().sendMessage(m);
                break;
            case MantaIridium:
                ImcIridiumMessage m2 = wrap(msg);
                m2.setDestination(ImcSystemsHolder.getSystemWithName(auv1).getId().intValue());
                getDuneMessenger().sendMessage(m2);
                break;
            default:
                break;
        }
    }
    
    @Override
    public void deliveryError(IMCMessage message, Object error) {
        if (error instanceof Throwable)
            deliveryResult(ResultEnum.Error, new Exception((Throwable)error));
        else
            deliveryResult(ResultEnum.Error, new Exception(""+error));
    }
    
    @Override
    public void deliverySuccess(IMCMessage message) {
        deliveryResult(ResultEnum.Success, null);
    }
    
    @Override
    public void deliveryTimeOut(IMCMessage message) {
        deliveryResult(ResultEnum.TimeOut, new Exception("Time out while sending "+message.getAbbrev()));
    }
    
    @Override
    public void deliveryUncertain(IMCMessage message, Object msg) {
        deliveryResult(ResultEnum.Success, null);
    }
    
    @Override
    public void deliveryUnreacheable(IMCMessage message) {
        deliveryResult(ResultEnum.Unreacheable, new Exception("Destination is unreacheable"));
    }
    
    @Override
    public void deliveryResult(ResultEnum result, Exception error) {
       
    }
    
    /**
     * @return the hubMessenger
     */
    public synchronized HubIridiumMessenger getHubMessenger() {
        if (hubMessenger == null)
            hubMessenger = new HubIridiumMessenger();
        return hubMessenger;
    }

    /**
     * @return the duneMessenger
     */
    public synchronized DuneIridiumMessenger getDuneMessenger() {
        if (duneMessenger == null)
            duneMessenger = new DuneIridiumMessenger();
        return duneMessenger;
    }
    
    public synchronized UDPTransport getUdpTransport() {
        if (imcTransport == null)
            imcTransport = new UDPTransport();
        return imcTransport;
    }
    
    @Override
    public void propertiesChanged() {
        try {
            europtus_host = europtus.split(":")[0];
            europtus_port = Integer.parseInt(europtus.split(":")[1]);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error parsing Europtus host:port", e);
            europtus_host = null;
            europtus_port = -1;
        }
        
        try {
            auv2_host = auv2.split(":")[0];
            auv2_port = Integer.parseInt(auv2.split(":")[1]);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error parsing AUV2 simulator host:port", e);
            auv2_host = null;
            auv2_port = -1;
        }

        try {
            auv1_host = auv1.split(":")[0];
            auv1_port = Integer.parseInt(auv1.split(":")[1]);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error parsing AUV1 simulator host:port", e);
            auv1_host = null;
            auv1_port = -1;
        }
    }
    

    @Override
    public void initInteraction() {
        propertiesChanged();
    }

    @Override
    public void cleanInteraction() {

    }
}
