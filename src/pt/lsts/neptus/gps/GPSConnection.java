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
 * Author: José Pinto
 * 2005/07/29
 */
package pt.lsts.neptus.gps;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.dinopolis.gpstool.gpsinput.GPSDataProcessor;
import org.dinopolis.gpstool.gpsinput.GPSDevice;
import org.dinopolis.gpstool.gpsinput.GPSException;
import org.dinopolis.gpstool.gpsinput.GPSPosition;
import org.dinopolis.gpstool.gpsinput.GPSPositionError;
import org.dinopolis.gpstool.gpsinput.GPSSerialDevice;
import org.dinopolis.gpstool.gpsinput.nmea.GPSNmeaDataProcessor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;
/**
 * This class provides a way for connecting to a GPS device and start listening for GPS events
 * @author ZP
 *
 */
public class GPSConnection implements PropertyChangeListener {

	private String serialPort = "COM1";
	private int baudRate = 9600;
	private GPSState currentGPSState = new GPSState();
	private GPSState lastGPSState = currentGPSState;
	private GPSDataProcessor processor = new GPSNmeaDataProcessor();
	private Vector<GPSListener> gpsListeners = new Vector<GPSListener>();
	private boolean okPressed = false;
	private Thread logger = null, simulator = null;
	
	/**
	 * Class constructor - creates a new GPSConnection with the given parameters.
	 * @param serialPort The serial port where to listen for the GPS device (Examples: 'COM2', '/dev/ttyS0')
	 * @param baudRate The baud rate of the connection (Examples: '9600', '1200')
	 */
	public GPSConnection(String serialPort, int baudRate) {
		this.serialPort = serialPort;
		this.baudRate = baudRate;
	}
	
	public void simulateGPS(String gpsLogFile) {
		final String logFileName = gpsLogFile;
		simulator = new Thread() {
			public void run() {
				try {
					 BufferedReader br = new BufferedReader(new FileReader(logFileName));
					 //To terminate this Thread all you have to do is to set logger = null
					 br.readLine();
					 String logString = br.readLine();
					 while (this == simulator && logString != null) {
						 currentGPSState = new GPSState(logString);
						 propertyChange(new PropertyChangeEvent(this, "Simulation", null, null));
						 Thread.sleep(1000);
						 logString = br.readLine();
					 }
					 br.close();
				 }
				 catch (Exception e) {
					 System.err.println("Error reading the log file "+logFileName);
					 return;
				 }
			};
		};
		simulator.start();
	}
	
	public void logReceivedData(String logFile) {
		 if (logger != null)
			 return;
		 final String logFileName = logFile;
		 logger = new Thread() {
			 public void run() {
				 try {
					 BufferedWriter bw = new BufferedWriter(new FileWriter(logFileName));
					 //To terminate this Thread all you have to do is to set logger = null
					 while (this == logger) {
						 
						 bw.write(currentGPSState.toLogFormattedString()+"\n");
						 Thread.sleep(1000);
					 }
					 bw.close();
				 }
				 catch (Exception e) {
					 NeptusLog.pub().error(this, e);
					 return;
				 }				 
			 };
		 };
		 logger.start();
	}
	
	/**
	 * Activates the connection and start receiving data. If the connection can't be established, a error dialog is presented
	 * and <b>false</b> is returned
	 * @return <b>true</b> if the connection could be established or <b>false</b> if an error has occured
	 */
	public boolean connect() {
		GPSDevice gpsDevice = new GPSSerialDevice();
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(GPSSerialDevice.PORT_NAME_KEY, serialPort);
		env.put(GPSSerialDevice.PORT_SPEED_KEY, new Integer(baudRate));
		try {
			gpsDevice.init(env);			
			processor.setGPSDevice(gpsDevice);
			processor.open();
			processor.addGPSDataChangeListener(this);
		}
		catch(GPSException gpsException) {
			GuiUtils.errorMessage(new JFrame(), "Error connecting to GPS", gpsException.getMessage());
			NeptusLog.pub().error(this, gpsException);
			return false;
		}
		return true;
	}
	
	/**
	 * Closes the current connection with the device.
	 */
	public void disconnect() {
		try {
			if (simulator != null)
				simulator = null;
			else {
				if (logger != null)
					logger = null;
				processor.close();
			}
		}
		catch(GPSException gpsException) {
			GuiUtils.errorMessage(new JFrame(), "Error while disconnecting from GPS", gpsException.getMessage());
		}
	}

	/**
	 * Every time a value of the GPS changes, this method is called
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		Object value = evt.getNewValue();
		String name = evt.getPropertyName();
		
		
		if (name.equals(GPSDataProcessor.LOCATION)) {
			currentGPSState.setLatitude(((GPSPosition) value).getLatitude());
			currentGPSState.setLongitude(((GPSPosition) value).getLongitude());			
		}
				
		if (name.equals(GPSDataProcessor.EPE)) {
			currentGPSState.setEstimatedError(((GPSPositionError) value).getHorizontalError());
		}

		if (name.equals(GPSDataProcessor.HDOP)) {
			currentGPSState.setHdop(((Float) value).floatValue());
		}

		if (name.equals(GPSDataProcessor.PDOP)) {
			currentGPSState.setPdop(((Float) value).floatValue());
		}

		if (name.equals(GPSDataProcessor.VDOP)) {
			currentGPSState.setVdop(((Float) value).floatValue());
		}

		if (name.equals(GPSDataProcessor.ALTITUDE)) {
			//NeptusLog.pub().info("<###>Altitude received!");
			currentGPSState.setAltitude(((Float) value).floatValue());
		}
		
		if (name.equals(GPSDataProcessor.SPEED)) {
			currentGPSState.setSpeed(((Float) value).floatValue());
		}
		
		if (name.equals(GPSDataProcessor.NUMBER_SATELLITES)) {
			currentGPSState.setNumberOfVisibleSatellites(((Integer) value).intValue());
		}

		if (name.equals(GPSDataProcessor.HEADING)) {
			currentGPSState.setHeading(((Float) value).floatValue());
		}

		int numListeners = gpsListeners.size();
		if (numListeners > 0) {
			for (int i = 0; i < numListeners; i++) {
				((GPSListener)gpsListeners.get(i)).GPSStateChanged(lastGPSState, currentGPSState);
			}
		}
		lastGPSState = (GPSState) currentGPSState.clone();
		//NeptusLog.pub().info("<###> "+currentGPSState);
	};
	
	/**
	 * Add a GPSListener to this connection - Every time the GPSState changes, the listeners will be informed
	 * @param listener The GPSListener that will be informed of the GPS state changes
	 */
	public void addGPSListener(GPSListener listener) {
		gpsListeners.add(listener);
		// envia o estado corrente do GPS
		GPSState dummyState = new GPSState();
		dummyState.setAltitude(Integer.MIN_VALUE);
		listener.GPSStateChanged(dummyState, currentGPSState);
	}
	
	/**
	 * Stop sending events for the given GPSListener
	 * @param listener The GPSListener that will no longer receive GPS events
	 */
	public void removeGPSListener(GPSListener listener) {
		gpsListeners.remove(listener);
	}
	
	/**
	 * Presents a dialog with the connection parameters.
	 * If the user pressed the 'Cancel' button, this method returns false.
	 * If the user pressed 'OK', returns true.
	 */
	public boolean showConnectionDialog() {
		final JDialog topLevel = new JDialog(new JFrame(), "GPS connection parameters");
		
		JPanel params = new JPanel(new GridLayout(3,1));
		JPanel portPanel = new JPanel();
		JPanel baudRatePanel = new JPanel();
		JPanel controlsPanel = new JPanel();
		
		final JTextField portField = new JTextField(serialPort);
		portField.setColumns(10);
		
		final JTextField baudRateField = new JTextField(String.valueOf(baudRate));
		baudRateField.setColumns(10);
		
		portPanel.add(new JLabel("Serial Port: "));
		portPanel.add(portField);
		
		baudRatePanel.add(new JLabel("Baud Rate: "));
		baudRatePanel.add(baudRateField);
		
		JButton cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				topLevel.setVisible(false);
				topLevel.dispose();
			};
		});
		
		JButton okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				try {
					baudRate = Integer.parseInt(baudRateField.getText());
				}
				catch (Exception c) {
					GuiUtils.errorMessage(topLevel, "Invalid parameters", "The entered baud rate is not valid");
					return;
				}
				serialPort = portField.getText();
				if (serialPort.length() == 0) {
					GuiUtils.errorMessage(topLevel, "Invalid parameters", "The serial port field is empty");
					return;		
				}
				okPressed = true;
				topLevel.setVisible(false);
				topLevel.dispose();
			};
		});		
		
		controlsPanel.add(cancelBtn);
		controlsPanel.add(okBtn);
		
		params.add(portPanel);
		params.add(baudRatePanel);
		params.add(controlsPanel);
		
		topLevel.setContentPane(params);
		topLevel.pack();
		topLevel.setSize(300, 150);
		topLevel.setModal(true);
		topLevel.setAlwaysOnTop(true);
		GuiUtils.centerOnScreen(topLevel);
		topLevel.setVisible(true);
		return okPressed;
	}
	
	/**
	 * Returns the current values read by the GPS
	 * @return the current values read by the GPS in the form of a GPSState object
	 */
	public GPSState getCurrentState() {
		return (GPSState) currentGPSState.clone();
	}
	
	/**
	 * Unit testing
	 * @param args
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		GPSConnection conn = new GPSConnection("COM10", 9600);
//		if (conn.showConnectionDialog()) {
			conn.connect();
			conn.addGPSListener(new GPSListener() {
				public void GPSStateChanged(GPSState oldState, GPSState newState) {
					NeptusLog.pub().info("<###> "+newState.toString());
				};
			});
			//conn.disconnect();
//		}
			
			
	}
}
