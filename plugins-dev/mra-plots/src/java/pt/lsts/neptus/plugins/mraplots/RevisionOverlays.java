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
 * Dec 5, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.Plot3D;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author zp
 */
@PluginDescription(name="Log Preview", icon="pt/lsts/neptus/plugins/mraplots/eye.png")
public class RevisionOverlays extends SimpleRendererInteraction {

    @NeptusProperty
    public String defaultDirectory = ".";

    private static final long serialVersionUID = -1737553152800873793L;
    private RevisionSidePanel sidePanel = new RevisionSidePanel(this);    
    StateRenderer2D renderer = null;

    public RevisionOverlays(ConsoleLayout console) {
        super(console);
        setVisibility(false);
        sidePanel.setMinimumSize(new Dimension(150, 150));        
        sidePanel.setPreferredSize(new Dimension(150, 150));
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    protected JPopupMenu buildPopup() {
        JPopupMenu popup = new JPopupMenu();

        popup.add("Open Lsf").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sidePanel.selectFile();
            }
        });


        if (sidePanel.getLogSource() != null) {
            popup.addSeparator();

            popup.add("Open in MRA").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            JFrame mra = new NeptusMRA();
                            mra.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            ((NeptusMRA) mra).getMraFilesHandler().openLog(sidePanel.getLogFile());
                        }
                    }).run();


                }
            });            

            popup.add("Import plan").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    importPlan();
                }
            });

            popup.add("3D Plot").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog = new JDialog(getConsole());
                    final Plot3D plot = new Plot3D(null);
                    dialog.getContentPane().add(plot.getComponent(sidePanel.getLogSource(), 1.0));
                    dialog.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            plot.onCleanup();
                        }
                    });
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setSize(600, 400);
                    dialog.setResizable(false);
                    dialog.setTitle("3D log review - "+sidePanel.getLogFile().getName());                    
                    plot.onShow();
                    dialog.setVisible(true);
                }
            });
        }
        return popup;
    }

    protected void importMarkers() {

    }

    protected void importPlan() {
        IMraLogGroup logSource = sidePanel.getLogSource();
        if (logSource == null)
            return;

        PlanType plan = LogUtils.generatePlan(getConsole().getMission(), logSource);

        if (plan == null) {
            GuiUtils.errorMessage(getConsole(), "Import plan", "No plan was found in this log.");
            return;
        }
        String lastPlanId = plan.getId();
        String planId = null;

        while (true) {
            planId = JOptionPane.showInputDialog(getConsole(), I18n.text("Enter the plan ID"), lastPlanId);

            if (planId == null)
                return;
            if (getConsole().getMission().getIndividualPlansList().get(planId) != null) {
                int option = JOptionPane.showConfirmDialog(getConsole(),
                        I18n.text("Do you wish to replace the existing plan with same name?"));
                if (option == JOptionPane.CANCEL_OPTION)
                    return;
                else if (option == JOptionPane.YES_OPTION) {
                    break;
                }
                lastPlanId = planId;
            }
            else
                break;
        }
        plan.setId(planId);
        plan.setVehicle(getConsole().getMainSystem());
        plan.setMissionType(getConsole().getMission());
        getConsole().getMission().addPlan(plan);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                getConsole().getMission().save(true);
                return null;
            }
        };
        worker.execute();

        if (getConsole().getPlan() == null || getConsole().getPlan().getId().equalsIgnoreCase(plan.getId())) {
            getConsole().setPlan(plan);
        }

        getConsole().warnMissionListeners();
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = buildPopup();
            popup.show(source, event.getX(), event.getY());
        }
        else
            super.mouseClicked(event, source);
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        Container parent = source.getParent();
        this.renderer = source;
        while (parent != null && !(parent.getLayout() instanceof BorderLayout)) 
            parent = parent.getParent();
        if (mode)
            parent.add(sidePanel, BorderLayout.EAST);
        else {
            parent = sidePanel.getParent();
            sidePanel.getParent().remove(sidePanel);
        }
        parent.invalidate();
        parent.validate();
        parent.repaint();
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void cleanSubPanel() {

    };
}
