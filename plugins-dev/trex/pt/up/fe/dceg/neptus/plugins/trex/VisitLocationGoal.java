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

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.Vector;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.TrexAttribute;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author meg
 *
 */
public class VisitLocationGoal extends TrexGoal implements Renderer2DPainter {

    protected ManeuverLocation location;

    public VisitLocationGoal() {
        super("estimator", "At");
    }

    @Override
    public Collection<TrexAttribute> getAttributes() {
        //TODO;
        return new Vector<TrexAttribute>();
    }

    @Override
    public void parseAttributes(Collection<TrexAttribute> attributes) {
        //TODO
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
        super("navigator", "At");
        this.speed = speed;
        this.depth = depth;
        this.lat_deg = lat_deg;
        this.lon_deg = lon_deg;
        this.tolerance = tolerance;
    }

    @Override
    public Collection<DefaultProperty> getSpecificProperties() {

        Vector<DefaultProperty> props = new Vector<>();

        props.add(PropertiesEditor.getPropertyInstance("Depth", Double.class, depth, true));
        props.add(PropertiesEditor.getPropertyInstance("Speed", Double.class, speed, true));
        props.add(PropertiesEditor.getPropertyInstance("Latitude", Double.class, lat_deg, true));
        props.add(PropertiesEditor.getPropertyInstance("Longitude", Double.class, lon_deg, true));

        return props;
    }

    @Override
    public void setSpecificProperties(Collection<Property> properties) {
        for (Property p : properties) {
            switch (p.getName()) {
                case "Depth":
                    depth = (Double) p.getValue();
                    break;
                case "Speed":
                    speed = (Double) p.getValue();
                case "Latitude":
                    lat_deg = (Double) p.getValue();
                case "Longitude":
                    lon_deg = (Double) p.getValue();
                default:
                    break;
            }
        }
    }

    public LocationType getLocation() {
        LocationType loc = new LocationType(lat_deg, lon_deg);
        loc.setAbsoluteDepth(depth);
        return loc;
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
    }

}
