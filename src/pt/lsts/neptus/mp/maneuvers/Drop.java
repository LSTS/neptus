/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Manuel Ribeiro
 * Nov 23, 2015
 */
package pt.lsts.neptus.mp.maneuvers;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Drop.DIRECTION;
import pt.lsts.imc.Drop.TYPE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel Ribeiro
 *
 */
public class Drop extends Loiter {

    protected static final String DEFAULT_ROOT_ELEMENT = "Drop";

    @Override
    public String getType() {
        return "Drop";
    }

    @Override
    public Object clone() {
        Drop l = new Drop();
        super.clone(l);
        l.setBearing(getBearing());
        l.setDirection(getDirection());
        l.setLength(getLength());
        l.setLoiterDuration(getLoiterDuration());
        l.setLoiterType(getLoiterType());
        l.setManeuverLocation(getManeuverLocation());
        l.setRadius(getRadius());
        l.setRadiusTolerance(getRadiusTolerance());
        l.setSpeed(getSpeed());
        l.setSpeedTolerance(getSpeedTolerance());
        l.setSpeedUnits(getSpeedUnits());
        return l;       
    }

    @Override
    public void loadFromXML(String XML) {
        try {
            Document doc = DocumentHelper.parseText(XML);

            // basePoint
            Node node = doc.selectSingleNode("Drop/basePoint/point");
            ManeuverLocation loc = new ManeuverLocation();
            loc.load(node.asXML());
            setManeuverLocation(loc);
            setRadiusTolerance(Double.parseDouble(doc.selectSingleNode("Drop/basePoint/radiusTolerance").getText()));

            // Velocity
            Node speedNode = doc.selectSingleNode("Drop/speed");
            if (speedNode == null)
                speedNode = doc.selectSingleNode("Drop/velocity");
            setSpeed(Double.parseDouble(speedNode.getText()));
            setSpeedUnits(speedNode.valueOf("@unit"));
            setSpeedTolerance(Double.parseDouble(speedNode.valueOf("@tolerance")));

            // Duration
            setLoiterDuration(Integer.parseInt(doc.selectSingleNode("Drop/duration").getText()));

            // Trajectory
            setRadius(Double.parseDouble(doc.selectSingleNode("Drop/trajectory/radius").getText()));
            setRadiusTolerance(Double.parseDouble(doc.selectSingleNode("Drop/trajectory/radiusTolerance").getText()));
            setLoiterType(doc.selectSingleNode("Drop/trajectory/type").getText());
            setDirection(doc.selectSingleNode("Drop/trajectory/direction").getText());            
            setBearing(Double.parseDouble(doc.selectSingleNode("Drop/trajectory/bearing").getText()));                        

            if (doc.selectSingleNode("Drop/trajectory/length") != null)           
                setLength(Double.parseDouble(doc.selectSingleNode("Drop/trajectory/length").getText()));

            if (doc.selectSingleNode("Drop/trajectory/lenght") != null)           
                setLength(Double.parseDouble(doc.selectSingleNode("Drop/trajectory/lenght").getText()));

        }
        catch (Exception e) {

            NeptusLog.pub().error(this, e);
            return;
        }
    }

    @Override
    public IMCMessage serializeToIMC() {

        pt.lsts.imc.Drop drop = new pt.lsts.imc.Drop();
        drop.setTimeout(this.getMaxTime());

        LocationType loc = getManeuverLocation();
        loc.convertToAbsoluteLatLonDepth();

        drop.setLat(loc.getLatitudeRads());
        drop.setLon(loc.getLongitudeRads());
        drop.setZ(getManeuverLocation().getZ());
        drop.setZUnits(pt.lsts.imc.Drop.Z_UNITS.valueOf(getManeuverLocation().getZUnits().name()));
        drop.setSpeed(this.getSpeed());
        drop.setDuration(getLoiterDuration());

        switch (this.getSpeedUnits()) {
            case "m/s":
                drop.setSpeedUnits(pt.lsts.imc.Drop.SPEED_UNITS.METERS_PS);
                break;
            case "RPM":
                drop.setSpeedUnits(pt.lsts.imc.Drop.SPEED_UNITS.RPM);
                break;
            case "PERCENTAGE":
                drop.setSpeedUnits(pt.lsts.imc.Drop.SPEED_UNITS.PERCENTAGE);
                break;
            default:
                break;
        }


        String loiterType = this.getLoiterType();
        try {
            if ("Default".equalsIgnoreCase(loiterType))
                drop.setType(TYPE.DEFAULT);
            else if ("Circular".equalsIgnoreCase(loiterType))
                drop.setType(TYPE.CIRCULAR);
            else if ("Racetrack".equalsIgnoreCase(loiterType))
                drop.setType(TYPE.RACETRACK);
            else if ("Figure 8".equalsIgnoreCase(loiterType))
                drop.setType(TYPE.EIGHT);
            else if ("Hover".equalsIgnoreCase(loiterType))
                drop.setType(TYPE.HOVER);
        } catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }

        drop.setRadius(getRadius());
        drop.setLength(getLength());
        drop.setBearing(getBearing());

        String lDirection = this.getDirection();

        try {
            if ("Vehicle Dependent".equalsIgnoreCase(lDirection))
                drop.setDirection(DIRECTION.VDEP);
            else if ("Clockwise".equalsIgnoreCase(lDirection))
                drop.setDirection(DIRECTION.CLOCKW);
            else if ("Counter Clockwise".equalsIgnoreCase(lDirection))
                drop.setDirection(DIRECTION.CCLOCKW);
            else if ("Counter-Clockwise".equalsIgnoreCase(lDirection))
                drop.setDirection(DIRECTION.CCLOCKW);
            else if (lDirection.startsWith("Into the wind"))
                drop.setDirection(DIRECTION.IWINDCURR);
        } catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }

        drop.setCustom(getCustomSettings());

        return drop;
    }
}