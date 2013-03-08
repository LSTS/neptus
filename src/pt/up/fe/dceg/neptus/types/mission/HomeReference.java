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
 * 16/Jan/2005
 * $Id:: HomeReference.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.types.mission;

import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;

/**
 * @author Paulo
 *
 */
public class HomeReference extends CoordinateSystem
{

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private MissionType mission = null;
	
    /**
     * 
     */
    public HomeReference()
    {
        super();
        this.id = "home";
        this.name = "home";
    }

    public HomeReference(String xml)
    {
        super(xml);
        this.id = "home";
        this.name = "home";
        //this.coordinate = coord;
    }

	public MissionType getMission() {
		return mission;
	}

	public void setMission(MissionType mission) {
		this.mission = mission;
	}

}
