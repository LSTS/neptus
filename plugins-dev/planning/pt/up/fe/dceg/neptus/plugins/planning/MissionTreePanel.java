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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * 2009/09/22
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.IPlanSelection;
import pt.up.fe.dceg.neptus.console.plugins.ISystemsSelection;
import pt.up.fe.dceg.neptus.console.plugins.ITransponderSelection;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.gui.MissionBrowser;
import pt.up.fe.dceg.neptus.gui.MissionBrowser.State;
import pt.up.fe.dceg.neptus.gui.SimpleTransponderPanel;
import pt.up.fe.dceg.neptus.gui.VehicleSelectionDialog;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.imc.LblRangeAcceptance;
import pt.up.fe.dceg.neptus.imc.LblRangeAcceptance.ACCEPTANCE;
import pt.up.fe.dceg.neptus.imc.PlanSpecification;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.mp.MapChangeListener;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBAdapter;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBControl;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.HTTPUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author ZP
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Mission Tree", author = "José Pinto, Paulo Dias", icon = "pt/up/fe/dceg/neptus/plugins/planning/mission_tree.png", 
category = CATEGORY.PLANNING, version = "1.5.0")
public class MissionTreePanel extends SimpleSubPanel implements MissionChangeListener, MainVehicleChangeListener,
PlanChangeListener, DropTargetListener, NeptusMessageListener, MapChangeListener, IPlanSelection,
        IPeriodicUpdates, ConfigurationListener, ITransponderSelection {

    @NeptusProperty(name = "Use Plan DB Sync. Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean usePlanDBSyncFeatures = true;

    @NeptusProperty(name = "Use Plan DB Sync. Features Extended", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER,
            description = "Needs 'Use Plan DB Sync. Features' on")
    public boolean usePlanDBSyncFeaturesExt = false;

    @NeptusProperty(name = "Use Navigation Start Point", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean useStartPoint = false;

    @NeptusProperty(name = "Debug", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean debugOn = false;

    protected MissionBrowser browser = new MissionBrowser();
    protected PlanDBControl pdbControl = new PlanDBControl();

    protected PlanDBAdapter planDBListener = new PlanDBAdapter() {
        @Override
        public void dbCleared() {
        }

        @Override
        public void dbInfoUpdated(PlanDBState updatedInfo) {
        }

        @Override
        public void dbPlanReceived(PlanType spec) {
            PlanType lp = getConsole().getMission().getIndividualPlansList().get(spec.getId());

            spec.setMissionType(getConsole().getMission());

            getConsole().getMission().addPlan(spec);
            getConsole().getMission().save(true);
            getConsole().updateMissionListeners();

            if (debugOn && lp != null) {
                try {
                    IMCMessage p1 = lp.asIMCPlan(), p2 = spec.asIMCPlan();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IMCOutputStream imcOs = new IMCOutputStream(baos);

                    ByteUtil.dumpAsHex(p1.payloadMD5(), System.out);
                    ByteUtil.dumpAsHex(p2.payloadMD5(), System.out);

                    System.out.println(IMCUtil.getAsHtml(p1));
                    System.out.println(IMCUtil.getAsHtml(p2));

                    p1.serialize(imcOs);
                    ByteUtil.dumpAsHex(baos.toByteArray(), System.out);

                    baos = new ByteArrayOutputStream();
                    imcOs = new IMCOutputStream(baos);
                    p2.serialize(imcOs);
                    ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void dbPlanRemoved(String planId) {
        }

        @Override
        public void dbPlanSent(String planId) {
        }
    };

    public MissionTreePanel(ConsoleLayout console) {
        super(console);
        removeAll();
        setPreferredSize(new Dimension(150, 400));
        setMinimumSize(new Dimension(0, 0));
        setMaximumSize(new Dimension(1000, 1000));

        setLayout(new BorderLayout());
        add(browser, BorderLayout.CENTER);

        new DropTarget(browser, this).setActive(true);

        browser.getElementTree().addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.isAddedPath()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) browser.getElementTree().getSelectionPath()
                            .getLastPathComponent();
                    if (node.getUserObject() instanceof PlanType) {
                        PlanType selectedPlan = (PlanType) node.getUserObject();
                        if (getConsole() != null)
                            getConsole().setPlan(selectedPlan);
                    }
                    else if (getConsole() != null) {
                        getConsole().setPlan(null);                        
                    }
                }
            }
        });

        browser.getElementTree().addMouseListener(new MouseAdapter() {
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

                    TreePath path = browser.getElementTree().getPathForLocation(e.getX(), e.getY());
                    browser.getElementTree().setSelectionPath(path);
                }

                final Object selection = browser.getSelectedItem();
                DefaultMutableTreeNode selectionNode = browser.getSelectedTreeNode();

                JPopupMenu popupMenu = new JPopupMenu();
                JMenu dissemination = new JMenu(I18n.text("Dissemination"));

                if (selection == null) {
                    popupMenu.addSeparator();
                    popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addTransponder();
                        }
                    });

                    if (useStartPoint) {
                        popupMenu.add(I18n.text("Set optional navigation startup position")).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                setStartupPos();
                            }
                        });
                    }

                    // if (!getConsole().getSubPanelsActivesType(new PlanEditor[0]).isEmpty()) {
                    //
                    // final PlanEditor pp = getConsole().getSubPanelsActivesType(new PlanEditor[0]).firstElement();
                    //
                    // popupMenu.add("Add a new plan").addActionListener(new ActionListener() {
                    //
                    // public void actionPerformed(ActionEvent e) {
                    // pp.newPlan();
                    // }
                    // });
                    //
                    // }
                }

                if (Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null) != null) {
                    dissemination.add(I18n.text("Paste URL")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            setContent(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null));
                        }
                    });
                    dissemination.addSeparator();
                }

                if (selection instanceof PlanType) {
                    if (plansCount == 1) {
                        popupMenu.addSeparator();

                        popupMenu.add(I18n.textf("Remove '%planName'", selection)).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (selection != null) {
                                    int resp = JOptionPane.showConfirmDialog(getConsole(),
                                            I18n.textf("Remove the plan '%planName'?", selection.toString()));
                                    if (resp == JOptionPane.YES_OPTION) {
                                        PlanType sel = (PlanType) selection;
                                        getConsole().getMission().getIndividualPlansList().remove(sel.getId());
                                        getConsole().getMission().save(false);

                                        if (getConsole() != null)
                                            getConsole().setPlan(null);
                                        refreshBrowser();
                                    }
                                }
                            }
                        });

                        State syncState = (State) ((ExtendedTreeNode) selectionNode).getUserInfo().get("sync");
                        if (syncState == null)
                            syncState = State.LOCAL;

                        popupMenu.add(
                                I18n.textf("Send '%planName' to %system", selection, getConsole().getMainSystem()))
                                .addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        if (selection != null) {
                                            PlanType sel = (PlanType) selection;
                                            pdbControl.sendPlan(sel);
                                        }
                                    }
                                });

                        if (debugOn) {
                            popupMenu.add("debug").addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    if (selection != null) {
                                        PlanType sel = (PlanType) selection;
                                        IMCMessage msg = sel.asIMCPlan();
                                        String str = "Plan->" + sel.getId() + " " + sel.toStringWithVehicles(true);
                                        str += msg.asJSON();
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        try {
                                            IMCDefinition.getInstance().serialize(msg, new IMCOutputStream(baos));
                                            str += ByteUtil.dumpAsHexToString(baos.toByteArray());
                                        }
                                        catch (Exception e1) {
                                            e1.printStackTrace();
                                        }
                                        System.out.println(str);
                                    }
                                }
                            });
                        }

                        if (syncState == State.SYNC || syncState == State.NOT_SYNC) {
                            popupMenu.add(
                                    I18n.textf("Remove '%planName' from %system", selection, getConsole()
                                            .getMainSystem())).addActionListener(new ActionListener() {
                                                @Override
                                                public void actionPerformed(ActionEvent e) {
                                                    if (selection != null) {
                                                        PlanType sel = (PlanType) selection;
                                                        pdbControl.deletePlan(sel.getId());
                                                    }
                                                }
                                            });

                            if (syncState == State.NOT_SYNC || (debugOn ? true : false)) {
                                popupMenu.add(
                                        I18n.textf("Get '%planName' from %system", selection, getConsole()
                                                .getMainSystem())).addActionListener(new ActionListener() {
                                                    @Override
                                                    public void actionPerformed(ActionEvent e) {
                                                        if (selection != null) {
                                                            PlanType sel = (PlanType) selection;
                                                            pdbControl.requestPlan(sel.getId());
                                                        }
                                                    }
                                                });
                            }

                        }
                        if (debugOn) {
                            popupMenu.add("Test '" + selection + "' from " + getConsole().getMainSystem())
                            .addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    if (selection != null) {
                                        PlanType sel = (PlanType) selection;
                                        IMCMessage pm1 = sel.asIMCPlan();
                                        PlanType p2 = IMCUtils.parsePlanSpecification(new MissionType(), pm1);
                                        IMCMessage pm2 = p2.asIMCPlan();
                                        System.out.println(".....");
                                        System.out.println(ByteUtil.encodeAsString(pm1.payloadMD5()));
                                        System.out.println(ByteUtil.encodeAsString(pm2.payloadMD5()));
                                        System.out.println(IMCUtil.getAsHtml(pm1));
                                        System.out.println(IMCUtil.getAsHtml(pm2));
                                    }
                                }
                            });
                        }

                        dissemination.add(I18n.textf("Share '%planName'", selection)).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                //disseminate((XmlOutputMethods) selection, "Plan");
                                getConsole().getImcMsgManager().broadcastToCCUs(((PlanType) selection).asIMCPlan());
                            }
                        });

                        popupMenu.add(I18n.text("Change plan vehicles")).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (selection != null) {
                                    PlanType sel = (PlanType) selection;

                                    String[] vehicles = VehicleSelectionDialog.showSelectionDialog(getConsole(), sel
                                            .getVehicles().toArray(new VehicleType[0]));
                                    Vector<VehicleType> vts = new Vector<VehicleType>();
                                    for (String v : vehicles) {
                                        vts.add(VehiclesHolder.getVehicleById(v));
                                    }
                                    sel.setVehicles(vts);
                                    repaint();
                                }
                            }
                        });
                    }
                }
                else if (selection instanceof PlanDBInfo) {

                    if (((DefaultMutableTreeNode) selectionNode.getParent()).getUserObject().toString()
                            .equalsIgnoreCase(browser.getPlansNodeName())) {
                        State syncState = selectionNode instanceof ExtendedTreeNode ? (State) ((ExtendedTreeNode) selectionNode)
                                .getUserInfo().get("sync") : null;
                                if (syncState == null)
                                    syncState = State.LOCAL;

                                if (syncState == State.REMOTE) {
                                    popupMenu.add(I18n.textf("Get '%planName' from %system", selection, getConsole().getMainSystem()))
                                    .addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            if (selection != null) {
                                                PlanDBInfo sel = (PlanDBInfo) selection;
                                                pdbControl.requestPlan(sel.getPlanId());
                                            }
                                        }
                                    });

                                    popupMenu.add(I18n.textf("Remove '%planName' from %system", selection, getConsole().getMainSystem()))
                                    .addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            if (selection != null) {
                                                PlanDBInfo sel = (PlanDBInfo) selection;
                                                pdbControl.requestPlan(sel.getPlanId());
                                            }
                                        }
                                    });
                                }
                    }

                    // }
                }
                else if (selection instanceof TransponderElement) {

                    popupMenu.addSeparator();

                    popupMenu.add(I18n.textf("View/Edit '%transponderName'", selection)).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            editTransponder((TransponderElement) selection);
                        }
                    });

                    popupMenu.add(I18n.textf("Remove '%transponderName'", selection)).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeTransponder((TransponderElement) selection);
                        }
                    });

                    Vector<TransponderElement> allTransponderElements = MapGroup.getMapGroupInstance(
                            getConsole().getMission()).getAllObjectsOfType(TransponderElement.class);
                    for (final AbstractElement tel : allTransponderElements) {
                        if ((TransponderElement) selection != (TransponderElement) tel) {
                            popupMenu.add(I18n.textf("Switch '%transponderName1' with '%transponderName2'", selection, tel)).addActionListener(
                                    new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            new Thread() {
                                                @Override
                                                public void run() {
                                                    swithLocationsTransponder((TransponderElement) selection,
                                                            (TransponderElement) tel);
                                                };
                                            }.start();
                                        }
                                    });
                        }
                    }

                    dissemination.add(I18n.textf("Share '%transponderName'", selection)).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            disseminate((XmlOutputMethods) selection, "Transponder");
                        }
                    });

                    popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addTransponder();
                        }
                    });

                }

                else if (selection instanceof MarkElement) {

                    popupMenu.addSeparator();

                    dissemination.add(I18n.text("Share startup position")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            disseminate((XmlOutputMethods) selection, "StartLocation");
                        }
                    });

                    popupMenu.add(I18n.text("Set optional navigation startup position")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setStartupPos();
                        }
                    });
                }
                else if (selection instanceof HomeReference) {
                    popupMenu.add(I18n.text("View/Edit home reference")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            LocationType loc = new LocationType((HomeReference) selection);
                            LocationType after = LocationPanel.showLocationDialog(getConsole(), I18n.text("Set home reference"),
                                    loc, getConsole().getMission(), true);
                            if (after == null)
                                return;

                            getConsole().getMission().getHomeRef().setLocation(after);

                            Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(
                                    getConsole().getMission()).getAllObjectsOfType(HomeReferenceElement.class);
                            hrefElems.get(0).setCoordinateSystem(getConsole().getMission().getHomeRef());
                            getConsole().getMission().save(false);
                            getConsole().updateMissionListeners();
                        }
                    });
                }

                if (plansCount > 1) {
                    popupMenu.addSeparator();

                    popupMenu.add(I18n.text("Remove selected plans")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (selection != null) {
                                int resp = JOptionPane.showConfirmDialog(getConsole(),
                                        I18n.textf("Remove all selected plans (%numberOfPlans)?", multiSel.length));

                                if (resp == JOptionPane.YES_OPTION) {
                                    for (Object o : multiSel) {
                                        PlanType sel = (PlanType) o;
                                        getConsole().getMission().getIndividualPlansList().remove(sel.getId());
                                    }
                                    getConsole().getMission().save(false);

                                    if (getConsole() != null)
                                        getConsole().setPlan(null);
                                    refreshBrowser();
                                }
                            }
                        }
                    });
                }

                popupMenu.addSeparator();
                popupMenu.add(I18n.text("Reload Panel")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refreshBrowser();
                    }
                });

                popupMenu.add(dissemination);

                popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
            }
        });

        planControlUpdate(getMainVehicleId());
    }

    @Override
    public void cleanSubPanel() {
        removePlanDBListener();
    }

    protected void addTransponder() {
        if (getConsole() == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
                    I18n.text("Unable to find a parent console"));
            return;
        }

        if (getConsole().getMission() == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
                    I18n.text("No mission opened in the console"));
            return;
        }

        MissionType mt = getConsole().getMission();
        MapType pivot;
        Vector<TransponderElement> ts = MapGroup.getMapGroupInstance(mt).getAllObjectsOfType(TransponderElement.class);
        if (ts.size() > 0) {
            pivot = ts.firstElement().getParentMap();
        }
        else {
            if (mt.getMapsList().size() > 0)
                pivot = mt.getMapsList().values().iterator().next().getMap();
            else {
                MapType map = new MapType(new LocationType(mt.getHomeRef()));
                MapMission mm = new MapMission();
                mm.setMap(map);
                mt.addMap(mm);
                MapGroup.getMapGroupInstance(mt).addMap(map);
                pivot = map;
            }
        }

        TransponderElement te = new TransponderElement(MapGroup.getMapGroupInstance(mt), pivot);
        te = SimpleTransponderPanel.showTransponderDialog(te, I18n.text("New transponder properties"), true, true, 
                pivot.getObjectNames(), MissionTreePanel.this);
        if (te != null) {
            te.getParentMap().addObject(te);
            te.getParentMap().saveFile(te.getParentMap().getHref());
            if (getConsole() != null && getConsole().getMission() != null
                    && getConsole().getMission().getCompressedFilePath() != null) {
                getConsole().getMission().save(false);
            }
            refreshBrowser();
            disseminate(te, "Transponder");
        }
    }

    protected void setStartupPos() {
        MarkElement startEl = getStartPos();

        LocationType before = new LocationType();
        if (startEl != null)
            before.setLocation(startEl.getCenterLocation());
        else
            before.setLocation(getConsole().getMission().getHomeRef());

        LocationType after = LocationPanel.showLocationDialog(getConsole(), I18n.text("Set navigation startup position"), 
                before, getConsole().getMission(), true);
        if (after == null)
            return;

        MapType pivot = null;

        if (startEl != null) {
            startEl.setCenterLocation(after);
            pivot = startEl.getParentMap();
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setMapGroup(pivot.getMapGroup());
            mce.setChangedObject(startEl);
            pivot.warnChangeListeners(mce);
        }
        else {
            MissionType mt = getConsole().getMission();

            if (mt.getMapsList().size() > 0)
                pivot = mt.getMapsList().values().iterator().next().getMap();
            else {
                GuiUtils.errorMessage(getConsole(), I18n.text("Set navigation startup position"),
                        I18n.text("There is no map in the active mission"));
                return;
            }

            startEl = new MarkElement(pivot.getMapGroup(), pivot);
            startEl.setId("start");
            startEl.setName("start");
            startEl.setCenterLocation(after);
            pivot.addObject(startEl);
        }

        pivot.saveFile(pivot.getHref());

        if (getConsole() != null && getConsole().getMission() != null
                && getConsole().getMission().getCompressedFilePath() != null) {
            getConsole().getMission().save(false);
        }
        refreshBrowser();
    }

    private void disseminate(XmlOutputMethods object, String rootElementName) {
        IMCMessage msg = IMCDefinition.getInstance().create("mission_chunk", "xml_data", object.asXML(rootElementName));
        disseminateToCCUs(msg);
    }

    private void disseminateToCCUs(IMCMessage msg) {
        if (msg == null)
            return;

        ImcSystem[] systems = ImcSystemsHolder.lookupActiveSystemCCUs();
        for (ImcSystem s : systems) {
            System.out.println("sending msg '" + msg.getAbbrev() + "' to '" + s.getName() + "'...");
            ImcMsgManager.getManager().sendMessage(msg, s.getId(), null);
        }
    }

    @Override
    public void missionReplaced(MissionType mission) {
        refreshBrowser();
    }

    @Override
    public void missionUpdated(MissionType mission) {
        refreshBrowser();
    }

    boolean inited = false;

    @Override
    public void initSubPanel() {
        if (inited)
            return;
        inited = true;
        // pdbControl.setConsole(getConsole());
        planControlUpdate(getMainVehicleId());

        refreshBrowser();

        addMenuItem(I18n.text("Advanced") + ">" + I18n.text("Clear remote PlanDB for main system"),
                new ImageIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pdbControl != null)
                    pdbControl.clearDatabase();
            }
        });
    }

    protected MarkElement getStartPos() {
        MarkElement startEl = null;
        Vector<MarkElement> marks = MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(
                MarkElement.class);

        for (MarkElement el : marks) {
            if (el.getId().equals("start")) {
                startEl = el;
                break;
            }
        }

        return startEl;
    }

    protected void refreshBrowser() {
        browser.clearTree();

        if (getConsole().getMission() == null)
            return;

        browser.setHomeRef(getConsole().getMission().getHomeRef());
        if (useStartPoint)
            browser.setStartPos(getStartPos());

        Vector<TransponderElement> trans = MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(
                TransponderElement.class);

        // ImcSystem[] imcSystems = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        // LBLRanges lbltimer;
        for (TransponderElement elem : trans) {
            // lbltimer = null;
            // if (imcSystems.length > 0) {
            // if (!imcSystems[0].containsData(elem.getName())) {
            // lbltimer = new LBLRanges();
            // imcSystems[0].storeData(elem.getName(), lbltimer);
            // }
            // }
            browser.addTransponder(elem);

        }
        for (PlanType plan : getConsole().getMission().getIndividualPlansList().values()) {
            browser.addPlan(plan);
        }

        if (getConsole().getPlan() != null)
            browser.setSelectedPlan(getConsole().getPlan());

        browser.revalidate();
    }

    private void editTransponder(TransponderElement elem) {
        TransponderElement res = SimpleTransponderPanel.showTransponderDialog(elem, I18n.text("Transponder properties"), true,
                true, elem.getParentMap().getObjectNames(), MissionTreePanel.this);

        if (res != null) {
            MapType pivot = elem.getParentMap();
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setMapGroup(MapGroup.getMapGroupInstance(getConsole().getMission()));
            mce.setChangedObject(elem);
            pivot.warnChangeListeners(mce);

            LinkedHashMap<String, MapMission> mapsList = getConsole().getMission().getMapsList();
            MapMission mm = mapsList.get(pivot.getId());
            mm.setMap(pivot);
            pivot.saveFile(mm.getHref());

            if (getConsole() != null && getConsole().getMission() != null
                    && getConsole().getMission().getCompressedFilePath() != null) {
                getConsole().getMission().save(false);
                getConsole().updateMissionListeners();
            }

            ExtendedTreeNode selectionNode = (ExtendedTreeNode) browser.getSelectedTreeNode();
            HashMap<String, Object> userInfo = selectionNode.getUserInfo();
            userInfo.put("sync", State.NOT_SYNC);
            // TODO
        }
    }

    private void removeTransponder(TransponderElement elem) {
        int ret = JOptionPane.showConfirmDialog(this, I18n.textf("Delete '%transponderName'?", elem.getId()), I18n.text("Delete"),
                JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            elem.getParentMap().remove(elem.getId());
            elem.getParentMap().warnChangeListeners(new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED));
            elem.getParentMap().saveFile(elem.getParentMap().getHref());

            if (getConsole().getMission() != null) {
                if (getConsole().getMission().getCompressedFilePath() != null)
                    getConsole().getMission().save(false);
                getConsole().updateMissionListeners();
            }

            refreshBrowser();
        }
    }

    private void swithLocationsTransponder(TransponderElement tel1, TransponderElement tel2) {
        LocationType loc1 = tel1.getCenterLocation();
        LocationType loc2 = tel2.getCenterLocation();
        tel1.setCenterLocation(loc2);
        tel2.setCenterLocation(loc1);

        MapType pivot = tel1.getParentMap();
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(pivot);
        mce.setMapGroup(MapGroup.getMapGroupInstance(getConsole().getMission()));
        mce.setChangedObject(tel1);
        pivot.warnChangeListeners(mce);

        MapType pivot2 = tel2.getParentMap();
        MapChangeEvent mce2 = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce2.setSourceMap(pivot2);
        mce2.setMapGroup(MapGroup.getMapGroupInstance(getConsole().getMission()));
        mce2.setChangedObject(tel2);
        pivot2.warnChangeListeners(mce2);

        LinkedHashMap<String, MapMission> mapsList = getConsole().getMission().getMapsList();
        MapMission mm = mapsList.get(pivot.getId());
        mm.setMap(pivot);
        pivot.saveFile(mm.getHref());
        mm = mapsList.get(pivot2.getId());
        mm.setMap(pivot2);
        pivot2.saveFile(mm.getHref());

        if (getConsole() != null && getConsole().getMission() != null
                && getConsole().getMission().getCompressedFilePath() != null) {
            getConsole().getMission().save(false);
            getConsole().updateMissionListeners();
        }

    }

    @Override
    public void PlanChange(PlanType plan) {
        browser.setSelectedPlan(plan);
    }

    // private void editHomeRef(HomeReference homeref) {
    // LocationType res = SimpleLocationPanel.showLocationDialog(
    // new LocationType(homeref), "Home reference location", false);
    // if (res != null) {
    // CoordinateSystem cs = new CoordinateSystem();
    // cs.setLocation(res);
    // getConsole().getMission().setHomeRef(cs);
    // getConsole().getMission().save(false);
    // MapGroup.setMissionInstance(getConsole().getMission().getId(),
    // getConsole().getMission().generateMapGroup());
    // }
    // }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    public boolean setContent(Transferable tr) {
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].isMimeTypeEqual("text/plain; class=java.lang.String; charset=Unicode")) {
                String url = null;

                try {
                    Object data = tr.getTransferData(flavors[i]);
                    if (data instanceof InputStreamReader) {
                        BufferedReader reader = new BufferedReader((InputStreamReader) data);
                        url = reader.readLine();
                        reader.close();
                    }
                    else if (data instanceof String) {
                        url = data.toString();
                    }

                    return parseURL(url);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
        return false;
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                // System.out.println("Possible flavor: " + flavors[i].getMimeType());
                if (flavors[i].isMimeTypeEqual("text/plain; class=java.lang.String; charset=Unicode")) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    String url = null;

                    Object data = tr.getTransferData(flavors[i]);
                    if (data instanceof InputStreamReader) {
                        BufferedReader reader = new BufferedReader((InputStreamReader) data);
                        url = reader.readLine();
                        reader.close();
                        dtde.dropComplete(true);

                    }
                    else if (data instanceof String) {
                        url = data.toString();
                    }
                    else
                        dtde.rejectDrop();

                    if (parseURL(url))
                        dtde.dropComplete(true);
                    else
                        dtde.rejectDrop();

                    return;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        dtde.rejectDrop();
    }

    public boolean parseURL(String url) {
        System.out.println("parsing " + url);
        if (url == null || getConsole().getMission() == null)
            return false;

        if (url.startsWith("http")) {
            String xml = HTTPUtils.get(url);
            return parseContents(xml);
        }
        return false;
    }

    public boolean parseContents(String file) {

        MissionType mission = getConsole().getMission();

        try {
            Document doc = DocumentHelper.parseText(file);
            String root = doc.getRootElement().getName();
            if (root.equalsIgnoreCase("home-reference")) {
                HomeReference homeRef = new HomeReference();
                boolean loadOk = homeRef.load(file);
                if (loadOk) {
                    mission.getHomeRef().setCoordinateSystem(homeRef);
                    Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(getConsole().getMission())
                            .getAllObjectsOfType(HomeReferenceElement.class);
                    hrefElems.get(0).setCoordinateSystem(getConsole().getMission().getHomeRef());
                    getConsole().getMission().save(false);
                    getConsole().updateMissionListeners();
                }
            }
            else if (root.equalsIgnoreCase("StartLocation")) {
                MarkElement start = new MarkElement(file);
                AbstractElement[] startLocs = MapGroup.getMapGroupInstance(mission).getMapObjectsByID("start");
                MapType pivot = null;

                if (startLocs.length == 1 && startLocs[0] instanceof MarkElement) {
                    ((MarkElement) startLocs[0]).setCenterLocation(start.getCenterLocation());
                    pivot = startLocs[0].getParentMap();
                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                    mce.setSourceMap(pivot);
                    mce.setMapGroup(pivot.getMapGroup());
                    mce.setChangedObject(startLocs[0]);
                    pivot.warnChangeListeners(mce);
                }
                else if (startLocs.length == 0) {
                    try {
                        pivot = mission.getMapsList().values().iterator().next().getMap();
                        start.setId("start");
                        start.setName("start");
                        start.setParentMap(pivot);
                        start.setMapGroup(pivot.getMapGroup());
                        pivot.addObject(start);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
                if (pivot != null) {
                    pivot.saveFile(pivot.getHref());
                    if (getConsole() != null && getConsole().getMission() != null
                            && getConsole().getMission().getCompressedFilePath() != null) {
                        getConsole().getMission().save(false);
                    }
                }
                getConsole().updateMissionListeners();
            }
            else if (root.equalsIgnoreCase("Transponder")) {
                TransponderElement transponder = new TransponderElement(file);
                AbstractElement[] sameId = MapGroup.getMapGroupInstance(mission).getMapObjectsByID(transponder.getId());
                MapType pivot = null;

                if (sameId.length == 1 && sameId[0] instanceof TransponderElement) {
                    ((TransponderElement) sameId[0]).setCenterLocation(transponder.getCenterLocation());
                    ((TransponderElement) sameId[0]).setBuoyAttached(transponder.isBuoyAttached());
                    ((TransponderElement) sameId[0]).setConfiguration(transponder.getConfiguration());

                    pivot = sameId[0].getParentMap();
                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                    mce.setSourceMap(pivot);
                    mce.setMapGroup(pivot.getMapGroup());
                    mce.setChangedObject(sameId[0]);
                    pivot.warnChangeListeners(mce);
                }
                else if (sameId.length == 0) {
                    try {
                        pivot = mission.getMapsList().values().iterator().next().getMap();
                        transponder.setParentMap(pivot);
                        transponder.setMapGroup(pivot.getMapGroup());
                        pivot.addObject(transponder);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
                if (pivot != null) {
                    pivot.saveFile(pivot.getHref());
                    if (getConsole() != null && getConsole().getMission() != null
                            && getConsole().getMission().getCompressedFilePath() != null) {
                        getConsole().getMission().save(false);
                    }
                }
                getConsole().updateMissionListeners();
            }
            else if (root.equalsIgnoreCase("Plan") && getConsole().getMission() != null) {
                PlanType plan = new PlanType(file, getConsole().getMission());

                getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
                getConsole().updateMissionListeners();

                if (getConsole() != null && getConsole().getMission() != null
                        && getConsole().getMission().getCompressedFilePath() != null) {
                    getConsole().getMission().save(false);
                }
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return true;
    }

    @Override
    public void mapChanged(MapChangeEvent mapChange) {
        refreshBrowser();
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public String[] getObservedMessages() {
        String[] messages = new String[4];
        messages[0] = "LblRangeAcceptance";
        messages[2] = "PlanDB";
        messages[3] = "EntityState";
        if (IMCDefinition.getInstance().getMessageId("PlanSpecification") != -1) {
            messages[1] = "PlanSpecification";
        }
        else {
            messages[1] = "MissionSpecification";
        }
        return messages;
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        planControlUpdate(id);
    }

    /**
     * @param id
     */
    private void planControlUpdate(String id) {
        removePlanDBListener();
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);
        if (sys == null) {
            pdbControl = new PlanDBControl();
            pdbControl.setRemoteSystemId(id);
        }
        else
            pdbControl = sys.getPlanDBControl();
        addPlanDBListener();
    }

    private void addPlanDBListener() {
        pdbControl.addListener(planDBListener);
    }

    private void removePlanDBListener() {
        pdbControl.removeListener(planDBListener);
    }

    @Override
    public Vector<PlanType> getSelectedPlans() {
        final Object[] multiSel = browser.getSelectedItems();
        Vector<PlanType> plans = new Vector<PlanType>();
        if (multiSel != null) {
            for (Object o : multiSel) {
                if (o instanceof PlanType)
                    plans.add((PlanType) o);
            }
        }

        if (plans.isEmpty() && getConsole().getPlan() != null)
            plans.add(getConsole().getPlan());

        return plans;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.console.plugins.ITransponderSelection#getSelectedTransponders()
     */
    @Override
    public Collection<TransponderElement> getSelectedTransponders() {
        final Object[] multiSel = browser.getSelectedItems();
        ArrayList<TransponderElement> trans = new ArrayList<>();
        if (multiSel != null) {
            for (Object o : multiSel) {
                if (o instanceof TransponderElement)
                    trans.add((TransponderElement) o);
            }
        }
        return trans;
    }

    @Override
    public void messageArrived(IMCMessage message) {
        String abbrev = message.getAbbrev();
        pdbControl.onMessage(null, message);
        if (message.getMgid() == PlanSpecification.ID_STATIC) {
            PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), message);
            if (getConsole().getMission().getIndividualPlansList().containsKey(plan.getId())) {
            }
            else {
                getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
                getConsole().updateMissionListeners();
                getConsole().getMission().save(true);
            }
        }
        else if (abbrev.equals("LblRangeAcceptance")) {
            LblRangeAcceptance acceptance;
            try {
                acceptance = LblRangeAcceptance.clone(message);
                int id = acceptance.getId();
                double range = acceptance.getRange();
                long timeStampMillis = acceptance.getTimestampMillis();
                if (acceptance.getAcceptance() == ACCEPTANCE.AT_SURFACE) { // clean up when at surface
                    range = -1;
                    browser.updateTransponderRange((short) id, -1, timeStampMillis, getMainVehicleId());
                }
                else {
                    browser.updateTransponderRange((short) id, range, timeStampMillis, getMainVehicleId());
                }
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        // else if (abbrev.equals("LblRange")) {
        // int id = message.getInteger("id");
        // double range = message.getDouble("range");
        // long timeStampMillis = message.getTimestampMillis();
        // System.out.println("-->LblRange [" + id + "] " + range);
        // browser.updateTransponderRange((short) id, range, timeStampMillis, getMainVehicleId());
        // }
        // else if (abbrev.equals("LblRangeRejection")) {
        // int id = message.getInteger("id");
        // double range = message.getDouble("range");
        // long timeStampMillis = message.getTimestampMillis();
        // reason = I18n.text(message.getString("reason"));
        // if (reason.equals("AT_SURFACE")) { // clean up when at surface
        // range = -1;
        // browser.updateTransponderRange((short) id, -1, timeStampMillis, getMainVehicleId());
        // }
        // else {
        // browser.updateTransponderRange((short) id, range, timeStampMillis, getMainVehicleId());
        // }
        // System.out.println("-->LblRangeRejection [" + id + "] " + range);
        // }
    }

    @Override
    public long millisBetweenUpdates() {
        return 1500;
    }

    @Override
    public boolean update() {
        if (getMainVehicleId() == null || getMainVehicleId().length() == 0 || !usePlanDBSyncFeatures) {
            browser.updatePlansState(null);
            browser.rebuildTransponderNodes(null);
        }
        else {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
            if (sys == null) {
                browser.updatePlansState(null);
                browser.rebuildTransponderNodes(null);
            }
            else {
                browser.updatePlansState(sys);
                browser.rebuildTransponderNodes(sys);
            }
        }

        if (getConsole() != null) {
            Vector<ISystemsSelection> sys = getConsole().getSubPanelsOfInterface(ISystemsSelection.class);
            if (sys.size() != 0) {
                if (usePlanDBSyncFeaturesExt) {
                    ImcSystem[] imcSystemsArray = convertToImcSystemsArray(sys);
                    browser.updateRemotePlansState(imcSystemsArray);
                }
            }
        }
        return true;
    }

    private ImcSystem[] convertToImcSystemsArray(Vector<ISystemsSelection> sys) {
        Collection<String> asys = sys.firstElement().getAvailableSelectedSystems();
        Vector<ImcSystem> imcSystems = new Vector<ImcSystem>();
        for (String id : asys) {
            ImcSystem tsys = ImcSystemsHolder.lookupSystemByName(id);
            if (tsys != null) {
                if (!imcSystems.contains(tsys))
                    imcSystems.add(tsys);
            }
        }
        ImcSystem[] imcSystemsArray = imcSystems.toArray(new ImcSystem[imcSystems.size()]);
        return imcSystemsArray;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        browser.setDebugOn(debugOn);
    }
}
