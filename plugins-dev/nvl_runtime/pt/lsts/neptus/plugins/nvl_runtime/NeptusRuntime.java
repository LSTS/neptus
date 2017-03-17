package pt.lsts.neptus.plugins.nvl_runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import nvl.Filter;
import nvl.NVLRuntime;
import nvl.TaskExecution;
import nvl.TaskSpecification;
import nvl.Vehicle;
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
   private final Map<String,NVLVehicleData> vehicles = Collections.synchronizedMap(new HashMap<>());
   private final Map<String,NVLTaskSpecification> tasks = Collections.synchronizedMap(new HashMap<>()); //Or List?
	/**
     * @param console
     */
    public NeptusRuntime(ConsoleLayout console) {
        super(console);
        for(ImcSystem vec: ImcSystemsHolder.lookupAllActiveSystems()){
            if(vec.getType() == SystemTypeEnum.VEHICLE)
                vehicles.put(vec.getName(),new NVLVehicleData(vec,STATE.CONNECTED));//TODO
        }
        for(PlanType plan: getConsole().getMission().getIndividualPlansList().values()){
            tasks.put(plan.getId(),new NVLTaskSpecification(plan));
        }
    }

    @Override
	public List<Vehicle> getVehicles(Filter<Vehicle> f) {
	        List <Vehicle> result = new ArrayList<>();
	        
	        for(Vehicle v: vehicles.values()){
	            if(f.apply(v))
	                result.add(v);
	        }
        //vehicles.values().stream().filter(x -> f.apply(x)).forEach(v -> result.add(v));
		return result;
	}

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
          
        ImcSystem imcsystem = ImcSystemsHolder.getSystemWithName(e.getVehicle());
        vehicles.put(e.getVehicle(), new NVLVehicleData(imcsystem,e.getState()));
    }

    @Override
	public Vehicle getVehicle(String id) {
		return vehicles.get(id);
	}

	@Override
	public List<TaskSpecification> getTasks(Filter<TaskSpecification> filter) {
	    List <TaskSpecification> result = new ArrayList<>();
	    
        for(TaskSpecification task: tasks.values()){
            if(filter.apply(task))
                result.add(task);
        }
		return result;
	}

	@Override
	public TaskSpecification getTask(String id) {
		
	    //PlanType plan = this.getConsole().getMission().getIndividualPlansList().get(id);
		return tasks.get(id);
	}

	@Override
	public TaskExecution launchTask(TaskSpecification task, List<Vehicle> vehicles) {
	    //TODO define if critical msg -> need to use acoustics communications
		PlanControl pc = task.toPlanControl();
		boolean acoustics=false;
		//sendMessage(IMCMessage msg, String errorTextForDialog, boolean sendOnlyThroughOneAcoustically,String... ids)
		String[] vs = new String[vehicles.size()];
		vs = vehicles.toArray(vs);
		//this.getConsole().getMission().addPlan(new PlanType());
		IMCSendMessageUtils.sendMessage(pc,null,acoustics, vs);
		return new NVLTaskExecution(pc);
	}


}
