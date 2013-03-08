/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/09/23
 * $Id:: CollapsibleContainer.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.plugins.containers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.MattePainter;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author ZP
 * 
 */
@PluginDescription(name = "Console Layout: Collapsible", description = "Collapsible subpanels", icon = "pt/up/fe/dceg/neptus/plugins/containers/layout.png")
public class CollapsibleContainer extends ContainerSubPanel {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(hidden = true)
    public String state = "";

    protected LinkedHashMap<SubPanel, JXCollapsiblePane> colPanes = new LinkedHashMap<SubPanel, JXCollapsiblePane>();
    protected LinkedHashMap<SubPanel, JPanel> auxPanels = new LinkedHashMap<SubPanel, JPanel>();

    public CollapsibleContainer(ConsoleLayout console) {
        super(console);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray.darker()));
    }

    @Override
    public void addSubPanel(SubPanel panel) {
        // panel.setBorder(BorderFactory.createEmptyBorder());
        int pos = panels.size();
        panels.add(panel);

        String[] states = state.split(",");
        if (states.length > pos && states[pos].equals("0"))
            addCollapsiblePanel(panel, panel.getName(), true);
        else
            addCollapsiblePanel(panel, panel.getName(), false);

        doLayout();
        invalidate();
        revalidate();
    }

    @Override
    public void removeSubPanel(SubPanel sp) {
        panels.remove(sp);
        remove(colPanes.get(sp));
        remove(auxPanels.get(sp));
        colPanes.remove(sp);
        auxPanels.remove(sp);

        doLayout();
        invalidate();
        revalidate();
    }

    private JXCollapsiblePane addCollapsiblePanel(SubPanel cmp, String label, boolean collapsed) {
        // JPanel tmp = new JPanel(new BorderLayout());
        // tmp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2),
        // BorderFactory.createLineBorder(Color.black)));

        final JXCollapsiblePane colPane = new JXCollapsiblePane();
        colPane.setLayout(new BorderLayout());
        colPane.add(cmp, BorderLayout.CENTER);
        final JLabel minimizeLbl = new JLabel(ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/containers/minus.png", 12, 12));
        minimizeLbl.setPreferredSize(new Dimension(12, 12));
        minimizeLbl.setMaximumSize(new Dimension(12, 12));
        minimizeLbl.setMinimumSize(new Dimension(12, 12));
        minimizeLbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        minimizeLbl.setToolTipText("minimize");

        final JLabel auxLbl = new JLabel(label);
        JXPanel auxPanel = new JXPanel(new BorderLayout());
        auxPanel.add(auxLbl, BorderLayout.CENTER);
        auxPanel.add(minimizeLbl, BorderLayout.EAST);

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getSource() == minimizeLbl || e.getClickCount() == 2) {
                    if (minimizeLbl.getToolTipText().equals("minimize")) {
                        colPane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION)
                                .actionPerformed(new ActionEvent(minimizeLbl, ActionEvent.RESERVED_ID_MAX, "toggle"));
                        minimizeLbl.setToolTipText("maximize");
                        minimizeLbl.setIcon(ImageUtils.getScaledIcon(
                                "pt/up/fe/dceg/neptus/plugins/containers/plus.png", 12, 12));
                    }
                    else {
                        colPane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION)
                                .actionPerformed(new ActionEvent(minimizeLbl, ActionEvent.RESERVED_ID_MAX, "toggle"));
                        minimizeLbl.setToolTipText("minimize");
                        minimizeLbl.setIcon(ImageUtils.getScaledIcon(
                                "pt/up/fe/dceg/neptus/plugins/containers/minus.png", 12, 12));
                    }
                    state = computeState();
                    // getConsole().setConsoleSaved(false);
                }
            }
        };

        minimizeLbl.addMouseListener(adapter);
        auxLbl.addMouseListener(adapter);
        auxPanel.setBorder(LineBorder.createBlackLineBorder());
        auxPanel.setPreferredSize(new Dimension(100, 14));
        auxPanel.setMinimumSize(new Dimension(100, 14));
        auxPanel.setMaximumSize(new Dimension(1000, 14));
        auxPanel.setBackgroundPainter(new CompoundPainter<JXPanel>(new MattePainter(new GradientPaint(0, 0, Color.gray
                .brighter(), 0, 15, Color.gray)), new GlossPainter()));
        add(auxPanel);
        colPane.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Color.black));

        colPane.setPreferredSize(new Dimension(getWidth(), (int) cmp.getPreferredSize().getHeight()));
        // colPane.setMinimumSize(new Dimension(1, getWidth()));
        // colPane.setMaximumSize(getPreferredSize());

        if (collapsed) {
            colPane.setCollapsed(true);
            minimizeLbl.setToolTipText("maximize");
            minimizeLbl.setIcon(ImageUtils.getScaledIcon("pt/up/fe/dceg/neptus/plugins/containers/plus.png", 12, 12));
        }
        add(colPane);

        auxPanels.put(cmp, auxPanel);
        colPanes.put(cmp, colPane);

        return colPane;
    }

    protected String computeState() {
        String state = "";
        for (JXCollapsiblePane pane : colPanes.values()) {
            if (pane.isCollapsed())
                state += "0,";
            else
                state += "1,";
        }
        return state + "0";
    }

    public static void main(String[] args) {
        // ConfigFetch.initialize();
        // GuiUtils.setLookAndFeel();
        // CollapsibleContainer cc = new CollapsibleContainer();
        // cc.addCollapsiblePanel(new MissionTreePanel(), "testing1", true);
        // cc.addCollapsiblePanel(new MissionTreePanel(), "testing2", false);
        //
        // GuiUtils.testFrame(cc, "test");
    }
}
