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
 * Author: José Pinto
 * Feb 26, 2013
 */
package pt.lsts.neptus.maneuvers;

import java.util.Arrays;

import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.FollowReference;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.Reference;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.imc.net.UDPTransport;
import pt.lsts.neptus.comm.IMCSendMessageUtils;

/**
 * This program sends a PlanControl message containing a (quick) plan with a FollowReference maneuver
 * @author zp
 * 
 */
public class FollowReferenceTest {

    public static void main(String[] args) throws Exception {
        PlanControl startPlan = new PlanControl();
        startPlan.setType(TYPE.REQUEST);
        startPlan.setOp(OP.START);
        startPlan.setPlanId("follow_ref_test");
        FollowReference man = new FollowReference();
        man.setControlEnt((short)255);
        man.setControlSrc(65535);
        man.setAltitudeInterval(5);
        man.setTimeout(10);
        
        //startPlan.setPlanId("followref_test");
        PlanSpecification spec = new PlanSpecification();
        spec.setPlanId("followref_test");
        spec.setStartManId("1");
        PlanManeuver pm = new PlanManeuver();
        pm.setData(man);
        pm.setManeuverId("1");
        spec.setManeuvers(Arrays.asList(pm));
        startPlan.setArg(spec);
        int reqId = IMCSendMessageUtils.getNextRequestId();
        startPlan.setRequestId(reqId);
        startPlan.setFlags(0);
        UDPTransport t = new UDPTransport(6006, 1);
        //t.sendMessage("127.0.0.1", 6002, startPlan);
        
        while (true) {
            Thread.sleep(1000);
            
            DesiredSpeed dspeed = new DesiredSpeed();
            dspeed.setValue(1);
            dspeed.setSpeedUnits(SpeedUnits.METERS_PS);
            DesiredZ z = new DesiredZ();
            z.setValue(0);
            z.setZUnits(ZUnits.DEPTH);
            
            Reference ref = new Reference();
            ref.setLat(Math.toRadians(41.184199));
            ref.setLon(Math.toRadians(-8.705643));
            ref.setSpeed(dspeed);
            ref.setZ(z);
            ref.setFlags((short)(Reference.FLAG_SPEED | Reference.FLAG_LOCATION | Reference.FLAG_Z));
            t.sendMessage("127.0.0.1", 6002, ref);
        }     
    }    
}
