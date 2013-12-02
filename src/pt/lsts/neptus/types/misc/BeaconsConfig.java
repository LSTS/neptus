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
 * Author: meg
 * Nov 14, 2013
 */
package pt.lsts.neptus.types.misc;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import pt.lsts.imc.LblBeacon;
import pt.lsts.neptus.util.PropertiesLoader;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Margarida
 * 
 */
public class BeaconsConfig {
    private static Vector<PropertiesLoader> confsInFile;
    static {
        try {
            confsInFile = new Vector<PropertiesLoader>();
            PropertiesLoader propConf;
            String confsPath = ConfigFetch.resolvePath("maps/");
            File folder = new File(confsPath);
            File[] listOfFiles = folder.listFiles();
            for (int f = 0; f < listOfFiles.length; f++) {
                propConf = new PropertiesLoader(listOfFiles[f].getAbsolutePath(), PropertiesLoader.PROPERTIES);
                // private void fixPropertiesConfFormat() {
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
                confsInFile.add(propConf);
                // }
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    static public PropertiesLoader getMatchingConf(LblBeacon beacon) {
        int countMatching = 0;
        for (PropertiesLoader propConf : confsInFile) {
            if (propConf.getProperty("interrogation channel").equals(beacon.getQueryChannel()))
                countMatching++;
            if (propConf.getProperty("reply channel").equals(beacon.getReplyChannel()))
                countMatching++;
            if (propConf.getProperty("transponder delay (msecs.)").equals(beacon.getTransponderDelay()))
                countMatching++;
            if (countMatching == 3) {
                return propConf;
            }
        }
        return null;
    }

}
