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
 * $Id:: GPSState.java 9616 2012-12-30 23:23:22Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.gps;

import java.util.StringTokenizer;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

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
