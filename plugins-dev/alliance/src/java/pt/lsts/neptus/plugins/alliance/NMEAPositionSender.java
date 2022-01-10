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
 * Author: zp
 * 20/07/2016
 */
package pt.lsts.neptus.plugins.alliance;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.google.common.eventbus.Subscribe;

import dk.dma.ais.message.AisMessage1;
import dk.dma.ais.message.AisPosition;
import dk.dma.ais.sentence.Vdm;
import dk.dma.enav.model.geometry.Position;
import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.UnitsUtil;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription
public class NMEAPositionSender extends ConsolePanel {

    private static final long serialVersionUID = 8057240981558832292L;

    @NeptusProperty(userLevel = LEVEL.REGULAR)
    public String hostAddress = "127.0.0.1";

    @NeptusProperty(userLevel = LEVEL.REGULAR)
    public int hostPort = 1234;

    @NeptusProperty(userLevel = LEVEL.REGULAR)
    public boolean publishExternalSystems = false;

    @NeptusProperty(userLevel = LEVEL.ADVANCED)
    public boolean debug = false;

    private LinkedHashMap<String, AISPosition> positions = new LinkedHashMap<>();

    @Subscribe
    public void on(Announce announce) {
        synchronized (positions) {
            if (!positions.containsKey(announce.getSysName()))  {
                AISPosition pos = new AISPosition();
                pos.name = announce.getSysName();
                pos.mmsi = announce.getSrc();
                positions.put(pos.name, pos);
            }

            AISPosition pos = positions.get(announce.getSysName());

            pos.latitude = Math.toDegrees(announce.getLat());
            pos.longitude = Math.toDegrees(announce.getLon());
            
            pos.timeMillis = announce.getTimestampMillis();
        }
    }

    @Subscribe
    public void on(EstimatedState state) {
        synchronized (positions) {
            if (!positions.containsKey(state.getSourceName()))
                return;

            AISPosition pos = positions.get(state.getSourceName());

            LocationType loc = IMCUtils.getLocation(state).convertToAbsoluteLatLonDepth();

            pos.latitude = loc.getLatitudeDegs();
            pos.longitude = loc.getLongitudeDegs();
            pos.heading = Math.toDegrees(state.getPsi());
            
            double vx = state.getVx();
            double vy = state.getVy();
            // double vz = state.getVz();
            double courseRad = AngleUtils.calcAngle(0, 0, vy, vx);
            double groundSpeedMS = Math.sqrt(vx * vx + vy * vy);
            
            pos.speed_knots = groundSpeedMS * UnitsUtil.MS_TO_KNOT;
            pos.turnRate = Math.toDegrees(state.getR());
            pos.cog = AngleUtils.nomalizeAngleDegrees360(Math.toDegrees(courseRad));
            
            pos.timeMillis = state.getTimestampMillis();
        }
    }

    @Periodic(millisBetweenUpdates=1000)
    public void sendPositions() {

        synchronized (positions) {
            for (Entry<String, AISPosition> pos : positions.entrySet()) {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    String[] aisStrings = convert(pos.getValue());
                    for (String s : aisStrings) {
                        byte[] buff = s.toString().getBytes(Charset.forName("ASCII"));
                        DatagramPacket packet = new DatagramPacket(buff, buff.length, new InetSocketAddress(hostAddress, hostPort));
                        socket.send(packet);
                        socket.close();    
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
    }

    @Periodic(millisBetweenUpdates=1000)
    public void sendExternalPositions() {
        if (!publishExternalSystems)
            return;
        
        ExternalSystem[] allExtSystems = ExternalSystemsHolder.lookupAllSystems();
        for (ExternalSystem externalSystem : allExtSystems) {
            try {
                AISPosition pos = new AISPosition();
                pos.name = externalSystem.getName();
                try {
                    int mmsi = (int) externalSystem.retrieveData(SystemUtils.MMSI_KEY);
                    pos.mmsi = mmsi;
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(e);;
                    pos.mmsi = pos.name.hashCode();
                }

                LocationType loc = externalSystem.getLocation().getNewAbsoluteLatLonDepth();
                pos.latitude = loc.getLatitudeDegs();
                pos.longitude = loc.getLongitudeDegs();

                pos.timeMillis = externalSystem.getLocationTimeMillis();
                
                pos.heading = externalSystem.getYawDegrees();

                double groundSpeedMS = 0;
                try {
                    groundSpeedMS = (double) externalSystem.retrieveData(SystemUtils.GROUND_SPEED_KEY);
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(e);;
                }
                pos.speed_knots = groundSpeedMS * UnitsUtil.MS_TO_KNOT;
                        
                double courseDeg = pos.heading;
                try {
                    courseDeg = (double) externalSystem.retrieveData(SystemUtils.COURSE_DEGS_KEY);
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(e);;
                }
                
                // pos.turnRate = Math.toDegrees(state.getR());
                pos.cog = AngleUtils.nomalizeAngleDegrees360(courseDeg);

                try {
                    DatagramSocket socket = new DatagramSocket();
                    String[] aisStrings = convert(pos);
                    for (String s : aisStrings) {
                        try {
                            byte[] buff = s.toString().getBytes(Charset.forName("ASCII"));
                            DatagramPacket packet = new DatagramPacket(buff, buff.length,
                                    new InetSocketAddress(hostAddress, hostPort));
                            socket.send(packet);
                            if (debug)
                                NeptusLog.pub().warn(String.format("Sent external system AIS: %s", s));
                            socket.close();
                        }
                        catch (Exception e) {
                            if (debug)
                                NeptusLog.pub().warn(String.format("Error sending external system AIS: %s", s));
                            NeptusLog.pub().error(e);
                        }    
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String[] convert(AISPosition position) throws Exception {
        AisMessage1 msg1 = new AisMessage1();
        msg1.setRepeat(0);
        msg1.setUserId((int)position.mmsi);
        msg1.setNavStatus(0);
        msg1.setRot(0);
        msg1.setSog(201);
        msg1.setPosAcc(1);
        AisPosition pos = new AisPosition(Position.create(position.latitude, position.longitude));
        msg1.setPos(pos);
        msg1.setCog((int)position.cog);
        msg1.setTrueHeading((int)position.heading);
        
        if (position.timeMillis < 0)
            msg1.setUtcSec((int) (System.currentTimeMillis() / 1000));
        else
            msg1.setUtcSec((int) (position.timeMillis / 1000));
        
        msg1.setSpecialManIndicator(0);
        msg1.setSpare(0);
        msg1.setRaim(0);
        msg1.setSyncState(0);
        msg1.setSlotTimeout(0);
        
        String[] sentences = Vdm.createSentences(msg1, 1);
        
        return sentences;
    }

    public NMEAPositionSender(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
    }

    class AISPosition {
        String name;
        double latitude, longitude, speed_knots, heading, cog, turnRate;
        long mmsi;
        long timeMillis = -1;
    }
}
