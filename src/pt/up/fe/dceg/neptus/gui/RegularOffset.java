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
 * Mar 9, 2005
 * $Id:: RegularOffset.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zecarlos
 */
public class RegularOffset extends ParametersPanel {
    private static final long serialVersionUID = 1L;

    private static final String NORTH = I18n.text("North");
    private static final String SOUTH = I18n.text("South");
    private static final String EAST = I18n.text("East");
    private static final String WEST = I18n.text("West");
    private static final String UP = I18n.text("Up");
    private static final String DOWN = I18n.text("Down");
    
    private JTextField offsetNS = null;
	private JTextField offsetEW = null;
	private JComboBox<?> selectorNS = null;
	private JComboBox<?> selectorEW = null;
	private JComboBox<?> selectorUD = null;
	private JTextField offsetUD = null;
    private boolean editable;

	public RegularOffset() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new FlowLayout());
        this.setSize(416, 30);
        this.add(getSelectorNS(), null);
        this.add(getOffsetNS(), null);
        this.add(getSelectorEW(), null);
        this.add(getOffsetEW(), null);
        this.add(getSelectorUD(), null);
        this.add(getOffsetUD(), null);
	}

	public String getErrors() {
        try {
            Double.parseDouble(getOffsetNS().getText());
            Double.parseDouble(getOffsetEW().getText());
            Double.parseDouble(getOffsetUD().getText());
        }
        catch (Exception e) {
            return I18n.text("The entered text does not represent numeric values");
        }
        return null;
    }

	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getOffsetNS() {
		if (offsetNS == null) {
			offsetNS = new JTextField();
			offsetNS.setColumns(5);
			offsetNS.setText("0");
			offsetNS.setToolTipText("(m)");
			offsetNS.addFocusListener(new SelectAllFocusListener());
		}
		return offsetNS;
	}
	
	public char[] getSelectedOrientations() {
	    char[] orientations = new char[3];
	    
	    if (selectorNS.getSelectedItem().equals(NORTH))
	        orientations[0] = 'N';
	    else
	        orientations[0] = 'S';
	    
	    if (selectorEW.getSelectedItem().equals(EAST))
	        orientations[1] = 'E';
	    else
	        orientations[1] = 'W';
	    
	    if (selectorUD.getSelectedItem().equals(UP))
	        orientations[2] = 'U';
	    else
	        orientations[2] = 'D';
	    	    
	    return orientations;
	}
	
	
	public double getDownOffset() {
	    char[] orientations = getSelectedOrientations();
	    if (orientations[2] == 'U') {
	        return (-Double.parseDouble(getOffsetUD().getText()));
	    }
	    else
	        return Double.parseDouble(getOffsetUD().getText());
	}

	
	public double getNorthOffset() {
	    char[] orientations = getSelectedOrientations();
	    if (orientations[0] == 'N') {
	        return (Double.parseDouble(getOffsetNS().getText()));
	    }
	    else
	        return -Double.parseDouble(getOffsetNS().getText());
	}
	
	
	public double getEastOffset() {
	    char[] orientations = getSelectedOrientations();
	    if (orientations[1] == 'E') {
	        return (Double.parseDouble(getOffsetEW().getText()));
	    }
	    else {
	        return -Double.parseDouble(getOffsetEW().getText());
	    }
	}


	/**
	 * This method initializes jTextField1	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getOffsetEW() {
		if (offsetEW == null) {
			offsetEW = new JTextField();
			offsetEW.setColumns(5);
			offsetEW.setText("0");
			offsetEW.setToolTipText("(m)");
			offsetEW.addFocusListener(new SelectAllFocusListener());
		}
		return offsetEW;
	}
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getSelectorNS() {
		if (selectorNS == null) {
			selectorNS = new JComboBox<Object>(new Object[] {NORTH, SOUTH});
			selectorNS.setPreferredSize(new java.awt.Dimension(64,20));
		}
		return selectorNS;
	}
	/**
	 * This method initializes jComboBox1	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getSelectorEW() {
		if (selectorEW == null) {
			selectorEW = new JComboBox<Object>(new Object[] {EAST, WEST});
			selectorEW.setPreferredSize(new java.awt.Dimension(58,20));
		}
		return selectorEW;
	}
	/**
	 * This method initializes jComboBox2	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getSelectorUD() {
		if (selectorUD == null) {
			selectorUD = new JComboBox<Object>(new Object[] {DOWN, UP});
			selectorUD.setPreferredSize(new java.awt.Dimension(69,20));
		}
		return selectorUD;
	}
	
	
	public void setNSOffset(double value, boolean isNorthSelected) {
	    String selected = NORTH;
	    if (!isNorthSelected) {
	        selected = SOUTH;
	        value = -value;
	        if (value == 0) value = 0;
	    }
	    getOffsetNS().setText(String.valueOf(value));
	    getSelectorNS().setSelectedItem(selected);
	}
	
	
	public void setEWOffset(double value, boolean isEastSelected) {
	    String selected = EAST;
	    if (!isEastSelected) {
	        selected = WEST;
	        value = -value;
	        if (value == 0) value = 0;
	    }
	    getOffsetEW().setText(String.valueOf(value));
	    getSelectorEW().setSelectedItem(selected);
	}

	
	public void setUDOffset(double value, boolean isUpSelected) {
	    String selected = UP;
	    value = -value;
	    if (value == 0) value = 0;
	    if (!isUpSelected) {
	        selected = DOWN;
	        value = -value;
	        if (value == 0) value = 0;		    
	    }
	    getOffsetUD().setText(String.valueOf(value));
	    getSelectorUD().setSelectedItem(selected);
	}
	
	public void setLocationType(LocationType location) {
	    setUDOffset(location.getOffsetDown(), location.isOffsetUpUsed());
	    setEWOffset(location.getOffsetEast(), location.isOffsetEastUsed());
	    setNSOffset(location.getOffsetNorth(), location.isOffsetNorthUsed());
	}

	/**
	 * This method initializes jTextField2	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getOffsetUD() {
		if (offsetUD == null) {
			offsetUD = new JTextField();
			offsetUD.setColumns(5);
			offsetUD.setText("0");
			offsetUD.setToolTipText("(m)");
			offsetUD.addFocusListener(new SelectAllFocusListener());	
		}
		return offsetUD;
	}
	
	public void setEditable(boolean value) {
	    this.editable = value;
	    getSelectorNS().setEnabled(editable);
	    getSelectorEW().setEnabled(editable);
	    getSelectorUD().setEnabled(editable);
	    getOffsetNS().setEditable(editable);
	    getOffsetEW().setEditable(editable);
	    getOffsetUD().setEditable(editable);
	}
}
