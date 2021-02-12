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
 * 02/09/2016
 */
package org.necsave.test;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowPoint;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.SystemType;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.imc.net.Consume;
import pt.lsts.imc.net.IMCProtocol;
import pt.lsts.neptus.messages.listener.Periodic;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.util.WGS84Utilities;

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
    
    @NeptusProperty(description="Duration, in seconds, after which maneuver is finished.")
    protected double duration = 60.0;
    
    protected static final String PLANID = "TestFollowPoint";
    protected IMCProtocol imc = null;
    protected EstimatedState state = null;
    protected PathControlState pathState = null;
    protected boolean controlling = false, finished = false;
    protected long controlStart = 0;
    
    @Consume
    void on(PlanControlState msg) {
        if (msg.getSourceName().equals(follower))
            controlling = msg.getState() == STATE.EXECUTING && msg.getPlanId().equals(PLANID);
    }
    
    @Consume
    void on(PathControlState msg) {
        if (msg.getSourceName().equals(leader))
            pathState = msg;
    }
    
    @Consume
    void on(EstimatedState msg) {
        if (msg.getSourceName().equals(leader))
            state = msg;        
    }
    
    @Periodic(1000)
    void step() {
        if (!controlling && !finished) {
            PlanControl pc = new PlanControl()
                    .setPlanId(PLANID)
                    .setOp(OP.START)
                    .setType(TYPE.REQUEST)
                    .setFlags(0)
                    .setArg(new FollowPoint()
                                .setTarget(leader)
                                .setMaxSpeed(max_speed)
                                .setSpeedUnits(SpeedUnits.METERS_PS)
                                .setZ(0)
                                .setZUnits(ZUnits.DEPTH));
            
            imc.sendMessage(follower, pc);
            System.err.println("Not controlling...");
        }
        else {
            if (controlStart == 0)
                controlStart = System.currentTimeMillis();
            
            finished = System.currentTimeMillis() - controlStart > duration * 1000; 
            
            if (finished) {
                imc.sendMessage(follower, new RemoteSensorInfo().setId(leader).setData("finished=true"));
                System.out.println("Maneuver is finished.");
            }
            else {
                EstimatedState toSend = state;
                state = null;
                
                if (toSend == null) {
                    System.err.println("Target state is unknown...");
                    return;
                }
                
                double lld[] = WGS84Utilities.toLatLonDepth(toSend);

                RemoteSensorInfo sinfo = new RemoteSensorInfo()
                    .setId(leader)
                    .setLat(Math.toRadians(lld[0]))
                    .setLon(Math.toRadians(lld[1]))
                    .setHeading(getCourse())
                    .setData("speed="+toSend.getU());
                
                imc.sendMessage(follower, sinfo);
                System.out.println("Sent position to follower.");
            }
        }
    }
    
    double getCourse() {
        PathControlState path = pathState;
        EstimatedState pos = state;
        
        if (path != null) {
            double startLat = Math.toDegrees(path.getStartLat());
            double endLat = Math.toDegrees(path.getEndLat());
            double startLon = Math.toDegrees(path.getStartLon());
            double endLon = Math.toDegrees(path.getEndLon());
            double[] displacement = WGS84Utilities.WGS84displacement(startLat, startLon, 0, endLat, endLon, 0);
            return Math.atan2(displacement[1], displacement[0]);
        }
        
        if (pos != null)
            return pos.getPsi();
        
        return 0;
    }
    
    void init() {
        imc = new IMCProtocol("FollowPointTest", 7007, 0x8032, SystemType.CCU);
        imc.connect(follower);
        imc.connect(leader);
        imc.register(this);
    }
    
    public static void main(String[] args) throws Exception {
        TestDuneFollowPoint followPointTest = new TestDuneFollowPoint();
        boolean cancelled = PluginUtils.editPluginProperties(followPointTest, true);
        
        if (!cancelled)
            followPointTest.init();
    }
}
