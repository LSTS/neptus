/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.map.TransponderUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.NameNormalizer;

@SuppressWarnings("serial")
public class SimpleTransponderPanel extends SimpleLocationPanel {
	
	protected JTextField idField = new JTextField(7);
	protected JComboBox<String> configurationFile;
	protected TransponderElement transponder;

	protected ArrayList<String> takenNames = new ArrayList<>(); 
	
    public SimpleTransponderPanel(TransponderElement transponder, boolean idEditable, String[] takenNames) {
        super(transponder.getCenterLocation());
        if (takenNames.length > 0) {
            this.takenNames.addAll(Arrays.asList(takenNames));
        }
        removeAll();
        setLayout(new BorderLayout());
        add(getTopPanel(), BorderLayout.NORTH);
        add(selector, BorderLayout.CENTER);
        add(getButtonsPanel(), BorderLayout.SOUTH);
        this.transponder = transponder;
        getConfigurationFile().setSelectedItem(transponder.getConfiguration());
        setLocation(transponder.getCenterLocation());
        idField.setText(transponder.getId());
        idField.setEditable(idEditable);        
        idField.addFocusListener(new SelectAllFocusListener());
    }
    
	protected JPanel getTopPanel() {
		JPanel tmp = new JPanel(new FlowLayout());
		tmp.add(new JLabel(I18n.text("Beacon Name")+": "));
		tmp.add(idField);
		tmp.add(new JSeparator());
		tmp.add(new JLabel(I18n.text("Configuration")+": "));
		tmp.add(getConfigurationFile());
        ToolbarButton btn = new ToolbarButton(new ImageIcon(
                ImageUtils.getImage("images/menus/editpaste.png")), I18n.text("Paste copied location"),
                "pasteLoc");
		btn.addActionListener(new ActionListener() {
			@Override
            public void actionPerformed(ActionEvent e) {
					
				Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
				
				if ( (contents != null) &&
						contents.isDataFlavorSupported(DataFlavor.stringFlavor) ) {
					try {
						String text = (String)contents.getTransferData(DataFlavor.stringFlavor);
						LocationType lt = new LocationType();
						lt.fromClipboardText(text);
						selector.setLocationType(new LocationType(lt.convertToAbsoluteLatLonDepth()));
					}
					catch (Exception ex) {
						NeptusLog.pub().error(ex);
					}
				}
			}
		});

        ToolbarButton btnC = new ToolbarButton(new ImageIcon(
                ImageUtils.getImage("images/menus/editcopy.png")), I18n.text("Copy location"),
                "copyLoc");
        btnC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClipboardOwner owner = new ClipboardOwner() {
                    @Override
                    public void lostOwnership(
                            java.awt.datatransfer.Clipboard clipboard,
                            java.awt.datatransfer.Transferable contents) {
                    }
                };
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(
                                new StringSelection(getLocationType()
                                        .getClipboardText()), owner);
            }
        });

        tmp.add(btnC);
        tmp.add(btn);

		return tmp;
	}
	
	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */    
	private JComboBox<String> getConfigurationFile() {
		if (configurationFile == null) {
//		    String[] confs = new String[] {"lsts1.conf", "lsts2.conf", "lsts3.conf"/*, "lsts1m.conf", "lsts2m.conf", "lsts3m.conf"*/};
            String[] confs = TransponderUtils.getTranspondersConfsNamesList();
			configurationFile = new JComboBox<String>(confs);
			configurationFile.setPreferredSize(new Dimension(90,20));
			configurationFile.setEditable(false);
			configurationFile.setEnabled(true);
			configurationFile.setSelectedIndex(0);
			
		}
		return configurationFile;
	}
	
	public String getConfiguration() {
	    Object sel = getConfigurationFile().getSelectedItem();
        // NeptusLog.pub().info("<###>kkkkkkkkkkkkkkkkkkkkkkk    " + sel);
	    return sel.toString();
	}
	
	public void setConfiguration(String configuration) {
		if (configuration == null)
			return;
		
		getConfigurationFile().setSelectedItem(configuration);
	}
	
	private TransponderElement getTransponderElement() {
		if (isUserCancel())
			return null;
		
		if (getLocationType() == null)
			return null;
		
		if (getErrors() != null)
			return null;
		
		
		transponder.setCenterLocation(getLocationType());
		transponder.setConfiguration(getConfiguration());
		transponder.setId(idField.getText());
		
		return transponder;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(450, 290);		
	}
	
    public static TransponderElement showTransponderDialog(TransponderElement transponder,
            String title, boolean editable, boolean idEditable, String[] takenNames, Component parentComponent) {
		LocationType location = new LocationType();

		location.setLocation(transponder.getCenterLocation().convertToAbsoluteLatLonDepth());

		SimpleTransponderPanel tp = new SimpleTransponderPanel(transponder, idEditable, takenNames);
		tp.getLocationDialog(title, parentComponent);
		
		if (tp.isUserCancel())
			return null; 
		
		return tp.getTransponderElement();
	}
	
	@Override
	protected String getErrors() {
	
		if (super.getErrors() != null)
			return super.getErrors();
		
		if (!NameNormalizer.isNeptusValidIdentifier(idField.getText()))
			return I18n.text("The entered name is not valid");

		if (getConfigurationFile().getSelectedItem() == null)
			return I18n.text("No configuration file selected");
		
		return null;
		
	}
	
	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		TransponderElement elem = new TransponderElement(MapGroup.getNewInstance(new CoordinateSystem()), null);
		elem = showTransponderDialog(elem, "testing", true, true, new String[0], null);
		NeptusLog.pub().info("<###> "+elem);
	}
}
