/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * 15/4/2024
 */
package pt.lsts.neptus.comm.manager.imc;

import pt.lsts.imc.AssetReport;
import pt.lsts.imc.EntityInfo;
import pt.lsts.imc.EntityList;
import pt.lsts.imc.EulerAngles;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.ReportedState;
import pt.lsts.imc.StateReport;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.MathMiscUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

class ImcMsgManagerMessageProcessor {
    private static String IRIDIUM_PUBLISHER_PREFIX = "iridium";
    private static String ACOUSTIC_PUBLISHER_PREFIX = "acoustic";
    private static String ACOUSTIC2_PUBLISHER_PREFIX = "acomms";
    private static String SMS_PUBLISHER_PREFIX = "sms";
    private static String GSM_PUBLISHER_PREFIX = "gsm";
    private static String MQTT_PUBLISHER_PREFIX = "mqtt";

    private final ImcMsgManager manager;
    private final IMCFragmentHandler fragmentHandler;

    public ImcMsgManagerMessageProcessor(ImcMsgManager manager) {
        this.manager = manager;
        fragmentHandler = new IMCFragmentHandler(manager.imcDefinition);
    }

    private ImcSystem.MEDIUM getMediumReported(String publisherName) {
        if (publisherName == null || publisherName.isEmpty())
            return ImcSystem.MEDIUM.WIFI;

        if (publisherName.toLowerCase().startsWith(IRIDIUM_PUBLISHER_PREFIX)) {
            return ImcSystem.MEDIUM.SATELLITE;
        }
        else if (publisherName.toLowerCase().startsWith(ACOUSTIC_PUBLISHER_PREFIX)
                || publisherName.toLowerCase().startsWith(ACOUSTIC2_PUBLISHER_PREFIX)) {
            return ImcSystem.MEDIUM.ACOUSTIC;
        }
        else if (publisherName.toLowerCase().startsWith(SMS_PUBLISHER_PREFIX)
                || publisherName.toLowerCase().startsWith(GSM_PUBLISHER_PREFIX)) {
            return ImcSystem.MEDIUM.SMS;
        }
        else if (publisherName.toLowerCase().startsWith(MQTT_PUBLISHER_PREFIX)
                || publisherName.toLowerCase().startsWith(MQTT_PUBLISHER_PREFIX)) {
            return ImcSystem.MEDIUM.MQTT;
        }

        return ImcSystem.MEDIUM.WIFI; // Default
    }

    void processEntityInfo(MessageInfo info, EntityInfo msg) {
        manager.imcDefinition.getResolver().setEntityName(msg.getSrc(), msg.getSrcEnt(), msg.getLabel());
    }

    void processMessagePart(MessageInfo info, MessagePart msg) {
        IMCMessage m = fragmentHandler.setFragment((MessagePart)msg);
        if (m != null)
            manager.postInternalMessage(info.getPublisher(), m);
    }

    void processEntityList(ImcId16 id, MessageInfo info, EntityList msg) {
        EntitiesResolver.setEntities(id.toString(), msg);
        manager.imcDefinition.getResolver().setEntityMap(msg.getSrc(), msg.getList());
        ImcSystem sys = ImcSystemsHolder.lookupSystem(id);
        if (sys != null) {
            EntitiesResolver.setEntities(sys.getName(), msg);
        }
    }

    void processReportedState(MessageInfo info, ReportedState msg) {
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

            if (imcSys != null) {
                ImcSystem.MEDIUM mediumReported = getMediumReported(msg.getSourceName());
                imcSys.setLocation(loc, recTimeMillis, mediumReported);
            }
            else {
                extSys.setLocation(loc, recTimeMillis);
            }
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

    void checkPlanChecksum(PlanType lastActivePlan, StateReport srep, PlanControlState pcsMsg) {
        if (lastActivePlan == null)
            return;

        String[] tks = lastActivePlan.getId().split("\\|");

        byte[] bytes = lastActivePlan.getId().getBytes(StandardCharsets.UTF_8);
        int lastActivePlanChecksum = IMCUtil.computeCrc16(bytes, 0, 0);
        if (srep.getPlanChecksum() == lastActivePlanChecksum && tks.length <= 1) {
            // If the plan checksum matches and there is no maneuver id, we can use the plan id directly
            pcsMsg.setPlanId(lastActivePlan.getId());
            return;
        }

        if (tks.length > 1) {
            byte[] bytesTks = tks[0].getBytes(StandardCharsets.UTF_8);
            int lastCSun = IMCUtil.computeCrc16(bytesTks, 0, 0);
            if (srep.getPlanChecksum() == lastCSun) {
                pcsMsg.setPlanId(tks[0]);
                return;
            }
        }
        pcsMsg.setPlanId("?");

        // Let us see if we can get the plan id from the plan name plus the maneuver id
        String planId = tks[0].trim();
        String manId = tks.length > 1 ? tks[1].replaceAll("Man:", "").trim() : "";
        if (srep.getPlanChecksum() == lastActivePlanChecksum) {
            pcsMsg.setPlanId(planId);
            pcsMsg.setManId(manId);
            return;
        }

        byte[] bytes2 = (planId + "|Man:1").getBytes(StandardCharsets.UTF_8);
        int planChecksum = IMCUtil.computeCrc16(bytes2, 0, 0);
        if (planChecksum == lastActivePlanChecksum) {
            pcsMsg.setPlanId(planId);
            pcsMsg.setManId("1");
        }

        // If the plan checksum does not match, it might be a different maneuver
        // Do nothing
    }

    void processStateReport(MessageInfo info, StateReport msg, ArrayList<IMCMessage> messagesCreatedToForward, ImcMsgManager imcMsgManager) {
        String sysId = msg.getSourceName();

        long dataTimeMillis = msg.getStime() * 1000;

        double lat = msg.getLatitude();
        double lon = msg.getLongitude();
        double depth = msg.getDepth() == 0xFFFF ? -1 : msg.getDepth() / 10.0;
        // double altitude = msg.getAltitude() == 0xFFFF ? -1 : msg.getAltitude() / 10.0;
        double heading = Math.toDegrees(AngleUtils.nomalizeAngleRads2Pi(((double) msg.getHeading() / 65535.0) * Math.PI * 2));
        double speedMS = msg.getSpeed() / 100.;
        NeptusLog.pub().info("Received report from "+msg.getSourceName());

        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(sysId);
        if (imcSys == null) {
            NeptusLog.pub().error("Could not find system with id "+sysId);
            return;
        }

        boolean isDataNewerThanLastPcs = true;
        ImcSystemState sysState = imcMsgManager.getState(sysId);
        if (sysState != null) {
            PlanControlState pcsMsg = sysState.last(PlanControlState.class);
            isDataNewerThanLastPcs = pcsMsg == null || dataTimeMillis > pcsMsg.getTimestampMillis() ||
                    (dataTimeMillis >= pcsMsg.getTimestampMillis() && msg.getPlanChecksum() > 0 &&
                            ("".equalsIgnoreCase(pcsMsg.getPlanId()) || "?".equalsIgnoreCase(pcsMsg.getPlanId())));
        }

        LocationType loc = new LocationType(lat, lon);
        loc.setDepth(depth);
        ImcSystem.MEDIUM mediumReported = getMediumReported(msg.getSourceName());
        imcSys.setLocation(loc, dataTimeMillis, mediumReported);
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

            if (sysState != null) {
                FuelLevel lastFuelLevel = sysState.last(FuelLevel.class);
                if (lastFuelLevel == null || dataTimeMillis > lastFuelLevel.getTimestampMillis()) {
                    messagesCreatedToForward.add(fuelLevelMsg);
                }
            }
        }

        int execState = msg.getExecState();
        PlanControlState pcsMsg = new PlanControlState();
        IMCUtils.copyHeader(msg, pcsMsg);
        pcsMsg.setTimestampMillis(dataTimeMillis);
        switch (execState) {
            case -1:
                pcsMsg.setState(PlanControlState.STATE.READY);
                break;
            case -3:
                pcsMsg.setState(PlanControlState.STATE.INITIALIZING);
                break;
            case -2:
            case -4:
                pcsMsg.setState(PlanControlState.STATE.BLOCKED);
                break;
            default:
                if (execState > 0)
                    pcsMsg.setState(PlanControlState.STATE.EXECUTING);
                else
                    pcsMsg.setState(PlanControlState.STATE.BLOCKED);
                break;
        }

        pcsMsg.setPlanEta(-1);
        pcsMsg.setPlanProgress(execState >= 0 ? execState : -1);
        pcsMsg.setManId("");
        pcsMsg.setManEta(-1);
        pcsMsg.setManType(0xFFFF);

        if (isDataNewerThanLastPcs) {
            messagesCreatedToForward.add(pcsMsg);
            checkPlanChecksum(imcSys.getActivePlan(), msg, pcsMsg);
        }
    }

    void processAssetReport(MessageInfo info, AssetReport msg, ArrayList<IMCMessage> messagesCreatedToForward) {

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
            if (imcSys != null) {
                ImcSystem.MEDIUM locMedium = ImcSystem.MEDIUM.WIFI;
                switch (mediumReported) {
                    case ACOUSTIC:
                        locMedium = ImcSystem.MEDIUM.ACOUSTIC;
                        break;
                    case SMS:
                        locMedium = ImcSystem.MEDIUM.SMS;
                        break;
                    case SATELLITE:
                        locMedium = ImcSystem.MEDIUM.SATELLITE;
                        break;
                    case MQTT:
                        locMedium = ImcSystem.MEDIUM.MQTT;
                        break;
                    case WIFI:
                    default:
                        break;
                }
                imcSys.setLocation(loc, dataTimeMillis, locMedium);
            } else {
                extSys.setLocation(loc, dataTimeMillis);
            }
        }

        double rollRads = Double.NaN;
        double pitchRads = Double.NaN;
        double headingRads = Double.NaN;
        EulerAngles ea = (EulerAngles) otherMsgs.stream().filter(m -> m instanceof EulerAngles)
                .findFirst().orElse(null);
        if (ea != null) {
            rollRads = Double.isFinite(ea.getPhi()) ? ea.getPhi() : 0.0;
            pitchRads = Double.isFinite(ea.getTheta()) ? ea.getTheta() : 0.0;
            headingRads = ea.getPsi();
        } else if (Double.isFinite(cogRads) && Double.isFinite(speedMS) && Math.abs(speedMS) > 0.2) {
            headingRads = AngleUtils.nomalizeAngleRads2Pi(cogRads * (speedMS < 0 ? -1 : 1));
        }

        if (Double.isFinite(headingRads)) {
            if (imcSys != null) {
                imcSys.setAttitudeDegrees(Math.toDegrees(rollRads), Math.toDegrees(pitchRads),
                        Math.toDegrees(headingRads), dataTimeMillis);
                imcSys.storeData(
                        SystemUtils.HEADING_DEGS_KEY,
                        (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(headingRads), 0)),
                        dataTimeMillis, true);
            }
            else {
                extSys.setAttitudeDegrees(Math.toDegrees(rollRads), Math.toDegrees(pitchRads),
                        Math.toDegrees(headingRads), dataTimeMillis);
                extSys.storeData(
                        SystemUtils.HEADING_DEGS_KEY,
                        (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(headingRads), 0)),
                        dataTimeMillis, true);
            }
        }
        if (imcSys != null) {
            imcSys.storeData(SystemUtils.GROUND_SPEED_KEY, speedMS, dataTimeMillis, true);
            imcSys.storeData(SystemUtils.COURSE_DEGS_KEY,
                    (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(cogRads), 0)),
                    dataTimeMillis, true);
        } else {
            extSys.storeData(SystemUtils.GROUND_SPEED_KEY, speedMS, dataTimeMillis, true);
            extSys.storeData(SystemUtils.COURSE_DEGS_KEY,
                    (int) AngleUtils.nomalizeAngleDegrees360(MathMiscUtils.round(Math.toDegrees(cogRads), 0)),
                    dataTimeMillis, true);
        }
    }

    void processRemoteSensorInfo(MessageInfo info, RemoteSensorInfo msg) {
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

            if (imcSys != null) {
                ImcSystem.MEDIUM mediumReported = getMediumReported(msg.getSourceName());
                imcSys.setLocation(loc, recTimeMillis, mediumReported);
            }
            else {
                extSys.setLocation(loc, recTimeMillis);
            }
        }

        if (Double.isFinite(headingRad)) {
            double headingDeg = Math.toDegrees(headingRad);

            if (imcSys != null)
                imcSys.setAttitudeDegrees(headingDeg, recTimeMillis);
            else
                extSys.setAttitudeDegrees(headingDeg, recTimeMillis);
        }

        // Process sensor class
        VehicleType.SystemTypeEnum type = SystemUtils.getSystemTypeFrom(sensorClass);
        VehicleType.VehicleTypeEnum typeVehicle = SystemUtils.getVehicleTypeFrom(sensorClass);
        ExternalSystem.ExternalTypeEnum typeExternal = SystemUtils.getExternalTypeFrom(sensorClass);
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
}
