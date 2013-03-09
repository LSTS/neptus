/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Feb 26, 2013
 */
package pt.up.fe.dceg.neptus.maneuvers;

import java.util.Arrays;

import pt.up.fe.dceg.neptus.imc.DesiredSpeed;
import pt.up.fe.dceg.neptus.imc.DesiredSpeed.SPEED_UNITS;
import pt.up.fe.dceg.neptus.imc.DesiredZ;
import pt.up.fe.dceg.neptus.imc.DesiredZ.Z_UNITS;
import pt.up.fe.dceg.neptus.imc.FollowReference;
import pt.up.fe.dceg.neptus.imc.PlanControl;
import pt.up.fe.dceg.neptus.imc.PlanControl.OP;
import pt.up.fe.dceg.neptus.imc.PlanControl.TYPE;
import pt.up.fe.dceg.neptus.imc.PlanManeuver;
import pt.up.fe.dceg.neptus.imc.PlanSpecification;
import pt.up.fe.dceg.neptus.imc.Reference;
import pt.up.fe.dceg.neptus.imc.net.UDPTransport;
import pt.up.fe.dceg.neptus.util.comm.IMCSendMessageUtils;

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
            dspeed.setSpeedUnits(SPEED_UNITS.METERS_PS);
            DesiredZ z = new DesiredZ();
            z.setValue(0);
            z.setZUnits(Z_UNITS.DEPTH);
            
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
