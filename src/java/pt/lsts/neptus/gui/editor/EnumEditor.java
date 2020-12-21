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
 * Author: José Pinto
 * 2009/06/02
 */
package pt.lsts.neptus.gui.editor;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.lsts.neptus.i18n.I18n;

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
