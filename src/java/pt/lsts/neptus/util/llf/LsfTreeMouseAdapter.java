/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Sep 20, 2012
 */
package pt.lsts.neptus.util.llf;

import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.CloseTabbedPane;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAChartPanel;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.GenericMultiAxisPlot;
import pt.lsts.neptus.mra.plots.GenericPlot;
import pt.lsts.neptus.mra.plots.ReplayPlot;
import pt.lsts.neptus.mra.visualizations.ColorMapVisualization;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.chart.LLFChart;

/**
 * This class deals with all the instatiation logic associated with LLFTree interaction
 * 
 * @author jqcorreia
 * 
 */
public class LsfTreeMouseAdapter extends MouseAdapter {
    LsfTree tree;
    IMraLogGroup source;
    MRAPanel panel;
    CloseTabbedPane tabPane;

    public LsfTreeMouseAdapter(MRAPanel mraPanel) {
        panel = mraPanel;
        tree = panel.getTree();
        source = panel.getSource();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TreePath[] path = tree.getSelectionPaths();
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            if (path == null)
                return;
            if (path.length == 1 && path[0].getPath().length == 2) {
                final String fileToOpen = path[0].getPath()[1].toString();
                IMraLog log = source.getLog(fileToOpen);
                panel.loadVisualization(new LogTableVisualization(log, panel), true);
            }
        }
        if (e.getButton() == MouseEvent.BUTTON3) {
            if (path == null)
                return;

            if (path.length == 1 && path[0].getPath().length == 2) {
                final String fileToOpen = path[0].getPath()[1].toString();
                JPopupMenu popup = new JPopupMenu();

                String text = I18n.textf("Show %log data", fileToOpen);

                popup.add(text).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        IMraLog log = source.getLog(fileToOpen);
                        panel.loadVisualization(new LogTableVisualization(log, panel), true);
                    }
                });

                popup.show(tree, e.getX(), e.getY());
                return;
            }

            final Vector<String> fieldsToPlot = new Vector<String>();
            int count = 0;

            for (int i = 0; i < path.length; i++) {
                if (path[i].getPath().length == 3) {
                    count++;
                    String message = path[i].getPath()[1].toString();
                    String field = path[i].getPath()[2].toString();

                    fieldsToPlot.add(message + "." + field);

                }
            }

            if (count == 0)
                return;

            JPopupMenu popup = new JPopupMenu();

            popup.add(I18n.text("Plot data")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    panel.loadVisualization(new GenericPlot(fieldsToPlot.toArray(new String[0]), panel), true);
                }
            });

            if(count > 1) {
                popup.add(I18n.text("Multi plot data")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        panel.loadVisualization(new GenericMultiAxisPlot(fieldsToPlot.toArray(new String[0]), panel), true);
                    }
                });
            }

            popup.add(I18n.text("Timeline Plot")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    panel.loadVisualization(new ReplayPlot(panel, fieldsToPlot.toArray(new String[0])), true);
                }
            });

            popup.add(I18n.text("Plot data on new window")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {

                    LLFChart chart = new GenericPlot(fieldsToPlot.toArray(new String[0]), panel);
                    MRAChartPanel fcp = new MRAChartPanel(chart, source, panel);
                    JDialog dialog = new JDialog(ConfigFetch.getSuperParentAsFrame());
                    dialog.setTitle("[MRA] " + chart.getName());
                    dialog.setIconImage(ImageUtils.getScaledImage("images/menus/graph.png", 16, 16));
                    dialog.add(fcp);
                    dialog.setSize(640, 480);
                    dialog.setResizable(true);
                    dialog.setVisible(true);
                    fcp.regeneratePanel();
                }
            });

            if(count > 1) {
                popup.add(I18n.text("Multi plot data on new window")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {

                        LLFChart chart = new GenericMultiAxisPlot(fieldsToPlot.toArray(new String[0]), panel);
                        MRAChartPanel fcp = new MRAChartPanel(chart, source, panel);
                        JDialog dialog = new JDialog(ConfigFetch.getSuperParentAsFrame());
                        dialog.setTitle("[MRA] " + chart.getName());
                        dialog.setIconImage(ImageUtils.getScaledImage("images/menus/graph.png", 16, 16));
                        dialog.add(fcp);
                        dialog.setSize(640, 480);
                        dialog.setResizable(true);
                        dialog.setVisible(true);
                        fcp.regeneratePanel();
                    }
                });
            }

            if(count == 1) {
                popup.add(I18n.text("Plot ColorMap")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        NeptusLog.pub().info("<###> "+fieldsToPlot);
                        panel.loadVisualization(new ColorMapVisualization(panel, "ALL", fieldsToPlot.get(0)), true);
                    }
                });
            }
            popup.show(tree, e.getX(), e.getY());
        }
    }
}
