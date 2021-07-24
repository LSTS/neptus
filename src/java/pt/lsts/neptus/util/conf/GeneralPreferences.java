/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 2005/10/12 refactored in 2012/11/10
 */
package pt.lsts.neptus.util.conf;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.IridiumManager.IridiumMessengerEnum;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.renderer.ArrayAsStringRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.PropertiesLoader;

/**
 * @author Paulo Dias
 * 
 */
public class GeneralPreferences implements PropertiesProvider {
    
    private static PropertiesLoader properties = null;

    public static final String GENERAL_PROPERTIES_FILE = "conf/general-properties.xml";

    public static Vector<PreferencesListener> pListeners = new Vector<PreferencesListener>();

    @NeptusProperty(name = "Language", category = "Interface", userLevel = LEVEL.REGULAR,
            description = "Select the language to use for the interface (needs restart). (Format [a-z]{2}_[A-Z]{2}")
    public static String language = "en";

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Speech On", category = "Interface", userLevel = LEVEL.REGULAR, 
            description = "Select this if you want the speech on or off.")
    public static boolean speechOn = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Comms Local Port UDP", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int commsLocalPortUDP = 6001;

    @NeptusProperty(name = "Comms Local Port TCP", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int commsLocalPortTCP = 6001;

    @NeptusProperty(name = "IMC transports to use", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Comma separated transports list. Valid values are (UDP, TCP). (The order implies preference of use.)")
    public static String imcTransportsToUse = "UDP, TCP";

    @NeptusProperty(name = "IMC CCU ID", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static ImcId16 imcCcuId = new ImcId16("40:00");

    @NeptusProperty(name = "IMC CCU Name", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "The CCU Name to be presented to other peers")
    public static String imcCcuName = "CCU " + System.getProperty("user.name");

    @NeptusProperty(name = "IMC Multicast Enable", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Multicast enable or disable")
    public static boolean imcMulticastEnable = true;

    @NeptusProperty(name = "IMC Multicast Address", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static String imcMulticastAddress = "224.0.75.69";

    @NeptusProperty(name = "IMC Multicast/Broadcast Port Range", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Multicast port range to use for the announce channel.\n The form is, e.g. '6969','6967-6970'")
    public static String imcMulticastBroadcastPortRange = "30100-30104";

    @NeptusProperty(name = "IMC Broadcast Enable", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static boolean imcBroadcastEnable = true;

    @NeptusProperty(name = "IMC Change by Source IP Request", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "If enable allows the announce msg request to use the sender IP to be use in future comms. to the sender system.")
    public static boolean imcChangeBySourceIpRequest = true;

    @NeptusProperty(name = "IMC Unicast Annonce Enable", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "To send announce also by Unicast. Enable or disable")
    public static boolean imcUnicastAnnounceEnable = true;

    @NeptusProperty(name = "Reachability Test Timeout", units = "ms", category = "IMC Communications", userLevel = LEVEL.ADVANCED, 
            description = "Timeout to test reachability of IPs (in ms)")
    public static int imcReachabilityTestTimeout = 3000;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Logs Downloader - Enable Parcial Download", category = "IMC Logs Downloader", userLevel = LEVEL.ADVANCED, 
            description = "Enable the partial logs downloads (resume partial downloads). NOTE: THE DOWNLOAD BOXES ONLY READ THIS OPTION UPON CREATION.")
    public static boolean logsDownloaderEnablePartialDownload = true;

    @NeptusProperty(name = "Logs Downloader - Use number of simultaneous downloads control", category = "IMC Logs Downloader", userLevel = LEVEL.ADVANCED, 
            description = "")
    public static boolean logsNumberSimultaneousDownloadsControl = true;

    @NeptusProperty(name = "Logs Downloader - Wait for all To Stop", category = "IMC Logs Downloader", userLevel = LEVEL.ADVANCED, 
            description = "")
    public static boolean logsDownloaderWaitForAllToStop = true;

    @NeptusProperty(name = "Logs Downloader - Ignore active log", category = "IMC Logs Downloader", userLevel = LEVEL.ADVANCED,
            description = "Use it carefully knowing that it may corrupt the last log because are files being written.")
    public static boolean logsDownloaderIgnoreActiveLog = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Heartbeat Time Period (ms)", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int heartbeatTimePeriodMillis = 1000;

    @NeptusProperty(name = "Heartbeat Timeout (ms)", category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static int heartbeatTimeoutMillis = 2000;


    @NeptusProperty(name = "Number Of Shown Trails Points", category = "Map", userLevel = LEVEL.REGULAR)
    public static int numberOfShownPoints = 500;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Auto Snapshot Period (s)", category = "Interface", userLevel = LEVEL.REGULAR)
    public static int autoSnapshotPeriodSeconds = 60;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Renderer Plan Color", category = "Map", userLevel = LEVEL.ADVANCED)
    public static Color rendererPlanColor = new Color(255, 255, 255);

    @NeptusProperty(name = "Renderer 3D Priority", category = "Map", userLevel = LEVEL.ADVANCED,
            description = Thread.MIN_PRIORITY + "- Minimum Priority <br>"
                    + Thread.MAX_PRIORITY + "- Maximum Priority<br>" + Thread.NORM_PRIORITY + "- Normal Priority")
    public static int renderer3DPriority = Thread.MIN_PRIORITY;

    @NeptusProperty(name = "Renderer Update Periode For Vehicle State (ms)", category = "Map", userLevel = LEVEL.ADVANCED,
            description = "This is the update periode to "
                    + "update the vehicle state in the renders (in miliseconds). Use '-1' to disable it. One good "
                    + "good value is 100ms (10Hz) or 50ms (20hz).")
    public static int rendererUpdatePeriodeForVehicleStateMillis = 50;

    @NeptusProperty(name = "Console Edit Border Color", category = "Console", userLevel = LEVEL.ADVANCED)
    public static Color consoleEditBorderColor = new Color(150, 0, 0);

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "SSH Connection Timeout (ms)", category = "SSH", userLevel = LEVEL.ADVANCED)
    public static int sshConnectionTimeoutMillis = 3000;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Comms. Queue Size", category = "Communications", userLevel = LEVEL.ADVANCED, 
            description = "Select the comms. queues size.")
    public static int commsQueueSize = 1024;

    @NeptusProperty(name = "Comms. Messsage Separation Time (ms)", category = "Communications", userLevel = LEVEL.ADVANCED, 
            description = "Select the comms. separation time in miliseconds that a message (by type) should be warn. Use \"-1\" for always warn.")
    public static int commsMsgSeparationMillis = -1;

    @NeptusProperty(name = "Filter UDP Redirect Also By Port", editable = false, category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static boolean filterUdpAlsoByPort = false;

    @NeptusProperty(name = "Redirect Unknown Comms. To First Vehicle In Comm. List", editable = false, category = "IMC Communications", userLevel = LEVEL.ADVANCED,
            description = "Any messages comming from unknown vehicle will be redirect to the first on comm. list.")
    public static boolean redirectUnknownIdsToFirstCommVehicle = false;
    

    @NeptusProperty(name = "Use New System Activity Counter", editable = false, category = "IMC Communications", userLevel = LEVEL.ADVANCED)
    public static boolean commsUseNewSystemActivityCounter = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Enable Log Sent Messages", category = "Message Logging", userLevel = LEVEL.REGULAR)
    public static boolean messageLogSentMessages = true;

    @NeptusProperty(name = "Enable Log Received Messages", category = "Message Logging", userLevel = LEVEL.REGULAR)
    public static boolean messageLogReceivedMessages = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Extended Program Output Log", category = "Neptus Program Logging", userLevel = LEVEL.ADVANCED,
            description = "If true, the program output will be augmented with more info (mainly for problem solving).")
    public static boolean programLogExtendedLog = false;

    @NeptusProperty(name = "Tides file", category = "Tides", userLevel = LEVEL.REGULAR)
    public static File tidesFile = new File("conf/tides/Leixoes.tid");

    // -------------------------------------------------------------------------
    
    @NeptusProperty(name = "Iridium Messenger", category="Iridium Communications", userLevel = LEVEL.REGULAR,
        description = "Iridium messaging implementation")
    public static IridiumMessengerEnum iridiumMessenger = IridiumMessengerEnum.HubIridiumMessenger;
    
    // -------------------------------------------------------------------------
    
    @NeptusProperty(name = "Maximum Size of Plan Name For Acoustics", category="Plan", userLevel = LEVEL.ADVANCED,
        description = "Maximum size for a plan name to be started by acoustics. (Reboot required after change.) Minimum 1, maximum 255")
    public static int maximumSizePlanNameForAcoustics = 31;
    
    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Place Main Vehicle Combobox On Menu Or Status Bar", category="Console", userLevel = LEVEL.REGULAR,
            description = "Place the console vehicle combobox on the menu bar or status bar (overcomes Unity hidding menus).")
    public static boolean placeMainVehicleComboOnMenuOrStatusBar = true;

    @NeptusProperty(name = "Use Main Vehicle Combo on Consoles", category="Console", userLevel = LEVEL.ADVANCED,
            description = "Needs console retarts.")
    public static boolean useMainVehicleComboOnConsoles = true;

    @NeptusProperty(name = "Place Notification Button on Console Status Bar", category="Console", userLevel = LEVEL.ADVANCED,
            description = "Needs console retarts.")
    public static boolean placeNotificationButtonOnConsoleStatusBar = true;

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "AIS MMSI Query Prefix", category = "AIS MMSI Query", userLevel = LEVEL.ADVANCED)
    public static String aisMmsiQueryUrlPrefix = "http://api.ais.owm.io/1.2/vessels/";
    
    @NeptusProperty(name = "AIS MMSI Query Sufix", category = "AIS MMSI Query", userLevel = LEVEL.ADVANCED)
    public static String aisMmsiQueryUrlSufix = ".json?api_key=f7a0da8eacb49740eb45b5e74d130459";
    
    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Lat/Lon Preferable Display Format", category = "Location", userLevel = LEVEL.REGULAR)
    public static LatLonFormatEnum latLonPrefFormat = LatLonFormatEnum.DM;

    @NeptusProperty(name = "Preferred Speed Units", category = "Speed", userLevel = LEVEL.REGULAR)
    public static SpeedType.Units speedUnits = Units.MPS;
    
    @NeptusProperty(name = "Force Speed Units", category = "Speed", userLevel = LEVEL.ADVANCED, 
            description = "If speed units are forced, the user cannot set other units.")
    public static boolean forceSpeedUnits = false;
    
    @NeptusProperty(name = "Preferred Z Units Array", category = "Z Value", userLevel = LEVEL.ADVANCED, 
            editable = false, rendererClass = ArrayAsStringRenderer.class,
            description = "This lists the valid Z units to show. This can be overided by the per vehicle settings. "
                    + "Leeave it empty for no restrictions.")
    public static ManeuverLocation.Z_UNITS[] validZUnits = {};

    // -------------------------------------------------------------------------

    @NeptusProperty(name = "Show Local Time on Console", category = "Interface", userLevel = LEVEL.REGULAR, 
            description = "Select this if you want also to show local time on console status bar.")
    public static boolean localTimeOnConsoleOn = false;

    // -------------------------------------------------------------------------
    
    @NeptusProperty(name = "Ripples URL", category="Ripples", userLevel = LEVEL.REGULAR,
        description = "URL of the ripples web server")
    public static String ripplesUrl = "https://ripples.lsts.pt";
    @NeptusProperty(name = "Ripples API Access Token", category="Ripples", userLevel = LEVEL.REGULAR,
            description = "The API access token to talked with Ripples")
    public static String ripplesApiKey = "";

    // -------------------------------------------------------------------------
    // Constructor and initialize

    public GeneralPreferences() {
    }

    public static void initialize() {
        String generalPropertiesFile = ConfigFetch.resolvePathBasedOnConfigFile(GENERAL_PROPERTIES_FILE);
        if (!new File(generalPropertiesFile).exists()) {
            String testFile = ConfigFetch.resolvePathBasedOnConfigFile("../" + GENERAL_PROPERTIES_FILE);
            if (new File(testFile).exists())
                generalPropertiesFile = testFile;
        }
        PropertiesLoader generalProperties = new PropertiesLoader(generalPropertiesFile, PropertiesLoader.XML_PROPERTIES);
        setPropertiesLoader(generalProperties);
    }
    

    // -------------------------------------------------------------------------
    // Validators

    public static String validateLanguage(String value) {
        return new StringPatternValidator("[a-z]{2}_[A-Z]{2}").validate(value);
    }

    public static String validateCommsLocalPortUDP(int value) {
        return new IntegerMinMaxValidator(1, 65535).validate(value);
    }

    public static String validateCommsLocalPortTCP(int value) {
        return new IntegerMinMaxValidator(1, 65535).validate(value);
    }

    public static String validateImcTransportsToUse(String value) {
        return new StringCommaSeparatedListValidator("UDP", "TCP").validate(value);
    }

    public static String validateImcCcuId(ImcId16 value) {
        if (ImcId16.ANNOUNCE.equals(value) || ImcId16.BROADCAST_ID.equals(value) || ImcId16.NULL_ID.equals(value))
            return "This is a reserved ID, choose another other than " + ImcId16.ANNOUNCE.toPrettyString() + ", "
                    + ImcId16.BROADCAST_ID.toPrettyString() + ", and " + ImcId16.NULL_ID.toPrettyString() + ".";

        return null;
    }

    public static String validateImcCcuName(String value) {
        return new StringNonEmptyValidator().validate(value);
    }

    public static String validateImcMulticastAddress(String value) {
        return new StringPatternValidator("\\d{2,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").validate(value);
    }

    public static String validateImcMulticastBroadcastPortRange(String value) {
        return new PortRangeValidator().validate(value);
    }

    public static String validateHeartbeatTimePeriodMillis(int value) {
        return new IntegerMinMaxValidator(100, 65535).validate(value);
    }

    public static String validateHeartbeatTimeoutMillis(int value) {
        return new IntegerMinMaxValidator(1000, 65535).validate(value);
    }

    public static String validateNumberOfShownPoints(int value) {
        return new IntegerMinMaxValidator(-1, 1000).validate(value);
    }

    public static String validateAutoSnapshotPeriodSeconds(int value) {
        return new IntegerMinMaxValidator(20, 30 * 60).validate(value);
    }

    public static String validateRenderer3DPriority(int value) {
        return new IntegerMinMaxValidator(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY).validate(value);
    }

    public static String validateRendererUpdatePeriodeForVehicleStateMillis(int value) {
        return new IntegerMinMaxValidator(-1, 1000).validate(value);
    }
    
    public static String validateSshConnectionTimeoutMillis(int value) {
        return new IntegerMinMaxValidator(0, false).validate(value);
    }

    public static String validateCommsQueueSize(int value) {
        return new IntegerMinMaxValidator(1, Integer.MAX_VALUE).validate(value);
    }
    
    public static String validateCommsMsgSeparationMillis(int value) {
        return new IntegerMinMaxValidator(-1, 1000).validate(value);
    }

    public static String validateMaximumSizePlanNameForAcoustics(int value) {
        return new IntegerMinMaxValidator(1, 255).validate(value);
    }
    
    // -------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
        
        Thread t = new Thread("Properties Change Warner") {
            @Override
            public void run() {
                warnPreferencesListeners();
            }
        };
        t.setDaemon(true);
        t.start();
        
        saveProperties();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesDialogTitle()
     */
    @Override
    public String getPropertiesDialogTitle() {
        return I18n.text("General Preferences");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }

    /**
     * @param listener
     */
    public static void addPreferencesListener(PreferencesListener listener) {
        if (!pListeners.contains(listener))
            pListeners.add(listener);
    }

    /**
     * @param listener
     */
    public static void removePreferencesListener(PreferencesListener listener) {
        pListeners.remove(listener);
    }

    /**
     * @param propertyChanged
     */
    public static void warnPreferencesListeners() {
        for (PreferencesListener pl : pListeners) {
            try {
                pl.preferencesUpdated();
            }
            catch (Exception e) {
                NeptusLog.pub().error(
                        "Exception warning '" + pl.getClass().getSimpleName() + "["
                                + Integer.toHexString(pl.hashCode()) + "]" + "' for "
                                + GeneralPreferences.class.getSimpleName() + " propertieschanges", e);
            }
            catch (Error e) {
                NeptusLog.pub().error(
                        "Error warning '" + pl.getClass().getSimpleName() + "[" + Integer.toHexString(pl.hashCode())
                                + "]" + "' for " + GeneralPreferences.class.getSimpleName() + " propertieschanges", e);
            }
        }
    }

    /**
     * @param c
     * @return
     */
    public static String colorToString(Color c) {
        if (c == null)
            return "0,0,0";

        return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    /**
     * @param s
     * @return
     */
    public static Color stringToColor(String s) {
        StringTokenizer st = new StringTokenizer(s, ", ");
        if (st.countTokens() != 3) {
            return Color.black;
        }
        try {
            int red = Integer.parseInt(st.nextToken());
            int green = Integer.parseInt(st.nextToken());
            int blue = Integer.parseInt(st.nextToken());
            return new Color(red, green, blue);
        }
        catch (Exception e) {
            e.printStackTrace();
            return Color.black;
        }
    }

    public static void setPropertiesLoader(PropertiesLoader properties) {
        GeneralPreferences.properties = properties;
        PluginUtils.loadProperties(GeneralPreferences.properties, GeneralPreferences.class);
    }

    public static void saveProperties() {
        try {
            PluginUtils.savePropertiesToXML(properties.getWorkingFile(), true, GeneralPreferences.class);
        }
        catch (IOException e) {
            NeptusLog.pub().error("saveProperties", e);
        }
    }

    public static void dumpGeneralPreferences() {
        try {
            PluginUtils.savePropertiesToXML(properties.getWorkingFile(), false, GeneralPreferences.class);
        }
        catch (IOException e) {
            NeptusLog.pub().error("saveProperties", e);
        }
    }

    public static void main(String[] args) {
        final GeneralPreferences gp = new GeneralPreferences();
        
        GeneralPreferences.addPreferencesListener(new PreferencesListener() {
            @Override
            public void preferencesUpdated() {
                NeptusLog.pub().info("<###>preferencesUpdated");
            }
        });

        final String filenameProps = "" + GeneralPreferences.class.getSimpleName().toLowerCase() + ".properties";
        final String filenameXML = "" + GeneralPreferences.class.getSimpleName().toLowerCase() + ".xml";

        PropertiesLoader pl = new PropertiesLoader(filenameXML, PropertiesLoader.XML_PROPERTIES);
        GeneralPreferences.setPropertiesLoader(pl);

        @SuppressWarnings("serial")
        final JButton button = new JButton(new AbstractAction(I18n.text("General Preferences")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                try {
                    PluginUtils.loadProperties(filenameXML, GeneralPreferences.class);
                }
                catch (IOException e2) {
                    e2.printStackTrace();
                }

                PropertiesEditor.editProperties(gp, true);
                
                validZUnits = new ManeuverLocation.Z_UNITS[] { ManeuverLocation.Z_UNITS.NONE };
                
                try {
                    PluginUtils.saveProperties(filenameProps, true, GeneralPreferences.class);
                    PluginUtils.savePropertiesToXML(filenameXML, true, GeneralPreferences.class);
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        JFrame frame = GuiUtils.testFrame(button, I18n.text("General Preferences"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
