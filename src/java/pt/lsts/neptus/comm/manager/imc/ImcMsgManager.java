/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2007/05/25 pdias
 */
package pt.lsts.neptus.comm.manager.imc;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.InvalidNameException;
import javax.swing.JFrame;

import com.google.common.collect.HashBiMap;
import com.google.common.eventbus.AsyncEventBus;

import pt.lsts.imc.Announce;
import pt.lsts.imc.AssetReport;
import pt.lsts.imc.EntityInfo;
import pt.lsts.imc.EntityList;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.ReportedState;
import pt.lsts.imc.StateReport;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.CommUtil;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.NoTransportAvailableException;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.CommBaseManager;
import pt.lsts.neptus.comm.manager.CommManagerStatusChangeListener;
import pt.lsts.neptus.comm.manager.MessageFrequencyCalculator;
import pt.lsts.neptus.comm.manager.imc.ImcSystem.IMCAuthorityState;
import pt.lsts.neptus.comm.transports.ImcTcpTransport;
import pt.lsts.neptus.comm.transports.ImcUdpTransport;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.MessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystem.ExternalTypeEnum;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.NetworkInterfacesUtil;
import pt.lsts.neptus.util.NetworkInterfacesUtil.NInterface;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

/**
 * @author pdias
 */
public class ImcMsgManager extends
CommBaseManager<IMCMessage, MessageInfo, SystemImcMsgCommInfo, ImcId16, CommManagerStatusChangeListener> {

    public static final String TRANSPORT_UDP = "UDP";
    public static final String TRANSPORT_TCP = "TCP";

    private static final int DEFAULT_UDP_VEH_PORT = 6002;

    /**
     * Singleton
     */
    private static ImcMsgManager commManager = null;

    private ImcId16 localId = ImcId16.NULL_ID;
    private boolean sameIdErrorDetected = false;
    private long sameIdErrorDetectedTimeMillis = -1;

    protected IMCFragmentHandler fragmentHandler;;
    
    protected ImcSystemState imcState;

    // public static String CCU_VEH_STRING = "CCU-VEH";
    // public static String VEH_CCU_STRING = "VEH-CCU";
    // //public static final String MW_NODE_CONSOLE_PREFIX = "Console-";
    //
    // public static String CONNECTION4CCU = "Connection4CCUALL";

    private final HashBiMap<String, ImcId16> udpOnIpMapper = HashBiMap.create(); // new HashBiMap<String, ImcId16>();
    private boolean isFilterByPort = false;

    private boolean isRedirectToFirst = false;

    private boolean logSentMsg = false;

    @Deprecated
    private boolean dontIgnoreIpSourceRequest = true;

    private boolean multicastEnabled = true;
    private String multicastAddress = "224.0.75.69";
    private int[] multicastPorts = new int[] { 6969 };
    private boolean broadcastEnabled = true;

    private final IMCDefinition imcDefinition; // = IMCDefinition.getInstance();
    private AnnounceWorker announceWorker; // = new AnnounceWorker(this, imcDefinition);
    private long announceLastArriveTime = -1;

    private final PreferencesListener gplistener;

    // Transports
    protected ImcUdpTransport udpTransport = null;
    protected ImcUdpTransport multicastUdpTransport = null;
    protected ImcTcpTransport tcpTransport = null;

    //    protected Vector<Object> registeredObjects = new Vector<>();

    // EventBus
    private final ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {
        private final String namePrefix = ImcMsgManager.class.getSimpleName() + "::"
                + Integer.toHexString(ImcMsgManager.this.hashCode());
        private final AtomicInteger counter = new AtomicInteger(0);
        private final ThreadGroup group = new ThreadGroup(namePrefix);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r);
            t.setName("Message Event Bus " + (counter.getAndIncrement()));
            t.setDaemon(true);
            return t;
        }
    });
    protected AsyncEventBus bus = new AsyncEventBus(service);

    protected HashSet<Object> busListeners = new HashSet<>();

    // Network interfaces control
    private long lastNetInterfaceMillis = -1;
    private Vector<NInterface> netInterfaces = null;

    private final LinkedList<URL> registerServicesToAnnounce = new LinkedList<URL>();

    // Frequency Calculators
    private final MessageFrequencyCalculator toSendMessagesFreqCalc = new MessageFrequencyCalculator();
    private final MessageFrequencyCalculator sentMessagesFreqCalc = new MessageFrequencyCalculator();

    // Entities related vars
    protected short lastEntityId = 0;
    protected LinkedHashMap<String, Short> neptusEntities = new LinkedHashMap<String, Short>();

    private enum TransportPreference {
        ANY,
        UDP,
        TCP
    };

    public enum SendResult {
        SUCCESS,
        ERROR,
        TIMEOUT,
        UNREACHABLE,
        UNCERTAIN_DELIVERY    
    }

    private final ArrayList<TransportPreference> transportPreferenceToUse = new ArrayList<>();

    protected Vector<String> ignoredClasses = new Vector<String>();
    {
        ignoredClasses.add(getClass().getName());
        ignoredClasses.add(ConsolePanel.class.getName());
        ignoredClasses.add(Thread.class.getName());
        ignoredClasses.add(IMCSendMessageUtils.class.getName());
    }

    public void registerBusListener(Object listener) {
        if (!busListeners.contains(listener)) {
            getMessageBus().register(listener);
        }
        busListeners.add(listener);
    }

    public void unregisterBusListener(Object listener) {
        if (busListeners.contains(listener)) {
            getMessageBus().unregister(listener);
        }
        busListeners.remove(listener);
    }

    /**
     * @return The singleton manager.
     */
    public static ImcMsgManager getManager() {
        return createManager();
    }

    private static synchronized ImcMsgManager createManager() {
        if (commManager == null) {
            commManager = new ImcMsgManager(IMCDefinition.getInstance());
        }
        return commManager;
    }

    public ImcMsgManager(IMCDefinition imcDefinition) {
        super();
        gplistener = new PreferencesListener() {
            @Override
            public void preferencesUpdated() {
                logSentMsg = GeneralPreferences.messageLogSentMessages;
                isRedirectToFirst = GeneralPreferences.redirectUnknownIdsToFirstCommVehicle;

                dontIgnoreIpSourceRequest = GeneralPreferences.imcChangeBySourceIpRequest;

                // IMC Announce
                announceWorker.getAnnounceMessage().setValue("sys_name", StringUtils.toImcName(GeneralPreferences.imcCcuName));
                announceWorker.setUseUnicastAnnounce(GeneralPreferences.imcUnicastAnnounceEnable);

                localId = GeneralPreferences.imcCcuId;
            }
        };
        this.imcDefinition = imcDefinition;
        announceWorker = new AnnounceWorker(this, imcDefinition);
        
        fragmentHandler = new IMCFragmentHandler(imcDefinition);
        imcState = new ImcSystemState(imcDefinition);
        imcState.setIgnoreEntities(true);

        GeneralPreferences.addPreferencesListener(gplistener);
        gplistener.preferencesUpdated();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.comm.manager.CommBaseManager#start()
     */
    @Override
    public synchronized boolean start() {
        if (started)
            return true; // do nothing

        NeptusLog.pub().info("Starting IMC comms");

        boolean ret = super.start();
        if (!ret)
            return false;

        gplistener.preferencesUpdated();
        isFilterByPort = GeneralPreferences.filterUdpAlsoByPort;
        localId = GeneralPreferences.imcCcuId;

        updateUdpOnIpMapper();

        return ret;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.comm.manager.CommBaseManager#stop()
     */
    @Override
    public synchronized boolean stop() {
        NeptusLog.pub().info("Stoping IMC comms");
        return super.stop();
    }
    
    /**
     * Sends a message to local consumers
     * @param srcName The name of the message sender
     * @param message The message to be sent
     */
    public void postInternalMessage(String srcName, IMCMessage message) {
        MessageInfoImpl minfo = new MessageInfoImpl();
        minfo.setPublisher(srcName);
        minfo.setPublisherInetAddress("127.0.0.1");
        minfo.setPublisherPort(6001);
        minfo.setTimeReceivedSec(System.currentTimeMillis() / 1000.0);
        checkAndSetMessageSrcEntity(message);
        
        //message.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
        //message.setSrcEnt(255);
        
        onMessage(minfo, message);
        
        bus.post(message);
    }

    private void updateUdpOnIpMapper() {
        for (SystemImcMsgCommInfo vsci : commInfo.values()) {
            if (isUdpOn())
                udpOnIpMapper.forcePut(vsci.getIpAddress() + (isFilterByPort ? ":" + vsci.getIpRemotePort() : ""),
                        vsci.getSystemCommId());
            else
                udpOnIpMapper.remove(vsci.getIpAddress() + (isFilterByPort ? ":" + vsci.getIpRemotePort() : ""));
        }
    }

    private void updateUdpOnIpMapper(SystemImcMsgCommInfo vsci) {
        if (isUdpOn())
            udpOnIpMapper.forcePut(vsci.getIpAddress() + (isFilterByPort ? ":" + vsci.getIpRemotePort() : ""),
                    vsci.getSystemCommId());
        else
            udpOnIpMapper.remove(vsci.getIpAddress() + (isFilterByPort ? ":" + vsci.getIpRemotePort() : ""));
    }

    public synchronized SystemImcMsgCommInfo initVehicleCommInfo(String vehicleId, String inetAddress) {
        VehicleType veh = VehiclesHolder.getVehicleById(vehicleId);
        if (veh == null)
            return null;

        ImcId16 imcId = veh.getImcId();
        if (imcId == null)
            return null;
        return initSystemCommInfo(imcId, inetAddress);
    }

    @Override
    public synchronized SystemImcMsgCommInfo initSystemCommInfo(ImcId16 vIdS, String inetAddress) {
        SystemImcMsgCommInfo vci = commInfo.get(vIdS);
        if (vci != null) {
            NeptusLog.pub().debug("System already known to manager: " + vIdS);
            return vci;
        }

        NeptusLog.pub().debug("System not yet known to manager: " + vIdS);

        SystemImcMsgCommInfo vsci = new SystemImcMsgCommInfo();
        vsci.setMessageBus(bus);
        vsci.setSystemCommId(vIdS);
        
        boolean sysNameSet = false;

        VehicleType vehTmp = VehiclesHolder.getVehicleWithImc(vIdS);
        if (vehTmp == null) {
            NeptusLog.pub().warn("No vehicle with IMC ID " + vIdS + "!");
        }
        else {
            // VehicleType vehicle = vehTmp;
            NeptusLog.pub().debug("Found the Vehicle: " + vehTmp.getId());
            vsci.setSystemIdName(vehTmp.getId());
            sysNameSet = true;
        }

        ImcSystem resSys = ImcSystemsHolder.lookupSystem(vIdS);
        if (resSys == null) {
            if (vehTmp != null) {
                resSys = new ImcSystem(vehTmp);
                resSys.setType(ImcSystem.translateSystemTypeFromMessage(vehTmp.getType().toUpperCase()));
                resSys.setTypeVehicle(ImcSystem.translateVehicleTypeFromMessage(vehTmp.getType().toUpperCase()));
                if (resSys.getType() != SystemTypeEnum.CCU)
                    resSys.setAuthorityState(IMCAuthorityState.SYSTEM_FULL);
                ImcSystemsHolder.registerSystem(resSys);
            }
            else {
                InetSocketAddress isa = ImcSystem.parseInetSocketAddress(inetAddress);
                if (isa != null) {
                    resSys = new ImcSystem(vIdS, ImcSystem.createCommMean(isa.getAddress().getHostAddress(),
                            isa.getPort(), isa.getPort(), vIdS, true, false));
                    ImcSystemsHolder.registerSystem(resSys);
                }
                else {
                    resSys = new ImcSystem(vIdS);
                    ImcSystemsHolder.registerSystem(resSys);
                }
            }
        }

        if (!sysNameSet) {
            if (resSys.getName().equalsIgnoreCase(resSys.getId().toHexString())) {
                String name = imcDefinition.getResolver().resolve(resSys.getId().intValue());
                if (!name.contains("unknown")) {
                    vsci.setSystemIdName(name);
                    resSys.setName(name);
                }
            }
        }
        
        if (vsci.initSystemComms()) {
            if (!vsci.startSystemComms()) {
                NeptusLog.pub().error("Error starting " + vsci.getSystemIdName());
            }
        }
        else {
            NeptusLog.pub().error("Error initializing " + vsci.getSystemIdName());
        }

        updateUdpOnIpMapper(vsci);

        commInfo.put(vIdS, vsci);

        if (vehTmp != null) {
            sendManagerVehicleAdded(vehTmp);
            sendManagerVehicleStatusChanged(vehTmp, SYS_COMM_ON);
        }
        else {
            sendManagerSystemAdded(vIdS);
            sendManagerSystemStatusChanged(vIdS, SYS_COMM_ON);
        }
        return vsci;
    }

    protected String getAnnounceServicesList() {
        String ret = "";
        String loopbackServ = "";
        Vector<NInterface> netInterfaces = getNetworkInterfaces(); // NetworkInterfacesUtil.getNetworkInterfaces();
        for (NInterface ni : netInterfaces) {
            if (ni.isLoopback()) {
                if (getUdpTransport() != null) {
                    for (Inet4Address i4a : ni.getAddress()) {
                        loopbackServ += "imc+udp://" + i4a.getHostAddress() + ":" + getUdpTransport().getBindPort()
                                + "/;";
                    }
                }
                if (getTcpTransport() != null) {
                    for (Inet4Address i4a : ni.getAddress()) {
                        loopbackServ += "imc+tcp://" + i4a.getHostAddress() + ":" + getTcpTransport().getBindPort()
                                + "/;";
                    }
                }
                continue;
            }
            if (ni.hasIpv4Address()) {
                if (getUdpTransport() != null) {
                    for (Inet4Address i4a : ni.getAddress()) {
                        ret += "imc+udp://" + i4a.getHostAddress() + ":" + getUdpTransport().getBindPort() + "/;";
                    }
                }
                if (getTcpTransport() != null) {
                    for (Inet4Address i4a : ni.getAddress()) {
                        ret += "imc+tcp://" + i4a.getHostAddress() + ":" + getTcpTransport().getBindPort() + "/;";
                    }
                }
                synchronized (registerServicesToAnnounce) {
                    for (URL url : registerServicesToAnnounce) {
                        try {
                            URI uri = url.toURI();
                            String host = uri.getHost();
                            Vector<String> hostsStr = new Vector<String>();
                            if ("localhost".equalsIgnoreCase(host)) {
                                for (Inet4Address i4a : ni.getAddress()) {
                                    hostsStr.add(i4a.getHostAddress());
                                }
                            }
                            else
                                hostsStr.add(host);
                            for (String hst : hostsStr) {
                                URL serv = new URI(uri.getScheme(), uri.getUserInfo(), hst, uri.getPort(),
                                        uri.getPath(), uri.getQuery(), uri.getFragment()).toURL();
                                ret += serv.toString() + ";";
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            // if (ni.hasIpv6Address()) {
            // ret += "imc+udp://[" + ni.getAddress6().getHostAddress() + "]" +
            // ":" + getUdpTransport().getBindPort() + "/;";
            // }
        }
        return ret.length() != 0 ? ret : loopbackServ;
    }

    private synchronized Vector<NInterface> getNetworkInterfaces() {
        if (netInterfaces == null || (System.currentTimeMillis() - lastNetInterfaceMillis > 3000)) {
            netInterfaces = NetworkInterfacesUtil.getNetworkInterfaces();
            lastNetInterfaceMillis = System.currentTimeMillis();
        }
        return netInterfaces;
    }

    /**
     * @param service Register with 'localhost' so it can be substituted with active IPs
     */
    public boolean registerService(URL service) {
        synchronized (registerServicesToAnnounce) {
            if (!registerServicesToAnnounce.contains(service))
                return registerServicesToAnnounce.add(service);
            return false;
        }
    }

    /**
     * @param service
     * @return
     */
    public boolean unRegisterService(URL service) {
        synchronized (registerServicesToAnnounce) {
            return registerServicesToAnnounce.remove(service);
        }
    }

    public String getAllServicesString() {
        return announceWorker.getAllServices();
    }

    @Override
    protected boolean initManagerComms() {

        String transportsToUse = GeneralPreferences.imcTransportsToUse; // GeneralPreferences.getProperty(GeneralPreferences.IMC_TRANSPORTS);
        boolean udpConfig = StringUtils.isTokenInList(transportsToUse, TRANSPORT_UDP);

        if (udpConfig) {
            createUdpTransport();
            // udpTransport.addListener(this);
        }
        else {
            udpTransport = null;
        }

        // Multicast Comms
        createMulticastUdpTransport();

        boolean tcpConfig = StringUtils.isTokenInList(transportsToUse, TRANSPORT_TCP);
        if (tcpConfig) {
            createTcpTransport();
        }
        else {
            tcpTransport = null;
        }

        transportPreferenceToUse.clear();
        String[] tks = transportsToUse.split("[ ,]+");
        for (String t : tks) {
            if (TRANSPORT_UDP.equalsIgnoreCase(t) && isUdpOn()) {
                transportPreferenceToUse.add(TransportPreference.UDP);
            }
            else if (TRANSPORT_TCP.equalsIgnoreCase(t) && isTcpOn()) {
                transportPreferenceToUse.add(TransportPreference.TCP);
            }
        }
        return true;
    }

    @Override
    protected boolean startManagerComms() {
        // initManagerComms(); // This is also called in the start, so 2 times!!!

        if (udpTransport != null) {
            udpTransport.reStart();
            udpTransport.addListener(this);
        }

        if (multicastUdpTransport != null) {
            multicastUdpTransport.reStart();
            multicastUdpTransport.addListener(this);
            if (announceWorker != null) {
                announceWorker.startAnnounceAndPeriodicRequests();
            }
        }

        if (tcpTransport != null) {
            tcpTransport.reStart();
            tcpTransport.addListener(this);
        }

        return true;
    }

    @Override
    protected boolean stopManagerComms() {
        if (udpTransport != null) {
            udpTransport.removeListener(this);
            udpTransport.stop();
        }

        if (multicastUdpTransport != null) {
            multicastUdpTransport.removeListener(this);
            multicastUdpTransport.stop();
            if (announceWorker != null) {
                announceWorker.stopAnnounce();
            }
        }

        if (tcpTransport != null) {
            tcpTransport.removeListener(this);
            tcpTransport.stop();
        }

        return true;
    }

    /**
     * 
     */
    private void createUdpTransport() {
        int localport = GeneralPreferences.commsLocalPortUDP;

        if (udpTransport == null) {
            udpTransport = new ImcUdpTransport(localport, imcDefinition);
        }
        else {
            udpTransport.setBindPort(localport);
        }
        udpTransport.reStart();

        if (udpTransport.isOnBindError()) {
            for (int i = 1; i < 10; i++) {
                udpTransport.stop();
                udpTransport.setBindPort(localport + i);
                udpTransport.reStart();
                if (!udpTransport.isOnBindError())
                    break;
            }
        }
    }

    private ImcUdpTransport getUdpTransport() {
        return udpTransport;
    }

    /**
     * @return
     */
    public boolean isUdpOn() {
        if (udpTransport == null || !udpTransport.isRunnning())
            return false;
        return true;
    }

    private void createTcpTransport() {
        int localport = GeneralPreferences.commsLocalPortTCP;

        if (tcpTransport == null) {
            tcpTransport = new ImcTcpTransport(localport, imcDefinition);
        }
        else {
            tcpTransport.setBindPort(localport);
        }
        tcpTransport.reStart();

        if (tcpTransport.isOnBindError()) {
            for (int i = 1; i < 10; i++) {
                tcpTransport.stop();
                tcpTransport.setBindPort(localport + i);
                tcpTransport.reStart();
                if (!tcpTransport.isOnBindError())
                    break;
            }
        }

    }

    private ImcTcpTransport getTcpTransport() {
        return tcpTransport;
    }

    /**
     * @return
     */
    public boolean isTcpOn() {
        if (tcpTransport == null || !tcpTransport.isRunning())
            return false;
        return true;
    }

    boolean isTCPConnectionEstablished(String host, int port) {
        if (getTcpTransport() == null)
            return false;
        return getTcpTransport().isConnectionEstablished(host, port);
    }

    // @Override
    // public synchronized boolean shutdown() {
    // if (udpTransport != null)
    // udpTransport.stopAll();
    // return super.shutdown();
    // }

    /**
     * @return the announceLastArriveTime
     */
    public long getAnnounceLastArriveTime() {
        return announceLastArriveTime;
    }

    private void createMulticastUdpTransport() {
        multicastEnabled = GeneralPreferences.imcMulticastEnable;
        broadcastEnabled = GeneralPreferences.imcBroadcastEnable;

        if (!(multicastEnabled || broadcastEnabled))
            return;
        multicastAddress = GeneralPreferences.imcMulticastAddress;
        int localport = 6969;
        multicastPorts = CommUtil.parsePortRangeFromString(GeneralPreferences.imcMulticastBroadcastPortRange, new int[] { 6969 });

        if (multicastUdpTransport == null) {
            multicastUdpTransport = new ImcUdpTransport((multicastPorts.length == 0) ? localport : multicastPorts[0],
                    multicastAddress, imcDefinition);
        }
        else {
            multicastUdpTransport.setBindPort((multicastPorts.length == 0) ? localport : multicastPorts[0]);
            multicastUdpTransport.setMulticastAddress(multicastAddress);
        }
        multicastUdpTransport.setBroadcastEnable(broadcastEnabled);
        multicastUdpTransport.reStart();
        if (multicastUdpTransport.isOnBindError()) {
            for (int i = 1; i < multicastPorts.length; i++) {
                multicastUdpTransport.stop();
                multicastUdpTransport.setBindPort(multicastPorts[i]);
                multicastUdpTransport.reStart();
                if (!multicastUdpTransport.isOnBindError())
                    break;
            }
        }

        if (!multicastUdpTransport.isOnBindError()) {
            if (announceWorker == null) {
                announceWorker = new AnnounceWorker(this, imcDefinition);
            }
        }
    }

    public boolean is2IdErrorMode() {
        return sameIdErrorDetected;
    }

    private void processEntityInfo(MessageInfo info, EntityInfo msg) {
        imcDefinition.getResolver().setEntityName(msg.getSrc(), msg.getSrcEnt(), msg.getLabel());        
    }
    
    private void processMessagePart(MessageInfo info, MessagePart msg) {
        IMCMessage m = fragmentHandler.setFragment((MessagePart)msg);
        if (m != null)
            postInternalMessage(msg.getSourceName(), m);
    }
    
    private void processEntityList(ImcId16 id, MessageInfo info, EntityList msg) {
        EntitiesResolver.setEntities(id.toString(), msg);
        imcDefinition.getResolver().setEntityMap(msg.getSrc(), msg.getList());
        ImcSystem sys = ImcSystemsHolder.lookupSystem(id);
        if (sys != null) {
            EntitiesResolver.setEntities(sys.getName(), msg);
        }
    }
    
    private void processReportedState(MessageInfo info, ReportedState msg) {
        // Process pos. state reported from other system
        String sysId = msg.getSid();
        
        double latRad = msg.getLat();
        double lonRad = msg.getLon();
        double depth = msg.getDepth();
        
        double rollRad = msg.getRoll();
        double pitchRad = msg.getPitch();
        double yawRad = msg.getYaw();
        
        double recTimeSecs = msg.getRcpTime();
        long recTimeMillis = Double.isFinite(recTimeSecs) ? (long) (recTimeSecs * 1E3) : msg.getTimestampMillis();
        
        // msg.getSType(); // Not used
        
        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(sysId);
        ExternalSystem extSys = null;
        if (imcSys == null) {
            extSys = ExternalSystemsHolder.lookupSystem(sysId);
            if (extSys == null) {
                extSys = new ExternalSystem(sysId);
                ExternalSystemsHolder.registerSystem(extSys);
            }
        }
        
        if (Double.isFinite(latRad) && Double.isFinite(lonRad)) {
            LocationType loc = new LocationType(Math.toDegrees(latRad), Math.toDegrees(lonRad));
            if (Double.isFinite(depth))
                loc.setDepth(depth);
            
            if (imcSys != null)
                imcSys.setLocation(loc, recTimeMillis);
            else
                extSys.setLocation(loc, recTimeMillis);
        }
        
        if (Double.isFinite(rollRad) || Double.isFinite(pitchRad) || Double.isFinite(yawRad)) {
            double rollDeg = Double.isFinite(rollRad) ? Math.toDegrees(rollRad) : 0;
            double pitchDeg = Double.isFinite(pitchRad) ? Math.toDegrees(pitchRad) : 0;
            double yawDeg = Double.isFinite(yawRad) ? Math.toDegrees(yawRad) : 0;
            
            if (imcSys != null)
                imcSys.setAttitudeDegrees(rollDeg, pitchDeg, yawDeg, recTimeMillis);
            else
                extSys.setAttitudeDegrees(rollDeg, pitchDeg, yawDeg, recTimeMillis);
        }
    }

    private void processStateReport(MessageInfo info, StateReport msg, ArrayList<IMCMessage> messagesCreatedToFoward) {
        
        String sysId = msg.getSourceName();
        
        long dataTimeMillis = msg.getStime() * 1000;
        
        double lat = msg.getLatitude();
        double lon = msg.getLongitude();
        double depth = msg.getDepth() == 0xFFFF ? -1 : msg.getDepth() / 10.0;
        // double altitude = msg.getAltitude() == 0xFFFF ? -1 : msg.getAltitude() / 10.0;
        double heading = ((double)msg.getHeading() / 65535.0) * 360;
        double speedMS = msg.getSpeed() / 100.;
        NeptusLog.pub().info("Received report from "+msg.getSourceName());
        
        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(sysId);
        if (imcSys == null) {
            NeptusLog.pub().error("Could not find system with id "+sysId);
            return;
        }
        
        LocationType loc = new LocationType(lat, lon);
        loc.setDepth(depth);
        imcSys.setLocation(loc, dataTimeMillis);
        imcSys.setAttitudeDegrees(heading, dataTimeMillis);
        
        imcSys.storeData(SystemUtils.GROUND_SPEED_KEY, speedMS, dataTimeMillis, true);
        imcSys.storeData(SystemUtils.COURSE_DEGS_KEY,
                (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(heading, 0)),
                dataTimeMillis, true);
        imcSys.storeData(
                SystemUtils.HEADING_DEGS_KEY,
                (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(heading, 0)),
                dataTimeMillis, true);
        
        int fuelPerc = msg.getFuel();
        if (fuelPerc > 0) {
            FuelLevel fuelLevelMsg = new FuelLevel();
            IMCUtils.copyHeader(msg, fuelLevelMsg);
            fuelLevelMsg.setTimestampMillis(dataTimeMillis);
            fuelLevelMsg.setValue(fuelPerc);
            fuelLevelMsg.setConfidence(0);
            imcSys.storeData(SystemUtils.FUEL_LEVEL_KEY, fuelLevelMsg, dataTimeMillis, true);
            
            messagesCreatedToFoward.add(fuelLevelMsg);
        }
        
        int execState = msg.getExecState();
        PlanControlState pcsMsg = new PlanControlState();
        IMCUtils.copyHeader(msg, pcsMsg);
        pcsMsg.setTimestampMillis(dataTimeMillis);
        switch (execState) {
            case -1:
                pcsMsg.setState(STATE.READY);
                break;
            case -3:
                pcsMsg.setState(STATE.INITIALIZING);
                break;
            case -2:
            case -4:
                pcsMsg.setState(STATE.BLOCKED);
                break;
            default:
                if (execState > 0)
                    pcsMsg.setState(STATE.EXECUTING);
                else
                    pcsMsg.setState(STATE.BLOCKED);
                break;
        }

        pcsMsg.setPlanEta(-1);
        pcsMsg.setPlanProgress(execState >= 0 ? execState : -1);
        pcsMsg.setManId("");
        pcsMsg.setManEta(-1);
        pcsMsg.setManType(0xFFFF);
        
        messagesCreatedToFoward.add(pcsMsg);
    }

    private void processAssetReport(MessageInfo info, AssetReport msg, ArrayList<IMCMessage> messagesCreatedToFoward) {

        String reporterId = msg.getSourceName();
        String sysId = msg.getName();

        long dataTimeMillis = Double.valueOf(msg.getReportTime() * 1000).longValue();

        AssetReport.MEDIUM mediumReported = msg.getMedium();

        double latRad = msg.getLat();
        double lonRad = msg.getLon();
        double depth = msg.getDepth();
        double altitude = msg.getAlt();

        double speedMS = msg.getSog();
        double cogRads = msg.getCog();

        ArrayList<IMCMessage> otherMsgs = Collections.list(msg.getMsgs().elements()); // TODO

        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(sysId);
        ExternalSystem extSys = null;
        if (imcSys == null) {
            extSys = ExternalSystemsHolder.lookupSystem(sysId);
            if (extSys == null) {
                extSys = new ExternalSystem(sysId);
                ExternalSystemsHolder.registerSystem(extSys);
            }
        }

        if (Double.isFinite(latRad) && Double.isFinite(lonRad)) {
            LocationType loc = new LocationType(AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(latRad)),
                    AngleUtils.nomalizeAngleDegrees180(Math.toDegrees(lonRad)));
            if (Double.isFinite(depth)) {
                loc.setDepth(depth);
            }
            imcSys.setLocation(loc, dataTimeMillis);
        }
        double headingRads = Double.NaN;
        if (Double.isFinite(cogRads) && Double.isFinite(speedMS) && Math.abs(speedMS) > 0.2) {
            headingRads = AngleUtils.nomalizeAngleRads2Pi(cogRads * (speedMS < 0 ? -1 : 1));
            imcSys.setAttitudeDegrees(headingRads, dataTimeMillis);
            imcSys.storeData(
                    SystemUtils.HEADING_DEGS_KEY,
                    (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(headingRads, 0)),
                    dataTimeMillis, true);
        }

        imcSys.storeData(SystemUtils.GROUND_SPEED_KEY, speedMS, dataTimeMillis, true);
        imcSys.storeData(SystemUtils.COURSE_DEGS_KEY,
                (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(cogRads), 0)),
                dataTimeMillis, true);
    }

    private void processRemoteSensorInfo(MessageInfo info, RemoteSensorInfo msg) {
        // Process pos. state reported from other system
        String sysId = msg.getId();
        
        double latRad = msg.getLat();
        double lonRad = msg.getLon();
        double altitude = msg.getAlt();

        double headingRad = msg.getHeading();
        
        String sensorClass = msg.getSensorClass();

        long recTimeMillis = msg.getTimestampMillis();

        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(sysId);
        ExternalSystem extSys = null;
        if (imcSys == null) {
            extSys = ExternalSystemsHolder.lookupSystem(sysId);
            if (extSys == null) {
                extSys = new ExternalSystem(sysId);
                ExternalSystemsHolder.registerSystem(extSys);
            }
        }

        if (Double.isFinite(latRad) && Double.isFinite(lonRad)) {
            LocationType loc = new LocationType(Math.toDegrees(latRad), Math.toDegrees(lonRad));
            if (Double.isFinite(altitude))
                loc.setDepth(-altitude);
            
            if (imcSys != null)
                imcSys.setLocation(loc, recTimeMillis);
            else
                extSys.setLocation(loc, recTimeMillis);
        }
        
        if (Double.isFinite(headingRad)) {
            double headingDeg = Math.toDegrees(headingRad);
            
            if (imcSys != null)
                imcSys.setAttitudeDegrees(headingDeg, recTimeMillis);
            else
                extSys.setAttitudeDegrees(headingDeg, recTimeMillis);
        }

        // Process sensor class
        SystemTypeEnum type = SystemUtils.getSystemTypeFrom(sensorClass);
        VehicleTypeEnum typeVehicle = SystemUtils.getVehicleTypeFrom(sensorClass);
        ExternalTypeEnum typeExternal = SystemUtils.getExternalTypeFrom(sensorClass);
        if (imcSys != null) {
            imcSys.setType(type);
            imcSys.setTypeVehicle(typeVehicle);
        }
        else {
            extSys.setType(type);
            extSys.setTypeVehicle(typeVehicle);
            extSys.setTypeExternal(typeExternal);
        }
    }

    @Override
    protected boolean processMsgLocally(MessageInfo info, IMCMessage msg) {
        LocalTime timeStart = LocalTime.now();

        // msg.dump(System.out);
        SystemImcMsgCommInfo vci = null;
        imcState.setMessage(msg);
        try {
            ImcId16 id = new ImcId16(msg.getHeader().getValue("src"));
            // Lets clear the 2 IDs error
            if (sameIdErrorDetectedTimeMillis < -1 || (System.currentTimeMillis() - sameIdErrorDetectedTimeMillis > 20000))
                sameIdErrorDetected = false;
            // Let's see if another node is advertising the same ID 
            if (!ImcId16.NULL_ID.equals(localId) && localId.equals(id)) {
                // System.out.println(localId + " :: " + id + " :: " + localId.equals(id) + " :: " + (Announce.ID_STATIC == msg.getMgid()));
                if (Announce.ID_STATIC == msg.getMgid()) {
                    String localUid = announceWorker.getNeptusInstanceUniqueID();
                    String serv = announceWorker.getImcServicesFromMessage(msg);
                    imcDefinition.getResolver().addEntry(msg.getSrc(), msg.getString("sys_name"));
                    String uid = IMCUtils.getUidFromServices(serv);
                    boolean sameHost = false;
                    Vector<NInterface> iList = getNetworkInterfaces();
                    for (NInterface nInterface : iList) {
                        Inet4Address[] lad = nInterface.getAddress();
                        for (Inet4Address inet4Address : lad) {
                            if (info.getPublisherInetAddress().equalsIgnoreCase(inet4Address.getHostAddress())) {
                                sameHost = true;
                                break;
                            }
                        }
                        if (sameHost)
                            break;
                    }
                    if (!localUid.equalsIgnoreCase(uid)) {
                        NeptusLog.pub().warn(
                                "Another node on " + (sameHost ? "this computer" : "our network")
                                + " is advertising our node id '" + localId.toPrettyString() + "'");
                        sameIdErrorDetected = true;
                        sameIdErrorDetectedTimeMillis = System.currentTimeMillis();
                    }
                }
                postToBus(msg);
                return true;
            }

            vci = getCommInfoById(id);
            if (!ImcId16.NULL_ID.equals(id) && !ImcId16.BROADCAST_ID.equals(id) && !ImcId16.ANNOUNCE.equals(id)
                    && !localId.equals(id)) {
                
                ArrayList<IMCMessage> messagesCreatedToFoward = new ArrayList<>();
                
                switch (msg.getMgid()) {
                    case Announce.ID_STATIC:
                        announceLastArriveTime = System.currentTimeMillis();
                        vci = processAnnounceMessage(info, (Announce) msg, vci, id);
                        break;
                    case EntityList.ID_STATIC:
                        processEntityList(id, info, (EntityList) msg);
                        break;
                    case EntityInfo.ID_STATIC:
                        processEntityInfo(info, (EntityInfo) msg);
                        break;
                    case MessagePart.ID_STATIC:
                        processMessagePart(info, (MessagePart) msg);
                        break;
                    case ReportedState.ID_STATIC:
                        processReportedState(info, (ReportedState) msg);
                        break;
                    case RemoteSensorInfo.ID_STATIC:
                        processRemoteSensorInfo(info, (RemoteSensorInfo) msg);
                        break;
                    case StateReport.ID_STATIC:
                        processStateReport(info, new StateReport(msg), messagesCreatedToFoward);
                        break;
                    case AssetReport.ID_STATIC:
                        processAssetReport(info, new AssetReport(msg), messagesCreatedToFoward);
                        break;
                    default:
                        break;
                }
                
                if (vci == null) {
                    if (VehiclesHolder.getVehicleWithImc(id) != null) {
                        vci = initSystemCommInfo(id, "");
                    }
                }
                
                if (vci != null) {
                    NeptusLog.pub().trace(
                            this.getClass().getSimpleName() + ": Message redirected for system comm. "
                                    + vci.getSystemCommId() + ".");

                    for (IMCMessage imcMsg : messagesCreatedToFoward) {
                        vci.onMessage(info, imcMsg);
                    }

                    Duration deltaT = Duration.between(timeStart, LocalTime.now());
                    if (deltaT.getSeconds() > 1) {
                        NeptusLog.pub().warn("=====!!===== Too long processing F " + deltaT + " :: " + msg.getAbbrev() +
                                " @ " + new ImcId16(msg.getSrc()).toPrettyString());
                    }

                    vci.onMessage(info, msg);
                    
                    return true;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        ImcId16 id = new ImcId16(msg.getSrc());
        if (!ImcId16.isValidIdForSource(id)) {
            // Let us just log the message and return
            NeptusLog.pub().debug("Message \"" + msg.getAbbrev() + "\" received from \"" + info.getPublisher()
                    + "\" with improper src id \"" + id.toPrettyString() + "\" logged but droped!");
            logMessage(msg);
            return true;
        }
        
        // If we got here the system is not known
        
        String inetAddress = info.getPublisherInetAddress();
        String remotePortAddress = "" + info.getPublisherPort();

        NeptusLog.pub().info(
                this.getClass().getSimpleName() + ": No IMC "
                        + "0x" + Integer.toUnsignedString((msg.getSrc()), 16)
                        + " system found. trying to redirected " + "by IP/Port info. "
                        + (vci != null ? vci.getSystemCommId() + ">" : "") + inetAddress + ":" + remotePortAddress
                        + ".");

        boolean sentToBus = false;

        // Lets us create a new system 
        try {
            vci = initSystemCommInfo(id, inetAddress + ":" + remotePortAddress);
            vci.onMessage(info, msg);
            sentToBus = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (udpOnIpMapper.containsKey(inetAddress + (isFilterByPort ? ":" + remotePortAddress : ""))) {
            ImcId16 systemId = udpOnIpMapper.get(inetAddress + (isFilterByPort ? ":" + remotePortAddress : ""));
            SystemImcMsgCommInfo vciMapper = getCommInfoById(systemId);
            if (vciMapper != null && vci != vciMapper) {
                NeptusLog.pub().debug(
                        this.getClass().getSimpleName() + ": Message redirected for system comm. "
                                + vciMapper.getSystemCommId() + ".");
                vciMapper.onMessage(info, msg);
                sentToBus = true;
            }
        }
        else if (isRedirectToFirst && !getCommInfo().keySet().isEmpty()) {
            ImcId16 vehicleId = getCommInfo().keySet().iterator().next();
            SystemImcMsgCommInfo vciRedirect = getCommInfoById(vehicleId);
            NeptusLog.pub().debug(
                    this.getClass().getSimpleName() + ": Message redirected for system comm. "
                            + vciRedirect.getSystemCommId() + ".");
            vciRedirect.onMessage(info, msg);
            sentToBus = true;
        }
        if (!sentToBus) {
            postToBus(msg);
        }

        return true;
    }

    /**
     * @param msg
     */
    private void postToBus(IMCMessage msg) {
        try {
            bus.post(msg);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        catch (Error e) {
            e.printStackTrace();
        }
    }

    /**
     * @param info
     * @param msg
     * @param vci
     * @param id
     * @return
     * @throws IOException
     */
    private SystemImcMsgCommInfo processAnnounceMessage(MessageInfo info, Announce ann, SystemImcMsgCommInfo vci,
            ImcId16 id) throws IOException {

        LocalTime timeStart = LocalTime.now();

        String sia = info.getPublisherInetAddress();
        NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: publisher host address " + sia);
        
        boolean hostWasGuessed = true;
        
        InetSocketAddress[] retId = announceWorker.getImcIpsPortsFromMessageImcUdp(ann);
        int portUdp = 0;
        String hostUdp = "";
        boolean udpIpPortFound = false;
        if (retId.length > 0) {
            portUdp = retId[0].getPort();
            hostUdp = retId[0].getAddress().getHostAddress();
        }
        for (InetSocketAddress add : retId) {
            if (sia.equalsIgnoreCase(add.getAddress().getHostAddress())) {
                if (ReachableCache.firstReachable(GeneralPreferences.imcReachabilityTestTimeout, add) != null) {
                    udpIpPortFound = true;
                    portUdp = add.getPort();
                    hostUdp = add.getAddress().getHostAddress();
                    hostWasGuessed = false;
                    NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "UDP reachable @ " + hostUdp + ":" + portUdp);
                    break;
                }
            }
        }

        // Let us try know any one in the announce IPs
        if (portUdp > 0 && !udpIpPortFound) {
            InetSocketAddress reachableAddr = ReachableCache.firstReachable(GeneralPreferences.imcReachabilityTestTimeout, retId);
            if (reachableAddr != null) {
                udpIpPortFound = true;
                portUdp = reachableAddr.getPort();
                hostUdp = reachableAddr.getAddress().getHostAddress();
                hostWasGuessed = false;
                NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "UDP reachable @ " + hostUdp + ":" + portUdp);
            }
        }

        if (portUdp > 0 && !udpIpPortFound) {
            // Lets try to see if we received a message from any of the IPs
            String ipReceived = hostUdp.isEmpty() ? info.getPublisherInetAddress() : hostUdp;
            hostWasGuessed = hostUdp.isEmpty() ? hostWasGuessed : true;
            hostUdp = ipReceived;
            udpIpPortFound = true;
            NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "no UDP reachable using " + hostUdp + ":" + portUdp);
        }

        InetSocketAddress[] retIdT = announceWorker.getImcIpsPortsFromMessageImcTcp(ann);
        int portTcp = 0;
        boolean tcpIpPortFound = false;
        if (retIdT.length > 0) {
            portTcp = retIdT[0].getPort();
            if ("".equalsIgnoreCase(hostUdp))
                hostUdp = retIdT[0].getAddress().getHostAddress();
        }
        for (InetSocketAddress add : retIdT) {
            if (sia.equalsIgnoreCase(add.getAddress().getHostAddress())) {
                if ("".equalsIgnoreCase(hostUdp)) {
                    if (ReachableCache.firstReachable(GeneralPreferences.imcReachabilityTestTimeout, add) != null) {
                        tcpIpPortFound = true;
                        hostUdp = add.getAddress().getHostAddress();
                        hostWasGuessed = false;
                        portTcp = add.getPort();
                        NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "TCP reachable @ " + hostUdp + ":" + portTcp);
                        break;
                    }
                    else
                        continue;
                }
                portTcp = add.getPort();
                tcpIpPortFound = true;
                NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "no TCP reachable using " + hostUdp + ":" + portTcp);
                break;
            }
        }

        // Let us try know any one in the announce IPs
        if (portTcp > 0 && !tcpIpPortFound) {
            InetSocketAddress reachableAddr = ReachableCache.firstReachable(GeneralPreferences.imcReachabilityTestTimeout, retId);
            if (reachableAddr != null) {
                if ("".equalsIgnoreCase(hostUdp)) {
                    tcpIpPortFound = true;
                    hostUdp = reachableAddr.getAddress().getHostAddress();
                    hostWasGuessed = false;
                    portTcp = reachableAddr.getPort();
                    NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "TCP reachable @ " + hostUdp + ":" + portTcp);
                }
                portTcp = reachableAddr.getPort();
                tcpIpPortFound = true;
                NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "no TCP reachable using " + hostUdp + ":" + portTcp);
            }
        }

        NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "using UDP@" + hostUdp
                        + ":" + portUdp + " and using TCP@" + hostUdp + ":" + portTcp + "  with host "
                        + (hostWasGuessed ? "guessed" : "found"));

        boolean requestEntityList = false;
        if (vci == null) {
            // Create a new system
            vci = initSystemCommInfo(id, info.getPublisherInetAddress() + ":"
                    + (portUdp == 0 ? DEFAULT_UDP_VEH_PORT : portUdp));
            updateUdpOnIpMapper(vci);
            requestEntityList = true;
        }
        // announceWorker.processAnnouceMessage(msg);
        String name = ann.getSysName();
        String type = ann.getSysType().toString();
        vci.setSystemIdName(name);
        ImcSystem resSys = ImcSystemsHolder.lookupSystem(id);
        // NeptusLog.pub().info("<###>......................Announce..." + name + " | " + type + " :: " + hostUdp + "  " +
        // portUdp);
        // NeptusLog.pub().warn(ReflectionUtil.getCallerStamp()+ " ..........................| " + name + " | " + type);
        if (resSys != null) {
            resSys.setServicesProvided(announceWorker.getImcServicesFromMessage(ann));
            AnnounceWorker.processUidFromServices(resSys);

            // new 2012-06-23
            if (resSys.isOnIdErrorState()) {
                EntitiesResolver.clearAliases(resSys.getName());
                EntitiesResolver.clearAliases(resSys.getId());
            }

            resSys.setName(name);
            resSys.setType(ImcSystem.translateSystemTypeFromMessage(type));
            resSys.setTypeVehicle(ImcSystem.translateVehicleTypeFromMessage(type));
            // NeptusLog.pub().info(ReflectionUtil.getCallerStamp()+ " ------------------------| " + resSys.getName() +
            // " | " + resSys.getType());
            if (portUdp != 0 && udpIpPortFound) {
                resSys.setRemoteUDPPort(portUdp);
            }
            else {
                if (resSys.getRemoteUDPPort() == 0)
                    resSys.setRemoteUDPPort(DEFAULT_UDP_VEH_PORT);
            }
            if (!"".equalsIgnoreCase(hostUdp) && !AnnounceWorker.NONE_IP.equalsIgnoreCase(hostUdp)) {
//                hostWasGuessed = true;
                if (AnnounceWorker.USE_REMOTE_IP.equalsIgnoreCase(hostUdp)) {
                    if (dontIgnoreIpSourceRequest)
                        resSys.setHostAddress(info.getPublisherInetAddress());
                }
                else {
                    if ((udpIpPortFound || tcpIpPortFound) && !hostWasGuessed) {
                        resSys.setHostAddress(hostUdp);
                    }
                    else if (hostWasGuessed) {
                        boolean alreadyFound = false;
                        try {
                            Map<InetSocketAddress, Integer> fAddr = new LinkedHashMap<>();
                            InetAddress publisherIAddr = InetAddress.getByName(sia);
                            byte[] pba = publisherIAddr.getAddress();
                            int i = 0;
                            for (InetSocketAddress inetSAddr : retId) {
                                byte[] lta = inetSAddr.getAddress().getAddress();
                                if (lta.length != pba.length)
                                    continue;
                                i = 0;
                                for (; i < lta.length; i++) {
                                    if (pba[i] != lta[i])
                                        break;
                                }
                                if (i > 0 && i <= pba.length)
                                    fAddr.put(inetSAddr, i);
                            }
                            for (InetSocketAddress inetSAddr : retIdT) {
                                if (fAddr.containsKey(inetSAddr))
                                    continue;
                                byte[] lta = inetSAddr.getAddress().getAddress();
                                if (lta.length != pba.length)
                                    continue;
                                i = 0;
                                for (; i < lta.length; i++) {
                                    if (pba[i] != lta[i])
                                        break;
                                }
                                if (i > 0 && i <= pba.length)
                                    fAddr.put(inetSAddr, i);
                            }
                            
                            InetSocketAddress foundCandidateAddr = fAddr.keySet().stream().max((a1, a2) -> {
                                    return fAddr.get(a1) - fAddr.get(a2);
                                }).orElse(null);
                            if (foundCandidateAddr != null) {
                                resSys.setHostAddress(foundCandidateAddr.getAddress().getHostAddress());
                                alreadyFound = true;
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        if (!alreadyFound) {
                            String curHostAddr = resSys.getHostAddress();
                            boolean currIsInAnnounce = false;
                            for (InetSocketAddress inetSAddr : retId) {
                                if (curHostAddr.equalsIgnoreCase(inetSAddr.getAddress().getHostAddress())) {
                                    currIsInAnnounce = true;
                                    break;
                                }
                            }
                            if (!currIsInAnnounce) {
                                for (InetSocketAddress inetSAddr : retIdT) {
                                    if (curHostAddr.equalsIgnoreCase(inetSAddr.getAddress().getHostAddress())) {
                                        currIsInAnnounce = true;
                                        break;
                                    }
                                }
                            }
                            
                            if (!currIsInAnnounce)
                                resSys.setHostAddress(hostUdp);
                        }
                    }
                }
            }
            if (portTcp != 0 && tcpIpPortFound) {
                resSys.setTCPOn(true);
                resSys.setRemoteTCPPort(portTcp);
            }
            else if (portTcp == 0) {
                resSys.setTCPOn(false);
            }

            if (resSys.isTCPOn() && retId.length == 0) {
                resSys.setUDPOn(false);
            }
            else {
                resSys.setUDPOn(true);
            }

            NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "final setup UDP@" + resSys.getHostAddress()
                    + ":" + resSys.getRemoteUDPPort() + " and using TCP@" + resSys.getHostAddress() + ":" + resSys.getRemoteTCPPort());

            resSys.setOnAnnounceState(true);

            try {
                double latRad = ann.getLat();
                double lonRad = ann.getLon();
                double height = ann.getHeight();
                if (latRad != 0 && lonRad != 0) {
                    LocationType loc = new LocationType();
                    loc.setLatitudeDegs(Math.toDegrees(latRad));
                    loc.setLongitudeDegs(Math.toDegrees(lonRad));
                    loc.setHeight(height);
                    long locTime = (long) (info.getTimeSentSec() * 1000);
                    resSys.setLocation(loc, locTime);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            // Adding temp getting heading from services
            double headingDegreesFromServices = AnnounceWorker.processHeadingDegreesFromServices(resSys);
            if (!Double.isNaN(headingDegreesFromServices) && !Double.isInfinite(headingDegreesFromServices)) {
                long attTime = (long) (info.getTimeSentSec() * 1000);
                resSys.setAttitudeDegrees(headingDegreesFromServices, attTime);
            }
            
            Map<Integer, String> er = EntitiesResolver.getEntities(resSys.getName());
            if (er == null || er.size() == 0)
                requestEntityList = true;

            if (requestEntityList)
                announceWorker.sendEntityListRequestMsg(resSys);
            
            ImcSystemsHolder.registerSystem(resSys);
        }

        Duration deltaT = Duration.between(timeStart, LocalTime.now());
        if (deltaT.getSeconds() > 1) {
            NeptusLog.pub().warn("=====!!===== Too long processing announce DF " + deltaT + " :: " + ann.getAbbrev() +
                    " @ " + new ImcId16(ann.getSrc()).toPrettyString() + "\n=====!!===== Try reducing " +
                    "'General Preference->[IMC Communications]-> Reachability Test Timeout' from " +
                    GeneralPreferences.imcReachabilityTestTimeout +
                    " to in the order of tens or 1 or 2 hundreds of ms.");
        }

        imcDefinition.getResolver().addEntry(ann.getSrc(), ann.getSysName());
        return vci;
    }

    /**
     * @return the sentMessagesFreqCalc
     */
    public MessageFrequencyCalculator getSentMessagesFreqCalc() {
        return sentMessagesFreqCalc;
    }

    public MessageFrequencyCalculator getSentMessagesFreqCalc(ImcId16 id) {
        SystemImcMsgCommInfo commI = getCommInfoById(id);
        return commI == null ? null : commI.getSentMessagesFreqCalc();
    }

    /**
     * @return the toSentMessagesFreqCalc
     */
    public MessageFrequencyCalculator getToSendMessagesFreqCalc() {
        return toSendMessagesFreqCalc;
    }

    public MessageFrequencyCalculator getToSendMessagesFreqCalc(ImcId16 id) {
        SystemImcMsgCommInfo commI = getCommInfoById(id);
        return commI == null ? null : commI.getToSendMessagesFreqCalc();
    }

    @Override
    public boolean sendMessageToVehicle(IMCMessage message, VehicleType vehicle, String sendProperties) {
        return sendMessage(message, vehicle.getImcId(), sendProperties);
    }

    public boolean sendMessageToSystem(IMCMessage message, String systemName, MessageDeliveryListener listener) {
        return sendMessageToSystem(message, systemName, null, listener);
    }

    public boolean sendMessageToSystem(IMCMessage message, String systemName) {
        return sendMessageToSystem(message, systemName, null, null);
    }

    public boolean sendMessageToSystem(IMCMessage message, String systemName, String sendProperties, MessageDeliveryListener listener) {
        ImcSystem system = ImcSystemsHolder.lookupSystemByName(systemName);
        
        if (system != null)
            return sendMessage(message, system.id, sendProperties, listener);
        if (listener != null)
            listener.deliveryUnreacheable(message);
        return false;
    }

    public Future<SendResult> sendMessageReliably(IMCMessage message, String systemName) {
        final ResultWaiter waiter = new ResultWaiter(20000);
        FutureTask<SendResult> result = new FutureTask<SendResult>(waiter) {
            
            private long start = System.currentTimeMillis();
            
            @Override
            public SendResult get() throws InterruptedException, ExecutionException {
                try {
                    return waiter.call();
                }
                catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }
            
            @Override
            public SendResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                long end = start + unit.toMillis(timeout); 
                while (System.currentTimeMillis() < end) {
                    if (waiter.result == null)
                        Thread.sleep(100);
                    else {
                        try {
                            return waiter.call();
                        }
                        catch (Exception e) {
                            throw new ExecutionException(e);
                        }
                    }
                }
                throw new TimeoutException("Time out exceeded");
            }
        };
        
        sendMessageToSystem(message, systemName, "TCP", waiter);
        //Executors.newSingleThreadExecutor().execute(result);
        return result;
    }

    @Override
    public boolean sendMessageToVehicle(IMCMessage message, String vehicleID, String sendProperties) {
        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleID);
        if (vehicle != null)
            return sendMessage(message, vehicle.getImcId(), sendProperties);
        else
            return false;
    }

    private static final class OperationResult {
        boolean finished = false;
        boolean result = false;
    }

    public boolean sendReliablyBlocking(IMCMessage message, ImcId16 vehicleCommId,
            final MessageDeliveryListener listener) {
        final OperationResult ret2 = new OperationResult();
        boolean ret = sendMessage(message, vehicleCommId, "TCP", new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                if (listener != null)
                    listener.deliveryUnreacheable(message);
                ret2.result = false;
                ret2.finished = true;
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                if (listener != null)
                    listener.deliveryUncertain(message, msg);
                ret2.result = false;
                ret2.finished = true;
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                if (listener != null)
                    listener.deliveryTimeOut(message);
                ret2.result = false;
                ret2.finished = true;
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
                if (listener != null)
                    listener.deliverySuccess(message);
                ret2.result = true;
                ret2.finished = true;
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                if (listener != null)
                    listener.deliveryError(message, error);
                ret2.result = false;
                ret2.finished = true;
            }
        });
        while (!ret2.finished) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ret && ret2.result;
    }

    /**
     * @param message
     * @param vehicleCommId
     * @param listener
     */
    public boolean sendReliablyNonBlocking(IMCMessage message, ImcId16 vehicleCommId, MessageDeliveryListener listener) {
        return sendMessage(message, vehicleCommId, "TCP", listener);
    }

    public boolean broadcastToCCUs(IMCMessage message) {
        ImcSystem[] ccus = ImcSystemsHolder.lookupSystemCCUs();

        for (ImcSystem ccu : ccus)
            sendMessage(message, ccu.getId(), null);

        return ccus.length > 0;
    }

    /**
     * @param message
     * @return
     */
    public boolean sendMessage(IMCMessage message) {
        try {
            ImcId16 id = new ImcId16(message.getHeader().getValue("dst"));
            return sendMessage(message, id, null);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.util.comm.manager.CommBaseManager#sendMessage(pt.lsts.neptus.messages.IMessage,
     * java.lang.Object, java.lang.String)
     */
    @Override
    public boolean sendMessage(IMCMessage message, ImcId16 vehicleCommId, String sendProperties) {
        return sendMessage(message, vehicleCommId, sendProperties, null);
    }

    /**
     * @param message The message to send
     * @param systemCommId The id of the destination.
     * @param sendProperties The properties to take into consideration when sending the message. This will be a comma
     *            separated string values. Possible values: Multicast and/or Broadcast (if the message is sent by either
     *            or both, it exists), and alternatively UDP or TCP (only if the Multicast and/or Broadcast are not
     *            used).
     * @param listener If you want to be warn on the send status of the message. Use null if you don't care.
     * @return Return true if the message went to the transport to be delivered. If you need to know if the message left
     *         the transport use the listener.
     */
    public boolean sendMessage(IMCMessage message, ImcId16 systemCommId, String sendProperties,
            MessageDeliveryListener msgListener) {

        checkAndSetMessageSrcEntity(message);

        // If manager is stopped we must return error 
        if (!isRunning()) {
            if (msgListener != null)
                msgListener.deliveryError(message, "Manager stopped!");
            return false;
        }

        // Lets wrap the possible listener in our internal one for frequency count for to send and sent messages
        MessageDeliveryListener listener = wrapMessageDeliveryListenerForSentMessageCounter(systemCommId, msgListener,
                message);

        // If the destination to send to is null, just send back to the caller an error
        if (systemCommId == null) {
            System.err.println("systemCommId is null!");
            listener.deliveryError(message, new Exception("systemCommId is null!"));
            return false;
        }

        // if authority OFF don't send messages to it
        ImcSystem resSys = ImcSystemsHolder.lookupSystem(systemCommId);
        if (resSys != null) {
            if (resSys.getAuthorityState() == ImcSystem.IMCAuthorityState.OFF) {
                String msg = systemCommId + " is with authority " + resSys.getAuthorityState() + "!";
                // System.err.println(msg);
                listener.deliveryError(message, new Exception(msg));
                return false;
            }
        }

        // Lets set header values
        message.getHeader().setValue("src", localId.longValue());
        if (message.getHeader().getInteger("dst") == 0xFFFF)
            message.getHeader().setValue("dst", systemCommId.longValue());
        // if (message.getTimestamp() == 0) // For now all messages are timestamped here
        message.setTimestamp(System.currentTimeMillis() / 1000.0);

        //        bus.post(message);

        // Check if is requested to send by Multicast and/or Broadcast, if yes don't send by any other way
        if (sendProperties != null
                && (StringUtils.isTokenInList(sendProperties, "Multicast") || StringUtils.isTokenInList(sendProperties,
                        "Broadcast"))) {
            boolean sentResult = true;
            if (StringUtils.isTokenInList(sendProperties, "Multicast")) {
                try {
                    for (int port : multicastPorts) {
                        markMessageToSent(systemCommId);
                        sentResult = multicastUdpTransport.sendMessage(multicastAddress, port, message, listener);
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    sentResult = false;
                    if (listener != null)
                        listener.deliveryUncertain(message, new Exception("Multicast or broadcast used!"));
                }
            }
            if (StringUtils.isTokenInList(sendProperties, "Broadcast")) {
                try {
                    Vector<NInterface> vecList = getNetworkInterfaces(); // NetworkInterfacesUtil.getNetworkInterfaces();
                    for (int port : multicastPorts) {
                        // multicastUdpTransport.sendMessage("255.255.255.255", port, message);
                        for (NInterface nii : vecList) {
                            if (nii.supportsBroadcast()) {
                                for (Inet4Address i4a : nii.getBroadcastAddress()) {
                                    if (i4a != null) {
                                        markMessageToSent(systemCommId);
                                        sentResult = multicastUdpTransport.sendMessage(i4a.getHostAddress(), port,
                                                message, listener);
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(ImcMsgManager.class.getName() + ": " + e, e);
                    sentResult = false;
                    if (listener != null)
                        listener.deliveryUncertain(message, new Exception("Multicast or broadcast used!"));
                }
            }

            return sentResult;
        }


        // Indicates the transport to use, next we will adjust this value with respect with the available transports, both local and remote
        ArrayList<TransportPreference> transportChoiceToSend = new ArrayList<>(transportPreferenceToUse);

        // Let us see if it was indicated to send the message through a specific transport
        TransportPreference transportPreferenceRequested = TransportPreference.ANY;
        if (sendProperties != null && StringUtils.isTokenInList(sendProperties, "UDP"))
            transportPreferenceRequested = TransportPreference.UDP;
        else if (sendProperties != null && StringUtils.isTokenInList(sendProperties, "TCP"))
            transportPreferenceRequested = TransportPreference.TCP;

        ImcId16 sysId = systemCommId;
        @SuppressWarnings("unused")
        SystemImcMsgCommInfo sysComm = commInfo.get(sysId);
        // if not known try to initialize the comms
        if (VehiclesHolder.getVehicleWithImc(sysId) != null)
            initSystemCommInfo(sysId, "");

        // Let us check if the protocol we were asked to send the message to is active on the system
        ImcSystem imcSystem = ImcSystemsHolder.lookupSystem(systemCommId);
        if (!imcSystem.isUDPOn() && transportChoiceToSend.contains(TransportPreference.UDP))
            transportChoiceToSend.remove(TransportPreference.UDP);
        if (!imcSystem.isTCPOn() && transportChoiceToSend.contains(TransportPreference.TCP))
            transportChoiceToSend.remove(TransportPreference.TCP);

        if (transportPreferenceRequested != TransportPreference.ANY) {
            if (transportChoiceToSend.contains(transportPreferenceRequested)) {
                transportChoiceToSend.remove(transportPreferenceRequested);
                transportChoiceToSend.add(0, transportPreferenceRequested);
            }
        }

        boolean sentResult = true;

        // Let us send the message by the preferred transport or the default one on the system by the order UDP, TCP
        // this depends on the transports available locally and on the system
        try {
            if (transportChoiceToSend.isEmpty()) {
                throw new NoTransportAvailableException(I18n.textf("No transport available to send message %message to %system.", 
                        message.getAbbrev(), imcSystem.getName()));
            }

            markMessageToSent(systemCommId);
            for (TransportPreference transport : transportChoiceToSend) {
                if (transport == TransportPreference.UDP) {
                    boolean ret = getUdpTransport().sendMessage(imcSystem.getHostAddress(),
                            imcSystem.getRemoteUDPPort(), message.cloneMessage(), listener);
                    sentResult = sentResult && ret;
                    if (ret)
                        break;
                }
                else if (transport == TransportPreference.TCP) {
                    boolean ret = getTcpTransport().sendMessage(imcSystem.getHostAddress(),
                            imcSystem.getRemoteTCPPort(), message.cloneMessage(), listener);
                    sentResult = sentResult && ret;
                    if (ret)
                        break;
                } 
            }
        }
        catch (Exception e) {
            sentResult = false;
            
            boolean isNoTransportAvailable = false;
            if (e instanceof NoTransportAvailableException)
                isNoTransportAvailable = true;
            
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            if (!isNoTransportAvailable && message != null)
                sb.append("msg: ").append(message.getAbbrev());
            if (!isNoTransportAvailable && systemCommId != null)
                sb.append(sb.length() > 1 ? ", " : "").append("to: ").append(imcSystem.getName()).append("::").append(systemCommId);
            if (sendProperties != null && !sendProperties.isEmpty())
                sb.append(sb.length() > 1 ? ", " : "").append("prop: ").append(sendProperties);
            sb.append("]");
            String what = sb.toString();

            if (isNoTransportAvailable)
                NeptusLog.pub().error(this.getClass().getSimpleName() + ": Error sending message! " + what + " " + e.getMessage());
            else
                NeptusLog.pub().error(this.getClass().getSimpleName() + ": Error sending message! " + what, e);

            if (listener != null)
                listener.deliveryError(message, e);
        }

        return sentResult;
    }

    public static void disseminate(XmlOutputMethods object, String rootElementName) {
        IMCMessage msg = IMCDefinition.getInstance().create("mission_chunk", "xml_data", object.asXML(rootElementName));
        disseminateToCCUs(msg);
    }

    private static void disseminateToCCUs(IMCMessage msg) {
        if (msg == null)
            return;

        ImcSystem[] systems = ImcSystemsHolder.lookupSystemCCUs();
        for (ImcSystem s : systems) {
            NeptusLog.pub().info("sending msg '" + msg.getAbbrev() + "' to '" + s.getName() + "'...");
            ImcMsgManager.getManager().sendMessage(msg, s.getId(), null);
        }
    }
    
    public int getEntityId() {
        String caller = getCallerClass();

        if (caller != null) {
            if (!neptusEntities.containsKey(caller)) {
                short id = ++lastEntityId;
                neptusEntities.put(caller, id);
                
                EntityInfo info = new EntityInfo();
                info.setId(id);
                info.setComponent(caller);
                try {
                    caller = PluginUtils.getPluginName(Class.forName(caller));
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                }
                info.setLabel(caller);
                info.setSrcEnt(0);
                info.setSrc(getLocalId().intValue());

                LsfMessageLogger.log(info);
            }
            return neptusEntities.get(caller);
        }
        
        return 255;
    }
    
    public int registerEntity(String name) throws InvalidNameException {
        if (neptusEntities.containsKey(name))
            throw new InvalidNameException("There is already a registered entity named '"+name+"'.");

        neptusEntities.put(name, ++lastEntityId);
        EntityInfo info = new EntityInfo();
        info.setId(lastEntityId);
        info.setComponent(name);
        info.setLabel(name);
        info.setSrcEnt(0);
        info.setSrc(getLocalId().intValue());
        LsfMessageLogger.log(info);
        
        return neptusEntities.get(name);
    }

    /**
     * @param message
     */
    private void checkAndSetMessageSrcEntity(IMCMessage message) {
        if (message.getSrcEnt() == 0 || message.getSrcEnt() == IMCMessage.DEFAULT_ENTITY_ID) {
            String caller = getCallerClass();

            if (caller != null) {
                if (!neptusEntities.containsKey(caller)) {
                    neptusEntities.put(caller, ++lastEntityId);

                    EntityInfo info = new EntityInfo();
                    info.setId(lastEntityId);
                    info.setComponent(caller);
                    try {
                        caller = PluginUtils.getPluginName(Class.forName(caller));
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e.getMessage());
                        // nothing
                    }
                    info.setLabel(caller);
                    info.setSrcEnt(0);
                    info.setSrc(getLocalId().intValue());

                    // Lets log the EntityInfo message
                    // LsfMessageLogger.log(info);
                    LsfMessageLogger.log(info);
                }

                if (neptusEntities.get(caller) != null)
                    message.setSrcEnt(neptusEntities.get(caller));
            }
        }
    }

    protected String getCallerClass() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        String classname = null;
        for (int i = 0; i < trace.length; i++) {
            if (!ignoredClasses.contains(trace[i].getClassName())) {
                classname = trace[i].getClassName();
                if (classname.contains("$"))
                    classname = classname.substring(0, classname.indexOf("$"));
                break;
            }
        }

        return classname;
    }

    /**
     * To wrap the possible listener in our internal one for frequency count for to send and sent messages.
     * Also on success will log the message (at least success on one sent request).
     * @param systemCommId
     * @param listenerToWrap
     * @return
     */
    private MessageDeliveryListener wrapMessageDeliveryListenerForSentMessageCounter(final ImcId16 systemCommId,
            final MessageDeliveryListener listenerToWrap, final IMCMessage originalToSendMessage) {
        MessageDeliveryListener msgLstnr = new MessageDeliveryListener() {
            private AtomicBoolean messageAlreadyLogged = new AtomicBoolean(false);
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                if (listenerToWrap != null)
                    listenerToWrap.deliveryError(message, error);
            }

            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                if (listenerToWrap != null)
                    listenerToWrap.deliveryUnreacheable(message);
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                if (listenerToWrap != null)
                    listenerToWrap.deliveryTimeOut(message);
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                if (listenerToWrap != null)
                    listenerToWrap.deliveryUncertain(message, msg);

                tryLogMessage();
                markMessageSent(systemCommId);
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
                if (listenerToWrap != null)
                    listenerToWrap.deliverySuccess(message);

                tryLogMessage();
                markMessageSent(systemCommId);
            }

            private void tryLogMessage() {
                if (messageAlreadyLogged.compareAndSet(false, true))
                    logMessage(originalToSendMessage);
            }
        };

        return msgLstnr;
    }

    /**
     * @param systemCommId
     */
    private void markMessageSent(ImcId16 systemCommId) {
        sentMessagesFreqCalc.setTimeMillisLastMsg(System.currentTimeMillis());
        MessageFrequencyCalculator mfc = getSentMessagesFreqCalc(systemCommId);
        if (mfc != null)
            mfc.setTimeMillisLastMsg(System.currentTimeMillis());
    }

    /**
     * @param systemCommId
     */
    private void markMessageToSent(ImcId16 systemCommId) {
        toSendMessagesFreqCalc.setTimeMillisLastMsg(System.currentTimeMillis());
        MessageFrequencyCalculator mfc = getToSendMessagesFreqCalc(systemCommId);
        if (mfc != null)
            mfc.setTimeMillisLastMsg(System.currentTimeMillis());
    }

    /**
     * @param message
     */
    private void logMessage(IMCMessage message) {
        if (logSentMsg) {
            try {
                // Pass as pub/sub directly the int ID's present on the message
                // String pub = message.getHeaderValue("src").toString();
                // String sub = message.getHeaderValue("dst").toString();
                // LLFMessageLogger.logMessage(info, msg);
                // NeptusMessageLogger.logMessage(pub, sub, message.getTimestampMillis(), message);
                LsfMessageLogger.log(message);
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error logging message " + message.getMessageType().getShortName() + "!", e);
            }
        }
    }

    public boolean registerEntity() {
        String caller = getCallerClass();

        if (caller != null) {
            if (!neptusEntities.containsKey(caller)) {
                neptusEntities.put(caller, ++lastEntityId);
                return true;
            }
        }
        return false;
    }

    /**
     * @param listener
     * @param vehicleId
     * @return
     */
    public boolean addListener(MessageListener<MessageInfo, IMCMessage> listener, String vehicleId) {
        return addListener(listener, vehicleId, null);
    }

    /**
     * @param listener
     * @param systemId
     * @param filter
     * @return
     */
    public boolean addListener(MessageListener<MessageInfo, IMCMessage> listener, String systemId,
            MessageFilter<MessageInfo, IMCMessage> filter) {

        ImcId16 imcId;

        VehicleType veh = VehiclesHolder.getVehicleById(systemId);
        if (veh == null) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(systemId);
            if (sys == null)
                return false;
            else
                imcId = sys.getId();
        }
        else {
            imcId = veh.getImcId();
        }

        if (imcId == null)
            return false;
        return super.addListener(listener, imcId, filter);
    }

    /**
     * @param listener
     * @param systemId
     * @return
     */
    public boolean removeListener(MessageListener<MessageInfo, IMCMessage> listener, String systemId) {
        ImcId16 imcId;

        VehicleType veh = VehiclesHolder.getVehicleById(systemId);
        if (veh == null) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(systemId);
            if (sys == null)
                return false;
            else
                imcId = sys.getId();
        }
        else {
            imcId = veh.getImcId();
        }

        if (imcId == null)
            return false;

        return super.removeListener(listener, imcId);
    }

    public ImcSystemState getState(ImcId16 id) {
        if (super.getCommInfoById(id) != null)
            return super.getCommInfoById(id).getImcState();
        return new ImcSystemState(IMCDefinition.getInstance());
    }

    public ImcSystemState getState(VehicleType vehicle) {
        if (vehicle != null)
            return getState(vehicle.getImcId());
        else
            return new ImcSystemState(IMCDefinition.getInstance());
    }

    public ImcSystemState getState(String vehicle) {
        VehicleType vt = VehiclesHolder.getVehicleById(vehicle);
        if (vt != null)
            return getState(vt);
        else {
            int imc_id = imcDefinition.getResolver().resolve(vehicle);
            return getState(new ImcId16(imc_id));
        }
    }

    /**
     * @param vehicleId
     * @return
     */
    public SystemImcMsgCommInfo getCommInfoById(String vehicleId) {
        ImcId16 imcId;

        VehicleType veh = VehiclesHolder.getVehicleById(vehicleId);
        if (veh == null) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(vehicleId);
            if (sys == null)
                return null;
            else
                imcId = sys.getId();
        }
        else {
            imcId = veh.getImcId();
        }

        if (imcId == null)
            return null;
        return super.getCommInfoById(imcId);
    }

    /**
     * @return
     */
    public ImcId16 getLocalId() {
        return localId;
    }

    /**
     * @return
     */
    public String getCommStatusAsHtmlFragment() {
        String ret = "";
        ret += "<b>" + (isRunning() ? "On" : "Off") + "</b><br>";
        ret += "<b>UDP: </b>"
                + (udpTransport == null ? "Off" : (!udpTransport.isOnBindError() ? "OK" : "BIND ERROR") + " "
                        + (udpTransport.isRunnning() ? "On" : "Off") + " @" + udpTransport.getBindPort()) + "<br>";
        ret += "<b>Multicast UDP: </b>"
                + (multicastUdpTransport == null ? "Off" : (!multicastUdpTransport.isOnBindError() ? "OK"
                        : "BIND ERROR")
                        + " "
                        + (multicastUdpTransport.isRunnning() ? "On" : "Off")
                        + " @"
                        + multicastUdpTransport.getMulticastAddress() + ":" + multicastUdpTransport.getBindPort())
                        + "<br>";
        long nc = getTcpTransport() != null ? getTcpTransport().getActiveNumberOfConnections() : 0;
        ret += "<b>TCP: </b>"
                + (tcpTransport == null ? "Off" : (!tcpTransport.isOnBindError() ? "OK" : "BIND ERROR") + " "
                        + (tcpTransport.isRunning() ? (tcpTransport.isRunningNormally() ? "On" : "On:Error") : "Off")
                        + " @" + tcpTransport.getBindPort()
                        + (tcpTransport.isRunning() ? " (" + nc + " connection" + (nc == 1 ? "" : "s") + ")" : ""))
                        + "<br>";

        return ret;
    }

    /**
     * @return the bus
     */
    private final AsyncEventBus getMessageBus() {
        return bus;
    }

    public final ImcSystemState getImcState() {
        return imcState;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        ConfigFetch.initialize();
        VehiclesHolder.loadVehicles();
        getManager().start();

        GuiUtils.setLookAndFeel();
        JFrame frame = GuiUtils.testFrame(new MonitorIMCComms(getManager()), "Monitor IMC Comms");
        frame.setIconImage(MonitorIMCComms.ICON_ON.getImage());
        frame.setSize(396, 411 + 55);

        ImcSystem lsys = new ImcSystem(new ImcId16(0x4d15));
        lsys.setCommsInfo(ImcSystem.createCommMean("127.0.0.1", 6002, 6002, new ImcId16(0x4d15), true, true));
        ImcSystemsHolder.registerSystem(lsys);

        int portServer = 6002;
        ImcTcpTransport tcpT = new ImcTcpTransport(portServer, IMCDefinition.getInstance());
        tcpT.addListener(new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                info.dump(System.out);
                msg.dump(System.out);
                System.out.flush();
            }
        });

        for (int i = 0; true; i++) {
            System.err.println("HB " + i);
            boolean ret = getManager().sendReliablyBlocking(IMCDefinition.getInstance().create("Heartbeat"),
                    new ImcId16(0x4d15), new MessageDeliveryListener() {
                @Override
                public void deliveryUnreacheable(IMCMessage message) {
                    System.err.println("deliveryUnreacheable");
                }

                @Override
                public void deliveryUncertain(IMCMessage message, Object msg) {
                    System.err.println("deliveryUncertain  " + msg);
                }

                @Override
                public void deliveryTimeOut(IMCMessage message) {
                    System.err.println("deliveryTimeOut");
                }

                @Override
                public void deliverySuccess(IMCMessage message) {
                    System.err.println("deliverySuccess");
                }

                @Override
                public void deliveryError(IMCMessage message, Object error) {
                    System.err.println("deliveryError  " + error);
                }
            });
            System.err.println(ret);
            // getManager().sendMessage(imcDefinition.create("Abort"), new ImcId16(0x4d15), null);
            try {
                Thread.sleep(1500);
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    static class ResultWaiter implements Callable<SendResult>, MessageDeliveryListener {
        
        public SendResult result = null;
        private long timeoutMillis = 10000;
        private long start;
        
        public ResultWaiter(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            this.start = System.currentTimeMillis();
        }
        
        @Override
        public SendResult call() throws Exception {
            while (true) {
                synchronized (this) {
                    if (result != null) {
                        return result;
                    }
                    if (System.currentTimeMillis() - start > timeoutMillis) {                     
                        return SendResult.TIMEOUT;
                    }
                }
                Thread.sleep(100);
            }
        }
        
        @Override
        public void deliveryError(IMCMessage message, Object error) {
            result = SendResult.ERROR;
        }
        
        @Override
        public void deliverySuccess(IMCMessage message) {
            result = SendResult.SUCCESS;
        }

        @Override
        public void deliveryTimeOut(IMCMessage message) {
            result = SendResult.TIMEOUT;
        }

        @Override
        public void deliveryUncertain(IMCMessage message, Object msg) {
            result = SendResult.UNCERTAIN_DELIVERY;
                    
        }

        @Override
        public void deliveryUnreacheable(IMCMessage message) { 
            result = SendResult.UNREACHABLE;
        }        
    }
}
