/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.imc.Announce;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static pt.lsts.neptus.comm.manager.imc.ImcMsgManager.DEFAULT_UDP_VEH_PORT;

public class ImcMsgManagerAnnounceProcessor {
    private final ImcMsgManager manager;

    public ImcMsgManagerAnnounceProcessor(ImcMsgManager manager) {
        this.manager = manager;
    }

    /**
     * @param info
     * @param ann
     * @param vci
     * @param id
     * @return
     * @throws IOException
     */
    SystemImcMsgCommInfo processAnnounceMessage(MessageInfo info, Announce ann, SystemImcMsgCommInfo vci,
                                                        ImcId16 id) throws IOException {

        LocalTime timeStart = LocalTime.now();

        String sia = info.getPublisherInetAddress();
        NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: publisher host address " + sia);

        boolean hostWasGuessed = true;

        InetSocketAddress[] retId = manager.announceWorker.getImcIpsPortsFromMessageImcUdp(ann);
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

        // Let us try to know any one in the announce IPs
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
            // Let's try to see if we received a message from any of the IPs
            String ipReceived = hostUdp.isEmpty() ? info.getPublisherInetAddress() : hostUdp;
            hostWasGuessed = hostUdp.isEmpty() ? hostWasGuessed : true;
            hostUdp = ipReceived;
            udpIpPortFound = true;
            NeptusLog.pub().debug("processAnnounceMessage for " + ann.getSysName() + "@" + id + " :: " + "no UDP reachable using " + hostUdp + ":" + portUdp);
        }

        InetSocketAddress[] retIdT = manager.announceWorker.getImcIpsPortsFromMessageImcTcp(ann);
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

        // Let us try to know any one in the announce IPs
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
            vci = manager.initSystemCommInfo(id, info.getPublisherInetAddress() + ":"
                    + (portUdp == 0 ? DEFAULT_UDP_VEH_PORT : portUdp));
            manager.updateUdpOnIpMapper(vci);
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
            resSys.setServicesProvided(manager.announceWorker.getImcServicesFromMessage(ann));
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
                    if (manager.dontIgnoreIpSourceRequest)
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
            if (er == null || er.isEmpty())
                requestEntityList = true;

            if (requestEntityList)
                manager.announceWorker.sendEntityListRequestMsg(resSys);

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

        manager.imcDefinition.getResolver().addEntry(ann.getSrc(), ann.getSysName());
        return vci;
    }
}
