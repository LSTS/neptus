/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Nov 29, 2012
 */
package pt.lsts.neptus.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.ImcStringDefs;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author jqcorreia
 *
 */
public class SonarLogGenerator {
    IMCOutputStream out;
    
    public SonarLogGenerator(String dirPath) {
        try {
            new File(dirPath).mkdirs();
            out = new IMCOutputStream(IMCDefinition.getInstance(), new FileOutputStream(dirPath + "/Data.lsf"));
            FileUtil.saveToFile(dirPath + "/IMC.xml", ImcStringDefs.getDefinitions());
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void generateSidescanLog() {
        long timestamp = 1; //System.currentTimeMillis();
        int dataSize = 2000;
        
        byte scanlinesHigh[] = new byte[dataSize];
        
        
        for(int i = 0; i < dataSize; i++) {
            if(i % 3 == 0) 
                scanlinesHigh[i] = 127;
            else
                scanlinesHigh[i] = 0; 
        }

        byte scanlinesLow[] = new byte[dataSize];
        
        for(int i = 0; i < dataSize; i++) {
            if(i % 10 < 5) 
                scanlinesLow[i] = 127;
            else
                scanlinesLow[i] = 0; 
        }
        
        byte scanlinesMedium[] = new byte[dataSize];
        
        for(int i = 0; i < dataSize; i++) {
            if(i % 6 < 3) 
                scanlinesMedium[i] = 127;
            else
                scanlinesMedium[i] = 0; 
        }
        
        EstimatedState state = new EstimatedState();
        SonarData sonarHigh = new SonarData();
        SonarData sonarMedium = new SonarData();
        SonarData sonarLow = new SonarData();
        
        state.setAlt(1);
        state.setPsi(-3*Math.PI / 4);
        state.setU(2);
        
        sonarHigh.setData(scanlinesHigh);
        sonarHigh.setMaxRange(30);
        sonarHigh.setFrequency(900);
        
        sonarMedium.setData(scanlinesMedium);
        sonarMedium.setMaxRange(30);
        sonarMedium.setFrequency(800);
        
        sonarLow.setData(scanlinesLow);
        sonarLow.setMaxRange(30);
        sonarLow.setFrequency(700);
        
        for(int i = 0; i < 1000; i++) {
            timestamp += 10;
            state.setTimestampMillis(timestamp);
            sonarHigh.setTimestampMillis(timestamp + 1);
            sonarMedium.setTimestampMillis(timestamp + 2);
            sonarLow.setTimestampMillis(timestamp + 3);
            try {
                out.writeMessage(state);
                out.writeMessage(sonarHigh);
                out.writeMessage(sonarMedium);
                out.writeMessage(sonarLow);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SonarLogGenerator gen = new SonarLogGenerator("/home/jqcorreia/lsts/logs/test");
        gen.generateSidescanLog();
        
//        LsfLogSource source = new LsfLogSource(new File("/home/jqcorreia/lsts/logs/test/Data.lsf"), null);
//        
//        IMraLog log = source.getLog("SonarData");
//        IMCMessage ping = log.firstLogEntry();
//        while(ping != null) {
//            NeptusLog.pub().info("<###> "+ping.getTimestampMillis());
//            ping = log.nextLogEntry();
//        }
    }
}
