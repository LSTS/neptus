/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * 04/12/2018
 */
package pt.lsts.neptus.plugins.rtplot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

public class RealTimePlotScript extends JPanel {

    private static final long serialVersionUID = 1L;
    protected static RSyntaxTextArea editorPane = new RSyntaxTextArea();
    protected JFormattedTextField numPointsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    private JMenuBar bar = new JMenuBar();
    private JMenu methods = new JMenu("Options");
    private JMenu plotprops = new JMenu("Plot Properties");
    private static JMenu math = new JMenu("Math Formulas");
    private static JMenu sysdata;
    private JButton save = new JButton();
    private static RealTimePlotGroovy plot = null;

    /**
     * This class represents the editor panel used to edit the script executed periodically in the @RealTimePlotGroovy .
     * The editor has support to highlight the Groovy programming language. The options menu in the upper bar is dynamic
     * and varies according to the selected system on the main panel.
     * 
     * @param realTimePlotGroovy
     * @param sysID ID of system(s) being used on the script
     */
    public RealTimePlotScript(RealTimePlotGroovy rtplot) {
        editorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        editorPane.setCodeFoldingEnabled(true);
        plot = rtplot;
        // create script editor
        setLayout(new BorderLayout(3, 3));
        methods.setToolTipText("Insert formulas, methods and other settings");
        methods.add(sysdata);
        methods.add(math);
        methods.add(plotprops);
        bar.add(methods);
        save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String script = editorPane.getText();
                plot.traceScript = script;
                plot.resetSeries();
                plot.runScript(script);
            }
        });
        save.setToolTipText("Save changes to script");
        add(bar, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new GridLayout(0, 3));
        add(new JScrollPane(editorPane), BorderLayout.CENTER);
        save.setText("Save");
        save.setMinimumSize(new Dimension(5, 5));
        bottom.add(save, BorderLayout.WEST);
        JLabel label = new JLabel(I18n.text("Trace points") + ":");
        label.setMinimumSize(new Dimension(5, 5));
        bottom.add(label, BorderLayout.CENTER);
        numPointsField.setMinimumSize(new Dimension(5, 5));
        bottom.add(numPointsField, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        editorPane.setText(rtplot.traceScript);
        numPointsField.setText("" + rtplot.numPoints);
        editorPane.setText(plot.traceScript);
        fillPlotOptions();
        numPointsField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                plot.numPoints = Integer.parseInt(RealTimePlotScript.this.numPointsField.getText());

            }
        });
    }

    /**
     * Adds different types of plots available on the script to the correspondent @JMenu
     */
    private void fillPlotOptions() {
        JMenuItem plotType0 = new JMenuItem("Time Series");
        plotType0.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addText("s = value(\"EstimatedState.depth\")\naddTimeSeries s", true);

            }
        });
        JMenu plotType1 = new JMenu("XY Plots");
        JMenuItem latlong = new JMenuItem("Lat/Long Plot");
        latlong.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addText("plotLatLong()", true);

            }
        });
        JMenuItem other = new JMenuItem("XY Plot");
        other.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                editorPane.setText("");
                addText("s = value(\"EstimatedState.depth\")\nt = value(\"EstimatedState.timestamp\")\nxyseries t,s",
                        true);

            }
        });
        plotType1.add(latlong);
        plotType1.add(other);
        plotprops.add(plotType0);
        plotprops.add(plotType1);
    }

    public static void editSettings(final RealTimePlotGroovy rtplot, String sysID) {
        final JDialog dialog = new JDialog(rtplot.getConsole());
        updateLocalVars(sysID);
        RealTimePlotScript scriptSettings = new RealTimePlotScript(rtplot);
        dialog.getContentPane().add(scriptSettings);
        dialog.setSize(400, 400);
        dialog.setModal(true);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!editorPane.getText().equals(plot.traceScript)) {
                    int op = GuiUtils.confirmDialog(dialog, I18n.text("Select an Option"),
                            I18n.text("Do you wish to save changes?"));
                    if (op == JOptionPane.YES_OPTION) {
                        String script = editorPane.getText();
                        plot.traceScript = script;
                        plot.numPoints = Integer.parseInt(scriptSettings.numPointsField.getText());
                        plot.resetSeries();
                        plot.runScript(script);
                        dialog.dispose();
                    }
                    else if (op == JOptionPane.NO_OPTION) {
                        dialog.dispose();
                    }
                }
                else
                    dialog.dispose();
            }
        });

        GuiUtils.centerParent(dialog, rtplot.getConsole());
        dialog.setVisible(true);
    }

    /**
     * Updates the Option @JMenu according to the selected system(s)
     */
    private static void updateLocalVars(String id) {
        sysdata = new JMenu("System data");
        JMenu deft = new JMenu("Default");
        String[] defaults = {"state","roll","pitch","yaw"};
        createDefaultOptions(deft,defaults);
        sysdata.add(deft);
        for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles())
            if (id.equalsIgnoreCase("ALL")) {
                JMenu menu = new JMenu(s.getName());
                createExtraSysOptions(s.getName(), menu);
                sysdata.add(menu);
            }
            else if (id.equalsIgnoreCase(s.getName())) {
                createExtraSysOptions(s.getName(), sysdata);
                return;
            }
        fillMathOptions();
    }

    /**
     * @param component - @JMenu to add default methods options as @JMenuItem
     */
    private static void createDefaultOptions(JMenu component,String[] options) {
        for (int i=0;i<options.length;i++) {
            String var = options[i];
            String s = i==0 ? ("addTimeSeries " + var+"(\"<field>\")"):("addTimeSeries " + var+"(),\""+var+"\"");
            JMenuItem item = new JMenuItem(var);
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    addText(s, false);
                }
            });
            component.add(item);

        }
        
    }

    /**
     * Fills the Math @JMenu with the methods from the @java.lang.Math class as @JMenuItem
     */
    private static void fillMathOptions() {
        final String[] meths = {"acos","asin","atan","atan2","cos","cosh","sin","sinh","tan","tanh","toDegrees","toRadians"};
        final List<String> trigMethods = new ArrayList<>();
        for(String s: meths)
            trigMethods.add(s);
        JMenu trig = new JMenu("Trigonometric");
        JMenu other = new JMenu("Other");
        for (Method method : Math.class.getDeclaredMethods()) {
            JMenuItem item = new JMenuItem(method.getName());
            item.setPreferredSize(new Dimension(150, (int) item.getPreferredSize().getHeight()));
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    addText("closure = {arg -> Math." + methodSignature(method) + "}"
                            + "\nseries = apply(<series>, closure)", false);

                }
            });
            
            if (trigMethods.contains(method.getName())) 
                trig.add(item);
            else
                other.add(item);
        }
        MenuScroller.setScrollerFor(other, 15, 250);
        math.add(trig);
        math.add(other);
    }

    /**
     * Returns a method signature, including the parameters type
     * 
     * @param method
     * @return signature in a String
     */
    public static String methodSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0)
            return "";
        StringBuilder paramString = new StringBuilder();
        paramString.append(parameterTypes[0].getSimpleName());
        for (int i = 1; i < parameterTypes.length; i++) {
            paramString.append(",").append(parameterTypes[i].getSimpleName());
        }
        return method.getName() + "(" + paramString.toString() + ")";
    }

    /**
     * Adds options from the available messages for each selected system
     * 
     * @param system id
     * @param component the correspondent @JMenu to attach the options
     */
    protected static void createExtraSysOptions(String system, JMenu component) {
        for (String msg : ImcMsgManager.getManager().getState(system).availableMessages()) {
            JMenuItem item = new JMenuItem(msg);
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    IMCMessage m = ImcMsgManager.getManager().getState(system).get(msg);
                    m.cloneMessageTyped();
                    String var = m.getAbbrev().toLowerCase();
                    String s = var + " = value(\"" + msg + ".<field>\")" + "\n" + "addTimeSeries " + var;
                    addText(s, false);
                }
            });
            component.add(item);

        }
    }

    /**
     * Adds code to script editor, separated by a \newline char
     * 
     * @param code to be added
     * @param deletePrevious specifies if the existing code is removed or not
     */
    protected static void addText(String code, boolean deletePrevious) {
        String current, old = editorPane.getText();
        StringBuilder sb = new StringBuilder(code.length()+old.length());
        if (!deletePrevious)
            sb.append(old+"\n");
        sb.append(code);
        current = sb.toString();
        editorPane.setText(current);
    }
}
