package pt.lsts.neptus.plugins.mvplanning.interfaces;

import pt.lsts.neptus.plugins.mvplanning.PlanAllocator;
import pt.lsts.neptus.plugins.mvplanning.PlanGenerator;

/**
 * Type of monitor that needs to interact
 * with the main modules of MvPlanning, namely
 * the plans' allocator and generator.
 * @author tsmarques
 * @date 21/06/16
 */
public abstract class AbstractSupervisor {
    protected ConsoleAdapter console;
    protected PlanAllocator planAlloc;
    protected PlanGenerator pGen;

    public AbstractSupervisor() {

    }

    public AbstractSupervisor(ConsoleAdapter console, PlanAllocator planAlloc, PlanGenerator pGen) {
        this.console = console;
        this.planAlloc = planAlloc;
        this.pGen = pGen;
    }
}
