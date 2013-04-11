 package pt.up.fe.dceg.neptus.serial;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.GuiUtils;

@SuppressWarnings("serial")
public class PortSelector extends JDialog {

	private JPanel back = null;
	private JPanel controls = null;
	private JButton cancelBtn = null;
	private JButton connectBotao = null;
	private JPanel main = null;
	private JLabel jLabel = null;
	private JComboBox<String> portCombo = null;
	private Vector<CommPortIdentifier> ports = new Vector<CommPortIdentifier>();
	private CommPortIdentifier selectedPort = null;
	private boolean parallelPortsShown = true;
	private boolean serialPortsShown = true;
	private boolean parametersShown = true;
	private SerialPortParameters serialPortParameters = null;
	
	/**
	 * This method initializes 
	 * 
	 */
	public PortSelector() {
		super();
		initialize();
	}

	public PortSelector(Window owner) {
		super(owner);
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        this.setSize(new java.awt.Dimension(340,210));
        this.setTitle("Port selection");
        this.setContentPane(getBack());
	}

	/**
	 * This method initializes back	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getBack() {
		if (back == null) {
			back = new JPanel();
			back.setLayout(new BorderLayout());
			back.add(getControls(), java.awt.BorderLayout.SOUTH);
			back.add(getMain(), java.awt.BorderLayout.CENTER);
		}
		return back;
	}

	/**
	 * This method initializes controls	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getControls() {
		if (controls == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
			controls = new JPanel();
			controls.setLayout(flowLayout);
			controls.add(getCancelBtn(), null);
			controls.add(getConnectBtn(), null);
		}
		return controls;
	}

	/**
	 * This method initializes cancelBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getCancelBtn() {
		if (cancelBtn == null) {
			cancelBtn = new JButton();
			cancelBtn.setText("Cancel");
			cancelBtn.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					dispose();
				}
			});
		}
		return cancelBtn;
	}

	/**
	 * This method initializes connectBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getConnectBtn() {
		if (connectBotao == null) {
			connectBotao = new JButton();
			connectBotao.setText("Connect");
			connectBotao.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {					
					if (portCombo.getSelectedIndex() == -1) {
						return;
					}
					else {
						selectedPort = ports.get(portCombo.getSelectedIndex());
						try {
							CommPort cmp = selectedPort.open("PortSelector", 1000);								
							//if (selectedPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
								
								/*
								NeptusLog.pub().info("<###>BAUD RATE: "+sp.getBaudRate());
								NeptusLog.pub().info("<###>DATA BITS: "+sp.getDataBits());
								NeptusLog.pub().info("<###>PARITY: "+sp.getParity());
								NeptusLog.pub().info("<###>STOP BITS: "+sp.getStopBits());
								*/
							//}
							cmp.close();
						}
						catch (Exception ex) {
							ex.printStackTrace();
							GuiUtils.errorMessage(PortSelector.this, "Port in use", "The selected port is already being used by other application");
						}
						dispose();
					}					
				}
			});
		}
		return connectBotao;
	}

	/**
	 * This method initializes main	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getMain() {
		if (main == null) {
			FlowLayout flowLayout1 = new FlowLayout();
			flowLayout1.setVgap(20);
			flowLayout1.setHgap(20);
			jLabel = new JLabel();
			jLabel.setText("Select port to use:");
			jLabel.addMouseListener(new java.awt.event.MouseAdapter() {
				public void mouseClicked(java.awt.event.MouseEvent e) {
										
				}
			});
			main = new JPanel();
			main.setLayout(flowLayout1);
			main.add(jLabel, null);
			main.add(getPortCombo(), null);
			main.add(getSerialPortParameters(), null);
			//serialPortParameters.setVisible(false);			
			
			serialPortParameters.setBorder(BorderFactory.createLoweredBevelBorder());
			serialPortParameters.setEnabled(false);
			
			main.add(serialPortParameters);
		}
		return main;
	}	

	/**
	 * This method initializes portCombo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox<String> getPortCombo() {
		if (portCombo == null) {
			
			
			portCombo = new JComboBox<String>();			
			portCombo.setPreferredSize(new java.awt.Dimension(120,20));
			portCombo.addItemListener(new java.awt.event.ItemListener() {
				public void itemStateChanged(java.awt.event.ItemEvent e) {
					if (portCombo.getSelectedIndex() == -1)
						serialPortParameters.setEnabled(false);
					else {
						if (ports.get(portCombo.getSelectedIndex()).getPortType() == CommPortIdentifier.PORT_PARALLEL)
							serialPortParameters.setEnabled(false);
						else
							serialPortParameters.setEnabled(true);
					}
				}
			});
			
			refreshPortList();
			
		}
		return portCombo;
	}
	
	/**
	 * @return the selectedPort
	 */
	public CommPortIdentifier getSelectedPort() {
		return selectedPort;
	}
	
	public static CommPortIdentifier showPortSelectionDialog() {
		PortSelector ps = new PortSelector();
		ps.setParametersShown(false);
		ps.setResizable(false);
		ps.setModalityType(ModalityType.DOCUMENT_MODAL);
		ps.setVisible(true);
		
		return ps.selectedPort;		
	}
	
	

	public static PortSelector showSerialPortSelectionDialog(Window owner,
			boolean showParameters) {
		PortSelector ps = new PortSelector(owner);
		ps.setTitle("Serial Port Selection");
		ps.setSerialPortsShown(true);
		ps.setParallelPortsShown(false);
		ps.setParametersShown(showParameters);
		ps.setResizable(false);
		ps.setModalityType(ModalityType.DOCUMENT_MODAL);
		GuiUtils.centerOnScreen(ps);
		ps.setVisible(true);

		return ps;
	}
	
	public static CommPortIdentifier showParallelPortSelectionDialog() {
		PortSelector ps = new PortSelector();
		ps.setTitle("Parallel Port Selection");
		ps.setSerialPortsShown(false);
		ps.setParallelPortsShown(true);
		ps.setParametersShown(false);
		ps.setResizable(false);
		ps.setModalityType(ModalityType.DOCUMENT_MODAL);
		ps.setVisible(true);
		
		return ps.selectedPort;		
	}
	


	public boolean isParallelPortsShown() {
		return parallelPortsShown;
	}

	public void setParallelPortsShown(boolean parallelPortsShown) {
		this.parallelPortsShown = parallelPortsShown;
		refreshPortList();
	}

	public boolean isSerialPortsShown() {
		return serialPortsShown;
	}

	public void setParametersShown(boolean parametersShown) {
		this.parametersShown = parametersShown;
		getSerialPortParameters().setVisible(parametersShown);
	}

	public boolean isParametersShown() {
		return parametersShown;
	}

	public void setSerialPortsShown(boolean serialPortsShown) {
		this.serialPortsShown = serialPortsShown;
		refreshPortList();
	}
	
	private void refreshPortList() {
		ports.removeAllElements();
		Enumeration<?> avPorts = enumerateComPorts();
		
		while (avPorts.hasMoreElements()) {
			CommPortIdentifier commID = (CommPortIdentifier) avPorts.nextElement();
			if (commID.getPortType() == CommPortIdentifier.PORT_PARALLEL && isParallelPortsShown()) {
				ports.add(commID);
			}
			if (commID.getPortType() == CommPortIdentifier.PORT_SERIAL && isSerialPortsShown()) {
				ports.add(commID);
			}
		}

		portCombo.removeAllItems();
		for (CommPortIdentifier commID : ports) {
			portCombo.insertItemAt(commID.getName(), portCombo.getItemCount());			
		}
	}
	
	/**
	 * This method initializes serialPortParameters	
	 * 	
	 * @return pt.up.fe.dceg.neptus.util.transponders.SerialPortParameters	
	 */
	public SerialPortParameters getSerialPortParameters() {
		if (serialPortParameters == null) {
			serialPortParameters = new SerialPortParameters();
			
		}
		return serialPortParameters;
	}
	
	
	/**
	 * Workaround to list virtual ports correctly
	 * Found in "http://forum.java.sun.com/thread.jspa?threadID=575580&messageID=2874228"
	 * @return
	 */
	public static Enumeration<?> enumerateComPorts() {
		try {
			Field masterIdList_Field = CommPortIdentifier.class
					.getDeclaredField("masterIdList");
			masterIdList_Field.setAccessible(true);
			masterIdList_Field.set(null, null);

			String temp_string = System.getProperty("java.home")
					+ File.separator + "lib" + File.separator
					+ "javax.comm.properties";
			Method loadDriver_Method = CommPortIdentifier.class
					.getDeclaredMethod("loadDriver",
							new Class[] { String.class });
			loadDriver_Method.setAccessible(true); // unprotect it
			loadDriver_Method.invoke(null, new Object[] { temp_string });
		} catch (Exception e) {
			NeptusLog.pub().debug(e);
		}

		return CommPortIdentifier.getPortIdentifiers();
	}

	public static void main(String[] args) {
		System.err.println(PortSelector.showSerialPortSelectionDialog(null, true));
		System.err.println(PortSelector.showParallelPortSelectionDialog());
		System.err.println(PortSelector.showPortSelectionDialog());
	}
	
}
