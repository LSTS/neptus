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
 * Author: José Pinto
 * 2005/02/3
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JOptionPane;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Zé Carlos
 * @author pdias
 */
public class VehicleChooser {

	public static VehicleType showVehicleDialog(Vector<VehicleType> forbiddenVehicles, VehicleType initialSelection, Component parentComponent) {
	    List<String> vehicles = new ArrayList<>();
	    if (forbiddenVehicles == null)
	        forbiddenVehicles = new Vector<>();
	        
	    for (Entry<String, VehicleType> entry : VehiclesHolder.getVehiclesList().entrySet()) {
	        if (!forbiddenVehicles.contains(entry.getValue())) 
	            vehicles.add(entry.getKey());
	    }
	    
	    if (vehicles.isEmpty())
	        return null;
	    
	    if (vehicles.size() == 1)
	        return VehiclesHolder.getVehicleById(vehicles.get(0));
	    
	    Collections.sort(vehicles);
        Object ret = JOptionPane.showInputDialog(parentComponent, I18n.text("Select vehicle"),
                I18n.text("Select vehicle"), JOptionPane.QUESTION_MESSAGE, null, vehicles.toArray(new String[0]),
                initialSelection != null ? initialSelection.getId() : vehicles.iterator().next());
	    if (ret == null)
	        return null;
	    return VehiclesHolder.getVehicleById(""+ret);
	}

	static void main(String[] args) {
	    ConfigFetch.initialize();
      	VehicleChooser.showVehicleDialog(null, null, null);
	}
}