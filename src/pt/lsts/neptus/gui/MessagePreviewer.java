/*
 * Below is the copyright agreement for IMCJava.
 * 
 * Copyright (c) 2010-2019, Laboratório de Sistemas e Tecnologia Subaquática
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the names of IMC, LSTS, IMCJava nor the names of its 
 *       contributors may be used to endorse or promote products derived from 
 *       this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL LABORATORIO DE SISTEMAS E TECNOLOGIA SUBAQUATICA
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Author keila 
 * Dec,2019
 */
package pt.lsts.neptus.gui;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.jhe.hexed.JHexEditor;

import pt.lsts.imc.Abort;
import pt.lsts.imc.Goto;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.net.UDPTransport;
import pt.lsts.imc.sender.FormatUtils;
import pt.lsts.imc.sender.UIUtils;
import pt.lsts.neptus.util.llf.MessageHtmlVisualization;

public final class MessagePreviewer extends JPanel {

	private static final long serialVersionUID = -449856037981913932L;

	public enum MODE {JSON, XML, HEX, HTML}

	private final IMCMessage msg; 
	private MODE mode;
	private JToggleButton xmlToggle = new JToggleButton("XML");
	private JToggleButton jsonToggle = new JToggleButton("JSON");
	private JToggleButton hexToggle = new JToggleButton("HEX");
	private JToggleButton htmlToggle = new JToggleButton("HTML");

	private JPanel centerPanel = new JPanel(new CardLayout());
	private RSyntaxTextArea xmlTextArea, jsonTextArea;
    private MessageHtmlVisualization htmlMsgPreview;
	private RTextScrollPane xmlScroll, jsonScroll;
	private JHexEditor hexEditor;
	private JScrollPane hexScroll;
	
	
	public MessagePreviewer(IMCMessage m, boolean xml, boolean json, boolean hex) {
		this.msg = m;
		setLayout(new BorderLayout());
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ButtonGroup bgGroup = new ButtonGroup();
		

		
		htmlMsgPreview = new MessageHtmlVisualization(m);
		htmlMsgPreview.getComponent().setEnabled(false);
		htmlMsgPreview.getComponent().setBackground(Color.white);
        bgGroup.add(htmlToggle);
        top.add(htmlToggle);
        centerPanel.add(htmlMsgPreview.getComponent(), "html");
		
		if(xml) {
			xmlTextArea = new RSyntaxTextArea();
			xmlTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
			xmlTextArea.setEditable(false);
			xmlScroll = new RTextScrollPane(xmlTextArea);
			bgGroup.add(xmlToggle);
			top.add(xmlToggle);
			centerPanel.add(xmlScroll, "xml");
		}
		if(json) {
			jsonTextArea = new RSyntaxTextArea();
			jsonTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
			jsonTextArea.setEditable(false);
			jsonScroll = new RTextScrollPane(jsonTextArea);
			bgGroup.add(jsonToggle);
			top.add(jsonToggle);
			centerPanel.add(jsonScroll, "json");
		}
		if(hex) {
			hexEditor = new JHexEditor(new byte[0]);
			hexEditor.setEnabled(false);
			hexEditor.setBackground(Color.white);
			hexScroll = new JScrollPane(hexEditor);
			bgGroup.add(hexToggle);
			top.add(hexToggle);
			centerPanel.add(hexScroll, "hex");
		}
		
		add(top, BorderLayout.NORTH);

		setMode(MODE.HTML);


		ActionListener toggleListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if(htmlToggle.isSelected())
					setMode(MODE.HTML);
				if (jsonToggle.isSelected())
					setMode(MODE.JSON);
				if (xmlToggle.isSelected())
					setMode(MODE.XML);
				if (hexToggle.isSelected())
					setMode(MODE.HEX);
			}
		};
		
		htmlToggle.addActionListener(toggleListener);
		if(json)
			jsonToggle.addActionListener(toggleListener);
		if(xml)
			xmlToggle.addActionListener(toggleListener);
		if(hex)
			hexToggle.addActionListener(toggleListener);
		

		add(centerPanel, BorderLayout.CENTER);

		JButton validate = new JButton("Validate");
		JPanel valPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
//		valPanel.setPreferredSize(new Dimension(85, 26));
//		valPanel.setMaximumSize(new Dimension(85, 26));
		valPanel.add(validate, BorderLayout.SOUTH);
		
		validate.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					validateMessage();
				}
				catch (Exception ex) {
					ex.printStackTrace();
					UIUtils.exceptionDialog(MessagePreviewer.this, ex, "Error parsing message", "Validate message");					
					return;
				}
				JOptionPane.showMessageDialog(MessagePreviewer.this, "Message parsed successfully.", "Validate message", JOptionPane.INFORMATION_MESSAGE);
			}
		});

		add(valPanel, BorderLayout.SOUTH);
	}

	public void validateMessage() throws Exception {
		this.msg.validate();
	}

	public IMCMessage getMessage() {
		return this.msg;		
	}

	public void setMode(MODE mode) {
		this.mode = mode;
		switch (this.mode) {
		case JSON:
			((CardLayout)centerPanel.getLayout()).show(centerPanel, "json");
			break;
		case XML:
			((CardLayout)centerPanel.getLayout()).show(centerPanel, "xml");
			break;
		case HEX:
			((CardLayout)centerPanel.getLayout()).show(centerPanel, "hex");
			break;
		case HTML:
			((CardLayout)centerPanel.getLayout()).show(centerPanel, "html");
			break;
		default:
			break;	
		}
		
		htmlToggle.setSelected(mode == MODE.HTML);
		jsonToggle.setSelected(mode == MODE.JSON);
		xmlToggle.setSelected(mode == MODE.XML);
		hexToggle.setSelected(mode == MODE.HEX);

		setMessage(msg);
	}

	public void setMessage(IMCMessage msg) {
		if (msg == null) {
			htmlMsgPreview  = new MessageHtmlVisualization(new IMCMessage(0));
			jsonTextArea.setText("");
			xmlTextArea.setText("");
			hexEditor.setBytes(new byte[0]);			
		}
		else {
		    try {
		        htmlMsgPreview  = new MessageHtmlVisualization(msg);
		    }
		    catch(Exception e) {
		        e.printStackTrace();
		        htmlMsgPreview  = new MessageHtmlVisualization(new IMCMessage(0));
		    }
			try {
				jsonTextArea.setText(FormatUtils.formatJSON(msg.asJSON()));
			}
			catch(Exception e) {
				e.printStackTrace();
				jsonTextArea.setText("");
			}
		
			try {
				xmlTextArea.setText(FormatUtils.formatXML(msg.asXml(false)));
			}
			catch(Exception e) {
				e.printStackTrace();
				xmlTextArea.setText("");
			}
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				IMCOutputStream ios = new IMCOutputStream(baos);
				ios.writeMessage(msg);
				hexEditor.setBytes(baos.toByteArray());				
			}
			catch (Exception e) {
				e.printStackTrace();
				hexEditor.setBytes(new byte[0]);
			}
		}
	}
	
	static class MessageTemplate implements Comparable<MessageTemplate> {
		public final String name;
		public final IMCMessage message;
		
		public MessageTemplate(String name, IMCMessage msg) {
			this.name = name;
			this.message = msg;
		}
		
		@Override
		public int compareTo(MessageTemplate o) {
			return name.compareTo(o.name);
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	

	public static void main(String[] args) throws Exception  {
		UDPTransport.sendMessage(new Abort(), "127.0.0.1", 6002);
		JFrame frm = new JFrame("Test MessageEditor");
		PlanControl pc = new PlanControl();
		pc.setInfo("teste");
		pc.setArg(new Goto());
		MessagePreviewer editor = new MessagePreviewer(pc,true,true,true);
		frm.getContentPane().add(editor);
		frm.setSize(800, 600);
		frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frm.setVisible(true);
	}
}
