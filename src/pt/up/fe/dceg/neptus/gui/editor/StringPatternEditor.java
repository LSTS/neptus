/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * Feb 16, 2013
 */
package pt.up.fe.dceg.neptus.gui.editor;

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
