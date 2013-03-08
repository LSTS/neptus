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
 * Feb 14, 2013
 * $Id:: RealTimePlotSettings.java 9933 2013-02-15 03:32:23Z robot              $:
 */
package pt.up.fe.dceg.neptus.plugins.rtplot;

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

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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
