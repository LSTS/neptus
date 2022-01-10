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
 * May 23, 2005
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.platform.OsInfo;

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
			if (OsInfo.getName() == OsInfo.Name.LINUX) {
				UIManager.put("ClassLoader", LookUtils.class.getClass().getClassLoader());
				UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
			} else if (OsInfo.getName() == OsInfo.Name.WINDOWS) {
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
				NeptusLog.pub().info("<###>ioops");
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
