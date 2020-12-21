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
 * Author: Paulo Dias
 * 26 de Jan de 2013
 */
package pt.lsts.neptus.types.map;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashMap;

import pt.lsts.imc.LblBeacon;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.PropertiesLoader;

/**
 * @author pdias
 *
 */
public class TransponderUtils {

    private static LinkedHashMap<String, PropertiesLoader> transpondersConfList = new LinkedHashMap<>(); 

    static {
        final ArrayList<String> aTranspondersFiles = new ArrayList<>();
        final ArrayList<PropertiesLoader> aConfsInFile = new ArrayList<>();
        
        File dir = new File("maps/");
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                // NeptusLog.pub().info("<###> "+name + ": " +
                // name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z][A-Za-z\\-_0-9]+\\.conf)$"));
                if (name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z][A-Za-z0-9\\-\\_]*\\.conf)$")) {
                    return true;
                }
                return false;
            }
        });
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.getName().startsWith("lsts") && !o2.getName().startsWith("lsts"))
                    return -1;
                else if (!o1.getName().startsWith("lsts") && o2.getName().startsWith("lsts"))
                    return 1;
                return o1.compareTo(o2);
            }
        });
        
        for (File file : files) {
            // NeptusLog.pub().info("<###> "+file.getName());
            PropertiesLoader propConf = new PropertiesLoader(file.getAbsolutePath(), PropertiesLoader.PROPERTIES);
            Hashtable<String, String> fixedValues = new Hashtable<String, String>();
            for (Object keyO : propConf.keySet()) {
                String key = (String) keyO;
                String value = propConf.getProperty(key);
                String[] vs = value.split("=");
                if (vs.length > 1) {
                    key = key + " " + vs[0];
                    value = vs[1];
                }
                fixedValues.put(key, value);
            }
            propConf.clear();
            propConf.putAll(fixedValues);
            
            aTranspondersFiles.add(file.getName());
            aConfsInFile.add(propConf);
        }
        
        for (int i = 0; i < aTranspondersFiles.size(); i++) {
            transpondersConfList.put(aTranspondersFiles.get(i), aConfsInFile.get(i));
        }
    }

    /**
     * @return the transpondersConfsListArray
     */
    public static String[] getTranspondersConfsNamesList() {
        return transpondersConfList.keySet().toArray(new String[transpondersConfList.size()]);
    }
    
    static public PropertiesLoader getMatchingConf(LblBeacon beacon) {
        String filename = beacon.getBeacon()+".conf";
        return transpondersConfList.get(filename);
//        
//        for (PropertiesLoader propConf : transpondersConfList.values().toArray(new PropertiesLoader[transpondersConfList.size()])) {
//            countMatching = 0;
//            short prop = Short.parseShort(propConf.getProperty("interrogation channel"));
//            if (prop == beacon.getQueryChannel())
//                countMatching++;
//            prop = Short.parseShort(propConf.getProperty("reply channel"));
//            if (prop == beacon.getReplyChannel())
//                countMatching++;
//            prop = Short.parseShort(propConf.getProperty("transponder delay (msecs.)"));
//            if (prop == beacon.getTransponderDelay())
//                countMatching++;
//            if (countMatching == 3) {
//                return propConf;
//            }
//        }
//        return null;
    }

    
    /**
     * @param transp
     * @return
     */
    public static LblBeacon getTransponderAsLblBeaconMessage(TransponderElement transp) {
        LocationType tPos = transp.getCenterLocation();
        LocationType absLoc = tPos.getNewAbsoluteLatLonDepth();

        LblBeacon msgLBLBeaconSetup = new LblBeacon();
        msgLBLBeaconSetup.setBeacon(transp.getId());
        msgLBLBeaconSetup.setLat(absLoc.getLatitudeRads());
        msgLBLBeaconSetup.setLon(absLoc.getLongitudeRads());
        msgLBLBeaconSetup.setDepth(absLoc.getDepth());

        return msgLBLBeaconSetup;
    }
    
    /**
     *
     */
    public static ArrayList<TransponderElement> orderTransponders(ArrayList<TransponderElement> transponders) {
//        ArrayList<TransponderElement> tal = new ArrayList<TransponderElement>();
//        tal.addAll(Arrays.asList(transList));
        // Let us order the beacons in alphabetic order (case insensitive)
        Collections.sort(transponders, new Comparator<TransponderElement>() {
            @Override
            public int compare(TransponderElement o1, TransponderElement o2) {
                return o1.getId().compareToIgnoreCase(o2.getId());
            }
        });
//        return tal.toArray(new TransponderElement[tal.size()]);
        return transponders;
    }

    
}
