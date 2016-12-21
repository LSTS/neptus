/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 \* Alternatively, this file may be used under the terms of the Modified EUPL,
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.l2fprod.common.beans.editor.NumberPropertyEditor;
import com.l2fprod.common.util.converter.ConverterRegistry;

/**
 * @author pdias
 *
 */
public class NumberEditor<T extends Number> extends NumberPropertyEditor implements ValidationEnableInterface {
    
    protected final Class<T> classType;
    
    protected boolean enableValidation = true;
    
    protected T minValue = null;
    protected T maxValue = null;
    
    private Pattern pattern;
    protected String elementPattern;
    private Color errorColor = new Color(255, 108, 108);
    // private Color syncColor = new Color(108, 255, 108);
    // private Color blueColor = new Color(108, 108, 255);
    private Border defaultBorder = null;
    private Border errorBorder = null;

    private Timer timer = null;
    private TimerTask validatorTask = null;
    
    @SuppressWarnings("unchecked")
    public NumberEditor(Class<T> type, T minValue, T maxValue) {
        this(type);
        
        if (type == Double.class) {
            if (minValue == null)
                minValue = (T) new Double(Double.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Double(Double.MAX_VALUE);
            if (minValue.doubleValue() > maxValue.doubleValue())
                minValue = maxValue;
        }
        else if (type == Float.class) {
            if (minValue == null)
                minValue = (T) new Float(Float.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Float(Float.MAX_VALUE);
            if (minValue.floatValue() > maxValue.floatValue())
                minValue = maxValue;
        }
        else if (type == Long.class) {
            if (minValue == null)
                minValue = (T) new Long(Long.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Long(Long.MAX_VALUE);
            if (minValue.longValue() > maxValue.longValue())
                minValue = maxValue;
        }
        else if (type == Integer.class) {
            if (minValue == null)
                minValue = (T) new Integer(Integer.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Integer(Integer.MAX_VALUE);
            if (minValue.intValue() > maxValue.intValue())
                minValue = maxValue;
        }
        else if (type == Short.class) {
            if (minValue == null)
                minValue = (T) new Short(Short.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Short(Short.MAX_VALUE);
            if (minValue.shortValue() > maxValue.shortValue())
                minValue = maxValue;
        }
        else if (type == Byte.class) {
            if (minValue == null)
                minValue = (T) new Byte(Byte.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Byte(Byte.MAX_VALUE);
            if (minValue.byteValue() > maxValue.byteValue())
                minValue = maxValue;
        }
        else {
            minValue = maxValue = null;
        }

        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    
    public NumberEditor(Class<T> type) {
        super(type);
        this.classType = type;
        
        if (this.classType == Double.class || this.classType == Float.class) {
            elementPattern = ArrayListEditor.REAL_PATTERN;
        }
        else if (this.classType == Byte.class) {
            elementPattern = "[01]+";
        } 
        else {
            elementPattern = ArrayListEditor.INTEGER_PATTERN;
        }
        
        ((JTextField) editor).addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
//                char keyChar = e.getKeyChar();
//                if (Character.isAlphabetic(keyChar))
//                    e.consume();
//                if ((classType == Double.class || classType == Float.class) && !(Character.isDigit(keyChar) || keyChar == '.' || keyChar == 'e' || keyChar == 'E' || keyChar == '+' || keyChar == '-'))
//                    e.consume();
//                else if ((classType == Long.class || classType == Integer.class || classType == Short.class) && !Character.isDigit(keyChar))
//                        e.consume();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // System.out.println("keyPressed " + e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB)
                    validateValue();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // System.out.println("keyReleased " + e);
                // validateValue();
                revokeScheduleValidatorTask();
                scheduleValidatorTask();
            }

            private void validateValue() {
                boolean checkOk = true;
                String txt = ((JTextField) editor).getText().trim();
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
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }
                else {
                    ((JTextField) editor).setBorder(defaultBorder);
                }
            }
        });
        
        ((JTextField) editor).addFocusListener(new FocusAdapter() {
            private T oldVal = null;
            public void focusGained(FocusEvent fe) {
                try {
                    oldVal = convertFromString(((JTextField) editor).getText());
                }
                catch (Exception e) {
                    oldVal = null;
                }
            }

            public void focusLost(FocusEvent fe) {
                // System.out.println("focusLost " + fe);
                try {
                    T newVal = (T) convertFromString(((JTextField) editor).getText());
                    firePropertyChange(oldVal, newVal);
                }
                catch (Exception e) {                   
                    ((JTextField) editor).setText(convertToString(oldVal));
                }
            }
        });
    }
    
//    @Override
//    public void setValue(Object value) {
//        System.out.println("setValue Editor " + value);
//        super.setValue(value);
//    }
    
    @Override
    public Object getValue() {
//        System.out.println("getValue Editor " + super.getValue());
        return convertFromString(convertToString(super.getValue()));
    }
    
    private void scheduleValidatorTask() {
        if (validatorTask == null) {
            if (timer == null)
                timer = new Timer(NumberEditor.this.getClass().getSimpleName() + " keyboard entry value validator for "
                        + classType.getSimpleName() + " [" + minValue + ", " + maxValue + "]", true);
            validatorTask = createValidatorTimerTask();
            timer.schedule(validatorTask, 700);
        }
    }

    private void revokeScheduleValidatorTask() {
        if (validatorTask != null) {
            validatorTask.cancel();
            try {
                if (validatorTask != null)
                    validatorTask.wait();
            }
            catch (Exception e) {
                // Don't need to catch it
            }
            validatorTask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private TimerTask createValidatorTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
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
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }
                else {
                    ((JTextField) editor).setBorder(defaultBorder);
                }

                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    private T convertFromString(String txt) throws NumberFormatException {
        if (pattern == null) {
            pattern = Pattern.compile(elementPattern, Pattern.CASE_INSENSITIVE);
        }
        Matcher m = pattern.matcher(txt);
        boolean checkOk = m.matches();
        if (!checkOk)
            throw new NumberFormatException();
        
        T valueToReq = (T) ConverterRegistry.instance().convert(this.classType, txt);
        if (enableValidation && minValue != null && maxValue != null) {
            if (valueToReq.doubleValue() < minValue.doubleValue()) {
                ((JTextField) editor).setText(convertToString(minValue));
                valueToReq = minValue;
            }
            if (valueToReq.doubleValue() > maxValue.doubleValue()) {
                ((JTextField) editor).setText(convertToString(maxValue));
                valueToReq = maxValue;
            }
        }
        
        return valueToReq;
    }

    protected String convertToString(Object value) {
        if (value instanceof Double || value instanceof Float)
            return "" + value;
        else if (value instanceof Long || value instanceof Integer)
            return "" + value;

        return ((String) ConverterRegistry.instance().convert(String.class, value)); //.replace(",", ".");
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.editor.ValidationEnableInterface#isEnableValidation()
     */
    @Override
    public boolean isEnableValidation() {
        return enableValidation;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.editor.ValidationEnableInterface#setEnableValidation(boolean)
     */
    @Override
    public void setEnableValidation(boolean enableValidation) {
        this.enableValidation = enableValidation;
    }
}
