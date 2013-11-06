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
 * Author: jqcorreia
 * Apr 16, 2013
 */
package pt.up.fe.dceg.neptus.mra.exporters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.util.llf.LsfLogSource;

import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLStructure;

/**
 * @author jqcorreia
 *
 */
public class MatExporter implements MraExporter {
    IMraLogGroup source;
    
    
    public MatExporter(IMraLogGroup source) {
        this.source = source;
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process() {
        Collection<String> logList = source.getLsfIndex().getDefinitions().getMessageNames();
        IMraLog parser;
        
        File outFile = new File("/home/jqcorreia/test.mat");
        MatFileWriter writer = new MatFileWriter();
        
        MLStructure struct;
        
        Collection<MLArray> l = new ArrayList<MLArray>();
        int c = 0;
        
        for(String log : logList) {
            parser = source.getLog(log);
            
            if(parser == null)
                continue;
            
//            if(!log.equals("EstimatedState") && !log.equals("Acceleration"))
//                continue;
//            
            System.out.println("Reading " + log);
            struct = new MLStructure(log, new int[] {1, 1});
            int numEntries = parser.getNumberOfEntries();
            int numInserted = 0;
            
            IMCMessage m = parser.firstLogEntry();
            LinkedHashMap<String, MLArray> fieldMap = new LinkedHashMap<String, MLArray>();
            
            // Setup arrays for struct
            for(String field : m.getFieldNames()) {
                fieldMap.put(field, new MLDouble(field, new int[] { numEntries, 1 }));
                ((MLDouble)fieldMap.get(field)).set(m.getDouble(field), numInserted );
            }
            
            numInserted++;
            
            while((m = parser.nextLogEntry()) != null) { 
                for(String field : m.getFieldNames()) {
                    ((MLDouble)fieldMap.get(field)).set(m.getDouble(field), numInserted);
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
            
            l.add(struct);
            System.out.println("Writing " + log);
            try {
                writer.write(outFile, l, (c++ == 0));
            }
            catch (IOException e) {
                e.printStackTrace();
                return e.getClass().getSimpleName()+" while exporting to MAT: "+e.getMessage();
            }
        }
//        
//        
//
//        System.out.println("creating objects");
//        MLDouble mld = new MLDouble("foo", new int[] { 10000000, 1 });
//        
//        for(int i = 0; i < 10000000; i++)
//        {
//            mld.set(0.0, i);
//        }
//        
//        ArrayList<MLArray> z = new ArrayList<MLArray>();
//        System.out.println("creating objects #2");
//        System.out.println("creating objects #3");
//        z.add(mld);
//        System.out.println("starting write");
//        try {
//            new MatFileWriter("/home/jqcorreia/foo.mat", z);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
        
        return "Log exported to MAT successfully";
    }

    @Override
    public String getName() {
        return I18n.text("MatLab format .MAT");
    }

    public static void main(String[] args) throws Exception {
        IMraLogGroup source = new LsfLogSource(new File("/home/jqcorreia/lsts/logs/lauv-xtreme-2/20130405/135842_testSidescan_4m/Data.lsf"), null);
        MatExporter me = new MatExporter(source);
        
        me.process();
    }
}
