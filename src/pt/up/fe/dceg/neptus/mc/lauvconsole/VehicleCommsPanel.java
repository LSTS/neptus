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
 * 2007/08/23
 * $Id:: VehicleCommsPanel.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.mc.lauvconsole;

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

import pt.up.fe.dceg.neptus.gui.SelectAllFocusListener;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.types.comm.protocol.IMCArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.ProtocolArgs;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.GeneralPreferences;


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
