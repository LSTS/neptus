package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;
import pt.lsts.neptus.plugins.mvplanning.monitors.VehicleAwareness;

/**
 * Created by tsmarques on 21/07/16.
 */
public class IsAvailable extends TaskConstraint {

    public IsAvailable() {

    }

    public IsAvailable(String pddlSpec) {

    }

    @Override
    public boolean isValidated(Object... value) {
        VehicleAwareness.VEHICLE_STATE state = (VehicleAwareness.VEHICLE_STATE) value[0];
        return state == VehicleAwareness.VEHICLE_STATE.Available;
    }

    @Override
    public NAME getName() {
        return NAME.IsAvailable;
    }
}
