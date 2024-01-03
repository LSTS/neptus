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
 * 2010/01/18
 */
package pt.lsts.neptus.mp.templates;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zepinto
 *
 */
@PluginDescription(name="Infinite Rectangle", author="ZP", description="Follows a rectangle forever, optionally having multiple depths")
public class InfiniteRectTemplate extends AbstractPlanTemplate {

	@NeptusProperty(name="Width", description="Width of the rectangle, in meters")
	public double width = 50;

	@NeptusProperty(name="Height", description="Height of the rectangle, in meters")
	public double height = 50;

	
	@NeptusProperty(name="Depths", description="Enter the various depths, separated by commas")
	public String depths = "0,1";
	
	@NeptusProperty(name="Start location", description="The place to start from")
	public LocationType loc = new LocationType();

	@NeptusProperty(name="Is infinite", description="Set to true links the last maneuver to the first creating a cicled plan")
	public boolean isInfinite = false;

	
	@Override
	public DefaultProperty[] getProperties() {
		if (loc.getDistanceInMeters(LocationType.ABSOLUTE_ZERO) == 0 && mission != null)
			loc.setLocation(IMCUtils.lookForStartPosition(mission));			
		return super.getProperties();
	}
	
	private double[] parseDepths() throws Exception {
		String[] parts = depths.split("[, ]");
		double[] depths = new double[parts.length];
		for (int i = 0; i < parts.length; i++) {
			depths[i] = Double.parseDouble(parts[i]);
		}
		
		return depths;
	}
	
	public PlanType generatePlan() throws Exception {
		double depths[] = parseDepths();
		LocationType l = new LocationType(loc);
		PlanCreator planCreator = new PlanCreator(mission);		
		String lastId = "1";
		
		for (int i = 0; i < depths.length; i++) {
			l.setAbsoluteDepth(depths[i]);
			planCreator.setLocation(l);
			planCreator.setZ(depths[i], Z_UNITS.DEPTH);
			planCreator.addGoto(null);
			planCreator.move(width, 0);
			planCreator.addGoto(null);
			planCreator.move(0, height);
			planCreator.addGoto(null);
			planCreator.move(-width, 0);
			lastId = planCreator.addGoto(null);			
		}		
		PlanType plan = planCreator.getPlan();
		if (isInfinite)
			plan.getGraph().addTransition(lastId, "1", "true");						
		
		return plan;
	}
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		InfiniteRectTemplate sq = new InfiniteRectTemplate();
		sq.loc = new LocationType();
		sq.loc.translatePosition(100, 100, 20);
		sq.depths = "0";
		sq.width = 40;
		
		try {
			PlanType plan = sq.generatePlan();
			NeptusLog.pub().info("<###> "+plan.asXML());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
