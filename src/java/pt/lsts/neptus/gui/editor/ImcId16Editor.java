/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.gui.editor;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.util.GuiUtils;

public class ImcId16Editor extends AbstractPropertyEditor {

	ImcId16 oldValue;
	public JPanel imcId16Editor = new JPanel();
	
	JTextField fields[] = new JTextField[2];
	
	
	
	public ImcId16Editor() {
		//imcId16Editor.setLayout(new TableLayout(new double[] {0.22, 0.04, 0.22, 0.04, 0.22, 0.04, 0.22}, new double[] {TableLayout.FILL}));
		imcId16Editor.setLayout(new TableLayout(new double[] {0.22, 0.04, 0.22}, new double[] {TableLayout.FILL}));

		for (int i = 0 ; i < 2; i++) {
			fields[i] = buildField(i);
			imcId16Editor.add(fields[i], (i*2)+",0");
		}
				
		imcId16Editor.add(createLabel(), "1,0");
		//imcId16Editor.add(createLabel(), "3,0");
		//imcId16Editor.add(createLabel(), "5,0");
		
		editor = imcId16Editor;
		
		editor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {				
				if (e.getKeyCode() == 9) {
					fields[0].requestFocus();							
					e.consume();
				}
				if (ImcId16Editor.this.imcId16Editor.getParent() != null) {
                    for (KeyListener ml : ImcId16Editor.this.imcId16Editor.getParent()
                            .getKeyListeners()) {
                        ml.keyPressed(e);
                    }
                }
			}
		});
	}

	public JTextField buildField(int n) {
		final JTextField field = new JTextField("00", 2);
		field.setBorder(BorderFactory.createEmptyBorder());
		field.setHorizontalAlignment(JTextField.CENTER);
		field.addFocusListener(new SelectAllFocusListener());

		field.addFocusListener(new FocusAdapter() {
			String oldText = "";
			@Override
			public void focusGained(FocusEvent e) {
				oldText = field.getText();
				super.focusGained(e);
			}
			
			@Override
			public void focusLost(FocusEvent e) {
				try {
					Integer i = Integer.parseInt(field.getText(), 16);
					if (i < 0 || i > 255)
						field.setText(oldText);
				}
				catch (Exception ex) {
					field.setText(oldText);
				}
				super.focusLost(e);
			}
		});
		
		
		
		field.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {							
				if (e.getKeyCode() == 9) {
					if (fields[0].isFocusOwner())
						fields[1].requestFocus();
					if (fields[1].isFocusOwner())
//						fields[2].requestFocus();
//					if (fields[2].isFocusOwner())
//						fields[3].requestFocus();
//					if (fields[3].isFocusOwner())
						fields[0].requestFocus();									
					e.consume();
				}
				if (ImcId16Editor.this.imcId16Editor.getParent() != null) {
                    for (KeyListener ml : ImcId16Editor.this.imcId16Editor.getParent()
                            .getKeyListeners()) {
                        ml.keyPressed(e);
                    }
                }
			}
		});

		return field;		
	}
	
	public JLabel createLabel() {
		JLabel lbl = new JLabel(":");
		lbl.setHorizontalAlignment(JLabel.CENTER);
		lbl.setOpaque(true);
		lbl.setBackground(Color.white);
		
		return lbl;
	}
	
	@Override
	public Object getValue() {
		
		String s = fields[0].getText();
		for (int i = 1; i < 2; i++) {
			try {
				s = s + ":"+fields[i].getText();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return new ImcId16(s);
	}
	
	@Override
	public void setValue(Object arg0) {
		oldValue = (ImcId16) arg0;
		for (int i = 0; i < 2; i++) {			
			//fields[i].setText(String.valueOf(oldValue.getByte(i)));
			String bb = Long.toHexString(oldValue.getByte(i));
	    	if (bb.length() == 1)
	    		bb = "0"+bb;
			fields[i].setText(bb);
		}
	}
	
	public static void main(String[] args) {
		ImcId16Editor test = new ImcId16Editor();
		test.imcId16Editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                NeptusLog.pub().info("<###> "+e.getKeyCode());
            }
        });
		test.setValue(new ImcId16("ed:01"));
		GuiUtils.testFrame(test.imcId16Editor, "sdsd");
		NeptusLog.pub().info("<###> "+test.getValue());
	}
	
	
}
