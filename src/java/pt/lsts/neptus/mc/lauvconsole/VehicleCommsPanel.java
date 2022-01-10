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
 * 2007/08/23
 */
package pt.lsts.neptus.mc.lauvconsole;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.dom4j.Document;

import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.comm.protocol.IMCArgs;
import pt.lsts.neptus.types.comm.protocol.ProtocolArgs;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;


/**
 * @author pdias
 *
 */
class VehicleCommsPanel extends JPanel {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JTextField ipField = new JTextField(20);
	private JTextField portField = new JTextField(4);
	private JTextField localPortField = new JTextField(4);
	private JDialog mainFrame = null;
	
	private int port = 6002;
	private int localPort = 6001;
	private String hostname = "127.0.0.1";
	
	public VehicleCommsPanel(JDialog mainFrame) {
		this.mainFrame = mainFrame;
		
		init();
		
		setLayout(new TableLayout(new double[] {5,100,5, TableLayout.FILL,5}, new double[] {5,25,5,25,5,25,5,35}));
		add(new JLabel("remote hostname:", JLabel.RIGHT), "1,1");
		add(new JLabel("remote port:", JLabel.RIGHT), "1,3");
		add(new JLabel("local port:", JLabel.RIGHT), "1,5");
		
		ipField.addFocusListener(new SelectAllFocusListener());
		portField.addFocusListener(new SelectAllFocusListener());
		localPortField.addFocusListener(new SelectAllFocusListener());
		
		add(ipField, "3,1");
		add(portField, "3,3");
		add(localPortField, "3,5");
		
		ipField.setText(hostname);
		portField.setText(""+port);
		localPortField.setText(""+localPort);		
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					int tmpport = Integer.parseInt(portField.getText());
					int tmpLocalPort = Integer.parseInt(localPortField.getText());
					
					if (!(tmpLocalPort == localPort && hostname.equalsIgnoreCase((ipField.getText())) && tmpport == port)) {
						hostname = ipField.getText();
						port = tmpport;
						localPort = tmpLocalPort;
						save();
					}
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}				
				close();
			}
		});
		
		okButton.setPreferredSize(new Dimension(80, 25));		
		
		buttonsPanel.add(okButton);
		
		
		JButton cancelButton = new JButton("Cancel");
		
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		cancelButton.setPreferredSize(new Dimension(80, 25));
		
		buttonsPanel.add(cancelButton);
		add(buttonsPanel, "1,7,3,7");
		
		GuiUtils.reactEnterKeyPress(okButton);
		GuiUtils.reactEscapeKeyPress(cancelButton);
		
	}
	
	private void close() {
		if (mainFrame == null)
			return;
		mainFrame.setVisible(false);
		mainFrame.dispose();
	}
	
	private void init() {
		
		VehicleType vehicle = VehiclesHolder.getVehicleById(LAUVConsole.lauvVehicle);
		
		CommMean cm = vehicle.getCommunicationMeans().values().iterator().next();
		
		if (cm != null) {
			this.hostname = cm.getHostAddress();
			for (ProtocolArgs pArgs : cm.getProtocolsArgs().values())
            {
                if (pArgs instanceof IMCArgs)
                {
                    IMCArgs nArgs = (IMCArgs) pArgs;                    
                    this.port = nArgs.getPort();       
                    break;
                }	                
            }
		}
		
		localPort = GeneralPreferences.commsLocalPortUDP;
	}

	private void save() {
		VehicleType vehicle = VehiclesHolder.getVehicleById(LAUVConsole.lauvVehicle);

		CommMean cm = vehicle.getCommunicationMeans().values().iterator().next();

		if (cm != null) {
			cm.setHostAddress(hostname);

			for (ProtocolArgs pArgs : cm.getProtocolsArgs().values())
			{
				if (pArgs instanceof IMCArgs)
				{
					IMCArgs nArgs = (IMCArgs) pArgs;                    
					nArgs.setPort(port);       
					break;
				}	                
			}
		}
		try {
            GeneralPreferences.saveProperties();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		

		String filePath = vehicle.getOriginalFilePath();
		Document doc = vehicle.asDocument();
		String dataToSave = FileUtil.getAsPrettyPrintFormatedXMLString(doc);
		FileUtil.saveToFile(filePath, dataToSave);
	}
}
