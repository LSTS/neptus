/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2005/01/17 (came from VIDOP Project)
 * $Id:: ConfigFetch.java 9903 2013-02-11 14:42:23Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.util.conf;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.NameNormalizer;
import pt.up.fe.dceg.neptus.util.StreamUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.output.OutputMonitor;

/**
 * @author Paulo Dias <pdias@fe.up.pt>
 * @version 1.3.9 10/2006
 */
public class ConfigFetch {
    private static boolean onLockedMode = false;

    public static final short OS_ERROR = -1;
    public static final short OS_WINDOWS = 0;
    public static final short OS_LINUX = 1;
    public static final short OS_OTHER = 9;
    /**
     * OS DIRECTORY SEPARATOR
     */
    public static final String DS = System.getProperty("file.separator", "/");
    
    public static final long STARTTIME = System.currentTimeMillis();
    public static long mark = System.currentTimeMillis();
    public static Map<String, Long> timings = new HashMap<String, Long>();
    protected static boolean schemasInTempFile = false;
    protected static final Hashtable<String, String> listOfSchemas = new Hashtable<String, String>();
    protected static final Hashtable<String, String> listOfSchemasPaths = new Hashtable<String, String>();
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

    private static final String VERSION_FILE_NAME = "/version.txt";

    protected static final String classPackage = "pt/up/fe/dceg/neptus/util/conf";
    protected static final String className = "ConfigFetch.class";

    private static final String CONFIG_FILE_NAME = "neptus-config.xml";

    protected static Document confDoc = DocumentHelper.createDocument();
    protected static String configFile = CONFIG_FILE_NAME;
    protected static String baseJarFileDir = ".";
    protected static boolean ifLogger = true;

    private static boolean distributionSetChangedOrGet = false;
    private static NeptusProperty.DistributionEnum distributionType = DistributionEnum.DEVELOPER;

    protected static final String neptusTmpDir = System.getProperty("java.io.tmpdir", "tmp") + DS
            + NameNormalizer.getRandomID("neptus");

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

    protected static String[] parentPaths = {
            ".", // Necessário para carregar o workspace no webstart
            // TODO Isto pode dar problemas se disparar uma SecurityException
            System.getProperty("user.home", ".") + DS + ".neptus", System.getProperty("user.dir", "."),
            ".." + DS + "classes",
            ".." + DS + "config", ".." + DS + "conf", ".." + DS + "files", ".." + DS + "images",
            ".." + DS + "..", "..",
            System.getProperty("user.home", ".") };

    protected static Component superParentFrame = null;

    private static Hashtable<String, String> params = null;

    public BufferedWriter bw, bwhale;

    private static boolean alreadyInitialized = false;

    private static long neptusInitializationTime;

    public static ConfigFetch INSTANCE = null;

    /**
     * Simple constructor (using as config file name: "neptus-config.xml") that loads the configuration file.
     */
    private ConfigFetch() {
        this(configFile, true);
    }

    /**
     * Simple constructor that loads the configuration file.
     * 
     * @param configFile Configuration file name
     * @param ifLog
     */
    private ConfigFetch(String configFile, boolean ifLog) {
        ifLogger = ifLog;
        neptusInitializationTime = System.currentTimeMillis();
        init();
        loadSchemas();
        String fxt = resolvePath(configFile);
        NeptusLog.pub().info("Config. file found in: " + fxt);
        if (fxt != null)
            ConfigFetch.configFile = fxt;
        
        GeneralPreferences.initialize();
        
        load();
        
        loadIMCDefinitions();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                FileUtil.deltree(neptusTmpDir);
            }
        }));

        NeptusLog.pub().info("Found OS: " + System.getProperty("os.name"));
    }
    
    /**
     * Simple constructor that loads the configuration file.
     * 
     * @param ifLog
     */
    private ConfigFetch(boolean ifLog) {
        this(configFile, ifLog);
    }

    /**
     * Simple constructor that loads the configuration file.
     * 
     * @param configFile Configuration file name
     */
    private ConfigFetch(String configFile) {
        this(configFile, true);
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
            return false;
        }
        return true;
    }

    /**
     * @see ConfigFetch#ConfigFetch(String, boolean)
     * @return
     */
    public static boolean initialize(String configFile, boolean ifLog) {
        if (INSTANCE != null)
            return true;

        try {
            INSTANCE = new ConfigFetch(configFile, ifLog);
        }
        catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    /**
     * @see ConfigFetch#ConfigFetch(boolean)
     * @return
     */
    public static boolean initialize(boolean ifLog) {
        if (INSTANCE != null)
            return true;

        try {
            INSTANCE = new ConfigFetch(ifLog);
        }
        catch (RuntimeException e) {
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
            return false;
        }
        return true;
    }

    /**
     * This is basicaly to be used internaly. This method tries to find the directoy where this class resides (or the
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

        OutputMonitor.grab();

        try {
            if (ifLogger) {
                try {
                    DOMConfigurator.configure(getLoggingPropertiesLocation());
                    NeptusLog.pub().debug("Log4J configured with a XML conf. file!");
                }
                catch (Error e) {
                    BasicConfigurator.configure();
                    NeptusLog.pub().warn("Could not configure Log4J with a default config, will try to load from configuration file!!");
                }
            }
            String inFileName = ConfigFetch.class.getResource("/" + classPackage + "/" + className).getFile();
            
            String strNeptusVersion = "Starting Neptus " + getVersionSimpleString() + " ...";
            String strJavaVersion = "Using Java from: " + System.getProperty("java.vendor") + " | Version: "
                    + System.getProperty("java.version");
            String strOSVersion = "On OS: " + System.getProperty("os.name") + " | Version: "
                    + System.getProperty("os.version") + " | Arch.: " + System.getProperty("os.arch");
            System.out.println(strNeptusVersion);
            System.out.println(strJavaVersion);
            System.out.println(strOSVersion + "\n");
            NeptusLog.pub().info(strNeptusVersion);
            NeptusLog.pub().info(strJavaVersion);
            NeptusLog.pub().info(strOSVersion);
            
            NeptusLog.pub().debug("Path to ConfigFetch class: " + inFileName);

            int lind = inFileName.lastIndexOf(".jar");
            if (lind != -1)
                inFileName = inFileName.substring(0, lind);

            lind = inFileName.lastIndexOf(classPackage + "/");
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
     * This method set's a new file name. IMPORTANT: It doesn't load it. To load you have to call {@link #load(String)}.
     * 
     * @param configFile Configuration file name
     * @return
     */
    public boolean setFile(String configFile) {
        String fxt = resolvePath(configFile);
        if (fxt == null)
            return false;
        ConfigFetch.configFile = fxt;
        return true;
    }

    /**
     * Loads the configuration file. Also configures the log4j.
     * 
     * @param configFile Configuration file name
     * @return true if successful
     */
    public boolean load(String configFile) {
        String fxt = resolvePath(configFile);
        if (fxt == null) {
            fxt = CONFIG_FILE_NAME;
        }
        else {
            if (!fxt.contains("build") && !fxt.contains("classes"))
                ConfigFetch.configFile = fxt;
        }
        confDoc = readConfigFile(fxt);

        if (ifLogger) {
            if (getLoggingPropertiesType().equalsIgnoreCase("xml")) {
                try {
                    DOMConfigurator.configure(getLoggingPropertiesLocation());
                    NeptusLog.pub().debug("Log4J configured with a XML conf. file!");
                }
                catch (Error e) {
                    BasicConfigurator.configure();
                    NeptusLog.pub().warn("Could not configure Log4J!!");
                }
            }
            else if (getLoggingPropertiesType().equalsIgnoreCase("properties")) {
                try {
                    PropertyConfigurator.configure(getLoggingPropertiesLocation());
                    NeptusLog.pub().debug("Log4J configured with a JavaProperties conf. file!");
                }
                catch (Error e1) {
                    BasicConfigurator.configure();
                    NeptusLog.pub().warn("Could not configure Log4J!!");
                }
            }
        }

        // IMC Local ID
        try {
            String hostadr;
            try {
                InetAddress addr = InetAddress.getLocalHost();
                hostadr = addr.getHostAddress();
            }
            catch (Exception e1) { // UnknownHostException
                e1.printStackTrace();
                hostadr = "127.0.0.1";
            }
            String osName = System.getProperty("os.name");
            if (osName.toLowerCase().indexOf("linux") != -1) {
                try {
                    Enumeration<NetworkInterface> netInt = NetworkInterface.getNetworkInterfaces();
                    while (netInt.hasMoreElements()) {
                        NetworkInterface ni = netInt.nextElement();
                        Enumeration<InetAddress> iAddress = ni.getInetAddresses();
                        while (iAddress.hasMoreElements()) {
                            InetAddress ia = iAddress.nextElement();
                            if (!ia.isLoopbackAddress()) {
                                if (ia instanceof Inet4Address) {
                                    hostadr = ia.getHostAddress();
                                    break;
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String[] sl2 = hostadr.split("\\.");

            // IMC
            long idd = (Integer.parseInt(sl2[2]) << 8) + (Integer.parseInt(sl2[3])); 
            ImcId16 newCcuId = new ImcId16((idd & 0x1FFF) | 0x4000); // 010????? CCU IDs
            GeneralPreferences.imcCcuId = newCcuId;

            GeneralPreferences.imcCcuName = "CCU " + System.getProperty("user.name")
                    + " " + sl2[2] + "_" + sl2[3];
            
            GeneralPreferences.saveProperties();
        }
        catch (NumberFormatException e) {
            NeptusLog.pub().warn("Setting CCU Local IMC ID error", e);
        }
        finally {
            GeneralPreferences.warnPreferencesListeneres();
        }
        return true;
    }

    /**
     * Loads the configuration file. Also configures the log4j.
     * 
     * @return true if successful
     */
    public boolean load() {
        return load(configFile);
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
        File fx1 = null;

        try {
            NeptusLog.pub().debug("Trying to see if the Path exists...");
            if (fx.exists()) {
                NeptusLog.pub().debug("The Path exists! [" + path + "]");
                return path;
            }
            else {
                NeptusLog.pub().debug("Trying to see if the AbsolutePath exists... " + "[" + fx.getAbsoluteFile() + "]");
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
            NeptusLog.pub().info("The file '" + path + "' was not found in this system.");
            return null;
        }
        catch (Exception any) {
            NeptusLog.pub().info("The file '" + path + "' was not found in this system.");
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
        File fx1 = null;

        for (int i = 0; i < parentPaths.length; i++) {
            if ((new File(parentPaths[i])).isAbsolute())
                fx1 = (new File(parentPaths[i] + DS + path)).getAbsoluteFile();
            else
                fx1 = (new File(rootSolver + DS + parentPaths[i] + DS + path)).getAbsoluteFile();
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
                String parent = "";
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
     * @return Returns the configFile path.
     */
    public static String getConfigFile() {
        return configFile;
    }

    /**
     * 
     * @param xpath2Element
     * @param doDocument
     * @return
     */
    public static String getElementTextByXPath(String xpath2Element, Document doDocument) {
        String entity = null;

        XPath xpathSelector = DocumentHelper.createXPath(xpath2Element);
        List<?> results = xpathSelector.selectNodes(doDocument);
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            entity = ((Element) iter.next()).getTextTrim();
        }
        return entity;
    }

    /**
     * @param xpath2Attribute
     * @param doDocument
     * @return
     */
    public static String getAttributeTextByXPath(String xpath2Attribute, Document doDocument) {
        String entity = null;

        XPath xpathSelector = DocumentHelper.createXPath(xpath2Attribute);
        List<?> results = xpathSelector.selectNodes(doDocument);
        for (Iterator<?> iter = results.iterator(); iter.hasNext();) {
            entity = ((Attribute) iter.next()).getStringValue();
        }
        return entity;
    }

    /**
     * 
     * @param ConfigFile
     * @return
     */
    protected Document readConfigFile(String configFile) {
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
                System.err.println("Sorry, configuration file not found.");
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
            loc = "conf/log4j.xml";
        return loc;
    }

    /**
     * returns the central properties file type for log4j Loggers. This file type is expected to be "xml" or
     * "properties". <i>//log-conf-file/@type</i>
     * 
     * @return
     */
    public static String getLoggingPropertiesType() {
        String type = getAttributeTextByXPath("//log-conf-file/@type", confDoc);
        if (type == null)
            type = "xml";
        return type;
    }

    /**
     * @return
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
     * @return
     */
    public static String getDefaultIMCDefinitionsLocation() {
        String loc = getElementTextByXPath("//imc-default-defs", confDoc);
        if (loc != null) {
            String loc1 = resolvePath(loc);
            if (loc1 != null)
                return loc1;
            return loc;
        }
        else {
            return "conf/messages/IMC.xml";
        }
    }

    /**
     * @return
     */
    public static String getMiscSystemsConfigLocation() {
        String loc = getElementTextByXPath("//misc-systems-file", confDoc);
        if (loc != null) {
            String loc1 = resolvePath(loc);
            if (loc1 != null)
                return loc1;
            return loc;
        }
        else
            loc = "conf/neptus-misc-systems.xml";
        return loc;
    }

    /**
     * @return
     */
    public static LinkedList<String> getVehiclesList() {
        LinkedList<String> result = new LinkedList<String>();
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
        File[] filesVeh = fxDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory())
                    return false;

                String extension = FileUtil.getFileExtension(f);
                if (extension != null) {
                    if (extension.equals("xml") || extension.equals(FileUtil.FILE_TYPE_VEHICLE))
                        return true;
                    else
                        return false;
                }
                return false;
            }
        });

        // To sort the list (in Windows this is automatic, in Linux we need
        // this)
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
     * @return
     */
    public static String getMissionSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("mission");
    }

    /**
     * @return
     */
    public static String getVehicleSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("vehicle");
    }

    /**
     * @return
     */
    public static String getMapSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("map");
    }

    /**
     * @return
     */
    public static String getCoordinateSystemSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("coordinateSystems");
    }

    /**
     * @return
     */
    public static String getChecklistSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("checklist");
    }

    /**
     * @return
     */
    public static String getMiscSystemsSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("misc-systems");
    }

    /**
     * @return
     */
    public static String getConsoleSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("console");
    }

    /**
     * @return
     */
    public static String getActionMapSchemaLocation() {
        initialize();
        return listOfSchemasPaths.get("neptus-action-map");
    }

    /**
     * @return
     */
    public static Properties getVersionInfoAsProperties() {
        Properties prop = new Properties();
        @SuppressWarnings("unused")
        String versionString = "";
        InputStream ist = ConfigFetch.class.getResourceAsStream(VERSION_FILE_NAME);

        if (ist != null) {
            try {
                prop.load(ist);
                versionString = "\n\nVersion ";
                versionString += prop.getProperty("VERSION", "");
                versionString += " (";
                versionString += prop.getProperty("DATE", "");
                versionString += ", r";
                versionString += prop.getProperty("SVN_REV", "?");
                versionString += ")";
            }
            catch (IOException e) {
                NeptusLog.pub().debug(e);
            }
        }
        return prop;
    }

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
                versionString += ", r";
                versionString += prop.getProperty("SVN_REV", "?");
                versionString += ")";
            }
            catch (IOException e) {
                NeptusLog.pub().debug(e);
            }
        }
        return versionString;
    }

    /**
     * @return
     */
    public static String getVersionInfoAsString() {
        Properties prop = new Properties();
        String versionString = "";
        InputStream ist = ConfigFetch.class.getResourceAsStream(VERSION_FILE_NAME);

        if (ist != null) {
            try {
                prop.load(ist);
                versionString = "\n\nVersion ";
                versionString += prop.getProperty("VERSION", "");
                versionString += " (";
                versionString += prop.getProperty("DATE", "");
                versionString += ", r";
                versionString += prop.getProperty("SVN_REV", "?");
                versionString += ")";
            }
            catch (IOException e) {
                NeptusLog.pub().debug(e);
            }
        }
        return versionString;
    }

    /**
     * @return
     */
    public static String getSaveAsCommentForXML() {
        Properties prop = getVersionInfoAsProperties();
        Date trialTime = new Date();
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.0Z'");
        String ret = "Data saved on " + dateFormater.format(trialTime) + " with Neptus version "
                + prop.getProperty("VERSION", "") + " (compiled on " + prop.getProperty("DATE", "") + ", r"
                + prop.getProperty("SVN_REV", "?") + ").";
        return " " + ret + " ";
    }

    /**
     * @return Returns the superParentFrame.
     */
    public static Component getSuperParentFrame() {
        return superParentFrame;
    }

    /**
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
     * @param superParentFrame The superParentFrame to set.
     */
    public static void setSuperParentFrame(Component superParentFrame) {
        if (ConfigFetch.superParentFrame == null)
            ConfigFetch.superParentFrame = superParentFrame;
    }

    /**
     * @param superParentFrame The superParentFrame to set.
     */
    public static void setSuperParentFrameForced(Component superParentFrame) {
        ConfigFetch.superParentFrame = superParentFrame;
    }

    /**
     * @return The short corresponding to the OS found.
     */
    static public short getOS() {
        String osName = System.getProperty("os.name");
        short os = OS_ERROR;
        if (osName.toLowerCase().indexOf("windows") != -1)
            os = OS_WINDOWS;
        else if (osName.toLowerCase().indexOf("linux") != -1)
            os = OS_LINUX;
        else
            os = OS_OTHER;

        return os;
    }

    /**
     * @param os The shor representing the OS to test.
     * @return The result of the test.
     */
    static public boolean isOSEqual(short os) {
        return (getOS() == os) ? true : false;
    }

    /**
     * 
     */
    private static void parseVersionFile() {
        if (params != null)
            return;

        params = new Hashtable<String, String>();

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

    public static String getSvnRev() {
        parseVersionFile();
        return params.get("svn_rev");
    }

    public static long getNeptusInitializationTime() {
        return neptusInitializationTime;
    }

    public static String getNeptusTmpDir() {
        return neptusTmpDir;
    }

    public static boolean isOnLockedMode() {
        return onLockedMode;
    }

    public static void setOnLockedMode(boolean onLockedMode) {
        ConfigFetch.onLockedMode = onLockedMode;
    }

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
     * 
     */
    private void loadIMCDefinitions() {
        // IMC definition loading
        boolean loadDefault = false;
        File imcFx = new File(getDefaultIMCDefinitionsLocation());
        FileInputStream fis = null;
        if (imcFx.exists()) {
            try {
                fis = new FileInputStream(imcFx);
            }
            catch (FileNotFoundException e) {
                loadDefault = true;
                NeptusLog.pub().fatal(e);
            }
        }
        else {
            loadDefault = true;
        }
        String msg = "IMC definition loading from default: \""
                + getDefaultIMCDefinitionsLocation()
                + (!loadDefault ? "\"" : "\" [file doesn't exists!!! | loading \"" + IMCDefinition.pathToDefaults
                        + "\" inside the jar!!!]");
        System.out.println(msg);
        if (!loadDefault)
            NeptusLog.pub().info(msg);
        else
            NeptusLog.pub().fatal(msg);
        if (!loadDefault)
            IMCDefinition.getInstance(fis);
        else
            IMCDefinition.getInstance();
    }

    /**
     * @return
     */
    public static List<Image> getIconImagesForFrames() {
        ArrayList<Image> imageList = new ArrayList<Image>();
        imageList.add(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon.png")));
        imageList.add(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon1.png")));
        imageList.add(Toolkit.getDefaultToolkit().getImage(ConfigFetch.class.getResource("/images/neptus-icon2.png")));
        return imageList;
    }
    
    public static void mark(String tag){
//        timings.put(tag, System.currentTimeMillis());
    }
    public static void benchmark(String tag){
//        System.out.println("BENCHMARK "+tag + " took " + ((System.currentTimeMillis() - timings.get(tag))) + "ms and from the start "+ ((System.currentTimeMillis() - STARTTIME) / 1E3) + "s");
    }
    
    /**
     * @return the distributionType
     */
    public static NeptusProperty.DistributionEnum getDistributionType() {
        distributionSetChangedOrGet = true;
        return distributionType;
    }
    
    /**
     * @param dist
     * @return true if the change happen or not.
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
        System.out.println(ConfigFetch.getLoggingPropertiesLocation());

        String st = ConfigFetch.getConfigFile();
        String st1 = ConfigFetch.resolvePathWithParent(st, "../fe.txt");
        System.out.println(st.concat("\n").concat(st1));
        st1 = ConfigFetch.resolvePathWithParent(st, "c:/fe.txt");
        System.out.println(st.concat("\n").concat(st1));
    }
}
