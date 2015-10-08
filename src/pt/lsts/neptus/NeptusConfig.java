/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Hugo Dias
 * Oct 18, 2012
 */
package pt.lsts.neptus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * @author Hugo
 * 
 */
public class NeptusConfig {
    public static final String DS = System.getProperty("file.separator", "/");
    public static final String FILE_TYPE_VEHICLE = "nvcl";

    private final File tempDir;
    private Map<String, File> schemas = new HashMap<String, File>();
    private PropertiesConfiguration config;
    private Properties versionInfo = new Properties();

    public NeptusConfig(PropertiesConfiguration config) {
        tempDir = new File(System.getProperty("java.io.tmpdir") + NeptusConfig.DS + "Neptus_" + new Date().getTime());
        tempDir.mkdirs();
        // load config properties file
        this.config = config;
        this.loadVersionFile();
    }

    public NeptusConfig loadSchemas() {
        Iterator<?> keys = config.getKeys("schemas");
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String path = config.getString(key);
            InputStream in = NeptusConfig.class.getResourceAsStream("/" + path);
            if (in != null) {
                File file = new File(tempDir, key);
                OutputStream out;
                try {
                    out = FileUtils.openOutputStream(file);
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();
                    schemas.put(key, file);
                }
                catch (IOException e) {
                    NeptusLog.pub().error("error loading schema : " + path);
                }
            }
            else {
                NeptusLog.pub().error("error opening stream for schema : " + path);
            }
        }
        return this;
    }

    public NeptusConfig setupLog() {
        NeptusLog.extendedLog = config.getBoolean("core.extended-log");
        return this;
    }

    public String getVehiclesPath() {
        return this.config.getString("core.vehicles-path");
    }

    /**
     * Load the generated version file exception is catched here because the file may not exist in dev environment
     */
    private void loadVersionFile() {
        InputStream in = this.getClass().getResourceAsStream(config.getString("core.version-file"));
        versionInfo = new Properties();
        try {
            versionInfo.load(in);
        }
        catch (IOException e) {
            NeptusLog.pub().error("error loading version file", e);
        }
    }

    /**
     * Version info
     * 
     * @return the returned properties object maybe be empty in dev environment its better to have default values when
     *         getting properties
     */
    public Properties version() {
        return versionInfo;
    }

    /**
     * Version info 
     * @return  returns version info for xml comment header 
     */
    public String versionForXml() {
        Date trialTime = new Date();
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.0Z'");
        String ret = "Data saved on " + dateFormater.format(trialTime) + " with Neptus version "
                + versionInfo.getProperty("VERSION", "") + " (compiled on " + versionInfo.getProperty("DATE", "")
                + ".";
        return " " + ret + " ";
    }
    
    public void shutdown() {
        try {
            FileUtils.deleteDirectory(tempDir);
        }
        catch (IOException e) {
            NeptusLog.pub().error("error deleting temp folder");
        }
    }
}
