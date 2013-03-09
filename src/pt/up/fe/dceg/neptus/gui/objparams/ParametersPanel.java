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
 * 15/Fev/2005
 */
package pt.up.fe.dceg.neptus.gui.objparams;


import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.types.mission.MissionType;

import com.l2fprod.common.propertysheet.Property;

/**
 * @author Ze Carlos
 */
public abstract class ParametersPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public abstract String getErrors();
	private boolean editable = true;
	private MissionType missionType = new MissionType();
	
	public ParametersPanel() {
		super();
	}
	
	public void setEditable(boolean value) {
		this.editable = value;
	}
	public boolean isEditable() {
		return editable;
	}
	public MissionType getMissionType() {
		return missionType;
	}
	public void setMissionType(MissionType missionType) {
		this.missionType = missionType;
	}
	
	public static CustomParametersPanel getCustomPanel(Property[] properties) {
		return new CustomParametersPanel(properties);
	}
}
