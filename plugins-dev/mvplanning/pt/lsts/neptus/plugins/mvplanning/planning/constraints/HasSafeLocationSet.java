package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;

/**
 * Created by tsmarques on 21/07/16.
 */
public class HasSafeLocationSet extends TaskConstraint {

    public HasSafeLocationSet() {

    }

    public HasSafeLocationSet(String pddlSpec) {

    }

    @Override
    public NAME getName() {
        return NAME.HasSafeLocationSet;
    }
    @Override
    public <T> boolean isValidated(T... value) {
        return (Boolean) value[0];
    }
}
