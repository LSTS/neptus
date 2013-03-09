/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/05/02
 */
package pt.up.fe.dceg.neptus.mystate;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;

import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author pdias
 *
 */
public class MyState {

	private static String myStatePath = "./conf/mystate.xml";

    private static final MyState instance = loadFromXmlFile(myStatePath);
	
    @XmlElement
	private CoordinateSystem location = new CoordinateSystem();	
    
    private long lastLocationUpdateTimeMillis = -1;
	
	/**
	 * @return
	 */
	public static LocationType getLocation() {
		return new LocationType(instance.location);
	}
	
	/**
     * @return the lastLocationUpdateTimeMillis
     */
    public static long getLastLocationUpdateTimeMillis() {
        return instance.lastLocationUpdateTimeMillis;
    }
	
    /**
	 * @param location the location to set
	 */
	public static void setLocation(LocationType location) {
		instance.location.setLocation(location);
		instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		//instance.location.setRoll(0);
		//instance.location.setPitch(0);
		//instance.location.setYaw(0);
        instance.saveXml();
	}

	public static void setLocationAndAxis(LocationType location, double roll,
			double pitch, double yaw) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(roll);
		instance.location.setPitch(pitch);
		instance.location.setYaw(yaw);
        instance.saveXml();
	}

	public static void setLocationAndAxisRadians(LocationType location, double rollRadians,
			double pitchRadians, double yawRadians) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(Math.toDegrees(rollRadians));
		instance.location.setPitch(Math.toDegrees(pitchRadians));
		instance.location.setYaw(Math.toDegrees(yawRadians));
        instance.saveXml();
	}

	public static void setLocationAndAxis(LocationType location, double yaw) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(0);
		instance.location.setPitch(0);
		instance.location.setYaw(yaw);
        instance.saveXml();
	}

	public static void setLocationAndAxisRadians(LocationType location, double yawRadians) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(0);
		instance.location.setPitch(0);
		instance.location.setYaw(Math.toDegrees(yawRadians));
        instance.saveXml();
	}

	
	public static double[] getAxisAnglesDegrees() {
		return new double[] {instance.location.getRoll(),
				instance.location.getPitch(),
				instance.location.getYaw()};
	}

	public static double[] getAxisAnglesRadians() {
		return new double[] {Math.toRadians(instance.location.getRoll()),
				Math.toRadians(instance.location.getPitch()),
				Math.toRadians(instance.location.getYaw())};
	}

	public static double getHeadingInRadians() {
	    return Math.toRadians(instance.location.getYaw());
	}

	public static void setHeadingInRadians(double yawRadians) {
	    instance.location.setYaw(Math.toDegrees(yawRadians));
        instance.saveXml();
	}

	public static double getHeadingInDegrees() {
	    return instance.location.getYaw();
	}

	public static void setHeadingInDegrees(double yawDegrees) {
	    instance.location.setYaw(yawDegrees);
	    instance.saveXml();
	}

	private String asXml() {
	    StringWriter writer = new StringWriter();
	    JAXB.marshal(this, writer);
	    return writer.toString();
	}

	private static MyState loadXml(String xml) {
        MyState ms = JAXB.unmarshal(new StringReader(xml), MyState.class);
	    return ms;
	}

    private static MyState loadFromXmlFile(String myStatePath) {
        File msfx = new File(myStatePath);
        if (msfx.exists())
            return loadXml(FileUtil.getFileAsString(msfx));
        else
            return new MyState();
    }

	private void saveXml() {
	    String ms = asXml();
	    FileUtil.saveToFile(myStatePath, ms);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            MyState.setHeadingInDegrees(45);
        }
        System.err.println(" " + (System.currentTimeMillis()-start));
	}
}
