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
 * 7 de Jul de 2011
 */
package pt.lsts.neptus.plugins.webupdate;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicNameValuePair;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.StringPatternValidator;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;
import pt.lsts.neptus.ws.PublishHelper;

/**
 * @author pdias
 * 
 */
@SuppressWarnings({"serial"})
@PluginDescription(name = "Remote Position Publisher Updater", author = "Paulo Dias", version = "1.0", icon = "pt/lsts/neptus/plugins/webupdate/webupdate-pub-on.png")
public class RemotePositionPublisherUpdater extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener {

    private final ImageIcon ICON_ENABLE = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/webupdate/webupdate-pub-on.png", 32, 32);
    private final ImageIcon ICON_DISABLE = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/webupdate/webupdate-pub-off.png", 32, 32);

    @NeptusProperty(name = "Publish web address")
    public String pubURL = "http://whale.fe.up.pt/neptleaves/";

    @NeptusProperty(name = "Publish period (ms)", description = "The period to fetch the systems' positions.")
    public int publishPeriodMillis = 1000;

    @NeptusProperty(category = "Advanced", description = "If true any simulated system will not be published.")
    public boolean ignoreSimulatedSystems = true;

    @NeptusProperty
    public boolean active = false;

    @NeptusProperty
    public boolean publishOn = true;

    @NeptusProperty(editable = false)
    public String publishMessagesListStd = "EstimatedState, LbLConfig, PlanSpecification, "
            + "VehicleState, PlanControlState, EntityList";

    @NeptusProperty
    public String publishMessagesListExtra = "Heartbeat, EntityState, Announce, PlanControl, Voltage";

    @NeptusProperty
    public boolean publishWebReceivedMessages = true;

    @NeptusProperty
    public boolean publishOnlyVehicles = false;

    @NeptusProperty
    public boolean publishExternalSystems = true;

    @NeptusProperty
    public boolean publishBeaconsState = false;

    @NeptusProperty
    public boolean publishActiveConsolePlan = false;

    @NeptusProperty
    public boolean publishVehiclePlan = false;

    @NeptusProperty
    public boolean fetchRemoteSystemsOn = true;

    @NeptusProperty
    public boolean publishRemoteSystemsLocally = true;

    @NeptusProperty(category = "Advanced")
    public boolean debugOn = false;

    private JCheckBoxMenuItem publishCheckItem = null;

    private String publishMessagesList = "";

    private long lastFetchPosTimeMillis = System.currentTimeMillis();

    private HttpClientConnectionHelper httpComm;
    private HttpPost postHttpRequestPublishState;
    private HttpPost postHttpRequestPublishPlan;
    private HttpGet getHttpRequestImcMsg;
    private HttpGet getHttpRequestRemoteState;

    private Timer timer = null;
    private TimerTask ttask = null;

    private final LinkedHashMap<String, Integer> msgSysList = new LinkedHashMap<String, Integer>();

    private MultiSystemIMCMessageListener systemsMessageListener = null;

    private final String messageListRedex = "(\\w(:\\d{1,2})?(((\\s)*)?,((\\s)*)?)?)*";
    private final Pattern messageListPattern = Pattern.compile(messageListRedex);

    private DocumentBuilderFactory docBuilderFactory;

    {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setNamespaceAware(false);
        try {
            docBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        }
        catch (ParserConfigurationException e) {
            NeptusLog.pub().error(e.getMessage());
        }
    }

    private final LinkedHashMap<String, IMCMessage> estimatedStates = new LinkedHashMap<String, IMCMessage>();
    private IMCMessage lblConfig = null;
    private final Object lblConfigLock = new Object();
    private final LinkedHashMap<String, IMCMessage> planSpecifications = new LinkedHashMap<String, IMCMessage>();
    private final LinkedHashMap<String, IMCMessage> vehicleStates = new LinkedHashMap<String, IMCMessage>();
    private final LinkedHashMap<String, IMCMessage> planControlStates = new LinkedHashMap<String, IMCMessage>();
    private final LinkedHashMap<String, IMCMessage> entityLists = new LinkedHashMap<String, IMCMessage>();
    private final LinkedHashMap<String, Vector<IMCMessage>> otherLists = new LinkedHashMap<String, Vector<IMCMessage>>();

    private final LinkedHashMap<String, Long> timeSysList = new LinkedHashMap<String, Long>();
    private final LinkedHashMap<String, CoordinateSystem> locSysList = new LinkedHashMap<String, CoordinateSystem>();

    // For publishing vehicles' plans
    protected LinkedHashMap<String, String> vehicleToPlanIds = new LinkedHashMap<>();
    protected LinkedHashMap<String, Vector<LocationType>> planToLocations = new LinkedHashMap<>();
    protected LinkedHashMap<String, PathElement> planToPath = new LinkedHashMap<>();

    private ToolbarButton sendEnableDisableButton;

    /**
     * 
     */
    public RemotePositionPublisherUpdater(ConsoleLayout console) {
        super(console);
        initializeComm();
        initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {

        publishCheckItem = addCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass())
                + ">Start/Stop",
                null, new CheckMenuChangeListener() {
                    @Override
                    public void menuUnchecked(ActionEvent e) {
                        active = false;

                        synchronized (estimatedStates) {
                            estimatedStates.clear();
                        }
                        synchronized (lblConfigLock) {
                            if (lblConfig != null) {
                                lblConfig = null;
                            }
                        }
                        synchronized (planSpecifications) {
                            planSpecifications.clear();
                        }
                        synchronized (vehicleStates) {
                            vehicleStates.clear();
                        }
                        synchronized (planControlStates) {
                            planControlStates.clear();
                        }
                        synchronized (entityLists) {
                            entityLists.clear();
                        }
                        synchronized (otherLists) {
                            otherLists.clear();
                        }

                    }

                    @Override
                    public void menuChecked(ActionEvent e) {
                        active = true;
                    }
                });

        addMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">Settings", null,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(RemotePositionPublisherUpdater.this, getConsole(), true);
            }
        });

        publishCheckItem.setState(active);
    }

    /**
     * 
     */
    private void initialize() {
        // setVisibility(false);

        removeAll();
        setBackground(new Color(255, 255, 110));

        sendEnableDisableButton = new ToolbarButton(new AbstractAction("Pub on", ICON_ENABLE) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String cmd = e.getActionCommand();
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendEnableDisableButton.setEnabled(false);
                        if ("Pub on".equalsIgnoreCase(cmd)) {
                            active = true;
                            sendEnableDisableButton.setActionCommand("Pub off");
                            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_DISABLE);
                            sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Pub off");
                        }
                        else if ("Pub off".equalsIgnoreCase(cmd)) {
                            active = false;
                            sendEnableDisableButton.setActionCommand("Pub on");
                            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_ENABLE);
                            sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Pub on");
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        sendEnableDisableButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });
        sendEnableDisableButton.setActionCommand("Pub on");
        add(sendEnableDisableButton);

        publishMessagesList = publishMessagesListStd + ", " + publishMessagesListExtra;

        timer = new Timer("RemotePositionPublisher");
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, publishPeriodMillis);

        systemsMessageListener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName() + " ["
                + Integer.toHexString(hashCode()) + "]") {

            @Override
            public void messageArrived(ImcId16 id, IMCMessage msg) {
                // if (!publishOn) {
                //
                // }

                ImcSystem sys = ImcSystemsHolder.lookupSystem(id);
                if (sys != null && !(ignoreSimulatedSystems && sys.isSimulated())) {
                    if ("LbLConfig".equalsIgnoreCase(msg.getAbbrev())) {
                        synchronized (lblConfigLock) {
                            lblConfig = msg;
                        }
                    }
                    else if ("EstimatedState".equalsIgnoreCase(msg.getAbbrev())) {
                        synchronized (estimatedStates) {
                            estimatedStates.put(sys.getName(), msg);
                        }
                    }
                    else if ("PlanSpecification".equalsIgnoreCase(msg.getAbbrev())) {
                        synchronized (planSpecifications) {
                            planSpecifications.put(sys.getName(), msg);
                        }
                    }
                    else if ("VehicleState".equalsIgnoreCase(msg.getAbbrev())) {
                        synchronized (vehicleStates) {
                            vehicleStates.put(sys.getName(), msg);
                        }
                    }
                    else if ("PlanControlState".equalsIgnoreCase(msg.getAbbrev())) {
                        synchronized (planControlStates) {
                            planControlStates.put(sys.getName(), msg);
                        }
                        processPlanControlStateForVehiclePlanInformation(msg);
                    }
                    else if ("EntityList".equalsIgnoreCase(msg.getAbbrev())) {
                        synchronized (entityLists) {
                            entityLists.put(sys.getName(), msg);
                        }
                    }
                    else {
                        synchronized (otherLists) {
                            // otherLists.put(sys.getName(), msg);
                            Vector<IMCMessage> osm = otherLists.get(sys.getName());
                            if (osm == null) {
                                osm = new Vector<IMCMessage>();
                                otherLists.put(sys.getName(), osm);
                            }
                            if (msg.hasFlag("periodic")/* || true */) {
                                IMCMessage remove = null;
                                for (IMCMessage imcMessage : osm) {
                                    if (imcMessage.getAbbrev().equalsIgnoreCase(msg.getAbbrev())) {
                                        // osm.remove(imcMessage);
                                        remove = imcMessage;
                                    }
                                }
                                if (remove != null)
                                    osm.remove(remove);
                            }
                            if (osm.size() > 1024)
                                osm.remove(0);
                            if (active || msg.hasFlag("periodic"))
                                osm.add(msg);
                        }
                    }
                }
            }
        };
    }

    private void initializeComm() {
        httpComm = new HttpClientConnectionHelper();
        httpComm.initializeComm();
    }

    /**
     * 
     */
    public String validatePublishMessagesListExtra(String publishMessagesListExtra) {
        return new StringPatternValidator(messageListRedex).validate(publishMessagesListExtra);
    }

    private void refreshUI() {
        if (active && "Pub on".equalsIgnoreCase(sendEnableDisableButton.getActionCommand())) {
            sendEnableDisableButton.setActionCommand("Pub off");
            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_DISABLE);
            sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Pub off");
        }
        else if (!active && !"Pub on".equalsIgnoreCase(sendEnableDisableButton.getActionCommand())) {
            sendEnableDisableButton.setActionCommand("Pub on");
            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_ENABLE);
            sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Pub on");
        }
    }

    private TimerTask getTimerTask() {
        if (ttask == null) {
            ttask = new TimerTask() {
                @Override
                public void run() {
                    if (!active)
                        return;

                    if (publishOn) {
                        try {
                            publishData();
                            publishPlan();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            getRemoteImcData();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (fetchRemoteSystemsOn) {
                        try {
                            getRemoteStateData();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }
        return ttask;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates ()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        refreshUI();

        if (!publishOn && !fetchRemoteSystemsOn)
            active = false;

        if (publishCheckItem != null)
            publishCheckItem.setState(active);

        if (publishMessagesList.length() == 0) {
            systemsMessageListener.clean();
            return true;
        }

        Vector<String> sn = new Vector<String>();
        // for (VehicleTreeListener vtl : getConsole().getVehicleTreesListeners()) {
        // sn.add(vtl.getVehicleId());
        // }
        ImcSystem[] systems = ImcSystemsHolder.lookupSystemVehicles();
        for (ImcSystem sys : systems) {
            sn.add(sys.getName());
        }
        if (sn.size() == 0)
            systemsMessageListener.clean();
        else
            systemsMessageListener.setSystemToListenStrings(sn.toArray(new String[sn.size()]));

        if (!publishOn) {
            abortPublishOnAllActiveConnections();
        }
        if (!fetchRemoteSystemsOn) {
            abortFetchOnAllActiveConnections();
        }

        return true;
    }

    private boolean publishData() {
        Document doc = DocumentHelper.createDocument();
        Element ms = doc.addElement("MissionState");
        ImcSystem[] systems = publishOnlyVehicles ? ImcSystemsHolder.lookupSystemVehicles() : ImcSystemsHolder
                .lookupAllSystems();
        for (ImcSystem sys : systems) {
            if (ignoreSimulatedSystems && sys.isSimulated())
                continue;
            // if ((System.currentTimeMillis() - sys.getLocationTimeMillis() < publishPeriodMillis))
            // continue;

            Element vs = ms.addElement("VehicleState");
            vs.addAttribute("id", sys.getName()); // vtl.getVehicleId());

            try {
                if (System.currentTimeMillis() - sys.getLocationTimeMillis() > DateTimeUtil.HOUR) {
                    ms.remove(vs);
                    continue;
                }
                if (sys.getLocation().getLatitudeDegs() == 0d
                        && sys.getLocation().getLongitudeDegs() == 0d) {
                    ms.remove(vs);
                    continue;
                }

                Date date = new Date(sys.getLocationTimeMillis());
                vs.addAttribute("time", DateTimeUtil.timeFormatterUTC.format(date));
                vs.addAttribute("date", DateTimeUtil.dateFormatterUTC.format(date));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Element crd = new LocationType(sys.getLocation()).asElement(); // vtl.getState().getPosition().asElement();
            vs.add(crd);
            Element att = vs.addElement("attitude");
            att.addElement("phi").addText("" + sys.getRollDegrees()); // vtl.getState().getRoll());
            att.addElement("theta").addText("" + sys.getPitchDegrees()); // vtl.getState().getPitch());
            att.addElement("psi").addText("" + sys.getYawDegrees()); // vtl.getState().getYaw());

            vs.addElement("imc");

            try {
                IMCMessage opLimits = (IMCMessage) sys.retrieveData("OperationalLimits");
                if (opLimits != null) {
                    long millis = sys.retrieveDataTimeMillis("OperationalLimits");
                    if (System.currentTimeMillis() - millis < DateTimeUtil.DAY) {
                        LinkedHashMap<String, Boolean> mask = opLimits.getBitmask("mask");
                        if (mask.containsKey("AREA") && mask.get("AREA")) {

                            Element opLimitsElm = vs.addElement("oplimits");

                            double latDegrees = Math.toDegrees(opLimits.getDouble("lat"));
                            double lonDegrees = Math.toDegrees(opLimits.getDouble("lon"));
                            double orientationDegrees = Math.toDegrees(opLimits.getDouble("orientation"));
                            double width = opLimits.getDouble("width");
                            double height = opLimits.getDouble("length");

                            opLimitsElm.addAttribute("lat", "" + latDegrees);
                            opLimitsElm.addAttribute("lon", "" + lonDegrees);
                            opLimitsElm.addAttribute("orientation", "" + orientationDegrees);
                            opLimitsElm.addAttribute("width", "" + width);
                            opLimitsElm.addAttribute("height", "" + height);
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // Lets put plans path if exists
            if (vehicleToPlanIds.containsKey(sys.getName())) {
                String vehStr = sys.getName();
                String planStr = vehicleToPlanIds.get(vehStr);

                Vector<LocationType> locs = null;
                PathElement path = null;
                if ((getConsole() != null && getConsole().getMission() != null && getConsole().getMission()
                        .getIndividualPlansList() != null)) {
                    PlanType pt = getConsole().getMission().getIndividualPlansList().get(planStr);
                    if (pt != null) {
                        if (!planToLocations.containsKey(planStr)) {
                            locs = PublishHelper.planPathLocs(pt);
                            path = PublishHelper.planPathElement(locs, vehStr + ":" + planStr);
                            planToLocations.put(pt.getId(), locs);
                            planToPath.put(pt.getId(), path);
                        }
                        else {
                            locs = planToLocations.get(pt.getId());
                            path = planToPath.get(pt.getId());
                        }
                    }
                }
                if (path != null) {
                    Element planPathElement = path.asElement();
                    Element planElm = vs.addElement("plan");
                    planElm.add(planPathElement);
                }
            }
        }

        ExternalSystem[] externals = publishExternalSystems ? ExternalSystemsHolder.lookupAllSystems()
                : new ExternalSystem[0];
        for (ExternalSystem sys : externals) {
            // if ((System.currentTimeMillis() - sys.getLocationTimeMillis() < publishPeriodMillis))
            // continue;

            Element vs = ms.addElement("VehicleState");
            vs.addAttribute("id", sys.getName()); // vtl.getVehicleId());

            try {
                if (System.currentTimeMillis() - sys.getLocationTimeMillis() > DateTimeUtil.HOUR) {
                    ms.remove(vs);
                    continue;
                }
                if (sys.getLocation().getLatitudeDegs() == 0d
                        && sys.getLocation().getLongitudeDegs() == 0d) {
                    ms.remove(vs);
                    continue;
                }

                Date date = new Date(sys.getLocationTimeMillis());
                vs.addAttribute("time", DateTimeUtil.timeFormatterUTC.format(date));
                vs.addAttribute("date", DateTimeUtil.dateFormatterUTC.format(date));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Element crd = new LocationType(sys.getLocation()).asElement(); // vtl.getState().getPosition().asElement();
            vs.add(crd);
            Element att = vs.addElement("attitude");
            att.addElement("phi").addText("" + sys.getRollDegrees()); // vtl.getState().getRoll());
            att.addElement("theta").addText("" + sys.getPitchDegrees()); // vtl.getState().getPitch());
            att.addElement("psi").addText("" + sys.getYawDegrees()); // vtl.getState().getYaw());

            vs.addElement("imc");
        }

        if (publishBeaconsState) {
            if (getConsole() != null && getConsole().getMission() != null) {
                MissionType mt = getConsole().getMission();
                Vector<TransponderElement> ts = MapGroup.getMapGroupInstance(mt).getAllObjectsOfType(
                        TransponderElement.class);
                for (TransponderElement beacon : ts) {
                    Element vs = ms.addElement("VehicleState");
                    vs.addAttribute("id", beacon.getId()); // vtl.getVehicleId());
                    LocationType locBeacon = beacon.getCenterLocation().getNewAbsoluteLatLonDepth();
                    try {
                        // if (System.currentTimeMillis() - beacon.getCenterLocation() > DateTimeUtil.HOUR) {
                        // ms.remove(vs);
                        // continue;
                        // }
                        if (locBeacon.getLatitudeDegs() == 0d && locBeacon.getLongitudeDegs() == 0d) {
                            ms.remove(vs);
                            continue;
                        }

                        Date date = new Date(System.currentTimeMillis());
                        vs.addAttribute("time", DateTimeUtil.timeFormatterUTC.format(date));
                        vs.addAttribute("date", DateTimeUtil.dateFormatterUTC.format(date));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    Element crd = locBeacon.asElement(); // vtl.getState().getPosition().asElement();
                    vs.add(crd);
                    Element att = vs.addElement("attitude");
                    att.addElement("phi").addText("" + 0); // vtl.getState().getRoll());
                    att.addElement("theta").addText("" + 0); // vtl.getState().getPitch());
                    att.addElement("psi").addText("" + 0); // vtl.getState().getYaw());

                    vs.addElement("imc");
                }
            }
        }

        String xml = doc.asXML();
        // NeptusLog.pub().info("<###> "+xml);

        if (postHttpRequestPublishState != null)
            postHttpRequestPublishState.abort();
        postHttpRequestPublishState = null;
        try {
            String uri = pubURL + "state";
            postHttpRequestPublishState = new HttpPost(uri);
            NameValuePair nvp_type = new BasicNameValuePair("type", "state");
            NameValuePair nvp_xml = new BasicNameValuePair("xml", xml);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(nvp_type);
            nvps.add(nvp_xml);

            postHttpRequestPublishState.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpClientContext localContext = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(postHttpRequestPublishState, localContext);
//            ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, localContext);

            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>publishData [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (postHttpRequestPublishState != null) {
                    postHttpRequestPublishState.abort();
                }
                return false;
            }
        }
        catch (Exception e) {
            // e.printStackTrace();
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            if (postHttpRequestPublishState != null) {
                postHttpRequestPublishState.abort();
                postHttpRequestPublishState = null;
            }
        }

        try {
            // "EstimatedState, LbLConfig, PlanSpecification, VehicleState, PlanControlState, EntityList";
            Vector<IMCMessage> msgLst = new Vector<IMCMessage>();
            synchronized (estimatedStates) {
                // msgLst = IMCUtils.asMessageList(estimatedStates.values());
                msgLst.addAll(estimatedStates.values());
                estimatedStates.clear();
            }
            synchronized (lblConfigLock) {
                if (lblConfig != null) {
                    msgLst.add(lblConfig);
                    lblConfig = null;
                }
            }
            synchronized (planSpecifications) {
                msgLst.addAll(planSpecifications.values());
                planSpecifications.clear();
            }
            synchronized (vehicleStates) {
                msgLst.addAll(vehicleStates.values());
                vehicleStates.clear();
            }
            synchronized (planControlStates) {
                msgLst.addAll(planControlStates.values());
                planControlStates.clear();
            }
            synchronized (entityLists) {
                msgLst.addAll(entityLists.values());
                entityLists.clear();
            }
            synchronized (otherLists) {
                // msgLst.addAll(otherLists.values());
                for (Vector<IMCMessage> vecMsgs : otherLists.values()) {
                    msgLst.addAll(vecMsgs);
                    vecMsgs.clear();
                }
                otherLists.clear();
            }

            if (msgLst != null && msgLst.size() > 0)
                postMessages(msgLst);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private boolean publishPlan() {

        if (!publishActiveConsolePlan || getConsole().getPlan() == null)
            return false;

        // String xml = doc.asXML();
        String xml;
        try {
            xml = PublishHelper.createPlanPath(getConsole().getMission(), getConsole().getPlan());
        }
        catch (Exception e1) {
            e1.printStackTrace();
            return false;
        }
        // NeptusLog.pub().info("<###> "+xml);

        if (postHttpRequestPublishPlan != null)
            postHttpRequestPublishPlan.abort();
        postHttpRequestPublishPlan = null;
        try {
            String uri = pubURL + "state";
            postHttpRequestPublishPlan = new HttpPost(uri);
            NameValuePair nvp_type = new BasicNameValuePair("type", "plan");
            NameValuePair nvp_xml = new BasicNameValuePair("xml", xml);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(nvp_type);
            nvps.add(nvp_xml);

            postHttpRequestPublishPlan.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpClientContext localContext = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(postHttpRequestPublishPlan, localContext);
//            ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, localContext);

            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>publishData [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (postHttpRequestPublishPlan != null) {
                    postHttpRequestPublishPlan.abort();
                }
                return false;
            }
        }
        catch (Exception e) {
            // e.printStackTrace();
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            if (postHttpRequestPublishPlan != null) {
                postHttpRequestPublishPlan.abort();
                postHttpRequestPublishPlan = null;
            }
        }

        return true;
    }

    /**
     * 
     */
    private boolean postMessages(Collection<IMCMessage> messages) {
        HttpPost post = null;
        try {
            String uri = pubURL + "imc/";
            post = new HttpPost(uri);

            Vector<Byte> bv = new Vector<Byte>();
            for (IMCMessage msg : messages) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IMCOutputStream ios = new IMCOutputStream(baos);
                msg.serialize(ios);
                byte[] ba = baos.toByteArray();
                for (byte b : ba) {
                    bv.add(b);
                }
            }
            byte[] bf = new byte[bv.size()];
            int i = 0;
            for (byte b : bv) {
                bf[i++] = b;
            }
            ByteArrayInputStream inStream = new ByteArrayInputStream(bf);
            InputStreamEntity reqEntity = new InputStreamEntity(inStream, -1);
            reqEntity.setContentType("application/lsf");
            reqEntity.setChunked(true);

            post.setEntity(reqEntity);

            HttpClientContext context = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(post, context);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, context);

            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>postMessages [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (post != null) {
                    post.abort();
                }
                return false;
            }
            try {
                InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
                @SuppressWarnings("unused")
                long fullSize = iGetResultCode.getEntity().getContentLength();
                String timeStr = StringEscapeUtils.escapeCsv(StreamUtil.copyStreamToString(streamGetResponseBody).trim());
                @SuppressWarnings("unused")
                long serverTime = Long.parseLong(timeStr);
                // NeptusLog.pub().info("<###>server time delta: " +
                // (System.currentTimeMillis() - serverTime) + "ms");
            }
            catch (Exception e) {
                // e.printStackTrace();
                NeptusLog.pub().info("<###> "+e.getMessage());
            }
        }
        catch (Exception e) {
            // e.printStackTrace();
            NeptusLog.pub().info("<###> "+e.getMessage());
        }
        finally {
            if (post != null) {
                post.abort();
                post = null;
            }
        }
        return true;
    }

    private boolean getRemoteImcData() {
        if (!publishWebReceivedMessages) {
            return true;
        }

        if (getHttpRequestImcMsg != null)
            getHttpRequestImcMsg.abort();
        getHttpRequestImcMsg = null;
        long time = (lastFetchPosTimeMillis <= 0 ? 0 : lastFetchPosTimeMillis);
        try {
            String endpoint = pubURL; // GeneralPreferences.getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
            String uri = endpoint + "imc?after=" + time; // + "/state.xml";
            getHttpRequestImcMsg = new HttpGet(uri);
            @SuppressWarnings("unused")
            long reqTime = System.currentTimeMillis();
            
            HttpClientContext context = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequestImcMsg, context);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, context);
            
            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>getRemoteImcData [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (getHttpRequestImcMsg != null) {
                    getHttpRequestImcMsg.abort();
                }
                return false;
            }
            try {
                long serverTime = Long.parseLong(iGetResultCode.getFirstHeader("server-time").getValue().trim());
                // NeptusLog.pub().info("<###>server time delta: " + (reqTime -
                // serverTime) + "ms");
                lastFetchPosTimeMillis = serverTime;
            }
            catch (Exception e) {
                lastFetchPosTimeMillis = System.currentTimeMillis();
            }
            InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
            @SuppressWarnings("unused")
            long fullSize = iGetResultCode.getEntity().getContentLength();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            @SuppressWarnings("unused")
            boolean streamRes = StreamUtil.copyStreamToStream(streamGetResponseBody, baos);
            // ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
            // baos.flush();
            byte[] baa = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(baa);
            IMCMessage[] msgs = IMCUtils.parseLsf(bais);
            if (msgs.length > 0) {
                processWebMessages(msgs);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (getHttpRequestImcMsg != null) {
                getHttpRequestImcMsg.abort();
                getHttpRequestImcMsg = null;
            }
        }
        return true;
    }

    /**
     * @param msgs
     */
    private void processWebMessages(IMCMessage[] msgs) {
        if (publishWebReceivedMessages) {
            for (IMCMessage msg : msgs) {
                try {
                    ImcId16 id = new ImcId16(msg.getHeader().getValue("src"));
                    if (!debugOn) {
                        if (!ImcId16.NULL_ID.equals(ImcMsgManager.getManager().getLocalId())
                                && ImcMsgManager.getManager().getLocalId().equals(id))
                            return;
                    }

                    MessageInfo info = new MessageInfoImpl();
                    info.setTimeSentNanos((long) (msg.getHeader().getTimestamp() * 1E9));
                    info.setTimeReceivedNanos(lastFetchPosTimeMillis * (long) 1E6);
                    info.setProperty(MessageInfo.NOT_TO_LOG_MSG_KEY, "true");
                    info.setProperty(MessageInfo.WEB_FETCH_MSG_KEY, "true");

                    ImcSystem sys = ImcSystemsHolder.lookupSystem(id);
                    if (sys == null) {
                        if (id.intValue() >= 0x4000 && id.intValue() < 0x5FFF) {
                            // So it's a CCU from web
                            processWebCCUMessage(info, msg);
                        }
                        continue;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param info
     * @param msg
     */
    private void processWebCCUMessage(MessageInfo info, IMCMessage msg) {
        // NeptusLog.pub().info("<###> "+msg.asJSON());

        if ("PlanSpecification".equalsIgnoreCase(msg.getAbbrev())) {
            try {
                String planId = msg.getAsString("plan_id");
                ImcId16 imcId = null;
                try {
                    imcId = new ImcId16(msg.getHeaderValue("src"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                String srcId = (imcId == null ? msg.getHeaderValue("src").toString() : imcId.toString());

                int res = JOptionPane.showConfirmDialog(getConsole(), "Plan with id '" + planId
                        + "' just arrived from " + srcId + ". Want to accept it?",
                        PluginUtils.getPluginName(this.getClass()), JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }

                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), msg);
                if (getConsole().getMission().getIndividualPlansList().containsKey(planId)) {
                    res = JOptionPane.showConfirmDialog(getConsole(), "Overwrite existing plan?", "Plan editor",
                            JOptionPane.YES_NO_OPTION);
                    if (res != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                getConsole().getMission().getIndividualPlansList().put(planId, plan);

                new Thread() {
                    @Override
                    public void run() {
                        getConsole().getMission().save(false);
                    }
                }.start();

                getConsole().updateMissionListeners();
                getConsole().setPlan(plan);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            ImcId16 imcId = null;
            try {
                try {
                    imcId = new ImcId16(msg.getHeaderValue("src"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                String srcId = (imcId == null ? msg.getHeaderValue("src").toString() : imcId.toString());

                imcId = new ImcId16(msg.getHeaderValue("dst"));
                ImcSystem sys = ImcSystemsHolder.lookupSystem(imcId);

                if (sys != null) {
                    int res = JOptionPane.showConfirmDialog(getConsole(), "Message '" + msg.getAbbrev()
                            + "' just arrived from " + srcId + " to " + sys.getName() + ". \nWant to foward it?\n >> "
                            + StringUtils.wrapEveryNChars(msg.asJSON(), (short) 100, 300, true) + " <<",
                            PluginUtils.getPluginName(this.getClass()), JOptionPane.YES_NO_OPTION);
                    if (res != JOptionPane.YES_OPTION) {
                        return;
                    }
                    ImcMsgManager.getManager().sendMessage(msg, imcId, null);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean getRemoteStateData() {
        if (!fetchRemoteSystemsOn)
            return false;

        if (getHttpRequestRemoteState != null)
            getHttpRequestRemoteState.abort();
        getHttpRequestRemoteState = null;
        try {
            String endpoint = pubURL; // GeneralPreferences.getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
            String uri = endpoint + "state/state.xml"; // + "/state.xml";
            getHttpRequestRemoteState = new HttpGet(uri);
            
            HttpClientContext context = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequestRemoteState, context);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, context);
            
            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server");
                if (getHttpRequestRemoteState != null) {
                    getHttpRequestRemoteState.abort();
                }
                return false;
            }
            InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();

            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            org.w3c.dom.Document docProfiles = builder.parse(streamGetResponseBody);

            RemotePositionHelper.getRemoteState(timeSysList, locSysList, docProfiles);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            if (getHttpRequestRemoteState != null) {
                getHttpRequestRemoteState.abort();
                getHttpRequestRemoteState = null;
            }
        }

        processRemoteStates();

        return true;
    }

    private void processRemoteStates() {
        if (publishRemoteSystemsLocally)
            RemotePositionHelper.publishRemoteStatesLocally(timeSysList, locSysList);
    }

    // Vehicles' Plans processing
    public void processPlanControlStateForVehiclePlanInformation(IMCMessage msg) {
        try {
            PlanControlState pcstate = PlanControlState.clone(msg);
            ImcSystem system = ImcSystemsHolder.lookupSystem(msg.getSrc());
            if (system == null)
                return;
            String sysId = system.getName();
            switch (pcstate.getState()) {
                case INITIALIZING:
                case EXECUTING:
                    vehicleToPlanIds.put(sysId, pcstate.getPlanId());
                    break;
                case BLOCKED:
                case READY:
                    vehicleToPlanIds.remove(sysId);
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {

        removeCheckMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Start/Stop");
        removeMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Settings");

        if (httpComm != null) {
            httpComm.cleanUp();;
        }
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        msgSysList.clear();

        systemsMessageListener.clean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (!pubURL.endsWith("/"))
            pubURL += "/";

        publishMessagesList = publishMessagesListStd;

        Matcher m = messageListPattern.matcher(publishMessagesListExtra);
        boolean b = m.matches();
        if (b)
            publishMessagesList += ", " + publishMessagesListExtra;

        msgSysList.clear();
        if (publishMessagesList.length() != 0) {
            try {
                m = messageListPattern.matcher(publishMessagesList);
                b = m.matches();
                if (b) {
                    for (String pme : publishMessagesList.split(",")) {
                        String[] kk = pme.split(":");
                        msgSysList.put(kk[0].trim(), kk.length == 1 ? 0 : Integer.parseInt(kk[1].trim()));
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        systemsMessageListener.setMessagesToListen(msgSysList.keySet().toArray(new String[0]));

        if (!publishOn && !fetchRemoteSystemsOn)
            active = false;

        abortAllActiveConnections();
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, publishPeriodMillis);
    }

    private void abortAllActiveConnections() {
        abortPublishOnAllActiveConnections();
        abortFetchOnAllActiveConnections();
    }

    private void abortPublishOnAllActiveConnections() {
        try {
            if (postHttpRequestPublishState != null)
                postHttpRequestPublishState.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (getHttpRequestImcMsg != null)
                getHttpRequestImcMsg.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (postHttpRequestPublishPlan != null)
                postHttpRequestPublishPlan.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abortFetchOnAllActiveConnections() {
        try {
            if (getHttpRequestRemoteState != null)
                getHttpRequestRemoteState.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
