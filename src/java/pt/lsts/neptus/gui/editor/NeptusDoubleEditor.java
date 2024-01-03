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
 * Author: José Pinto
 * Nov 16, 2012
 */
package pt.lsts.neptus.gui.editor;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.JTextField;
import javax.swing.UIManager;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

/**
 * @author zp
 * 
 */
public class NeptusDoubleEditor extends AbstractPropertyEditor {

    protected Object lastGoodValue;
    protected DecimalFormat format = new DecimalFormat("0.0########");
    {
        format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        format.setGroupingUsed(false);
    }
    
    public NeptusDoubleEditor() {
        editor = new JTextField();
        ((JTextField)editor).setBorder(LookAndFeelTweaks.EMPTY_BORDER);
    }

    public Object getValue() {
        String text = ((JTextField) editor).getText();
        if (text == null || text.trim().length() == 0) {
            return getDefaultValue();
        }

        // collect all numbers from this textfield
        StringBuffer number = new StringBuffer();
        number.ensureCapacity(text.length());
        for (int i = 0, c = text.length(); i < c; i++) {
            char character = text.charAt(i);
            if ('.' == character || '-' == character || 'E' == character || Character.isDigit(character)) {
                number.append(character);
            }
            else if (' ' == character) {
                continue;
            }
            else {
                break;
            }
        }

        try {
            lastGoodValue = Double.parseDouble(number.toString());
        }
        catch (Exception e) {
            UIManager.getLookAndFeel().provideErrorFeedback(editor);
        }

        return lastGoodValue;
    }

    public void setValue(Object value) {
        if (value instanceof Number) {
            ((JTextField) editor).setText(format.format(((Number)value).doubleValue()));
        }
        else {
            ((JTextField) editor).setText("" + getDefaultValue());
        }
        lastGoodValue = value;
    }

    protected Object getDefaultValue() {
       return 0d;
    }
}
