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
 * Author: Junior
 * 10 de Set de 2013
 */
package pt.lsts.neptus.plugins.planqueue;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventNewSystem;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.IPlanSelection;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBAdapter;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBControl;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanCompability;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;

import com.google.common.eventbus.Subscribe;


/**
 * @author Manuel Ribeiro
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Plan Queue", icon = "pt/lsts/neptus/plugins/planqueue/images/plan_queue.png", author = "Manuel Ribeiro", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 100)

public class PlanQueue extends InteractionAdapter {


    @NeptusProperty(name = "Use Calibration on Start Plan", userLevel = LEVEL.ADVANCED)
    public boolean useCalibrationOnStartPlan = true;
    protected static final boolean DONT_USE_ACOUSTICS = true;
    protected static final boolean USE_ACOUSTICS = false;
    @NeptusProperty(name = "Use TCP To Send Messages", userLevel = LEVEL.ADVANCED)
    public boolean useTcpToSendMessages = true;
    @NeptusProperty(name = "Service name for acoustic message sending", userLevel = LEVEL.ADVANCED, 
            distribution = DistributionEnum.DEVELOPER)
    public String acousticOpServiceName = "acoustic/operation";
    @NeptusProperty(name = "Use only active systems for acoustic message sending", userLevel = LEVEL.ADVANCED, 
            distribution = DistributionEnum.DEVELOPER)
    public boolean acousticOpUseOnlyActive = false;
    private final LinkedHashMap<Integer, Long> registerRequestIdsTime = new LinkedHashMap<Integer, Long>();
    private final LinkedHashMap<Integer, PlanControl> requests = new LinkedHashMap<>();


    public enum ToolLocation {
        Right,
        Left
    };
    @NeptusProperty(name = "Toolbar Location", userLevel = LEVEL.REGULAR)
    public ToolLocation toolbLocation = ToolLocation.Right;

    private HashMap<String, STATE> vehiclePlanStates = null;

    private boolean active = false;
    protected PlanDBControl pdbControl;

    protected PlanDBAdapter planDBListener = new PlanDBAdapter() {
        @Override
        public void dbCleared() {
        }

        @Override
        public void dbInfoUpdated(PlanDBState updatedInfo) {
        }

        @Override
        public void dbPlanReceived(PlanType spec) {
            spec.setMissionType(getConsole().getMission());
            getConsole().getMission().addPlan(spec);
            getConsole().getMission().save(true);
            getConsole().updateMissionListeners();

        }

        @Override
        public void dbPlanRemoved(String planId) {
        }

        @Override
        public void dbPlanSent(String planId) {
        }
    };

    private ImcSystem[] veh = null;

    private Vector<VehicleType> avVehicles = null;
    private TreeMap<String, PlanType> planQueue = null;


    private PlanQueuePanel planQueuePanel = null;

    private StateRenderer2D renderer;
    private StateRendererInteraction delegate = null;
    private JPanel controls;
    protected JPanel sidePanel = null;
    protected JLabel statsLabel = null;
    JButton deleteBtn = null;
    JButton moveUpBtn = null;
    JToggleButton startStopBtn = null;
    JButton moveDownBtn = null;
    JButton closeBtn = null;

    /**
     * @param console
     */
    public PlanQueue(ConsoleLayout console) {
        super(console);
        //MissionTreePanel missionTreePanel2 = new MissionTreePanel(console);

        //  this.missionTreePanel = getConsole().getSubPanelsOfClass(MissionTreePanel.class);
        //this.missionBrowser = missionTreePanel.get(0).browser;
        this.planQueue = new  TreeMap<String, PlanType>();
        this.avVehicles = new Vector<VehicleType>();
        this.vehiclePlanStates = new HashMap<String, STATE>();
        /*
        missionBrowser.addTreeListener(getConsole());
        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                selection = missionBrowser.getSelectedItem();
                if (e.getClickCount() == 1 && !e.isConsumed()) {
                    e.consume();
                    if (selection instanceof PlanType)
                        getPlanQueuePanel().getAddBtn().setEnabled(true);
                    else
                        getPlanQueuePanel().getAddBtn().setEnabled(false);
                }
            }
        };

        missionBrowser.addMouseAdapter(mouseAdapter);

         */

    }

    protected PlanQueuePanel getPlanQueuePanel() {
        if (planQueuePanel == null)
            planQueuePanel = new PlanQueuePanel();
        return planQueuePanel;
    }

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(getClass()));
    }


    @Override
    public void initSubPanel() {

    }


    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        this.renderer = source;
        if (mode) {
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                c.add(getSidePanel(), BorderLayout.EAST);

                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new LineBorder(Color.black.darker(), 2));

            }
        } else {
            if (delegate != null) {
                delegate.setActive(false, source);
                getPlanQueuePanel().getAddBtn().setSelected(false);
                delegate = null;
            }
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                c.remove(getSidePanel());
                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new EmptyBorder(0, 0, 0, 0));
            }

            renderer.setToolTipText("");
        }

    }


    @Subscribe
    public void onNewSystem(ConsoleEventNewSystem e) {
    }

    private void uploadPlan(String vehicle){

        if (vehicle == null) { 
            if (!avVehicles.isEmpty()){      
                for(int i=0; i<avVehicles.size();i++){
                    uploadAux(avVehicles.get(i).getId());
                }
                avVehicles.clear();
            }
        }
        else {
            uploadAux(vehicle);
        }


    }

    private int getIndexOfvehicle(Vector<VehicleType> v, String vehicle){
        int index=-1;
        for (VehicleType vt : v) {
            if (vt.getId().equals(vehicle))
               return v.indexOf(vt);
        }
        return index;
    }

    private void uploadAux(String vehicle) {

        if (!planQueue.isEmpty()) {
            if (!vehiclePlanStates.isEmpty() && vehiclePlanStates.containsKey(vehicle) && ((vehiclePlanStates.get(vehicle).compareTo(STATE.READY) == 0) || (vehiclePlanStates.get(vehicle).compareTo(STATE.BLOCKED) == 0))) {
                Object planName = getPlanQueuePanel().getTableValue(0);
                PlanType planToSend = null;
                for (int j=0;j<planQueue.size();j++) {
                    if (planQueue.containsKey((String)planName) ){
                        planToSend = planQueue.get((String) planName);
                    }
                }
                boolean sent = false;
                if (planToSend!=null) {
                    if (PlanCompability.isVehicleCompatible(avVehicles.get(getIndexOfvehicle(avVehicles, vehicle)), planToSend)) {   
                        sendPlan(vehicle,planToSend);
                        sent=true;
                    } else {
                        //System.err.println("The vehicle "+vehicle+" is not compatible with "+planToSend.getId());
                        sent = false;
                    }
                }

                //apaga
                if (planName!=null && planToSend!=null && sent) {
                    if (planToSend.equals(planQueue.remove((String)planName))){
                        if (getPlanQueuePanel().removeRowZero()){
                            //removed successfully
                        }

                    }
                }
            }
        }
    }


    /**
     * @param vehicle
     */
    private void sendPlan(String vehicle, PlanType plan) {


        class OneShotTask implements Runnable {
            String vehicles;
            PlanType plan1;
            OneShotTask(PlanType s, String v) { plan1 = s; vehicles = v; }
            public void run() {
                try{   
                    
                    planControlUpdate(vehicles);
                    if (pdbControl == null) {
                        //System.out.println("Pdb null");
                    }



                    pdbControl.setRemoteSystemId(vehicles);
                    pdbControl.sendPlan(plan1);

                    System.out.println(">> Plan '"+plan1.toString() + "' sent to vehicle '"+vehicles+"'");


                    planControlUpdate(vehicles);
                    String[] systemsToSend = {vehicles};
                    sendStart(PlanControl.OP.START, plan1, systemsToSend);
                    System.out.println(">> Plan '"+plan1.toString() + "' executed at vehicle '"+vehicles+"'");
                }catch(Exception e)
                {
                    System.out.println("Exception caught");
                }

            }
        }
        
        Thread t = new Thread(new OneShotTask(plan, vehicle));
        t.start();


    }



    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        if (isDispatchActive()) {
            switch (e.getState()) {
                case SERVICE:
                    if (ImcSystemsHolder.getSystemWithName(e.getVehicle()).isActive()) {
      
                        uploadPlan(e.getVehicle());
                    }
                    break;
                case ERROR:
                    break;
                case CALIBRATION:
                    break;
                default:
                    break;
            }
        }
    }

    private void loadInitialActiveVehicles() {
        veh = ImcSystemsHolder.lookupActiveSystemVehicles(); 

        for (int i = 0; i < veh.length; i++) {
            VehicleType vehicle = VehiclesHolder.getVehicleWithImc(veh[i].getId());

            if (vehicle != null) {
                if (getConsole().getMainSystem() != null
                        && getConsole().getMainSystem().equalsIgnoreCase(vehicle.getId())) 
                    avVehicles.add(0, vehicle);           
                else 
                    avVehicles.add(vehicle);

            }

        }

    }


    /**
     * @param id
     */
    private void planControlUpdate(String id) {
        removePlanDBListener();
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);

        if (sys == null) {
            pdbControl = null;
            NeptusLog.pub().error(
                    "The main vehicle selected " + id
                    + " is not in the vehicles-defs folder. Please add definitions file for this vehicle.");
        }
        else {
            pdbControl = sys.getPlanDBControl();
            if (pdbControl == null) {
                pdbControl = new PlanDBControl();
                pdbControl.setRemoteSystemId(id);
            }

            pdbControl.addListener(planDBListener);
        }
    }

    private void removePlanDBListener() {
        if (pdbControl != null)
            pdbControl.removeListener(planDBListener);
    }

    protected AbstractAction deleteAction() {
        return new AbstractAction(I18n.text("Del"), ImageUtils.getScaledIcon(
                "pt/lsts/neptus/plugins/planqueue/images/edit_remove.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                Object tableValue = getPlanQueuePanel().getSelectedTableValue();
                if (tableValue!=null) {
                    PlanType toRemoveValue = planQueue.get(tableValue);
                    if (toRemoveValue.equals(planQueue.remove(tableValue))){
                        if (getPlanQueuePanel().removeRow()){
                            if (planQueue.size()==0) {
                                moveDownBtn.setEnabled(false);
                                moveUpBtn.setEnabled(false);
                                deleteBtn.setEnabled(false);
                                startStopBtn.setEnabled(false);
                            }

                        }

                    }
                }
            }
        };
    }
    protected AbstractAction moveUpAction() {
        return new AbstractAction(I18n.text("Up"), ImageUtils.getScaledIcon(
                "pt/lsts/neptus/plugins/planqueue/images/moveUp.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                getPlanQueuePanel().moveUp();

            }
        };
    }
    protected AbstractAction moveDownAction() {
        return new AbstractAction(I18n.text("Down"), ImageUtils.getScaledIcon(
                "pt/lsts/neptus/plugins/planqueue/images/moveDown.png", 16, 16)) {


            @Override
            public void actionPerformed(ActionEvent e) {

                getPlanQueuePanel().moveDown();

            }
        };
    }

    private void add(PlanType sel) {
        if (!planQueue.containsKey(sel.toString())) {
            planQueue.put(sel.toString(), sel); //add to queue
            String rows = null;


            int lastRow = getPlanQueuePanel().getTableRows();
            rows  = Integer.toString(lastRow);

            getPlanQueuePanel().addRow(new String[] { rows, sel.toString(), "0"});

        }
        
    }
    protected JPanel getSidePanel() {
        if (sidePanel == null) {
            sidePanel = new JPanel(new BorderLayout(2, 2));

            controls = new JPanel(new GridLayout(0, 5));


            deleteBtn = new JButton(deleteAction());
            moveUpBtn = new JButton(moveUpAction());
            moveDownBtn = new JButton(moveDownAction());
            startStopBtn = new JToggleButton("Start", false);
            closeBtn = new JButton(closeAction());

            deleteBtn.setEnabled(false);
            moveUpBtn.setEnabled(false);
            moveDownBtn.setEnabled(false);
            startStopBtn.setEnabled(false);

            controls.add(deleteBtn);
            controls.add(moveUpBtn);
            controls.add(moveDownBtn);
            controls.add(startStopBtn);
            controls.add(closeBtn);


            sidePanel.add(controls, BorderLayout.SOUTH);

            JPanel holder = new JPanel(new BorderLayout());
            holder.add(getPlanQueuePanel());

            sidePanel.add(holder, BorderLayout.CENTER);


            startStopBtn.addItemListener (new ItemListener() {

                public void itemStateChanged (ItemEvent ie ) {

                    if (startStopBtn.isSelected()) {
                        startStopBtn.setText("Stop");
                        startStopBtn.setSelected(true);
                        setDispatchActive(true);
                        loadInitialActiveVehicles();
                        uploadPlan(null);
                    } else {
                        startStopBtn.setText("Start");
                        startStopBtn.setSelected(false);
                        setDispatchActive(false);
                    }

                }

            } ) ;

            getPlanQueuePanel().getAddBtn().addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getConsole().getPlan()!=null) {
                        PlanType sel = null;
                        Vector<IPlanSelection> psel = getConsole().getSubPanelsOfInterface(IPlanSelection.class);
                        if (psel.get(0).getSelectedPlans().size()>1) { 
                            for (int i=0; i<psel.get(0).getSelectedPlans().size(); i++) {
                                sel = psel.get(0).getSelectedPlans().get(i);
                                add(sel);
                            }
                        } 
                        else { 
                            sel = getConsole().getPlan();
                            add(sel);
                        }
                        System.out.println(planQueue.toString());

                        if (getPlanQueuePanel().getTable()!=null) {
                            getPlanQueuePanel().getTable().addMouseListener(new MouseAdapter() {
                                public void mouseClicked(MouseEvent e) {

                                    deleteBtn.setEnabled(true);
                                    moveUpBtn.setEnabled(true);
                                    moveDownBtn.setEnabled(true);
                                    startStopBtn.setEnabled(true);
                                    // System.out.println("table click"); DEBUG

                                }
                            });
                        }
      
                    }

                }

            });

            getPlanQueuePanel().setOpaque(false);
            controls.setOpaque(false);
        }
        return sidePanel;
    }


    @Override
    public void cleanSubPanel() {

        removePlanDBListener();
    }


    @Override
    public boolean isExclusive() {

        return false;
    }


    /**
     * @return the active
     */
    public boolean isDispatchActive() {
        return active;
    }


    /**
     * @param active the active to set
     */
    public void setDispatchActive(boolean active) {
        this.active = active;
    }

    protected AbstractAction closeAction() {
        return new AbstractAction(I18n.text(""), ImageUtils.getScaledIcon(
                "pt/lsts/neptus/plugins/planning/images/edit_close.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setActive(false, renderer);
            }
        };
    }




    @Subscribe
    public void consume(PlanControlState pcstate) {
        ImcSystem system = ImcSystemsHolder.lookupSystem(pcstate.getSrc());
        if (system == null)
            return;
        switch (pcstate.getState()) {
            case INITIALIZING:
                vehiclePlanStates.put(pcstate.getSourceName(), pcstate.getState());
                break;
            case EXECUTING:
                vehiclePlanStates.put(pcstate.getSourceName(), pcstate.getState());
                break;
            case BLOCKED:
              vehiclePlanStates.put(pcstate.getSourceName(), pcstate.getState());
                break;
            case READY:
                vehiclePlanStates.put(pcstate.getSourceName(), pcstate.getState());

                break;
        }
    }


    /**
     * @author Paulo Dias
     * @param reqId
     */
    private void registerPlanControlRequest(int reqId) {
        registerRequestIdsTime.put(reqId, System.currentTimeMillis());
    }

    /**
     * @author Paulo Dias
     * @return
     */
    private MessageDeliveryListener createDefaultMessageDeliveryListener() {
        return (!useTcpToSendMessages || false ? null : new MessageDeliveryListener() {

            private String  getDest(IMCMessage message) {
                ImcSystem sys = message != null ? ImcSystemsHolder.lookupSystem(message.getDst()) : null;
                String dest = sys != null ? sys.getName() : I18n.text("unknown destination");
                return dest;
            }

            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery destination unreacheable",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery timeout",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery error. (%error)",
                                message.getAbbrev(), getDest(message), error)));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }

            @Override
            public void deliverySuccess(IMCMessage message) {

            }
        });
    }

    /**
     * @author Paulo Dias
     * @param systems
     */
    private boolean testAndShowWarningForNoSystemSelection(String... systems) {
        if (systems.length < 1) {
            // getConsole().warning(this.getName() + ": " + I18n.text("No systems selected to send to!"));
            return true;
        }
        return false;
    }

    /**
     * @author Paulo Dias
     * @param plan
     * @param systems
     * @return
     */
    private boolean verifyIfPlanIsInSyncOnTheSystem(PlanType plan, String... systems) {
        boolean planInSync = true;
        String systemsNotInSync = "";
        for (String sysStr : systems) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sysStr);
            if (sys == null)
                continue;
            PlanDBState prs = sys.getPlanDBControl().getRemoteState();
            if (prs == null || !prs.matchesRemotePlan(plan)) {
                planInSync = false;
                systemsNotInSync += (systemsNotInSync.length() > 0 ? ", " : "") + sysStr;
            }
        }

        return planInSync;
    }

    /**
     * @author Paulo Dias
     * @param component
     * @param checkMission
     * @param checkPlan
     * @return
     * 
     */
    protected boolean checkConditionToRun(Component component, boolean checkMission, boolean checkPlan, PlanType plan) {
        if (!ImcMsgManager.getManager().isRunning()) {
            //            GuiUtils.errorMessage(this, component.getName(), I18n.text("IMC comms. are not running!"));
            post(Notification.error(component.getName(), I18n.text("IMC comms. are not running!")));
            return false;
        }

        ConsoleLayout cons = getConsole();
        if (cons == null) {
            //            GuiUtils.errorMessage(this, component.getName(), I18n.text("Missing console attached!"));
            post(Notification.error(component.getName(), I18n.text("Missing console attached!")));
            return false;
        }

        if (checkMission) {
            MissionType miss = cons.getMission();
            if (miss == null) {
                //                GuiUtils.errorMessage(this, component.getName(), I18n.text("Missing attached mission!"));
                post(Notification.error(component.getName(), I18n.text("Missing attached mission!")));
                return false;
            }
        }

        if (checkPlan) {
            if (plan == null) {
                //                GuiUtils.errorMessage(this, component.getName(), I18n.text("Missing attached plan!"));
                post(Notification.error(component.getName(), I18n.text("Missing attached plan!")));
                return false;
            }
        }
        return true;
    }

    /**
     * @author Paulo Dias
     **/
    private boolean sendStart(PlanControl.OP cmd, PlanType plan, String... systems) {
        if (!checkConditionToRun(this, true, true, plan))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        int reqId = IMCSendMessageUtils.getNextRequestId();
        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setRequestId(reqId);
        String cmdStrMsg = "";

        try {

            cmdStrMsg += I18n.text("Error sending start plan");
            while(!verifyIfPlanIsInSyncOnTheSystem(plan, systems)) {}
               /* 
            if (!verifyIfPlanIsInSyncOnTheSystem(plan, systems)) {
                if (systems.length == 1)
                    post(Notification.error(I18n.text("Send Start Plan"),"Plan not in sync on system!"));
                return false;
            }
*/
            pc.setPlanId(plan.getId());
            if (useCalibrationOnStartPlan)
                pc.setFlags(PlanControl.FLG_CALIBRATE);

        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }
        pc.setOp(cmd);

        boolean dontSendByAcoustics = DONT_USE_ACOUSTICS;
        if (cmd == PlanControl.OP.START) {
            String planId = pc.getPlanId();
            if (planId.length() == 1) {
                dontSendByAcoustics = USE_ACOUSTICS;
            }
        }
        boolean ret = IMCSendMessageUtils.sendMessage(pc, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                createDefaultMessageDeliveryListener(), this, cmdStrMsg, dontSendByAcoustics,
                acousticOpServiceName, acousticOpUseOnlyActive,false, true, systems);

        if (!ret) {
            //            GuiUtils.errorMessage(this, I18n.text("Send Plan"), I18n.text("Error sending PlanControl message!"));
            post(Notification.error(I18n.text("Send Plan"), I18n.text("Error sending PlanControl message!")));
            return false;
        }
        else {
            registerPlanControlRequest(reqId);
            requests.put(reqId, pc);
        }

        return true;
    }


}