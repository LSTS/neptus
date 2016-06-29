package pt.lsts.neptus.plugins.mvplanning.events;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.systems.external.ExternalSystem;

/**
 * Event triggered when an external system gets
 * too close to a vehicle
 * @author tsmarques
 * @date 20/06/16
 */
public class MvPlanningNoSafeDistanceEvent {
    private ImcSystem sys;

    public MvPlanningNoSafeDistanceEvent(ImcSystem sys) {
        this.sys = sys;
    }

    public ImcSystem getImcVehicle() {
        return sys;
    }
}
