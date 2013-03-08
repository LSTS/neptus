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
 * 2007/09/22
 * $Id:: VehicleTailElement.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.Color;


/**
 * @author Paulo Dias
 *
 */
public class VehicleTailElement extends ScatterPointsElement {

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
}
