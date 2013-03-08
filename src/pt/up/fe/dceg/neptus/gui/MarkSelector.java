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
 * 9/03/2005
 * $Id:: MarkSelector.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;

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
            NeptusLog.pub().error(this + "Testing the object " + obj.getId()
                    + "which is a " + obj.getType());
			
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
