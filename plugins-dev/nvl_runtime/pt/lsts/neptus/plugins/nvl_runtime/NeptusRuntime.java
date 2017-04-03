package pt.lsts.neptus.plugins.nvl_runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.nvl.runtime.Filter;
import pt.lsts.neptus.nvl.runtime.NVLRuntime;
import pt.lsts.neptus.nvl.runtime.NVLVehicle;
import pt.lsts.neptus.nvl.runtime.TaskExecution;
import pt.lsts.neptus.nvl.runtime.TaskSpecification;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

@SuppressWarnings("serial")
public class NeptusRuntime extends InteractionAdapter implements NVLRuntime {
   private  Map<String,NeptusVehicleAdapter> vehicles;
   private  Map<String,NeptusTaskSpecificationAdapter> tasks; //Or List?
   private  List<NeptusTaskExecutionAdapter> runningTasks;
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
        for(ImcSystem vec: ImcSystemsHolder.lookupAllActiveSystems()){
            if(vec.getType() == SystemTypeEnum.VEHICLE)
                vehicles.put(vec.getName(),new NeptusVehicleAdapter(vec,STATE.values()[(int) (vec.getLastAnnounceStateReceived())]));
        }
        //initialize existing plans in the console
        for(PlanType plan: getConsole().getMission().getIndividualPlansList().values()){
            tasks.put(plan.getId(),new NeptusTaskSpecificationAdapter(plan));
        }

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
        vehicles.put(e.getVehicle(), new NeptusVehicleAdapter(imcsystem,e.getState())); //Pode ser disconnected
    }


	@Override
	public TaskExecution launchTask(TaskSpecification task, List<NVLVehicle> vehicles) { //Area to map?//TODO
	    
	    tasks.put(task.getId(),(NeptusTaskSpecificationAdapter) task);
		boolean acoustics=false; 
		String[] vs = new String[vehicles.size()];
		vs = vehicles.toArray(vs);
		
        //ADD plan to console
	    PlanType plan = ((NeptusTaskSpecificationAdapter) task).getPlan();
		
        plan.setMissionType(getConsole().getMission());
        getConsole().getMission().getIndividualPlansList().put(task.getId(),plan);
        getConsole().getMission().save(true);
        getConsole().updateMissionListeners();
        getConsole().getMission().addPlan(plan);
        
        //sendMessage(IMCMessage msg, String errorTextForDialog, boolean sendOnlyThroughOneAcoustically,String... ids)
        boolean sent = IMCSendMessageUtils.sendMessage(plan.asIMCPlan(),null,acoustics, vs);
        
        NeptusTaskExecutionAdapter exec = new NeptusTaskExecutionAdapter(task.getId());
        exec.synchronizedWithVehicles(sent); 
        runningTasks.add(exec);  
        
		return exec;
	   	}

    /**
     * @return the runningTasks
     */
    public List<NeptusTaskExecutionAdapter> getRunningTasks() {
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