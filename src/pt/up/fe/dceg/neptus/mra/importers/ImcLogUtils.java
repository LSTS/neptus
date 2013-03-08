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
 * Oct 30, 2011
 * $Id:: ImcLogUtils.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.mra.importers;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.IMCMessage;

/**
 * @author zp
 *
 */
public class ImcLogUtils {

    public static LinkedHashMap<Integer, String> getEntityList(IMraLogGroup log) {
        LinkedHashMap<Integer, String> entityList = new LinkedHashMap<Integer, String>();
        
        IMraLog mlog = log.getLog("EntityInfo");
        
        if (mlog != null) {
            IMCMessage msg;
            while ((msg = mlog.nextLogEntry()) != null)
                entityList.put(msg.getInteger("id"), msg.getString("label"));
        }       
        return entityList;
    }
    
    public static LinkedHashMap<String, Integer> getEntityListReverse(IMraLogGroup log) {
        LinkedHashMap<String, Integer> entityList = new LinkedHashMap<String, Integer>();        
        
        IMraLog mlog = log.getLog("EntityInfo");
        
        if (mlog != null) {
            IMCMessage msg;
            while ((msg = mlog.nextLogEntry()) != null)
                entityList.put(msg.getString("label"), msg.getInteger("id"));
        }       
        return entityList;                
    }
}
