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
 * 9/Jun/2005
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.renderer3d.Renderer3D;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
/**
 * @author ZePinto
 */
public class Grid3D extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private JPanel jPanel = null;
	private JPanel jPanel1 = null;
	private JFormattedTextField dimField = null;
	private JCheckBox checkNE = null;
	private JCheckBox checkUN = null;
	private JCheckBox checkUE = null;
	private JLabel jLabel = null;
	private JLabel jLabel1 = null;
	private JFormattedTextField cellSize = null;
	private JCheckBox showValues = null;
	private JButton colorBtn = null;
	private JButton color = null;
	private JButton cancelBtn = null;
	private JButton okBtn = null;
	private Renderer3D renderer = null;
	private NumberFormat df = GuiUtils.getNeptusDecimalFormat();
	
	private JLabel jLabel2 = null;
	private JFormattedTextField depthField = null;
	/**
	 * This method initializes 
	 * 
	 */
	public Grid3D(Frame parent) {
		super(parent);
		initialize();
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setContentPane(getJPanel());
        this.setSize(471, 284);
			
	}
	
	public void setRenderer3D(Renderer3D r3d) {
		this.renderer = r3d;
		setValues();
	}
	
	
	private void setValues() {
		//TODO inicializar com os valores do renderer exemplo:
		getCheckNE().setSelected(renderer.gNE);
		getCheckUE().setSelected(renderer.gUE);
		getCheckUN().setSelected(renderer.gUN);
		showValues.setSelected(renderer.gtext);
		//NeptusLog.pub().info("<###> "+String.valueOf(renderer.gdimension));
		//NeptusLog.pub().info("<###> "+String.valueOf(renderer.gspacing));
		dimField.setText(String.valueOf(renderer.gdimension)); //converter float em String
		cellSize.setText(String.valueOf(renderer.gspacing));
		depthField.setText(String.valueOf(renderer.gcenter.z));
		color.setBackground(renderer.gcolor);
		//renderer.
		//color.getC
	}
	
	private void setRenderer() {
		//TODO setar o renderer com os valores correntes
		renderer.gNE=checkNE.isSelected();
		renderer.gUE=checkUE.isSelected();
		renderer.gUN=checkUN.isSelected();
		renderer.gtext=showValues.isSelected();
		renderer.gdimension=Float.parseFloat(dimField.getText()); // converter String em Float
		renderer.gspacing=Float.parseFloat(cellSize.getText());
		renderer.gcenter.z=Float.parseFloat(depthField.getText());
		renderer.gcolor=color.getBackground();
		renderer.menuOKgrid();
	}
	
	
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel2 = new JLabel();
			jLabel1 = new JLabel();
			jLabel = new JLabel();
			jPanel = new JPanel();
			jPanel.setLayout(null);
			jLabel.setBounds(20, 95, 119, 25);
			jLabel.setText("Dimension (meters):");
			jLabel1.setBounds(20, 132, 110, 25);
			jLabel1.setText("Cell Size (meters):");
			jLabel2.setBounds(20, 170, 110, 25);
			jLabel2.setText("Grid Depth:");
			jPanel.add(getJPanel1(), null);
			jPanel.add(getDimField(), null);
			jPanel.add(jLabel, null);
			jPanel.add(jLabel1, null);
			jPanel.add(getCellSize(), null);
			jPanel.add(getShowValues(), null);
			jPanel.add(getColorBtn(), null);
			jPanel.add(getColor(), null);
			jPanel.add(getCancelBtn(), null);
			jPanel.add(getOkBtn(), null);
			jPanel.add(jLabel2, null);
			jPanel.add(getDepthField(), null);
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
			jPanel1 = new JPanel();
			GridLayout gridLayout1 = new GridLayout();
			jPanel1.setLayout(gridLayout1);
			jPanel1.setBounds(11, 13, 443, 60);
			jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Shown grids", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
			gridLayout1.setRows(1);
			gridLayout1.setColumns(3);
			jPanel1.add(getCheckNE(), null);
			jPanel1.add(getCheckUN(), null);
			jPanel1.add(getCheckUE(), null);
		}
		return jPanel1;
	}
	/**
	 * This method initializes dimField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getDimField() {
		if (dimField == null) {
			dimField = new JFormattedTextField(df);
			dimField.setBounds(160, 95, 60, 25);
		}
		return dimField;
	}
	/**
	 * This method initializes checkNE	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getCheckNE() {
		if (checkNE == null) {
			checkNE = new JCheckBox();
			checkNE.setText("North/East");
		}
		return checkNE;
	}
	/**
	 * This method initializes checkUN	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getCheckUN() {
		if (checkUN == null) {
			checkUN = new JCheckBox();
			checkUN.setText("Up/North");
		}
		return checkUN;
	}
	/**
	 * This method initializes checkUE	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getCheckUE() {
		if (checkUE == null) {
			checkUE = new JCheckBox();
			checkUE.setText("Up/East");
		}
		return checkUE;
	}
	/**
	 * This method initializes cellSize	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JFormattedTextField getCellSize() {
		if (cellSize == null) {
			cellSize = new JFormattedTextField(df);
			cellSize.setBounds(160, 132, 60, 25);
		}
		return cellSize;
	}
	/**
	 * This method initializes showValues	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getShowValues() {
		if (showValues == null) {
			showValues = new JCheckBox();
			showValues.setBounds(280, 89, 124, 22);
			showValues.setText("Show values");
		}
		return showValues;
	}
	/**
	 * This method initializes colorBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getColorBtn() {
		if (colorBtn == null) {
			colorBtn = new JButton();
			colorBtn.setBounds(305, 125, 100, 30);
			colorBtn.setText("Color...");
			colorBtn.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					//NeptusLog.pub().info("<###>actionPerformed()"); // TODO Auto-generated Event stub actionPerformed()
					Color c = JColorChooser.showDialog(new JFrame(), "Select the grid color", getColor().getBackground());
					if (c != null)
						getColor().setBackground(c);
				}
			});
		}
		return colorBtn;
	}
	/**
	 * This method initializes color	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getColor() {
		if (color == null) {
			color = new JButton();
			color.setBounds(280, 125, 25, 30);
			color.setEnabled(false);
			color.setBackground(java.awt.Color.gray);
		}
		return color;
	}
	/**
	 * This method initializes cancelBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setBounds(353, 206, 100, 30);
			cancelBtn.setText("Cancel");
			cancelBtn.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					setVisible(false);
					dispose();
				}
			});
		}
		return cancelBtn;
	}
	
	/**
	 * This method initializes okBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getOkBtn() {
		if (okBtn == null) {
			okBtn = new JButton();
			okBtn.setBounds(246, 206, 100, 30);
			okBtn.setText("OK");
			okBtn.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
					setRenderer();
					setVisible(false);
					dispose();
				}
			});
		}
		return okBtn;
	}
	
	public static void showGridDialog(Renderer3D r3d) {
		
		
		Window parentFrame = SwingUtilities.getWindowAncestor(r3d);
		Grid3D g3d;
		if (parentFrame instanceof Frame)
			g3d = new Grid3D((Frame)parentFrame);
		else
			g3d = new Grid3D(ConfigFetch.getSuperParentAsFrame());
		
		g3d.setRenderer3D(r3d);
		g3d.setVisible(true);
		g3d.setModal(true);
		GuiUtils.centerOnScreen(g3d);
	}
	
	/**
	 * This method initializes depthField	
	 * 	
	 * @return javax.swing.JFormattedTextField	
	 */    
	private JFormattedTextField getDepthField() {
		if (depthField == null) {
			depthField = new JFormattedTextField(df);
			depthField.setBounds(160, 170, 60, 25);
			//depthField.setF
		}
		return depthField;
	}
  	public static void main(String args[]) {
		Grid3D.showGridDialog(new Renderer3D());
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
