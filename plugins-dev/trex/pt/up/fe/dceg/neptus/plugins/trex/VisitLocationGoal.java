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
 * Author: meg
 * Apr 29, 2013
 */
package pt.up.fe.dceg.neptus.plugins.trex;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.TrexAttribute;
import pt.up.fe.dceg.neptus.imc.TrexAttribute.ATTR_TYPE;
import pt.up.fe.dceg.neptus.imc.TrexOperation;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author meg
 *
 */
public class VisitLocationGoal extends TrexGoal {
    private enum Properties {
        SPEED("Speed"),
        DEPTH("Depth", "z"),
        LAT("Latitude (degrees)", "latitude"),
        LON("Longitude (degrees)", "longitude"),
        TOLERANCE("Tolerance");

        public final String label;
        public final String varName;

        private Properties(String label, String varName) {
            this.label = label;
            this.varName = varName;
        }
    }

    // protected double speed = 1.0, depth = 2, lat_deg = 41, lon_deg = -8, tolerance = 15;
    protected double speed, depth, lat_deg, lon_deg, tolerance;// FIXME passar a location type

    /**
     * @param speed
     * @param depth
     * @param lat_deg
     * @param lon_deg
     * @param tolerance
     */
    public VisitLocationGoal(double speed, double depth, double lat_deg, double lon_deg, double tolerance) {
        super();
        super.timeline = Timelines.NAVIGATIOR;
        super.predicate = Predicates.AT;
        this.speed = speed;
        this.depth = depth;
        this.lat_deg = lat_deg;
        this.lon_deg = lon_deg;
        this.tolerance = tolerance;
    }

    @Override
    public DefaultProperty[] getProperties() {
        DefaultProperty[] superProperties = super.getProperties();
        DefaultProperty[] properties = new DefaultProperty[superProperties.length + 5];
        int i;
        for (i = 0; i < superProperties.length; i++) {
            properties[i] = superProperties[i];
        }
        properties[i] = PropertiesEditor.getPropertyInstance(Properties.DEPTH.name, Double.class, depth, true);
        i++;
        properties[i] = PropertiesEditor.getPropertyInstance(Properties.SPEED.name, Double.class, speed, true);
        i++;
        properties[i] = PropertiesEditor.getPropertyInstance(Properties.TOLERANCE.name, Double.class, tolerance, true);
        i++;
        properties[i] = PropertiesEditor.getPropertyInstance(Properties.LAT.name, Double.class, lat_deg, true);
        i++;
        properties[i] = PropertiesEditor.getPropertyInstance(Properties.LON.name, Double.class, lon_deg, true);
        return properties;
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        for (Property p : properties) {
            if (p.getName().equals(Properties.TOLERANCE.name)) {
                tolerance = (Double) p.getValue();
            }
            else if (p.getName().equals(Properties.DEPTH.name)) {
                depth = (Double) p.getValue();
            }
            else if (p.getName().equals(Properties.SPEED.name)) {
                speed = (Double) p.getValue();
            }
            else if (p.getName().equals(Properties.LAT.name)) {
                lat_deg = (Double) p.getValue();
            }
            else if (p.getName().equals(Properties.LON.name)) {
                lon_deg = (Double) p.getValue();
            }
        }
    }

    @Override
    public TrexOperation asIMCMsg() {
        // "<Variable name='z'>\n\t<float min='"+depth+"' max='"+depth+"'/>\n</Variable>\n";
        TrexAttribute attribute = new TrexAttribute();
        attribute.setName(Properties.DEPTH.varName);
        attribute.setAttrType(ATTR_TYPE.FLOAT);
        attribute.setMin(depth + "");
        attribute.setMax(depth + "");
        attributes.add(attribute);
        // "<Variable name='latitude'>\n\t<float min='" + Math.toRadians(lat_deg)
        // + "' max='" + Math.toRadians(lat_deg) + "'/>\n</Variable>\n";
        attribute = new TrexAttribute();
        attribute.setName(Properties.LAT.varName);
        attribute.setAttrType(ATTR_TYPE.FLOAT);
        attribute.setMin(lat_deg + "");
        attribute.setMax(lat_deg + "");
        attributes.add(attribute);
        // "<Variable name='longitude'>\n\t<float min='" + Math.toRadians(lon_deg)
        // + "' max='" + Math.toRadians(lon_deg) + "'/>\n</Variable>\n";
        attribute = new TrexAttribute();
        attribute.setName(Properties.LAT.varName);
        attribute.setAttrType(ATTR_TYPE.FLOAT);
        attribute.setMin(lon_deg + "");
        attribute.setMax(lon_deg + "");
        attributes.add(attribute);
        return super.asIMCMsg();
    }

    public LocationType getLocation() {
        LocationType loc = new LocationType(lat_deg, lon_deg);
        loc.setAbsoluteDepth(depth);
        return loc;
    }

}
