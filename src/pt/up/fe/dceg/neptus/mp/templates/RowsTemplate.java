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
 */
package pt.up.fe.dceg.neptus.mp.templates;

import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;

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
