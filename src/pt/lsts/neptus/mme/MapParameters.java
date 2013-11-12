/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 21/Fev/2005
 */
package pt.lsts.neptus.mme;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author Ze Carlos
 */
public class MapParameters extends ParametersPanel implements ActionListener {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private LocationType homeLoc = new LocationType();
	private JLabel jLabel = null;
	private JLabel jLabel1 = null;
	private JLabel jLabel2 = null;
	private JTextPane description = null;
	private JTextField mapName = null;
	private JTextField mapID = null;
	private JScrollPane descScroll;
	private JButton cancelBtn = null;
	private JButton okBtn = null;
	private JDialog mapDialog = null;
	private MapType map = null;

	public MapParameters() {
		super();
		// TODO Auto-generated constructor stub
		initialize();
	}
	
	public void setMapParameters(MapType map) {
		this.map = map;
		this.initializeMapParameters(map);
		this.homeLoc = map.getCenterLocation();
		getMapDialog().setSize(this.getWidth()+10, this.getHeight()+40);
		getMapDialog().setModal(true);
		GuiUtils.centerOnScreen(getMapDialog());
		getMapDialog().setAlwaysOnTop(true);
		getMapDialog().setVisible(true);
	}

	public JDialog getMapDialog() {
		if (mapDialog != null)
			return mapDialog;
		else {
			mapDialog = new JDialog();
			mapDialog.setTitle("Parameters for this map");
			mapDialog.add(this);
		}
		
		return mapDialog;
	}
	
	/**
	 * This method initializes description	
	 * 	
	 * @return javax.swing.JTextArea	
	 */    
	public JTextPane getDescription() {
		if (description == null) {
			description = new JTextPane();
			description.setToolTipText("The description of the current map. This description shuld help identifying the map location and purposes.");
			description.setText("The map description should go here. Enter some information about the surroundings, previous missions that used this map, etc...");
		}
		return description;
	}
	/**
	 * This method initializes mapName	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	public JTextField getMapName() {
		if (mapName == null) {
			mapName = new JTextField();
			mapName.setBounds(120, 20, 242, 20);
			mapName.setToolTipText("The name of this map (can be any phrase)");
			mapName.setText("unnamed map");
		}
		return mapName;
	}
	/**
	 * This method initializes mapID	
	 * 	
	 * @return javax.swing.JTextField	
	 */    
	public JTextField getMapID() {
		if (mapID == null) {
			mapID = new JTextField();
			mapID.setBounds(120, 60, 132, 20);
			mapID.setToolTipText("This map unique identifier (has to be a valid identifier: no spaces, beginning with a letter, ...)");
			mapID.setText("unnamed_map");
			mapID.setEnabled(false);
			
		}
		return mapID;
	}
	/**
	 * This method initializes cancelBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setBounds(265, 280, 90, 30);
			cancelBtn.setText("Cancel");
			cancelBtn.setActionCommand("cancel");
			cancelBtn.addActionListener(this);
			NeptusLog.pub().debug(this.getClass()+": Map Parameters Dialog closed");
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
			okBtn.setText("OK");
			okBtn.setActionCommand("ok");
			okBtn.setBounds(145, 281, 90, 30);
			okBtn.addActionListener(this);
		}
		return okBtn;
	}
	public String getErrors() {
		
		String tmp = this.getMapName().getText();
		if (tmp == null || tmp.equals(""))
			return "The map name is not vallid";
		
		tmp = this.getMapID().getText();
		
		if (tmp == null || tmp.equals(""))
			return "The map identifier is not vallid";

		return null;
	}
	
	
	public void actionPerformed(ActionEvent a) {
		
		if ("ok".equals(a.getActionCommand())) {
			map.setName(getMapName().getText());
			map.setId(getMapID().getText());
			map.setDescription(getDescription().getText());
			NeptusLog.pub().info("<###>Set the map description "+getDescription().getText());
			map.setCenterLocation(this.homeLoc);
			//map.setType(get)
			getMapDialog().setVisible(false);
			getMapDialog().dispose();
		}
		
		if ("cancel".equals(a.getActionCommand())) {
			getMapDialog().setVisible(false);
			getMapDialog().dispose();
		}
	}
	
	
	public void initializeMapParameters(MapType map) {
		getMapID().setText(map.getId());
		getMapName().setText(map.getName());
		getDescription().setText(map.getDescription());
		homeLoc = map.getCenterLocation();
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private  void initialize() {
		jLabel2 = new JLabel();
		jLabel1 = new JLabel();
		jLabel = new JLabel();
		this.setLayout(null);
		this.setSize(381, 326);
		jLabel.setBounds(20, 20, 80, 20);
		jLabel.setText("Map Name:");
		jLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel1.setBounds(20, 60, 80, 20);
		jLabel1.setText("Map ID:");
		jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel2.setBounds(20, 100, 80, 20);
		jLabel2.setText("Description:");
		this.add(jLabel, null);
		this.add(jLabel1, null);
		this.add(jLabel2, null);
		this.add(getDescScroll(), null);
		this.add(getMapName(), null);
		this.add(getCancelBtn(), null);
		this.add(getMapID(), null);
		this.add(getOkBtn(), null);
	}
	
	private JScrollPane getDescScroll() {
		if (descScroll == null) {
			descScroll = new JScrollPane(getDescription());
			descScroll.setBounds(20, 120, 340, 138);
		}
		
		return descScroll;
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
