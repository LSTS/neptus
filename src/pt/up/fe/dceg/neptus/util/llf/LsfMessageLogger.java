/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Aug 23, 2012
 * $Id:: LsfMessageLogger.java 9615 2012-12-30 23:08:28Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author zp
 * 
 */
class LsfMessageLogger {

    private static LsfMessageLogger instance = null;
    private IMCOutputStream ios = null;
    protected SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd'/'HHmmss");
    protected String logPath = null;

    private String logBaseDir = "log/messages/";

    private LsfMessageLogger() {
        changeLog();

        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        synchronized (LsfMessageLogger.this) {
                            IMCOutputStream copy = ios;
                            ios = null;
                            copy.close(); 
                        }                                               
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLogDir() {
        return logPath;
    }

    public static String getLogDirSingleton() {
        return instance.logPath;
    }
    
    public static boolean changeLogSingleton() {
        return instance.changeLog();
    }

    /**
     * @return
     */
    public boolean changeLog() {
        logPath = logBaseDir + fmt.format(new Date());
        
        File outputDir = new File(logPath);
        outputDir.mkdirs();
        FileUtil.copyFile("conf/messages/IMC.xml", outputDir.getAbsolutePath() + "/IMC.xml");

        IMCOutputStream iosTmp = null;
        try {
            iosTmp = new IMCOutputStream(new FileOutputStream(new File(outputDir, "Data.lsf")));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (iosTmp != null) {
            synchronized (this) {
                if (ios != null) {
                    try {
                        ios.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                ios = iosTmp;    
            }            
            return true;
        }
        return false;
    }

    private static LsfMessageLogger getInstance() {
        if (instance == null)
            instance = new LsfMessageLogger();

        return instance;
    }

    private synchronized boolean logMessage(IMCMessage msg) {
        try {
            if (ios != null)
                ios.writeMessage(msg);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean log(IMCMessage msg) {
        return getInstance().logMessage(msg);
    }
}
