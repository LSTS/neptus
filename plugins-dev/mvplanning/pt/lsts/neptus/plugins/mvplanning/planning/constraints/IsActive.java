package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;

/**
 * Created by tsmarques on 21/07/16.
 */
public class IsActive extends TaskConstraint {
    @Override
    public boolean isValidated(Object... value) {
        ImcSystem sys = ImcSystemsHolder.getSystemWithName((String) value[0]);
        return sys != null && sys.isActive();
    }

    @Override
    public NAME getName() {
        return NAME.IsActive;
    }
}
