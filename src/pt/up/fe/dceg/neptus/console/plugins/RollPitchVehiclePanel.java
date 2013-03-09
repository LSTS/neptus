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
package pt.up.fe.dceg.neptus.console.plugins;

import javax.swing.JPanel;

import com.rickyclarkson.java.awt.layout.PercentLayout;

public class RollPitchVehiclePanel extends JPanel {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    SideVehiclePanel side;
	BackVehiclePanel back;
	
	public RollPitchVehiclePanel()
	{
		super();
		initialize();
	}
	
	public void initialize()
	{
		side = new SideVehiclePanel();
		back = new BackVehiclePanel();
		this.setLayout(new PercentLayout());
		this.add(side,new PercentLayout.Constraint(30,0,70,100));
		
		this.add(back,  new PercentLayout.Constraint(0,0,25,100));
	}
	
	public void setVehicle(String id)
	{
		side.setVehicle(id);
		back.setVehicle(id);
	}
	
	public void setRoll(float y)
	{
		back.setRoll(y);
	}
	
	public void setPitch(float y)
	{
		side.setPitch(y);
	}
	
	public void setPitchDepth(float y,float x)
	{
		side.setValues(y, x);
	}
	
	public void setSea(boolean b)
	{
		side.setSea(b);
	}

}
