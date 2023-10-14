/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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

import java.util.Collection;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;

/**
 * @author zp
 *
 */
public class ImcIridiumMessage extends IridiumMessage {

    protected IMCMessage msg;
    
    public static int MaxPayloadSize = 270 - 12; 
    
    public ImcIridiumMessage() {
        super(2010);    
    }    
    
    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        if (msg != null) {
            out.writeUnsignedShort(msg.getMgid());
            out.writeUnsignedInt((int)msg.getTimestamp());
            int size = 6 + IMCDefinition.getInstance().serializeFields(msg, out);
            return size;
        }
        return 0;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        int type = in.readUnsignedShort();
        long timestamp = in.readUnsignedInt();
        msg = IMCDefinition.getInstance().create(IMCDefinition.getInstance().getMessageName(type));
        msg.setTimestamp(timestamp);
        try {
            IMCDefinition.getInstance().deserializeFields(msg, in);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return msg.getPayloadSize() + 6;
    }

    /**
     * @return the msg
     */
    public final IMCMessage getMsg() {
        return msg;
    }

    /**
     * @param msg the msg to set
     */
    public final void setMsg(IMCMessage msg) {
        if (msg != null) {
            this.source = msg.getSrc();
            this.destination = msg.getDst();
        }
        this.msg = msg;
    }
    
    @Override
    public Collection<IMCMessage> asImc() {
       Vector<IMCMessage> vec = new Vector<>();
       if (msg != null)
           vec.add(msg);
       
       msg.setSrc(getSource());
       msg.setDst(getDestination());
       msg.setTimestampMillis(timestampMillis);
       return vec;
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        return s + "\tMessage: "+getMsg().toString();         
    }

}
