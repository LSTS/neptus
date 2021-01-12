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
 * 2010/01/18
 */
package pt.lsts.neptus.mp.templates;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zepinto
 *
 */
@PluginDescription(name="Rows Plan", author="ZP", description="Multiple tracks over a rectangular area")
public class RowsTemplate extends AbstractPlanTemplate {

	@NeptusProperty(name="Length", description="Length, in meters of the area to be surveyed")
	public double height = 100;
	
	@NeptusProperty(name="Width", description="Width, in meters of the area to be surveyed")
	public double width = 100;
	
	@NeptusProperty(name="Number of Rows", description="The number of tracks to be generated. Distance between rows will be (Width/NumberOfRows)")
	public int numRows = 5;
	
	@NeptusProperty(name="Depths", description="Enter the various depths to scan, separated by commas.")
	public String depths = "1,2,3";
	
	@NeptusProperty(name="Start location", description="The place to start from")
	public LocationType loc = new LocationType();
	
	
	@Override
	public DefaultProperty[] getProperties() {
		if (loc.getDistanceInMeters(LocationType.ABSOLUTE_ZERO) == 0 && mission != null)
			loc.setLocation(IMCUtils.lookForStartPosition(mission));			
		return super.getProperties();
	}
	
	private double[] parseDepths() throws Exception {
		String[] parts = depths.split("[ ,]");
		double[] depths = new double[parts.length];
		for (int i = 0; i < parts.length; i++) {
			depths[i] = Double.parseDouble(parts[i]);
		}
		
		return depths;
	}
	
	protected static final int DIR_YUP = 0, DIR_X1 = 1, DIR_YDOWN = 2, DIR_X2 = 3;
	
	public PlanType generatePlan() throws Exception {
		double depths[] = parseDepths();
		PlanCreator planCreator = new PlanCreator(mission);
		LocationType l = new LocationType(loc);
		planCreator.setLocation(l);
		planCreator.setZ(depths[0], Z_UNITS.DEPTH);
		planCreator.addGoto(null);
		int direction = DIR_X2;
		int sign = 1;
		for (int i = 0; i < depths.length; i++) {
			planCreator.setZ(depths[i], Z_UNITS.DEPTH);
			double x = 0;
			boolean done = false;
			while(!done) {		
				direction = (++direction)%4;				
				switch (direction) {
				case DIR_YUP:
					planCreator.move(width, 0);
					planCreator.addGoto(null);
					break;
				case DIR_YDOWN:
					planCreator.move(-width, 0);
					planCreator.addGoto(null);
					break;
				default:
				case DIR_X1:
				case DIR_X2:						
					x += height/numRows;
					if (x > height) {
						done = true;
						direction--;
						break;
					}
					planCreator.move(0, (height/numRows) * sign);
					planCreator.addGoto(null);					
					break;				
				}				
			}
			sign = -sign;			
		}		

		return planCreator.getPlan();
	}
}
