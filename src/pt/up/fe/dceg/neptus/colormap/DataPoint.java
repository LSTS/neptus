/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.colormap;


public class DataPoint {
	
	double value = 0;
	//private LocationType center = new LocationType();
	public double xCoord, yCoord;
	
	public DataPoint(double xCoord, double yCoord, double value) {
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.value = value; 
	}
}
