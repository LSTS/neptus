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
 * Oct 12, 2013
 */
package pt.lsts.neptus.plugins.sunfish;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class VirtualDrifter implements PropertiesProvider {

    @NeptusProperty
    public LocationType loc = new LocationType();
    
    @NeptusProperty
    public double headingDegrees;
    
    @NeptusProperty
    public double speedMps;
    
    @NeptusProperty
    public String id;
    
    protected double[] velocity;
    
    protected long startTime;
    
    public VirtualDrifter(LocationType loc, double heading, double speed) {
        this.startTime = System.currentTimeMillis();
        this.loc = loc;
        this.velocity = new double[2];
        this.speedMps = speed;
        this.headingDegrees = heading;
        velocity[0] = Math.cos(Math.toRadians(heading))*speed;
        velocity[1] = Math.sin(Math.toRadians(heading))*speed;
    }
    
    public LocationType getLocation() {
        LocationType curLoc = new LocationType(loc);
        double ellapsedTime = (System.currentTimeMillis() - startTime) / 1000.0;
        double movementx = velocity[0] * ellapsedTime;
        double movementy = velocity[1] * ellapsedTime;
        curLoc.translatePosition(movementx, movementy, 0);
        curLoc.convertToAbsoluteLatLonDepth();
        return curLoc;
    }    
    
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }
    
    @Override
    public String getPropertiesDialogTitle() {
        return "Virtual Drifter Properties";
    }
    
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
    
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
        velocity[0] = Math.cos(Math.toRadians(headingDegrees))*speedMps;
        velocity[1] = Math.sin(Math.toRadians(headingDegrees))*speedMps;
    }
}
