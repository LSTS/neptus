/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Nov 29, 2012
 */
package pt.up.fe.dceg.neptus.generators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.util.FileUtil;

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
            FileUtil.copyFile("conf/messages/IMC.xml", dirPath + "/IMC.xml");
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
//            System.out.println(ping.getTimestampMillis());
//            ping = log.nextLogEntry();
//        }
    }
}
