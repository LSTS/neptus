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
 * Mar 15, 2005
 * $Id:: MarkParameters.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;

/**
 * @author zecarlos
 *
 */
public class MarkParameters extends ParametersPanel {

	private static final long serialVersionUID = -1806787087506152079L;

	private LocationPanel locationPanel = null;
	/**
	 * This method initializes 
	 * 
	 */
	public MarkParameters() {
		super();
		initialize();
		setPreferredSize(new Dimension(450,480));
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new BorderLayout());
        this.setBounds(0, 0, 438, 375);
        this.add(getLocationPanel(), java.awt.BorderLayout.CENTER);
        getLocationPanel().setMissionType(getMissionType());
		
	}
    public String getErrors() {
        
        if (getLocationPanel().getErrors() != null)
            return getLocationPanel().getErrors();
        
        return null;
    }
    
    public void setLocation(LocationType location) {
    	getLocationPanel().setLocationType(location);
    }

	/**
	 * This method initializes locationPanel	
	 * 	
	 * @return pt.up.fe.dceg.neptus.gui.LocationPanel	
	 */    
	public LocationPanel getLocationPanel() {
		if (locationPanel == null) {
			locationPanel = new LocationPanel(getMissionType());
			locationPanel.hideButtons();
			locationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
			
		}
		return locationPanel;
	}
	public static void main(String[] args) {
    
	    JFrame testFrame = new JFrame("Teste Unitário");
	    MarkParameters nmp = new MarkParameters();
	    testFrame.add(nmp);
	    testFrame.setSize(453, 450);
	    testFrame.setVisible(true);
	    
	    TransponderElement te = new TransponderElement();
	    te.showParametersDialog(null, new String[0], null, true);
	
	}
	
	public void setEditable(boolean value) {
		super.setEditable(value);
		locationPanel.setEditable(isEditable());
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
