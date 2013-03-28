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
import java.awt.Dimension;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.IPlanSelection;
import pt.up.fe.dceg.neptus.console.plugins.ISystemsSelection;
import pt.up.fe.dceg.neptus.console.plugins.ITransponderSelection;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.gui.MissionBrowser;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.LblRangeAcceptance;
import pt.up.fe.dceg.neptus.imc.LblRangeAcceptance.ACCEPTANCE;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.imc.PlanControlState.STATE;
import pt.up.fe.dceg.neptus.imc.PlanSpecification;
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
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author ZP
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Mission Tree", author = "José Pinto, Paulo Dias", icon = "pt/up/fe/dceg/neptus/plugins/planning/mission_tree.png", 
category = CATEGORY.PLANNING, version = "1.5.0")
public class MissionTreePanel extends SimpleSubPanel implements MissionChangeListener, MainVehicleChangeListener,
        DropTargetListener, NeptusMessageListener, IPlanSelection, IPeriodicUpdates, ConfigurationListener,
        ITransponderSelection {

    @NeptusProperty(name = "Use Plan DB Sync. Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean usePlanDBSyncFeatures = true;

    @NeptusProperty(name = "Use Plan DB Sync. Features Extended", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER,
            description = "Needs 'Use Plan DB Sync. Features' on")
    public boolean usePlanDBSyncFeaturesExt = false;

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
            System.out.println("dbPlanReceived");
            // PlanType lp = getConsole().getMission().getIndividualPlansList().get(spec.getId());
            //
            // spec.setMissionType(getConsole().getMission());
            //
            // getConsole().getMission().addPlan(spec);
            // getConsole().getMission().save(true);
            // getConsole().updateMissionListeners();
            //
            // if (debugOn && lp != null) {
            // try {
            // IMCMessage p1 = lp.asIMCPlan(), p2 = spec.asIMCPlan();
            //
            // ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // IMCOutputStream imcOs = new IMCOutputStream(baos);
            //
            // ByteUtil.dumpAsHex(p1.payloadMD5(), System.out);
            // ByteUtil.dumpAsHex(p2.payloadMD5(), System.out);
            //
            // System.out.println(IMCUtil.getAsHtml(p1));
            // System.out.println(IMCUtil.getAsHtml(p2));
            //
            // p1.serialize(imcOs);
            // ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
            //
            // baos = new ByteArrayOutputStream();
            // imcOs = new IMCOutputStream(baos);
            // p2.serialize(imcOs);
            // ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
            // }
            // catch (IOException e) {
            // e.printStackTrace();
            // }
            // }
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

        // planControlUpdate(getMainVehicleId());
        browser.setupListeners(getConsole(), pdbControl);
    }
    


    @Override
    public void cleanSubPanel() {
        removePlanDBListener();
    }



    protected void setStartupPos() {
        // MarkElement startEl = getStartPos();
        //
        // LocationType before = new LocationType();
        // if (startEl != null)
        // before.setLocation(startEl.getCenterLocation());
        // else
        // before.setLocation(getConsole().getMission().getHomeRef());
        //
        // LocationType after = LocationPanel.showLocationDialog(getConsole(),
        // I18n.text("Set navigation startup position"),
        // before, getConsole().getMission(), true);
        // if (after == null)
        // return;
        //
        // MapType pivot = null;
        //
        // if (startEl != null) {
        // startEl.setCenterLocation(after);
        // pivot = startEl.getParentMap();
        // MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        // mce.setSourceMap(pivot);
        // mce.setMapGroup(pivot.getMapGroup());
        // mce.setChangedObject(startEl);
        // pivot.warnChangeListeners(mce);
        // }
        // else {
        // MissionType mt = getConsole().getMission();
        //
        // if (mt.getMapsList().size() > 0)
        // pivot = mt.getMapsList().values().iterator().next().getMap();
        // else {
        // GuiUtils.errorMessage(getConsole(), I18n.text("Set navigation startup position"),
        // I18n.text("There is no map in the active mission"));
        // return;
        // }
        //
        // startEl = new MarkElement(pivot.getMapGroup(), pivot);
        // startEl.setId("start");
        // startEl.setName("start");
        // startEl.setCenterLocation(after);
        // pivot.addObject(startEl);
        // }
        //
        // pivot.saveFile(pivot.getHref());
        //
        // if (getConsole() != null && getConsole().getMission() != null
        // && getConsole().getMission().getCompressedFilePath() != null) {
        // getConsole().getMission().save(false);
        // }
        // browser.refreshBrowser(getConsole().getPlan(), getConsole().getMission());
    }


    @Override
    public void missionReplaced(MissionType mission) {
        System.out.println("missionReplaced");
        // browser.refreshBrowser(getConsole().getPlan(), getConsole().getMission());
    }

    @Override
    public void missionUpdated(MissionType mission) {
        System.out.println("missionUpdated");
        browser.refreshBrowser(getConsole().getPlan(), getConsole().getMission());
    }

    boolean inited = false;

    @Override
    public void initSubPanel() {
        if (inited)
            return;
        inited = true;
        // pdbControl.setConsole(getConsole());
        planControlUpdate(getMainVehicleId());

        browser.refreshBrowser(getConsole().getPlan(), getConsole().getMission());

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
        // MarkElement startEl = null;
        // Vector<MarkElement> marks = MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(
        // MarkElement.class);
        //
        // for (MarkElement el : marks) {
        // if (el.getId().equals("start")) {
        // startEl = el;
        // break;
        // }
        // }
        //
        // return startEl;
        return null;
    }








    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }



    @Override
    public void drop(DropTargetDropEvent dtde) {
        // try {
        // Transferable tr = dtde.getTransferable();
        // DataFlavor[] flavors = tr.getTransferDataFlavors();
        // for (int i = 0; i < flavors.length; i++) {
        // // System.out.println("Possible flavor: " + flavors[i].getMimeType());
        // if (flavors[i].isMimeTypeEqual("text/plain; class=java.lang.String; charset=Unicode")) {
        // dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        // String url = null;
        //
        // Object data = tr.getTransferData(flavors[i]);
        // if (data instanceof InputStreamReader) {
        // BufferedReader reader = new BufferedReader((InputStreamReader) data);
        // url = reader.readLine();
        // reader.close();
        // dtde.dropComplete(true);
        //
        // }
        // else if (data instanceof String) {
        // url = data.toString();
        // }
        // else
        // dtde.rejectDrop();
        //
        // if (browser.parseURL(url, getConsole().getMission()))
        // dtde.dropComplete(true);
        // else
        // dtde.rejectDrop();
        //
        // return;
        // }
        // }
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
        // dtde.rejectDrop();
    }



    

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public String[] getObservedMessages() {
        String[] messages = new String[5];
        messages[0] = "LblRangeAcceptance";
        messages[2] = "PlanDB";
        messages[3] = "EntityState";
        messages[4] = "PlanControlState";
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
        // planControlUpdate(id);
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
        pdbControl.onMessage(null, message);
        int mgid = message.getMgid();
        if (mgid == PlanSpecification.ID_STATIC) {
            PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), message);
            if (getConsole().getMission().getIndividualPlansList().containsKey(plan.getId())) {
            }
            else {
                getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
                getConsole().updateMissionListeners();
                getConsole().getMission().save(true);
            }
        }
        else if (mgid == LblRangeAcceptance.ID_STATIC) {
            LblRangeAcceptance acceptance;
            try {
                acceptance = LblRangeAcceptance.clone(message);
                int id = acceptance.getId();
                double range = acceptance.getRange();
                if (acceptance.getAcceptance() == ACCEPTANCE.AT_SURFACE) { // clean up when at surface
                    range = -1;
                    browser.updateTransponderRange((short) id, range, getMainVehicleId());
                }
                else {
                    browser.updateTransponderRange((short) id, range, getMainVehicleId());
                }
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        else if (mgid == PlanControlState.ID_STATIC) {
            PlanControlState planState = (PlanControlState) message;
            if (planState.getState() != STATE.EXECUTING) {
                browser.stopTransponderRange(getMainVehicleId());
            }
        }
    }


    @Override
    public long millisBetweenUpdates() {
        return 1500;
    }

    @Override
    public boolean update() {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                TreePath[] selectedNodes = browser.getSelectedNodes();

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

                ConsoleLayout console2 = getConsole();
                if (console2 != null) {
                    Vector<ISystemsSelection> sys = console2.getSubPanelsOfInterface(ISystemsSelection.class);
                    if (sys.size() != 0) {
                        if (usePlanDBSyncFeaturesExt) {
                            ImcSystem[] imcSystemsArray = convertToImcSystemsArray(sys);
                            browser.updateRemotePlansState(imcSystemsArray);
                        }
                    }
                }
                browser.expandTree();
                browser.setSelectedNodes(selectedNodes);
            }
        });
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
