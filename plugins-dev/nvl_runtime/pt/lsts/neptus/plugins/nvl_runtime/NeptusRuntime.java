package pt.lsts.neptus.plugins.nvl_runtime;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.nvl.runtime.Availability;
import pt.lsts.nvl.runtime.Filter;
import pt.lsts.nvl.runtime.NVLRuntime;
import pt.lsts.nvl.runtime.NVLVehicle;
import pt.lsts.nvl.runtime.NVLVehicleType;
import pt.lsts.nvl.runtime.TaskExecution;
import pt.lsts.nvl.runtime.TaskSpecification;
import pt.lsts.nvl.runtime.TaskState;
import pt.lsts.nvl.runtime.VehicleRequirements;

@PluginDescription(name = "NVL Runtime Feature", author = "Keila Lima")
@Popup(pos = POSITION.BOTTOM_RIGHT, width=300, height=300, accelerator='y')
@SuppressWarnings("serial")
public class NeptusRuntime extends InteractionAdapter implements NVLRuntime {
   private  Map<String,NeptusVehicleAdapter> vehicles;
   private  Map<String,NeptusTaskSpecificationAdapter> tasks; //Or List?
   private  List<TaskExecution> runningTasks;
   private JButton testButton;
	/**
     * @param console
     */
    public NeptusRuntime(ConsoleLayout console) {
        super(console);
        
    }

    
    @Override
    public void initSubPanel() {
        runningTasks = Collections.synchronizedList(new ArrayList<>());
        vehicles = Collections.synchronizedMap(new HashMap<>());
        tasks = Collections.synchronizedMap(new HashMap<>());
        //initialize active vehicles
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles()){
            VehicleState systemState  = ImcMsgManager.getManager().getState(vec.getName()).last(VehicleState.class);
            EstimatedState estimState = ImcMsgManager.getManager().getState(vec.getName()).last(EstimatedState.class);
            STATE state = systemState!=null ? STATE.valueOf(systemState.getOpModeStr()) : estimState!=null? STATE.valueOf(estimState.getString("op_mode")): null;
            vehicles.put(vec.getName(),new NeptusVehicleAdapter(vec,state));
//          OP_MODE o = OP_MODE.valueOf(estimState.getMessageType().getFieldPossibleValues("op_mode").get(estimState.getLong("op_mode")));
            //System.out.println("V " + vec.getName()+" "+state.toString());
        }
        //initialize existing plans in the console
        for(PlanType plan: getConsole().getMission().getIndividualPlansList().values()){
            tasks.put(plan.getId(),new NeptusTaskSpecificationAdapter(plan));
            //System.out.println("P " + plan.getId());

        }
        test();
    }
    private void test() {
        testButton = new JButton(
                new AbstractAction(I18n.text("Test!")) {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {   
                        // Plano IMC
                        NeptusTaskSpecificationAdapter ts = (NeptusTaskSpecificationAdapter) getTasks( x -> x.getId().equals("DSL")).get(0); //Filter<TaskSpecification>
                        VehicleRequirements reqs = ts.getRequirements() .get(0)
                        .type(NVLVehicleType.AUV)
                        .availability(Availability.AVAILABLE)
                        .name("lauv-noptilus-2");
                        
                        //System.out.println("Requirements after change params: "+reqs);
                        
                        ts.setRequirements(reqs);
                        tasks.put(ts.getId(),ts);
                        // Veículos disponíveis
                        List<NVLVehicle> vs = getVehicles(ts.getRequirements().get(0));
                        for(NVLVehicle v: vs){
                            NeptusLog.pub().info(I18n.text("SELECTED VEHICLE "+v.getId()));
                        }
                        launchTask(ts, vs);
                    }
                });
        
        add(testButton);

    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
    
    
    @Override
	public List<NVLVehicle> getVehicles(Filter<NVLVehicle> f) { //VehicleRequirement <- Filter<NVLVehicle>
	        List <NVLVehicle> result = new ArrayList<>();
	        
	        for(NVLVehicle v: vehicles.values()){
	            if(f.apply(v))
	                result.add(v);
	        }
        //vehicles.values().stream().filter(x -> f.apply(x)).forEach(v -> result.add(v));
		return result;
	}

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
          
        ImcSystem imcsystem = ImcSystemsHolder.getSystemWithName(e.getVehicle());
        vehicles.put(e.getVehicle(), new NeptusVehicleAdapter(imcsystem,e.getState())); //might be disconnected
    }

    @Subscribe
    public void on(ConsoleEventPlanChange changedPlan) {
        if(changedPlan.getCurrent() == null){
            if(!this.getConsole().getMission().getIndividualPlansList().containsKey(changedPlan.getOld().getId())){
                tasks.remove(changedPlan.getOld().getId());
            }
        }
        else{
            tasks.put(changedPlan.getCurrent().getId(), new NeptusTaskSpecificationAdapter(changedPlan.getCurrent()));
        }
    }
    

    @Subscribe
    public void on(PlanControlState pcstate) {
        for(TaskExecution rt: runningTasks){
            NeptusTaskExecutionAdapter tmp = (NeptusTaskExecutionAdapter) rt;
            if (tmp.getPlanId().equals(pcstate.getPlanId())){
                switch (pcstate.getState()) {
                    case INITIALIZING:
                    case EXECUTING:
                        //tmp.setState(TaskState.EXECUTING);
                        System.out.println("EXECUTING");
                        break;
                    case BLOCKED:
                        tmp.setState(TaskState.BLOCKED);
                        //System.out.println("BLOCKED");
                        break;
                    case READY:
                        //tmp.setState(TaskState.READY_TO_EXECUTE);
                        System.out.println("READY");
                        break;
     
                        
                
                }
            switch (pcstate.getLastOutcome()) {
                case NONE:
                case FAILURE:
                    if(tmp.isDone())
                        tmp.setDone(false);
                    break;
                case SUCCESS:
                    if(!tmp.isDone())
                        tmp.setDone(true);
                    //TODO remove task from list in #NeptusRuntime ?
                    break;
                default:
                    break;
    
                
                }
            }
        }
    }


	@Override
	public List<TaskExecution> launchTask(TaskSpecification task, List<NVLVehicle> vehicles) {
	    
	    NeptusTaskSpecificationAdapter neptus_plan = (NeptusTaskSpecificationAdapter) task;
	    tasks.put(task.getId(),neptus_plan);
	    List<String> vs = new ArrayList<>();
	    VehicleRequirements req = task.getRequirements().get(0);
		vehicles.stream().filter(x -> req.apply(x)).forEach(v -> vs.add(v.getId()));
		vehicles.stream().map(v -> v.getId()).forEach(id -> neptus_plan.getPlan().setVehicle(id));
	    boolean sent= true;
	    PlanControl plan = new PlanControl();
        plan.setType(TYPE.REQUEST);
        plan.setOp(OP.START);
        plan.setPlanId(neptus_plan.getId());
        plan.setArg(neptus_plan.getPlan().asIMCPlan(true));
        int reqId = IMCSendMessageUtils.getNextRequestId();
        plan.setRequestId(reqId);
        plan.setFlags(PlanControl.FLG_CALIBRATE);

	    for(String vehicle_id: vs){
	         
	        sent = ImcMsgManager.getManager().sendMessageToSystem(plan, vehicle_id); //IMCSendMessageUtils.sendMessage(plan, NeptusRuntime.this, "Error sending " + neptus_plan.getId()+ " plan", true, false, false, vehicle_id);
	        NeptusTaskExecutionAdapter exec = new NeptusTaskExecutionAdapter(task.getId());
            if(sent)
                NeptusLog.pub().info(I18n.text(task.getId()+" sent to "+vehicle_id));
            else
                NeptusLog.pub().info(I18n.text("Unable to send "+task.getId()+" to"+vehicle_id));
            exec.synchronizedWithVehicles(sent); 
            runningTasks.add(exec);  
	    }
		return runningTasks;
	   	}

    /**
     * @return the runningTasks
     */
    public List<TaskExecution> getRunningTasks() {
        return runningTasks;
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.NVLRuntime#getVehicle(java.lang.String)
     */
    @Override
    public NVLVehicle getVehicle(String id) {
        
        return vehicles.get(id);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.NVLRuntime#getTasks(pt.lsts.neptus.nvl.runtime.Filter)
     */
    @Override
    public List<TaskSpecification> getTasks(Filter<TaskSpecification> filter) {
        List <TaskSpecification> result = new ArrayList<>();
        for(TaskSpecification task: tasks.values()){
            if(filter.apply(task))
                result.add(task);
        }
        return result;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.nvl.runtime.NVLRuntime#getTask(java.lang.String)
     */
    @Override
    public TaskSpecification getTask(String id) {
        
        return tasks.get(id);
    }
}