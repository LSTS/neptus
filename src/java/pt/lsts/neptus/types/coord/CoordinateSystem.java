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
 * 2005/01/15
 */
package pt.lsts.neptus.types.coord;

import java.util.StringTokenizer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * The base for the definition of this coordinate system
 * is the N-E-D. That is x pointing to north, y to east 
 * and z down. The origin of the default coordinate system
 * is N0 E0 with height 0 meters. The axis are in degrees.
 * 
 * @see pt.lsts.neptus.types.coord.CoordAxisUtil
 * @version 0.3   2005-03-09
 * @author Paulo Dias
 */
@SuppressWarnings("serial")
public class CoordinateSystem extends LocationType { //implements XmlOutputMethods {
    
    protected static final String DEFAULT_CS_NAME = "N0E0_NED";
    protected static final String DEFAULT_CS_ID   = "N0E0_NED";
    
    public static final short MIN_DIRECTION     = -1;
    public static final short UNKNOWN_DIRECTION = -1;
    public static final short NORTH_DIRECTION   =  0;
    public static final short SOUTH_DIRECTION   =  1;
    public static final short EAST_DIRECTION    =  2;
    public static final short WEST_DIRECTION    =  3;
    public static final short UP_DIRECTION      =  4;
    public static final short DOWN_DIRECTION    =  5;
    public static final short MAX_DIRECTION     =  5;

    
    public static final String UNKNOWN_DIRECTION_STRING = "unknown";
    public static final String NORTH_DIRECTION_STRING   = "north";
    public static final String SOUTH_DIRECTION_STRING   = "south";
    public static final String EAST_DIRECTION_STRING    = "east";
    public static final String WEST_DIRECTION_STRING    = "west";
    public static final String UP_DIRECTION_STRING      = "up";
    public static final String DOWN_DIRECTION_STRING    = "down";
    
    
    protected static final String DEFAULT_ROOT_ELEMENT = "coordinate-system-def";
    
    //axis-attitude
    //The base for the definition of this coordinate system
    //is the N-E-D. That is x pointing to north, y to east 
    //and z down. The origin of the default coordinate system
    //is N0 E0 with height 0 meters.
    protected boolean anglesUsed = true; //indicates if (roll,pitch,yaw) is used, or the other form
    protected double roll  = 0;
    protected double pitch = 0;
    protected double yaw   = 0;
    //Only one will be used but they should be synchronized
    protected short xAxisDirection = NORTH_DIRECTION;
    protected short yAxisDirection = EAST_DIRECTION;
    protected short zAxisDirection = DOWN_DIRECTION;
    
    /**
     * 
     */
    public CoordinateSystem() {
        super();
    }

    /**
     * @param xml
     */
    public CoordinateSystem(String xml) {
        // super();
        load(xml);
    }

    /**
     * @param anotherCS
     */
    public void setCoordinateSystem(CoordinateSystem anotherCS) {
        setLocation(anotherCS);
        setRoll(anotherCS.getRoll());
        setPitch(anotherCS.getPitch());
        setYaw(anotherCS.getYaw());
    }
    
    /**
     * @param xml
     * @return
     */
    public boolean load(Document doc) {
        try {
            boolean res = super.load(doc);
            if (!res)
                throw new DocumentException();

            Node nd = doc.selectSingleNode("//axis-attitude/roll");
            if (nd != null) {
                this.setRoll(Double.parseDouble(nd.getText()), true);
                nd = doc.selectSingleNode("//axis-attitude/pitch");
                if (nd != null)
                    this.setPitch(Double.parseDouble(nd.getText()));
                nd = doc.selectSingleNode("//axis-attitude/yaw");
                if (nd != null)
                    this.setYaw(Double.parseDouble(nd.getText()));
            }
            else {
                nd = doc.selectSingleNode("//axis-attitude/x-axis-direction");
                if (nd != null) {
                    short[] dir = new short[3];
                    dir[0] = CoordAxisUtil.parseDirection(nd.getText());
                    nd = doc.selectSingleNode("//axis-attitude/y-axis-direction");
                    dir[1] = CoordAxisUtil.parseDirection(nd.getText());
                    nd = doc.selectSingleNode("//axis-attitude/z-axis-direction");
                    dir[2] = CoordAxisUtil.parseDirection(nd.getText());
                    boolean tr = setAxisDirections(dir[0], dir[1], dir[2], true);
                    if (tr == false)
                        return false;
                }
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(e.getMessage(), e);
            return false;
        }
        return true;
    }
    
//    /**
//     * @return Returns the label.
//     */
//    public String getId() {
//        return id;
//    }
//
//    /**
//     * @param label
//     *            The label to set.
//     */
//    public void setId(String label) {
//        this.id = label;
//    }
//
//    /**
//     * @return Returns the name.
//     */
//    public String getName() {
//        return name;
//    }
//
//    /**
//     * @param name
//     *            The name to set.
//     */
//    public void setName(String name) {
//        this.name = name;
//    }

    /**
     * @param xAxisDirection
     * @param yAxisDirection
     * @param zAxisDirection
     * @param useDirectionsInXMLOutput
     *            updates the {@link #isAnglesUsed()}.
     * @return
     */
    private boolean setAxisDirections(short xAxisDirection, short yAxisDirection,
            short zAxisDirection, boolean useDirectionsInXMLOutput) {
        boolean res = setAxisDirections(xAxisDirection, yAxisDirection, zAxisDirection);
        if (res == true)
            setAnglesUsed(!useDirectionsInXMLOutput);
        return res;
    }

    /**
     * @param xAxisDirection
     * @param yAxisDirection
     * @param zAxisDirection
     * @return
     */
    private boolean setAxisDirections(short xAxisDirection, short yAxisDirection,
            short zAxisDirection) {
        if ((xAxisDirection < MIN_DIRECTION) & (xAxisDirection > MAX_DIRECTION))
            return false;
        if ((yAxisDirection < MIN_DIRECTION) & (yAxisDirection > MAX_DIRECTION))
            return false;
        if ((zAxisDirection < MIN_DIRECTION) & (zAxisDirection > MAX_DIRECTION))
            return false;

        double[] ang = CoordAxisUtil.getAxisAngles(xAxisDirection, yAxisDirection, zAxisDirection);
        if (ang == null)
            return false;

        setXAxisDirection(xAxisDirection);
        setYAxisDirection(yAxisDirection);
        setZAxisDirection(zAxisDirection);

        setRoll(ang[0]);
        setPitch(ang[1]);
        setYaw(ang[2]);

        return true;
    }

    /**
     * 
     */
    private void updateAxisDirections() {
        short[] dir = CoordAxisUtil.getAxisDirections(this.roll, this.pitch, this.yaw);
        setXAxisDirection(dir[0]);
        setYAxisDirection(dir[1]);
        setZAxisDirection(dir[2]);
    }

    /**
     * @return Returns the roll.
     */
    public double getRoll() {
        return roll;
    }

    /**
     * @param roll
     *            The roll to set.
     */
    public void setRoll(double rollDegrees) {
        this.roll = rollDegrees;
        updateAxisDirections();
    }

    /**
     * @param roll
     * @param useAnglesInXMLOutput
     *            updates the {@link #isAnglesUsed()}.
     */
    public void setRoll(double rollDegrees, boolean useAnglesInXMLOutput) {
        setRoll(rollDegrees);
        setAnglesUsed(useAnglesInXMLOutput);
    }

    /**
     * @return Returns the pitch.
     */
    public double getPitch() {
        return pitch;
    }

    /**
     * @param pitch
     *            The pitch to set.
     */
    public void setPitch(double pitchDegrees) {
        this.pitch = pitchDegrees;
        updateAxisDirections();
    }

    /**
     * @param pitch
     * @param useAnglesInXMLOutput
     *            updates the {@link #isAnglesUsed()}.
     */
    public void setPitch(double pitchDegrees, boolean useAnglesInXMLOutput) {
        setPitch(pitchDegrees);
        setAnglesUsed(useAnglesInXMLOutput);
    }

    /**
     * @return Returns the yaw.
     */
    public double getYaw() {
        return yaw;
    }

    /**
     * @param yaw
     *            The yaw to set.
     */
    public void setYaw(double yawDegrees) {
        this.yaw = yawDegrees;
        updateAxisDirections();
    }

    /**
     * @param yaw
     * @param useAnglesInXMLOutput
     *            updates the {@link #isAnglesUsed()}.
     */
    public void setYaw(double yawDegrees, boolean useAnglesInXMLOutput) {
        setYaw(yawDegrees);
        setAnglesUsed(useAnglesInXMLOutput);
    }

    /**
     * @return Returns the xAxisDirection.
     */
    private short getXAxisDirection() {
        return xAxisDirection;
    }

    /**
     * @param axisDirection
     *            The xAxisDirection to set.
     */
    private void setXAxisDirection(short axisDirection) {
        xAxisDirection = axisDirection;
    }

    /**
     * @return Returns the yAxisDirection.
     */
    private short getYAxisDirection() {
        return yAxisDirection;
    }

    /**
     * @param axisDirection
     *            The yAxisDirection to set.
     */
    private void setYAxisDirection(short axisDirection) {
        yAxisDirection = axisDirection;
    }

    /**
     * @return Returns the zAxisDirection.
     */
    private short getZAxisDirection() {
        return zAxisDirection;
    }

    /**
     * @param axisDirection
     *            The zAxisDirection to set.
     */
    private void setZAxisDirection(short axisDirection) {
        zAxisDirection = axisDirection;
    }

    /**
     * @return Returns the anglesUsed.
     */
    public boolean isAnglesUsed() {
        return anglesUsed;
    }

    /**
     * @param anglesUsed
     *            The anglesUsed to set.
     */
    public void setAnglesUsed(boolean anglesUsed) {
        this.anglesUsed = anglesUsed;
    }

    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("id").addText(getId());
        root.addElement("name").addText(getName());

        Element origin = root.addElement("origin");

        origin.addElement("latitude").addText(getLatitudeStr());
        origin.addElement("longitude").addText(getLongitudeStr());

        origin.addElement("depth").addText(String.valueOf(getDepth()));

        if (getOffsetDistance() != 0d) {
            origin.addElement("azimuth").addText(String.valueOf(getAzimuth()));
            origin.addElement("offset-distance").addText(String.valueOf(getOffsetDistance()));
            origin.addElement("zenith").addText(String.valueOf(getZenith()));
        }

        if (!((getOffsetNorth() == 0) && (getOffsetEast() == 0) && (getOffsetUp() == 0))) {
            if (isOffsetNorthUsed())
                origin.addElement("offset-north").addText(String.valueOf(getOffsetNorth()));
            else
                origin.addElement("offset-south").addText(String.valueOf(getOffsetSouth()));

            if (isOffsetEastUsed())
                origin.addElement("offset-east").addText(String.valueOf(getOffsetEast()));
            else
                origin.addElement("offset-west").addText(String.valueOf(getOffsetWest()));

            if (isOffsetUpUsed())
                origin.addElement("offset-up").addText(String.valueOf(getOffsetUp()));
            else
                origin.addElement("offset-down").addText(String.valueOf(getOffsetDown()));
        }

        if (!((getRoll() == 0) & (getPitch() == 0) & (getYaw() == 0))) {
            Element axisAttitude = root.addElement("axis-attitude");

            boolean teb = isAnglesUsed();
            if (!teb) {
                if ((getXAxisDirection() == UNKNOWN_DIRECTION)
                        || (getYAxisDirection() == UNKNOWN_DIRECTION)
                        || (getZAxisDirection() == UNKNOWN_DIRECTION))
                    teb = true;
            }

            if (teb) {
                axisAttitude.addElement("roll").addText(
                        String.valueOf(MathMiscUtils.round(getRoll(), 2)));
                axisAttitude.addElement("pitch").addText(
                        String.valueOf(MathMiscUtils.round(getPitch(), 2)));
                axisAttitude.addElement("yaw").addText(
                        String.valueOf(MathMiscUtils.round(getYaw(), 2)));
            }
            else {
                axisAttitude.addElement("x-axis-direction").addText(
                        CoordAxisUtil.getDirectionAsString(getXAxisDirection()));
                axisAttitude.addElement("y-axis-direction").addText(
                        CoordAxisUtil.getDirectionAsString(getYAxisDirection()));
                axisAttitude.addElement("z-axis-direction").addText(
                        CoordAxisUtil.getDirectionAsString(getZAxisDirection()));
            }
        }
        return document;
    }

    /**
     * @return
     */
    public static CoordinateSystem createDefaultCoordinateSystem() {
        CoordinateSystem cs = new CoordinateSystem();
        cs.setId(DEFAULT_CS_ID);
        cs.setName(DEFAULT_CS_NAME);
        return cs;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.coord.LocationType#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
    	if (!(obj instanceof CoordinateSystem))
    		return false;
    	CoordinateSystem otherCS = (CoordinateSystem) obj;
    	if (!(otherCS.getDistanceInMeters(new LocationType(this)) == 0))
    		return false;
    	if (otherCS.getRoll() != getRoll())
    		return false;
    	if (otherCS.getYaw() != getYaw())
    		return false;
    	if (otherCS.getPitch() != getPitch())
    		return false;
    	
    	return true;
    }
    
    @Override
    public String toString() {
    	String ang = "   ::   ";
    	if (isAnglesUsed()) {
    		ang += "roll=" +getRoll()+
    		     "\u00B0 pitch="+getPitch()+
    		     "\u00B0 yaw="+getYaw()+"\u00B0";
    	}
    	else {
    		ang += "x-dir=" +getXAxisDirection()+
		     "  y-dir="+getYAxisDirection()+
		     "  z-dir="+getZAxisDirection();
    	}
    	return super.toString() + ang;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.coord.LocationType#getNewAbsoluteLatLonDepth()
     */
    @SuppressWarnings("unchecked")
    @Override
    public <L extends LocationType> L getNewAbsoluteLatLonDepth() {
        L ret = super.getNewAbsoluteLatLonDepth();
        CoordinateSystem coord;
        if (ret instanceof CoordinateSystem) {
            coord = (CoordinateSystem) ret;
        }
        else {
            coord = new CoordinateSystem();
            coord.setLocation(ret);
        }
        coord.anglesUsed = this.anglesUsed;
        coord.roll  = this.roll;
        coord.pitch  = this.pitch;
        coord.yaw  = this.yaw;
        coord.xAxisDirection  = this.xAxisDirection;
        coord.yAxisDirection  = this.yAxisDirection;
        coord.zAxisDirection  = this.zAxisDirection;
        ret = (L) coord;
        return ret;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.coord.LocationType#getClipboardText()
     */
    @Override
    public String getClipboardText() {
        return super.getClipboardText() + getRoll() + " " + getPitch() + " " + getYaw() + "\n";
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.coord.LocationType#fromClipboardText(java.lang.String)
     */
    @Override
    public boolean fromClipboardText(String text) {
        boolean ret = super.fromClipboardText(text);
        if (!ret)
            return ret;
        StringTokenizer st = new StringTokenizer(text);
        if (st.countTokens() == 11) {
            setAnglesUsed(false);
            setRoll(0);
            setPitch(0);
            setYaw(0);
        }
        else if (st.countTokens() >= 14) {
            for (int i = 0; i < 11; i++)
                st.nextToken();
            setRoll(Double.parseDouble(st.nextToken()));
            setPitch(Double.parseDouble(st.nextToken()));
            setYaw(Double.parseDouble(st.nextToken()));
        }
        else
            ret = false;
        
        return ret;
    }
    
    public static void main(String[] args) {
        CoordinateSystem c1 = new CoordinateSystem();
        CoordinateSystem c2 = c1.getNewAbsoluteLatLonDepth();
        NeptusLog.pub().info("<###> "+c1);
        NeptusLog.pub().info("<###> "+c2);
        
        LocationType l1 = new LocationType();
        LocationType l2 = l1.getNewAbsoluteLatLonDepth();
        LocationType l3 = c1.getNewAbsoluteLatLonDepth();
        NeptusLog.pub().info("<###> "+l1);
        NeptusLog.pub().info("<###> "+l2);
        NeptusLog.pub().info("<###> "+l3);
        try {
            c2 = l1.getNewAbsoluteLatLonDepth();
            NeptusLog.pub().info("<###> "+l1);
            NeptusLog.pub().info("<###> "+c2);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        l1.translatePosition(100, 50, 2);
        String clp = l1.getClipboardText();
        CoordinateSystem cc1 = new CoordinateSystem();
        cc1.fromClipboardText(clp);
        LocationType ll1 = new LocationType();
        ll1.fromClipboardText(clp);
        CoordinateSystem cc2 = new CoordinateSystem();
        cc2.fromClipboardText(cc1.getClipboardText());
        LocationType ll2 = new LocationType();
        ll2.fromClipboardText(cc2.getClipboardText());
        NeptusLog.pub().info("<###> "+l1);
        NeptusLog.pub().info("<###> "+cc1);
        NeptusLog.pub().info("<###> "+ll1);
        NeptusLog.pub().info("<###> "+cc2);
        NeptusLog.pub().info("<###> "+ll2);
        
        
        NeptusLog.pub().info("<###> "+l1.asXML());        
        NeptusLog.pub().info("<###> "+c1.asXML());
    }
}
