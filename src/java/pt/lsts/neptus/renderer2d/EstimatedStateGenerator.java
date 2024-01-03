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
 * Author: José Pinto and pdias
 * 2007/05/30
 */
package pt.lsts.neptus.renderer2d;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.renderer3d.Camera3D;
import pt.lsts.neptus.renderer3d.Renderer3D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zepinto
 * @author pdias
 */
public class EstimatedStateGenerator {

	private SystemPositionAndAttitude lastState = new SystemPositionAndAttitude(new LocationType(), 0, 0, 0);
	private long lastTimeStamp = 0;
	private long lastGenStamp = 0;
	private SystemPositionAndAttitude lastGenState = lastState;
	private double[] disturbanceVxyz = {0.0, 0.0, 0.0};
	
	public void setState(SystemPositionAndAttitude state) {
		this.lastState = state;
		this.lastTimeStamp = System.currentTimeMillis();
		lastGenStamp = lastTimeStamp;
		lastGenState = new SystemPositionAndAttitude(lastState);
		disturbanceVxyz = calcDisturbance();
	}
	
	/**
	 * @return
	 */
	private double[] calcDisturbance() {
		SystemPositionAndAttitude st = lastState;
		double[] uvwNED = CoordinateUtil.bodyFrameToInertialFrame(
				st.getU(), st.getV(), st.getW(),
				st.getRoll(), st.getPitch(), st.getYaw());
		double[] dist = { st.getVx() - uvwNED[0], st.getVy() - uvwNED[1],
				st.getVz() - uvwNED[2] };
		return dist;
	}

	public long timeSinceLastState() {
		return System.currentTimeMillis() - lastTimeStamp;		
	}
	
	public SystemPositionAndAttitude getInterpolatedState() {
		SystemPositionAndAttitude st = new SystemPositionAndAttitude(lastGenState);
				
		double diffSecs = (double)(System.currentTimeMillis() - lastGenStamp)/1000.0;
		//double diffSecs2 = (double)(System.currentTimeMillis() - lastTimeStamp)/1000.0;

	
		st.setYaw(st.getYaw()+st.getR()*diffSecs);     //applying azimuth 
		st.setPitch(st.getPitch()+st.getQ()*diffSecs); //applying elevation
		st.setRoll(st.getRoll()+st.getP()*diffSecs);   //applying spin

		/*
		double au, av, aw;
		au = st.getRoll() - lastState.getRoll();
		av = st.getPitch() - lastState.getPitch();
		aw = st.getYaw() - lastState.getYaw();
		*/	
		
		//Point3d pww=Util3D.setTransform(new Point3d(st.getU() * diffSecs, st.getV()
		//		* diffSecs,st.getW() * diffSecs), st.getP()*diffSecs2, st.getQ()*diffSecs2,
		//		st.getR()*diffSecs2);
		//double[] ww={pww.x,pww.y,pww.z};
		
//		double[] ww1 = CoordinateUtil.bodyFrameToInertialFrame(st.getU() * diffSecs, st.getV()
//				* diffSecs, st.getW() * diffSecs, st.getRoll(), st.getPitch(),
//				st.getYaw());

		// (pdias@20091201) Considering only [Vx,Vy,Vz] is not enough
		//        So will go back to [u,v,w] and adding the disturbance
		double[] deltaUVW_NED = CoordinateUtil.bodyFrameToInertialFrame(st.getU()
				* diffSecs, st.getV() * diffSecs, st.getW() * diffSecs, st
				.getRoll(), st.getPitch(), st.getYaw());
		double[] ww = new double[] {deltaUVW_NED[0] + disturbanceVxyz[0] * diffSecs,
				deltaUVW_NED[1] + disturbanceVxyz[1] * diffSecs,
				deltaUVW_NED[2] + disturbanceVxyz[2] * diffSecs};
		// (pdias@20100125) Considering Vxyz is more or less the same as the 20091201 
		//System.err.println("ww[x]" + ww[0] + " " + ww2[0]);
		//System.err.println("ww[y]" + ww[1] + " " + ww2[1]);
		//System.err.println("ww[z]" + ww[2] + " " + ww2[2]);
		
		st.addNEDOffsets(ww[0], ww[1], ww[2]);
		
		//Calculating new VxVyVz
		double[] uvwNED = CoordinateUtil.bodyFrameToInertialFrame(
				st.getU(), st.getV(), st.getW(),
				st.getRoll(), st.getPitch(), st.getYaw());
		double[] distP = { uvwNED[0] +  disturbanceVxyz[0],
				uvwNED[1] +  disturbanceVxyz[1],
				uvwNED[2] +  disturbanceVxyz[2]};
		st.setVxyz(distP[0], distP[1], distP[2]);

		
		lastGenState = st;
		lastGenStamp = System.currentTimeMillis();
		
		return st;
	}
	
	public static void main(String[] args) throws Exception {
		ConfigFetch.initialize();
		Renderer3D sr = new Renderer3D(new Camera3D[]{new Camera3D(Camera3D.USER)}, (short)1, (short)1);
		
		VehicleType veh = VehiclesHolder.getVehicleById("alfa02");
		GuiUtils.testFrame(sr, "Test Interpolation State", 800, 600 );
		SystemPositionAndAttitude st = new SystemPositionAndAttitude(new LocationType(), 0, 0, 0);
		st.setUVW(1.0, 0.0, 0.0);
		//st.setPQR(Math.toRadians(0), Math.toRadians(1), Math.toRadians(360/5));
		st.setPQR(Math.toRadians(0), Math.toRadians(0), Math.toRadians(5));
		st.setVxyz(0.0, 0.0, 0.0);
		sr.vehicleStateChanged(veh.getId(), st);
		sr.followVehicle(veh.getId());
		sr.setVehicleTailOn(new String[]{veh.getId()});
		EstimatedStateGenerator gen = new EstimatedStateGenerator();
		gen.setState(st);
		for (int i = 0; i < 100000; i++) {			
			//NeptusLog.pub().info("<###> "+st.getNEDPosition()[0]+", "+st.getNEDPosition()[1]+", "+st.getNEDPosition()[2]);
			System.out.printf("x=%f, y=%f, z=%f,  |uvw|=%f, |VxVyVz|=%f\n",
					st.getNEDPosition()[0], st.getNEDPosition()[1], st.getNEDPosition()[2],
					Math.sqrt(st.getU()*st.getU() + st.getV()*st.getV() + st.getW()*st.getW()),
					Math.sqrt(st.getVx()*st.getVx() + st.getVy()*st.getVy() + st.getVz()*st.getVz()));
			Thread.sleep(100);
			st = gen.getInterpolatedState();
			sr.vehicleStateChanged(veh.getId(), st);
		}
	}

}
