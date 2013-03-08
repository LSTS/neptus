/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: SensorObj.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.renderer3d;

import pt.up.fe.dceg.neptus.console.plugins.JVideoPanelConsole;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;

public class SensorObj {
	
	
	// Neptus
	public Renderer3D render;

	public JVideoPanelConsole videoSource = null;

	public VehicleType vehicle;
	
	
	// Sensor Obj 
	public double xOffSet = 0, yOffSet = 0, zOffSet = 0;

	public boolean activated = false;

	public double pan = 0, tilt = 0 , roll =0;
	

	public SensorObj (Renderer3D R, VehicleType v)
	{
		render = R;
		vehicle = v;
	}
	
	public VehicleType getVehicle() {
		return vehicle;
	}

	public void setVehicle(VehicleType vehicle) {
		this.vehicle = vehicle;
	}
	
	public double getXOffSet() {
		return xOffSet;
	}


	public void setXOffSet(double offSet) {
		xOffSet = offSet;
	}


	public double getYOffSet() {
		return yOffSet;
	}


	public void setYOffSet(double offSet) {
		yOffSet = offSet;
	}


	public double getZOffSet() {
		return zOffSet;
	}


	public void setZOffSet(double offSet) {
		zOffSet = offSet;
	}


	public boolean isActivated() {
		return activated;
	}


	public void setActivated(boolean activated) {
		this.activated = activated;
	}


	public double getPan() {
		return pan;
	}


	public void setPan(double pan) {
		this.pan = pan;
	}


	public double getTilt() {
		return tilt;
	}


	public void setTilt(double tilt) {
		this.tilt = tilt;
	}


	public double getRoll() {
		return roll;
	}


	public void setRoll(double roll) {
		this.roll = roll;
	}


}
