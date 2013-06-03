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
 * Author: Margarida Faria
 * Apr 29, 2013
 */
package pt.up.fe.dceg.neptus.plugins.trex.goals;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.Vector;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.TrexAttribute;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author meg
 *
 */
public class Surveil extends TrexGoal implements Renderer2DPainter {
    private static final String predicate = "Surveil";
    private static final String timeline = "reference";
    
    protected double speed = 1000;
    protected double height = 200;
    protected double tolerance = 10;
    // FIXME passar a location type
    protected double latitude, longitude;

    public Surveil(double lat_deg, double lon_deg) {
        super(timeline, predicate);
        this.latitude = lat_deg;
        this.longitude = lon_deg;
    }	
	
    public Surveil() {
        super(timeline, predicate);
    }

    /**
     * @param speed
     * @param depth
     * @param lat_deg
     * @param lon_deg
     * @param tolerance
     */
    public Surveil(double speed, double height, double lat_deg, double lon_deg) {
        super(timeline, predicate);
        this.speed = speed;
        this.height = height;
        this.latitude = lat_deg;
        this.longitude = lon_deg;
    }


    @Override
    public Collection<TrexAttribute> getAttributes() {
        Vector<TrexAttribute> attributes = new Vector<TrexAttribute>();
        TrexAttribute attrTemp = new TrexAttribute();
        attrTemp.setName("speed");
        attrTemp.setMin(speed + "");
        attrTemp.setMax(speed + "");
        attrTemp.setAttrType(TrexAttribute.ATTR_TYPE.FLOAT);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName("latitude");
        attrTemp.setMin(latitude + "");
        attrTemp.setMax(latitude + "");
        attrTemp.setAttrType(TrexAttribute.ATTR_TYPE.FLOAT);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName("longitude");
        attrTemp.setMin(longitude + "");
        attrTemp.setMax(longitude + "");
        attrTemp.setAttrType(TrexAttribute.ATTR_TYPE.FLOAT);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName("z");
        attrTemp.setMin(height + "");
        attrTemp.setMax(height + "");
        attrTemp.setAttrType(TrexAttribute.ATTR_TYPE.FLOAT);
        attributes.add(attrTemp);
        return attributes;
    }

    @Override
    public void parseAttributes(Collection<TrexAttribute> attributes) {
        //TODO
    }
    
    @Override
    public Collection<DefaultProperty> getSpecificProperties() {

        Vector<DefaultProperty> props = new Vector<>();

        props.add(PropertiesEditor.getPropertyInstance("Depth", Double.class, height, true));
        props.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, speed, true));
        props.add(PropertiesEditor.getPropertyInstance("Latitude", Double.class, latitude, true));
        props.add(PropertiesEditor.getPropertyInstance("Longitude", Double.class, longitude, true));

        return props;
    }

    @Override
    public void setSpecificProperties(Collection<Property> properties) {
        for (Property p : properties) {
            switch (p.getName()) {
                case "Height":
                    height = (Double) p.getValue();
                    break;
                case "Speed":
                    speed = (Double) p.getValue();
                case "Latitude":
                    latitude = (Double) p.getValue();
                case "Longitude":
                    longitude = (Double) p.getValue();
                default:
                    break;
            }
        }
    }

    public LocationType getLocation() {
        LocationType loc = new LocationType(latitude, longitude);
        loc.setHeight(height);
        return loc;
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
    }

    @Override
    public String toJson() {
        return "{"
                + "\"on\": \""+super.timeline+"\",\"pred\": \""+super.predicate+"\","
                + "\"Variable\":"
                + "["
                + "{\"float\":{\"value\": \""+speed+"\"}, \"name\": \"speed\"},"
                + "{\"float\":{\"value\": \""+latitude+"\"}, \"name\": \"latitude\"},"
                + "{\"float\":{\"value\": \""+longitude+"\"}, \"name\": \"longitude\"}"
                + "{\"float\":{\"value\": \"" + -height + "\"}, \"name\": \"z\"},"
//                + "{\"duration\":{\"min\": \"00:01:00.403000\", \"max\": \"00:01:00.403000\"}, \"type\": \"duration\", \"name\": \"duration\"}"
                + "]}";
    }

}
