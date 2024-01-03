/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Mar 26, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * Exporter from log data do XTF format
 * @author jqcorreia
 *
 */
//@PluginDescription(name="XTF Exporter")
public class XTFExporter implements MRAExporter {
    IMraLogGroup source;
    BathymetryParser parser = null;
    File outFile;
    RandomAccessFile raf;
    FileChannel chan;
    ByteBuffer buf;
    
    
    public XTFExporter(IMraLogGroup source) {
        this.source = source;
        try {
            parser = BathymetryParserFactory.build(source);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return parser != null;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        try {
            outFile = new File(source.getFile("Data.lsf").getParent() + "/mra/Data.xtf");
            raf = new RandomAccessFile(outFile, "rw");
            chan = raf.getChannel();
            buf = chan.map(MapMode.READ_WRITE, 0, 1024);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            
            // Write XTF file header
            buf.put((byte) 0x7b);
            buf.put((byte) 1);
            
            writeString("KleinXlt");
            writeString("4.0");
            
            buf.putShort(34, (short)0); // Sonar Type 0 = NONE
            buf.position(164);
            buf.putShort((short)3); // Nav Units 3 = LatLon
            buf.putShort((short) 0); // Number of sidescan channels
            buf.putShort((short) 1); // Number of bathy channels
            
            // Still inside XTF file header, write CHANINFO structure
            buf.position(256);
            
            buf.put((byte) 3); // Channel type 3 = Bathy
            buf.putShort((short) 4); // Bytes per sample
            
            // Write bathymetry data
            BathymetrySwath swath;
            parser.rewind();
            int swathsRead = 0;
            
            while((swath = parser.nextSwath()) != null) {
                int swathSize = 64;
                buf = chan.map(MapMode.READ_WRITE, 1024 + (swathsRead * swathSize), swathSize);
                buf.putShort((short) 0xFACE);
                buf.put((byte) 42);
                buf.putInt(64);
                buf.putDouble(33, swath.getPose().getPosition().getLatitudeDegs());
                buf.putDouble(41, swath.getPose().getPosition().getLongitudeDegs());
                buf.putDouble(49, swath.getPose().getAltitude());
            }
            
            raf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return I18n.text("XTF conversion done.");
    }

    public void writeString(String s) 
    {
        for(char c : s.toCharArray()) {
            buf.put((byte) c);
        }
    }
}
