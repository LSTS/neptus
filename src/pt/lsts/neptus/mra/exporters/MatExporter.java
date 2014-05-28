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
 * Author: jqcorreia
 * Apr 16, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCFieldType;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.llf.LsfLogSource;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt16;
import com.jmatio.types.MLInt64;
import com.jmatio.types.MLInt8;
import com.jmatio.types.MLSingle;
import com.jmatio.types.MLStructure;
import com.jmatio.types.MLUInt64;
import com.jmatio.types.MLUInt8;

/**
 * @author jqcorreia
 * @author pdias
 */
//@PluginDescription
public class MatExporter implements MRAExporter {
    private static final int MAX_PLAINTEXT_RAWDATA_LENGHT = 256; //256; 0xFFFF

    private IMraLogGroup source;
    
    public MatExporter(IMraLogGroup source) {
        this.source = source;
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        Collection<String> logList = source.getLsfIndex().getDefinitions().getMessageNames();
        IMraLog parser;
        
        File outFile = new File(source.getDir(), "mra/DataIMC.mat");
        outFile.getParentFile().mkdir();
        MatFileWriter writer = new MatFileWriter();
        
        MLStructure struct;
        
        Collection<MLArray> baseMatLabDataToWrite = new ArrayList<MLArray>();
        int c = 0;
        double messageLogPartialPerc = 1. / logList.size();
        double progress = 0;
        final double structFullPrec = 60;
        final double writeFullPrec = 100 - structFullPrec;
        for(String messageLog : logList) {
            parser = source.getLog(messageLog);
            
            if(parser == null) {
                System.out.println("Reading nothing for " + messageLog);
                if (pmonitor != null)
                    pmonitor.setNote(I18n.textf("Reading nothing for %message", messageLog));
                progress += (structFullPrec + writeFullPrec) * messageLogPartialPerc;
                if (pmonitor != null)
                    pmonitor.setProgress((int) progress);
                continue;
            }
            
            System.out.println("Reading " + messageLog);
            if (pmonitor != null)
                pmonitor.setNote(I18n.textf("Reading %message", messageLog));
            
            struct = new MLStructure(messageLog, new int[] {1, 1});
            int numEntries = parser.getNumberOfEntries();
            int numInserted = 0;
            
            IMCMessage m = parser.firstLogEntry();
            LinkedHashMap<String, MLArray> fieldMap = new LinkedHashMap<String, MLArray>();
            
            // Setup arrays for struct
            for(String field : m.getFieldNames()) {
                IMCFieldType filedType = m.getMessageType().getFieldType(field);
                switch (filedType) {
                    case TYPE_FP32:
                        fieldMap.put(field, new MLSingle(field, new int[] { numEntries, 1 }, MLArray.mxSINGLE_CLASS, 0));
                        ((MLSingle) fieldMap.get(field)).set((float) m.getDouble(field), numInserted);
                        break;
                    case TYPE_FP64:
                        fieldMap.put(field, new MLDouble(field, new int[] { numEntries, 1 }));
                        ((MLDouble) fieldMap.get(field)).set(m.getDouble(field), numInserted);
                        break;
                    case TYPE_INT8:
                        fieldMap.put(field, new MLInt8(field, new int[] { numEntries, 1 }));
                        ((MLInt8) fieldMap.get(field)).set((byte) m.getInteger(field), numInserted);
                        break;
                    case TYPE_INT16:
                        fieldMap.put(field, new MLInt16(field, new int[] { numEntries, 1 }));
                        ((MLInt16) fieldMap.get(field)).set((short) m.getInteger(field), numInserted);
                        break;
                    case TYPE_INT32:
//                        fieldMap.put(field, new MLInt32(field, new int[] { numEntries, 1 }));
//                        ((MLInt32) fieldMap.get(field)).set(m.getInteger(field), numInserted);
//                        break;
                    case TYPE_INT64:
                        fieldMap.put(field, new MLInt64(field, new int[] { numEntries, 1 }));
                        ((MLInt64) fieldMap.get(field)).set(m.getLong(field), numInserted);
                        break;
                    case TYPE_UINT8:
                        fieldMap.put(field, new MLUInt8(field, new int[] { numEntries, 1 }));
                        ((MLUInt8) fieldMap.get(field)).set((byte) m.getInteger(field), numInserted);
                        break;
                    case TYPE_UINT16:
                    case TYPE_UINT32:
//                        fieldMap.put(field, new MLUInt32(field, new int[] { numEntries, 1 }));
//                        ((MLUInt32) fieldMap.get(field)).set(m.getInteger(field), numInserted);
//                        break;
//                    case TYPE_UINT64:
                        fieldMap.put(field, new MLUInt64(field, new int[] { numEntries, 1 }));
                        ((MLUInt64) fieldMap.get(field)).set(m.getLong(field), numInserted);
                        break;
                    case TYPE_PLAINTEXT:
                    default:
                        fieldMap.put(field, new MLChar(field, new int[] { numEntries, MAX_PLAINTEXT_RAWDATA_LENGHT }, MLArray.mxCHAR_CLASS, 0));
                        String val = m.getAsString(field);
                        ((MLChar) fieldMap.get(field)).set(val == null ? "" : val, numInserted);
                        break;
                }
//                fieldMap.put(field, new MLDouble(field, new int[] { numEntries, 1 }));
//                ((MLDouble)fieldMap.get(field)).set(m.getDouble(field), numInserted );
            }
            
            numInserted++;
            
            while((m = parser.nextLogEntry()) != null) { 
                for(String field : m.getFieldNames()) {
                    IMCFieldType filedType = m.getMessageType().getFieldType(field);
                    switch (filedType) {
                        case TYPE_FP32:
                            ((MLSingle) fieldMap.get(field)).set((float) m.getDouble(field), numInserted);
                            break;
                        case TYPE_FP64:
                            ((MLDouble) fieldMap.get(field)).set(m.getDouble(field), numInserted);
                            break;
                        case TYPE_INT8:
                            ((MLInt8) fieldMap.get(field)).set((byte) m.getInteger(field), numInserted);
                            break;
                        case TYPE_INT16:
                            ((MLInt16) fieldMap.get(field)).set((short) m.getInteger(field), numInserted);
                            break;
                        case TYPE_INT32:
//                            ((MLInt32) fieldMap.get(field)).set(m.getInteger(field), numInserted);
//                            break;
                        case TYPE_INT64:
                            ((MLInt64) fieldMap.get(field)).set(m.getLong(field), numInserted);
                            break;
                        case TYPE_UINT8:
                            ((MLUInt8) fieldMap.get(field)).set((byte) m.getInteger(field), numInserted);
                            break;
                        case TYPE_UINT16:
                        case TYPE_UINT32:
//                            ((MLUInt32) fieldMap.get(field)).set(m.getInteger(field), numInserted);
//                            break;
//                        case TYPE_UINT64:
                            ((MLUInt64) fieldMap.get(field)).set(m.getLong(field), numInserted);
                            break;
                        case TYPE_PLAINTEXT:
                        default:
                            String val = m.getAsString(field);
                            ((MLChar) fieldMap.get(field)).set(val == null ? "" : val, numInserted);
                            break;
                    }
//                    ((MLDouble)fieldMap.get(field)).set(m.getDouble(field), numInserted);
                }
                numInserted++;
            }
            
            for(String field : fieldMap.keySet()) {
                struct.setField(field, fieldMap.get(field));
//                System.out.println(source.getLsfIndex().getDefinitions().getType(log).getFieldType(field).getJavaType() == double.class);
//                if(source.getLsfIndex().getDefinitions().getType(log).getFieldType(field).getJavaType() == double.class) {
//                    struct.setField(field, new MLDouble(field, (Double[]) fieldMap.get(field).toArray(new Double[fieldMap.get(field).size()]), fieldMap.get(field).size()));
//                }
            }
            
            progress += structFullPrec * messageLogPartialPerc;
            if (pmonitor != null)
                pmonitor.setProgress((int) progress);
            
            baseMatLabDataToWrite.add(struct);
            System.out.println("Writing " + messageLog);
            if (pmonitor != null)
                pmonitor.setNote(I18n.textf("Writing %message", messageLog));
            try {
                writer.write(outFile, baseMatLabDataToWrite, (c++ == 0));
            }
            catch (IOException e) {
                e.printStackTrace();
                return e.getClass().getSimpleName()+" while exporting to MAT: "+e.getMessage();
            }
            
            progress += writeFullPrec * messageLogPartialPerc;
            if (pmonitor != null)
                pmonitor.setProgress((int) progress);
        }
        
        if (pmonitor != null) {
            pmonitor.setNote(I18n.text("Log exported to MAT successfully"));
            pmonitor.setProgress(100);
        }
        return "Log exported to MAT successfully";
    }

    @Override
    public String getName() {
        return I18n.text("MatLab format .MAT");
    }

    public static void main(String[] args) throws Exception {

//      System.out.println("creating objects");
//      MLDouble mld = new MLDouble("foo", new int[] { 10000000, 1 });
//      
//      for(int i = 0; i < 10000000; i++)
//      {
//          mld.set(0.0, i);
//      }
//      
//      ArrayList<MLArray> z = new ArrayList<MLArray>();
//      System.out.println("creating objects #2");
//      System.out.println("creating objects #3");
//      z.add(mld);
//      System.out.println("starting write");
//      try {
//          new MatFileWriter("/home/jqcorreia/foo.mat", z);
//      }
//      catch (IOException e) {
//          e.printStackTrace();
//      }
        
        IMraLogGroup source = new LsfLogSource(new File("D:\\LSTS-Logs\\2014-03-27-apdl-xplore1-noptilus2\\logs\\lauv-xplore-1\\20140327\\142100\\Data.lsf.gz"), null);
        MatExporter me = new MatExporter(source);
        me.process(source, new ProgressMonitor(null, "", "", 0, 100));
    }
}
