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
 * Author: Paulo Dias
 * 2010/06/29
 */
package pt.up.fe.dceg.neptus.types.miscsystems.config;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
public class SideScanConfigPanel extends JDialog {

	private JPanel jPanel = null;

	private boolean userCanceled = false;
	
	private String title = "Imagenex 881SS";
	
	private JLabel labelHight = null;
	private JLabel labelLow = null;

	private JComboBox<?> ctype = null;
	private JComboBox<String> cvalues = null;

	private JButton okButton = null;
	private JButton cancelButton = null;

	private String[] freqType = { "HIGH", "LOW" };
    private HashMap<String, String[]> freqValues = new HashMap<String, String[]>() {{
            put("HIGH", new String[] { "15", "30" });
            put("LOW", new String[] { "15", "30", "60", "90", "120" });}};
    
    private String defaultType = "HIGH";
    private String defaultValue = "30";

	public SideScanConfigPanel(Window parent) {
		super(parent);
		initialize();
	}

	public SideScanConfigPanel(Window parent, String title, String[] freqType,
	        HashMap<String, String[]> freqValues) {
	    this(parent, title, freqType, freqValues, "", "");
	}

	public SideScanConfigPanel(Window parent, String title, String[] freqType,
	        HashMap<String, String[]> freqValues, String defaultType, String defaultValue) {
		super(parent);
		this.title = title;
		this.freqType = freqType;
		this.freqValues = freqValues;
		this.defaultType = defaultType;
		this.defaultValue = defaultValue;
		initialize();
	}

	public void initialize() {
		this.setContentPane(getJPanel());
		this.setSize(230, 145);
		this.setResizable(false);
		this.setTitle(title);
	}

	/**
	 * @param freqType the freqType to set
	 */
	public void setFreqType(String[] freqType) {
		this.freqType = freqType;
	}

	/**
     * @param freqValues the freqValues to set
     */
    public void setFreqValues(HashMap<String, String[]> freqValues) {
        this.freqValues = freqValues;
    }
    
    
    /**
     * @return the userCanceled
     */
    public boolean isUserCanceled() {
        return userCanceled;
    }
    
	private JComboBox<?> getCtype() {
		if (ctype == null) {

			ctype = new JComboBox<Object>(freqType);
			if (defaultType.length() != 0) {
			    for (String vl : freqType) {
			        if (defaultType.equals(defaultType)) {
			            ctype.setSelectedItem(vl);
			            break;
			        }
			    }
			}
			ctype.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				    for (String type : freqType) {
                        if (type.equals(ctype.getSelectedItem())) {
                            String sel;
                            try {
                                sel = (String) cvalues.getSelectedItem();
                            }
                            catch (Exception e1) {
                                sel = defaultValue;
                            }
                            cvalues.removeAllItems();
                            for (String s : freqValues.get(type)) {
                                cvalues.addItem(s);
                            }
                            if (sel.length() != 0) {
                                for (String vl : freqValues.get(type)) {
                                    if (vl.equals(sel)) {
                                        cvalues.setSelectedItem(vl);
                                        break;
                                    }
                                }
                            }
                        }
                    }
				}
			});
			ctype.setBounds(100, 10, 100, 25);
		}
		return ctype;
	}

	private JComboBox<String> getCvalues() {
		if (cvalues == null) {
			cvalues = new JComboBox<String>(freqValues.get(getCtype().getSelectedItem()));
			cvalues.setBounds(100, 40, 100, 25);
		}
		return cvalues;
	}

	private JPanel getJPanel() {
		if (jPanel == null) {

			jPanel = new JPanel();
			jPanel.setLayout(null);

			labelHight = new JLabel("Frequency");
			labelHight.setBounds(10, 10, 70, 25);
			labelHight.setHorizontalAlignment(JLabel.RIGHT);

			labelLow = new JLabel("Ranges");
			labelLow.setBounds(10, 40, 70, 25);
			labelLow.setHorizontalAlignment(JLabel.RIGHT);

			jPanel.add(labelLow, null);
			jPanel.add(labelHight, null);

			jPanel.add(getCvalues(), null);
			jPanel.add(getCtype(), null);

			jPanel.add(getOkButton(), null);
			jPanel.add(getCancelButton(), null);
		}
		return jPanel;
	}

	public JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText("ok");
			okButton.setBounds(30, 80, 78, 25);
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setReturnValues();
					setVisible(false);
					userCanceled = false;
					dispose();
				}
			});
			GuiUtils.reactEnterKeyPress(okButton);
		}
		return okButton;
	}

	public JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText("cancel");
			cancelButton.setBounds(122, 80, 78, 25);
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					userCanceled = true;
					dispose();
				}
			});
			GuiUtils.reactEscapeKeyPress(cancelButton);
		}
		return cancelButton;
	}

	public void setReturnValues() {
		// set the selected values on the "parent" object
		NeptusLog.pub().info("<###>Type: " + ctype.getSelectedItem());
		NeptusLog.pub().info("<###>Value: " + cvalues.getSelectedItem());
	}

	public boolean setSelectedFrequency(String str) {
		ctype.setSelectedItem(str);
		if (ctype.getSelectedItem().toString().equalsIgnoreCase(str))
			return true;
		else
			return false;
	}
	
	public String getSelectedFrequency() {
		return ctype.getSelectedItem().toString();
	}

	public boolean setSelectedRange(String str) {
		cvalues.setSelectedItem(str);
		if (cvalues.getSelectedItem().toString().equalsIgnoreCase(str))
			return true;
		else
			return false;
	}

	public String getSelectedRange() {
		return cvalues.getSelectedItem().toString();
	}

	public static void showDialog(Frame parentFrame) {
		SideScanConfigPanel d = new SideScanConfigPanel(parentFrame);
		d.setModalityType(ModalityType.DOCUMENT_MODAL);
		d.setVisible(true);
	}

	public static void main(String args[]) {
	    JFrame frame = new JFrame();
		SideScanConfigPanel d = new SideScanConfigPanel(frame);
		System.out.println(d.setSelectedFrequency("Low"));
		System.out.println(d.setSelectedRange("15"));
		d.setModalityType(ModalityType.DOCUMENT_MODAL);
		d.setVisible(true);
		frame.dispose();
	}

}