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
 * Author: Paulo Dias, Ze Pinto
 * 2005/03/05
 */
package pt.lsts.neptus.types.coord;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.StringTokenizer;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * The base for the definition of this coordinate system
 * is the N-E-D. That is x pointing to north, y to east 
 * and z down. The origin of the default coordinate system
 * is N0 E0 with height 0 meters.
 * 
 * 
 * <b>Important note: </b> you should always implement all the methods in the
 * interface {@link pt.lsts.neptus.types.XmlOutputMethods} and the
 * variable {@link #DEFAULT_ROOT_ELEMENT}. If not the root element of the output
 * XML will be the one of the parent class.<br/>
 * You should also implement the constructors and the {@link #load(String)} to
 * perfect results. It can be something like this: <code><br/>
 * &nbsp;&nbsp;public boolean load (String xml)<br/>
 * &nbsp;&nbsp;{<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;try<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;{<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;boolean res = super.load(xml);<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;if (!res)<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;throw new DocumentException();<br/>
 * <br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;} catch (DocumentException e)<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;{<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;NeptusLog.pub().error(e.getMessage(), e);<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;return false;<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;}<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;return true;<br/>
 * &nbsp;&nbsp;}<br/>
 * </code>
 * 
 * @version 1.0 2005-03-09
 * @version 1.1 2005-05-18
 * @version 2.0 2011-10-03 Merge LocationType and AbstractLocationPoint
 * @author Paulo Dias
 * @author ZP
 */
public class LocationType implements XmlOutputMethods, Serializable, Comparable<LocationType>, Cloneable {

    /**
     * 13/12/2010 - pdias - Added Serializable interface for S57Map plugin
     */
    private static final long serialVersionUID = -5672043713440034944L;

    protected static final String DEFAULT_ROOT_ELEMENT = "coordinate";

    public static final LocationType ABSOLUTE_ZERO = new LocationType();

    public static final LocationType FEUP = new LocationType(41.17785, -8.59796);
    
    public static double ONE_LAT_DEGREE = 0;
    static {
        ABSOLUTE_ZERO.setId("ABSOLUTE_ZERO");
        LocationType lt = new LocationType();
        lt.setLatitudeDegs(1);
        ONE_LAT_DEGREE = lt.getDistanceInMeters(ABSOLUTE_ZERO);
    }

    private static NumberFormat nf2 = GuiUtils.getNeptusDecimalFormat(2);

    protected String id = NameNormalizer.getRandomID();
    protected String name = id;

    protected double latitudeRads = 0;
    protected double longitudeRads = 0;

    private double depth = 0;

    // spherical coordinates in degrees (º)
    protected double offsetDistance = 0;
    protected double azimuth = 0;
    protected double zenith = 90;

    // offsets are in meters (m)
    private boolean isOffsetNorthUsed = true;
    private double offsetNorth = 0;
    private boolean isOffsetEastUsed = true;
    private double offsetEast = 0;
    private boolean isOffsetUpUsed = true;
    private double offsetDown = 0;

    public LocationType() {
        super();
    }

    /**
     * @param xml
     */
    public LocationType(String xml) {
        super();
        load(xml);
    }

    /**
     * @param anotherLocation
     */
    public LocationType(LocationType anotherLocation) {
        super();
        setLocation(anotherLocation);
    }

    public LocationType(double latitudeDegrees, double longitudeDegrees) {
        this.latitudeRads = Math.toRadians(latitudeDegrees);
        this.longitudeRads = Math.toRadians(longitudeDegrees);
    }

    public boolean load(Element elem) {
        Document doc = Dom4JUtil.elementToDocument(elem);
        if (doc == null)
            return false;
        return load(doc);
    }

    public boolean load(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            return load(doc);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    /**
     * @param xml
     * @return
     */
    public boolean load(Document doc) {

        Node node, nd;

        node = doc.selectSingleNode("//id");
        if (node != null)
            setId(node.getText());

        node = doc.selectSingleNode("//name");
        if (node != null)
            this.setName(node.getText());

        this.setLatitudeStr(doc.selectSingleNode("//latitude").getText());
        this.setLongitudeStr(doc.selectSingleNode("//longitude").getText());

        nd = doc.selectSingleNode("//height");
        if (nd != null)
            this.setHeight(Double.parseDouble(nd.getText()));
        else {
            nd = doc.selectSingleNode("//depth");
            if (nd != null) {
                this.setDepth(Double.parseDouble(nd.getText()));         
            }
        }

        nd = doc.selectSingleNode("//azimuth");
        if (nd != null)
            this.setAzimuth(Double.parseDouble(nd.getText()));
        nd = doc.selectSingleNode("//offset-distance");
        if (nd != null)
            this.setOffsetDistance(Double.parseDouble(nd.getText()));
        nd = doc.selectSingleNode("//zenith");
        if (nd != null)
            this.setZenith(Double.parseDouble(nd.getText()));

        nd = doc.selectSingleNode("//offset-north");
        if (nd != null)
            this.setOffsetNorth(Double.parseDouble(nd.getText()), true);
        else {
            nd = doc.selectSingleNode("//offset-south");
            if (nd != null)
                this.setOffsetSouth(Double.parseDouble(nd.getText()), true);
        }
        nd = doc.selectSingleNode("//offset-east");
        if (nd != null)
            this.setOffsetEast(Double.parseDouble(nd.getText()), true);
        else {
            nd = doc.selectSingleNode("//offset-west");
            if (nd != null)
                this.setOffsetWest(Double.parseDouble(nd.getText()), true);
        }
        nd = doc.selectSingleNode("//offset-up");
        if (nd != null)
            this.setOffsetUp(Double.parseDouble(nd.getText()), true);
        else {
            nd = doc.selectSingleNode("//offset-down");
            if (nd != null)
                this.setOffsetDown(Double.parseDouble(nd.getText()), true);
        }
        return true;
    }

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param label
     *            The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the latitude.
     */
    public String getLatitudeStr() {
        //return CoordinateUtil.dmsToLatString(CoordinateUtil.decimalDegreesToDMS(Math.toDegrees(latitudeRads)));
        return CoordinateUtil.latitudeAsPrettyString(getLatitudeDegs());
    }

    /**
     * @return in decimal degrees
     */
    public double getLatitudeDegs() {
        return Math.toDegrees(latitudeRads);
    }

    /**
     * @return
     */
    public double getLatitudeRads() {
        return latitudeRads;
    }

    /**
     * @param latitude
     *            The latitude to set.
     */
    public void setLatitudeStr(String latitude) {
        if (latitude != null)
            setLatitudeDegs(CoordinateUtil.parseCoordString(latitude));
    }

    /**
     * @param latitude
     *            The latitude to set in decimal degrees.
     */
    public void setLatitudeDegs(double latitude) {
        setLatitudeRads(Math.toRadians(latitude));        
    }

    /**
     * @param latitudeRads
     *            The latitude to set in radians.
     */
    public void setLatitudeRads(double latitudeRads) {
        this.latitudeRads = latitudeRads;
    }

    /**
     * @return Returns the longitude.
     */
    public String getLongitudeStr() {
        // return CoordinateUtil.dmsToLonString(CoordinateUtil.decimalDegreesToDMS(getLongitudeDegs()));
        return CoordinateUtil.longitudeAsPrettyString(getLongitudeDegs());
    }

    /**
     * @return in decimal degrees
     */
    public double getLongitudeDegs() {
        return Math.toDegrees(this.longitudeRads);
    }

    /**
     * @return
     */
    public double getLongitudeRads() {
        return longitudeRads;
    }

    /**
     * @param longitude
     *            The longitude to set.
     */
    public void setLongitudeStr(String longitude) {
        if (longitude != null)
            setLongitudeDegs(CoordinateUtil.parseCoordString(longitude));
    }

    /**
     * @param longitude
     *            The longitude to set in decimal degrees.
     */
    public void setLongitudeDegs(double longitude) {
        setLongitudeRads(Math.toRadians(longitude));
    }

    /**
     * @param longitudeRads
     *            The longitude to set in radians.
     */
    public void setLongitudeRads(double longitudeRads) {
        this.longitudeRads = longitudeRads;
    }

    /**
     * @return Returns the z value. 
     * @see #getZUnits()
     */
    public double getDepth() {
        if (depth == 0)
            return 0;
        return depth;
    }

    /**
     * @param depth The value for depth
     */
    public void setDepth(double depth) {
        this.depth = depth;
    }

    /**
     * @return Returns the height.
     */
    public double getHeight() {
        return -getDepth();
    }

    /**
     * @param height
     *            The height to set.
     */
    public void setHeight(double height) {
        this.depth = -height;
    }

    /**
     * @return Returns the offsetDistance.
     */
    public double getOffsetDistance() {
        return offsetDistance;
    }

    /**
     * @param offsetDistance
     *            The offsetDistance to set.
     */
    public void setOffsetDistance(double offsetDistance) {
        this.offsetDistance = offsetDistance;
    }

    /**
     * @return Returns the azimuth.
     */
    public double getAzimuth() {
        return azimuth;
    }

    /**
     * @param azimuth
     *            The azimuth to set.
     */
    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    /**
     * @return Returns the zenith.
     */
    public double getZenith() {
        return zenith;
    }

    /**
     * @param zenith
     *            The zenith to set.
     */
    public void setZenith(double zenith) {
        this.zenith = zenith;
    }

    /**
     * @return Returns the offsetNorth.
     */
    public double getOffsetNorth() {
        return offsetNorth;
    }

    /**
     * @param offsetNorth
     *            The offsetNorth to set.
     */
    public void setOffsetNorth(double offsetNorth) {
        this.offsetNorth = offsetNorth;
    }

    /**
     * @param offsetNorth
     *            The offsetNorth to set.
     * @param useOffsetNorthInXMLOutput
     *            updates the {@link #isOffsetNorthUsed()}.
     */
    public void setOffsetNorth(double offsetNorth, boolean useOffsetNorthInXMLOutput) {
        setOffsetNorth(offsetNorth);
        setOffsetNorthUsed(useOffsetNorthInXMLOutput);
    }

    /**
     * @return Returns the offsetSouth.
     */
    public double getOffsetSouth() {
        if (this.offsetNorth == 0)
            return offsetNorth;
        else
            return -offsetNorth;
    }

    /**
     * @param offsetSouth
     *            The offsetSouth to set.
     */
    public void setOffsetSouth(double offsetSouth) {
        this.offsetNorth = -offsetSouth;
        if (this.offsetNorth == 0)
            this.offsetNorth = 0;
    }

    /**
     * @param offsetNorth
     *            The offsetNorth to set.
     * @param useOffsetSouthInXMLOutput
     *            updates the {@link #isOffsetNorthUsed()}.
     */
    public void setOffsetSouth(double offsetSouth, boolean useOffsetSouthInXMLOutput) {
        setOffsetSouth(offsetSouth);
        setOffsetNorthUsed(!useOffsetSouthInXMLOutput);
    }

    /**
     * @return Returns the offsetEast.
     */
    public double getOffsetEast() {
        return offsetEast;
    }

    /**
     * @param offsetEast
     *            The offsetEast to set.
     */
    public void setOffsetEast(double offsetEast) {
        this.offsetEast = offsetEast;
    }

    /**
     * @param offsetEast
     *            The offsetEast to set.
     * @param useOffsetEastInXMLOutput
     *            updates the {@link #isOffsetEastUsed()}.
     */
    public void setOffsetEast(double offsetEast, boolean useOffsetEastInXMLOutput) {
        setOffsetEast(offsetEast);
        setOffsetEastUsed(useOffsetEastInXMLOutput);
    }

    /**
     * @return Returns the offsetWest.
     */
    public double getOffsetWest() {
        if (this.offsetEast == 0)
            return offsetEast;
        else
            return -offsetEast;
    }

    /**
     * @param offsetWest
     *            The offsetWest to set.
     */
    public void setOffsetWest(double offsetWest) {
        this.offsetEast = -offsetWest;
        if (this.offsetEast == 0)
            this.offsetEast = 0;
    }

    /**
     * @param offsetWest
     *            The offsetWest to set.
     * @param useOffsetWestInXMLOutput
     *            updates the {@link #isOffsetEastUsed()}.
     */
    public void setOffsetWest(double offsetWest, boolean useOffsetWestInXMLOutput) {
        setOffsetWest(offsetWest);
        setOffsetEastUsed(!useOffsetWestInXMLOutput);
    }

    /**
     * @return Returns the offsetUp.
     */
    public double getOffsetUp() {
        if (this.offsetDown == 0)
            return this.offsetDown;
        else
            return -this.offsetDown;
    }

    /**
     * @param offsetUp
     *            The offsetUp to set.
     */
    public void setOffsetUp(double offsetUp) {
        this.offsetDown = -offsetUp;
        if (this.offsetDown == 0)
            this.offsetDown = 0;
    }

    /**
     * @param offsetUp
     *            The offsetUp to set.
     * @param useOffsetUpInXMLOutput
     *            updates the {@link #isOffsetUpUsed()}.
     */
    public void setOffsetUp(double offsetUp, boolean useOffsetUpInXMLOutput) {
        setOffsetUp(offsetUp);
        setOffsetUpUsed(useOffsetUpInXMLOutput);
    }

    /**
     * @return Returns the offsetDown.
     */
    public double getOffsetDown() {
        return offsetDown;
    }

    /**
     * @param offsetDown
     *            The offsetDown to set.
     */
    public void setOffsetDown(double offsetDown) {
        this.offsetDown = offsetDown;
    }

    /**
     * @param offsetDown
     *            The offsetDown to set.
     * @param useOffsetDownInXMLOutput
     *            updates the {@link #isOffsetUpUsed()}.
     */
    public void setOffsetDown(double offsetDown, boolean useOffsetDownInXMLOutput) {
        setOffsetDown(offsetDown);
        setOffsetUpUsed(!useOffsetDownInXMLOutput);
    }

    /**
     * @return Returns the isOffsetEastUsed.
     */
    public boolean isOffsetEastUsed() {
        return isOffsetEastUsed;
    }

    /**
     * @param isOffsetEastUsed
     *            The isOffsetEastUsed to set.
     */
    public void setOffsetEastUsed(boolean isOffsetEastUsed) {
        this.isOffsetEastUsed = isOffsetEastUsed;
    }

    /**
     * @return Returns the isOffsetNorthUsed.
     */
    public boolean isOffsetNorthUsed() {
        return isOffsetNorthUsed;
    }

    /**
     * @param isOffsetNorthUsed
     *            The isOffsetNorthUsed to set.
     */
    public void setOffsetNorthUsed(boolean isOffsetNorthUsed) {
        this.isOffsetNorthUsed = isOffsetNorthUsed;
    }

    /**
     * @return Returns the isOffsetUpUsed.
     */
    public boolean isOffsetUpUsed() {
        return isOffsetUpUsed;
    }

    /**
     * @param isOffsetUpUsed
     *            The isOffsetUpUsed to set.
     */
    public void setOffsetUpUsed(boolean isOffsetUpUsed) {
        this.isOffsetUpUsed = isOffsetUpUsed;
    }

    @Override
    public String toString() {
        double[] absLoc = getAbsoluteLatLonDepth();

        // Any change to this, reflects the #valueOf method
        return CoordinateUtil.latitudeAsPrettyString(absLoc[0], GeneralPreferences.latLonPrefFormat) + ", "
                + CoordinateUtil.longitudeAsPrettyString(absLoc[1], GeneralPreferences.latLonPrefFormat) 
                + (getHeight() != 0  ? ", " + nf2.format(getHeight()) : "");
    }

    public static LocationType valueOf(String value) {
        return CoordinateUtil.parseLocation(value);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    @Override
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    @Override
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
    @Override
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
    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    @Override
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);

        root.addElement("id").addText(getId());
        root.addElement("name").addText(getName());

        Element coordinate = root.addElement("coordinate");

        if (getLatitudeStr() != null)
            coordinate.addElement("latitude").addText(String.valueOf(getLatitudeDegs()));

        if (getLongitudeStr() != null)
            coordinate.addElement("longitude").addText(String.valueOf(getLongitudeDegs()));

        coordinate.addElement("height").addText(String.valueOf(getHeight()));

        if (getOffsetDistance() != 0d) {
            coordinate.addElement("azimuth").addText(String.valueOf(getAzimuth()));
            coordinate.addElement("offset-distance").addText(String.valueOf(getOffsetDistance()));
            coordinate.addElement("zenith").addText(String.valueOf(getZenith()));
        }

        if (!((getOffsetNorth() == 0) && (getOffsetEast() == 0) && (getOffsetUp() == 0))) {
            if (isOffsetNorthUsed)
                coordinate.addElement("offset-north").addText(String.valueOf(getOffsetNorth()));
            else
                coordinate.addElement("offset-south").addText(String.valueOf(getOffsetSouth()));

            if (isOffsetEastUsed)
                coordinate.addElement("offset-east").addText(String.valueOf(getOffsetEast()));
            else
                coordinate.addElement("offset-west").addText(String.valueOf(getOffsetWest()));

            if (isOffsetUpUsed)
                coordinate.addElement("offset-up").addText(String.valueOf(getOffsetUp()));
            else
                coordinate.addElement("offset-down").addText(String.valueOf(getOffsetDown()));
        }

        return document;
    }

    /**
     * This method gives a vector from otherLocation to this location
     * 
     * @param otherLocation
     * @return
     */
    public double[] getOffsetFrom(LocationType otherLocation) {
        return CoordinateUtil.WGS84displacement(otherLocation, this);
    }

    /**
     * Returns the distance relative to other location, in meters
     * 
     * @param anotherLocation
     *            Another Location
     * @return The distance, in meters, to the location given
     */
    public double getDistanceInMeters(LocationType anotherLocation) {
        // NeptusLog.pub().info("<###>distance in meters from "+getLatitude()+","+getLongitude()+" to "+anotherLocation.getLatitude()+","+getLongitude());
        double[] offsets = getOffsetFrom(anotherLocation);
        double sum = offsets[0] * offsets[0] + offsets[1] * offsets[1] + offsets[2] * offsets[2];
        return Math.sqrt(sum);
    }

    /**
     * Returns the distance relative to other location, in meters
     * 
     * @param anotherLocation
     *            Another Location
     * @return The distance, in meters, to the location given
     */
    public double getHorizontalDistanceInMeters(LocationType anotherLocation) {
        double[] offsets = getOffsetFrom(anotherLocation);
        double sum = offsets[0] * offsets[0] + offsets[1] * offsets[1];
        return Math.sqrt(sum);
    }

    /**
     * @param anotherLocation
     * @return The angle to the other location, in radians
     */
    public double getXYAngle(LocationType anotherLocation) {
        double o2[] = anotherLocation.getOffsetFrom(this);
        double ang = AngleUtils.calcAngle(0, 0, o2[1], o2[0]);

        if (ang < 0)
            ang += Math.PI * 2;

        return ang;
    }

    /**
     * Copies the given location to this one. (Does not link them together.)
     * @param anotherPoint
     */
    public void setLocation(LocationType anotherPoint) {
        if (anotherPoint == null)
            return;

        this.setLatitudeRads(anotherPoint.getLatitudeRads());
        this.setLongitudeRads(anotherPoint.getLongitudeRads());
        this.setDepth(anotherPoint.getDepth());

        this.setAzimuth(anotherPoint.getAzimuth());
        this.setZenith(anotherPoint.getZenith());
        this.setOffsetDistance(anotherPoint.getOffsetDistance());

        this.setOffsetDown(anotherPoint.getOffsetDown());
        this.setOffsetEast(anotherPoint.getOffsetEast());
        this.setOffsetNorth(anotherPoint.getOffsetNorth());

        this.setOffsetEastUsed(anotherPoint.isOffsetEastUsed());
        this.setOffsetNorthUsed(anotherPoint.isOffsetNorthUsed());
        this.setOffsetUpUsed(anotherPoint.isOffsetUpUsed());
    }

    /**
     * Translate this location by the offsets.
     * @param offsetNorth
     * @param offsetEast
     * @param offsetDown
     * @return This location.
     */
    @SuppressWarnings("unchecked")
    public <L extends LocationType> L translatePosition(double offsetNorth, double offsetEast,
            double offsetDown) {

        setOffsetNorth(getOffsetNorth() + offsetNorth);
        setOffsetEast(getOffsetEast() + offsetEast);
        setOffsetDown(getOffsetDown() + offsetDown);

        return (L) this;
    }

    /**
     * This calls {@link #translatePosition(double, double, double)}.
     * @param nedOffsets
     * @return This location.
     */
    @SuppressWarnings("unchecked")
    public <L extends LocationType> L translatePosition(double[] nedOffsets) {
        if (nedOffsets.length < 3) {
            NeptusLog.pub().error("Invalid offsets length: found " + nedOffsets.length
                    + " values, expecting 3");
            return (L) this;
        }
        return translatePosition(nedOffsets[0], nedOffsets[1], nedOffsets[2]);
    }

    /**
     * 
     * @return The total Lat(degrees), Lon(degrees) and Depth(m)
     */
    public double[] getAbsoluteLatLonDepth() {
        double[] totalLatLonDepth = new double[] { 0d, 0d, 0d };
        totalLatLonDepth[0] = getLatitudeDegs();
        totalLatLonDepth[1] = getLongitudeDegs();
        totalLatLonDepth[2] = getDepth();

        double[] tmpDouble = CoordinateUtil.sphericalToCartesianCoordinates(getOffsetDistance(),
                getAzimuth(), getZenith());
        double north = getOffsetNorth() + tmpDouble[0];
        double east = getOffsetEast() + tmpDouble[1];
        double down = getOffsetDown() + tmpDouble[2];

        if (north != 0.0 || east != 0.0 || down != 0.0)
            return CoordinateUtil.WGS84displace(totalLatLonDepth[0],totalLatLonDepth[1], totalLatLonDepth[2], north, east, down);
        else
            return totalLatLonDepth;
    }

    public String getLatitudeAsPrettyString() {
        return CoordinateUtil.latitudeAsPrettyString(getLatitudeDegs());
    }
    
    public String getLatitudeAsPrettyString(LatLonFormatEnum format) {
        return CoordinateUtil.latitudeAsPrettyString(getLatitudeDegs(), format);
    }

    public String getLongitudeAsPrettyString() {
        return CoordinateUtil.longitudeAsPrettyString(getLongitudeDegs());
    }

    public String getLongitudeAsPrettyString(LatLonFormatEnum format) {
        return CoordinateUtil.longitudeAsPrettyString(getLongitudeDegs(), format);
    }

    /**
     * Converts this Location to absolute (Lat/Lon/Depth without offsets). 
     * @return The Location itself. 
     */
    @SuppressWarnings("unchecked")
    public <L extends LocationType> L convertToAbsoluteLatLonDepth() {
        if (offsetNorth == 0 && offsetEast == 0 && offsetDown == 0 && offsetDistance == 0) {
            return (L) this;
        }
        
        double latlondepth[] = getAbsoluteLatLonDepth();

        setLocation(ABSOLUTE_ZERO);
        setLatitudeDegs(latlondepth[0]);
        setLongitudeDegs(latlondepth[1]);
        setDepth(latlondepth[2]);

        return (L) this;
    }

    /**
     * Converts a copy of this Location to absolute (Lat/Lon/Depth without offsets). 
     * @return A copy of the location.
     */
    @SuppressWarnings("unchecked")
    public <L extends LocationType> L getNewAbsoluteLatLonDepth() {
        double latlondepth[] = getAbsoluteLatLonDepth();
        L loc;
        try {
            loc = (L) this.getClass().getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            loc = (L) new LocationType();
        }
        loc.setLatitudeDegs(latlondepth[0]);
        loc.setLongitudeDegs(latlondepth[1]);
        loc.setDepth(latlondepth[2]);

        return loc;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return getNewAbsoluteLatLonDepth();
    }

    /**
     * @return The total North(m), East(m) and Depth(m) offsets from the 
     * Lat/Lon/Depth value.
     */
    public double[] getAbsoluteNEDInMeters() {
        double[] absoluteLatLonDepth = getAbsoluteLatLonDepth();
        if (absoluteLatLonDepth == null)
            return null;

        double[] tmpDouble = CoordinateUtil.latLonDiff(0, 0, absoluteLatLonDepth[0],
                absoluteLatLonDepth[1]);
        double[] result = new double[3];
        result[0] = tmpDouble[0];
        result[1] = tmpDouble[1];
        result[2] = absoluteLatLonDepth[2];
        return result;
    }

    /**
     * @return
     */
    public String getClipboardText() {
        return "[NeptusLocation] id=" + getId() + "\n" + getLatitudeStr() + " " + getLongitudeStr() + " "
                + getDepth() + "\n" + getOffsetNorth() + " " + getOffsetEast() + " "
                + getOffsetDown() + "\n" + getOffsetDistance() + " " + getAzimuth() + " "
                + getZenith() + "\n";
    }

    /**
     * @param text
     * @return
     */
    public boolean fromClipboardText(String text) {
        StringTokenizer st = new StringTokenizer(text);

        if (st.countTokens() < 11) { // !=
            NeptusLog.pub().error("Invalid location found in the clipboard (" + st.countTokens()
                    + " tokens)");
            return false;
        }

        String val = st.nextToken();
        if (!val.equals("[NeptusLocation]")) {
            NeptusLog.pub().error("Invalid location found in the clipboard (first word: '" + val
                    + "')");
            return false;
        }

        try {
            st.nextToken(); // location id
            setLatitudeStr(st.nextToken());
            setLongitudeStr(st.nextToken());
            setDepth(Double.parseDouble(st.nextToken()));
            setOffsetNorth(Double.parseDouble(st.nextToken()));
            setOffsetEast(Double.parseDouble(st.nextToken()));
            setOffsetDown(Double.parseDouble(st.nextToken()));
            setOffsetDistance(Double.parseDouble(st.nextToken()));
            setAzimuth(Double.parseDouble(st.nextToken()));
            setZenith(Double.parseDouble(st.nextToken()));
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return true;
    }

    public Element asGPXWaypointElement(String wptName) {
        Element wpt = DocumentHelper.createElement("wpt");

        double[] absCoords = getAbsoluteLatLonDepth();

        wpt.addAttribute("lat", String.valueOf(absCoords[0]));
        wpt.addAttribute("lon", String.valueOf(absCoords[1]));
        wpt.addElement("ele").setText(String.valueOf((-absCoords[2])));
        wpt.addElement("name").setText(wptName);
        wpt.addElement("cmt").setText("Computer generated waypoint");
        wpt.addElement("desc").setText(wptName);
        wpt.addElement("sym").setText("City (Medium)");

        return wpt;
    }

    public Element asGPXWaypointElement(String wptName, String symbol, String desc) {
        Element wpt = DocumentHelper.createElement("wpt");

        double[] absCoords = getAbsoluteLatLonDepth();

        wpt.addAttribute("lat", String.valueOf(absCoords[0]));
        wpt.addAttribute("lon", String.valueOf(absCoords[1]));
        wpt.addElement("ele").setText(String.valueOf((-absCoords[2])));
        wpt.addElement("name").setText(wptName);
        wpt.addElement("cmt").setText("Computer generated waypoint");
        wpt.addElement("desc").setText(desc);
        wpt.addElement("sym").setText(symbol);

        return wpt;
    }

    public Element asGPXRoutePoint() {
        Element trkpt = DocumentHelper.createElement("rtept");

        double[] absCoords = getAbsoluteLatLonDepth();

        trkpt.addAttribute("lat", String.valueOf(absCoords[0]));
        trkpt.addAttribute("lon", String.valueOf(absCoords[1]));
        trkpt.addElement("ele").setText(String.valueOf((-absCoords[2])));
        trkpt.addElement("name").setText(getId());
        trkpt.addElement("desc").setText("Route Waypoint");
        return trkpt;
    }

    /**
     * Combines absolute z and additional offset values and returns the result
     * @return z with all offsets added
     */
    public double getAllZ() {
        double[] stc = CoordinateUtil.sphericalToCartesianCoordinates(getOffsetDistance(),
                getAzimuth(), getZenith());
        stc[2] += getOffsetDown();
        stc[2] += getDepth();

        return stc[2];
    }

    /**
     * Sets both z and down offsets to zero.
     */
    protected void makeTotalDepthZero() {
        setDepth(0);
        setOffsetDown(0);
        setOffsetDistance(0);
        setAzimuth(0);
        setZenith(90);
    }

    /**
     * Calls {@link #makeTotalDepthZero()} and then sets the value of depth to the given value
     * 
     * @param depth
     */
    public void setAbsoluteDepth(double z) {
        makeTotalDepthZero();
        setDepth(z);
    }

    /**
     * @param pt1
     * @param pt2
     * @return
     */
    public double getXYDistanceToLine(LocationType pt1, LocationType pt2) {
        double p0[] = getAbsoluteLatLonDepth();
        double p1[] = pt1.getAbsoluteLatLonDepth();
        double p2[] = pt2.getAbsoluteLatLonDepth();

        Line2D l = new Line2D.Double(p1[0], p1[1], p2[0], p2[1]);
        return l.ptLineDist(p0[0], p0[1]);
    }

    /**
     * Get global x y coordinates of this location in a given level of details (map size)
     * @param levelOfDetail
     * @return
     */
    public Point2D getPointInPixel(int levelOfDetail) {
        LocationType meWithOffset = this.getNewAbsoluteLatLonDepth();
        return MapTileUtil.degreesToXY(meWithOffset.getLatitudeDegs(), meWithOffset.getLongitudeDegs(), levelOfDetail);
    }

    /**
     * Get distance in pixels to the target point in the given level
     * @param target
     * @param level
     * @return
     */
    public double[] getDistanceInPixelTo(LocationType target, int levelOfDetail) {
        LocationType meWithOffset = this.getNewAbsoluteLatLonDepth();
        LocationType targetWithOffset = target.getNewAbsoluteLatLonDepth();
        return MapTileUtil.getOffsetInPixels(meWithOffset, targetWithOffset, levelOfDetail);
    }
    
    /**
     * Get distance in pixels to the target point in the given level
     * @param target
     * @param level
     * @return
     */
    public double getPixelDistanceTo(LocationType target, int levelOfDetail) {
        double[] offsets =  getDistanceInPixelTo(target, levelOfDetail);        
        return Math.sqrt(offsets[0] * offsets[0] + offsets[1] * offsets[1]);
    }
    
    public static LocationType clipboardLocation() {
        @SuppressWarnings({ "unused" })
        ClipboardOwner owner = new ClipboardOwner() {
            public void lostOwnership(Clipboard clipboard, Transferable contents) {};                       
        };
        
        Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        
        boolean hasTransferableText = (contents != null)
                && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        
        if ( hasTransferableText ) {
            try {
                String text = (String)contents.getTransferData(DataFlavor.stringFlavor);
                LocationType lt = new LocationType();
                lt.fromClipboardText(text);
                return lt;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        return null;
    }

    

    /**
     * Translate a location by pixels
     * @param deltaX
     * @param deltaY
     * @param level
     */
    public void translateInPixel(double deltaX, double deltaY, int levelOfDetail){
        Point2D pixs = getPointInPixel(levelOfDetail);
        double[] latlon = MapTileUtil.xyToDegrees(pixs.getX() + deltaX, pixs.getY() + deltaY, levelOfDetail);
        convertToAbsoluteLatLonDepth(); // just to clear the offsets and save the depth
        this.setLatitudeDegs(latlon[0]);
        this.setLongitudeDegs(latlon[1]);
    }

    public void setLocationByPixel(double x, double y, int levelOfDetail){
        double[] degrees = MapTileUtil.xyToDegrees(x, y, levelOfDetail);
        convertToAbsoluteLatLonDepth(); // just to clear the offsets and save the depth
        this.latitudeRads = Math.toRadians(degrees[0]);
        this.longitudeRads = Math.toRadians(degrees[1]);
    }

    /**
     * Compares 2 locations
     * @param location
     * @return
     */
    public boolean isLocationEqual(LocationType location) {
        if (location == null)
            return false;

        LocationType loc1 = this.getNewAbsoluteLatLonDepth();
        LocationType loc2 = location.getNewAbsoluteLatLonDepth();

        if (loc2.getLatitudeDegs() == 0.0) {
            if (loc1.getLatitudeDegs() == 0.0) {
                return true;
            }
            else {
                return false;
            }
        }

        double loc1LatDouble = cropDecimalDigits(10, loc1.getLatitudeDegs());
        double loc2LatDouble = cropDecimalDigits(10, loc2.getLatitudeDegs());
        double loc1LonDouble = cropDecimalDigits(10, loc1.getLongitudeDegs());
        double loc2LonDouble = cropDecimalDigits(10, loc2.getLongitudeDegs());

        // System.out.println();
        // System.out.println("Lat:" + loc1LatDouble + " Lon:" + loc1LonDouble);
        // System.out.println("Lat:" + loc2LatDouble + " Lon:" + loc2LonDouble);
        // System.out.println();

        if (Double.compare(loc1LatDouble, loc2LatDouble) == 0 && Double.compare(loc1LonDouble, loc2LonDouble) == 0
                && (loc1.getDepth()) == (loc2.getDepth())) // Double.compare(loc1.getDepth(),loc2.getDepth()) == 0)
            return true;
        else
            return false;
    }

    private double cropDecimalDigits(int digit, double value) {
        String string = value + "";
        String[] tokens = string.split("\\.");
        StringBuilder res = new StringBuilder(tokens[0]);
        res.append(".");
        res.append(tokens[1].substring(0, (tokens[1].length() > digit) ? digit : tokens[1].length()));
        return Double.valueOf(res.toString());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof LocationType))
            return false;
        LocationType otherLoc = (LocationType) obj;

        return isLocationEqual(otherLoc);
    }

    @Override
    public int hashCode() {
        LocationType loc1 = this.getNewAbsoluteLatLonDepth();
        double loc1LatDouble = cropDecimalDigits(10, loc1.getLatitudeDegs());
        double loc1LonDouble = cropDecimalDigits(10, loc1.getLongitudeDegs());

        int hash = 7;
        hash = 31 * hash + Double.hashCode(loc1LatDouble);
        hash = 31 * hash + Double.hashCode(loc1LonDouble);
        hash = 31 * hash + Double.hashCode(depth);
        return hash;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(LocationType o) {
        return (int) getDistanceInMeters(o);
    }

    public String getDebugString() {
        double[] abs = getOffsetFrom(new LocationType());
        return "LOCATION/DEBUG:\n"+
        "Latitude: "+getLatitudeStr()+", Longitude: "+getLongitudeStr()+"\n"+
        "Depth: "+getDepth()+"\n"+
        "OffsetUp: "+getOffsetUp()+", OffsetDown: "+getOffsetDown()+"\n"+
        "OffsetNorth: "+getOffsetNorth()+", OffsetSouth: "+getOffsetSouth()+"\n"+
        "OffsetEast: "+getOffsetEast()+", OffsetWest: "+getOffsetWest()+"\n"+
        "OffsetDistance: "+getOffsetDistance()+", Azimuth: "+getAzimuth()+"\n"+
        "Zenith: "+getZenith()+", Absolut XY: ("+abs[0]+", "+abs[1]+")";
    }

    public static void LocationTypeTest() {
        for (LatLonFormatEnum lp : LatLonFormatEnum.values()) {
            GeneralPreferences.latLonPrefFormat = lp;
            System.out.println("for >> " + lp.toString());
            
            LocationType loc = new LocationType();
            loc.setLatitudeRads(0.7188013442408926);
            loc.setLongitudeRads(0.7188013442408926);
            loc.setHeight(3);
            loc.setOffsetNorth(95.97593750551583);
            loc.setOffsetEast(-274.7049781636526);
            loc.setOffsetDown(1.6755575514192749);
            NeptusLog.pub().info("<###> "+loc);
            loc.convertToAbsoluteLatLonDepth();
            NeptusLog.pub().info("<###> "+loc);
            LocationType loc2 = LocationType.valueOf(loc.toString());
            NeptusLog.pub().info("<###> "+loc2);
            loc.setHeight(0);
            LocationType loc3 = LocationType.valueOf(loc.toString());
            NeptusLog.pub().info("<###> "+loc3);
        }
    }
    
    public static void main(String[] args) {
        
        LocationType loc = new LocationType(41.77841222222222, -8.2343122222222);
        
        GeneralPreferences.latLonPrefFormat = LatLonFormatEnum.DECIMAL_DEGREES;
        System.out.println(loc);
        System.out.println(CoordinateUtil.parseCoordString(loc.toString().split(",")[1]));
        
        GeneralPreferences.latLonPrefFormat = LatLonFormatEnum.DM;
        System.out.println(loc);
        System.out.println(CoordinateUtil.parseCoordString(loc.toString().split(",")[1]));
        GeneralPreferences.latLonPrefFormat = LatLonFormatEnum.DMS;
        System.out.println(loc);
        System.out.println(CoordinateUtil.parseCoordString(loc.toString().split(",")[1]));
        
        
        LocationTypeTest();
        
        for (LatLonFormatEnum lp : LatLonFormatEnum.values()) {
            GeneralPreferences.latLonPrefFormat = lp;
            System.out.println("for >> " + lp.toString());
            System.out.println(loc.getClipboardText());
            System.out.println(loc.fromClipboardText(loc.getClipboardText()));
            System.out.println(loc.getClipboardText());
        }

        LocationType loc1 = new LocationType();
        loc1.setLatitudeDegs(41.2323232323);
        loc1.setLongitudeDegs(-8.4343434343);
        LocationType loc2 = new LocationType(loc1);
        LocationType loc3 = new LocationType(loc1);
        loc3.setDepth(1);

        System.out.println("Equals => true :=: " + loc1.equals(loc1));
        System.out.println("Equals => true :=: " + loc1.equals(loc2));
        System.out.println("Equals => true :=: " + loc2.equals(loc1));
        System.out.println("Equals => false :=: " + loc1.equals(loc3));
        System.out.println("Equals => false :=: " + loc3.equals(loc1));

        System.out.println("HashCode => loc1 :=: " + loc1.hashCode());
        System.out.println("HashCode => loc2 :=: " + loc2.hashCode());
        System.out.println("HashCode => loc3 :=: " + loc3.hashCode());
    }
}
