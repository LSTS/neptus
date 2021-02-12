/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
	private JLabel latitudeStrLabel = null;
	private NumberFormat df = GuiUtils.getNeptusDecimalFormat();

	private JFormattedTextField latDeg = null;
	private JFormattedTextField latMin = null;
	private JFormattedTextField latSec = null;
	private JLabel longitudeStrLabel = null;
	private JFormattedTextField lonDeg = null;
	private JFormattedTextField lonMin = null;
	private JFormattedTextField lonSec = null;
	private JLabel longitudeMinutesStrLabel = null;
	private JLabel longitudeSecondsStrLabel = null;
	private JLabel longitudeDegsStrLabel = null;
	private JLabel latitudeDegsStrLabel = null;
	private JLabel latitudeMinutesStrLabel = null;
	private JLabel latitudeSecondsStrLabel = null;
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
			latitudeStrLabel = new JLabel();
			latitudeSecondsStrLabel = new JLabel();
			latitudeMinutesStrLabel = new JLabel();
			latitudeDegsStrLabel = new JLabel();
			longitudeDegsStrLabel = new JLabel();
			longitudeSecondsStrLabel = new JLabel();
			longitudeMinutesStrLabel = new JLabel();
			longitudeStrLabel = new JLabel();
			dmsPanel = new JPanel();
			dmsPanel.setLayout(null);
			latitudeStrLabel.setBounds(10, 24, 59, 20);
			latitudeStrLabel.setText(I18n.text("Latitude:"));
			longitudeStrLabel.setBounds(10, 84, 90, 20);
			longitudeStrLabel.setText(I18n.text("Longitude:"));
			latitudeDegsStrLabel.setBounds(91+23, 49, 10, 20);
			latitudeDegsStrLabel.setText(""+CoordinateUtil.CHAR_DEGREE);
			latitudeDegsStrLabel.setHorizontalTextPosition(SwingConstants.CENTER);
			latitudeDegsStrLabel.setHorizontalAlignment(SwingConstants.CENTER);
			latitudeDegsStrLabel.setFont(new Font("Dialog", Font.BOLD, 14));
			latitudeMinutesStrLabel.setBounds(177+23+10, 49, 10, 20);
			latitudeMinutesStrLabel.setText("'");
			latitudeMinutesStrLabel.setHorizontalTextPosition(SwingConstants.CENTER);
			latitudeMinutesStrLabel.setHorizontalAlignment(SwingConstants.CENTER);
			latitudeSecondsStrLabel.setBounds(252+23+10+10, 49, 10, 20);
			latitudeSecondsStrLabel.setText("''");
			latitudeSecondsStrLabel.setHorizontalTextPosition(SwingConstants.CENTER);
			latitudeSecondsStrLabel.setHorizontalAlignment(SwingConstants.CENTER);
            longitudeDegsStrLabel.setBounds(91+23, 109, 10, 20);
            longitudeDegsStrLabel.setText(""+CoordinateUtil.CHAR_DEGREE);
            longitudeDegsStrLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            longitudeDegsStrLabel.setHorizontalAlignment(SwingConstants.CENTER);
            longitudeDegsStrLabel.setFont(new Font("Dialog", Font.BOLD, 14));
            longitudeMinutesStrLabel.setBounds(177+23+10, 109, 10, 20);
            longitudeMinutesStrLabel.setText("'");
            longitudeMinutesStrLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            longitudeMinutesStrLabel.setHorizontalAlignment(SwingConstants.CENTER);
            longitudeSecondsStrLabel.setBounds(252+23+10+10, 109, 10, 20);
            longitudeSecondsStrLabel.setText("''");
            longitudeSecondsStrLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            longitudeSecondsStrLabel.setHorizontalAlignment(SwingConstants.CENTER);
			dmsPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Degrees/Minutes/Seconds"), TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, null, null));
			dmsPanel.add(latitudeStrLabel, null);
			dmsPanel.add(getLatDeg(), null);
			dmsPanel.add(latitudeDegsStrLabel, null);
			dmsPanel.add(getLatMin(), null);
			dmsPanel.add(latitudeMinutesStrLabel, null);
			dmsPanel.add(getLatSec(), null);
			dmsPanel.add(latitudeSecondsStrLabel, null);
			dmsPanel.add(longitudeStrLabel, null);
			dmsPanel.add(getLonDeg(), null);
			dmsPanel.add(longitudeDegsStrLabel, null);
			dmsPanel.add(getLonMin(), null);
			dmsPanel.add(longitudeMinutesStrLabel, null);
			dmsPanel.add(getLonSec(), null);
			dmsPanel.add(longitudeSecondsStrLabel, null);
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
		getLatDeg().setText(String.valueOf(dms[0]).replaceAll("\\.0$", ""));
		getLatMin().setText(String.valueOf(dms[1]).replaceAll("\\.0$", ""));
		getLatSec().setText(String.valueOf(dms[2]).replaceAll("\\.0$", ""));
		
		if (isEditable()) {
			latMin.setEnabled(true);
			latSec.setEnabled(true);
			latMin.setBackground(Color.WHITE);
			latSec.setBackground(Color.WHITE);
		}
	}

	public void setLongitude(double dms[]) {
		getLonDeg().setText(String.valueOf(dms[0]).replaceAll("\\.0$", ""));
		getLonMin().setText(String.valueOf(dms[1]).replaceAll("\\.0$", ""));
		getLonSec().setText(String.valueOf(dms[2]).replaceAll("\\.0$", ""));

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
			latDeg.setBounds(10, 49, 105, 20);
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
			latMin.setBounds(129, 49, 82, 20);
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
			latSec.setBounds(191+23+10, 49, 72, 20);
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
			lonDeg.setBounds(10, 109, 105, 20);
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
			lonMin.setBounds(129, 109, 82, 20);
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
			lonSec.setBounds(191+23+10, 109, 72, 20);
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
    		}
    		else {
    			latMin.setEnabled(true);
    			latSec.setEnabled(true);
    		}
    	}
    	
    	if (e.getSource() == lonDeg) {
    		if (lonDeg.getText().contains(".")) {
    			lonMin.setText("0");
    			lonMin.setEnabled(false);
    			lonSec.setText("0");
    			lonSec.setEnabled(false);
    		}
    		else {
       			lonMin.setEnabled(true);
    			lonSec.setEnabled(true);  
    		}
    	}
    	
    	// If the field "minutes" is decimal deactivate the "seconds" field.
    	if (e.getSource() == latMin) {
    		if (latMin.getText().contains(".")) {
    			latSec.setText("0");
    			latSec.setEnabled(false);
    		}
    		else {
    			latSec.setEnabled(true);
    		}
    	}
    	
    	if (e.getSource() == lonMin) {
    		if (lonMin.getText().contains(".")) {
    			lonSec.setText("0");
    			lonSec.setEnabled(false);
    		}
    		else {
    			lonSec.setEnabled(true);
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
            convPanel.setBorder(BorderFactory.createTitledBorder(null, I18n.text("Format"),
                    TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                    new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
			convPanel.setBounds(new Rectangle(261+23+10+10+5, 30, 149-(23+10+10-10), 98));
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
			ddegreesRadioButton.setText(I18n.text("Decimal"));
			ddegreesRadioButton.setToolTipText(I18n.text("Decimal Degrees"));
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
			dmRadioButton.setText(I18n.text("DM"));
            dmRadioButton.setToolTipText(I18n.text("Degrees, Minutes"));
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
            dmsRadioButton.setToolTipText(I18n.text("Degrees, Minutes, Seconds"));
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
                this.setLatitude(new double[] {
                        MathMiscUtils.round(loc.getLatitudeDegs(), CoordinateUtil.LAT_LON_DDEGREES_DECIMAL_PLACES), 0,
                        0 });
                this.setLongitude(new double[] {
                        MathMiscUtils.round(loc.getLongitudeDegs(), CoordinateUtil.LAT_LON_DDEGREES_DECIMAL_PLACES), 0,
                        0 });
    			latMin.setEnabled(false);
                lonMin.setEnabled(false);
                latSec.setEnabled(false);
                lonSec.setEnabled(false);
    			break;
    
    		case DM_DISPLAY:
    			double[] dmLat = CoordinateUtil.decimalDegreesToDM(loc.getLatitudeDegs());
    			double[] dmLon = CoordinateUtil.decimalDegreesToDM(loc.getLongitudeDegs());
                this.setLatitude(new double[] { dmLat[0],
                        MathMiscUtils.round(dmLat[1], CoordinateUtil.LAT_LON_DM_DECIMAL_PLACES), 0 });
                this.setLongitude(new double[] { dmLon[0],
                        MathMiscUtils.round(dmLon[1], CoordinateUtil.LAT_LON_DM_DECIMAL_PLACES), 0 });
    			latMin.setEnabled(true);
                lonMin.setEnabled(true);
                latSec.setEnabled(false);
    			lonSec.setEnabled(false);
    			break;
    
    		case DMS_DISPLAY:
    			double[] dmsLat = CoordinateUtil.decimalDegreesToDMS(loc.getLatitudeDegs());
    			double[] dmsLon = CoordinateUtil.decimalDegreesToDMS(loc.getLongitudeDegs());
                this.setLatitude(new double[] { dmsLat[0], dmsLat[1],
                        MathMiscUtils.round(dmsLat[2], CoordinateUtil.LAT_LON_DMS_DECIMAL_PLACES) });
                this.setLongitude(new double[] { dmsLon[0], dmsLon[1],
                        MathMiscUtils.round(dmsLon[2], CoordinateUtil.LAT_LON_DMS_DECIMAL_PLACES) });
   			
    			latMin.setEnabled(true);
                lonMin.setEnabled(true);
                
                latSec.setEnabled(true);
                lonSec.setEnabled(true);
    			break;
    
    		default:
    			break;
		}

		return true;
	}
}
