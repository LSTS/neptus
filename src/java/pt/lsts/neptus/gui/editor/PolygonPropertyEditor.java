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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 22/08/2016
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.gui.PolygonPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.PolygonType;

/**
 * @author zp
 *
 */
public class PolygonPropertyEditor extends AbstractPropertyEditor {

    protected JTextField textField = new JTextField();
    private JButton button = new FixedButton();
    private PolygonType polygon; 
    
    
    public PolygonPropertyEditor() {
        textField.setEditable(false);
        editor = new JPanel(new BorderLayout(0,0));
        ((JPanel)editor).add(textField, BorderLayout.CENTER);
        ((JPanel)editor).add(button, BorderLayout.EAST);
        
        textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
        
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PolygonType edit = polygon.clone();
                edit = PolygonPanel.showPolygonDialog(editor, I18n.text("Edit Polygon"), edit, null, true);
                if (edit != null) {
                    setValue(edit);
                    firePropertyChange(polygon, edit);
                    textField.setText(edit.toString());
                }
            }
        });
    }
    
    public Object getValue() {
        return polygon;
    }
    
    public void setValue(Object arg0) {
        if (arg0 instanceof PolygonType) {
            this.polygon = (PolygonType)arg0;
            textField.setText(""+polygon);
        }
    }
}
