/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 2007/09/22
 */
package pt.lsts.neptus.types.map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.LocationType;

import java.awt.Color;


/**
 * @author Paulo Dias
 *
 */
public class VehicleTailElement extends ScatterPointsElement {

    private long lastLocationTimeMillis = -1;

    public VehicleTailElement() {
        super();
    }
    
	public VehicleTailElement(MapGroup mg, MapType parentMap) {
		super(mg, parentMap);
	}

	public VehicleTailElement(MapGroup mg, MapType parentMap, Color baseColor) {
		super(mg, parentMap, baseColor);
	}	
	
	@Override
	public String getType() {
		return "Vehicle tail";
	}

    public void addPoint(LocationType loc, long timeMillis) {
        if (timeMillis <= lastLocationTimeMillis && lastLocationTimeMillis != -1) {
            // If the new point is older than the last one, ignore it
            NeptusLog.pub().trace("Received a location point with time {} older than or equal to the last one: {}",
                    timeMillis, lastLocationTimeMillis);
            return;
        }

        super.addPoint(loc);
        lastLocationTimeMillis = timeMillis;
    }

    @Override
    public void addPoint(LocationType loc) {
        super.addPoint(loc);
        lastLocationTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void clearPoints() {
        super.clearPoints();
        lastLocationTimeMillis = -1;
    }

    public long getLastLocationTimeMillis() {
        return lastLocationTimeMillis;
    }
}
