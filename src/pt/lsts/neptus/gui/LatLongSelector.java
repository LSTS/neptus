/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * 23/Fev/2005
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
/**
 * @author Ze Carlos
 * @author Paulo Dias
 */
public class LatLongSelector extends ParametersPanel implements KeyListener {

	private static final long serialVersionUID = 1L;
	private static final int DECIMAL_DEGREES_PRECISION = 8;
	public static final short DECIMAL_DEGREES_DISPLAY = 0;
	public static final short DM_DISPLAY = 1;
	public static final short DMS_DISPLAY = 2;

	private JPanel dmsPanel = null;
	private JLabel jLabel = null;
	private NumberFormat df = GuiUtils.getNeptusDecimalFormat();

	private JFormattedTextField latDeg = null;
	private JFormattedTextField latMin = null;
	private JFormattedTextField latSec = null;
	private JLabel jLabel1 = null;
	private JFormattedTextField lonDeg = null;
	private JFormattedTextField lonMin = null;
	private JFormattedTextField lonSec = null;
	private JLabel jLabel2 = null;
	private JLabel jLabel3 = null;
	private JLabel jLabel4 = null;
	private JLabel jLabel5 = null;
	private JLabel jLabel6 = null;
	private JLabel jLabel7 = null;
	private JTextField latDecDegrees = null;
	private JTextField lonDecDegrees = null;
    private boolean editable;
	private JPanel convPanel = null;
	private JRadioButton ddegreesRadioButton = null;
	private JRadioButton dmRadioButton = null;
	private JRadioButton dmsRadioButton = null;

    public LatLongSelector() {
		super();
		initialize();
	}

	/**
	 * This method initializes decDegreesPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getDMSPanel() {
		if (dmsPanel == null) {
			jLabel = new JLabel();
			jLabel7 = new JLabel();
			jLabel6 = new JLabel();
			jLabel5 = new JLabel();
			jLabel4 = new JLabel();
			jLabel3 = new JLabel();
			jLabel2 = new JLabel();
			jLabel1 = new JLabel();
			dmsPanel = new JPanel();
			dmsPanel.setLayout(null);
			jLabel.setBounds(10, 24, 59, 20);
			jLabel.setText(I18n.text("Latitude:"));
			jLabel1.setBounds(10, 84, 90, 20);
			jLabel1.setText(I18n.text("Longitude:"));
			jLabel2.setBounds(167, 109, 10, 20);
			jLabel2.setText("'");
			jLabel2.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel2.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel3.setBounds(252, 109, 10, 20);
			jLabel3.setText("''");
			jLabel3.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel4.setBounds(81, 109, 10, 20);
			jLabel4.setText(""+CoordinateUtil.CHAR_DEGREE);
			jLabel4.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel4.setFont(new Font("Dialog", Font.BOLD, 14));
			jLabel5.setBounds(81, 49, 10, 20);
			jLabel5.setText(""+CoordinateUtil.CHAR_DEGREE);
			jLabel5.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel5.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel5.setFont(new Font("Dialog", Font.BOLD, 14));
			jLabel6.setBounds(167, 49, 10, 20);
			jLabel6.setText("'");
			jLabel6.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel6.setHorizontalAlignment(SwingConstants.CENTER);
			jLabel7.setBounds(252, 49, 10, 20);
			jLabel7.setText("''");
			jLabel7.setHorizontalTextPosition(SwingConstants.CENTER);
			jLabel7.setHorizontalAlignment(SwingConstants.CENTER);
			dmsPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Degrees/Minutes/Seconds"), TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
			dmsPanel.add(jLabel, null);
			dmsPanel.add(getLatDeg(), null);
			dmsPanel.add(getLatMin(), null);
			dmsPanel.add(getLatSec(), null);
			dmsPanel.add(jLabel1, null);
			dmsPanel.add(getLonDeg(), null);
			dmsPanel.add(getLonMin(), null);
			dmsPanel.add(getLonSec(), null);
			dmsPanel.add(jLabel2, null);
			dmsPanel.add(jLabel3, null);
			dmsPanel.add(jLabel4, null);
			dmsPanel.add(jLabel5, null);
			dmsPanel.add(jLabel6, null);
			dmsPanel.add(jLabel7, null);
			dmsPanel.add(getConvPanel(), null);
		}
		return dmsPanel;
	}

	/**
	 * @param style Use one of {@link #DECIMAL_DEGREES_DISPLAY}, {@link #DM_DISPLAY}, or
	 * {@link #DMS_DISPLAY}.
	 */
	public void setDMSStyleIndicatorTo(int style) {
	    clearConvRadioButtons();
	    switch (style) {
            case DECIMAL_DEGREES_DISPLAY:
                ddegreesRadioButton.setSelected(true);
                break;
            case DM_DISPLAY:
                dmRadioButton.setSelected(true);
                break;
            default:
                dmsRadioButton.setSelected(true);
                break;
        }
	}
	
	public String getLatitude() {
		return CoordinateUtil.dmsToLatString(
				Double.parseDouble(getLatDeg().getText()),
				Double.parseDouble(getLatMin().getText()),
				Double.parseDouble(getLatSec().getText()),
				DECIMAL_DEGREES_PRECISION);
	}
	
	public String getLongitude() {
	    return CoordinateUtil.dmsToLonString(
	    		Double.parseDouble(getLonDeg().getText()),
	    		Double.parseDouble(getLonMin().getText()),
	    		Double.parseDouble(getLonSec().getText()),
				DECIMAL_DEGREES_PRECISION);
	}
	
	public void setLatitude(double dms[]) {
		getLatDeg().setText(String.valueOf(dms[0]));
		getLatMin().setText(String.valueOf(dms[1]));
		getLatSec().setText(String.valueOf(dms[2]));
		
		if (isEditable()) {
			latMin.setEnabled(true);
			latSec.setEnabled(true);
			latMin.setBackground(Color.WHITE);
			latSec.setBackground(Color.WHITE);
		}
	}

	public void setLongitude(double dms[]) {
		getLonDeg().setText(String.valueOf(dms[0]));
		getLonMin().setText(String.valueOf(dms[1]));
		getLonSec().setText(String.valueOf(dms[2]));

		if (isEditable()) {
			lonMin.setEnabled(true);
			lonSec.setEnabled(true);
			lonMin.setBackground(Color.WHITE);
			lonSec.setBackground(Color.WHITE);
		}
	}

	/**
	 * This method initializes latDeg	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getLatDeg() {
		if (latDeg == null) {
			latDeg = new JFormattedTextField(df);
			latDeg.setBounds(10, 49, 72, 20);
			latDeg.setText("0");
			latDeg.addKeyListener(this);
			latDeg.addFocusListener(new SelectAllFocusListener());
		}
		return latDeg;
	}

	/**
	 * This method initializes latMin	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getLatMin() {
		if (latMin == null) {
			latMin = new JFormattedTextField(df);
			latMin.setBounds(96, 49, 72, 20);
			latMin.setText("0");
			latMin.addKeyListener(this);
			latMin.addFocusListener(new SelectAllFocusListener());
		}
		return latMin;
	}

	/**
	 * This method initializes latSec	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getLatSec() {
		if (latSec == null) {
			latSec = new JFormattedTextField(df);
			latSec.setBounds(181, 49, 72, 20);
			latSec.setText("0");
			latSec.addKeyListener(this);
			latSec.addFocusListener(new SelectAllFocusListener());
		}
		return latSec;
	}

	/**
	 * This method initializes lonDeg	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getLonDeg() {
		if (lonDeg == null) {
			lonDeg = new JFormattedTextField(df);
			lonDeg.setBounds(10, 109, 72, 20);
			lonDeg.setText("0");
			lonDeg.addKeyListener(this);
			lonDeg.addFocusListener(new SelectAllFocusListener());
		}
		return lonDeg;
	}

	/**
	 * This method initializes lonMin	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getLonMin() {
		if (lonMin == null) {
			lonMin = new JFormattedTextField(df);
			lonMin.setBounds(96, 109, 72, 20);
			lonMin.setText("0");
			lonMin.addKeyListener(this);
			lonMin.addFocusListener(new SelectAllFocusListener());
		}
		return lonMin;
	}

	/**
	 * This method initializes lonSec	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getLonSec() {
		if (lonSec == null) {
			lonSec = new JFormattedTextField(df);
			lonSec.setBounds(181, 109, 72, 20);
			lonSec.setText("0");
			lonSec.addKeyListener(this);
			lonSec.addFocusListener(new SelectAllFocusListener());
		}
		return lonSec;
	}

	/**
	 * This method initializes latDecDegrees	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getLatDecDegrees() {
		if (latDecDegrees == null) {
			latDecDegrees = new JTextField();
			latDecDegrees.setBounds(42, 50, 120, 20);
			latDecDegrees.setText("0");
			latDecDegrees.addKeyListener(this);
			latDecDegrees.addFocusListener(new SelectAllFocusListener());
		}
		return latDecDegrees;
	}

	/**
	 * This method initializes lonDecDegrees	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getLonDecDegrees() {
		if (lonDecDegrees == null) {
			lonDecDegrees = new JTextField();
			lonDecDegrees.setBounds(42, 110, 120, 20);
			lonDecDegrees.setText("0");
			lonDecDegrees.addKeyListener(this);
			lonDecDegrees.addFocusListener(new SelectAllFocusListener());
		}
		return lonDecDegrees;
	}

    public void keyPressed(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}
	
    public void keyReleased(KeyEvent e) {
    	if (!Character.isDefined(e.getKeyChar()))
    		return;

		// Only allow negative values in the fields for the latitude and longitude degrees.
        if (e.getSource() != latDeg && e.getSource() != lonDeg && e.getSource() != latDecDegrees
                && e.getSource() != lonDecDegrees) {
            if (e.getKeyChar() == '-') {
                JTextField src = (JTextField) e.getSource();
                src.setText(src.getText().replaceAll("-", ""));
            }
        }    	
    	
    	// If the field "degrees" is decimal deactivate the "minutes" and "seconds" fields.
    	if (e.getSource() == latDeg) {
    		if (latDeg.getText().contains(".")) {
    			latMin.setText("0");
    			latMin.setEnabled(false);
    			latSec.setText("0");
    			latSec.setEnabled(false);
    			latMin.setBackground(Color.GRAY);
    			latSec.setBackground(Color.GRAY);
    		}
    		else {
    			latMin.setEnabled(true);
    			latSec.setEnabled(true);
    			
    			latMin.setBackground(Color.WHITE);
    			latSec.setBackground(Color.WHITE);
    		}
    	}
    	
    	if (e.getSource() == lonDeg) {
    		if (lonDeg.getText().contains(".")) {
    			lonMin.setText("0");
    			lonMin.setEnabled(false);
    			lonSec.setText("0");
    			lonSec.setEnabled(false);
    			lonMin.setBackground(Color.GRAY);
    			lonSec.setBackground(Color.GRAY);
    		}
    		else {
       			lonMin.setEnabled(true);
    			lonSec.setEnabled(true);  
    			lonMin.setBackground(Color.WHITE);
    			lonSec.setBackground(Color.WHITE);
    		}
    	}
    	
    	// If the field "minutes" is decimal deactivate the "seconds" field.
    	if (e.getSource() == latMin) {
    		if (latMin.getText().contains(".")) {
    			latSec.setText("0");
    			latSec.setEnabled(false);
    			latSec.setBackground(Color.GRAY);
    		}
    		else {
    			latSec.setEnabled(true);
    			latSec.setBackground(Color.WHITE);
    		}
    	}
    	
    	if (e.getSource() == lonMin) {
    		if (lonMin.getText().contains(".")) {
    			lonSec.setText("0");
    			lonSec.setEnabled(false);
    			lonSec.setBackground(Color.GRAY);
    		}
    		else {
    			lonSec.setEnabled(true);
    			lonSec.setBackground(Color.WHITE);
    		}
    	}
    	
    	clearConvRadioButtons();
    }
    
    
	public String getErrors() {
    	int counter = 0;
    	try {
    		Double.parseDouble(getLatDeg().getText());
    		Double.parseDouble(getLatMin().getText());
    		Double.parseDouble(getLatSec().getText());
    		counter = 1;
    		Double.parseDouble(getLonDeg().getText());
    		Double.parseDouble(getLonMin().getText());
    		 Double.parseDouble(getLonSec().getText());
    	}
    	catch (Exception e) {
    		switch (counter) {
    			case (0):
    				return I18n.text("The D/M/S latitude is invalid");
    			case (1):
    				return I18n.text("The D/M/S longitude is invalid");
    			case (2):
    				return I18n.text("The Decimal Degrees Latitude is invalid");
    			case (3):
    				return I18n.text("The Decimal Degrees Longitude is invalid");
    			default:
    				return I18n.text("The Latitude/Longitude parameters are not valid");
    		}
    	}
		return null;
    }
    
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setLayout(new BorderLayout());
		this.setSize(420, 145);
		this.add(getDMSPanel(), BorderLayout.CENTER);
	}

	public void setEditable(boolean value) {
	    this.editable = value;
	    getLatDeg().setEditable(editable);
	    getLatMin().setEditable(editable);
	    getLatSec().setEditable(editable);
	    getLonDeg().setEditable(editable);
	    getLonMin().setEditable(editable);
	    getLonSec().setEditable(editable);
	    getLatDecDegrees().setEditable(editable);
	    getLonDecDegrees().setEditable(editable);
	    
	    getConvPanel().setEnabled(editable);
	    getDdegreesRadioButton().setEnabled(editable);
	    getDmRadioButton().setEnabled(editable);
	    getDmsRadioButton().setEnabled(editable);
	}
	
	/**
	 * This method initializes convPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getConvPanel() {
		if (convPanel == null) {
			convPanel = new JPanel();
			convPanel.setLayout(new BoxLayout(getConvPanel(), BoxLayout.Y_AXIS));
            convPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Lat/Lon Display"),
                    TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                    new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			convPanel.setBounds(new Rectangle(261, 30, 149, 98));
			convPanel.add(getDdegreesRadioButton(), null);
			convPanel.add(getDmRadioButton(), null);
			convPanel.add(getDmsRadioButton(), null);
		}
		return convPanel;
	}

	/**
	 * This method initializes ddegreesRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getDdegreesRadioButton() {
		if (ddegreesRadioButton == null) {
			ddegreesRadioButton = new JRadioButton();
			ddegreesRadioButton.setText(I18n.text("Decimal Degrees"));
			ddegreesRadioButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    dmRadioButton.setSelected(false);
                    dmsRadioButton.setSelected(false);
                    convertLatLonTo(DECIMAL_DEGREES_DISPLAY);
                }
            });
		}
		return ddegreesRadioButton;
	}

	/**
	 * This method initializes dmRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getDmRadioButton() {
		if (dmRadioButton == null) {
			dmRadioButton = new JRadioButton();
			dmRadioButton.setText(I18n.text("Degrees, Minutes"));
			dmRadioButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ddegreesRadioButton.setSelected(false);
                    dmsRadioButton.setSelected(false);
                    convertLatLonTo(DM_DISPLAY);
                }
            });
		}
		return dmRadioButton;
	}

	/**
	 * This method initializes dmsRadioButton	
	 * 	
	 * @return javax.swing.JRadioButton	
	 */
	private JRadioButton getDmsRadioButton() {
		if (dmsRadioButton == null) {
			dmsRadioButton = new JRadioButton();
			dmsRadioButton.setHorizontalAlignment(SwingConstants.LEADING);
			/// DMS = Degrees, Minutes, Seconds
			dmsRadioButton.setText(I18n.text("DMS"));
			dmsRadioButton.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ddegreesRadioButton.setSelected(false);
                    dmRadioButton.setSelected(false);
                    convertLatLonTo(DMS_DISPLAY);
                }
            });
		}
		return dmsRadioButton;
	}

    /**
     * 
     */
    private void clearConvRadioButtons() {
		ddegreesRadioButton.setSelected(false);
		dmRadioButton.setSelected(false);
		dmsRadioButton.setSelected(false);
	}

	/**
	 * @param type
	 */
	protected boolean convertLatLonTo(short type) {
		LocationType loc = new LocationType();
		loc.setLatitudeStr(getLatitude());
		loc.setLongitudeStr(getLongitude());
		switch (type) {
    		case DECIMAL_DEGREES_DISPLAY:
    			this.setLatitude(new double[] {MathMiscUtils.round(loc.getLatitudeDegs(), 6), 0, 0});
    			this.setLongitude(new double[] {MathMiscUtils.round(loc.getLongitudeDegs(), 6), 0, 0});
    			break;
    
    		case DM_DISPLAY:
    			double[] dmLat = CoordinateUtil.decimalDegreesToDM(loc.getLatitudeDegs());
    			double[] dmLon = CoordinateUtil.decimalDegreesToDM(loc.getLongitudeDegs());
    			this.setLatitude(new double[] {dmLat[0], MathMiscUtils.round(dmLat[1], 4), 0});
    			this.setLongitude(new double[] {dmLon[0], MathMiscUtils.round(dmLon[1], 4), 0});
    			break;
    
    		case DMS_DISPLAY:
    			double[] dmsLat = CoordinateUtil.decimalDegreesToDMS(loc.getLatitudeDegs());
    			double[] dmsLon = CoordinateUtil.decimalDegreesToDMS(loc.getLongitudeDegs());
    			this.setLatitude(new double[] {dmsLat[0], dmsLat[1], MathMiscUtils.round(dmsLat[2], 2)});
    			this.setLongitude(new double[] {dmsLon[0], dmsLon[1], MathMiscUtils.round(dmsLon[2], 2)});
    			break;
    
    		default:
    			break;
		}

		return true;
	}
}
