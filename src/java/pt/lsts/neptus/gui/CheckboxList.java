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
 * Jan 15, 2016
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import pt.lsts.neptus.i18n.I18n;

/**
 * @author zp
 *
 */
public class CheckboxList extends JList<JCheckBox> {
    private static final long serialVersionUID = 7637534594039239173L;
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public CheckboxList() {
        setCellRenderer(new CellRenderer());

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                int index = locationToIndex(e.getPoint());

                if (index != -1) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        JPopupMenu popup = new JPopupMenu();
                        popup.add(I18n.text("Select all")).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                for (int i = 0; i < getModel().getSize(); i++)
                                    getModel().getElementAt(i).setSelected(true);
                                repaint();
                            }
                        });

                        popup.add(I18n.text("Select none")).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                for (int i = 0; i < getModel().getSize(); i++)
                                    getModel().getElementAt(i).setSelected(false);
                                repaint();
                            }
                        });

                        popup.show(CheckboxList.this, getX(), e.getY());
                    }
                    else {
                        JCheckBox checkbox = getModel().getElementAt(index);
                        checkbox.setSelected(!checkbox.isSelected());
                        repaint();
                    }
                }
            }
        });

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    protected class CellRenderer implements ListCellRenderer<JCheckBox> {
        public Component getListCellRendererComponent(JList<? extends JCheckBox> list, JCheckBox checkbox, int index,
                boolean isSelected, boolean cellHasFocus) {
            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
            return checkbox;
        }
    }

    public static CheckboxList getInstance(String... options) {
        JCheckBox[] items = new JCheckBox[options.length];

        for (int i = 0; i < options.length; i++)
            items[i] = new JCheckBox(options[i]);

        CheckboxList list = new CheckboxList();
        list.setListData(items);

        return list;
    }

    public String[] getSelectedStrings() {
        ArrayList<String> selected = new ArrayList<>();
        for (int i = 0; i < getModel().getSize(); i++) {
            if (getModel().getElementAt(i).isSelected())
                selected.add(getModel().getElementAt(i).getText());
        }

        return selected.toArray(new String[selected.size()]);
    }

    public static String[] selectOptions(Component parent, String title, String... options) {
        CheckboxList checkList = CheckboxList.getInstance(options);

        int op = JOptionPane.showConfirmDialog(parent, new JScrollPane(checkList), title, JOptionPane.OK_CANCEL_OPTION);
        if (op != JOptionPane.OK_OPTION)
            return null;
        return checkList.getSelectedStrings();
    }

    // example usage
    public static void main(String[] args) {
        String[] options = selectOptions(null, "Title", "Option A", "Option B", "Option C");
        if (options == null)
            System.out.println("User cancelled the dialog");
        else
            System.out.println(Arrays.asList(options));
    }
}
