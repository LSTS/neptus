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
 * Author: Paulo Dias
 * 12 de Fev de 2013
 */
package pt.lsts.neptus.gui.editor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.l2fprod.common.beans.editor.StringConverterPropertyEditor;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.util.converter.ConverterRegistry;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * ArrayList that is shown as comma separated values
 * 
 * @author pdias
 * 
 */
public class ArrayListEditor<E extends ArrayList<T>, T extends Object> extends StringConverterPropertyEditor {

    public static final String REAL_PATTERN = "([+-])?\\d+(\\.\\d*)?(E([+-])?\\d{1,2})?";
    public static final String INTEGER_PATTERN = "([+-])?\\d+";
    public static final String STRING_LIST_PATTERN = "([^,])+";
    
    // xxx.xxx.xxx.xxx (0-255)
    public static final String IP_ADDRESS_PATTERN = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
    // 1 through 65535
    public static final String IP_PORT_RANGE_PATTERN = "(?:6553[0-5]|655[0-2][0-9]|65[0-4][0-9][0-9]|6[0-4][0-9][0-9][0-9]|((([0-5]?[0-9])?[0-9])?[0-9][0-9])|[1-9])";
    public static final String IP_ADDRESSWITH_PORT_PATTERN = "(" + IP_ADDRESS_PATTERN + "(:" + IP_PORT_RANGE_PATTERN + ")?)";
    
    public static final int UNLIMITED_SIZE = -1; 

    protected ArrayList<T> arrayList = new ArrayList<>();
    protected final Class<T> classType;
    protected int minSize = 0;
    protected int maxSize = UNLIMITED_SIZE;
    
    protected T minValue = null;
    protected T maxValue = null;
    

    private Pattern pattern;
    protected String elementPattern;
    private Color errorColor = new Color(255, 108, 108);
    // private Color syncColor = new Color(108, 255, 108);
    // private Color blueColor = new Color(108, 108, 255);
    private Border defaultBorder = null;
    private Border errorBorder = null;

    private ArrayListEditor(Class<T> classType, int size) {
        this(classType, size, size);
    }

    @SuppressWarnings("unchecked")
    private ArrayListEditor(Class<T> classType, int minSize, int maxSize, T minValue, T maxValue) {
        this(classType, minSize, maxSize);
        
        if (classType == Double.class) {
            if (minValue == null)
                minValue = (T) new Double(Double.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Double(Double.MAX_VALUE);
            if (((Double) minValue).doubleValue() > ((Double) maxValue).doubleValue())
                minValue = maxValue;
        }
        else if (classType == Float.class) {
            if (minValue == null)
                minValue = (T) new Float(Float.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Float(Float.MAX_VALUE);
            if (((Float) minValue).floatValue() > ((Float) maxValue).floatValue())
                minValue = maxValue;
        }
        else if (classType == Long.class) {
            if (minValue == null)
                minValue = (T) new Long(Long.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Long(Long.MAX_VALUE);
            if (((Long) minValue).longValue() > ((Long) maxValue).longValue())
                minValue = maxValue;
        }
        else if (classType == Integer.class) {
            if (minValue == null)
                minValue = (T) new Integer(Integer.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Integer(Integer.MAX_VALUE);
            if (((Integer) minValue).intValue() > ((Integer) maxValue).intValue())
                minValue = maxValue;
        }
        else if (classType == Short.class) {
            if (minValue == null)
                minValue = (T) new Short(Short.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Short(Short.MAX_VALUE);
            if (((Short) minValue).shortValue() > ((Short) maxValue).shortValue())
                minValue = maxValue;
        }
        else if (classType == Byte.class) {
            if (minValue == null)
                minValue = (T) new Byte(Byte.MIN_VALUE);
            if (maxValue == null)
                maxValue = (T) new Byte(Byte.MAX_VALUE);
            if (((Byte) minValue).byteValue() > ((Byte) maxValue).byteValue())
                minValue = maxValue;
        }
        else {
            minValue = maxValue = null;
        }

        this.minValue = minValue;
        this.maxValue = maxValue;

    }
    
    private ArrayListEditor(Class<T> classType, int minSize, int maxSize) {
        super();
        this.classType = classType;
        if (classType == Double.class || classType == Float.class) {
            elementPattern = REAL_PATTERN;
        }
        else if (classType == Long.class || classType == Integer.class || classType == Short.class) {
            elementPattern = INTEGER_PATTERN;
        }
        else if (classType == String.class) {
            elementPattern = STRING_LIST_PATTERN;
        }
        
        if (minSize < 0)
            minSize = 0;
        if (maxSize <= 0)
            maxSize = UNLIMITED_SIZE;
        if (minSize > maxSize)
            minSize = 0;

        this.minSize = minSize;
        this.maxSize = maxSize;
        
        init();
    }
    
    public static ArrayListEditor<ArrayList<Double>, Double> forgeDouble(int size) {
        return new ArrayListEditor<ArrayList<Double>, Double>(Double.class, size);
    }

    public static ArrayListEditor<ArrayList<Double>, Double> forgeDouble(int minSize, int maxSize) {
        return new ArrayListEditor<ArrayList<Double>, Double>(Double.class, minSize, maxSize);
    }

    public static ArrayListEditor<ArrayList<Double>, Double> forgeDouble(int minSize, int maxSize, double minValue, double maxValue) {
        return new ArrayListEditor<ArrayList<Double>, Double>(Double.class, minSize, maxSize, minValue, maxValue);
    }

    public static ArrayListEditor<ArrayList<Float>, Float> forgeFloat(int size) {
        return new ArrayListEditor<ArrayList<Float>, Float>(Float.class, size);
    }

    public static ArrayListEditor<ArrayList<Float>, Float> forgeFloat(int minSize, int maxSize) {
        return new ArrayListEditor<ArrayList<Float>, Float>(Float.class, minSize, maxSize);
    }

    public static ArrayListEditor<ArrayList<Integer>, Integer> forgeInteger(int size) {
        return new ArrayListEditor<ArrayList<Integer>, Integer>(Integer.class, size);
    }

    public static ArrayListEditor<ArrayList<Integer>, Integer> forgeInteger(int minSize, int maxSize) {
        return new ArrayListEditor<ArrayList<Integer>, Integer>(Integer.class, minSize, maxSize);
    }

    public static ArrayListEditor<ArrayList<Long>, Long> forgeLong(int size) {
        return new ArrayListEditor<ArrayList<Long>, Long>(Long.class, size);
    }

    public static ArrayListEditor<ArrayList<Long>, Long> forgeLong(int minSize, int maxSize) {
        return new ArrayListEditor<ArrayList<Long>, Long>(Long.class, minSize, maxSize);
    }

    public static ArrayListEditor<ArrayList<Long>, Long> forgeLong(int minSize, int maxSize, long minValue, long maxValue) {
        return new ArrayListEditor<ArrayList<Long>, Long>(Long.class, minSize, maxSize, minValue, maxValue);
    }

    public static ArrayListEditor<ArrayList<Short>, Short> forgeShort(int size) {
        return new ArrayListEditor<ArrayList<Short>, Short>(Short.class, size);
    }

    public static ArrayListEditor<ArrayList<Short>, Short> forgeShort(int minSize, int maxSize) {
        return new ArrayListEditor<ArrayList<Short>, Short>(Short.class, minSize, maxSize);
    }

    public static ArrayListEditor<ArrayList<String>, String> forgeString(int size) {
        return new ArrayListEditor<ArrayList<String>, String>(String.class, size);
    }

    public static ArrayListEditor<ArrayList<String>, String> forgeString(int minSize, int maxSize) {
        return new ArrayListEditor<ArrayList<String>, String>(String.class, minSize, maxSize);
    }

    public static ArrayListEditor<ArrayList<String>, String> forgeString(int size, final String regexp) {
        return forgeString(size, size, regexp);
    }

    public static ArrayListEditor<ArrayList<String>, String> forgeString(int minSize, int maxSize, final String regexp) {
        return new ArrayListEditor<ArrayList<String>, String>(String.class, minSize, maxSize) {
            {
                elementPattern = regexp == null || regexp.length() == 0 ? STRING_LIST_PATTERN : regexp;
            }
        };
    }


    protected void init() {
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
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }
                else {
                    ((JTextField) editor).setBorder(defaultBorder);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public ArrayList<T> getValue() {
        arrayList = (ArrayList<T>) super.getValue();
        return arrayList;
    }

    protected T convertElement(String text) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.l2fprod.common.beans.editor.StringConverterPropertyEditor#convertFromString(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object convertFromString(String text) {
        if (pattern == null) {
            String pat = "";
            if (maxSize == 1) {
                pat = "(" + elementPattern + ")"  + (minSize == 0 ? "{0,1}": "");
            }
            else if (maxSize > 1 && minSize == maxSize) {
                pat = "(" + elementPattern + ")" + "(\\ *,\\ *(" + elementPattern + ")\\ *){" +  (maxSize - 1) + "}";
            }
            else if (maxSize > 1 && minSize != maxSize) {
                pat = "(" + elementPattern + ")" + "(\\ *,\\ *(" + elementPattern + ")\\ *){" + minSize + "," +  (maxSize - 1) + "}";
            }
            else {
                pat = "(" + elementPattern + ")" + "(\\ *,\\ *" + elementPattern + "\\ *)*";
            }
            
            pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
        }
        Matcher m = pattern.matcher(((JTextField) editor).getText());
        boolean checkOk = m.matches();
        if (!checkOk)
            throw new NumberFormatException();
        
        ArrayList<T> valuesList = new ArrayList<>();
        String[] tokens = text.split(",");
        boolean changeContent = false;
        for (String tk : tokens) {
            tk = tk.trim();
            
            T tokenObjVal = convertElement(tk);
            if (tokenObjVal == null) {
                if (classType == String.class)
                    tokenObjVal = (T) tk;
                else if (classType == Double.class)
                    tokenObjVal = (T) ConverterRegistry.instance().convert(Double.class, tk);
                else if (classType == Float.class)
                    tokenObjVal = (T) ConverterRegistry.instance().convert(Float.class, tk);
                else if (classType == Long.class)
                    tokenObjVal = (T) ConverterRegistry.instance().convert(Long.class, tk);
                else if (classType == Integer.class)
                    tokenObjVal = (T) ConverterRegistry.instance().convert(Integer.class, tk);
                else if (classType == Short.class)
                    tokenObjVal = (T) ConverterRegistry.instance().convert(Short.class, tk);
            }
            
            if (tokenObjVal != null && Number.class.isAssignableFrom(classType)) {
                T valueToReq = tokenObjVal;
                if (minValue != null && maxValue != null) {
                    if (((Number) valueToReq).doubleValue() < ((Number) minValue).doubleValue()) {
                        valueToReq = minValue;
                        changeContent = true;
                    }
                    if (((Number) valueToReq).doubleValue() > ((Number) maxValue).doubleValue()) {
                        valueToReq = maxValue;
                        changeContent = true;
                    }
                }
            }

            if (tokenObjVal != null) {
                valuesList.add(tokenObjVal);
            }
        }
        if (maxSize > UNLIMITED_SIZE && valuesList.size() > maxSize)
            throw new NumberFormatException();
        if (valuesList.size() < minSize)
            throw new NumberFormatException();

        if (changeContent)
            ((JTextField) editor).setText(convertToString(valuesList));
        
        return valuesList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.l2fprod.common.beans.editor.StringConverterPropertyEditor#convertToString(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected String convertToString(Object value) {
        return Arrays.toString(((ArrayList<Object>) value).toArray(new Object[0])).replace("[", "").replace("]", "");
    }

    /**
     * @return the minSize
     */
    public int getMinSize() {
        return minSize;
    }
    
    /**
     * @return the maxSize
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * @param args
     */
    @SuppressWarnings("serial")
    public static void main(String[] args) {
        JPanel panel = new JPanel();

        panel.setLayout(new MigLayout());

        JLabel titleLabel = new JLabel("<html><b>Test</b></html>");
        panel.add(titleLabel, "w 100%, wrap");

        // Configure Property sheet
        final PropertySheetPanel psp = new PropertySheetPanel();
        psp.setSortingCategories(true);
        psp.setSortingProperties(false);
        psp.setDescriptionVisible(true);
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        psp.setToolBarVisible(false);

        final PropertyEditorRegistry per = new PropertyEditorRegistry();
        // per.registerDefaults();
        psp.setEditorFactory(per);

        panel.add(psp, "w 100%, h 100%, wrap");

        final Random rnd = new Random(System.currentTimeMillis());
        JButton refreshButton = new JButton(new AbstractAction(I18n.text("add")) {
            int c = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                c++;
                {
                    SystemProperty property = new SystemProperty();
                    property.setName("prop D " + c);
                    property.setDisplayName(property.getName());
                    ArrayList<Double> value = new ArrayList<Double>();
                    for (int i = 0; i < c; i++) {
                        double v = MathMiscUtils.round(rnd.nextDouble(), 1);
                        value.add(v);
                    }
                    property.setValue(value);
                    psp.addProperty(property);
                    per.registerEditor(property, ArrayListEditor.forgeDouble(c));
                    psp.revalidate();
                }

                {
                    SystemProperty property = new SystemProperty();
                    property.setName("prop L " + c);
                    property.setDisplayName(property.getName());
                    ArrayList<Long> value = new ArrayList<Long>();
                    for (int i = 0; i < c; i++) {
                        long v = rnd.nextLong();
                        value.add(v);
                    }
                    property.setValue(value);
                    psp.addProperty(property);
                    per.registerEditor(property, ArrayListEditor.forgeLong(c));
                    psp.revalidate();
                }

                {
                    SystemProperty property = new SystemProperty();
                    property.setName("prop Short " + c);
                    property.setDisplayName(property.getName());
                    ArrayList<Short> value = new ArrayList<Short>();
                    for (int i = 0; i < c; i++) {
                        short v = (short) rnd.nextInt();
                        value.add(v);
                    }
                    property.setValue(value);
                    psp.addProperty(property);
                    per.registerEditor(property, ArrayListEditor.forgeShort(c));
                    psp.revalidate();
                }

                {
                    SystemProperty property = new SystemProperty();
                    property.setName("prop String " + c);
                    property.setDisplayName(property.getName());
                    ArrayList<String> value = new ArrayList<String>();
                    for (int i = 0; i < c; i++) {
                        char v = (char) rnd.nextInt();
                        value.add("" + v);
                    }
                    property.setValue(value);
                    psp.addProperty(property);
                    per.registerEditor(property, ArrayListEditor.forgeString(c > 2 ? ArrayListEditor.UNLIMITED_SIZE : c));
                    psp.revalidate();
                }

                {
                    SystemProperty property = new SystemProperty();
                    property.setName("prop String regexp " + c);
                    property.setDisplayName(property.getName());
                    ArrayList<String> value = new ArrayList<String>();
                    for (int i = 0; i < c; i++) {
                        String ip = "";
                        for (int j = 0; j < 4; j++) {
                            int v = (int) (Math.abs(rnd.nextDouble()) * 255);
                            ip += (ip.length() > 0 ? "." : "") + v;
                        }
                        value.add(ip);
                    }
                    property.setValue(value);
                    psp.addProperty(property);
                    String ipAddRegexp = IP_ADDRESS_PATTERN;
                    String portAddRexexp = "(:" + IP_PORT_RANGE_PATTERN + ")?";
                    per.registerEditor(property, ArrayListEditor.forgeString(c > 2 ? ArrayListEditor.UNLIMITED_SIZE : c, ipAddRegexp + portAddRexexp));
                    psp.revalidate();
                }
            }
        });
        panel.add(refreshButton, "split");
        JButton dumpButton = new JButton(new AbstractAction(I18n.text("dump")) {
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder sb = new StringBuilder();
                sb.append("\n\nProperties {\n");
                for (Property prop : psp.getProperties()) {
                    sb.append("\t\"" + prop.getName() + "\": \"");
                    sb.append(Arrays.toString(((ArrayList<Object>) prop.getValue()).toArray(new Object[0])));
                    sb.append("\",\n");
                }
                sb.append("}");
                NeptusLog.pub().info("<###> "+sb.toString());
            }
        });
        panel.add(dumpButton);

        GuiUtils.testFrame(panel, "Test", 600, 600);
    }
}
