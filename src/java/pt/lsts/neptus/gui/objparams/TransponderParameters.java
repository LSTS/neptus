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
 * Mar 15, 2005
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.TransponderUtils;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.editors.EditorLauncher;
/**
 * @author zecarlos
 *
 */
public class TransponderParameters extends ParametersPanel {

	private static final long serialVersionUID = 7696810945439062905L;

	private LocationPanel locationPanel = null;
	private JPanel jPanel = null;
	private JLabel jLabel = null;
	private JComboBox<String> configurationFile = null;
	private JButton editBtn = null;
	private CoordinateSystem homeRef = null;
	private JButton jButton = null;
	private JLabel jLabel1 = null;
	
	private JTextField idEditor;
	
	/**
	 * This method initializes 
	 * @param idEditor 
	 * 
	 */
	public TransponderParameters(CoordinateSystem homeRef, JTextField idEditor) {
		super();
		this.idEditor = idEditor;
		this.homeRef = homeRef;
		initialize();
		setPreferredSize(new Dimension(470, 530-130));
	}
	
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
        this.setLayout(new BorderLayout());
        //this.setBounds(0, 0, 441, 398);
        this.add(getLocationPanel(), java.awt.BorderLayout.CENTER);
        this.add(getJPanel(), java.awt.BorderLayout.SOUTH);
			
	}
    public String getErrors() {
        
    	if (getConfigurationFile().getSelectedItem() == null) {
    		return I18n.text("A configuration file must be selected");
    	}
    	
        if (getLocationPanel().getErrors() != null)
            return getLocationPanel().getErrors();
        
        return null;
    }
    
    public  void setIdEditor(JTextField idEditor) {
        this.idEditor = idEditor;
    }
    
    public void setLocation(LocationType location) {
    	getLocationPanel().setLocationType(location); 
    }

    public void setMap(MapType map) {
    	getLocationPanel().setMissionType(getMissionType());
    	if (map.getMission() != null) {
    	    getLocationPanel().setMissionType(map.getMission());
    	}
    	else {
    	    MissionType mt = new MissionType();
    	    MapMission mapm = new MapMission();
    	    mapm.setMap(map);
    	    mt.addMap(mapm);
    	    MapGroup.getMapGroupInstance(mt);
    	}
    }
    
    
	/**
	 * This method initializes locationPanel	
	 * 	
	 * @return pt.lsts.neptus.gui.LocationPanel	
	 */    
	public LocationPanel getLocationPanel() {
		if (locationPanel == null) {
			locationPanel = new LocationPanel(getMissionType());
			locationPanel.hideButtons();
			locationPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
			
		}
		return locationPanel;
	}
	
	
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getJPanel() {
		if (jPanel == null) {
			jLabel1 = new JLabel();
			FlowLayout flowLayout1 = new FlowLayout();
			jLabel = new JLabel();
			jPanel = new JPanel();
			jPanel.setLayout(flowLayout1);
			jLabel.setText(I18n.text("Configuration File: "));
			flowLayout1.setHgap(5);
			jLabel1.setText("      ");
			jPanel.add(getJButton(), null);
			jPanel.add(jLabel1, null);
			
			jPanel.add(jLabel, null);
			jPanel.add(getConfigurationFile(), null);
			jPanel.add(getEditBtn(), null);
		}
		return jPanel;
	}
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<String> getConfigurationFile() {
		if (configurationFile == null) {
            String[] confs = TransponderUtils.getTranspondersConfsNamesList();
			configurationFile = new JComboBox<String>(confs);
			configurationFile.setPreferredSize(new Dimension(150,20));
			configurationFile.setEditable(false);
			configurationFile.setEnabled(true);
			configurationFile.setSelectedIndex(0);
			
			configurationFile.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        String beaconIDItem = (String) e.getItem();
                        if (beaconIDItem != null)
                            updateIdFromConfiguration(beaconIDItem);
                    }
                }
            });
		}
		return configurationFile;
	}
	
	public String getConfiguration() {
	    return (String) getConfigurationFile().getSelectedItem();
	}
	
	public void setConfiguration(String configuration) {
		if (configuration == null) {
		    updateIdFromConfiguration((String) getConfigurationFile().getSelectedItem());
		    return;
		}
		
		getConfigurationFile().setSelectedItem(configuration);
        updateIdFromConfiguration((String) getConfigurationFile().getSelectedItem());
	}
	
    private void updateIdFromConfiguration(String beaconIDItem) {
        idEditor.setText(beaconIDItem.replaceAll("\\.conf$", ""));
    }

    /**
	 * This method initializes jButton	
	 * 	
	 * @return javax.swing.JButton	
	 */    
	private JButton getJButton() {
		if (jButton == null) {
			jButton = new JButton();
			jButton.setText(I18n.text("Triangulation"));
			jButton.setPreferredSize(new java.awt.Dimension(110,25));
			jButton.addActionListener(new java.awt.event.ActionListener() { 
				public void actionPerformed(java.awt.event.ActionEvent e) {    
                    LocationType lt = TranspondersPositionHelper
                            .showTranspondersPositionHelperDialog(homeRef,
                                    TransponderParameters.this);
					if (lt != null)
						setLocation(lt);
				}
			});
		}
		return jButton;
	}
  	
  	public void setEditable(boolean value) {
		super.setEditable(value);
		locationPanel.setEditable(isEditable());
		if (!isEditable()) {
			getConfigurationFile().setEnabled(false);
			getJButton().setEnabled(false);
		}
		else {
			getConfigurationFile().setEnabled(true);
			getJButton().setEnabled(true);
		}
	}
	public JButton getEditBtn() {
		if (editBtn == null) {
			editBtn = new JButton(I18n.text("Edit file"));
			editBtn.setPreferredSize(new java.awt.Dimension(110,25));
			editBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(
                            new JFrame(),
                            "<html><strong>" + I18n.text("Full attention when altering this file") + "</strong>"
                                    + ", <br>"
                                    + I18n.text("The changes will apply to all existing missions!") + "</html>",
                            I18n.text("Warning"), JOptionPane.WARNING_MESSAGE);
						//FIXME : alterar o caminho para os ficheiros de configuração para o caminho correcto!
						(new EditorLauncher()).editFile(ConfigFetch.resolvePath("maps/"+getConfigurationFile().getSelectedItem()));					
				}
			});
		}
		return editBtn;
	}

	public CoordinateSystem getHomeRef() {
		return homeRef;
	}

	public void setHomeRef(CoordinateSystem homeRef) {
		this.homeRef = homeRef;
	}

    public static void main(String[] args) {
        
        JFrame testFrame = new JFrame("Teste Unitario");
        TransponderParameters nmp = new TransponderParameters(new CoordinateSystem(), new JTextField("id"));
        testFrame.add(nmp);
        testFrame.setSize(453, 450);
        testFrame.setVisible(true);

        final Vector<String> aTranspondersFiles = new Vector<String>();
        File dir = new File("maps/");
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                NeptusLog.pub().info("<###> "+name + ": " + name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z]+[0-9]+\\.conf)$"));
                if (name.matches("^(lsts[0-9]+\\.conf)|([A-Za-z]+[0-9]+\\.conf)$")) {
                    return true;
                }
                return false;
            }
        });
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.getName().startsWith("lsts") && !o2.getName().startsWith("lsts"))
                    return -1;
                else if (!o1.getName().startsWith("lsts") && o2.getName().startsWith("lsts"))
                    return 1;
                return o1.compareTo(o2);
            }
        });
        for (File file : files) {
            NeptusLog.pub().info("<###> "+file.getName());
            aTranspondersFiles.add(file.getName());
        }
    }
}
