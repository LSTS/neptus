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
 * Nov 12, 2015
 */
package org.necsave;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.Inflater;

import com.google.common.io.LittleEndianDataInputStream;

import info.necsave.msgs.CompressedMsg;
import info.necsave.msgs.PlatformInfo;
import info.necsave.msgs.Header.MEDIUM;
import info.necsave.proto.Message;
import info.necsave.proto.ProtoDefinition;

/**
 * @author zp
 *
 */
public class NMPUtilities {

    public static String getAsHtml(Message message) {
        return "<html><h1>"+message.getAbbrev()+"</h1>"+
                getAsInnerHtml(message.getHeader())+"<br/>"+
                getAsInnerHtml(message)+
                "</html>";
    }
    
    private static Vector<String> hexFields = new Vector<String>();
    static {
        hexFields.add("proto");        
    }
    
    private static String getAsInnerHtml(Message msg) {
        if (msg == null)
            return "null";
        
        String ret = "<table border=1><tr bgcolor='#CCCCEE'><th>"+msg.getAbbrev()+"</th><th>"+msg.getFieldNames().length+" fields</th></tr>";
        if (msg.getAbbrev() == null)
            ret = "<table border=1 width=100%><tr bgcolor='#CCEECC'><th>Header</th><th>"+msg.getFieldNames().length+" fields</th></tr>";
        
        for (String fieldName : msg.getFieldNames()) {
            String value = msg.getString(fieldName);
            if (msg.getAbbrev() == null && fieldName.equals("timestamp")) {
                SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS ");
                dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
                value = dateFormatUTC.format(new Date((long)(msg.getDouble("timestamp")*1000.0)))+"UTC";
            }
            else if (msg.getAbbrev() == null && hexFields.contains(fieldName)) {
                value += "  [0x" + Long.toHexString(msg.getLong(fieldName)).toUpperCase() + "]";
            }       
            
            if (msg.getTypeOf(fieldName).equalsIgnoreCase("message") && msg.getValue(fieldName) != null) 
                value = getAsInnerHtml(msg.getMessage(fieldName));       
            
            else if (msg.getTypeOf(fieldName).equalsIgnoreCase("message-list") && msg.getValue(fieldName) != null) {
                value = "<table><tr>";
                for (Message m : msg.getMessageList(fieldName))
                    value += "<td>"+getAsInnerHtml(m)+"</td>";
                
                value += "</tr></table>";
            }
            
            ret += "<tr><td align=center width=225>"+fieldName+"</td><td width=225>"+value+"</td></tr>";
        }       
        return ret+"</table>";
    }
    
    public static Message decompress(CompressedMsg msg) {
        Inflater inflater = new Inflater();
        inflater.setInput(msg.getPayload());
        byte[] out = new byte[65535];
        try {
            int len = inflater.inflate(out);
            Message decompressed = ProtoDefinition.getFactory().createMessage(msg.getMsgType(), ProtoDefinition.getInstance());
            ProtoDefinition.getInstance().deserializeFields(decompressed, new LittleEndianDataInputStream(new ByteArrayInputStream(out, 0, len)));
            return decompressed;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void main(String[] args) {
        PlatformInfo pinfo = new PlatformInfo();
        pinfo.setMedium(MEDIUM.ACOUSTIC);
        System.out.println(pinfo.getHeader().getString("medium"));
        System.out.println(getAsHtml(pinfo));
    }
}
