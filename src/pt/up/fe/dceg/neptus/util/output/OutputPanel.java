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
 */
package pt.up.fe.dceg.neptus.util.output;

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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
