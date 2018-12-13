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
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

public class RealTimePlotScript extends JPanel {

    private static final long serialVersionUID = 1L;
    protected static JEditorPane editorPane = new JEditorPane();
    protected JFormattedTextField periodicityField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    protected JFormattedTextField numPointsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    private JMenuBar bar = new JMenuBar();
    private JMenu methods = new JMenu("Options"), math = new JMenu("Math Formulas"),
            plotprops = new JMenu("Plot Properties");
    private static JMenu sysdata = new JMenu("System data");
    private JButton save = new JButton();
    private static List<String> systems = new ArrayList<>(); // "{ s, clo -> s.collect{clo.call(it)} }";
    private static RealTimePlotGroovy plot = null;
    private static boolean editing = false;

    /**
     * @param realTimePlotGroovy
     * @param sysID ID of system(s) being used on the script
     */
    public RealTimePlotScript(RealTimePlotGroovy rtplot) {
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
                plot.runScript(script);
                editing = false;

            }
        });
        save.setToolTipText("Save changes to script");
        add(bar, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new GridLayout(0, 2));
        add(new JScrollPane(editorPane), BorderLayout.CENTER);
        JPanel bottomL = new JPanel(new BorderLayout());
        JPanel bottomR = new JPanel(new GridLayout(0, 2));
        save.setText("Save");
        save.setSize(new Dimension(5, 5));
        bottomL.add(save, BorderLayout.CENTER);
        bottomR.add(new JLabel(I18n.text("Periodicity (milliseconds)") + ":"));
        bottomR.add(periodicityField);
        bottomR.add(new JLabel(I18n.text("Trace points") + ":"));
        bottomR.add(numPointsField);
        bottom.add(bottomL, BorderLayout.WEST);
        bottom.add(bottomR, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        editorPane.setText(rtplot.traceScript);
        periodicityField.setText("" + rtplot.periodicity);
        numPointsField.setText("" + rtplot.numPoints);
        editorPane.setText(plot.traceScript);

        final JDialog dialog = new JDialog(rtplot.getConsole());
        dialog.getContentPane().add(this);
        dialog.setSize(400, 400);
        dialog.setModal(true);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // int op = JOptionPane.showConfirmDialog(dialog, I18n.text("Do you wish to save changes?")); // Avoid
                // this due to Modality blocks all Neotus
                int op = GuiUtils.confirmDialog(dialog, I18n.text("Select an Option"),
                        I18n.text("Do you wish to save changes?"));
                if (op == JOptionPane.YES_OPTION) {
                    String script = editorPane.getText();
                    plot.runScript(script);
                    plot.traceScript = script;
                    dialog.dispose();
                }
                else if (op == JOptionPane.NO_OPTION) {
                    dialog.dispose();
                }
            }

        });

        GuiUtils.centerParent(dialog, rtplot.getConsole());
        dialog.setVisible(true);
    }

    public static void editSettings(final RealTimePlotGroovy rtplot, String sysID) {
        editing = true;
        updateLocalVars(sysID);
        PlotScript.setSystems(systems);
        PlotScript.setPlot(rtplot);
        rtplot.bind("systems", systems);
        new RealTimePlotScript(rtplot);
        rtplot.initScripts = editorPane.getText();
        // eval script and create trace(s) for each system
        // TODO reset GUI and script/bind variables
    }

    /**
     * 
     */
    private static void updateLocalVars(String id) {
        for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles())
            if (id.equalsIgnoreCase("ALL")) {
                systems.add(s.getName());
                JMenu menu = new JMenu(s.getName());
                createExtraOptions(s.getName(), menu);
                sysdata.add(menu);
            }
            else if (id.equalsIgnoreCase(s.getName())) {
                systems.add(s.getName());
                createExtraOptions(s.getName(), sysdata);
                return;
            }
        // plot.runScript(binding, STATE);
    }

    /**
     * @return the editing
     */
    public static boolean isBeingEditing() {
        return editing;
    }

    protected static void createExtraOptions(String system, JMenu component) {
        for (String msg : ImcMsgManager.getManager().getState(system).availableMessages()) {
            JMenuItem item = new JMenuItem(msg);
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    // RealTimePlotScript.class.getClassLoader();
                    // clazz = Class.forName(msg,false,RealTimePlotScript.class.getClassLoader());
                    // clazz.newInstance();
                    // clazz.cast(m.cloneMessageTyped());
                    //IMCMessage mtyped = new IMCMessage(msg);
                    IMCMessage m = ImcMsgManager.getManager().getState(system).get(msg);
                    m.cloneMessageTyped();
                    Object result = ImcMsgManager.getManager().getState("lauv-xlpore-2").expr(msg+".depth");
//                    System.err.println("Class: "+result.getClass());
//                    System.err.println("double?: "+new Double(result.toString()));
                    addText("msgs("+msg+".<field>)");
                }
            });
            component.add(item);

        }
    }

    /**
     * 
     */
    protected static void addText(String code) {
        String current, old = editorPane.getText();
        StringJoiner sj = new StringJoiner("\n");
        // TODO reset sj?
        sj.setEmptyValue(old);
        sj.add(code);
        current = sj.toString();
        editorPane.setText(current);
    }

    protected EstimatedState getSystemState(ImcSystem s) {
        return (EstimatedState) ImcMsgManager.getManager().getState(s.getName()).get("EstimatedState");
    }

    public static void addSerie(Object data) {
        // Transform data in Jfree serie
        // plot.addSerie();

    }
}
