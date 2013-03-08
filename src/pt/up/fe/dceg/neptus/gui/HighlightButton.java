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
 * May 23, 2005
 * $Id:: HighlightButton.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

/**
 * @author zp
 */
public class HighlightButton extends JButton {
    private static final long serialVersionUID = 1L;

    private JLabel label;
	private Border downBorder = BorderFactory.createLoweredBevelBorder();
	private Border upBorder = BorderFactory.createRaisedBevelBorder();
	private boolean state = false;
	
	public HighlightButton(String txt) {
		super(txt);
		this.label = new JLabel(txt, JLabel.CENTER);
		label.setBorder(upBorder);
		
		this.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				state = true;
				label.setBorder(downBorder);
			}
			
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				state = false;
				label.setBorder(upBorder);
			}
			
			public void mouseExited(MouseEvent e) {
				super.mouseExited(e);
				label.setBorder(upBorder);
			}
			
			public void mouseEntered(MouseEvent e) {
				super.mouseEntered(e);
				if (state)
					label.setBorder(downBorder);
			}
		});
	}
	
	public void paint(Graphics g) {
		label.setBounds(this.getBounds());
		label.setBackground(getBackground());
		label.setForeground(getForeground());
		label.setOpaque(true);
		label.setText(getText());
		label.paint(g);
	}
	
	
	
	public static void main(String args[]) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			if (System.getProperty("os.name").equals("Linux")) {
				//PlasticLookAndFeel.setMyCurrentTheme(new com.jgoodies.looks.plastic.theme.SkyBlue());
				UIManager.put("ClassLoader", LookUtils.class.getClass().getClassLoader());
				UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
			}
			if (System.getProperty("os.name").startsWith("Windows")) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		}
		
		catch(Exception e){
		}

		
		JFrame test = new JFrame("Unitary test");
		test.setLayout(new FlowLayout());
		HighlightButton btn = new HighlightButton("test");
		btn.setBackground(Color.RED);
		test.getContentPane().add(btn);
		btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("ioops");
			}
		});
		btn = new HighlightButton("green");
		btn.setBackground(Color.GREEN);
		btn.setPreferredSize(new Dimension(100, 30));
		test.getContentPane().add(btn);
		
		test.pack();
		test.setVisible(true);
		test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
