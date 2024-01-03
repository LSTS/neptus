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
 * Author: Margarida Faria
 * Apr 29, 2013
 */
package pt.lsts.neptus.plugins.trex.goals;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.TrexAttribute;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author meg
 *
 */
public class UavSpotterSurvey extends TrexGoal implements Renderer2DPainter {
    private static final String predicate = "Survey";
    private static final String timeline = "spotter";
    

    public enum Attributes {
        LATITUDE("center_lat"),
        LONGITUDE("center_lon"),
        HEIGHT("z");

        public String name;

        private Attributes(String name) {
            this.name = name;
        }
    }

    private final HashMap<Attributes, Double> attributes;

    /**
     * @param lat_deg
     * @param lon_deg
     */
    public UavSpotterSurvey(double lat_deg, double lon_deg, int spotterHeight) {
        super(timeline, predicate);
        attributes = new HashMap<UavSpotterSurvey.Attributes, Double>();
        attributes.put(Attributes.LATITUDE, lat_deg);
        attributes.put(Attributes.LONGITUDE, lon_deg);
        attributes.put(Attributes.HEIGHT, (double) spotterHeight);
    }


    @Override
    public Collection<TrexAttribute> getAttributes() {
        Vector<TrexAttribute> attributes = new Vector<TrexAttribute>();
        TrexAttribute attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.LATITUDE.name);
        attrTemp.setMin(this.attributes.get(Attributes.LATITUDE) + "");
        attrTemp.setMax(this.attributes.get(Attributes.LATITUDE) + "");
        attrTemp.setAttrType(TrexAttribute.ATTR_TYPE.FLOAT);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.LONGITUDE.name);
        attrTemp.setMin(this.attributes.get(Attributes.LONGITUDE) + "");
        attrTemp.setMax(this.attributes.get(Attributes.LONGITUDE) + "");
        attrTemp.setAttrType(TrexAttribute.ATTR_TYPE.FLOAT);
        attributes.add(attrTemp);
        attrTemp = new TrexAttribute();
        attrTemp.setName(Attributes.HEIGHT.name);
        attrTemp.setMin(this.attributes.get(Attributes.HEIGHT) + "");
        attrTemp.setMax(this.attributes.get(Attributes.HEIGHT) + "");
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

        props.add(PropertiesEditor.getPropertyInstance("Latitude", Double.class,
                this.attributes.get(Attributes.LATITUDE), true));
        props.add(PropertiesEditor.getPropertyInstance("Longitude", Double.class,
                this.attributes.get(Attributes.LONGITUDE), true));

        return props;
    }

    @Override
    public void setSpecificProperties(Collection<Property> properties) {
        for (Property p : properties) {
            switch (p.getName()) {
                case "Latitude":
                    this.attributes.put(Attributes.LATITUDE, (Double) p.getValue());
                case "Longitude":
                    this.attributes.put(Attributes.LONGITUDE, (Double) p.getValue());
                default:
                    break;
            }
        }
    }

    public LocationType getLocation() {
        LocationType loc = new LocationType(this.attributes.get(Attributes.LATITUDE),
                this.attributes.get(Attributes.LONGITUDE));
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
                + "{\"float\":{\"value\": \""+this.attributes.get(Attributes.LATITUDE)+"\"}, \"name\": \""+Attributes.LATITUDE.name+"\"},"
                + "{\"float\":{\"value\": \""+this.attributes.get(Attributes.LONGITUDE)+"\"}, \"name\": \""+Attributes.LONGITUDE.name+"\"}"
                + "{\"float\":{\"value\": \"" + this.attributes.get(Attributes.HEIGHT) + "\"}, \"name\": \""+Attributes.HEIGHT.name+"\"}"
                + "]}";
    }

}
