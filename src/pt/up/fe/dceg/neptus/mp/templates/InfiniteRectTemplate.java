/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 2010/01/18
 * $Id:: InfiniteRectTemplate.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.mp.templates;

import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;

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
			System.out.println(plan.asXML());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
