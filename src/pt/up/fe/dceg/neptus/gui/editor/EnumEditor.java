/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/06/02
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import pt.up.fe.dceg.neptus.i18n.I18n;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

/**
 * @author zp
 *
 */
@SuppressWarnings("all")
public class EnumEditor extends AbstractPropertyEditor {

    private Class<? extends Enum> clazz;
	private JComboBox<String> combo;
    private String oldValue = null;
	
	public EnumEditor(Class<? extends Enum> clazz) {
		
		this.clazz = clazz;
		
		Enum<?>[] enums = clazz.getEnumConstants();
		
		String[] options = new String[enums.length];
		
		for (int i = 0; i < options.length; i++) {
			if (enums[i].name().startsWith("_"))
				options[i] = enums[i].name().substring(1);
			else
				options[i] = enums[i].name();			
		}
		
        combo = new JComboBox<String>(options) {
            @SuppressWarnings("unchecked")
            public void setSelectedItem(Object anObject) {
                oldValue = (String) getSelectedItem();
                super.setSelectedItem(anObject);
            }
        };
		combo.setRenderer(new ComboRenderer());

        combo.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                EnumEditor.this.firePropertyChange(oldValue, combo.getSelectedItem());
            }

            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        combo.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    EnumEditor.this.firePropertyChange(oldValue, combo.getSelectedItem());
                }
            }
        });

		editor = combo;
	}
	
	@Override
	public Object getValue() {
		try {
			return Enum.valueOf(clazz, combo.getSelectedItem().toString());
		}
		catch (Exception e) {
			return Enum.valueOf(clazz, "_"+combo.getSelectedItem());
		}
	}
	
	/* (non-Javadoc)
	 * @see com.l2fprod.common.beans.editor.AbstractPropertyEditor#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		combo.setSelectedItem(((Enum)value).name());
	}
	
    private class ComboRenderer extends JLabel implements ListCellRenderer<String> {
        public ComboRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(CENTER);
//            setPreferredSize(new Dimension(270, 25));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value != null)
                setText(I18n.text(value));
            
            return this;
        }
    }
}
