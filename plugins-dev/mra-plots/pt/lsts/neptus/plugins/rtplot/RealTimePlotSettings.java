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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Feb 14, 2013
 */
package pt.lsts.neptus.plugins.rtplot;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class RealTimePlotSettings extends JPanel {

    private static final long serialVersionUID = 1L;
    protected JEditorPane editorPane = new JEditorPane();
    protected JFormattedTextField periodicityField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    protected JFormattedTextField numPointsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    
    public RealTimePlotSettings(RealTimePlot plot) {
        setLayout(new BorderLayout(3,3));
        add(new JScrollPane(editorPane), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 2));
        bottom.add(new JLabel(I18n.text("Periodicity (milliseconds)") + ":"));
        bottom.add(periodicityField);
        bottom.add(new JLabel(I18n.text("Trace points") + ":"));
        bottom.add(numPointsField);
        
        add(bottom, BorderLayout.SOUTH);
        editorPane.setText(plot.traceScripts);
        periodicityField.setText(""+plot.periodicity);
        numPointsField.setText(""+plot.numPoints);
    }
    
    public static void editSettings(final RealTimePlot plot) {
        final JDialog dialog = new JDialog(plot.getConsole());
        final RealTimePlotSettings settings = new RealTimePlotSettings(plot);
        dialog.getContentPane().add(settings);
        dialog.setSize(400, 400);
        dialog.setModal(true);
        
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // int op = JOptionPane.showConfirmDialog(dialog, I18n.text("Do you wish to save changes?")); // Avoid this due to Modality blocks all Neotus
                int op = GuiUtils.confirmDialog(dialog, I18n.text("Select an Option"), I18n.text("Do you wish to save changes?"));
                if (op == JOptionPane.YES_OPTION) {
                    plot.periodicity = Integer.parseInt(settings.periodicityField.getText());
                    plot.numPoints = Integer.parseInt(settings.numPointsField.getText());
                    plot.traceScripts = settings.editorPane.getText();
                    plot.propertiesChanged();
                    dialog.dispose();
                }
                else if (op == JOptionPane.NO_OPTION) {
                    dialog.dispose();
                }
            }

        });
        GuiUtils.centerParent(dialog, plot.getConsole());
        dialog.setVisible(true);        
    }
}
