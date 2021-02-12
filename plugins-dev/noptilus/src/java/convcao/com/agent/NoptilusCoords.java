/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 6, 2013
 */
package convcao.com.agent;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * This class is used to translate between noptilus-specific world coordinates and real-world coordinates
 * Properties for this conversion are stored in "conf/noptcoords.properties"
 * @author zp
 */
public class NoptilusCoords implements PropertiesProvider {
    
    @NeptusProperty(name="Area Center") 
    public LocationType squareCenter = new LocationType();
    
    @NeptusProperty(name="Cell size", description="Cell size in meters for each map square")
    public double cellWidth = 1;
    
    @NeptusProperty(name="Number of Rows", description="Number of rows in the area rectangle") 
    public double numRows = 240;
    
    @NeptusProperty(name="Number of Columns", description="Number of columns in the area rectangle") 
    public double numCols = 240;
    
    @NeptusProperty(name="Maximum Depth", description="Number of depth units to use for the Noptilus map") 
    public double maxDepth = 50;    
    
    {
        loadProps();
    }
    
    protected void loadProps() {
        try {
            PluginUtils.loadProperties("conf/noptcoords.properties", this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void saveProps() {
        try {
            PluginUtils.saveProperties("conf/noptcoords.properties", this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public LocationType convert(double row, double col) {
        
        if (col < 0)
            col = 0;
        if (col >= numCols)
            col = numCols - 0.1;
        
        if (row < 0)
            row = 0;
        if (row >= numRows)
            row = numRows - 0.1;
        
        LocationType loc = new LocationType(squareCenter);
        double transN = ((-numRows/2) + row) * cellWidth;
        double transE = ((-numCols/2) + col) * cellWidth;
        loc.translatePosition(transN, transE, 0);
        
        return loc;        
    }
    
    //FIXME
    public double convertWgsDepthToNoptilusDepth(double depth) {
        return maxDepth - depth;
    }
    
    //FIXME
    public double convertNoptilusDepthToWgsDepth(double depth) {
        return maxDepth - depth;
    }
    
    public double[] convert(LocationType loc) {
        LocationType sw = new LocationType(squareCenter);
        sw.translatePosition(-cellWidth * numRows/2, -cellWidth * numCols/2, 0);
        double[] offsets = loc.getOffsetFrom(sw);
        offsets[0] /= cellWidth;
        offsets[1] /= cellWidth;
        if (offsets[0] < 0)
            offsets[0] = 0;
        
        if (offsets[0] > numRows-1)
            offsets[0] = numRows-1;
        if (offsets[1] < 0)
            offsets[1] = 0;
        if (offsets[1] > numCols-1)
            offsets[1] = numCols-1;
        
        return new double[] {offsets[0],offsets[1]};
    }
    
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }
    
    @Override
    public String getPropertiesDialogTitle() {
        return "Noptilus Coordinate System properties";
    }
    
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }
    
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }
    
    public static void main(String[] args) {
        NoptilusCoords coords = new NoptilusCoords();
        PluginUtils.editPluginProperties(coords, true);
        coords.saveProps();
        System.out.println(coords.convert(20, 21));
        System.out.println(coords.convert(new LocationType(41,  -7.999941))[1]);
    }
}