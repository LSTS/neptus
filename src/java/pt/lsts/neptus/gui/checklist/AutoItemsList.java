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
 * Author: Rui Gonçalves
 * 200?/??/??
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author rjpg
 *
 */
public class AutoItemsList extends JPanel {

    private static final long serialVersionUID = -7753351429507075143L;

    private static final ImageIcon ICON_ADD = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/add.png", 16, 16));

    private JComboBox<?> optionsList = null;
    private String [] optionsListString;

    private JButton addAutoCheckItem = null;

    private CheckItemPanel parentCheckItemPanel;

    public AutoItemsList(CheckItemPanel p) {
        super();
        parentCheckItemPanel = p;
        initialize();
    }

    private void initialize() {
        this.setOpaque(false);

        addAutoCheckItem = new JButton(I18n.text("Add"), ICON_ADD);
        addAutoCheckItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int Selection;
                Selection = getOptionsList().getSelectedIndex();
                if (Selection == 2) {
                    AutoItemsList.this.add(new VariableIntervalItem(AutoItemsList.this));
                }
                else if (Selection == 0) {
                    AutoItemsList.this.add(new UserActionItem(AutoItemsList.this));
                }
                else if (Selection == 1) {
                    AutoItemsList.this.add(new UserCommentItem(AutoItemsList.this));
                }
                repaintCheck();
                fireChangeEvent(AutoItemsList.this);
            }
        });

        JPanel title=new JPanel();
        title.setLayout(new FlowLayout(FlowLayout.LEFT));
        title.setOpaque(false);
        /*title.add(new JLabel("Variable name"),null);
		title.add(new JLabel("[Start  "),null);
		title.add(new JLabel("  End]"),null);
		title.add(new JLabel(" Out of"),null);*/
        title.add(addAutoCheckItem);
        title.add(getOptionsList());

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(title);
    }

    private JComboBox<?> getOptionsList() {
        if(optionsList==null) {
            optionsListString = new String [2];
            optionsListString[0] = I18n.text("User Action");
            optionsListString[1] = I18n.text("User Comment");
            // optionsListString[2] = I18n.text("Variable Test");

            optionsList = new JComboBox<Object> (optionsListString);
        }
        return optionsList;
    }

    public void removeVarIntervalItem(VariableIntervalItem vti) {
        this.remove(vti);
        repaintCheck();
        fireChangeEvent(vti);
    }

    public void removeUserActionItem(UserActionItem uai) {
        this.remove(uai);
        repaintCheck();
        fireChangeEvent(uai);
    }

    public void removeUserCommentItem(UserCommentItem uci) {
        this.remove(uci);
        repaintCheck();
        fireChangeEvent(uci);
    }

    public void repaintCheck() {
        Component cmp = parentCheckItemPanel.getParent();
        while(cmp != null) {
            cmp.doLayout();
            cmp.invalidate();
            cmp.validate();				
            cmp = cmp.getParent();
        }
    }

    public int numberOfSubItems() {
        Component[] list = getComponents();
        int count = 0;
        for(Component c : list) {
            try {			
                @SuppressWarnings("unused")
                CheckSubItem si = (CheckSubItem) c;
                count++;
            }
            catch (Exception e2) {
                //e2.printStackTrace();
            }
        }
        return count;
    }

    void fireChangeEvent(Component source) {
        parentCheckItemPanel.fireChangeEvent(source);    
    }
}