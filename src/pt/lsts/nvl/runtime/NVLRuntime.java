package pt.lsts.nvl.runtime;

import java.util.List;


public interface NVLRuntime {

  List<NVLVehicle> getVehicles(Filter<NVLVehicle> filter);//substitute to VehicleRequirements?
  NVLVehicle getVehicle(String id);

  List<TaskSpecification> getTasks(Filter<TaskSpecification> filter);  
  TaskSpecification getTask(String id);

  TaskExecution launchTask(TaskSpecification task, List<NVLVehicle> vehicles);

}
