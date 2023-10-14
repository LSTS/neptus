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
 * Author: Paulo Dias
 * 2010/05/02
 */
package pt.lsts.neptus.mystate;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
public class MyState {

	private static String myStatePath = "./conf/mystate.xml";

    private static final MyState instance = loadFromXmlFile(myStatePath);
	
    @XmlElement
	private CoordinateSystem location = new CoordinateSystem();	
    @XmlElement
    private double length = 0; 
    @XmlElement
    private double width = 0;
    @XmlElement
    private double widthOffsetFromCenter = 0;
    @XmlElement
    private double lengthOffsetFromCenter = 0;
    @XmlElement
    private double draught = 0;

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
        instance.triggerChange();
	}

	public static void setLocationAndAxis(LocationType location, double roll,
			double pitch, double yaw) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(roll);
		instance.location.setPitch(pitch);
		instance.location.setYaw(yaw);
        instance.triggerChange();
	}

	public static void setLocationAndAxisRadians(LocationType location, double rollRadians,
			double pitchRadians, double yawRadians) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(Math.toDegrees(rollRadians));
		instance.location.setPitch(Math.toDegrees(pitchRadians));
		instance.location.setYaw(Math.toDegrees(yawRadians));
        instance.triggerChange();
	}

	public static void setLocationAndAxis(LocationType location, double yaw) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(0);
		instance.location.setPitch(0);
		instance.location.setYaw(yaw);
        instance.triggerChange();
	}

	public static void setLocationAndAxisRadians(LocationType location, double yawRadians) {
		instance.location.setLocation(location);
        instance.lastLocationUpdateTimeMillis = System.currentTimeMillis();
		instance.location.setRoll(0);
		instance.location.setPitch(0);
		instance.location.setYaw(Math.toDegrees(yawRadians));
        instance.triggerChange();
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
        instance.triggerChange();
	}

	public static double getHeadingInDegrees() {
	    return instance.location.getYaw();
	}

	public static void setHeadingInDegrees(double yawDegrees) {
	    instance.location.setYaw(yawDegrees);
	    instance.triggerChange();
	}

	/**
     * @return the length
     */
    public static double getLength() {
        return instance.length;
    }
    
    /**
     * @param length the length to set
     */
    public static void setLength(double length) {
        instance.length = length < 0 ? 0 : length;
        instance.triggerChange();
    }

    /**
     * @return the width
     */
    public static double getWidth() {
        return instance.width;
    }
    
    /**
     * @param width the width to set
     */
    public static void setWidth(double width) {
        instance.width = width < 0 ? 0 : width;
        instance.triggerChange();
    }
    
    
    /**
     * @return the lengthOffsetFromCenter
     */
    public static double getLengthOffsetFromCenter() {
        return instance.lengthOffsetFromCenter;
    }
    
    /**
     * @param lengthOffsetFromCenter the lengthOffsetFromCenter to set
     */
    public static void setLengthOffsetFromCenter(double lengthOffsetFromCenter) {
        instance.lengthOffsetFromCenter = lengthOffsetFromCenter < 0 ? 0 : lengthOffsetFromCenter;
        instance.triggerChange();
    }
    
    /**
     * @return the widthOffsetFromCenter
     */
    public static double getWidthOffsetFromCenter() {
        return instance.widthOffsetFromCenter;
    }

    /**
     * @param widthOffsetFromCenter the widthOffsetFromCenter to set
     */
    public static void setWidthOffsetFromCenter(double widthOffsetFromCenter) {
        instance.widthOffsetFromCenter = widthOffsetFromCenter < 0 ? 0 : widthOffsetFromCenter;
        instance.triggerChange();
    }
    
    /**
     * @return the draught
     */
    public static double getDraught() {
        return instance.draught;
    }
    
    /**
     * @param draught the draught to set
     */
    public static void setDraught(double draught) {
        instance.draught = draught < 0 ? 0 : draught;
        instance.triggerChange();
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
        if (msfx.exists()) {
            try {
                return loadXml(FileUtil.getFileAsString(msfx));
            }
            catch (Exception e) {
                NeptusLog.pub().warn("Problem loading MyState from file. Reverting to default.", e);
                return new MyState();
            }
        }
        else {
            return new MyState();
        }
    }

	private void triggerChange() {
	    logEstimatedState();
	    saveXml();
	}

    private void logEstimatedState() {
        LocationType loc = MyState.getLocation().getNewAbsoluteLatLonDepth();
        EstimatedState es = new EstimatedState(loc.getLatitudeRads(), loc.getLongitudeRads(), (float) loc.getHeight(),
                0f, 0f, 0f, 0f, 0f, (float) MyState.getHeadingInRadians(), 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        es.setSrc(GeneralPreferences.imcCcuId.intValue());
        es.setTimestampMillis(MyState.getLastLocationUpdateTimeMillis());
        LsfMessageLogger.log(es);
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
