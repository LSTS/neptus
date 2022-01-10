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
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleConstants.ColorConstants;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 */
public class LogBook extends JPanel {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private JTextPane msgTextArea = new JTextPane();	
	private static String operatorName = System.getProperty("user.name");
	private JLabel operatorLbl = new JLabel(operatorName);
	private JTextField messageField = new JTextField();
    private IMCMessage logMsg = new IMCMessage("LogBookEntry");
	public String getOperatorName() {
		return operatorName;
	}

	public void setOperatorName(String operatorName) {
		LogBook.operatorName = operatorName;
	}

	private String getTimeStamp() {
        return dateFormat.format(new Date(System.currentTimeMillis()));
    }
    
	private synchronized void writeMessageText(String message) {
        SimpleAttributeSet attrTS = new SimpleAttributeSet();
        attrTS.addAttribute(ColorConstants.Foreground, Color.DARK_GRAY);
        attrTS.addAttribute(StyleConstants.Bold, true);
        logMsg.setValue("source", "ccu");
        logMsg.setValue("destination", "ccu");
        
        Document doc = msgTextArea.getDocument();
        try {
        	SimpleAttributeSet attr = new SimpleAttributeSet();
            attr.addAttribute(ColorConstants.Foreground, Color.blue);            
            doc.insertString(doc.getLength(), "[" + getTimeStamp() + "]: ", attrTS);
            
            attr = new SimpleAttributeSet();
            attr.addAttribute(ColorConstants.Foreground, Color.black);
            doc.insertString(doc.getLength(), message+"\n", attr);
            
            msgTextArea.setCaretPosition(doc.getLength());
        }
        catch (Exception e) {}
    }
	
	public LogBook() {		
	    msgTextArea.setEditable(false);
		setLayout(new BorderLayout(3,3));
		setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		topPanel.add(new JLabel("Operator: "));
		operatorLbl.setForeground(Color.blue.darker());
		topPanel.add(operatorLbl);
		
		JButton btn = new JButton("Change...");		
		topPanel.add(btn);
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String opName = JOptionPane.showInputDialog(LogBook.this, "Change operator name", operatorName);
				if (opName != null) {
					operatorName = opName;
					operatorLbl.setText(operatorName);
				}
			}
		});
		
		add(topPanel, BorderLayout.NORTH);
		add(new JScrollPane(msgTextArea), BorderLayout.CENTER);
		
		JPanel bottom = new JPanel(new BorderLayout(3,3));
		bottom.add(messageField, BorderLayout.CENTER);
		JButton btn2 = new JButton("Add Entry");
		ActionListener addActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (messageField.getText().isEmpty())
					return;
				
				writeMessageText(messageField.getText());
				
				try {
					//logMsg.setValue("op", operatorName);
					logMsg.setValue("context", operatorName);
					logMsg.setValue("htime", DateTimeUtil.timeStampSeconds());
					logMsg.setValue("type", 0);
					logMsg.setValue("text", messageField.getText());			
					LsfMessageLogger.log(logMsg);							
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				messageField.setText("");
			}
		};
		
		btn2.addActionListener(addActionListener);		
		messageField.addActionListener(addActionListener);
		
		bottom.add(btn2, BorderLayout.EAST);
		add(bottom, BorderLayout.SOUTH);		
	}
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		GuiUtils.setLookAndFeel();
		GuiUtils.testFrame(new LogBook());
	}
}
