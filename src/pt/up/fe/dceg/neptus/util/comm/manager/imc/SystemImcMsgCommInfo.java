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
 * 2007/05/25 pdias
 */
package pt.up.fe.dceg.neptus.util.comm.manager.imc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.AcousticSystems;
import pt.up.fe.dceg.neptus.imc.EmergencyControlState;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.FuelLevel;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IndicatedSpeed;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.imc.LblConfig.OP;
import pt.up.fe.dceg.neptus.imc.OperationalLimits;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.imc.PlanDB;
import pt.up.fe.dceg.neptus.imc.Rpm;
import pt.up.fe.dceg.neptus.imc.SimulatedState;
import pt.up.fe.dceg.neptus.imc.TrueSpeed;
import pt.up.fe.dceg.neptus.imc.VehicleState;
import pt.up.fe.dceg.neptus.imc.state.ImcSysState;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.types.comm.protocol.IMCArgs;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.AngleCalc;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.StringUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.MessageFrequencyCalculator;
import pt.up.fe.dceg.neptus.util.comm.manager.SystemCommBaseInfo;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;
import pt.up.fe.dceg.neptus.util.conf.PreferencesListener;
import pt.up.fe.dceg.neptus.util.llf.NeptusMessageLogger;

import com.google.common.eventbus.AsyncEventBus;

/**
 * @author pdias
 */
public class SystemImcMsgCommInfo extends SystemCommBaseInfo<IMCMessage, MessageInfo, ImcId16> {
    // private boolean udpOn = false;
    // private boolean rtpsOn = false;

    private String ipAddress = null;
    private int ipRemotePort = 0;

    private boolean logReceivedMsg = false;

    private boolean useActivityCounter = true;
    private Vector<Long> activityCounter = new Vector<Long>();

    protected ImcSysState imcState = new ImcSysState();

    private PreferencesListener gplistener = new PreferencesListener() {
        public void preferencesUpdated() {
            logReceivedMsg = GeneralPreferences.messageLogReceivedMessages;
            useActivityCounter = GeneralPreferences.commsUseNewSystemActivityCounter;
        }
    };

    // Transports
    // protected MiddlewareNode privateNode = null;
    // protected ImcUdpTransport udpTransport = null;

    // Frequency Calculators
    private MessageFrequencyCalculator toSendMessagesFreqCalc = new MessageFrequencyCalculator();
    private MessageFrequencyCalculator sentMessagesFreqCalc = new MessageFrequencyCalculator();
    
    protected AsyncEventBus bus = null;

    public SystemImcMsgCommInfo() {
        super();
        GeneralPreferences.addPreferencesListener(gplistener);
        gplistener.preferencesUpdated();
    }

    /**
     * @return the bus
     */
    public final AsyncEventBus getMessageBus() {
        return bus;
    }

    /**
     * @return the bus
     */
    final void setMessageBus(AsyncEventBus bus) {
        this.bus = bus;
    }


    
    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.util.comm.manager.CommonCommBaseImplementation#triggerExtraActionOnSetActive(boolean,
     * pt.up.fe.dceg.neptus.messages.listener.MessageInfo, pt.up.fe.dceg.neptus.messages.IMessage)
     */
    @Override
    protected void triggerExtraActionOnSetActive(boolean isActive, MessageInfo info, IMCMessage message) {
        ImcSystem sys = ImcSystemsHolder.lookupSystem(getSystemCommId());
        if (sys != null) {
            if (message == null || !StringUtils.isTokenInList("Announce,EntityList", message.getAbbrev())) {
                if (!useActivityCounter) {
                    sys.setActive(isActive);
                    // System.out.println(sys.getName()+": "+isActive()+"  "+(message !=
                    // null?message.getAbbrevName():""));
                }
                else {
                    // If IMCAuthorityState.OFF then we consider not active
                    if (sys.getAuthorityState() == ImcSystem.IMCAuthorityState.OFF) {
                        sys.setActive(false);
                    }
                    else if (!isActive) {
                        sys.setActive(false);
                    }
                    else {
                        activityCounter.add(System.currentTimeMillis());
                        int vecSize = activityCounter.size();
                        if (vecSize > 3) {
                            activityCounter.remove(0);
                        }
                        vecSize = activityCounter.size();
                        if (vecSize == 3) {
                            if (activityCounter.get(2) - activityCounter.get(0) <= 3000)
                                sys.setActive(true);
                            else
                                sys.setActive(false);
                        }
                        else {
                            sys.setActive(false);
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress the ipAddress to set
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * @return the ipRemotePort
     */
    public int getIpRemotePort() {
        return ipRemotePort;
    }

    /**
     * @param ipRemotePort the ipRemotePort to set
     */
    public void setIpRemotePort(int ipRemotePort) {
        this.ipRemotePort = ipRemotePort;
    }

    @Override
    protected boolean initSystemComms() {
        VehicleType vehicleaux = VehiclesHolder.getVehicleWithImc(getSystemCommId());

        ImcSystem resSys = ImcSystemsHolder.lookupSystem(getSystemCommId());
        if (resSys == null) {
            if (vehicleaux != null) {
                resSys = new ImcSystem(vehicleaux);
                ImcSystemsHolder.registerSystem(resSys);
            }
        }

        if (resSys == null) {
            NeptusLog.pub().error("Error creating private sending transports for " + getSystemCommId() + ".");
            return false;
        }

        // CommMean commMean = CommUtil.getActiveCommMeanForProtocol(
        // vehicleaux, "imc");

        CommMean commMean = resSys.commsInfo;
        if (commMean == null) {
            NeptusLog.pub().error("Error creating private sending transports for " + getSystemCommId() + ".");
            return false;
        }

        this.ipAddress = commMean.getHostAddress();
        this.ipRemotePort = ((IMCArgs) (commMean.getProtocolsArgs().get(CommMean.IMC))).getPort();

        return true;
    }

    @Override
    protected boolean startSystemComms() {
        return true;
    }

    @Override
    protected boolean stopSystemComms() {
        return true;
    }

    @Override
    protected boolean processMsgLocally(MessageInfo info, IMCMessage msg) {
        // msg.dump(System.out);
        // System.out.flush();

        ImcSystem resSys = ImcSystemsHolder.lookupSystem(systemCommId);

        if (resSys != null) {
            if (resSys.getAuthorityState() == ImcSystem.IMCAuthorityState.OFF)
                return false;
        }

        logMessage(info, msg);
        
        try {
            if (bus != null)
                bus.post(msg);
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
        catch (Error e1) {
            e1.printStackTrace();
        }
        
        imcState.setMessage(msg);

        if (resSys == null)
            return true;

        switch (msg.getMgid()) {
            case VehicleState.ID_STATIC:

                try {
                    int errorCount = msg.getInteger("error_count");
                    if (errorCount > 0)
                        resSys.setOnErrorState(true);
                    else
                        resSys.setOnErrorState(false);

                    Object errEntStr = msg.getValue("error_ents");
                    if (errEntStr != null)
                        resSys.setOnErrorStateStr(errEntStr.toString());
                    else
                        resSys.setOnErrorStateStr("");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case PlanControlState.ID_STATIC:
                try {
                    String planId = msg.getString("plan_id");
                    String maneuver = msg.getString("man_id");
                    String state = msg.getString("state");
                    PlanType plan = new PlanType(null);
                    plan.setId(planId + "|" + I18n.textc("Man", "Maneuver (short form)") + ":" + maneuver);
                    if ("EXECUTING".equalsIgnoreCase(state))
                        resSys.setActivePlan(plan);
                    else
                        resSys.setActivePlan(null);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case EmergencyControlState.ID_STATIC:
                try {
                    String planId;
                    try {
                        planId = msg.getString("plan_id");
                    }
                    catch (Exception e) {
                        planId = msg.getString("mission_id");
                    }
                    String state = msg.getString("state");
                    resSys.setEmergencyPlanId(planId);
                    resSys.setEmergencyStatusStr(state);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case EstimatedState.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    
                    double lat = msg.getDouble("lat");
                    double lon = msg.getDouble("lon");
                    double height = msg.getDouble("height");
                    msg.getDouble("depth");
                    msg.getDouble("altitude");
                    double x = msg.getDouble("x");
                    double y = msg.getDouble("y");
                    double z = msg.getDouble("z");
                    double phi = msg.getDouble("phi");
                    double theta = msg.getDouble("theta");
                    double psi = msg.getDouble("psi");

                    LocationType loc = new LocationType();
                    loc.setLatitudeRads(lat);
                    loc.setLongitudeRads(lon);
                    loc.setHeight(height);
                    loc.setOffsetNorth(x);
                    loc.setOffsetEast(y);
                    loc.setOffsetDown(z);
                    loc.convertToAbsoluteLatLonDepth();

                    if (loc != null) {
                        resSys.setLocation(loc, timeMillis);
                    }

                    resSys.setAttitudeDegrees(Math.toDegrees(phi), Math.toDegrees(theta), Math.toDegrees(psi),
                            timeMillis);

                    // double u = msg.getDouble("u");
                    // double v = msg.getDouble("v");
                    // double w = msg.getDouble("w");
                    double vx = msg.getDouble("vx");
                    double vy = msg.getDouble("vy");
                    double vz = msg.getDouble("vz");

                    double courseRad = AngleCalc.calcAngle(0, 0, vy, vx);
                    double groundSpeed = Math.sqrt(vx * vx + vy * vy);
                    double verticalSpeed = vz;

                    resSys.storeData(ImcSystem.COURSE_KEY,
                            (int) AngleCalc.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(courseRad), 0)),
                            timeMillis, true);
                    resSys.storeData(ImcSystem.GROUND_SPEED_KEY, groundSpeed, timeMillis, true);
                    resSys.storeData(ImcSystem.VERTICAL_SPEED_KEY, verticalSpeed, timeMillis, true);

                    double headingRad = msg.getDouble("psi");
                    resSys.storeData(
                            ImcSystem.HEADING_KEY,
                            (int) AngleCalc.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(headingRad), 0)),
                            timeMillis, true);

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case SimulatedState.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    resSys.storeData(msg.getAbbrev(), msg, timeMillis, true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case OperationalLimits.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    resSys.storeData(msg.getAbbrev(), msg, timeMillis, true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case IndicatedSpeed.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    double value = msg.getDouble("value");
                    resSys.storeData(ImcSystem.INDICATED_SPEED_KEY, value, timeMillis, true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case TrueSpeed.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    double value = msg.getDouble("value");
                    resSys.storeData(ImcSystem.TRUE_SPEED_KEY, value, timeMillis, true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case PlanDB.ID_STATIC:
                try {
                    resSys.getPlanDBControl().onMessage(info, msg);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case Rpm.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    int entityId = (Integer) msg.getHeaderValue("src_ent");
                    final int value = msg.getInteger("value");
                    if (entityId == 0xFF) {
                        resSys.storeData(ImcSystem.RPM_MAP_ENTITY_KEY, value, timeMillis, true);
                    }
                    else {
                        final String entityName = EntitiesResolver.resolveName(resSys.getName(), entityId);
                        if (entityName != null) {
                            Object obj = resSys.retrieveData(ImcSystem.RPM_MAP_ENTITY_KEY);
                            if (obj == null) {
                                Map<String, Integer> map = (Map<String, Integer>) Collections
                                        .synchronizedMap(new HashMap<String, Integer>());
                                map.put(entityName, value);
                                resSys.storeData(ImcSystem.RPM_MAP_ENTITY_KEY, map, timeMillis, true);
                            }
                            else {
                                @SuppressWarnings("unchecked")
                                Map<String, Integer> rpms = (Map<String, Integer>) resSys
                                        .retrieveData(ImcSystem.RPM_MAP_ENTITY_KEY);
                                rpms.put(entityName, value);
                                resSys.storeData(ImcSystem.RPM_MAP_ENTITY_KEY, rpms, timeMillis, false);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case FuelLevel.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    FuelLevel fuelLevelMsg = (FuelLevel) msg;
                    resSys.storeData(ImcSystem.FUEL_LEVEL_KEY, fuelLevelMsg, timeMillis, true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case LblConfig.ID_STATIC:
                try {
                    if (((LblConfig) msg).getOp() == OP.CUR_CFG)
                        resSys.storeData(ImcSystem.LBL_CONFIG_KEY, (LblConfig) msg, msg.getTimestampMillis(), true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case AcousticSystems.ID_STATIC:
                try {
                    long timeMillis = msg.getTimestampMillis();
                    AcousticSystems acousticSystemsMsg = (AcousticSystems) msg;
                    resSys.storeData(ImcSystem.ACOUSTIC_SYSTEMS, acousticSystemsMsg, timeMillis, true);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }

        return true;
    }

    /**
     * @param info
     * @param msg
     */
    private void logMessage(MessageInfo info, IMCMessage msg) {
        if (logReceivedMsg) {
            try {
                String noLogStr = info.getProperty(MessageInfo.NOT_TO_LOG_MSG_KEY);
                if ("true".equalsIgnoreCase(noLogStr)) {
                    NeptusLog.pub().info("Skip logging message " + msg.getMessageType().getShortName() + "!");
                }
                else {
                    String strUDP = "<UDP peer identified by IP>";

                    // Pass as pub/sub directly the int ID's present on the message
                    String pub = msg.getHeaderValue("src").toString();
                    msg.getHeaderValue("dst").toString();
                    if (strUDP.equals(pub))
                        pub = getSystemIdName().toUpperCase();
//                    NeptusMessageLogger.getLogger().logMessage(pub, sub, info.getTimeReceivedNanos() / 1000000, msg);
                    NeptusMessageLogger.logMessage(msg);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error logging message " + msg.getMessageType().getShortName() + "!", e);
            }
        }
    }

    /**
     * @return the sentMessagesFreqCalc
     */
    public MessageFrequencyCalculator getSentMessagesFreqCalc() {
        return sentMessagesFreqCalc;
    }

    /**
     * @return the toSentMessagesFreqCalc
     */
    public MessageFrequencyCalculator getToSendMessagesFreqCalc() {
        return toSendMessagesFreqCalc;
    }

    @Override
    public String toString() {
        return getSystemIdName();
    }

    public final ImcSysState getImcState() {
        return imcState;
    }
}