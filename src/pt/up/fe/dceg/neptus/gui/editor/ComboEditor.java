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
 * $Id:: ComboEditor.java 10050 2013-03-04 12:40:45Z pdias                $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import pt.up.fe.dceg.neptus.i18n.I18n;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

public class ComboEditor<T extends Object> extends AbstractPropertyEditor {
	
	protected JComboBox<T> combo;
    private T oldValue = null;
    protected Vector<String> stringValues = new Vector<>(); 

    public ComboEditor(T[] options) {
        this(options, null);
    }

	@SuppressWarnings("serial")
    public ComboEditor(T[] options, String[] stringValues) {
	    if (stringValues != null && options.length == stringValues.length) {
	        for (String str : stringValues) {
	            this.stringValues.add(str);
            }
	    }
	    
		combo = new JComboBox<T>(options) {
            @SuppressWarnings("unchecked")
            public void setSelectedItem(Object anObject) {
                oldValue = (T) getSelectedItem();
                super.setSelectedItem(anObject);
            }
        };
		combo.setRenderer(new ListCellRenderer<T>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends T> list, T value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                String str = I18n.text(value.toString());
                if (!ComboEditor.this.stringValues.isEmpty()) {
                    for (int i = 0; i < combo.getItemCount(); i++) {
                        if (combo.getItemAt(i).equals(value)) {
                            str = ComboEditor.this.stringValues.get(i);
                            break;
                        }
                    }
                }
                JLabel label = new JLabel(str);
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
    
                return label;
            }
        });
        combo.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                ComboEditor.this.firePropertyChange(oldValue, combo.getSelectedItem());
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        combo.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ComboEditor.this.firePropertyChange(oldValue, combo.getSelectedItem());
                }
            }
        });

		editor = combo;
	}
	
	@Override
	@SuppressWarnings("unchecked")
    public T getValue() {
		return (T) combo.getSelectedItem(); 
	}
		
    @Override
	public void setValue(Object arg0) {
		combo.setSelectedItem(arg0);
	}
}
