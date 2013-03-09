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
package pt.up.fe.dceg.neptus.gui.editor;

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

import pt.up.fe.dceg.neptus.gui.SelectAllFocusListener;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

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
                System.out.println(e.getKeyCode());
            }
        });
		test.setValue(new ImcId16("ed:01"));
		GuiUtils.testFrame(test.imcId16Editor, "sdsd");
		System.out.println(test.getValue());
	}
	
	
}
