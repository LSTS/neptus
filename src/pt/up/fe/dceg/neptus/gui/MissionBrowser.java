/*
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Author: José Pinto, Margarida Faria
 * 22/03/2005, 19/03/2013
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBControl;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.types.Identifiable;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.map.TransponderUtils;
import pt.up.fe.dceg.neptus.types.misc.LBLRangesTimer;
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
 * This is a visual class that displays the various items contained in a mission like maps, vehicles and plans...
 * 
 * @author Jose Pinto
 * @author Paulo Dias
 * @author Margarida Faria
 */
public class MissionBrowser extends JPanel implements PlanChangeListener {

    private static final long serialVersionUID = 1L;
    private final boolean debugOn = false;

    public enum State {
        SYNC("Sync"),
        NOT_SYNC("Unsync"),
        REMOTE("Remote"),
        LOCAL("Local");
        private final String fileName;

        private State(String name) {
            this.fileName = name;
        }

        /**
         * @return the name
         */
        public String getFileName() {
            return fileName;
        }
    };

    private final JTree elementTree;
    final private Model treeModel;
    private final Vector<ChangeListener> listeners = new Vector<ChangeListener>();

    /**
     * Creates a new mission browser which will display the items contained in the given mission type
     * 
     * @param mission The MissionType whose elements are to be displayed
     */
    public MissionBrowser() {
        ConfigFetch.mark("MissionBrowser");

        elementTree = new JTree();
        ConfigFetch.mark("MissionTreeCellRenderer");
        elementTree.setCellRenderer(new MissionTreeCellRenderer());
        elementTree.setRootVisible(true);
        elementTree.setShowsRootHandles(true);
        ConfigFetch.benchmark("MissionTreeCellRenderer");

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(elementTree), BorderLayout.CENTER);
        ConfigFetch.benchmark("MissionBrowser");

        treeModel = new Model(new DefaultMutableTreeNode("Mission Elements"));
        elementTree.setModel(treeModel);
    }

    public void setDebugOn(boolean value) {
        TreeCellRenderer cr = elementTree.getCellRenderer();
        if (cr instanceof MissionTreeCellRenderer)
            ((MissionTreeCellRenderer) cr).debugOn = value;
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public Object[] getSelectedItems() {
        TreePath[] selectionPaths = elementTree.getSelectionPaths();
        if (selectionPaths == null)
            return null;

        Vector<Object> sel = new Vector<Object>();
        for (TreePath path : selectionPaths) {
            sel.add(((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
        }

        return sel.toArray();
    }

    public DefaultMutableTreeNode getSelectedTreeNode() {
        if (elementTree.getSelectionPath() == null)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath().getLastPathComponent();

        return node;
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public Object getSelectedItem() {
        if (elementTree.getSelectionPath() == null)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath().getLastPathComponent();

        return node.getUserObject();
    }

    public void setupListeners(final ConsoleLayout console2, final PlanDBControl pdbControl) {
        elementTree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.isAddedPath()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath()
                            .getLastPathComponent();
                    if (node.getUserObject() instanceof PlanType) {
                        PlanType selectedPlan = (PlanType) node.getUserObject();
                        if (console2 != null)
                            console2.setPlan(selectedPlan);
                    }
                    else if (console2 != null) {
                        console2.setPlan(null);
                    }
                }
            }
        });

        elementTree.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {

                final Object[] multiSel = getSelectedItems();
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

                    TreePath path = elementTree.getPathForLocation(e.getX(), e.getY());
                    elementTree.setSelectionPath(path);
                }

                final Object selection = getSelectedItem();
                DefaultMutableTreeNode selectionNode = getSelectedTreeNode();

                JPopupMenu popupMenu = new JPopupMenu();
                JMenu dissemination = new JMenu(I18n.text("Dissemination"));

                if (selection == null) {
                    popupMenu.addSeparator();
                    popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addTransponder(console2);
                        }
                    });
                }

                if (Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null) != null) {
                    dissemination.add(I18n.text("Paste URL")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            if (setContent(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null),
                                    console2.getMission())) {
                                console2.updateMissionListeners();
                            }
                        }
                    });
                    dissemination.addSeparator();
                }

                if (selection instanceof PlanType) {
                    if (plansCount == 1) {
                        popupMenu.addSeparator();

                        popupMenu.add(I18n.textf("Remove '%planName'", selection)).addActionListener(
                                new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        if (selection != null) {
                                            int resp = JOptionPane.showConfirmDialog(console2,
                                                    I18n.textf("Remove the plan '%planName'?", selection.toString()));
                                            if (resp == JOptionPane.YES_OPTION) {
                                                PlanType sel = (PlanType) selection;
                                                console2.getMission().getIndividualPlansList().remove(sel.getId());
                                                console2.getMission().save(false);

                                                if (console2 != null)
                                                    console2.setPlan(null);
                                                refreshBrowser(console2.getPlan(), console2.getMission());
                                            }
                                        }
                                    }
                                });

                        State syncState = (State) ((ExtendedTreeNode) selectionNode).getUserInfo().get("sync");
                        if (syncState == null)
                            syncState = State.LOCAL;

                        popupMenu.add(I18n.textf("Send '%planName' to %system", selection, console2.getMainSystem()))
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
                                    I18n.textf("Remove '%planName' from %system", selection, console2.getMainSystem()))
                                    .addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            if (selection != null) {
                                                PlanType sel = (PlanType) selection;
                                                pdbControl.deletePlan(sel.getId());
                                            }
                                        }
                                    });

                            if (syncState == State.NOT_SYNC || (debugOn ? true : false)) {
                                popupMenu
                                        .add(I18n.textf("Get '%planName' from %system", selection,
                                                console2.getMainSystem())).addActionListener(new ActionListener() {
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
                            popupMenu.add("Test '" + selection + "' from " + console2.getMainSystem())
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

                        dissemination.add(I18n.textf("Share '%planName'", selection)).addActionListener(
                                new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        // disseminate((XmlOutputMethods) selection, "Plan");
                                        console2.getImcMsgManager().broadcastToCCUs(((PlanType) selection).asIMCPlan());
                                    }
                                });

                        popupMenu.add(I18n.text("Change plan vehicles")).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (selection != null) {
                                    PlanType sel = (PlanType) selection;

                                    String[] vehicles = VehicleSelectionDialog.showSelectionDialog(console2, sel
                                            .getVehicles().toArray(new VehicleType[0]));
                                    Vector<VehicleType> vts = new Vector<VehicleType>();
                                    for (String v : vehicles) {
                                        vts.add(VehiclesHolder.getVehicleById(v));
                                    }
                                    sel.setVehicles(vts);
                                }
                            }
                        });
                    }
                }
                else if (selection instanceof PlanDBInfo) {
                    State syncState = selectionNode instanceof ExtendedTreeNode ? (State) ((ExtendedTreeNode) selectionNode)
                            .getUserInfo().get("sync") : null;
                    if (syncState == null)
                        syncState = State.LOCAL;

                    else if (syncState == State.REMOTE) {
                        popupMenu.add(I18n.textf("Get '%planName' from %system", selection, console2.getMainSystem()))
                                .addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        if (selection != null) {
                                            PlanDBInfo sel = (PlanDBInfo) selection;
                                            pdbControl.requestPlan(sel.getPlanId());
                                        }
                                    }
                                });

                        popupMenu.add(
                                I18n.textf("Remove '%planName' from %system", selection, console2.getMainSystem()))
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
                else if (selection instanceof TransponderElement) {

                    popupMenu.addSeparator();

                    popupMenu.add(I18n.textf("View/Edit '%transponderName'", selection)).addActionListener(
                            new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    editTransponder((TransponderElement) selection, console2.getMission());
                                }
                            });

                    popupMenu.add(I18n.textf("Remove '%transponderName'", selection)).addActionListener(
                            new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    removeTransponder((TransponderElement) selection, console2);
                                }
                            });

                    Vector<TransponderElement> allTransponderElements = MapGroup.getMapGroupInstance(
                            console2.getMission()).getAllObjectsOfType(TransponderElement.class);
                    for (final AbstractElement tel : allTransponderElements) {
                        if ((TransponderElement) selection != (TransponderElement) tel) {
                            popupMenu.add(
                                    I18n.textf("Switch '%transponderName1' with '%transponderName2'", selection, tel))
                                    .addActionListener(new ActionListener() {
                                        @Override
                                        public void actionPerformed(ActionEvent e) {
                                            new Thread() {
                                                @Override
                                                public void run() {
                                                    swithLocationsTransponder((TransponderElement) selection,
                                                            (TransponderElement) tel, console2);
                                                };
                                            }.start();
                                        }
                                    });
                        }
                    }

                    dissemination.add(I18n.textf("Share '%transponderName'", selection)).addActionListener(
                            new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    ImcMsgManager.disseminate((XmlOutputMethods) selection, "Transponder");
                                }
                            });

                    popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            addTransponder(console2);
                        }
                    });

                }

                else if (selection instanceof MarkElement) {

                    popupMenu.addSeparator();

                    dissemination.add(I18n.text("Share startup position")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ImcMsgManager.disseminate((XmlOutputMethods) selection, "StartLocation");
                        }
                    });
                }
                else if (selection instanceof HomeReference) {
                    popupMenu.add(I18n.text("View/Edit home reference")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            LocationType loc = new LocationType((HomeReference) selection);
                            LocationType after = LocationPanel.showLocationDialog(console2,
                                    I18n.text("Set home reference"), loc, console2.getMission(), true);
                            if (after == null)
                                return;

                            console2.getMission().getHomeRef().setLocation(after);

                            Vector<HomeReferenceElement> hrefElems = MapGroup
                                    .getMapGroupInstance(console2.getMission()).getAllObjectsOfType(
                                            HomeReferenceElement.class);
                            hrefElems.get(0).setCoordinateSystem(console2.getMission().getHomeRef());
                            console2.getMission().save(false);
                            console2.updateMissionListeners();
                        }
                    });
                }

                if (plansCount > 1) {
                    popupMenu.addSeparator();

                    popupMenu.add(I18n.text("Remove selected plans")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (selection != null) {
                                int resp = JOptionPane.showConfirmDialog(console2,
                                        I18n.textf("Remove all selected plans (%numberOfPlans)?", multiSel.length));

                                if (resp == JOptionPane.YES_OPTION) {
                                    for (Object o : multiSel) {
                                        PlanType sel = (PlanType) o;
                                        console2.getMission().getIndividualPlansList().remove(sel.getId());
                                    }
                                    console2.getMission().save(false);

                                    if (console2 != null)
                                        console2.setPlan(null);
                                    refreshBrowser(console2.getPlan(), console2.getMission());
                                }
                            }
                        }
                    });
                }

                popupMenu.addSeparator();
                popupMenu.add(I18n.text("Reload Panel")).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refreshBrowser(console2.getPlan(), console2.getMission());
                    }
                });

                popupMenu.add(dissemination);

                popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
            }
        });
    }


    protected void addTransponder(ConsoleLayout console2) {
        if (console2 == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
                    I18n.text("Unable to find a parent console"));
            return;
        }

        if (console2.getMission() == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
                    I18n.text("No mission opened in the console"));
            return;
        }

        MissionType mt = console2.getMission();
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
                pivot.getObjectNames(), MissionBrowser.this);
        if (te != null) {
            te.getParentMap().addObject(te);
            te.getParentMap().saveFile(te.getParentMap().getHref());
            if (console2 != null && console2.getMission() != null
                    && console2.getMission().getCompressedFilePath() != null) {
                console2.getMission().save(false);
            }
            refreshBrowser(console2.getPlan(), console2.getMission());
            ImcMsgManager.disseminate(te, "Transponder");
        }
    }

    private void editTransponder(TransponderElement elem, MissionType mission) {
        TransponderElement res = SimpleTransponderPanel.showTransponderDialog(elem,
                I18n.text("Transponder properties"), true, true, elem.getParentMap().getObjectNames(),
                MissionBrowser.this);

        if (res != null) {
            MapType pivot = elem.getParentMap();
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setMapGroup(MapGroup.getMapGroupInstance(mission));
            mce.setChangedObject(elem);
            pivot.warnChangeListeners(mce);

            LinkedHashMap<String, MapMission> mapsList = mission.getMapsList();
            MapMission mm = mapsList.get(pivot.getId());
            mm.setMap(pivot);
            pivot.saveFile(mm.getHref());

            if (mission != null && mission.getCompressedFilePath() != null) {
                mission.save(false);
            }
        }
    }

    private void removeTransponder(TransponderElement elem, ConsoleLayout console2) {
        int ret = JOptionPane.showConfirmDialog(this, I18n.textf("Delete '%transponderName'?", elem.getId()),
                I18n.text("Delete"), JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            elem.getParentMap().remove(elem.getId());
            elem.getParentMap().warnChangeListeners(new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED));
            elem.getParentMap().saveFile(elem.getParentMap().getHref());

            if (console2.getMission() != null) {
                if (console2.getMission().getCompressedFilePath() != null)
                    console2.getMission().save(false);
                console2.updateMissionListeners();
            }

            refreshBrowser(console2.getPlan(), console2.getMission());
        }
    }

    private void swithLocationsTransponder(TransponderElement tel1, TransponderElement tel2, ConsoleLayout console2) {
        LocationType loc1 = tel1.getCenterLocation();
        LocationType loc2 = tel2.getCenterLocation();
        tel1.setCenterLocation(loc2);
        tel2.setCenterLocation(loc1);

        MapType pivot = tel1.getParentMap();
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(pivot);
        mce.setMapGroup(MapGroup.getMapGroupInstance(console2.getMission()));
        mce.setChangedObject(tel1);
        pivot.warnChangeListeners(mce);

        MapType pivot2 = tel2.getParentMap();
        MapChangeEvent mce2 = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce2.setSourceMap(pivot2);
        mce2.setMapGroup(MapGroup.getMapGroupInstance(console2.getMission()));
        mce2.setChangedObject(tel2);
        pivot2.warnChangeListeners(mce2);

        LinkedHashMap<String, MapMission> mapsList = console2.getMission().getMapsList();
        MapMission mm = mapsList.get(pivot.getId());
        mm.setMap(pivot);
        pivot.saveFile(mm.getHref());
        mm = mapsList.get(pivot2.getId());
        mm.setMap(pivot2);
        pivot2.saveFile(mm.getHref());

        if (console2 != null && console2.getMission() != null && console2.getMission().getCompressedFilePath() != null) {
            console2.getMission().save(false);
            console2.updateMissionListeners();
        }

    }

    public void refreshBrowser(final PlanType selectedPlan, final MissionType mission) {
        if (mission == null) {
            System.out.println("Empty mission");
            treeModel.clearTree();
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                HomeReference homeRef = mission.getHomeRef();
                Vector<TransponderElement> trans = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(
                        TransponderElement.class);
                Collection<PlanType> plans = mission.getIndividualPlansList().values();
                PlanType plan = selectedPlan;
                System.out.println("Refresh mission tree with:" + trans.size() + " transponders, " + plans.size()
                        + " plans and selected plan " + plan);
                treeModel.redoModel(trans, homeRef, plans, plan);

                // elementTree.expandPath(new TreePath(treeModel.trans));
                expandTree();
                // JTreeUtils.expandAll(elementTree);
                revalidate();
                repaint();
            }

        });
    }

    public void expandTree() {
        // elementTree.expandRow(0);
        expandParent((DefaultMutableTreeNode) treeModel.getRoot());
        expandParent(treeModel.trans);
        expandParent(treeModel.plans);
    }

    private void expandParent(DefaultMutableTreeNode parent) {
        if (parent.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) parent.getFirstChild();
            elementTree.makeVisible(new TreePath(firstChild.getPath()));
        }
    }
    @Override
    public void PlanChange(PlanType plan) {
        setSelectedPlan(plan);
    }

    /**
     * 
     * @param tr
     * @param mission
     * @return true if mission listeners should be updated
     */
    public boolean setContent(Transferable tr, MissionType mission) {
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

                    return parseURL(url, mission);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
        return false;
    }

    /**
     * 
     * @param url
     * @param mission
     * @return true if mission listeners should be updated
     */
    public boolean parseURL(String url, MissionType mission) {
        System.out.println("parsing " + url);
        if (url == null || mission == null)
            return false;

        if (url.startsWith("http")) {
            String xml = HTTPUtils.get(url);
            return parseContents(xml, mission);
        }
        return false;
    }

    /**
     * 
     * @param file
     * @param mission
     * @return true if mission listeners should be updated
     */
    public boolean parseContents(String file, MissionType mission) {

        try {
            Document doc = DocumentHelper.parseText(file);
            String root = doc.getRootElement().getName();
            if (root.equalsIgnoreCase("home-reference")) {
                HomeReference homeRef = new HomeReference();
                boolean loadOk = homeRef.load(file);
                if (loadOk) {
                    mission.getHomeRef().setCoordinateSystem(homeRef);
                    Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(
                            HomeReferenceElement.class);
                    hrefElems.get(0).setCoordinateSystem(mission.getHomeRef());
                    mission.save(false);
                    return true;
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
                    if (mission != null && mission.getCompressedFilePath() != null) {
                        mission.save(false);
                    }
                }
                return true;
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
                    if (mission != null && mission.getCompressedFilePath() != null) {
                        mission.save(false);
                    }
                }
                return true;
            }
            else if (root.equalsIgnoreCase("Plan") && mission != null) {
                PlanType plan = new PlanType(file, mission);

                mission.getIndividualPlansList().put(plan.getId(), plan);

                if (mission != null && mission.getCompressedFilePath() != null) {
                    mission.save(false);
                }
                return true;
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return false;
    }


    /**
     * Removes the given item from the mission browser
     * 
     * @param item The item to be removed from this component
     */
    public void removeItem(Object item) {
        boolean isChanged = false;
        if (item instanceof MapType) {
            isChanged = treeModel.removeById((MapType) item, treeModel.maps);
        }
        else if (item instanceof PlanType) {
            isChanged = treeModel.removeById((PlanType) item, treeModel.plans);
        }
        else {
            System.out.println("Missing support for " + item.getClass().getCanonicalName());
        }
        if (isChanged) {
            warnListeners();
        }
    }


    /**
     * Adds a new map item to this component
     * 
     * @param map The new map to add
     */
    public void addMap(MapType map) {
        // createMapsNode();
        // treeModel.insertNodeInto(new DefaultMutableTreeNode(map), maps, treeModel.getChildCount(maps));
        // JTreeUtils.expandAll(elementTree);

        treeModel.addToParents(new DefaultMutableTreeNode(map), ParentNodes.MAP);
    }

    /**
     * Adds a new plan to the component
     * 
     * @param plan A plan which will now be displayed in this component
     */
    public void addPlan(PlanType plan) {
        treeModel.addToParents(new ExtendedTreeNode(plan), ParentNodes.PLANS);

    }


    // /**
    // * Returns the JTree where all the items are displayed
    // *
    // * @return The JTree component that displays all the items of the mission
    // */
    // public JTree getElementTree() {
    // return elementTree;
    // }

    public Object getElementAt(int x, int y) {
        TreePath path = elementTree.getPathForLocation(x, y);
        if (path != null) {
            elementTree.setSelectionPath(path);
            return getSelectedItem();
        }
        return null;
    }

    public TreePath getPathForLocation(int x, int y) {
        return elementTree.getPathForLocation(x, y);
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public void warnListeners() {
        ChangeEvent ce = new ChangeEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            ChangeListener listener = listeners.get(i);
            listener.stateChanged(ce);
        }
    }

    public void setSelectedPlan(PlanType plan) {
        if (getSelectedItem() == plan) {
            return;
        }

        if (plan == null) {
            return;
        }

        DefaultMutableTreeNode plans = treeModel.trans;
        if (plans != null) {
            int numPlans = treeModel.getChildCount(plans);

            for (int i = 0; i < numPlans; i++) {
                DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeModel.getChild(plans, i);
                if (tmp.getUserObject() == plan) {
                    TreePath selPath = new TreePath(treeModel.getPathToRoot(tmp));
                    elementTree.setSelectionPath(selPath);
                    elementTree.scrollPathToVisible(selPath);
                    return;
                }
            }
        }
    }

    public TreePath[] getSelectedNodes() {
        return elementTree.getSelectionPaths();
    }

    public void setSelectedNodes(TreePath[] selectedNodes) {
        elementTree.setSelectionPaths(selectedNodes);
    }

    /**
     * @param imcSystem
     */
    public void updatePlansState(ImcSystem imcSystem) {
        try {
            if (imcSystem == null)
                return;

            PlanDBState prs = imcSystem.getPlanDBControl().getRemoteState();

            Vector<PlanType> plansLocal = new Vector<PlanType>();
            Vector<String> plansThatMatchLocal = new Vector<String>();
            Vector<ExtendedTreeNode> pathsToRemove = new Vector<ExtendedTreeNode>();

            DefaultMutableTreeNode plans = treeModel.plans;
            if (plans != null && plans.getChildCount() != 0) {
                ExtendedTreeNode childPlan = (ExtendedTreeNode) plans.getFirstChild();
                while (childPlan != null) {
                    if (childPlan.getUserObject() instanceof PlanType) {
                        try {
                            PlanType plan = (PlanType) childPlan.getUserObject();
                            plansLocal.add(plan);
                            boolean containsPlan = prs.getStoredPlans().containsKey(plan.getId());
                            if (!containsPlan) {
                                childPlan.getUserInfo().remove("sync");
                            }
                            else {
                                childPlan.getUserInfo().put("sync",
                                        prs.matchesRemotePlan(plan) ? State.SYNC : State.NOT_SYNC);
                                plansThatMatchLocal.add(plan.getId());
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else if (childPlan.getUserObject() instanceof PlanDBInfo) {
                        PlanDBInfo planDBInfo = (PlanDBInfo) childPlan.getUserObject();
                        if (!prs.getStoredPlans().values().contains(planDBInfo))
                            pathsToRemove.add(childPlan);

                    }
                    childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
                }

                for (ExtendedTreeNode extendedTreeNode : pathsToRemove) {
                    try {
                        treeModel.removeNodeFromParent(extendedTreeNode);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                pathsToRemove.clear();

                if (!plans.isLeaf()) {
                    childPlan = (ExtendedTreeNode) plans.getFirstChild();
                    while (childPlan != null) {
                        if (childPlan.getUserObject() instanceof PlanDBInfo) {
                            PlanDBInfo planDBInfo = (PlanDBInfo) childPlan.getUserObject();
                            boolean ct = false;
                            for (PlanType pl : plansLocal) {
                                if (pl.getId().equals(planDBInfo.getPlanId())) {
                                    pathsToRemove.add(childPlan);
                                    ct = true;
                                    break;
                                }
                            }
                            if (!ct)
                                plansThatMatchLocal.add(planDBInfo.getPlanId());
                        }

                        childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
                    }

                    for (ExtendedTreeNode extendedTreeNode : pathsToRemove) {
                        treeModel.removeNodeFromParent(extendedTreeNode);
                    }
                }
            }

            ArrayList<Identifiable> objectsToAdd = new ArrayList<Identifiable>();

            for (PlanDBInfo pdbi : prs.getStoredPlans().values()) {
                if (!plansThatMatchLocal.contains(pdbi.getPlanId())) {
                    objectsToAdd.add(pdbi);
                }
            }

            treeModel.addToParents(objectsToAdd, ParentNodes.PLANS, State.REMOTE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateRemotePlansState(ImcSystem[] imcSystems) {
        NeptusLog.pub().error(
                "The method updateRemotePlansState is desabled, please review code in "
                        + MissionBrowser.class.getCanonicalName());
        // TODO
        // try {
        // DefaultMutableTreeNode remotePlans = treeModel.remotePlans;
        // if (remotePlans != null && (imcSystems == null || imcSystems.length == 0)) {
        // treeModel.removeNodeFromParent(remotePlans);
        // remotePlans = null;
        // return;
        // }
        //
        // // Adding or update systems planDBInfos
        // for (ImcSystem imcSystem : imcSystems) {
        // PlanDBState prs = imcSystem.getPlanDBControl().getRemoteState();
        //
        // ExtendedTreeNode systemRemotePlansRoot = null;
        //
        // // Find if already in the tree
        // if (remotePlans.getChildCount() != 0) {
        // ExtendedTreeNode childPlan = (ExtendedTreeNode) remotePlans.getFirstChild();
        // while (childPlan != null) {
        // try {
        // String id = (String) childPlan.getUserObject();
        // if (imcSystem.getName().equalsIgnoreCase(id)) {
        // systemRemotePlansRoot = childPlan;
        // break;
        // }
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
        // }
        // }
        //
        // // If not in the tree create and add system remote plan holder
        // if (systemRemotePlansRoot == null) {
        // systemRemotePlansRoot = new ExtendedTreeNode(imcSystem.getName());
        // treeModel.addToParents(systemRemotePlansRoot, ParentNodes.REMOTE_PLANS);
        // }
        //
        // // So now let's update or create planInfos for system
        // String[] planNames = prs.getStoredPlans().keySet().toArray(new String[0]);
        // PlanDBInfo[] planInfos = prs.getStoredPlans().values().toArray(new PlanDBInfo[0]);
        // for (int i = 0; i < planNames.length; i++) {
        // // look for planInfo on tree
        // ExtendedTreeNode systemPlanInfoRoot = null;
        // if (systemRemotePlansRoot.getChildCount() != 0) {
        // ExtendedTreeNode childPlan = (ExtendedTreeNode) systemRemotePlansRoot.getFirstChild();
        // while (childPlan != null) {
        // try {
        // String id = ((PlanDBInfo) childPlan.getUserObject()).getPlanId();
        // if (planNames[i].equalsIgnoreCase(id)) {
        // systemPlanInfoRoot = childPlan;
        // break;
        // }
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
        // }
        // }
        //
        // // If not in the tree create and add
        // if (systemPlanInfoRoot != null) {
        // systemPlanInfoRoot.setUserObject(planInfos[i]);
        // }
        // else {
        // systemPlanInfoRoot = new ExtendedTreeNode(planInfos[i]);
        // treeModel.addToParents(systemPlanInfoRoot, ParentNodes.REMOTE_PLANS);
        // }
        //
        // // test if this remote plan is in this console plan list
        // systemPlanInfoRoot.getUserInfo().put("sync", testPlanDBInfoForEqualityInMission(planInfos[i]));
        // }
        //
        // // see if planDBInfo is for removal
        // if (systemRemotePlansRoot.getChildCount() != 0) {
        // ExtendedTreeNode childPlan = (ExtendedTreeNode) systemRemotePlansRoot.getFirstChild();
        // while (childPlan != null) {
        // try {
        // String id = ((PlanDBInfo) childPlan.getUserObject()).getPlanId();
        // for (String planId : planNames) {
        // if (planId.equalsIgnoreCase(id)) {
        // id = null;
        // break;
        // }
        // }
        // if (id != null) {
        // treeModel.removeNodeFromParent(childPlan);
        // }
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
        // }
        // }
        // }
        //
        // // If is to remove and remove it
        // if (remotePlans != null && remotePlans.getChildCount() != 0) {
        // ExtendedTreeNode childPlan = (ExtendedTreeNode) remotePlans.getFirstChild();
        // while (childPlan != null) {
        // try {
        // String id = (String) childPlan.getUserObject();
        // for (ImcSystem imcSystem : imcSystems) {
        // if (imcSystem.getName().equalsIgnoreCase(id)) {
        // id = null;
        // break;
        // }
        // }
        // if (id != null) {
        // treeModel.removeNodeFromParent(childPlan);
        // // System.out.println(id);
        // }
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
        // }
        // }
        // repaint();
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    /**
     * @param planDBInfo
     */
    private State testPlanDBInfoForEqualityInMission(PlanDBInfo planDBInfo) {
        DefaultMutableTreeNode plans = treeModel.plans;
        if (plans == null)
            return State.REMOTE;

        if (plans.getChildCount() == 0)
            return State.REMOTE;

        ExtendedTreeNode childPlan = (ExtendedTreeNode) plans.getFirstChild();
        while (childPlan != null) {
            try {
                if (childPlan.getUserObject() instanceof PlanType) {
                    PlanType plan = (PlanType) childPlan.getUserObject();
                    boolean equals = planDBInfo.getPlanId().equalsIgnoreCase(plan.getId());
                    if (equals) {
                        byte[] localMD5 = plan.asIMCPlan().payloadMD5();
                        byte[] remoteMD5 = planDBInfo.getMd5();
                        return ByteUtil.equal(localMD5, remoteMD5) ? State.SYNC : State.NOT_SYNC;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
        }

        return State.REMOTE;
    }

    public void rebuildTransponderNodes(ImcSystem imcSystem) {
        try {
            if (imcSystem == null)
                return;

            DefaultMutableTreeNode trans = treeModel.trans;
            if (trans != null && trans.getChildCount() != 0) {
                LblConfig lblCfg = (LblConfig) imcSystem.retrieveData(ImcSystem.LBL_CONFIG_KEY);
                Vector<LblBeacon> beacons = lblCfg != null ? lblCfg.getBeacons() : new Vector<LblBeacon>();
                ExtendedTreeNode childTrans = (ExtendedTreeNode) trans.getFirstChild();
                int i = 0;
                while (childTrans != null) {
                    if (childTrans.getUserObject() instanceof TransponderElement) {
                        HashMap<String, Object> userInfo = childTrans.getUserInfo();
                        updateTimeElapsed(childTrans, userInfo);
                        userInfo.put("sync", State.NOT_SYNC);

                        if (lblCfg != null && !beacons.isEmpty()) {
                            try {
                                TransponderElement transE = (TransponderElement) childTrans.getUserObject();
                                LblBeacon bc = TransponderUtils.getTransponderAsLblBeaconMessage(transE);
                                byte[] localMD5 = bc.payloadMD5();
                                for (LblBeacon b : beacons) {
                                    byte[] remoteMD5 = b.payloadMD5();
                                    if (ByteUtil.equal(localMD5, remoteMD5)) {
                                        userInfo.put("sync", State.SYNC);
                                        userInfo.put("id", i);
                                        userInfo.put("vehicle", imcSystem.getName());
                                        beacons.remove(b);
                                        break;
                                    }
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    i++;
                    childTrans = (ExtendedTreeNode) childTrans.getNextSibling();
                }
            }
            treeModel.nodeStructureChanged(trans);// --> has twitches
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateTimeElapsed(ExtendedTreeNode childTrans, HashMap<String, Object> userInfo) {
        ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
        if (imcSystems != null) {
            LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(((TransponderElement) childTrans
                    .getUserObject()).getName());
            if (timer != null) {
                if (timer.isRunning()) {
                    treeModel.nodeChanged(childTrans);
                }
            }
        }
    }

    /**
     * @param id
     * @param range
     * @param timestampMillis
     * @param reason
     */
    public void updateTransponderRange(short id, double range, String mainVehicle) {
        DefaultMutableTreeNode trans = treeModel.trans;
        int childCount = trans.getChildCount();
        for (int c = 0; c < childCount; c++) {
            ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
            HashMap<String, Object> userInfo = transNode.getUserInfo();
            if (((int) userInfo.get("id")) == id && ((String) userInfo.get("vehicle")).equals(mainVehicle)) {

                ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
                if (imcSystems != null) {
                    String name = ((TransponderElement) transNode.getUserObject()).getName();
                    LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer == null) {
                        timer = new LBLRangesTimer();
                        imcSystems.storeData(name, timer);
                    }
                    if (range == -1) {
                        timer.stopTimer();
                    }
                    else {
                        timer.resetTime();
                    }
                }
                revalidate();
                break;
            }
        }
    }

    /**
     * @param id
     * @param range
     * @param timestampMillis
     * @param reason
     */
    public void stopTransponderRange(String mainVehicle) {
        DefaultMutableTreeNode trans = treeModel.trans;
        int childCount = trans.getChildCount();
        for (int c = 0; c < childCount; c++) {
            ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
            HashMap<String, Object> userInfo = transNode.getUserInfo();
            if (((String) userInfo.get("vehicle")).equals(mainVehicle)) {

                ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
                if (imcSystems != null) {
                    String name = ((TransponderElement) transNode.getUserObject()).getName();
                    LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer == null) {
                        timer = new LBLRangesTimer();
                        imcSystems.storeData(name, timer);
                    }
                    timer.stopTimer();
                }
                revalidate();
                break;
            }
        }
    }

    private enum ParentNodes {
        MAP("Maps"),
        TRANSPONDERS("Transponders"),
        PLANS("Plans"),
        // REMOTE_PLANS("Remote Plans"),
        MARKS("Marks"),
        CHECKLISTS("Checklists");

        public final String nodeName;

        private ParentNodes(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    /**
     * Handles major changes in node structure.
     * 
     * @author Margarida Faria
     * 
     */
    private class Model extends DefaultTreeModel {
        private static final long serialVersionUID = 5581485271978065950L;
        private final DefaultMutableTreeNode trans, plans, maps;
        private DefaultMutableTreeNode homeR;

        // !!Important!! Always add with insertNodeInto (instead of add) and remove with removeNodeFromParent (instead
        // of remove). It will remove directly from the Vector that support the model and notify of the structure
        // changes.

        /**
         * @param root
         */
        public Model(TreeNode root) {
            super(root);
            maps = new DefaultMutableTreeNode(ParentNodes.MAP.nodeName);
            plans = new DefaultMutableTreeNode(ParentNodes.PLANS.nodeName);
            trans = new DefaultMutableTreeNode(ParentNodes.TRANSPONDERS.nodeName);
        }

        public void clearTree() {

            System.out.println("Clean tree");
            if (((DefaultMutableTreeNode) root).getChildCount() > 0) {
                DefaultMutableTreeNode firstLeaf = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) root)
                        .getFirstChild();
                System.out.println("first leaf " + firstLeaf.toString());
                removeNodeFromParent(firstLeaf);
            }
            if (trans.getParent() != null) {
                removeNodeFromParent(trans);
                System.out.println("trans " + trans.getChildCount());
                cleanParent(ParentNodes.TRANSPONDERS);
                System.out.println("trans " + trans.getChildCount());
            }
            if (plans.getParent() != null) {
                removeNodeFromParent(plans);
                System.out.println("plans " + plans.getChildCount());
                cleanParent(ParentNodes.PLANS);
                System.out.println("plans " + plans.getChildCount());
            }
            if (maps.getParent() != null) {
                removeNodeFromParent(maps);
                System.out.println("maps " + maps.getChildCount());
                cleanParent(ParentNodes.MAP);
            }
        }

        public void redoModel(final Vector<TransponderElement> transElements, final HomeReference homeRef,
                final Collection<PlanType> plansElements, final PlanType selectedPlan) {
            clearTree();
            setHomeRef(homeRef);
            int index = 0; // homeRef is at index 0

            for (TransponderElement elem : transElements) {
                ExtendedTreeNode node = new ExtendedTreeNode(elem);
                node.getUserInfo().put("id", -1);
                node.getUserInfo().put("vehicle", "");
                addToParents(node, ParentNodes.TRANSPONDERS);
            }
            if (trans.getChildCount() >= 0 && !((DefaultMutableTreeNode) root).isNodeChild(trans)) {
                index++;
                System.out.println("trans was added to root");
                insertNodeInto(trans, (MutableTreeNode) root, index);
            }
            System.out.println("trans.getChildCount()" + trans.getChildCount() + ", root.isNodeChild(trans)"
                    + ((DefaultMutableTreeNode) root).isNodeChild(trans) + ", root children:" + root.getChildCount());

            for (PlanType planT : plansElements) {
                addToParents(new ExtendedTreeNode(planT), ParentNodes.PLANS);
            }
            if (plans.getChildCount() >= 0 && !plans.isNodeChild(root)) {
                index++;
                insertNodeInto(plans, (MutableTreeNode) root, index);
            }

            if (selectedPlan != null)
                setSelectedPlan(selectedPlan);

        }

        private void cleanParent(ParentNodes parent) {
            DefaultMutableTreeNode parentNode;
            switch (parent) {
                case PLANS:
                    parentNode = plans;
                    break;
                case TRANSPONDERS:
                    parentNode = trans;
                    break;
                case MAP:
                    parentNode = maps;
                    break;
                default:
                    System.out.println("ADD SUPPORT FOR " + parent.nodeName + " IN MissionBrowser");
                    return;
            }
            while (parentNode.getChildCount() > 0) {
                removeNodeFromParent((MutableTreeNode) parentNode.getFirstChild());
            }
        }


        private void addToParents(DefaultMutableTreeNode node, ParentNodes parent) {
            System.out.println("Adding to " + parent.nodeName + " " + node.getUserObject());
            switch (parent) {
                case PLANS:
                    // plans.add(node);
                    insertNodeInto(node, plans, 0);
                    break;
                case TRANSPONDERS:
                    trans.add(node);
                    insertNodeInto(node, trans, 0);
                    break;
                case MAP:
                    maps.add(node);
                    insertNodeInto(node, maps, 0);
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parent.nodeName + " IN MissionBrowser");
                    break;
            }
        }

        private void addToParents(ArrayList<Identifiable> objectToAdd, ParentNodes parent, State state) {
            DefaultMutableTreeNode parentNode;
            switch (parent) {
                case PLANS:
                    parentNode = plans;
                    break;
                case TRANSPONDERS:
                    parentNode = trans;
                    break;
                case MAP:
                    parentNode = maps;
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parent.nodeName + " IN MissionBrowser");
                    return;
            }

            Identifiable object;
            ExtendedTreeNode newChild;
            for (Iterator<Identifiable> iterator = objectToAdd.iterator(); iterator.hasNext();) {
                object = iterator.next();
                newChild = new ExtendedTreeNode(object);
                newChild.getUserInfo().put("sync", state);
                parentNode.add(newChild);
            }
            nodeStructureChanged(parentNode);
        }

        /**
         * 
         * @param item
         * @param parent
         * @return true changes have been made
         */
        private <E extends Identifiable> boolean removeById(E item, DefaultMutableTreeNode parent) {
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                @SuppressWarnings("unchecked")
                E userObject = (E) ((DefaultMutableTreeNode) parent.getChildAt(i)).getUserObject();
                if (userObject.getIdentification().equals(item.getIdentification())) {
                    MutableTreeNode child = (MutableTreeNode) parent.getChildAt(i);
                    removeNodeFromParent(child);
                    if (childCount == 0) {
                        removeNodeFromParent(parent);
                        parent = null;
                    }
                    return true;
                }
            }
            return false;
        }

        public void setHomeRef(HomeReference href) {
            homeR = new DefaultMutableTreeNode(href);
            System.out.println("Inserting homeref");
            insertNodeInto(homeR, (MutableTreeNode) root, 0);
        }
    }
}

