package pt.lsts.nvl.runtime;

import java.util.List;

public interface TaskSpecification {
    String getId();
	List<VehicleRequirements> getRequirements();
    void setRequirements(VehicleRequirements reqs);
}
