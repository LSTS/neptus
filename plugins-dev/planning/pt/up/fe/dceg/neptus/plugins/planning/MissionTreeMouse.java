/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: meg
 * Sep 18, 2013
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.gui.MissionBrowser;
import pt.up.fe.dceg.neptus.gui.MissionBrowser.NodeInfoKey;
import pt.up.fe.dceg.neptus.gui.MissionBrowser.State;
import pt.up.fe.dceg.neptus.gui.VehicleSelectionDialog;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBControl;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.types.Identifiable;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * MouseAdapter for the Mission Tree panel.
 * 
 * @author Margarida
 * 
 */
public class MissionTreeMouse extends MouseAdapter {

    private final MissionBrowser browser;
    private final ConsoleLayout console;
    private final PlanDBControl pdbControl;
    private Container popupMenu;
    private final Vector<ActionItem> extraPlanActions = new Vector<ActionItem>();

    /**
     * @param browser
     * @param console
     * @param pdbControl
     */
    public MissionTreeMouse(MissionBrowser browser, ConsoleLayout console, PlanDBControl pdbControl) {
        super();
        this.browser = browser;
        this.console = console;
        this.pdbControl = pdbControl;
    }

    /**
     * Item to be added to the menu in case selected item is a local plan.
     * 
     * @param item
     */
    public void addPlanMenuItem(ActionItem item) {
        extraPlanActions.add(item);
    }

    /**
     * 
     * @param label in the menu item to remove (before translation)
     * @return true if menu item is found, false otherwise
     */
    public boolean removePlanMenuItem(String label) {
        int componentCount = popupMenu.getComponentCount();
        for (int c = 0; c < componentCount; c++) {
            Component component = popupMenu.getComponent(c);
            if (component instanceof JMenuItem) {
                String labelM = ((JMenuItem) component).getText();
                if (labelM.equals(I18n.text(label))) {
                    popupMenu.remove(component);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        final Object[] multiSel = browser.getSelectedItems();
        if (e.getButton() != MouseEvent.BUTTON3)
            return;

        int plansCount = 0;
        if (multiSel != null)
            for (Object o : multiSel) {
                if (o instanceof PlanType) {
                    plansCount++;
                }
            }

        if (multiSel == null || multiSel.length <= 1) {

            browser.setMultiSelect(e);
        }

        final Object selection = browser.getSelectedItem();
        DefaultMutableTreeNode selectionNode = browser.getSelectedTreeNode();

        JPopupMenu popupMenu = new JPopupMenu();
        JMenu dissemination = new JMenu(I18n.text("Dissemination"));

        if (Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null) != null) {
            dissemination.add(I18n.text("Paste URL")).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (browser.setContent(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null),
                            console.getMission())) {
                        console.updateMissionListeners();
                    }
                }
            });
            dissemination.addSeparator();
        }

        if (selection == null) {
            popupMenu.addSeparator();
            popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.addTransponderElement(console);
                }
            });
        }
        else if (selection instanceof PlanType) {
            // if (plansCount == 1) {
                popupMenu.addSeparator();
                addActionRemovePlanLocally(console, (Identifiable) selection, popupMenu);
                addActionSendPlan(console, pdbControl, selection, popupMenu);

                State syncState = (State) ((ExtendedTreeNode) selectionNode).getUserInfo().get("sync");
                if (syncState == null)
                    syncState = State.LOCAL;
                else if (syncState == State.SYNC || syncState == State.NOT_SYNC) {
                    addActionRemovePlanRemotely(console, pdbControl, (Identifiable) selection, popupMenu);
                }

                dissemination.add(I18n.textf("Share '%planName'", selection)).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // disseminate((XmlOutputMethods) selection, "Plan");
                        console.getImcMsgManager().broadcastToCCUs(((PlanType) selection).asIMCPlan());
                    }
                });

                popupMenu.add(I18n.text("Change plan vehicles")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (selection != null) {
                            PlanType sel = (PlanType) selection;

                            String[] vehicles = VehicleSelectionDialog.showSelectionDialog(console, sel.getVehicles()
                                    .toArray(new VehicleType[0]));
                            Vector<VehicleType> vts = new Vector<VehicleType>();
                            for (String v : vehicles) {
                                vts.add(VehiclesHolder.getVehicleById(v));
                            }
                            sel.setVehicles(vts);
                        }
                    }
                });
            // }
            ActionItem actionItem;
            for (int a = 0; a < extraPlanActions.size(); a++) {
                actionItem = extraPlanActions.get(a);
                popupMenu.add(I18n.text(actionItem.label)).addActionListener(actionItem.action);
                }
        }
        else if (selection instanceof PlanDBInfo) {
            State syncState = selectionNode instanceof ExtendedTreeNode ? (State) ((ExtendedTreeNode) selectionNode)
                    .getUserInfo().get(NodeInfoKey.SYNC.name()) : null;
            if (syncState == null)
                syncState = State.LOCAL;

            else if (syncState == State.REMOTE) {
                addActionGetRemotePlan(console, pdbControl, selection, popupMenu);

                addActionRemovePlanRemotely(console, pdbControl, (Identifiable) selection, popupMenu);

                // popupMenu.add(
                // I18n.textf("bug Remove '%planName' from %system", selection, console2.getMainSystem()))
                // .addActionListener(new ActionListener() {
                // @Override
                // public void actionPerformed(ActionEvent e) {
                // if (selection != null) {
                // pdbControl.setRemoteSystemId(console2.getMainSystem());
                // PlanDBInfo sel = (PlanDBInfo) selection;
                // pdbControl.deletePlan(sel.getPlanId());
                // }
                // }
                // });
            }
        }
        else if (selection instanceof TransponderElement) {

            popupMenu.addSeparator();

            popupMenu.add(I18n.textf("View/Edit '%transponderName'", selection)).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            browser.editTransponder((TransponderElement) selection, console.getMission());
                        }
                    });

            popupMenu.add(I18n.textf("Remove '%transponderName'", selection)).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.removeTransponder((TransponderElement) selection, console);
                }
            });

            Vector<TransponderElement> allTransponderElements = MapGroup.getMapGroupInstance(console.getMission())
                    .getAllObjectsOfType(TransponderElement.class);
            for (final AbstractElement tel : allTransponderElements) {
                if ((TransponderElement) selection != (TransponderElement) tel) {
                    popupMenu.add(I18n.textf("Switch '%transponderName1' with '%transponderName2'", selection, tel))
                            .addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            browser.swithLocationsTransponder((TransponderElement) selection,
                                                    (TransponderElement) tel, console);
                                        };
                                    }.start();
                                }
                            });
                }
            }

            addActionShare((Identifiable) selection, dissemination, "Transponder");

            popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.addTransponderElement(console);
                }
            });

        }
        //
        // else if (selection instanceof MarkElement) {
        //
        // popupMenu.addSeparator();
        //
        // dissemination.add(I18n.text("Share startup position")).addActionListener(new ActionListener() {
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // ImcMsgManager.disseminate((XmlOutputMethods) selection, "StartLocation");
        // }
        // });
        // }
        else if (selection instanceof HomeReference) {
            popupMenu.add(I18n.text("View/Edit home reference")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    LocationType loc = new LocationType((HomeReference) selection);
                    LocationType after = LocationPanel.showLocationDialog(console, I18n.text("Set home reference"),
                            loc, console.getMission(), true);
                    if (after == null)
                        return;

                    console.getMission().getHomeRef().setLocation(after);

                    Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(console.getMission())
                            .getAllObjectsOfType(HomeReferenceElement.class);
                    hrefElems.get(0).setCoordinateSystem(console.getMission().getHomeRef());
                    console.getMission().save(false);
                    console.updateMissionListeners();
                }
            });
        }

        if (plansCount > 1) {
            popupMenu.addSeparator();

            popupMenu.add(I18n.text("Remove selected plans")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selection != null) {
                        int resp = JOptionPane.showConfirmDialog(console,
                                I18n.textf("Remove all selected plans (%numberOfPlans)?", multiSel.length));

                        if (resp == JOptionPane.YES_OPTION) {
                            for (Object o : multiSel) {
                                PlanType sel = (PlanType) o;
                                console.getMission().getIndividualPlansList().remove(sel.getId());
                            }
                            console.getMission().save(false);

                            if (console != null)
                                console.setPlan(null);
                            browser.refreshBrowser(console.getPlan(), console.getMission());
                        }
                    }
                }
            });
        }

        popupMenu.addSeparator();
        popupMenu.add(I18n.text("Reload Panel")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browser.refreshBrowser(console.getPlan(), console.getMission());
            }
        });

        popupMenu.add(dissemination);

        popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
    }

    private <T extends Identifiable> void addActionShare(final T selection, JMenu dissemination,
            final String objectTypeName) {
        dissemination.add(I18n.textf("Share '%transponderName'", selection.getIdentification())).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ImcMsgManager.disseminate((XmlOutputMethods) selection, objectTypeName);
                    }
                });
    }

    private <T> void addActionGetRemotePlan(final ConsoleLayout console2, final PlanDBControl pdbControl,
            final T selection, JPopupMenu popupMenu) {
        popupMenu.add(I18n.textf("Get '%planName' from %system", selection, console2.getMainSystem()))
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (selection != null) {
                            pdbControl.setRemoteSystemId(console2.getMainSystem());
                            pdbControl.requestPlan(((Identifiable) selection).getIdentification());
                        }
                    }
                });
    }

    private <T extends Identifiable> void addActionRemovePlanRemotely(final ConsoleLayout console2,
            final PlanDBControl pdbControl, final T selection, JPopupMenu popupMenu) {
        popupMenu.add(I18n.textf("Remove '%planName' from %system", selection, console2.getMainSystem()))
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (selection != null) {
                            // PlanType sel = (PlanType) selection;
                            pdbControl.setRemoteSystemId(console2.getMainSystem());
                            pdbControl.deletePlan(((Identifiable) selection).getIdentification());
                        }
                    }
                });
    }

    private void addActionSendPlan(final ConsoleLayout console2, final PlanDBControl pdbControl,
            final Object selection, JPopupMenu popupMenu) {
        popupMenu.add(I18n.textf("Send '%planName' to %system", selection, console2.getMainSystem()))
                .addActionListener(

                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (selection != null) {
                            PlanType sel = (PlanType) selection;
                            pdbControl.setRemoteSystemId(console2.getMainSystem());
                            pdbControl.sendPlan(sel);
                        }
                    }
                });
    }

    /**
     * @param console2
     * @param selection
     * @param popupMenu
     */
    private <T extends Identifiable> void addActionRemovePlanLocally(final ConsoleLayout console2, final T selection,
            JPopupMenu popupMenu) {

        popupMenu.add(I18n.textf("Remove '%planName'", selection)).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selection != null) {
                    int resp = JOptionPane.showConfirmDialog(console2,
                            I18n.textf("Remove the plan '%planName'?", selection.toString()));
                    if (resp == JOptionPane.YES_OPTION) {
                        console2.getMission().getIndividualPlansList().remove(selection.getIdentification());
                        console2.getMission().save(false);

                        if (console2 != null)
                            console2.setPlan(null);
                        browser.refreshBrowser(console2.getPlan(), console2.getMission());
                    }
                }
            }
        });
    }

}

/**
 * Class with items to add to a JMenu.
 * 
 * @author Margarida
 * 
 */
class ActionItem {
    /**
     * The label appering in the menu. This is the string without translation.
     */
    public String label;
    /**
     * The listner associated with the menu item.
     */
    public ActionListener action;
}
