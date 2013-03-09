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
 * Nov 16, 2012
 */
package pt.up.fe.dceg.neptus.gui.editor;

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

    private Object lastGoodValue;
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

    private Object getDefaultValue() {
       return 0d;
    }
}
