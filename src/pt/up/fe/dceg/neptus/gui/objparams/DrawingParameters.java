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
 * 9/Fev/2005
 * $Id:: DrawingParameters.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.gui.objparams;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
/**
 * @author Zé Carlos
 */
public class DrawingParameters extends ParametersPanel {

	static final long serialVersionUID = 23874623;

	private JLabel jLabel8 = null;
	private JButton choose = null;
	private JTextField ColorField = null;
	private JCheckBox shapeCheck = null;

	/**
	 *
	 */
	public DrawingParameters() {
		super();
		initialize();
		setPreferredSize(new Dimension(400,300));
	}

	/**
	 * This method initializes jButton
	 *
	 * @return javax.swing.JButton
	 */
	private JButton getChoose() {
		if (choose == null) {
			choose = new JButton();
			choose.setBounds(135, 15, 90, 25);
			choose.setText("Choose...");
		}
		return choose;
	}
	
	public JCheckBox getShapeCheck() {
		if (shapeCheck == null) {
			shapeCheck = new JCheckBox("Filled shape");
			shapeCheck.setOpaque(false);
			shapeCheck.setBounds(80, 60, 100, 25);			
		}
		return shapeCheck;
	}
	/**
	 * This method initializes jTextField
	 *
	 * @return javax.swing.JTextField
	 */
	public JTextField getColorField() {
		if (ColorField == null) {
			ColorField = new JTextField();
			ColorField.setBounds(100, 15, 25, 25);
			ColorField.setEditable(true);
			ColorField.setEnabled(false);
			ColorField.setBackground(java.awt.Color.orange);
		}
		return ColorField;
	}
 	/**
	 * This method initializes this
	 *
	 * @return void
	 */
	private  void initialize() {
		jLabel8 = new JLabel();
		this.setLayout(null);
		this.setSize(350, 50);
		this.setBackground(java.awt.SystemColor.control);
		jLabel8.setBounds(15, 15, 50, 25);
		jLabel8.setText("Color:");
		this.add(getChoose(), null);
		this.add(getColorField(), null);
		this.add(getShapeCheck());
		getChoose().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showColorDialog();
			}
		});
		this.add(jLabel8, null);
	}


	public Color getColor() {
	    return getColorField().getBackground();
	}

	public void showColorDialog() {
		Color newColor = JColorChooser.showDialog(this, "Choose the parallel piped color", getColorField().getBackground());
		getColorField().setBackground(newColor);
	}

  	public static void main(String args[]) {
		JFrame tstFrame = new JFrame("Dialog Unitary Test");
		tstFrame.setLayout(new BorderLayout());
		DrawingParameters params = new DrawingParameters();
		tstFrame.getContentPane().add(params, BorderLayout.CENTER);
		tstFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		tstFrame.setSize(350, 350);
		tstFrame.setVisible(true);

	}

  	public String getErrors() {
  		return null;
  	}

  	public void setEditable(boolean value) {
		super.setEditable(value);
		getChoose().setEnabled(isEditable());
		getShapeCheck().setEnabled(isEditable());
	}

}  //  @jve:decl-index=0:visual-constraint="68,42"
