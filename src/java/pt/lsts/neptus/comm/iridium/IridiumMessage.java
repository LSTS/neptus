/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import org.apache.commons.codec.binary.Hex;
import pt.lsts.dccl.DcclTranslator;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.EntityParameters;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.QueryEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;


/**
 * @author zp
 *
 */
public abstract class IridiumMessage implements Comparable<IridiumMessage> {

    public int source = ImcId16.NULL_ID.intValue();
    public int destination = ImcId16.NULL_ID.intValue();
    public int message_type = -1;
    public long timestampMillis = System.currentTimeMillis();
    public abstract int serializeFields(IMCOutputStream out) throws Exception;
    public abstract int deserializeFields(IMCInputStream in) throws Exception;
    public abstract Collection<IMCMessage> asImc();
    private static LinkedHashMap<Integer, Class<? extends IridiumMessage> > iridiumTypes = new LinkedHashMap<>();

    public IridiumMessage(int msgType) {
        this.message_type = msgType;        
    }
    
    static {
        iridiumTypes.put(2001, DeviceUpdate.class);
        iridiumTypes.put(2003, ActivateSubscription.class);
        iridiumTypes.put(2004, DeactivateSubscription.class);
        iridiumTypes.put(2005, IridiumCommand.class);
        iridiumTypes.put(2006, DesiredAssetPosition.class);
        iridiumTypes.put(2007, TargetAssetPosition.class);        
        iridiumTypes.put(2010, ImcIridiumMessage.class);
        iridiumTypes.put(2011, ExtendedDeviceUpdate.class);
        iridiumTypes.put(2012, UpdateDeviceActivation.class);
        iridiumTypes.put(2013, ImcFullIridiumMessage.class);
    }
    
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream ios = new IMCOutputStream(baos);
        ios.setBigEndian(false);
        int size = 6;
        ios.writeUnsignedShort(source);
        ios.writeUnsignedShort(destination);
        ios.writeUnsignedShort(message_type);
        size += serializeFields(ios);
        return Arrays.copyOf(baos.toByteArray(), size);
    }
    
    public static IridiumMessage deserialize(byte[] data) throws Exception {
        IMCInputStream iis = new IMCInputStream(new ByteArrayInputStream(data), IMCDefinition.getInstance());
        iis.setBigEndian(false);
        iis.mark(10);
        int avlBytes = iis.available();
        int source = avlBytes >= 2 ? iis.readUnsignedShort() : ImcId16.NULL_ID.intValue();
        int dest = avlBytes >= 4 ? iis.readUnsignedShort() : ImcId16.NULL_ID.intValue();
        int mgid = avlBytes >= 6 ? iis.readUnsignedShort() : -1;
        IridiumMessage m = null;
        if (iridiumTypes.containsKey(mgid)) {
            m = iridiumTypes.get(mgid).getDeclaredConstructor().newInstance();
        } else {
            try {
                byte[] dataToProcess = data;
                // Something to accept a byte[] and then returning a message of type ImcMessage
                if (data.length > 5) {
                    String rbString = new String(Arrays.copyOf(data, 5), StandardCharsets.UTF_8);
                    if (rbString.startsWith("RB")) {
                        // remove the first 5 bytes of the data array
                        dataToProcess = Arrays.copyOfRange(data, 5, data.length);
                    }
                }
                IMCMessage imcMessage = DcclTranslator.byteToImc(dataToProcess);
                NeptusLog.pub().info("Decoded using DCCL: " + imcMessage.asJSON());

                // Check if IMCSystem is already a DCCL Speaker
                ImcSystem imcSystem = ImcSystemsHolder.lookupSystem(imcMessage.getSrc());

                // TODO: imcSystem may not exist yet if 1st message
                if (imcSystem != null && !imcSystem.getDcclSpeaker()) {
                    NeptusLog.pub().info("Set System " + imcSystem.getName() + " as a dccl speaker");
                    imcSystem.setAsDcclSpeaker();
                }

                // Turn this message into an m
                ImcFullIridiumMessage imcFullIridiumMessage = new ImcFullIridiumMessage();
                imcFullIridiumMessage.setMsg(imcMessage);
                return imcFullIridiumMessage;
            }
            catch (IllegalArgumentException e) {
                System.out.println("Unable to decode message to DCLL");
            }

            mgid = -1;
            iis.reset();
            avlBytes = iis.available();
            // look for RB+3-bytes send type
            if (avlBytes >= 5) {
                byte[] ba = new byte[5];
                int read = iis.read(ba);
                if (read == 5) {
                    String rbString = new String(ba);
                    if (rbString.startsWith("RB")) {
                        source = avlBytes >= 5+2 ? iis.readUnsignedShort() : ImcId16.NULL_ID.intValue();
                        dest = avlBytes >= 5+4 ? iis.readUnsignedShort() : ImcId16.NULL_ID.intValue();
                        mgid = avlBytes >= 5+6 ? iis.readUnsignedShort() : -1;
                    }
                    if (iridiumTypes.containsKey(mgid)) {
                        m = iridiumTypes.get(mgid).getDeclaredConstructor().newInstance();
                    }
                    else {
                        mgid = -1;
                        iis.reset();
                        read = iis.read(ba);
                        // iis.mark(10);
                    }
                }
            }

            if (mgid == -1 || m == null) {
                iis.reset();
                m = PlainTextMessage.createTextMessageFrom(iis);
            } else {
                iis.mark(10);
            }
        }
        
        if (m != null) {
            //m.setSource(mgid > -1 ? source : 0xFFFF);
            //m.setDestination(mgid > -1 ? dest : 0xFFFF);
            m.setMessageType(mgid);
            if (mgid > -1)
                m.deserializeFields(iis);
        }
        iis.close();
        
        return m;        
    }
    
    /**
     * @return the source
     */
    public final int getSource() {
        return source;
    }
    
    /**
     * @param source the source to set
     */
    public final void setSource(int source) {
        this.source = source;
    }
    
    /**
     * @return the destination
     */
    public final int getDestination() {
        return destination;
    }
    
    /**
     * @param destination the destination to set
     */
    public final void setDestination(int destination) {
        this.destination = destination;
    }
    
    /**
     * @return the message_type
     */
    public final int getMessageType() {
        return message_type;
    }
    
    /**
     * @param message_type the message_type to set
     */
    public final void setMessageType(int message_type) {
        this.message_type = message_type;
    }
        
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Message of type "+getClass().getSimpleName()+" {\n"); 
        sb.append("\tSource: "+IMCDefinition.getInstance().getResolver().resolve(getSource())+"\n");
        sb.append("\tDestination: "+IMCDefinition.getInstance().getResolver().resolve(getDestination())+"\n");
        return sb.toString();
    }
    
    @Override
    public int compareTo(IridiumMessage o) {
        return (int)(timestampMillis - o.timestampMillis);
    }

    public static void processEntityParameterForDCCL(IMCMessage imcMessage ) {
        // Check if it is a query EntityParameters
        int imcMessageType = imcMessage.getMessageType().getId();
        if (imcMessageType != EntityParameters.ID_STATIC)
            return;

        ImcSystem imcSystem = ImcSystemsHolder.lookupSystem(imcMessage.getSrc());
        if (imcSystem == null)
            return;

        // Check if it is related with
        EntityParameters entityParameters = (EntityParameters) imcMessage;
        if (!entityParameters.getName().equals("Communications Manager"))
            return;

        for (EntityParameter entityParameter : entityParameters.getParams()) {
            if (!entityParameter.getName().equals("DCCL Encoding"))
                continue;

            boolean value = entityParameter.getValue().equals("true") || entityParameter.getValue().equals("1");
            if (value) {
                imcSystem.setAsDcclSpeaker();
            } else {
                imcSystem.setAsNonDcclSpeaker();
            }

            break; // we already found what we needed
        }
    }

    public static void main(String[] args) throws Exception {
        String msgTxt = "(T) (lauv-xplore-5) 15:47:41 / 38 31.661100, -28 37.417050 / f:56 v:258 c:100 / s: S";
        IridiumMessage msg = IridiumMessage.deserialize(msgTxt.getBytes());
        System.out.println(msg);

        //"524200202704080408da070202f86b106af86b106a03bd244203520bc1ffff000000000d0053fe0000"
        byte[] bytes = Hex.decodeHex("524200202704080408da070202f86b106af86b106a03bd244203520bc1ffff000000000d0053fe0000");
        IridiumMessage msg2 = IridiumMessage.deserialize(bytes);
        System.out.println(msg2);

        byte[] bytes3 = Hex.decodeHex("04080408da0707015d62106a00003842");
        IridiumMessage msg3 = IridiumMessage.deserialize(bytes3);
        System.out.println(msg3);

        String msgTxt4 = "(caravel) Boot: 2026-05-22 14:02:24 - caravel-aux";
        IridiumMessage msg4 = IridiumMessage.deserialize(msgTxt4.getBytes());
        System.out.println(msg4);

        String msgTxt5 = "Secondary Euler Angles provider not working!";
        IridiumMessage msg5 = IridiumMessage.deserialize(msgTxt5.getBytes());
        System.out.println(msg5);
    }
}
