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
 * 20??/??/??
 * $Id:: SerialPortParameters.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.serial;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.GuiUtils;

public class SerialPortParameters extends JPanel {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JPanel jPanel = null;
	private JLabel jLabel = null;
	private JComboBox<?> baudRateCombo = null;
	private JPanel jPanel1 = null;
	private JLabel jLabel1 = null;
	private JComboBox<?> stopBitsCombo = null;
	private JPanel jPanel2 = null;
	private JLabel jLabel2 = null;
	private JComboBox<?> parityCombo = null;
	private JPanel jPanel3 = null;
	private JLabel jLabel3 = null;
	private JComboBox<?> dataBitsCombo = null;

	/**
	 * This method initializes 
	 * 
	 */
	public SerialPortParameters() {
		super();
		initialize();
	}

	/**
	 * This method initializes this
	 * 
	 */
	private void initialize() {
        this.setSize(new Dimension(340,75));
        this.setPreferredSize(new Dimension(340,75));
        this.add(getJPanel(), null);
        this.add(getJPanel2(), null);
        this.add(getJPanel1(), null);
        this.add(getJPanel3(), null);
			
	}

	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
			jLabel = new JLabel();
			jLabel.setText("Baud Rate");
			jPanel = new JPanel();
			jPanel.setLayout(flowLayout);
			jPanel.setPreferredSize(new java.awt.Dimension(160,30));
			jPanel.add(jLabel, null);
			jPanel.add(getBaudRateCombo(), null);
		}
		return jPanel;
	}

	/**
	 * This method initializes baudRateCombo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox<?> getBaudRateCombo() {
		if (baudRateCombo == null) {
			baudRateCombo = new JComboBox<Object>(new String[] {"110", "300", "1200", "2400", "4800", "9600", "19200", "38400", "57600", "115200"});
			baudRateCombo.setSelectedIndex(5);
			baudRateCombo.setPreferredSize(new java.awt.Dimension(80,20));
		}
		return baudRateCombo;
	}

	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			FlowLayout flowLayout2 = new FlowLayout();
			flowLayout2.setAlignment(java.awt.FlowLayout.RIGHT);
			jLabel1 = new JLabel();
			jLabel1.setText("Stop Bits");
			jPanel1 = new JPanel();
			jPanel1.setLayout(flowLayout2);
			jPanel1.setPreferredSize(new java.awt.Dimension(160,30));
			jPanel1.add(jLabel1, null);
			jPanel1.add(getStopBitsCombo(), null);
		}
		return jPanel1;
	}

	/**
	 * This method initializes stopBitsCombo	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox<?> getStopBitsCombo() {
		if (stopBitsCombo == null) {
			stopBitsCombo = new JComboBox<Object>(new String[] {"1", "1.5", "2"});
			stopBitsCombo.setPreferredSize(new java.awt.Dimension(80,20));
		}
		return stopBitsCombo;
	}

	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			FlowLayout flowLayout1 = new FlowLayout();
			flowLayout1.setAlignment(java.awt.FlowLayout.RIGHT);
			jLabel2 = new JLabel();
			jLabel2.setText("Parity");
			jPanel2 = new JPanel();
			jPanel2.setLayout(flowLayout1);
			jPanel2.setPreferredSize(new java.awt.Dimension(160,30));
			jPanel2.add(jLabel2, null);
			jPanel2.add(getParityCombo(), null);
		}
		return jPanel2;
	}

	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox<?> getParityCombo() {
		if (parityCombo == null) {
			parityCombo = new JComboBox<Object>(new String[] {"None", "Even", "Odd", "Mark", "Space"} );
			parityCombo.setPreferredSize(new java.awt.Dimension(80,20));
		}
		return parityCombo;
	}

	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			FlowLayout flowLayout3 = new FlowLayout();
			flowLayout3.setAlignment(java.awt.FlowLayout.RIGHT);
			jLabel3 = new JLabel();
			jLabel3.setText("Data Bits");
			jPanel3 = new JPanel();
			jPanel3.setLayout(flowLayout3);
			jPanel3.setPreferredSize(new java.awt.Dimension(160,30));
			jPanel3.add(jLabel3, null);
			jPanel3.add(getDataBitsCombo(), null);
		}
		return jPanel3;
	}

	/**
	 * This method initializes jComboBox	
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox<?> getDataBitsCombo() {
		if (dataBitsCombo == null) {
			dataBitsCombo = new JComboBox<Object>(new String[] {"8", "7", "6", "5"});
			dataBitsCombo.setPreferredSize(new Dimension(80, 20));
		}
		return dataBitsCombo;
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		super.setEnabled(enabled);
		dataBitsCombo.setEnabled(enabled);
		stopBitsCombo.setEnabled(enabled);
		parityCombo.setEnabled(enabled);
		baudRateCombo.setEnabled(enabled);
	}
	
	public int getBaudrate() {
		try {
			return Integer.parseInt(getBaudRateCombo().getSelectedItem().toString());
		} catch (Exception e) {
			e.printStackTrace();
			return 9600;
		}
	}
	
	public int getDataBitsNumber() {
		try {
			return Integer.parseInt(getDataBitsCombo().getSelectedItem().toString());
		} catch (Exception e) {
			e.printStackTrace();
			return 8;
		}
	}	

	public String getParity() {
		try {
			return getParityCombo().getSelectedItem().toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "None";
		}
	}

	public int getParityCode() {
		try {
			return getParityCombo().getSelectedIndex();
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}

	public String getStopBits() {
		try {
			return getStopBitsCombo().getSelectedItem().toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "1";
		}
	}

	public int getStopBitsCode() {
		try {
			String valurStr = getStopBitsCombo().getSelectedItem().toString();
			if ("1".equalsIgnoreCase(valurStr))
				return 1;
			else if ("1".equalsIgnoreCase(valurStr))
				return 2;
			else if ("1".equalsIgnoreCase(valurStr))
				return 3;
			else
				return 1;
		} catch (Exception e) {
			e.printStackTrace();
			return 1;
		}
	}

	public static void main(String args[]) {
		SerialPortParameters sp = new SerialPortParameters();
		GuiUtils.testFrame(sp, "Serial Port Params");
		try {Thread.sleep(7000);}catch (Exception e) {}
		System.out.printf("BaudRate: %s\nDataBits: %s\nParity: %s\nStopBits: %s", 
				sp.getBaudrate(), sp.getDataBitsNumber(), sp.getParity(), sp.getStopBits());
	}

}  //  @jve:decl-index=0:visual-constraint="10,10"
