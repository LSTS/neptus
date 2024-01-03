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
 * Author: hfq
 * Jan 20, 2014
 */
package pt.lsts.neptus.mra;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;

/**
 * These are Neptus MRA default properties
 * 
 * @author hfq
 */
public class MRAProperties implements PropertiesProvider {

    @NeptusProperty(name = "Show 3D replay")
    public static boolean show3D = true;

    @NeptusProperty(name = "Default time step (seconds)")
    public static double defaultTimestep = 1.0;

    @NeptusProperty(name = "Minimum depth for bathymetry", description = "Filter all bathymetry data if vehicle's depth is less than this value (meters).")
    public static double minDepthForBathymetry = 1.0;

    @NeptusProperty(name = "Points to ignore on Multibeam 3D", description = "Fixed step of number of points to jump on multibeam Pointcloud stored for render purposes.", category = "Multibeam")
    public static int ptsToIgnore = 50;

    @NeptusProperty(name = "Approach to ignore points on Multibeam 3D", description = "Type of approach to ignore points on multibeam either by a fixed step (false) or by a probability (true).", category = "Multibeam")
    public static boolean approachToIgnorePts = true;

    @NeptusProperty(name = "Depth exaggeration multiplier", description = "Multiplier value for depth exaggeration.", category = "Multibeam")
    public static int zExaggeration = 10;

    @NeptusProperty(name = "Timestamp increment", description = "Timestamp increment for the 83P parser (in miliseconds).", category = "Multibeam")
    public static long timestampMultibeamIncrement = 0;

    @NeptusProperty(name = "Yaw Increment", description = "180 Yaw (psi) increment for the 83P parser, set true to increment +180\u00B0.", category = "Multibeam")
    public static boolean yawMultibeamIncrement = false;

    @NeptusProperty(name = "Remove Outliers", description = "Remove Outliers from Pointcloud redered on multibeam 3D", category = "Multibeam")
    public static boolean outliersRemoval = false;

    @NeptusProperty(name = "Apply Sound Speed Correction", description = "Apply sound speed correction.", category = "Multibeam")
    public static boolean soundSpeedCorrection = false;

    @NeptusProperty(name = "Multibeam roll bias", description = "Roll bias, in degrees.", category = "Multibeam")
    public static double rollBias = 0;

    @NeptusProperty(name = "Generate DeltaT Process Report", description = "Generate DeltaT process report. Does not generate if already exist the report or the bathy.info is present. (Re-generate the index for successful generation.)", category = "Multibeam")
    public static boolean generateDeltaTProcessReport = false;

    @NeptusProperty(name = "Maximum depth for bathymetry plots", description = "Maximum depth to be used in bathymetry plots.")
    public static double maxBathymDepth = 110;

    @NeptusProperty(name = "Print page number in generated reports")
    public static boolean printPageNumbers = true;

    @NeptusProperty(name = "Entity to use for depth measurements")
    public static depthEntities depthEntity = depthEntities.CTD;
    
    @NeptusProperty(name = "Magnetometer Threshold", description = "Minimal (excluded) difference between magnetometer raw and compensated values.", units = "MicroTeslas")
    public static double magThreshold = 0.0;
    
    @NeptusProperty(name = "Magnetometer Layer Cell Width")
    public static int magCellW = 5;
    
    @NeptusProperty(name = "Batch Mode", description = "Do not ask user for inputs, use defaults", userLevel = LEVEL.ADVANCED, editable = false)
    public static boolean batchMode = false;
    

    private LinkedHashMap<Class<?>, Boolean> visiblePlots = new LinkedHashMap<Class<?>, Boolean>();

    {
        try {
            load(new File("conf/mra.properties"));
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
        }
    }

    public synchronized boolean isVisualizationActive(Class<?> mraVisualization) {
        return visiblePlots.containsKey(mraVisualization) && visiblePlots.get(mraVisualization);
    }

    public synchronized Collection<DefaultProperty> getVisibilityProperties() {
        LinkedHashMap<String, Class<? extends MRAVisualization>> allVisualizations = PluginsRepository
                .getMraVisualizations();
        ArrayList<DefaultProperty> props = new ArrayList<DefaultProperty>();

        for (Entry<String, Class<? extends MRAVisualization>> viz : allVisualizations.entrySet()) {
            Class<?> pluginClass = viz.getValue();
            boolean visible = isVisualizationActive(pluginClass);
            // System.out.println(visible);
            DefaultProperty dp = PropertiesEditor.getPropertyInstance("visibilityOf" + pluginClass.getName(),
                    Boolean.class, visible, true);

            dp.setDisplayName(PluginUtils.getPluginName(pluginClass));
            dp.setCategory("Visualizations");
            dp.setShortDescription(PluginUtils.getPluginDescription(pluginClass));
            dp.setValue(visible);
            props.add(dp);
        }

        java.util.Collections.sort(props, new Comparator<DefaultProperty>() {
            @Override
            public int compare(DefaultProperty o1, DefaultProperty o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });

        return props;
    }

    public enum depthEntities {
        CTD,
        Depth_Sensor
    }

    @Override
    public DefaultProperty[] getProperties() {
        Vector<DefaultProperty> props = new Vector<DefaultProperty>();
        props.addAll(Arrays.asList(PluginUtils.getPluginProperties(this)));
        props.addAll(getVisibilityProperties());
        return props.toArray(new DefaultProperty[0]);
    }

    public void save() throws Exception {
        Properties props = PluginUtils.saveProperties(this, false);

        for (DefaultProperty dp : getVisibilityProperties())
            props.put(dp.getName(), "" + dp.getValue());
        props.store(new FileOutputStream(new File("conf/mra.properties")), "File generated by Neptus on " + new Date());
    }

    private void load(File f) throws Exception {
        Properties props = new Properties();
        if (f.canRead()) {
            props.load(new FileReader(f));
        }
        
        PluginUtils.loadProperties(props, this);
        
        for (Class<?> c : PluginsRepository.getMraVisualizations().values()) {
            String propName = "visibilityOf" + c.getName();
            if (props.containsKey(propName))
                visiblePlots.put(c, props.get(propName).equals("true"));
            else
                visiblePlots.put(c, PluginUtils.isPluginActive(c));            
        }
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
        String prefix = "visibilityOf";
        int prefixLength = prefix.length();

        for (Property p : properties) {
            if (p.getName().startsWith(prefix)) {
                try {
                    Class<?> plugin = Class.forName(p.getName().substring(prefixLength));
                    visiblePlots.put(plugin, (Boolean) p.getValue());
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "MRA Preferences";
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
}
