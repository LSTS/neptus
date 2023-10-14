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
 * Feb 24, 2014
 */
package pt.lsts.neptus.plugins.txtcmd;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public abstract class AbstractTextCommand implements ITextCommand {

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return getCommand()+" properties";
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }
    
    @Override
    public String buildCommand() {
        Property[] props = getProperties();
        String ret = getCommand()+" ";
        boolean added = false;
        for (Property p : props) {
            if (added)
                ret +=";";
            if (p.getType() == LocationType.class) {
                LocationType loc = (LocationType)p.getValue();
                loc.convertToAbsoluteLatLonDepth();
                ret +="lat="+GuiUtils.getNeptusDecimalFormat(8).format(loc.getLatitudeDegs());
                ret +=";lon="+GuiUtils.getNeptusDecimalFormat(8).format(loc.getLongitudeDegs());
            }
            if (p.getType() == SpeedType.class) {
                SpeedType speed = (SpeedType)p.getValue();
                ret +="speed="+GuiUtils.getNeptusDecimalFormat(1).format(speed.getMPS());                
            }
            else {
                ret += p.getName()+"="+p.getValue();
            }
            added = true;
        }
        return ret;
    }
    
    @Override
    public PlanType resultingPlan(MissionType mt) {
        return null;
    }
    
    @Override
    public void parseCommand(String text) throws Exception {
        throw new Exception("Not implemented");
    }

    /**
     * Empty implementation
     * 
     * @see pt.lsts.neptus.plugins.txtcmd.ITextCommand#setCenter(pt.lsts.neptus.types.coord.LocationType)
     */
    @Override
    public void setCenter(LocationType loc) {
        // Empty implementation
    }
}
