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
 * Author: Paulo Dias
 * Feb 16, 2013
 */
package pt.lsts.neptus.gui.editor;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.border.Border;

import com.l2fprod.common.beans.editor.StringConverterPropertyEditor;

/**
 * @author pdias
 *
 */
public class StringPatternEditor extends StringConverterPropertyEditor {

    private Pattern pattern;
    protected String elementPattern;
    private Color errorColor = new Color(255, 108, 108);
    // private Color syncColor = new Color(108, 255, 108);
    // private Color blueColor = new Color(108, 108, 255);
    private Border defaultBorder = null;
    private Border errorBorder = null;

    public StringPatternEditor(String regex) throws IllegalArgumentException, PatternSyntaxException {
        elementPattern = regex;
        this.pattern = Pattern.compile(elementPattern, Pattern.CASE_INSENSITIVE);
        init();
    }
    
    protected void init() {
        ((JTextField) editor).addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                boolean checkOk = true;
                String txt = ((JTextField) editor).getText();
                try {
                    convertFromString(txt);
                }
                catch (Exception e1) {
                    checkOk = false;
                }
                if (!checkOk) {
                    if (errorBorder == null) {
                        errorBorder = BorderFactory.createLineBorder(errorColor, 2);
                        defaultBorder = ((JTextField) editor).getBorder();
                    }
                    
                    ((JTextField) editor).setBorder(errorBorder);
                }
                else {
                    ((JTextField) editor).setBorder(defaultBorder);
                }
            }
        });
        
        ((JTextField) editor).addFocusListener(new FocusAdapter() {
            private String oldVal = null;
            public void focusGained(FocusEvent fe) {
                try {
                    oldVal = (String) convertFromString(((JTextField) editor).getText());
                }
                catch (Exception e) {
                    oldVal = null;
                }
            }

            public void focusLost(FocusEvent fe) {
                try {
                    String newVal = (String) convertFromString(((JTextField) editor).getText());
                    firePropertyChange(oldVal, newVal);
                }
                catch (Exception e) {                   
                    ((JTextField) editor).setText(convertToString(oldVal));
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see com.l2fprod.common.beans.editor.StringConverterPropertyEditor#convertFromString(java.lang.String)
     */
    @Override
    protected Object convertFromString(String value) {
        if (pattern == null) {
            pattern = Pattern.compile(elementPattern, Pattern.CASE_INSENSITIVE);
        }
        Matcher m = pattern.matcher(((JTextField) editor).getText());
        boolean checkOk = m.matches();
        if (!checkOk)
            throw new NumberFormatException();

        return checkOk ? value : null;
    }
    
    /* (non-Javadoc)
     * @see com.l2fprod.common.beans.editor.StringConverterPropertyEditor#convertToString(java.lang.Object)
     */
    @Override
    protected String convertToString(Object value) {
        return value.toString();
    }
}
