/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 10, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.conf.ConfigFetch;
/**
 * @author zecarlos
 *
 */
public class MissionInfoPanel extends ParametersPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private JPanel jPanel1 = null;
	private JTextField missionNameField = null;
	private JLabel jLabel1 = null;
	private JPanel jPanel2 = null;
	private JLabel jLabel = null;
	private JTextField missionIDField = null;
	private JPanel jPanel4 = null;
	private JLabel jLabel2 = null;
	private JComboBox<?> typeCombo = null;
	private JLabel jLabel3 = null;
	private JButton cancelBtn = null;
	private JButton okBtn = null;
	private MissionType mission;
	private JDialog dialog;
	private boolean userCanceled = false;
    private boolean editable;
	private JPanel jPanel = null;
	private JScrollPane jScrollPane = null;
	private JTextPane description = null;
	/**
	 * This method initializes 
	 * 
	 */
	public MissionInfoPanel() {
		super();
		initialize();
		this.add(getJPanel1(), java.awt.BorderLayout.CENTER);
	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        jLabel3 = new JLabel();
        this.setLayout(null);
        this.setSize(327, 324);
        this.setMinimumSize(new Dimension(327,324));
        jLabel3.setBounds(10, 128, 151, 21);
        jLabel3.setText("Mission Description:");
        this.add(getJPanel1(), null);
        this.add(getJPanel2(), null);
        this.add(getJPanel4(), null);
        this.add(jLabel3, null);
        this.add(getCancelBtn(), null);
        this.add(getOkBtn(), null);
        this.add(getJPanel(), null);
			
	}
    public String getErrors() {
        if (this.getMissionNameField().getText().length() == 0) {
            return "The mission must have a name";
        }
        if (this.getJTextField().getText().length() == 0) {
            return "The mission must have an identifier";
        }
        if (!NameNormalizer.isNeptusValidIdentifier(getJTextField().getText())) {
            return "The mission identifier is not vallid";
        }
        
        if (this.getDescription().getText().length() == 0)
            return "The mission description is empty";
        
        return null;
    }

	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jLabel1 = new JLabel();
			jPanel1 = new JPanel();
			jLabel1.setText("Mission Name:");
			jPanel1.setBounds(10, 8, 310, 29);
			jPanel1.add(jLabel1, null);
			jPanel1.add(getMissionNameField(), null);
			jPanel1.add(getMissionNameField(), null);
		}
		return jPanel1;
	}
 	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getMissionNameField() {
		if (missionNameField == null) {
			missionNameField = new JTextField("Unnamed Mission");
			missionNameField.setColumns(15);
		}
		return missionNameField;
	}
	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jLabel = new JLabel();
			jLabel.setText("Mission Identifier:");
			jPanel2.setBounds(10, 47, 310, 29);
			jPanel2.add(jLabel, null);
			jPanel2.add(getJTextField(), null);
		}
		return jPanel2;
	}
	/**
	 * This method initializes jTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	private JTextField getJTextField() {
		if (missionIDField == null) {
			missionIDField = new JTextField("MissionID");
			missionIDField.setColumns(15);
		}
		return missionIDField;
	}
	/**
	 * This method initializes jPanel4	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			jLabel2 = new JLabel();
			jPanel4 = new JPanel();
			jLabel2.setText("Mission Type:");
			jPanel4.setBounds(10, 86, 310, 30);
			jPanel4.add(jLabel2, null);
			jPanel4.add(getTypeCombo(), null);
		}
		return jPanel4;
	}
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<?> getTypeCombo() {
		if (typeCombo == null) {
			typeCombo = new JComboBox<Object>(new Object[] {"Inspection", "Test", "Other"});
			typeCombo.setPreferredSize(new java.awt.Dimension(100,20));
		}
		return typeCombo;
	}
	/**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setBounds(230, 280, 80, 25);
			cancelBtn.setText("Cancel");
			cancelBtn.setActionCommand("cancel");
			cancelBtn.addActionListener(this);
		}
		return cancelBtn;
	}
	/**
	 * This method initializes jButton1	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getOkBtn() {
		if (okBtn == null) {
			okBtn = new JButton();
			okBtn.setBounds(140, 280, 80, 25);
			okBtn.setText("OK");
			okBtn.setActionCommand("ok");
			okBtn.addActionListener(this);
		}
		return okBtn;
	}
	
	public void actionPerformed(ActionEvent e) {
	    if ("ok".equals(e.getActionCommand())) {
	        if (getErrors() != null) {
	            JOptionPane.showMessageDialog(this, getErrors(), "Error in the parameters", JOptionPane.ERROR_MESSAGE);
	        }
	        else {
	            //this.mission = new MissionType();
	            mission.setDescription(getDescription().getText());
		        mission.setName(getMissionNameField().getText());
		        mission.setId(getJTextField().getText());
		        mission.setType(getTypeCombo().getSelectedItem().toString());
		        setUserCanceled(true);
		        dialog.setVisible(false);
		        dialog.dispose();
	        }
	    }
	    
	    if ("cancel".equals(e.getActionCommand())) {	        
	        setUserCanceled(true);
	        dialog.setVisible(false);
	        dialog.dispose();
	    }
	}
	
	
	
	public void showDialog() {
	    dialog = new JDialog(ConfigFetch.getSuperParentAsFrame(), "Mission Information");
	    dialog.add(this);
	    dialog.setSize(getWidth() + 15, getHeight() + 25);
	    dialog.setAlwaysOnTop(true);
	    dialog.setResizable(false);
	    dialog.setModal(true);
	    GuiUtils.centerOnScreen(dialog);
	    dialog.setVisible(true);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setUserCanceled(true);
				dialog.setVisible(false);
				dialog.dispose();
			}
		});
	}
	
	public static MissionType changeMissionParameters(MissionType mission) {
	    if (mission == null) {
	    	JOptionPane.showMessageDialog(null, "You have to create a mission first", "Create a mission first", JOptionPane.WARNING_MESSAGE);
	    	return null;
	    }
		MissionInfoPanel panel = new MissionInfoPanel();
	    panel.setMission(mission);
	    panel.showDialog();
	    return panel.getMission();
	}

	
	
	public static MissionType showMissionInfoDialog(MissionType mission) {
	    if (mission == null) {
	    	JOptionPane.showMessageDialog(null, "You have to create a mission first", "Create a mission first", JOptionPane.WARNING_MESSAGE);
	    	return null;
	    }
		MissionInfoPanel panel = new MissionInfoPanel();
	    panel.setMission(mission);
	    panel.setEditable(false);
	    panel.showDialog();
	    return panel.getMission();
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(new BorderLayout());
			jPanel.setBounds(10, 158, 307, 111);
			jPanel.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
		}
		return jPanel;
	}
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */    
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getDescription());
		}
		return jScrollPane;
	}
	/**
	 * This method initializes jTextPane	
	 * 	
	 * @return javax.swing.JTextPane	
	 */    
	private JTextPane getDescription() {
		if (description == null) {
			description = new JTextPane();
		}
		return description;
	}
     	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    

      public static void main(String[] args) {
         MissionType mt = new MissionType();
         
         mt =  MissionInfoPanel.changeMissionParameters(mt);
         NeptusLog.pub().info("<###> "+mt);
    }
    public MissionType getMission() {
        return mission;
    }
    
    public void setMission(MissionType mission) {
        this.mission = mission;
        getDescription().setText(mission.getDescription());
        getMissionNameField().setText(mission.getName());
        getJTextField().setText(mission.getId());
        //if (mission.getType().length() > 0)
        getTypeCombo().setSelectedItem(mission.getType());
    }
    
    public boolean isUserCanceled() {
        return userCanceled;
    }
    public void setUserCanceled(boolean userCanceled) {
        this.userCanceled = userCanceled;
    }
    
    
	public void setEditable(boolean value) 
	{
	    this.editable = value;
	    getMissionNameField().setEditable(editable);
	    getDescription().setEditable(editable);
	    getJTextField().setEditable(editable);
	    getTypeCombo().setEnabled(editable);
	    getCancelBtn().setVisible(editable);
	}

	public void setIdEditable(boolean value) 
	{
	    getJTextField().setEditable(editable);
	    getCancelBtn().setVisible(editable);
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
