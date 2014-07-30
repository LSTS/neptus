/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * Jan 20, 2014
 */
package pt.lsts.neptus.mra;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author hfq
 * These are Neptus MRA default properties
 */
public class MRAProperties implements PropertiesProvider{

    @NeptusProperty(name = "Show 3D replay")
    public static boolean show3D = true;

    @NeptusProperty(name = "Default time step (seconds)")
    public static double defaultTimestep = 1.0;

    @NeptusProperty(name = "Minimum depth for bathymetry", description="Filter all bathymetry data if vehicle's depth is less than this value (meters).")
    public static double minDepthForBathymetry = 1.0;

    @NeptusProperty(name = "Points to ignore on Multibeam 3D", description="Fixed step of number of points to jump on multibeam Pointcloud stored for render purposes.")
    public static int ptsToIgnore = 50;

    @NeptusProperty(name = "Approach to ignore points on Multibeam 3D", description="Type of approach to ignore points on multibeam either by a fixed step (false) or by a probability (true).")
    public static boolean approachToIgnorePts = true; 

    @NeptusProperty(name = "Depth exaggeration multiplier", description="Multiplier value for depth exaggeration.")
    public static int zExaggeration = 10;

    @NeptusProperty(name = "Timestamp increment", description="Timestamp increment for the 83P parser (in miliseconds).")
    public static long timestampMultibeamIncrement = 0;

    @NeptusProperty(name = "Yaw Increment", description="180 Yaw (psi) increment for the 83P parser, set true to increment +180\u00B0.")
    public static boolean yawMultibeamIncrement = false;

    @NeptusProperty(name = "Remove Outliers", description="Remove Outliers from Pointcloud redered on multibeam 3D")
    public static boolean outliersRemoval = false; 

    @NeptusProperty(name = "Maximum depth for bathymetry plots", description="Maximum depth to be used in bathymetry plots.")
    public static double maxBathymDepth = 15;

    @NeptusProperty(name = "Print page number in generated reports")
    public static boolean printPageNumbers = true;
    
    @NeptusProperty(name = "Entity to use for depth measurements")
    public static depthEntities depthEntity = depthEntities.CTD;
    

    public enum depthEntities {
        CTD,
        Depth_Sensor
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesDialogTitle()
     */
    @Override
    public String getPropertiesDialogTitle() {
        return "MRA Preferences";
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
}
