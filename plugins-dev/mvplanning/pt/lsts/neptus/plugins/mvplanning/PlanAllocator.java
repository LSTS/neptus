package pt.lsts.neptus.plugins.mvplanning;

import pt.lsts.imc.PlanDB;
import pt.lsts.imc.PlanDB.OP;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.mvplanning.utils.VehicleAwareness;

/* Sends plans to the vehicles.
 Also send 'execution commands'*/
public class PlanAllocator {
    private VehicleAwareness vawareness;
    public PlanAllocator(VehicleAwareness vawareness) {
        this.vawareness = vawareness;
    }
 
    public void allocate(PlanTask pTask) {
        /* allocate plans to vehicle */
        System.out.println("[mvplanning/PlanAlocater]: Allocating plan");
    }
}
