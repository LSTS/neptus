/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
import pt.lsts.neptus.types.checklist.CheckAutoUserLogItem;

public class UserCommentItem extends JPanel implements CheckSubItem {

    private static final long serialVersionUID = 4053699765892712805L;

    public static final String TYPE_ID = "userLog";

    private AutoItemsList parent = null;

    private JTextField logRequest = null;
    private JTextField logMessage = null;

    private JButton remove = null;

    private JCheckBox check = null;


    public UserCommentItem(AutoItemsList p, CheckAutoUserLogItem cauli) 	{
        this(p);
        fillFromCheckAutoUserLogItem(cauli);
    }

    public UserCommentItem(AutoItemsList p) {
        super();
        parent = p;
        initialize();
    }

    private void initialize() {
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.setOpaque(false);
        logRequest = new JTextField();
        logRequest.setColumns(20);
        logRequest.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(UserCommentItem.this);
            }
        });

        this.add(new JLabel(I18n.text("User Comment:") + " "));
        this.add(logRequest);

        logMessage = new JTextField();
        logMessage.setColumns(20);
        logMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(UserCommentItem.this);
            }
        });


        this.add(new JLabel(" " + I18n.text("Comment:") + " "));
        this.add(logMessage);

        remove = new JButton(ICON_CLOSE);
        remove.setMargin(new Insets(0,0,0,0));
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.removeUserCommentItem(UserCommentItem.this);
            }
        });

        this.add(new JLabel(" " + I18n.text("Checked:")));
        this.add(getCheck());

        this.add(remove);
    }

    private JCheckBox getCheck()
    {
        if (check == null) {
            check = new JCheckBox("check");
            check.setOpaque(false);
            check.setText(" ");
            check.addItemListener(new java.awt.event.ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    parent.fireChangeEvent(UserCommentItem.this);
                } 
            });
        }
        return	check; 
    }

    private void fillFromCheckAutoUserLogItem(CheckAutoUserLogItem cauli) {
        check.setSelected(cauli.isChecked());
        logRequest.setText(cauli.getLogRequest());
        logMessage.setText(cauli.getLogMessage());

    }

    @Override
    public CheckAutoSubItem getCheckAutoSubItem() {
        CheckAutoUserLogItem ret=new CheckAutoUserLogItem();
        ret.setChecked(getCheck().isSelected());
        ret.setLogMessage(logMessage.getText());
        ret.setLogRequest(logRequest.getText());
        return ret;
    }
}
