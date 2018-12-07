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
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import groovy.lang.Binding;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

public class RealTimePlotScript extends JPanel {

    private static final long serialVersionUID = 1L;
    protected JEditorPane editorPane = new JEditorPane();
    protected JFormattedTextField periodicityField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    protected JFormattedTextField numPointsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    private JMenuBar bar = new JMenuBar();
    private JMenu methods = new JMenu("Options");
    private JMenuItem math = new JMenuItem("Math Formulas"), plotprops = new JMenuItem("Plot Properties"),
            sysdata = new JMenuItem("System data");
    private final String systems = "{ s, clo -> s.collect{clo.call(it)} }";
    private final String state = "{ l -> l.EstimatedState }";
    private final String roll = "state.phi * 180/PI", pitch = "state.theta*180/PI", yaw = "state.psi*180/PI";
    private static RealTimePlotGroovy plot = null;
    private Binding binding;

    /**
     * @param realTimePlotGroovy
     * @param sysID ID of system(s) being used on the script
     */
    public RealTimePlotScript(RealTimePlotGroovy rtplot) {
        plot = rtplot;
        binding = new Binding();
        binding.setVariable("roll", roll);
        binding.setVariable("pitch", pitch);
        binding.setVariable("yaw", yaw);
        // create script editor
        setLayout(new BorderLayout(3, 3));
        methods.setToolTipText("Insert formulas, methods and other settings");
        methods.add(sysdata);
        methods.add(math);
        methods.add(plotprops);
        bar.add(methods);
        add(bar, BorderLayout.NORTH);

        add(new JScrollPane(editorPane), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 2));
        bottom.add(new JLabel(I18n.text("Periodicity (milliseconds)") + ":"));
        bottom.add(periodicityField);
        bottom.add(new JLabel(I18n.text("Trace points") + ":"));
        bottom.add(numPointsField);

        add(bottom, BorderLayout.SOUTH);
        editorPane.setText(rtplot.traceScripts);
        periodicityField.setText("" + rtplot.periodicity);
        numPointsField.setText("" + rtplot.numPoints);

        final JDialog dialog = new JDialog(rtplot.getConsole());
        dialog.getContentPane().add(this);
        dialog.setSize(400, 400);
        dialog.setModal(true);

        GuiUtils.centerParent(dialog, rtplot.getConsole());
        dialog.setVisible(true);
    }

    public static void editSettings(final RealTimePlotGroovy rtplot, String sysID) {
        new RealTimePlotScript(rtplot);
        // eval script and create trace(s) for each system
        // rtplot.runScript(b, script);
    }
    
    public static void addSerie(Object data) {
        //Transform data in Jfree serie
        //plot.addSerie();
       
    }
}
