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
 * 21/Fev/2005
 * $Id:: MapParameters.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.mme;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
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
			System.out.println("Set the map description "+getDescription().getText());
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
