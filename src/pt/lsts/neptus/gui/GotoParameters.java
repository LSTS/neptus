/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zecarlos
 * Mar 24, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author zecarlos
 *
 */
@SuppressWarnings("serial")
public class GotoParameters extends ParametersPanel implements ActionListener {

	private JPanel jPanel = null;
	private JPanel jPanel1 = null;
	private JLabel jLabel = null;
	private JButton jButton = null;
	private JLabel jLabel1 = null;
	private JFormattedTextField velocity = null;
	private JFormattedTextField radiusTolerance = null;
	private JLabel jLabel2 = null;
	private JFormattedTextField velocityTolerance = null;
	private JComboBox<?> unitsCombo = null;
	private JLabel jLabel5 = null;
	private JLabel jLabel3 = null;
	private static NumberFormat nf = NumberFormat.getNumberInstance();
	private LocationType destination;
	/**
	 * This method initializes 
	 * 
	 */
	public GotoParameters() {
		super();
		nf.setGroupingUsed(false);
		initialize();
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.setSize(398, 152);
        this.add(getJPanel(), null);
        this.add(getJPanel1(), null);
			
	}
    public String getErrors() {
        return null;
    }

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel3 = new JLabel();
			jLabel = new JLabel();
			jPanel = new JPanel();
			jLabel.setText("Destination:");
			jLabel3.setText(" Tolerance:");
			jPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Destination", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			jPanel.add(jLabel, null);
			jPanel.add(getJButton(), null);
			jPanel.add(jLabel3, null);
			jPanel.add(getRadiusTolerance(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jLabel5 = new JLabel();
			jLabel2 = new JLabel();
			jLabel1 = new JLabel();
			jPanel1 = new JPanel();
			jLabel1.setText("Velocity:");
			jLabel2.setText("  Tolerance:");
			jLabel5.setText("  Units:");
			jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Velocity", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			jPanel1.add(jLabel1, null);
			jPanel1.add(getVelocity(), null);
			jPanel1.add(jLabel2, null);
			jPanel1.add(getVelocityTolerance(), null);
			jPanel1.add(jLabel5, null);
			jPanel1.add(getUnitsCombo(), null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText(I18n.text("Change..."));
			jButton.setPreferredSize(new java.awt.Dimension(90,20));
			jButton.addActionListener(this);
		}
		return jButton;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getVelocity() {
		if (velocity == null) {
			velocity = new JFormattedTextField(nf);
			velocity.setText("0.0");
			velocity.setColumns(4);
			velocity.setToolTipText(I18n.text("The desired velocity over the trajectory"));
			velocity.addFocusListener(new SelectAllFocusListener());
		}
		return velocity;
	}
	/**
	 * This method initializes jTextField1	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getRadiusTolerance() {
		if (radiusTolerance == null) {
			radiusTolerance = new JFormattedTextField(nf);
			radiusTolerance.setText("0.0");
			radiusTolerance.setColumns(4);
			radiusTolerance.setToolTipText(I18n.text("The radius tolerance over the trajectory (in meters)"));
			radiusTolerance.addFocusListener(new SelectAllFocusListener());
		}
		return radiusTolerance;
	}
	/**
	 * This method initializes jTextField2	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getVelocityTolerance() {
		if (velocityTolerance == null) {
			velocityTolerance = new JFormattedTextField(nf);
			velocityTolerance.setColumns(4);
			velocityTolerance.setText("0.0");
			velocityTolerance.setToolTipText(I18n.text("The maximum allowed drift in the velocity"));
			velocityTolerance.addFocusListener(new SelectAllFocusListener());
		}
		return velocityTolerance;
	}
	
	public void actionPerformed(ActionEvent e) {
	    LocationType loc = LocationPanel.showLocationDialog(I18n.text("Goto destination"), getDestination(), getMissionType());
	    if (loc != null)
	        setDestination(loc);
	}
	
	
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getUnitsCombo() {
		if (unitsCombo == null) {
		    String[] units = new String[] {I18n.text("RPM"), I18n.text("m/s")};
			unitsCombo = new JComboBox<Object>(units);
			unitsCombo.setPreferredSize(new java.awt.Dimension(70,20));
		}
		return unitsCombo;
	}
	
	public String getUnits() {
	    return (String)getUnitsCombo().getSelectedItem();
	}
	
	public double getVelocityValue() {
	    return Double.parseDouble(getVelocity().getText());
	}
	
	public double getVelocityToleranceValue() {
	    return Double.parseDouble(getVelocityTolerance().getText());
	}
	
	public double getRadiusToleranceValue() {
	    return Double.parseDouble(getRadiusTolerance().getText());
	}
	
	public void setVelocityValue(double value) {
	    getVelocity().setText(String.valueOf(value));
	}
	
	public void setVelocityToleranceValue(double value) {
	    getVelocityTolerance().setText(String.valueOf(value));
	}
	
	public void setRadiusToleranceValue(double value) {
	    getRadiusTolerance().setText(String.valueOf(value));
	}
	
	public void setUnits(String units) {
	    getUnitsCombo().setSelectedItem(units);
	}
                 
	public static void main(String[] args) {
    
	    GuiUtils.testFrame(new GotoParameters(), "Teste Unitário");
	}
    public LocationType getDestination() {
        return destination;
    }
    public void setDestination(LocationType destination) {
        this.destination = destination;
    }
}  //  @jve:decl-index=0:visual-constraint="10,10"
