/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/09/23
 */
package pt.lsts.neptus.console.plugins.containers;

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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.MattePainter;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author ZP
 * 
 */
@PluginDescription(name = "Console Layout: Collapsible", description = "Collapsible subpanels", 
    icon = "pt/lsts/neptus/console/plugins/containers/layout.png", experimental = true)
public class CollapsibleContainer extends ContainerSubPanel {

    private static final long serialVersionUID = 1L;

    private static final ImageIcon MINUS_IMG = ImageUtils
            .getScaledIcon(FileUtil.getPackageAsPath(CollapsibleContainer.class) + "/minus.png", 12, 12);
    private static final ImageIcon PLUS_IMG = ImageUtils
            .getScaledIcon(FileUtil.getPackageAsPath(CollapsibleContainer.class) + "/plus.png", 12, 12);

    @NeptusProperty(editable = false)
    public String state = "";

    protected LinkedHashMap<ConsolePanel, JXCollapsiblePane> colPanes = new LinkedHashMap<ConsolePanel, JXCollapsiblePane>();
    protected LinkedHashMap<ConsolePanel, JPanel> auxPanels = new LinkedHashMap<ConsolePanel, JPanel>();

    public CollapsibleContainer(ConsoleLayout console) {
        super(console);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray.darker()));
    }

    @Override
    public boolean addSubPanelExtra(ConsolePanel panel) {
        int pos = panels.size();

        String[] states = state.split(",");
        if (states.length > pos && states[pos].equals("0"))
            addCollapsiblePanel(panel, panel.getName(), true);
        else
            addCollapsiblePanel(panel, panel.getName(), false);

        return true;
    }

    @Override
    public void removeSubPanelExtra(ConsolePanel sp) {
        remove(colPanes.get(sp));
        remove(auxPanels.get(sp));
        colPanes.remove(sp);
        auxPanels.remove(sp);
    }

    private JXCollapsiblePane addCollapsiblePanel(ConsolePanel cmp, String label, boolean collapsed) {
        // JPanel tmp = new JPanel(new BorderLayout());
        // tmp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2),
        // BorderFactory.createLineBorder(Color.black)));

        final JXCollapsiblePane colPane = new JXCollapsiblePane();
        colPane.setLayout(new BorderLayout());
        colPane.add(cmp, BorderLayout.CENTER);
        final JLabel minimizeLbl = new JLabel(MINUS_IMG);
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
                        minimizeLbl.setIcon(PLUS_IMG);
                    }
                    else {
                        colPane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION)
                                .actionPerformed(new ActionEvent(minimizeLbl, ActionEvent.RESERVED_ID_MAX, "toggle"));
                        minimizeLbl.setToolTipText("minimize");
                        minimizeLbl.setIcon(MINUS_IMG);
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
            minimizeLbl.setIcon(PLUS_IMG);
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
