/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Sep 20, 2012
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JPopupMenu;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.gui.CloseTabbedPane;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.ColorMapVisualization;
import pt.up.fe.dceg.neptus.mra.MraChartPanel;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.GenericPlot;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.llf.chart.LLFChart;

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
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println(fieldsToPlot);
                    panel.loadVisualization(new GenericPlot(fieldsToPlot.toArray(new String[0]), panel), true);
                }
            });
            popup.add(I18n.text("Plot data on new window")).addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {

                    LLFChart chart = new GenericPlot(fieldsToPlot.toArray(new String[0]), panel);
                    MraChartPanel fcp = new MraChartPanel(chart, source, panel);
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
            if(count == 1) {
                popup.add(I18n.text("Plot ColorMap")).addActionListener(new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        System.out.println(fieldsToPlot);
                        panel.loadVisualization(new ColorMapVisualization(panel, fieldsToPlot.get(0)), true);
                    }
                });
            }
            popup.show(tree, e.getX(), e.getY());
        }
    }
}
