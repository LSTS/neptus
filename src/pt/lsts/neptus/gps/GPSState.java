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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gps;

import java.util.StringTokenizer;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * This class represents a GPS State which can be obtained by a physical GPS device.
 * @author ZP
 */
public class GPSState {

	private int numberOfVisibleSatellites;
	private double heading, altitude, latitude, longitude, estimatedError, speed, hdop, pdop, vdop;
	
	public GPSState(String logFormattedString) {
		super();
		StringTokenizer st = new StringTokenizer(logFormattedString, ", ");
		if (st.countTokens() <= 5) {
			System.err.println("Bad formatted log string: "+logFormattedString);
			return;
		}
		st.nextToken();
		setLatitude(Double.parseDouble(st.nextToken()));
		setLongitude(Double.parseDouble(st.nextToken()));
		setAltitude(Double.parseDouble(st.nextToken()));
		setHeading(Double.parseDouble(st.nextToken()));
		
	}
	
	public GPSState() {
		super();
	}
	
	/**
	 * The read altitude, in meters
	 * @return The altitude, in meters
	 */
	public double getAltitude() {
		return altitude;
	}
	
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	
	/**
	 * Generates a LocationType object based on this GPS state
	 * @return A LocationType object based on this GPS state
	 */
	public LocationType getCurrentLocation() {
		LocationType lt = new LocationType();
		lt.setLatitude(getLatitude());
		lt.setLongitude(getLongitude());
		lt.setHeight(getAltitude());
		return lt;
	}
	
	/**
	 * @return The error estimation in meters
	 */
	public double getEstimatedError() {
		return estimatedError;
	}
	public void setEstimatedError(double estimatedError) {
		this.estimatedError = estimatedError;
	}
	
	/**
	 * @return the hdop
	 */
	public double getHdop() {
		return hdop;
	}
	
	/**
	 * @param hdop the hdop to set
	 */
	public void setHdop(double hdop) {
		this.hdop = hdop;
	}

	/**
	 * @return the pdop
	 */
	public double getPdop() {
		return pdop;
	}
	
	/**
	 * @param pdop the pdop to set
	 */
	public void setPdop(double pdop) {
		this.pdop = pdop;
	}
	
	/**
	 * @return the vdop
	 */
	public double getVdop() {
		return vdop;
	}
	
	/**
	 * @param vdop the vdop to set
	 */
	public void setVdop(double vdop) {
		this.vdop = vdop;
	}
	
	/**
	 * 
	 * @return The latitude in this GPS state
	 */
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	/**
	 * 
	 * @return The longitude in this GPS state
	 */
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	/**
	 * 
	 * @return The number of visible satellites reported by the device
	 */
	public int getNumberOfVisibleSatellites() {
		return numberOfVisibleSatellites;
	}
	public void setNumberOfVisibleSatellites(int numberOfVisibleSatellites) {
		this.numberOfVisibleSatellites = numberOfVisibleSatellites;
	}
	
	/**
	 * 
	 * @return The speed reported by the device
	 */
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	
	/**
	 * Generates a string representation of the GPS state
	 */
	public String toString() {
		return "[GPS_STATE]\n"
		 + "Lat: "+getLatitude()+", Long: "+getLongitude()+"\n"
		 + "Alt: "+getAltitude()+", Speed: "+getSpeed()+"\n"
		 + "Heading: "+getHeading()+"\n"
		 + "HDOP: "+getHdop()+"\n"
		 + "NoSatellites: "+getNumberOfVisibleSatellites()+", Error: "+getEstimatedError();
	}
	
	public String toLogFormattedString() {
		return "GPS_State: " + getLatitude() + "," + getLongitude() + ","
				+ getAltitude() + "," + getHeading() + "," + getSpeed() + ","
				+ getHdop();
	}
	
	protected Object clone() {
		GPSState clone = new GPSState();
		clone.setAltitude(getAltitude());
		clone.setLatitude(getLatitude());
		clone.setLongitude(getLongitude());
		clone.setHeading(getHeading());		
		clone.setSpeed(getSpeed());
		clone.setEstimatedError(getEstimatedError());
		clone.setNumberOfVisibleSatellites(getNumberOfVisibleSatellites());
		clone.setAltitude(getAltitude());
		clone.setHdop(getHdop());
		return clone;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof GPSState) {
			GPSState tmp = (GPSState) obj;
			return 
				tmp.getAltitude() == getAltitude() && 
				tmp.getLatitude() == getLatitude() &&
				tmp.getLatitude() == getLongitude() &&
				tmp.getSpeed() == getSpeed() &&
				tmp.getHeading() == getHeading() &&				
				tmp.getNumberOfVisibleSatellites() == getNumberOfVisibleSatellites() &&
				tmp.getEstimatedError() == getEstimatedError() &&
				tmp.getHdop() == getHdop();
		}
		return false;
	}

	public double getHeading() {
		return heading;
	}

	public void setHeading(double heading) {
		this.heading = heading;
	}
}
