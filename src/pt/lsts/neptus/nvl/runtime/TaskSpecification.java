package pt.lsts.neptus.nvl.runtime;

import java.util.List;

import pt.lsts.imc.PlanControl;
import pt.lsts.neptus.types.mission.plan.PlanType;

public interface TaskSpecification {

	List<VehicleRequirements> getRequirements();

	
	
	String getId();




	
	
}
