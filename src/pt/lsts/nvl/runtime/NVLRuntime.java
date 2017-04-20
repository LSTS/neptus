package pt.lsts.nvl.runtime;

import java.util.List;

import pt.lsts.neptus.plugins.nvl_runtime.NeptusTaskExecutionAdapter;


public interface NVLRuntime {

  List<NVLVehicle> getVehicles(Filter<NVLVehicle> filter);//substitute to VehicleRequirements?
  NVLVehicle getVehicle(String id);

  List<TaskSpecification> getTasks(Filter<TaskSpecification> filter);  
  TaskSpecification getTask(String id);

  List<NeptusTaskExecutionAdapter> launchTask(TaskSpecification task, List<NVLVehicle> vehicles);

}
