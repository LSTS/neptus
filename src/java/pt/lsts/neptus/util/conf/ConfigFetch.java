/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 2005/01/17 (came from VIDOP Project)
 */
package pt.lsts.neptus.util.conf;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.output.OutputMonitor;

/**
 * @author Paulo Dias <pdias@fe.up.pt>
 * @version 1.3.10
 */
public class ConfigFetch {
    /**
     * This enum provides info if Neptus is running from jars or 
     * by a development environment (e.g. Eclipse).
     */
    public enum Environment {
        PRODUCTION,
        DEVELOPMENT
    }

    /** 
     * This is the static instance of the {@link ConfigFetch}.
     * To be initialized call first {@link ConfigFetch}{@link #initialize()}
     * or {@link ConfigFetch}{@link #initialize(String)}.
     */
    public static ConfigFetch INSTANCE = null;

    /** This hold the information if Neptus is running from jars or development environment. */
    private static Environment runEnvironment = Environment.DEVELOPMENT;

    /** This is the directory separator from {@link System}.getProperty("file.separator"). */
    public static final String DS = System.getProperty("file.separator", "/");

    /** To hold the list of XML Schemas used in Neptus. */
    private static final Hashtable<String, String> listOfSchemas = new Hashtable<>();
    /** To hold the loaded XML Schemas used in Neptus. */
    private static final Hashtable<String, String> listOfSchemasPaths = new Hashtable<>();
    static {
        DateTimeUtil.getUID();
        listOfSchemas.put("mission", "schemas/neptus-mission.xsd");
        listOfSchemas.put("vehicle", "schemas/neptus-vehicle.xsd");
        listOfSchemas.put("map", "schemas/neptus-map.xsd");
        listOfSchemas.put("messages", "schemas/neptus-messages.xsd");
        listOfSchemas.put("misc-systems", "schemas/neptus-misc-systems.xsd");
        listOfSchemas.put("missionWeb", "schemas/mwebschema.xsd");
        listOfSchemas.put("checklist", "schemas/neptus-checklist.xsd");
        listOfSchemas.put("console", "schemas/neptus-console.xsd");
        listOfSchemas.put("coordinateSystems", "schemas/neptus-coordinateSystems.xsd");
        listOfSchemas.put("maneuvers", "schemas/neptus-maneuvers.xsd");
        listOfSchemas.put("textures", "schemas/neptus-textures.xsd");
        listOfSchemas.put("types", "schemas/neptus-types.xsd");
    }

    /** The resource path for the version info (inside main jar. */
    private static final String VERSION_FILE_NAME = "/version.txt";
    /** The resource path for the extended version info (inside main jar. */
    private static final String VERSION__EXTENDED_FILE_NAME = "/info";

    /** A list of base file paths */
    private static final String CONFIG_FILE_NAME = "neptus-config.xml";
    private static final String MISSION_BASE_FOLDER = "missions";
    private static final String CONF_BASE_FOLDER = "conf";
    private static final String CONSOLES_BASE_FOLDER = CONF_BASE_FOLDER + "/" + "consoles";
    private static final String LOG_BASE_FOLDER = "log";
    private static final String LOG_DOWNLOADED_BASE_FOLDER = LOG_BASE_FOLDER + "/" + "downloaded";
    private static final String MAP_BASE_FOLDER = "maps";
    private static final String VEHICLES_BASE_FOLDER = "vehicles-defs";

    /** DOM4J Doc to hold the configuration. */
    private static Document confDoc = DocumentHelper.createDocument();
    /** The configuration file name. */
    private static String configFile = CONFIG_FILE_NAME;

    /** The base jar file folder, initialized on {@link #init()}. Will be used for path resolver. */
    private static String baseJarFileDir = ".";

    /** Holds if Neptus is on lock mode. Use {@link #getDistributionType()} instead. */
    private static boolean onLockedMode = false;
    
    /** Holds the type of distribution used. See {@link NeptusProperty.DistributionEnum}. */
    private static NeptusProperty.DistributionEnum distributionType = DistributionEnum.DEVELOPER;
    /** Control variable to only set the {@link #distributionType} once. */
    private static boolean distributionSetChangedOrGet = false;

    /** This is the created temporary folder available for this instance of Neptus. */
    private static final String neptusTmpDir = System.getProperty("java.io.tmpdir", "tmp") + DS
            + NameNormalizer.getRandomID("neptus");

    /** Initialize the Neptus temporary folder. */
    static {
        try {
            File ntd = new File(neptusTmpDir);
            ntd.deleteOnExit();
            ntd.mkdir();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String[] parentPaths = {
            ".", // Needed to load the workspace on Webstart
            // TODO This can trigger a SecurityException
            System.getProperty("user.home", ".") + DS + ".neptus", System.getProperty("user.dir", "."),
            ".." + DS + "classes", ".." + DS + "config", ".." + DS + "conf", ".." + DS + "files", ".." + DS + "images",
            ".." + DS + "..", "..", System.getProperty("user.home", ".") };

    /** This is to hold the current parent (the console or MRA visible). */
    private static Component superParentFrame = null;

    private static Hashtable<String, String> params = null;

    private static boolean alreadyInitialized = false;

    /**
     * Simple constructor (using as config file name: "neptus-config.xml") that loads the configuration file.
     */
    private ConfigFetch() {
        this(configFile);
    }

    /**
     * Base constructor
     * 
     * @param configFile Configuration file name
     */
    private ConfigFetch(String configFile) {
        // Set Environment
        if (ConfigFetch.class.getResource("/version.txt").toString().startsWith("jar:")) {
            runEnvironment = Environment.PRODUCTION;
        }
        
        OutputMonitor.grab();
        NeptusLog.init();
        
        // Set Default Exception Handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> NeptusLog.pub().error("Uncaught Exception! " + ReflectionUtil.getCallerStamp(), e));

        init();
        loadSchemas();
        String fxt = resolvePath(configFile);
        NeptusLog.pub().info("Config. file found in: " + fxt);
        if (fxt != null)
            ConfigFetch.configFile = fxt;

        GeneralPreferences.initialize();

        load();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtil.deltree(neptusTmpDir);
                OutputMonitor.end();
            }
        }));
    }

    /**
     * @see ConfigFetch#ConfigFetch()
     * @return
     */
    public static boolean initialize() {
        if (INSTANCE != null)
            return true;

        try {
            INSTANCE = new ConfigFetch();
        }
        catch (RuntimeException e) {
            NeptusLog.pub().error(e.getStackTrace());
            return false;
        }
        return true;
    }

    /**
     * @see ConfigFetch#ConfigFetch(String)
     * @return
     */
    public static boolean initialize(String configFile) {
        if (INSTANCE != null)
            return true;

        try {
            INSTANCE = new ConfigFetch(configFile);
        }
        catch (RuntimeException e) {
            NeptusLog.pub().error(e.getStackTrace());
            return false;
        }
        return true;
    }

    /**
     * This is basically to be used internally. This method tries to find the directory where this class resides (or the
     * jar file resides).
     * 
     * @return true if it's successful
     */
    private boolean init() {
        if (alreadyInitialized)
            return true;
        alreadyInitialized = true;

        File fxTmpDir = new File(getNeptusTmpDir());
        fxTmpDir.mkdirs();
        fxTmpDir.deleteOnExit();

        try {
            String classPackageFilePath = ConfigFetch.class.getPackage().getName().replace('.', '/');
            String classNameFilePath = ConfigFetch.class.getTypeName().replace('.', '/') + ".class";
            
            String inFileName = ConfigFetch.class.getResource("/" + classNameFilePath).getFile();

            String strNeptusVersion = "Starting Neptus " + getVersionSimpleString() + " ...";
            String strJavaVersion = "Using Java from: " + System.getProperty("java.vendor") + " | Version: "
                    + System.getProperty("java.version");
            String strOSVersion = "On OS: " + System.getProperty("os.name") + " | Version: "
                    + System.getProperty("os.version") + " | Arch.: " + System.getProperty("os.arch");

            NeptusLog.pub().info(strNeptusVersion);
            NeptusLog.pub().info(strJavaVersion);
            NeptusLog.pub().info(strOSVersion);

            String strNeptusExtendedVersionInfo = getVersionExtendedInfoSimpleString();
            if (strNeptusExtendedVersionInfo != null && strNeptusExtendedVersionInfo.length() > 0) {
                String str = "SCM Extended Info:\n";
                str += strNeptusExtendedVersionInfo;
                NeptusLog.pub().info(str);
            }

            NeptusLog.pub().debug("Path to ConfigFetch class: " + inFileName);

            int lind = inFileName.lastIndexOf(".jar");
            if (lind != -1)
                inFileName = inFileName.substring(0, lind);

            lind = inFileName.lastIndexOf(classPackageFilePath + "/");
            if (lind != -1)
                inFileName = inFileName.substring(0, lind);
            lind = inFileName.lastIndexOf("/");
            if (lind != -1)
                inFileName = inFileName.substring(0, lind);
            if (inFileName.startsWith("file:"))
                inFileName = inFileName.replaceFirst("file:", "");
            inFileName = URLDecoder.decode(inFileName, "UTF-8");
            baseJarFileDir = inFileName;
            NeptusLog.pub().debug("Base dir. for the search: " + baseJarFileDir);
        }
        catch (Exception any) {
            NeptusLog.pub().fatal("Error in finding an initial base dir.", any);
            return false;
        }

        if (baseJarFileDir.equals("")) {
            NeptusLog.pub().fatal("Error finding an initial base dir.");
            return false;
        }

        return true;
    }

    /**
     * Loads the configuration file. Also configures the log4j.
     * 
     * @return true if successful
     */
    private boolean load() {
        return load(configFile);
    }

    /**
     * Loads the configuration file. Also configures the log4j.
     * 
     * @param configFile Configuration file name
     * @return true if successful
     */
    private boolean load(String configFile) {
        String fxt = resolvePath(configFile);
        if (fxt == null) {
            fxt = CONFIG_FILE_NAME;
        }
        else {
            if (!fxt.contains("build") && !fxt.contains("classes"))
                ConfigFetch.configFile = fxt;
        }
        confDoc = readConfigFile(fxt);

        // IMC Local ID
        try {
            initializeLocalImcId();
            GeneralPreferences.saveProperties();
        }
        catch (NumberFormatException e) {
            NeptusLog.pub().warn("Setting CCU Local IMC ID error", e);
        }
        finally {
            GeneralPreferences.warnPreferencesListeners();
        }
        return true;
    }

    /**
     * Sets the local IMC ID using the IP of the machine.
     * The name will be CCU plus "user.name" and the last 2 bytes of the IP.
     * The ID is the last 2 bytes of the IP logically and with 0x1FFF and then
     * logically or with 0x4000.
     */
    private void initializeLocalImcId() {
        String hostadr;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            hostadr = addr.getHostAddress();
        }
        catch (Exception e1) {
            e1.printStackTrace();
            hostadr = "127.0.0.1";
        }
        NeptusLog.pub().debug("Using initial option for IMC ID is '" + hostadr + "'");
        if (OsInfo.getName() == OsInfo.Name.LINUX) {
            try {
                Enumeration<NetworkInterface> netInt = NetworkInterface.getNetworkInterfaces();
                while (netInt.hasMoreElements()) {
                    NetworkInterface ni = netInt.nextElement();
                    Enumeration<InetAddress> iAddress = ni.getInetAddresses();
                    while (iAddress.hasMoreElements()) {
                        InetAddress ia = iAddress.nextElement();
                        if (!ia.isLoopbackAddress()) {
                            if (ia instanceof Inet4Address) {
                                String msg = "Changing initial option for IMC ID from '" + hostadr + "' to '";
                                hostadr = ia.getHostAddress();
                                msg += hostadr + "'";
                                NeptusLog.pub().debug(msg);
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getStackTrace());
            }
        }
        String[] sl2 = hostadr.split("\\.");

        long idd = (Integer.parseInt(sl2[2]) << 8) + (Integer.parseInt(sl2[3]));
        ImcId16 newCcuId = new ImcId16((idd & 0x1FFF) | 0x4000); // 010????? CCU IDs
        GeneralPreferences.imcCcuId = newCcuId;
        GeneralPreferences.imcCcuName = "CCU " + System.getProperty("user.name") + " " + sl2[2] + "_" + sl2[3];
        NeptusLog.pub().debug("Using IMC ID " + newCcuId.toPrettyString() + " with name '" + GeneralPreferences.imcCcuName + "'");
    }
    
    /**
     * Tries to resolve a relative path. The searches are done in: &lt;ul&gt; &lt;li&gt;If it's absolute returns the
     * same path.&lt;/li&gt; &lt;li&gt;Looks in the path given by {@link #init()}.&lt;/li&gt; &lt;li&gt;Looks in one
     * level down from the path given by {@link #init()} .&lt;/li&gt; &lt;li&gt;Looks in the path given by
     * {@link #init()} + "../classes".&lt;/li&gt; &lt;li&gt;Looks in the path given by {@link #init()} +
     * "../config".&lt;/li&gt; &lt;li&gt;Looks in the path given by {@link #init()} + "../files".&lt;/li&gt;
     * &lt;li&gt;Looks in the path given by {@link #init()} + "../images".&lt;/li&gt; &lt;li&gt;Looks in two level down
     * from the path given by {@link #init()}.&lt;/li&gt; &lt;/ul&gt;
     * 
     * @since v1.3.5 is static
     * @param path Relative path to resolve.
     * @return true if successful
     */
    public static String resolvePath(String path) {
        File fx = new File(path);
        File fx1;

        try {
            NeptusLog.pub().debug("Trying to see if the Path exists...");
            if (fx.exists()) {
                NeptusLog.pub().debug("The Path exists! [" + path + "]");
                return path;
            }
            else {
                NeptusLog.pub()
                        .debug("Trying to see if the AbsolutePath exists... " + "[" + fx.getAbsoluteFile() + "]");
                fx1 = fx.getAbsoluteFile();
                if (!fx1.exists()) {
                    fx1 = resolvePathInner(path);
                    if (!fx1.exists()) {
                        return null;
                    }

                    try {
                        NeptusLog.pub().debug("The Path exists! [" + fx1.getCanonicalPath() + "]");
                        return fx1.getCanonicalPath();
                    }
                    catch (Exception any) {
                        NeptusLog.pub().debug("The Path exists! [" + fx1.getAbsolutePath() + "]");
                        return fx1.getAbsolutePath();
                    }

                }
            }
            NeptusLog.pub().debug("The file '" + path + "' was not found in this system.");
            return null;
        }
        catch (Exception any) {
            NeptusLog.pub().debug(any, any);
            return null;
        }
    }

    /**
     * @since v1.3.5 is static
     * @param path
     * @return
     */
    private static File resolvePathInner(String path) {
        String rootSolver = baseJarFileDir;
        File fx1;

        for (String parentPath : parentPaths) {
            if ((new File(parentPath)).isAbsolute())
                fx1 = (new File(parentPath + DS + path)).getAbsoluteFile();
            else
                fx1 = (new File(rootSolver + DS + parentPath + DS + path)).getAbsoluteFile();
            NeptusLog.pub().debug("Trying to find Path in " + fx1.getAbsolutePath() + "...");
            if (fx1.exists())
                return fx1;
        }
        return null;
    }

    /**
     * Resolves a path based on a parent or the original path if it's alredy absolute.<br/>
     * The file may not exist! The parentPath is suposed to exist.<br/>
     * It returns always the absolute path.
     * 
     * @param parentPath
     * @param filePath
     * @return
     */
    public static String resolvePathWithParent(String parentPath, String filePath) {
        File fx = new File(filePath);
        File fxParent = new File(parentPath);

        if (fx.isAbsolute()) {
            return fx.getAbsolutePath();
        }
        else {
            if (fxParent.exists()) {
                String parent;
                if (fxParent.isDirectory())
                    parent = fxParent.getAbsolutePath();
                else
                    parent = fxParent.getAbsoluteFile().getParent();
                File fx1 = new File(parent + "/" + filePath).getAbsoluteFile();
                if (fx1.isAbsolute()) {
                    try {
                        return fx1.getCanonicalPath();
                    }
                    catch (IOException e) {
                        return fx1.getAbsolutePath();
                    }
                }
            }
        }
        return filePath;
    }

    /**
     * Resolves a path based on a parent or the original path if it's already absolute. The parent used here is the
     * {@link #getConfigFile()}
     * 
     * @see {@link #resolvePathWithParent(String, String)}
     * @param filePath
     * @return
     */
    public static String resolvePathBasedOnConfigFile(String filePath) {
        return resolvePathWithParent(ConfigFetch.getConfigFile(), filePath);
    }

    /**
     * @return The user home folder.
     */
    public static String getUserHomeFolder() {
        return System.getProperty("user.home");
    }
    
    /**
     * @return Returns the configFile path.
     */
    public static String getConfigFile() {
        return configFile;
    }

    /**
     * @return The config folder path.
     */
    public static String getConfFolder() {
        return resolvePathBasedOnConfigFile(CONF_BASE_FOLDER);
    }

    /**
     * @return The missions folder path.
     */
    public static String getMissionsFolder() {
        return resolvePathBasedOnConfigFile(MISSION_BASE_FOLDER);
    }

    /**
     * @return The consoles folder path.
     */
    public static String getConsolesFolder() {
        return resolvePathBasedOnConfigFile(CONSOLES_BASE_FOLDER);
    }

    /**
     * @return The logs folder path.
     */
    public static String getLogsFolder() {
        return resolvePathBasedOnConfigFile(LOG_BASE_FOLDER);
    }

    /**
     * @return The logs downloaded folder path.
     */
    public static String getLogsDownloadedFolder() {
        return resolvePathBasedOnConfigFile(LOG_DOWNLOADED_BASE_FOLDER);
    }

    /**
     * @return The maps folder path.
     */
    public static String getMapsFolder() {
        return resolvePathBasedOnConfigFile(MAP_BASE_FOLDER);
    }

    /**
     * @return The vehicle defs folder path.
     */
    public static String getVehiclesDefsFolder() {
        return resolvePathBasedOnConfigFile(VEHICLES_BASE_FOLDER);
    }

    /**
     * 
     * @param xpath2Element
     * @param doDocument
     * @return
     */
    private static String getElementTextByXPath(String xpath2Element, Document doDocument) {
        String entity = null;

        XPath xpathSelector = DocumentHelper.createXPath(xpath2Element);
        List<?> results = xpathSelector.selectNodes(doDocument);
        for (Object result : results) {
            entity = ((Element) result).getTextTrim();
        }
        return entity;
    }

    /**
     * Reads the configuration file.
     * @param configFile
     * @return
     */
    private Document readConfigFile(String configFile) {
        Document ret = DocumentHelper.createDocument();

        File file = new File(configFile);
        if (file.exists()) {
            try {
                SAXReader reader = new SAXReader();
                ret = reader.read(file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (ConfigFetch.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME) != null) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(ConfigFetch.class.getClassLoader()
                        .getResourceAsStream(CONFIG_FILE_NAME)));
                SAXReader reader = new SAXReader();
                ret = reader.read(br);
            }
            catch (Exception e) {
                NeptusLog.pub().error("Configuration file not found. " + e.getMessage());
            }
        }
        else {
            System.err.println("Sorry, configuration file not found.");
        }
        return ret;
    }

    /**
     * returns the central properties file for log4j Loggers. This file is expected to be xml or properties depending on
     * the attribute "type". <i>//log-conf-file</i>
     * 
     * @return
     */
    public static String getLoggingPropertiesLocation() {
        String loc = getElementTextByXPath("//log-conf-file", confDoc);
        if (loc != null) {
            String loc1 = resolvePath(loc);
            if (loc1 != null)
                return loc1;
            return loc;
        }
        else
            loc = "conf/log4j2.xml";
        return loc;
    }

    /**
     * @return The CoordinateSystems file def. (Will be deprecated in near future).
     */
    public static String getCoordinateSystemsConfigLocation() {
        String loc = getElementTextByXPath("//coordinate-systems-conf-file", confDoc);
        if (loc != null) {
            String loc1 = resolvePath(loc);
            if (loc1 != null)
                return loc1;
            return loc;
        }
        else
            loc = "conf/neptus-coordinateSystems.xml";
        return loc;
    }

    /**
     * @return The ordered list of vehicle defs. 
     */
    public static LinkedList<String> getVehiclesList() {
        LinkedList<String> result = new LinkedList<>();
        Element elem;
        String xpath2Element = "//vehicles-base-path";
        Document doDocument = confDoc;
        XPath xpathSelector = DocumentHelper.createXPath(xpath2Element);
        Node node = xpathSelector.selectSingleNode(doDocument);

        String baseDir;
        if (node == null) {
            baseDir = "vehicles-defs";
        }
        else {
            elem = (Element) node;
            baseDir = elem.getTextTrim();
        }

        baseDir = ConfigFetch.resolvePath(baseDir);
        File fxDir = new File(baseDir);
        if (!fxDir.isDirectory())
            return result;
        File[] filesVeh = fxDir.listFiles(f -> {
            if (f.isDirectory())
                return false;

            String extension = FileUtil.getFileExtension(f);
            return extension != null && (extension.equals("xml") || extension.equals(FileUtil.FILE_TYPE_VEHICLE));
        });

        // To sort the list (in Windows this is automatic, in Linux we need this)
        Arrays.sort(filesVeh);

        for (File fx1 : filesVeh) {
            String path;
            try {
                path = fx1.getCanonicalPath();
            }
            catch (IOException e) {
                path = fx1.getAbsolutePath();
            }
            result.add(path);
        }
        return result;
    }

    /**
     * @return The Mission XML Schema.
     */
    public static String getMissionSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("mission");
    }

    /**
     * @return The Vehicle XML Schema.
     */
    public static String getVehicleSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("vehicle");
    }

    /**
     * @return The Map XML Schema.
     */
    public static String getMapSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("map");
    }

    /**
     * @return The CoordinateSystem XML Schema.
     */
    public static String getCoordinateSystemSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("coordinateSystems");
    }

    /**
     * @return The Checklist XML Schema.
     */
    public static String getChecklistSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("checklist");
    }

    /**
     * @return The Console XML Schema.
     */
    public static String getConsoleSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("console");
    }

    /**
     * @return The {@link #VERSION_FILE_NAME} as properties.
     */
    private static Properties getVersionInfoAsProperties() {
        Properties prop = new Properties();
        InputStream ist = ConfigFetch.class.getResourceAsStream(VERSION_FILE_NAME);

        if (ist != null) {
            try {
                prop.load(ist);
            }
            catch (IOException e) {
                NeptusLog.pub().debug(e);
            }
        }
        return prop;
    }

    /**
     * @return A simple String text with version and date of Neptus.
     */
    public static String getVersionSimpleString() {
        Properties prop = new Properties();
        String versionString = " ";
        InputStream ist = ConfigFetch.class.getResourceAsStream(VERSION_FILE_NAME);

        if (ist != null) {
            try {
                prop.load(ist);
                versionString += prop.getProperty("VERSION", "");
                versionString += " (";
                versionString += prop.getProperty("DATE", "");
                versionString += ", g";
                versionString += prop.getProperty("SCM_REV", "?");
                versionString += ")";
            }
            catch (IOException e) {
                NeptusLog.pub().debug(e);
            }
        }
        return versionString;
    }

    /**
     * @return The loaded {@link #VERSION__EXTENDED_FILE_NAME} as String. 
     */
    private static String getVersionExtendedInfoSimpleString() {
        String versionString = "";
        InputStream ist = ConfigFetch.class.getResourceAsStream(VERSION__EXTENDED_FILE_NAME);

        if (ist != null) {
            try {
                versionString = StreamUtil.copyStreamToString(ist);
            }
            catch (Exception e) {
                NeptusLog.pub().debug(e);
            }
        }
        return versionString;
    }

    /**
     * The comment text to be put on XML or other files saved.
     * Holds a date time and version of Neptus. 
     * @return The comment text.
     */
    public static String getSaveAsCommentForXML() {
        Properties prop = getVersionInfoAsProperties();
        Date trialTime = new Date();
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.0Z'");
        String ret = "Data saved on " + dateFormater.format(trialTime) + " with Neptus version "
                + prop.getProperty("VERSION", "") + " (compiled on " + prop.getProperty("DATE", "") + ", g"
                + prop.getProperty("SCM_REV", "?") + ").";
        return " " + ret + " ";
    }

    /**
     * This return the current parent (the console or MRA visible).
     * This should only be used if no other means to get a valid parent can be used
     * @return Returns the superParentFrame.
     */
    public static Component getSuperParentFrame() {
        return superParentFrame;
    }

    /**
     * This return the current parent (the console or MRA visible) as {@link Frame}
     * (if not possible return a new {@link Frame}).
     * This should only be used if no other means to get a valid parent can be used
     * @return Returns the superParentFrame.
     */
    public static Frame getSuperParentAsFrame() {
        Component component = superParentFrame;

        if (component instanceof JFrame)
            return (Frame) component;
        else if (component instanceof JDialog)
            return (Frame) component;

        return new Frame();
    }

    /**
     * This is to set the current parent (the console or MRA visible).
     * Only if none is already set.
     * @param superParentFrame The superParentFrame to set.
     */
    public static void setSuperParentFrame(Component superParentFrame) {
        if (ConfigFetch.superParentFrame == null)
            ConfigFetch.superParentFrame = superParentFrame;
    }

    /**
     * This is to set the current parent (the console or MRA visible).
     * @param superParentFrame The superParentFrame to set.
     */
    public static void setSuperParentFrameForced(Component superParentFrame) {
        ConfigFetch.superParentFrame = superParentFrame;
    }

    /** This will parse the version file. */
    private static void parseVersionFile() {
        if (params != null)
            return;

        params = new Hashtable<>();

        BufferedReader br = new BufferedReader(new InputStreamReader(ConfigFetch.class.getClassLoader()
                .getResourceAsStream("version.txt")));
        try {
            String line = br.readLine();

            while (line != null) {
                StringTokenizer st = new StringTokenizer(line, "= ");
                if (st.countTokens() == 2) {
                    params.put(st.nextToken().toLowerCase(), st.nextToken());
                }
                line = br.readLine();
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    /**
     * @return The Neptus version.
     */
    public static String getNeptusVersion() {
        parseVersionFile();
        return params.get("version");
    }

    /**
     * @return The compilation date.
     */
    public static String getCompilationDate() {
        parseVersionFile();
        return params.get("date");
    }

    /**
     * @return The SCM revision.
     */
    public static String getScmRev() {
        parseVersionFile();
        return params.get("scm_rev");
    }

    /**
     * @return The current Neptus temporary folder.
     */
    public static String getNeptusTmpDir() {
        return neptusTmpDir;
    }

    /**
     * Return if is on lock mode. Use {@link #getDistributionType()} instead.
     * @return
     */
    public static boolean isOnLockedMode() {
        return onLockedMode;
    }

    /**
     * Sets if is on lock mode. Use {@link #getDistributionType()} instead.
     * @param onLockedMode
     */
    public static void setOnLockedMode(boolean onLockedMode) {
        ConfigFetch.onLockedMode = onLockedMode;
    }

    /**
     * This will load the schemas from {@link #listOfSchemas} into {@link #listOfSchemasPaths}.
     */
    private static void loadSchemas() {
        for (String key : listOfSchemas.keySet()) {
            String name = listOfSchemas.get(key);
            InputStream inStream = ConfigFetch.class.getResourceAsStream("/" + name);
            if (inStream != null) {
                File dirBase = new File(getNeptusTmpDir());
                dirBase.mkdirs();
                File outFile = new File(dirBase, name);
                outFile.getParentFile().mkdirs();
                outFile.deleteOnExit();
                StreamUtil.copyStreamToFile(inStream, outFile);
                listOfSchemasPaths.put(key, outFile.getAbsolutePath());
            }
            else {
                String loc1 = resolvePath(name);
                listOfSchemasPaths.put(key, loc1);
            }
        }
    }

    /**
     * @return a list of {@link Image}s to be set to frames.
     */
    public static List<Image> getIconImagesForFrames() {
        ArrayList<Image> imageList = new ArrayList<>();
        imageList.add(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon.png")));
        imageList.add(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon1.png")));
        imageList.add(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon2.png")));
        return imageList;
    }

    /**
     * Return the information if Neptus is running from jars or development environment.
     * @return the runEnvironment
     */
    public static Environment getRunEnvironment() {
        return runEnvironment;
    }
    
    /**
     * Return the type of distribution used. See {@link NeptusProperty.DistributionEnum}
     * @return the distributionType
     */
    public static NeptusProperty.DistributionEnum getDistributionType() {
        distributionSetChangedOrGet = true;
        return distributionType;
    }

    /**
     * Sets the type of distribution used. See {@link NeptusProperty.DistributionEnum}
     * Only one set is possible.
     * @param dist
     * @return true if the change happened or not.
     */
    public static boolean setDistributionType(NeptusProperty.DistributionEnum dist) {
        if (distributionSetChangedOrGet)
            return false;

        distributionType = dist;
        distributionSetChangedOrGet = true;
        return true;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        NeptusLog.pub().info(ConfigFetch.getLoggingPropertiesLocation());

        String st = ConfigFetch.getConfigFile();
        String st1 = ConfigFetch.resolvePathWithParent(st, "../fe.txt");
        NeptusLog.pub().info(st.concat("\n").concat(st1));
        st1 = ConfigFetch.resolvePathWithParent(st, "c:/fe.txt");
        NeptusLog.pub().info(st.concat("\n").concat(st1));
    }
}
