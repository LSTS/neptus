package pt.lsts.neptus.nvl.runtime;

import java.util.List;

public interface TaskSpecification {

	List<VehicleRequirements> getRequirements();
	//List<PayloadComponent> getComponents();
	String getId();
	List<Position> getAreaToMap();
	
}
