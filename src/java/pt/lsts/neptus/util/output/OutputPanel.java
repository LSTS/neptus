/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.util.output;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

public class OutputPanel extends JPanel implements OutputListener {

	private static final long serialVersionUID = -2153507983997000152L;

	private static DefaultStyledDocument doc = new DefaultStyledDocument();
	private static int MAX_CHARS = 500000;
	
	private MutableAttributeSet out;
	private MutableAttributeSet err;
	private MutableAttributeSet special;
	private JTextPane textPane;
	private static JFrame frame = null;
	private static OutputPanel instance = null;
	
	public static void showWindow() {
		if (frame != null) {
			frame.setVisible(true);
			frame.toFront();
		}
		else {
			frame = new JFrame("Neptus Output");
			frame.setIconImage(ImageUtils.getImage("images/neptus-icon.png"));
			frame.add(getInstance());
			frame.setSize(400, 300);
			frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			GuiUtils.centerOnScreen(frame);
			frame.setVisible(true);
			frame.toFront();			
		}
		OutputMonitor.addListener(getInstance());
	}
	
	private OutputPanel() {
		out = new SimpleAttributeSet();
		StyleConstants.setForeground(out, Color.black);
		StyleConstants.setFontFamily(out, "Helvetica");
		
		err = new SimpleAttributeSet();		
		StyleConstants.setForeground(err, Color.red);
		StyleConstants.setFontFamily(err, "Helvetica");
		
		special = new SimpleAttributeSet();		
		StyleConstants.setForeground(special, Color.green);
		StyleConstants.setItalic(special, true);
		StyleConstants.setFontFamily(special, "Helvetica");
		
		setLayout(new BorderLayout());
		textPane = new JTextPane(doc);
		textPane.setEditable(false);
		
		add(new JScrollPane(textPane), BorderLayout.CENTER);
		JButton clearBtn = new JButton("clear");
		clearBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				try {
					doc.remove(0, doc.getLength());
				}
				catch (BadLocationException locEx) {
					NeptusLog.pub().error(locEx);
				}
			};
		});
		add(clearBtn, BorderLayout.SOUTH);
	}
	
	public synchronized void addOut(String text) {		
		
		try {
			doc.insertString(doc.getLength(), text, out);
			if (doc.getLength() > MAX_CHARS)
				doc.remove(0, doc.getLength() - MAX_CHARS - 1);
			
			textPane.setCaretPosition(doc.getLength()-1);
		}
		catch (BadLocationException e) {
			NeptusLog.pub().error(e);
		}
	}

	public synchronized void addErr(String text) {
		try {
			doc.insertString(doc.getLength(), text, err);
			textPane.setCaretPosition(doc.getLength()-1);
			if (doc.getLength() > MAX_CHARS)
				doc.remove(0, doc.getLength() - MAX_CHARS - 1);
		}
		catch (BadLocationException e) {
			NeptusLog.pub().error(e);
		}
	}

	public static OutputPanel getInstance() {
		if (instance == null)
			instance = new OutputPanel();
		
		return instance;
	}
}
