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
 * 9/03/2005
 */
package pt.lsts.neptus.gui;

import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.MissionType;

/**
 * @author zecarlos
 * @author pdias
 */
public class MarkSelector extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JLabel jLabel = null;
	private JComboBox<AbstractElement> marksCombo = null;
	private MissionType missionType = null;
	
	public MarkSelector() {
	    super();
	    initialize();
	}
	
	/**
	 * This method initializes 
	 * 
	 */
	public MarkSelector(MissionType mt) {
		super();
		setMissionType(mt);
		initialize();
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        jLabel = new JLabel();
        this.setSize(217, 60);
        jLabel.setText(I18n.text("Choose an existing Mark:"));
        this.add(jLabel, null);
        this.add(getMarksCombo(), null);
        initializeMarksCombo();
	}
	
	/**
	 * @return the selected location or null if none selected.
	 */
	public LocationType getLocationType() {
	    try {
            return ((MarkElement) marksCombo.getSelectedItem()).getCenterLocation();
        }
        catch (Exception e) {
            return null;
        }
	}
	
	public void initializeMarksCombo() {
	    marksCombo = getMarksCombo();

	    marksCombo.removeAllItems();
	    
	    MapGroup mg = MapGroup.getMapGroupInstance(getMissionType());
	    Object[] objs = mg.getAllObjects();	    
	    NeptusLog.pub().debug(this + "Number of objects: " + objs.length);
//	    if (objs.length > 0)
//	        marksCombo.addItem("");
		for (int i = 0; i < objs.length; i++) {		    
			AbstractElement obj = (AbstractElement) objs[i];
            // NeptusLog.pub().error(this + "Testing the object " + obj.getId()
            // + "which is a " + obj.getType());
			
			if (obj.getType().equals("Mark")) {
			    marksCombo.addItem(obj);
			}
		}
	}
	
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<AbstractElement> getMarksCombo() {
		if (marksCombo == null) {
			marksCombo = new JComboBox<AbstractElement>();
			marksCombo.setPreferredSize(new java.awt.Dimension(130,24));
		}
		return marksCombo;
	}
	public MissionType getMissionType() {
		if (missionType == null) {
			missionType = new MissionType();
			missionType.setId("dummy_mission");
		}
		return missionType;
	}
	public void setMissionType(MissionType missionType) {
		this.missionType = missionType;
		initializeMarksCombo();
	}
	
	public void addMarksComboListener(MarksComboListener mcl) {
		final MarksComboListener markListener = mcl;
		marksCombo.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(java.awt.event.ActionEvent arg0) {
				try {
                    MarkElement mobj = (MarkElement) marksCombo.getSelectedItem(); 
                    markListener.MarkComboChanged(mobj);
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                }
			};
		});
	}
 }  //  @jve:decl-index=0:visual-constraint="10,10"

abstract class MarksComboListener {
	public abstract void MarkComboChanged(MarkElement selectedMarkObject);
}
