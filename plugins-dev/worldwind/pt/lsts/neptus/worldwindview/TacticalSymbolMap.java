/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * Dec 6, 2016
 */
package pt.lsts.neptus.worldwindview;

import java.util.HashMap;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.symbology.BasicTacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.SymbologyConstants;
import gov.nasa.worldwind.symbology.TacticalSymbol;
import gov.nasa.worldwind.symbology.TacticalSymbolAttributes;
import gov.nasa.worldwind.symbology.milstd2525.MilStd2525TacticalSymbol;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author Manuel R.
 *
 */
public class TacticalSymbolMap {
    private HashMap<String, TacticalSymbol> systemSymbolMap = new HashMap<>();
    private TacticalSymbolAttributes sharedAttrs;
    private TacticalSymbolAttributes sharedHighlightAttrs;
    
    public TacticalSymbolMap() {
        this.sharedAttrs = new BasicTacticalSymbolAttributes();
        this.sharedHighlightAttrs = new BasicTacticalSymbolAttributes();
    }
    
    /**
     * @return the systemSymbolMap
     */
    public HashMap<String, TacticalSymbol> getSystemSymbol() {
        return systemSymbolMap;
    }

    /**
     * @param systemSymbolMap the systemSymbolMap to set
     */
    public void setSystemSymbol(HashMap<String, TacticalSymbol> systemSymbolMap) {
        this.systemSymbolMap = systemSymbolMap;
    }
    
    public TacticalSymbol addSystem(String sysName, VehicleType.VehicleTypeEnum vehType, LocationType loc, double heading) {
        String mil2525String = VehicleType.getMilStd2525TypeString(vehType, VehicleType.IdentityTypeEnum.FRIEND);
        TacticalSymbol symbol = new MilStd2525TacticalSymbol(mil2525String, Position.fromDegrees(loc.getLatitudeDegs(), loc.getLongitudeDegs(), loc.getHeight()));
        symbol.setValue(AVKey.DISPLAY_NAME, sysName); //FIXME tool tip not working
        symbol.setModifier(SymbologyConstants.DIRECTION_OF_MOVEMENT, Angle.fromDegrees(heading));
        symbol.setModifier(SymbologyConstants.ADDITIONAL_INFORMATION, sysName);
        symbol.setHighlightAttributes(this.sharedHighlightAttrs);
        symbol.setAttributes(this.sharedAttrs);
        
        systemSymbolMap.put(sysName, symbol);
        
        return symbol;
    }
    
    public void setIconScale(double size) {
        sharedAttrs.setScale(size);
        sharedHighlightAttrs.setScale(size);
    }
    
    public void updateSysPosAndHeading(String sysName, double heading, Position pos) {
        TacticalSymbol ts = systemSymbolMap.get(sysName);
        
        //update heading
        ts.setModifier(SymbologyConstants.DIRECTION_OF_MOVEMENT, Angle.fromDegrees(heading));
        
        //update position
        ts.setPosition(pos);
    }
}
