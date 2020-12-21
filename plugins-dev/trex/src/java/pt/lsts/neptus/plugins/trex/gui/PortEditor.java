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
 * Author: meg
 * May 10, 2013
 */
package pt.lsts.neptus.plugins.trex.gui;

import java.text.DecimalFormat;

import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.NeptusLog;

/**
 * @author meg
 *
 */
public class PortEditor extends AbstractPropertyEditor {

    protected Object lastGoodValue;
    protected DecimalFormat format = new DecimalFormat("####");

    public PortEditor() {
        NeptusLog.pub().info("<###>Created a numericeditor!");
        editor = new JTextField();
        ((JTextField) editor).setBorder(LookAndFeelTweaks.EMPTY_BORDER);
    }

    @Override
    public Object getValue() {
        String text = ((JTextField) editor).getText();
        if (text == null || text.trim().length() == 0) {
            return getDefaultValue();
        }
        return Integer.parseInt(text);
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof Number) {
            ((JTextField) editor).setText(format.format(((Number) value).intValue()));
        }
        else {
            ((JTextField) editor).setText("" + getDefaultValue());
        }
        lastGoodValue = value;
    }

    private Object getDefaultValue() {
        return 8888;
    }
}

