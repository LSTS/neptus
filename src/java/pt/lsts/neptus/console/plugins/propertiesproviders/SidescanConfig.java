/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Mar 19, 2013
 */
package pt.lsts.neptus.console.plugins.propertiesproviders;

import java.awt.Color;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author jqcorreia
 *
 */
public class SidescanConfig implements PropertiesProvider {
    
    @NeptusProperty (name="Apply slant range correction (cross path)", category="Visualization parameters")
    public boolean slantRangeCorrection = false;

    @NeptusProperty (name="Apply speed correction (along path)", category="Visualization parameters")
    public boolean speedCorrection = false;
    
    //@NeptusProperty (name="Apply time variable gain", category="Visualization parameters")
    //public boolean timeVariableGain = false;
    
    @NeptusProperty (name="Color map to use", category="Visualization parameters")
    public ColorMap colorMap = ColorMapFactory.createBronzeColormap();
    
    @NeptusProperty (name="Normalization factor", category="Visualization parameters")
    public double normalization = 0.2;
    
    @NeptusProperty (name="Time Variable Gain factor", category="Visualization parameters")
    public double tvgGain = 280;

    @NeptusProperty (name="Slice Minimum Value", category="Visualization parameters",
            description = "Trim values between this minimum and the window. Values in [0.0; 1.0].")
    public double sliceMinValue = 0.0;

    @NeptusProperty (name="Slice Window Size", category="Visualization parameters",
            description = "Trim values between minimum and this window (max = min + window). Values in [0.0; 1.0].")
    public double sliceWindowValue = 1.0;

    @NeptusProperty (name="Display Vehicle Path", category="Vehicle Path")
    public boolean showPositionHud = true;
    
    @NeptusProperty (name="Size of Vehicle Path Display", category="Vehicle Path")
    public int hudSize = 200;
    
    @NeptusProperty (name="Path display color", category="Vehicle Path")
    public Color pathColor = Color.WHITE;
    
    
    {
        loadProps();
    }
    
    protected void loadProps() {
        try {
            PluginUtils.loadProperties("conf/sidescan.properties", this);
            validateValues();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void validateValues() {
        sliceMinValue = Math.min(1, Math.max(0, sliceMinValue));
        sliceWindowValue = Math.min(1 - sliceMinValue, Math.max(0, sliceWindowValue));
    }

    public void saveProps() {
        try {
            PluginUtils.saveProperties("conf/sidescan.properties", this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Properties
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
        validateValues();
    }

    @Override
    public String getPropertiesDialogTitle() {
        return I18n.textf("%plugin parameters", PluginUtils.getPluginName(this.getClass()));
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }
}