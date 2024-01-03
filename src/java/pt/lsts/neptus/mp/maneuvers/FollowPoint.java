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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 02/09/2016
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.messages.TupleList;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class FollowPoint extends Maneuver
        implements LocatedManeuver, ManeuverWithSpeed, IMCSerialization, StatisticsProvider {

    protected ManeuverLocation location = new ManeuverLocation();
    protected static final String DEFAULT_ROOT_ELEMENT = "FollowPoint";
    
    protected TupleList customData = new TupleList();

    @NeptusProperty(name = "Maximum Speed")
    protected SpeedType speed = new SpeedType(1000, Units.RPM);

    
    @NeptusProperty(name = "Position Source", description = "IMC ID of the position source to follow")
    protected String idToFollow = "lauv-noptilus-1";

    @Override
    public void loadManeuverFromXML(String xml) {
        try {
            Document doc = DocumentHelper.parseText(xml);
            try {
                ManeuversXMLUtil.parseLocation(doc.getRootElement(), this);
                SpeedType.parseManeuverSpeed(doc.getRootElement(), this);
             
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Node node = doc.selectSingleNode("//idToFollow");
            if (node != null)
                idToFollow = node.getText();
            else
                idToFollow = "lauv-xplore-1";

            node = doc.selectSingleNode("//customData");
            if (node != null)
                customData.parse(node.getText());
            else
                customData = new TupleList();
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return;
        }
    }

    @Override
    public Document getManeuverAsDocument(String rootElementName) {

        Document doc = ManeuversXMLUtil.createBaseDoc(getType());

        try {
            ManeuversXMLUtil.addLocation(doc.getRootElement(), this);
            SpeedType.addSpeedElement(doc.getRootElement(), this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        root.addElement("idToFollow").setText(idToFollow);
        root.addElement("customData").setText("" + customData);

        return doc;
    }

    @Override
    public IMCMessage serializeToIMC() {
        pt.lsts.imc.FollowPoint msg = new pt.lsts.imc.FollowPoint();
        speed.setSpeedToMessage(msg, "max_speed", "speed_units");
        
        LocationType l = getManeuverLocation().convertToAbsoluteLatLonDepth();
        msg.setLat(l.getLatitudeRads());
        msg.setLon(l.getLongitudeRads());
        msg.setTarget(idToFollow);
        msg.setZ(location.getZ());
        msg.setZUnits(ZUnits.valueOf(location.getZUnits().toString()));
        msg.setCustom(customData.toString());

        return msg;
    }

    @Override
    public void parseIMCMessage(IMCMessage message) {
        pt.lsts.imc.FollowPoint man = null;
        try {
            man = pt.lsts.imc.FollowPoint.clone(message);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        ManeuverLocation loc = new ManeuverLocation();
        loc.setLatitudeRads(man.getLat());
        loc.setLongitudeRads(man.getLon());
        loc.setZ(man.getZ());
        loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(man.getZUnits().toString()));
        setManeuverLocation(loc);

        idToFollow = man.getTarget();
        
        customData.clear();
        customData.parse(man.getCustom());
        speed = SpeedType.parseImcSpeed(message, "max_speed", "speed_units");
    }

    @Override
    public ManeuverLocation getEndLocation() {
        return getManeuverLocation();
    }

    @Override
    public ManeuverLocation getStartLocation() {
        return getManeuverLocation();
    }

    @Override
    public ManeuverLocation getManeuverLocation() {
        return location.clone();
    }

    @Override
    public void setManeuverLocation(ManeuverLocation location) {
        this.location.setLocation(location);
        this.location.setZ(location.getZ());
        this.location.setZUnits(location.getZUnits());
    }

    @Override
    public Collection<ManeuverLocation> getWaypoints() {
        ArrayList<ManeuverLocation> locs = new ArrayList<>();
        locs.add(getManeuverLocation());
        return locs;
    }

    @Override
    public void translate(double offsetNorth, double offsetEast, double offsetDown) {
        location.translatePosition(offsetNorth, offsetEast, offsetDown);
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        return 0;
    }

    @Override
    public double getDistanceTravelled(LocationType initialPosition) {
        return initialPosition.getDistanceInMeters(getManeuverLocation());
    }

    @Override
    public double getMaxDepth() {
        return location.getDepth();
    }

    @Override
    public double getMinDepth() {
        return 0;
    }
    
    @Override
    protected Vector<DefaultProperty> additionalProperties() {
        return ManeuversUtil.getPropertiesFromManeuver(this);
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        ManeuversUtil.setPropertiesToManeuver(this, properties);
    }
    
    @Override
    public SpeedType getSpeed() {
        return new SpeedType(speed);
    }
    
    @Override
    public void setSpeed(SpeedType speed) {
        this.speed = new SpeedType(speed);       
    }

    public static void main(String[] args) {
        pt.lsts.imc.FollowPoint fp = new pt.lsts.imc.FollowPoint();
        IMCUtil.fillWithRandomData(fp);
        FollowPoint man1 = new FollowPoint();
        man1.parseIMCMessage(fp);
        System.out.println(man1.asXML());

    }
}
