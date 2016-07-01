package pt.lsts.neptus.plugins.mvplanning.planning.tasks;

import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Task used to move the vehicle to a safe position,
 * where it stays in station-keeping.
 * <p>
 * This task is not marshalled/unmarshalled by
 * {@link pt.lsts.neptus.plugins.mvplanning.jaxb.PlanTaskMarshaler}
 * @author tsmarques
 * @date 01/07/16
 */
public class ToSafety extends PlanTask {
    private LocationType safeLoc ;
    private String vehicle;
    public ToSafety(LocationType safeLoc, String vehicle) {
        super("s_" + vehicle, null);
        this.safeLoc = safeLoc;
        this.vehicle = vehicle;
    }

    @Override
    public TASK_TYPE getTaskType() {
        return TASK_TYPE.SAFETY;
    }

    public String getVehicle() {
        return vehicle;
    }
}
