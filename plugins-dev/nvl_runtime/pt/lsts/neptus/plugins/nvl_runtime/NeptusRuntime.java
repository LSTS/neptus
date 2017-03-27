package pt.lsts.neptus.plugins.nvl_runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.nvl.runtime.TaskExecution;
import pt.lsts.neptus.nvl.runtime.NVLVehicle;
import pt.lsts.neptus.nvl.runtime.TaskSpecification;
import pt.lsts.neptus.nvl.runtime.Filter;
import pt.lsts.neptus.nvl.runtime.NVLRuntime;
import pt.lsts.neptus.nvl.runtime.NVLVehicle;
import pt.lsts.imc.PlanControl;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

@SuppressWarnings("serial")
public class NeptusRuntime extends InteractionAdapter implements NVLRuntime {
   private  Map<String,NeptusVehicleAdapter> vehicles;
   private  Map<String,NeptusTaskSpecificationAdapter> tasks; //Or List?
	/**
     * @param console
     */
    public NeptusRuntime(ConsoleLayout console) {
        super(console);
        
    }

    
    @Override
    public void initSubPanel() {
        vehicles = Collections.synchronizedMap(new HashMap<>());
        tasks = Collections.synchronizedMap(new HashMap<>());
        
        for(ImcSystem vec: ImcSystemsHolder.lookupAllActiveSystems()){
            if(vec.getType() == SystemTypeEnum.VEHICLE)
                vehicles.put(vec.getName(),new NeptusVehicleAdapter(vec,STATE.CONNECTED));//TODO
        }
        for(PlanType plan: getConsole().getMission().getIndividualPlansList().values()){
            tasks.put(plan.getId(),new NeptusTaskSpecificationAdapter(plan));
        }

    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
    
    
    @Override
	public List<NVLVehicle> getVehicles(Filter<NVLVehicle> f) {
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
	public TaskExecution launchTask(TaskSpecification task, List<NVLVehicle> vehicles) { //Area to cover?
	    
		PlanType plan = ((NeptusTaskSpecificationAdapter) task).toPlanType();
		boolean acoustics=false; //TODO define if critical msg -> need to use acoustics communications
		String[] vs = new String[vehicles.size()];
		vs = vehicles.toArray(vs);
		       //sendMessage(IMCMessage msg, String errorTextForDialog, boolean sendOnlyThroughOneAcoustically,String... ids)
        plan.setId(task.getId());
        plan.setMissionType(getConsole().getMission());
        getConsole().getMission().getIndividualPlansList().put(task.getId(),plan);
        getConsole().getMission().save(true);
        getConsole().updateMissionListeners();
        getConsole().getMission().addPlan(plan);
        IMCSendMessageUtils.sendMessage(((NeptusTaskSpecificationAdapter) task).getMessage(),null,acoustics, vs);  
		return new NeptusTaskExecutionAdapter(plan);
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