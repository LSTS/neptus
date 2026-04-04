/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * 9/Fev/2005
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.gui.TextureComboChooser;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.texture.TextureType;
import pt.lsts.neptus.types.texture.TexturesHolder;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author Ze Carlos
 */
public class ParallelepipedParameters extends ParametersPanel implements ActionListener {
	public static final long serialVersionUID = 23756234;

	private NumberFormat df = GuiUtils.getNeptusDecimalFormat();
	private JLabel dimWidthLabel = null;
	private JTextField xDim = null;
	private JLabel dimLengthLabel = null;
	private JTextField yDim = null;
	private JLabel dimHeightLabel = null;
	private JTextField zDim = null;
	private JLabel colorLabel = null;
	private JButton choose = null;
	private JTextField colorField = null;
	private JLabel centerLocationLabel = null;
	private JButton changeCenterLoc = null;
	private LocationType location = new LocationType();
	private JPanel rotationPanel = null;
	private JLabel rollLabel = null;
	private JFormattedTextField rollField = null;
	private JLabel pitchLabel = null;
	private JFormattedTextField pitchField = null;
	private JLabel yawLabel = null;
	private JFormattedTextField yawField = null;
	private JPanel colorTexturePanel = null;
	private JPanel dimensionsPanelPanel = null;
	private JLabel textureLabel1 = new JLabel(I18n.text("Texture:"));
	private JLabel filledLabel = new JLabel(I18n.text("Filled:"));
	private JCheckBox chkFilled = new JCheckBox();
	private TextureComboChooser textureCombo;
	private JPanel centerLocAndFilledPanel = null;

	public ParallelepipedParameters() {
		super();
		initialize();
		setPreferredSize(new Dimension(450,350));
	}

	public void setDimensions(double dimX, double dimY, double dimZ) {
		getXDim().setText(String.valueOf(dimX));
		getXDim().setCaretPosition(0);
		getYDim().setText(String.valueOf(dimY));
		getYDim().setCaretPosition(0);
		getZDim().setText(String.valueOf(dimZ));
		getZDim().setCaretPosition(0);
	}
	
	public void setFilled(boolean filled) {
	    chkFilled.setSelected(filled);
	}
	
	public void setRotation(double roll, double pitch, double yaw) {
		getRollField().setText(String.valueOf(roll));
		getRollField().setCaretPosition(0);
		getPitchField().setText(String.valueOf(pitch));
		getPitchField().setCaretPosition(0);
		getYawField().setText(String.valueOf(yaw));
		getYawField().setCaretPosition(0);
	}

	public TextureType getSelectedTexture() {
		return getTexturesCombo().getCurrentlySelectedTexture();
	}
	
	/**
	 * Returns the user selected rotation in the form of an array of doubles:
	 * [roll, pitch, yaw]
	 * @return The rotation in the form of an array of the type [roll, pitch, yaw]
	 */
	public double[] getRotation() {
		double[] ret = new double[3];
		ret[0] = Double.parseDouble(getRollField().getText());
		ret[1] = Double.parseDouble(getPitchField().getText());
		ret[2] = Double.parseDouble(getYawField().getText());
		
		return ret;
	}
	
	public boolean isFilled() {
	    return chkFilled.isSelected();
	}
	
	public void setCenterLocation(LocationType centerLoc) {
		this.location = centerLoc;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getXDim() {
		if (xDim == null) {
			xDim = new JTextField();
			xDim.setPreferredSize(new java.awt.Dimension(80,20));
			xDim.addFocusListener(new SelectAllFocusListener());
		}
		return xDim;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getYDim() {
		if (yDim == null) {
			yDim = new JTextField();
			yDim.setPreferredSize(new java.awt.Dimension(80,20));
			yDim.addFocusListener(new SelectAllFocusListener());
		}
		return yDim;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getZDim() {
		if (zDim == null) {
			zDim = new JTextField();
			zDim.setPreferredSize(new java.awt.Dimension(80,20));
			zDim.addFocusListener(new SelectAllFocusListener());
		}
		return zDim;
	}
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getChoose() {
		if (choose == null) {
			choose = new JButton();
			choose.setText(I18n.text("Choose..."));
			choose.setPreferredSize(new java.awt.Dimension(90,25));
		}
		return choose;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getColorField() {
		if (colorField == null) {
			colorField = new JTextField();
			colorField.setEditable(true);
			colorField.setEnabled(false);
			colorField.setBackground(java.awt.Color.orange);
			colorField.setPreferredSize(new java.awt.Dimension(25,25));
		}
		return colorField;
	}
 	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private  void initialize() {
		GridLayout gridLayout1 = new GridLayout();
		centerLocationLabel = new JLabel();
		colorLabel = new JLabel();
		dimHeightLabel = new JLabel();
		dimLengthLabel = new JLabel();
		dimWidthLabel = new JLabel();
		this.setLayout(gridLayout1);
		this.setSize(428, 197);
		this.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
		dimWidthLabel.setText(I18n.text("Width:"));
		dimWidthLabel.setPreferredSize(new java.awt.Dimension(60,20));
		dimLengthLabel.setText(I18n.text("Length:"));
		dimLengthLabel.setPreferredSize(new java.awt.Dimension(60,20));
		dimHeightLabel.setText(I18n.text("Height:"));
		dimHeightLabel.setPreferredSize(new java.awt.Dimension(60,20));
		colorLabel.setText(I18n.text("Color:"));
		centerLocationLabel.setText(I18n.text("Center Location:"));
		
		gridLayout1.setRows(4);
		this.add(getCenterLocAndFilledPanel(), null);
		this.add(getColorTexturePanel(), null);
		this.add(getDimensionsPanel(), null);
		this.add(getJPanel(), null);
		
		getChoose().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showColorDialog();
			}
		});
	}
	
	
	public void setColor(Color color) {
		getColorField().setBackground(color);
	}
	
/*	public void initializeObject(ParallelPiped obj) {
		try {
			obj.centerX = Float.parseFloat(getXCenter().getText());
			obj.centerY = Float.parseFloat(getYCenter().getText());
			obj.centerZ = Float.parseFloat(getZCenter().getText());
		
			obj.dimX = Float.parseFloat(getXDim().getText());
			obj.dimY = Float.parseFloat(getYDim().getText());
			obj.dimZ = Float.parseFloat(getZDim().getText());
		
			obj.ObjColor = getColorField().getBackground();
		}
		catch (Exception e) {
			System.err.println("Params panel had some errors (witch were ignored)");
		}
	}
	*/
	public void actionPerformed(ActionEvent e) {
	    LocationType tmp = LocationPanel.showLocationDialog(I18n.text("Set the object center location"), location, getMissionType(), isEditable());
	    if (tmp != null)
	        setLocationType(tmp);
	}
	
	public void showColorDialog() {
		Color newColor = JColorChooser.showDialog(this, I18n.text("Choose the parallel piped color"), getColorField().getBackground());
		getColorField().setBackground(newColor);
	}
	
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getChangeCenterLoc() {
		if (changeCenterLoc == null) {
			changeCenterLoc = new JButton();
			changeCenterLoc.setText(I18n.text("Change..."));
			changeCenterLoc.addActionListener(this);
		}
		return changeCenterLoc;
	}
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (rotationPanel == null) {
			rotationPanel = new JPanel(new MigLayout("center, wrap 6", "[]5[]20[]5[]20[]5[]", "[]"));
			rollLabel = new JLabel();
			pitchLabel = new JLabel();
			yawLabel = new JLabel();
			rollLabel.setText(I18n.text("Roll:"));
			pitchLabel.setText(I18n.text("Pitch:"));
			yawLabel.setText(I18n.text("Yaw:"));
			rotationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
                    I18n.textf("Rotation (%angleSymbol)", CoordinateUtil.CHAR_DEGREE) ,
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION,
                    null, null));
			rotationPanel.add(rollLabel, "sg labels");
			rotationPanel.add(getRollField(), "sg values");
			rotationPanel.add(pitchLabel, "sg labels");
			rotationPanel.add(getPitchField(), "sg values");
			rotationPanel.add(yawLabel, "sg labels");
			rotationPanel.add(getYawField(), "sg values");
		}
		return rotationPanel;
	}

	/**
	 * This method initializes rollField	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getRollField() {
		if (rollField == null) {
			rollField = new JFormattedTextField(df);
			rollField.setPreferredSize(new java.awt.Dimension(80,20));
			rollField.setText("0.0");
			rollField.setHorizontalAlignment(JTextField.LEADING);
			rollField.addFocusListener(new SelectAllFocusListener());
		}
		return rollField;
	}
	/**
	 * This method initializes pitchField	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getPitchField() {
		if (pitchField == null) {
			pitchField = new JFormattedTextField(df);
			pitchField.setPreferredSize(new java.awt.Dimension(80,20));
			pitchField.setHorizontalAlignment(JTextField.LEADING);
			pitchField.setText("0.0");
			pitchField.addFocusListener(new SelectAllFocusListener());
		}
		return pitchField;
	}
	/**
	 * This method initializes yawField	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getYawField() {
		if (yawField == null) {
			yawField = new JFormattedTextField(df);
			yawField.setHorizontalAlignment(JTextField.LEADING);
			yawField.setPreferredSize(new java.awt.Dimension(80,20));
			yawField.setText("0.0");
			yawField.addFocusListener(new SelectAllFocusListener());
		}
		return yawField;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getColorTexturePanel() {
		if (colorTexturePanel == null) {
			FlowLayout flowLayout3 = new FlowLayout();
			colorTexturePanel = new JPanel();
			colorTexturePanel.setLayout(flowLayout3);
			flowLayout3.setAlignment(java.awt.FlowLayout.LEFT);
			colorTexturePanel.add(colorLabel, null);
			colorTexturePanel.add(getColorField(), null);
			colorTexturePanel.add(getChoose(), null);
			
			colorTexturePanel.add(new JLabel("   "));
			colorTexturePanel.add(textureLabel1);
			colorTexturePanel.add(getTexturesCombo());
		}
		return colorTexturePanel;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getDimensionsPanel() {
		if (dimensionsPanelPanel == null) {
			dimensionsPanelPanel = new JPanel(new MigLayout("center, wrap 6", "[]5[]20[]5[]20[]5[]", "[]"));
			dimensionsPanelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null,
                    I18n.text("Dimension (m)"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			dimensionsPanelPanel.add(dimWidthLabel, "sg labels");
			dimensionsPanelPanel.add(getXDim(), "sg values");
			dimensionsPanelPanel.add(dimLengthLabel, "sg labels");
			dimensionsPanelPanel.add(getYDim(), "sg values");
			dimensionsPanelPanel.add(dimHeightLabel, "sg labels");
			dimensionsPanelPanel.add(getZDim(), "sg values");
		}
		return dimensionsPanelPanel;
	}
	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getCenterLocAndFilledPanel() {
		if (centerLocAndFilledPanel == null) {
			centerLocAndFilledPanel = new JPanel();
			FlowLayout flowLayout2 = new FlowLayout();
			centerLocAndFilledPanel.setLayout(flowLayout2);
			flowLayout2.setAlignment(java.awt.FlowLayout.LEFT);
			centerLocAndFilledPanel.add(centerLocationLabel, null);
			centerLocAndFilledPanel.add(getChangeCenterLoc(), null);
			centerLocAndFilledPanel.add(filledLabel, null);
			centerLocAndFilledPanel.add(chkFilled, null);
			
		}
		return centerLocAndFilledPanel;
	}

    public static void main(String[] args) {
		JFrame tstFrame = new JFrame("Dialog Unitary Test");
		tstFrame.setLayout(new BorderLayout());
		ParallelepipedParameters params = new ParallelepipedParameters();
		tstFrame.getContentPane().add(params, BorderLayout.CENTER);
		tstFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		tstFrame.setSize(350, 180);
		tstFrame.setVisible(true);
		
	}
  	
  	public String getErrors() {
  		ParallelepipedElement obj = new ParallelepipedElement(null, null);
		try {
			obj.setWidth(Float.parseFloat(getXDim().getText()));
			obj.setLength(Float.parseFloat(getYDim().getText()));
			obj.setHeight(Float.parseFloat(getZDim().getText()));
		
			obj.setColor(getColorField().getBackground());
		}
		catch (NumberFormatException e) {
			return I18n.text("The parameters entered are not valid numbers.");
		}
		
		if (obj.getWidth() < 0 || obj.getLength() < 0 || obj.getHeight() < 0)
			return I18n.text("The object dimensions have to be positive.");
		
		return null;
	}
  	
  	public double[] getDimension() {
  	    double[] dim = new double[3];
  	    dim[0] = Double.parseDouble(getXDim().getText());
  	    dim[1] = Double.parseDouble(getYDim().getText());
  	    dim[2] = Double.parseDouble(getZDim().getText());
  	    return dim;
  	}
	
  	
  	public Color getChosenColor() {
  	    return getColorField().getBackground();
  	}
  	
    public LocationType getLocationType() {
        return location;
    }
    public void setLocationType(LocationType location) {
        this.location = location;
    }
    
    public void setEditable(boolean editable) {
		super.setEditable(editable);
		if (!isEditable()) {
			getChoose().setEnabled(false);
			getChangeCenterLoc().setText(I18n.text("View..."));
			getXDim().setEditable(false);
			getYDim().setEditable(false);
			getZDim().setEditable(false);
			getYawField().setEditable(false);
			getRollField().setEditable(false);
			getPitchField().setEditable(false);
			
		}
		else {
			getChoose().setEnabled(true);
			getChangeCenterLoc().setText(I18n.text("Change..."));
			getXDim().setEditable(true);
			getYDim().setEditable(true);
			getZDim().setEditable(true);
			getYawField().setEditable(true);
			getRollField().setEditable(true);
			getPitchField().setEditable(true);
		}
	}
    
    public TextureComboChooser getTexturesCombo() {
    	 if (textureCombo == null)
    		 textureCombo = TexturesHolder.getTextureListChooser();
    	 return textureCombo;
    }
    
}  //  @jve:decl-index=0:visual-constraint="57,40"
