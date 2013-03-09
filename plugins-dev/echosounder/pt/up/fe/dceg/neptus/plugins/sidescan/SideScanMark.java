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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Sep 14, 2011
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * @author zp
 *
 */
public class SideScanMark {

    public Rectangle2D bounds = new Rectangle2D.Double();
    public String id = "";
    public String description = "";

    public static int write(SideScanMark orig, DataOutputStream dos) throws IOException {
        dos.writeDouble(orig.bounds.getCenterX());        
        dos.writeDouble(orig.bounds.getCenterY());
        dos.writeDouble(orig.bounds.getWidth());        
        dos.writeDouble(orig.bounds.getHeight());
        dos.writeShort(orig.id.length());
        dos.writeChars(orig.id);
        dos.writeShort(orig.description.length());
        dos.writeChars(orig.description);        
        return 36 + orig.id.length()*2+orig.description.length()*2;
    }

    public static int read(SideScanMark dest, DataInputStream dis) throws IOException {
        dest.bounds = new Rectangle2D.Double(dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble());
        int read = 36;
        int len = dis.readShort();
        read += len * 2;
        char[] str = new char[len];
        for (int i = 0; i < len; i++) {
            str[i] = dis.readChar();
        }
        dest.id = new String(str);

        len = dis.readShort();
        read += len * 2;
        str = new char[len];
        for (int i = 0; i < len; i++) {
            str[i] = dis.readChar();
        }
        dest.description = new String(str);
        return read;
    }

    public static Vector<SideScanMark> read(File input) throws IOException {
        Vector<SideScanMark> marks = new Vector<SideScanMark>();

        if (input.canRead()) {
            DataInputStream dis = new DataInputStream(new FileInputStream(input));
            while (dis.available() > 0) {
                SideScanMark mark = new SideScanMark();
                read(mark, dis);
                marks.add(mark);
            }
            dis.close();
        }

        return marks;
    }

    public static void write(File output, Vector<SideScanMark> marks) throws IOException {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(output));

        for (SideScanMark m : marks) {
            write(m, dos);
        }
        dos.close();                
    }
}
