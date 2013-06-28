/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2013
 */
package pt.up.fe.dceg.neptus.comm.iridium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCInputStream;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;

/**
 * @author zp
 *
 */
public abstract class IridiumMessage {

    public int source, destination, message_type;
    public abstract int serializeFields(IMCOutputStream out) throws Exception;
    public abstract int deserializeFields(IMCInputStream in) throws Exception;
    public abstract Collection<IMCMessage> asImc();
    private static LinkedHashMap<Integer, Class<? extends IridiumMessage> > iridiumTypes = new LinkedHashMap<>();
    
    static {
        iridiumTypes.put(2001, DeviceUpdate.class);
        iridiumTypes.put(2003, ActivateSubscription.class);
        iridiumTypes.put(2004, DeactivateSubscription.class);
        iridiumTypes.put(2005, IridiumCommand.class);        
    }
    
    public byte[] serialize() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream ios = new IMCOutputStream(baos);
        ios.setBigEndian(false);
        ios.writeUnsignedShort(source);
        ios.writeUnsignedInt(destination);
        ios.writeUnsignedInt(message_type);
        serializeFields(ios);
        return baos.toByteArray();
    }
    
    public static IridiumMessage deserialize(byte[] data) throws Exception {

        IMCInputStream iis = new IMCInputStream(new ByteArrayInputStream(data));
        iis.setBigEndian(false);
        int source = iis.readUnsignedShort();
        int dest = iis.readUnsignedShort();
        int mgid = iis.readUnsignedShort();
        IridiumMessage m = null;
        if (iridiumTypes.containsKey(mgid)) {
            m = iridiumTypes.get(mgid).newInstance();

        }
        else if (IMCDefinition.getInstance().getMessageName(mgid) != null) {
            m = new ImcIridiumMessage();
        }        
        
        if (m != null) {
            m.setSource(source);
            m.setDestination(dest);
            m.setMessageType(mgid);
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


}
