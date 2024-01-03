/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Rui Gonçalves
 * 200?/??/??
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.checklist.CheckAutoSubItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserActionItem;

@SuppressWarnings("serial")
public class UserActionItem extends JPanel implements CheckSubItem{

    public static final String TYPE_ID = "userAction";

    private AutoItemsList parent = null;

    private JTextField userMsgActionText = null;
    private JButton remove = null;
    private JCheckBox check = null;

    public UserActionItem(AutoItemsList p, CheckAutoUserActionItem cauai) {
        this(p);
        fillFromCheckAutoUserActionItem(cauai);
    }

    public UserActionItem(AutoItemsList p) {
        super();
        parent = p;
        initialize();
    }

    private void initialize() {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setOpaque(false);
        userMsgActionText = new JTextField();
        userMsgActionText.setColumns(20);
        userMsgActionText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(UserActionItem.this);
            }
        });

        this.add(new JLabel(I18n.text("User Action:")));
        this.add(userMsgActionText);

        remove = new JButton(ICON_CLOSE);
        remove.setMargin(new Insets(0,0,0,0));
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.removeUserActionItem(UserActionItem.this);
            }
        });

        this.add(new JLabel(" " + I18n.text("Checked:")));
        this.add(getCheck());

        this.add(remove);
    }

    private JCheckBox getCheck() {
        if (check == null) {
            check = new JCheckBox("check");
            check.setOpaque(false);
            check.setText(" ");
            check.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    parent.fireChangeEvent(UserActionItem.this);
                } 
            });
        }
        return	check; 
    }

    private void fillFromCheckAutoUserActionItem(CheckAutoUserActionItem cauai) {
        check.setSelected(cauai.isChecked());
        userMsgActionText.setText(cauai.getAction());
    }

    @Override
    public CheckAutoSubItem getCheckAutoSubItem() {
        CheckAutoUserActionItem ret = new CheckAutoUserActionItem();
        ret.setAction(userMsgActionText.getText());
        ret.setChecked(getCheck().isSelected());
        return ret;
    }
}
