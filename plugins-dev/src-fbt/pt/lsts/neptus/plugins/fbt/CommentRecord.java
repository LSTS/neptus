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
 * Jan 7, 2014
 */
package pt.lsts.neptus.plugins.fbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.lowagie.text.pdf.codec.Base64.OutputStream;

/**
 * @author zp
 *
 */
public class CommentRecord {

    static final int RECORD_TYPE = 28270;
    
    public int   recordtype;
    public String comment;
    
    public void read(boolean skipType, InputStream is) throws IOException {
        DataInputStream input = new DataInputStream(is);
        if (!skipType) {
            recordtype = input.readUnsignedShort();
            if (recordtype != RECORD_TYPE) {
                throw new IOException("Record type does not match exptected value: "+recordtype+" != "+RECORD_TYPE);
            }
        }
        
        int read = 0;
        byte[] cmt = new byte[128];
        while (read < 128)
            read += input.read(cmt, read, 128 - read);
        
        comment = new String(cmt);
    }
    
    public void write(OutputStream os) throws IOException {
        DataOutputStream out = new DataOutputStream(os);
        if(comment.length() > 126)
            throw new IOException("Comment is too big ("+comment.length()+" bytes)"); 
        out.writeShort(RECORD_TYPE);
        byte[] bytes = comment.getBytes("ASCII"); 
        out.write(bytes);
        for (int i = bytes.length; i < 128; i++)
            out.writeByte(0);
    }
}
