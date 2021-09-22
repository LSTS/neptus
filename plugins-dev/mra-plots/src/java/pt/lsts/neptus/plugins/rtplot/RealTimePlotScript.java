/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

public class RealTimePlotScript extends JPanel {

    private static final long serialVersionUID = 1L;
    protected RSyntaxTextArea editorPane = new RSyntaxTextArea();
    protected JFormattedTextField numPointsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    protected JFormattedTextField periodicityField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    private JMenuBar bar = new JMenuBar();
    private JMenu plotprops = new JMenu("Plot Properties");
    private JMenu methods = new JMenu("Options");
    private JMenu math = new JMenu("Math Formulas");
    private JMenu sysdata = new JMenu("System data");
    private JMenu storedScripts = new JMenu("Stored Scripts");
    private JButton save = new JButton("Save");
    private JButton store = new JButton("Store");
    private final String path = "conf/mraplots/realtime/";
    private final JDialog dialog;
    private RealTimePlotGroovy plot = null;
    private final String[] defaults = { "state", "roll", "pitch", "yaw" };

    /**
     * This class represents the editor panel used to edit the script executed periodically in the @RealTimePlotGroovy .
     * The editor has support to highlight the Groovy programming language. The options menu in the upper bar is dynamic
     * and varies according to the selected system on the main panel.
     * 
     * @param realTimePlotGroovy
     * @param sysID ID of system(s) being used on the script
     */
    public RealTimePlotScript(RealTimePlotGroovy rtplot) {
        this.editorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
        this.editorPane.setCodeFoldingEnabled(true);
        this.plot = rtplot;
        this.dialog = new JDialog(SwingUtilities.getWindowAncestor(plot.getConsole()),
                ModalityType.DOCUMENT_MODAL);
        this.dialog.setTitle("Real-time plot settings");
        // create script editor
        setLayout(new BorderLayout(3, 3));

        this.save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String script = editorPane.getText();
                plot.numPoints = Integer.parseInt(RealTimePlotScript.this.numPointsField.getText());
                long temp = Long.parseLong(RealTimePlotScript.this.periodicityField.getText());
                if(temp < plot.PERIODICMIN) {
                    GuiUtils.errorMessage(RealTimePlotScript.this.dialog,"Invalid periodicity parameter value", "The periodicity must be at least 100 milliseconds.");
                    return;
                }
                else
                    plot.periodicity = temp;
                if (!script.equals(plot.traceScript)) {
                    try {
                        plot.runScript(script);
                        plot.traceScript = script;
                        plot.propertiesChanged();
                    }
                    catch (Exception e1) {
                        GuiUtils.errorMessage(RealTimePlotScript.this.dialog, "Error Parsing Current Script",
                                e1.getLocalizedMessage());
                        e1.printStackTrace();
                        return;
                    }
                }
            }
        });
        this.store.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int choice,replace=JOptionPane.YES_OPTION;
                final JFileChooser fc = new JFileChooser(path);
                fc.setAcceptAllFileFilterUsed(false);
                fc.setSelectedFile(new File("script.groovy"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Groovy scripts", "groovy");
                fc.addChoosableFileFilter(filter);
                choice = fc.showDialog(rtplot, "Store Script");
                if (choice == JFileChooser.APPROVE_OPTION) {
                    if(fc.getSelectedFile().exists()) {
                        replace = GuiUtils.confirmDialog(editorPane, "Store Script", "Do you want to replace file: "+fc.getSelectedFile().getName()+"?");
                    }
                    if (filter.accept(fc.getSelectedFile()) && replace == JOptionPane.YES_OPTION) {
                        FileUtil.saveToFile(fc.getSelectedFile().getAbsolutePath(), editorPane.getText(),"UTF-8",false);
                        fillStoredScripts();
                    }
                    else if (!filter.accept(fc.getSelectedFile())){
                        GuiUtils.errorMessage(editorPane, "Wrong file extension",
                                "The script extension must be groovy. Please store the script with the correct extension."
                                        + FileUtil.getFileExtension(fc.getSelectedFile()));
                    }
                }
            }
        });
        save.setToolTipText("Save changes to script");
        store.setToolTipText("Store current script locally");
        save.setMinimumSize(new Dimension(5, 5));
        store.setMinimumSize(new Dimension(5, 5));
        numPointsField.setMinimumSize(new Dimension(5, 5));

        add(this.bar, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new GridLayout(2, 3));
        add(new JScrollPane(editorPane), BorderLayout.CENTER);
        //Editors parameters
        JLabel numLabel = new JLabel(I18n.text("Trace points") + ":");
        JLabel periodLabel = new JLabel(I18n.text("Periodicity (milliseconds)") + ":");
        numLabel.setMinimumSize(new Dimension(5, 5));
        bottom.add(numLabel);//, BorderLayout.CENTER
        bottom.add(numPointsField);
        bottom.add(save);
        bottom.add(periodLabel);
        bottom.add(periodicityField);
        bottom.add(store);
        add(bottom, BorderLayout.SOUTH);
        
        fillTextFields();

        methods.setToolTipText("Insert formulas, methods and other settings");
        methods.add(sysdata);
        methods.add(math);
        methods.add(plotprops);
        methods.add(storedScripts);
        bar.add(methods);
        
        dialog.setSize(400, 400);
        //dialog.setModal(true);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!editorPane.getText().equals(plot.traceScript)) {
                    int op = GuiUtils.confirmDialog(RealTimePlotScript.this.dialog, I18n.text("Select an Option"),
                            I18n.text("Do you wish to save changes?"));
                    if (op == JOptionPane.YES_OPTION) {
                        save.doClick();
                        RealTimePlotScript.this.dialog.dispose();
                        
                    }
                    else if (op == JOptionPane.NO_OPTION) {
                        RealTimePlotScript.this.dialog.dispose();
                    }
                }
                else {
                    RealTimePlotScript.this.dialog.dispose();
                }
            }
        });
    }

    /**
     * @param rtplot
     */
    private void fillTextFields() {
        editorPane.setText(plot.traceScript);
        periodicityField.setText("" + plot.periodicity);
        numPointsField.setText("" + plot.numPoints);
        editorPane.setText(plot.traceScript);
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
                addText("plotAbsoluteLatLong()", true);

            }
        });
        JMenuItem ned = new JMenuItem("NED Plot");
        ned.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addText("plotNED()", true);

            }
        });
        JMenuItem nedFrom = new JMenuItem("Relative NED Plot");
        nedFrom.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addText("double lat = Math.toDegrees(0.0)\ndouble lon = Math.toDegrees(0.0)\ndouble h = 0.0 \nplotNEDFrom(lat,lon,h)", true);

            }
        });
        JMenuItem drawLine = new JMenuItem("Connected Plot Points");
        drawLine.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addText("setDrawLineForXY true", false);

            }
        });
        JMenuItem other = new JMenuItem("XY Plot");
        other.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                RealTimePlotScript.this.editorPane.setText("");
                addText("s = value(\"EstimatedState.depth\")\nt = value(\"EstimatedState.timestamp\")\nxyseries t,s",
                        true);

            }
        });
        plotType1.add(drawLine);
        plotType1.add(latlong);
        plotType1.add(ned);
        plotType1.add(nedFrom);
        plotType1.add(drawLine);
        plotType1.add(other);
        plotprops.removeAll();
        plotprops.add(plotType0);
        plotprops.add(plotType1);

    }

    /**
     * Creates and configures the settings editor according to the selected system(s)
     * @param rtplot - The caller plugin panel
     * @param sysID  - The selected system(s)
     */
    public void editSettings(String sysID) {

        fillTextFields();
        updateLocalVars(sysID);
        fillPlotOptions();
        fillMathOptions();
        fillStoredScripts();
        this.dialog.setContentPane(this);
        GuiUtils.centerParent(this.dialog, (Window) this.plot.getConsole());
        this.dialog.setVisible(true);
    }

    /**
     * Updates the Option @JMenu according to the selected system(s)
     */
    private void updateLocalVars(String id) {
        this.sysdata.removeAll();
        JMenu deft = new JMenu("Default");
        createDefaultOptions(deft, defaults);
        this.sysdata.add(deft);
        for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles())
            if (id.equalsIgnoreCase("ALL")) {
                JMenu menu = new JMenu(s.getName());
                createExtraSysOptions(s.getName(), menu);
                this.sysdata.add(menu);
            }
            else if (id.equalsIgnoreCase(s.getName())) {
                createExtraSysOptions(s.getName(), this.sysdata);
                return;
            }
    }

    /**
     * List in the @JMenu the stored scripts under the directory conf/mraplots/realtime/
     */
    private void fillStoredScripts() {
        File conf_mra_rtplot = new File(path);

        // ensure path exists
        if (!conf_mra_rtplot.exists()) {
            conf_mra_rtplot.mkdirs();
        }
        this.storedScripts.removeAll();
        for (final File fileEntry : conf_mra_rtplot.listFiles()) {
            if (!fileEntry.isDirectory()) {
                String scriptName = FileUtil.getFileNameWithoutExtension(fileEntry);
                JMenuItem item = new JMenuItem(scriptName);

                item.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String aux = scriptName.concat(".groovy");
                        File f = new File(path.concat(aux));
                        if (f.exists() && f.isFile())
                            RealTimePlotScript.this.editorPane.setText(FileUtil.getFileAsString(f));
                    }
                });
                this.storedScripts.add(item);
            }
        }
    }

    /**
     * @param component - @JMenu to add default methods options as @JMenuItem
     */
    private void createDefaultOptions(JMenu component, String[] options) {
        for (int i = 0; i < options.length; i++) {
            String var = options[i];
            String s = i == 0 ? ("addTimeSeries " + var + "(\"<field>\")")
                    : ("addTimeSeries " + var + "(),\"" + var + "\"");
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
    private void fillMathOptions() {
        final String[] meths = { "acos", "asin", "atan", "atan2", "cos", "cosh", "sin", "sinh", "tan", "tanh",
                "toDegrees", "toRadians" };
        final List<String> trigMethods = new ArrayList<>();
        for (String s : meths)
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
        this.math.removeAll();
        this.math.add(trig);
        this.math.add(other);
    }

    /**
     * Returns a method signature, including the parameters type
     * 
     * @param method
     * @return signature in a String
     */
    public String methodSignature(Method method) {
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
    protected void createExtraSysOptions(String system, JMenu component) {
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
    protected void addText(String code, boolean deletePrevious) {
        String current, old = editorPane.getText();
        StringBuilder sb = new StringBuilder(code.length() + old.length());
        if (!deletePrevious)
            sb.append(old + "\n");
        sb.append(code);
        current = sb.toString();
        this.editorPane.setText(current);
    }
}
