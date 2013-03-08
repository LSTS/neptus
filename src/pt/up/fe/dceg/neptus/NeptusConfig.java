/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 18, 2012
 * $Id:: NeptusConfig.java 9615 2012-12-30 23:08:28Z pdias                      $:
 */
package pt.up.fe.dceg.neptus;

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
                + ", r" + versionInfo.getProperty("SVN_REV", "?") + ").";
        return " " + ret + " ";
    }
    
    /**
     * Version info as string for general purposes 
     * @return may have empty values in dev environment
     */
    public String versionAsString(){
        String versionString = " ";
        versionString += versionInfo.getProperty("VERSION", "");
        versionString += " (";
        versionString += versionInfo.getProperty("DATE", "");
        versionString += ", r";
        versionString += versionInfo.getProperty("SVN_REV", "?");
        versionString += ")";
        return versionString;
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
