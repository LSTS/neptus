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
 * Author: 
 * Mar 15, 2005
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.TransponderElement;

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
	 * @return pt.lsts.neptus.gui.LocationPanel	
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
