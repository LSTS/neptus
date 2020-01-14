/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.gui.TextureComboChooser;
import pt.lsts.neptus.i18n.I18n;
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
	private JLabel jLabel5 = null;
	private JTextField XDim = null;
	private JLabel jLabel6 = null;
	private JTextField YDim = null;
	private JLabel jLabel7 = null;
	private JTextField ZDim = null;
	private JLabel jLabel8 = null;
	private JButton choose = null;
	private JTextField ColorField = null;
	private JLabel jLabel = null;
	private JButton changeCenterLoc = null;
	private LocationType location = new LocationType();
	private JPanel jPanel = null;
	private JLabel jLabel1 = null;
	private JFormattedTextField rollField = null;
	private JLabel jLabel2 = null;
	private JLabel jLabel3 = null;
	private JFormattedTextField pitchField = null;
	private JLabel jLabel9 = null;
	private JLabel jLabel10 = null;
	private JFormattedTextField yawField = null;
	private JPanel jPanel1 = null;
	private JPanel jPanel2 = null;
	private JLabel jLabel4 = null;
	private JLabel jLabel11 = null;
	private JLabel jLabel12 = new JLabel(I18n.text("Texture:"));
	private JLabel lblFilled = new JLabel(I18n.text("Filled:"));
	private JCheckBox chkFilled = new JCheckBox();
	private TextureComboChooser textureCombo;
	private JPanel jPanel3 = null;
	/**
	 * 
	 */
	public ParallelepipedParameters() {
		super();
		initialize();
		setPreferredSize(new Dimension(450,350));
	}

	public void setDimensions(double dimx, double dimy, double dimz) {
		getXDim().setText(String.valueOf(dimx));
		getYDim().setText(String.valueOf(dimy));
		getZDim().setText(String.valueOf(dimz));
	}
	
	public void setFilled(boolean filled) {
	    chkFilled.setSelected(filled);
	}
	
	public void setRotation(double roll, double pitch, double yaw) {
		getRollField().setText(String.valueOf(roll));
		getPitchField().setText(String.valueOf(pitch));
		getYawField().setText(String.valueOf(yaw));
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
		if (XDim == null) {
			XDim = new JTextField();
			XDim.setPreferredSize(new java.awt.Dimension(40,20));
			XDim.addFocusListener(new SelectAllFocusListener());
		}
		return XDim;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getYDim() {
		if (YDim == null) {
			YDim = new JTextField();
			YDim.setPreferredSize(new java.awt.Dimension(40,20));
			YDim.addFocusListener(new SelectAllFocusListener());
		}
		return YDim;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getZDim() {
		if (ZDim == null) {
			ZDim = new JTextField();
			ZDim.setPreferredSize(new java.awt.Dimension(40,20));
			ZDim.addFocusListener(new SelectAllFocusListener());
		}
		return ZDim;
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
		if (ColorField == null) {
			ColorField = new JTextField();
			ColorField.setEditable(true);
			ColorField.setEnabled(false);
			ColorField.setBackground(java.awt.Color.orange);
			ColorField.setPreferredSize(new java.awt.Dimension(25,25));
		}
		return ColorField;
	}
 	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private  void initialize() {
		GridLayout gridLayout1 = new GridLayout();
		jLabel = new JLabel();
		jLabel8 = new JLabel();
		jLabel7 = new JLabel();
		jLabel6 = new JLabel();
		jLabel5 = new JLabel();
		this.setLayout(gridLayout1);
		this.setSize(428, 197);
		this.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,5,5,5));
		jLabel5.setText(I18n.text("Width:"));
		jLabel5.setPreferredSize(new java.awt.Dimension(60,20));
		jLabel6.setText(I18n.text("Length:"));
		jLabel6.setPreferredSize(new java.awt.Dimension(60,20));
		jLabel7.setText(I18n.text("Height:"));
		jLabel7.setPreferredSize(new java.awt.Dimension(60,20));
		jLabel8.setText(I18n.text("Color:"));
		jLabel.setText(I18n.text("Center Location:"));
		
		gridLayout1.setRows(4);
		this.add(getJPanel3(), null);
		this.add(getJPanel1(), null);
		this.add(getJPanel2(), null);
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
		if (jPanel == null) {
			jPanel = new JPanel();
			jLabel1 = new JLabel();
			jLabel2 = new JLabel();
			jLabel3 = new JLabel();
			jLabel9 = new JLabel();
			jLabel10 = new JLabel();
			jLabel1.setText(I18n.text("Roll:"));
			jLabel2.setText("   ");
			jLabel3.setText(I18n.text("Pitch:"));
			jLabel9.setText("   ");
			jLabel10.setText(I18n.text("Yaw:"));
			jPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, I18n.text("Rotation"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			jPanel.add(jLabel1, null);
			jPanel.add(getRollField(), null);
			jPanel.add(jLabel2, null);
			jPanel.add(jLabel3, null);
			jPanel.add(getPitchField(), null);
			jPanel.add(jLabel9, null);
			jPanel.add(jLabel10, null);
			jPanel.add(getYawField(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes rollField	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getRollField() {
		if (rollField == null) {
			rollField = new JFormattedTextField(df);
			rollField.setPreferredSize(new java.awt.Dimension(40,20));
			rollField.setText("0.0");
			rollField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
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
			pitchField.setPreferredSize(new java.awt.Dimension(40,20));
			pitchField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
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
			yawField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
			yawField.setPreferredSize(new java.awt.Dimension(40,20));
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
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			FlowLayout flowLayout3 = new FlowLayout();
			jPanel1 = new JPanel();
			jPanel1.setLayout(flowLayout3);
			flowLayout3.setAlignment(java.awt.FlowLayout.LEFT);
			jPanel1.add(jLabel8, null);
			jPanel1.add(getColorField(), null);
			jPanel1.add(getChoose(), null);
			
			jPanel1.add(new JLabel("   "));
			jPanel1.add(jLabel12);
			jPanel1.add(getTexturesCombo());
		}
		return jPanel1;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jLabel4 = new JLabel();
			jLabel11 = new JLabel();
			jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, I18n.text("Dimension"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			jLabel4.setText("   ");
			jLabel11.setText("   ");
			jPanel2.add(jLabel5, null);
			jPanel2.add(getXDim(), null);
			jPanel2.add(jLabel4, null);
			jPanel2.add(jLabel6, null);
			jPanel2.add(getYDim(), null);
			jPanel2.add(jLabel11, null);
			jPanel2.add(jLabel7, null);
			jPanel2.add(getZDim(), null);
		}
		return jPanel2;
	}
	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			jPanel3 = new JPanel();
			FlowLayout flowLayout2 = new FlowLayout();
			jPanel3.setLayout(flowLayout2);
			flowLayout2.setAlignment(java.awt.FlowLayout.LEFT);
			jPanel3.add(jLabel, null);
			jPanel3.add(getChangeCenterLoc(), null);
			jPanel3.add(lblFilled, null);
			jPanel3.add(chkFilled, null);
			
		}
		return jPanel3;
	}
            	public static void main(String args[]) {
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
