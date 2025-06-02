/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 31/May/2025
 */
package pt.lsts.neptus.gui.swing;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.ActionListener;

public class ActionLabel extends JPanel {
    private final JLabel label;
    private JButton actionButton;

    public ActionLabel(String text, Icon icon) {
        this(text, icon, JLabel.LEADING);
    }

    public ActionLabel(String text, Icon icon, int horizontalAlignment) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        label = new JLabel(text, icon, horizontalAlignment);
        add(label);
    }


    public ActionLabel(String text) {
        this(text, null);
    }

    public ActionLabel() {
        this("");
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setIcon(Icon icon) {
        label.setIcon(icon);
    }

    public void addActionButton(String buttonText, ActionListener listener) {
        if (actionButton != null) {
            remove(actionButton);
        }
        actionButton = new JButton(buttonText);
        actionButton.setOpaque(false);
        actionButton.setBackground(label.getBackground().darker().darker());
        actionButton.setFont(label.getFont().deriveFont(label.getFont().getSize() - 2f));
        actionButton.addActionListener(listener);
        add(actionButton);
        revalidate();
        repaint();
    }

    public void removeActionButton() {
        if (actionButton != null) {
            remove(actionButton);
            actionButton = null;
            revalidate();
            repaint();
        }
    }

    public JLabel getLabel() {
        return label;
    }

    public JButton getActionButton() {
        return actionButton;
    }
}
