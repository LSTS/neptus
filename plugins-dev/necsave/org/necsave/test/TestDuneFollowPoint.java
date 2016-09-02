/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 02/09/2016
 */
package org.necsave.test;

import pt.lsts.imc.Announce.SYS_TYPE;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowPoint;
import pt.lsts.imc.FollowPoint.SPEED_UNITS;
import pt.lsts.imc.FollowPoint.Z_UNITS;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.net.Consume;
import pt.lsts.imc.net.IMCProtocol;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.messages.listener.Periodic;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class TestDuneFollowPoint {

    @NeptusProperty
    protected String follower = "caravela";
    
    @NeptusProperty
    protected String leader = "lauv-xplore-1";
    
    @NeptusProperty
    protected double max_speed = 2.0;
    
    
    protected IMCProtocol imc = null;
    protected EstimatedState state = null;
    protected boolean controlling = false;
    
    @Consume
    void on(PlanControlState msg) {
        if (msg.getSourceName().equals(follower))
            controlling = msg.getManType() == FollowPoint.ID_STATIC;        
    }
    
    @Consume
    void on(EstimatedState msg) {
        if (msg.getSourceName().equals(leader))
            state = msg;        
    }
    
    @Periodic(1000)
    void step() {
        if (!controlling) {
            System.err.println("Not controlling...");
            FollowPoint maneuver = new FollowPoint()
                    .setMaxSpeed(max_speed)
                    .setSpeedUnits(SPEED_UNITS.METERS_PS)
                    .setZ(0)
                    .setZUnits(Z_UNITS.DEPTH);
            PlanControl pc = new PlanControl();
            pc.setPlanId("TestFollowPoint");
            pc.setOp(OP.START);
            pc.setType(TYPE.REQUEST);
            pc.setArg(maneuver);
            imc.sendMessage(follower, pc);
            System.out.println(pc.asJSON());
            return;
        }
        
        EstimatedState toSend = state;
        state = null;
        if (toSend == null) {
            System.err.println("Target state is unknown...");
            return;
        }
        
        RemoteSensorInfo sinfo = new RemoteSensorInfo();
        sinfo.setId(leader);
        LocationType loc = IMCUtils.parseLocation(state).convertToAbsoluteLatLonDepth();
        
        sinfo.setLat(loc.getLatitudeRads());
        sinfo.setLon(loc.getLongitudeRads());
        sinfo.setHeading(state.getPsi());
        sinfo.setData("speed="+state.getU());
        
        imc.sendMessage(follower, sinfo);
        System.out.println("Sent position to follower.");
        System.out.println(sinfo.asJSON());
        
    }
    
    void init() {
        imc = new IMCProtocol("FollowPointTest", 7007, 0x8032, SYS_TYPE.CCU);
        imc.connect(follower);
        imc.connect(leader);
        imc.register(this);
    }
    
    public static void main(String[] args) throws Exception {
        TestDuneFollowPoint followPointTest = new TestDuneFollowPoint();
        boolean cancelled = PluginUtils.editPluginProperties(followPointTest, true);
        
        if (cancelled)
            return;
        
        followPointTest.init();
    }
}
