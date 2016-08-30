package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;

public class BatteryLevel extends TaskConstraint {
    public static enum OPERATION {
        Gthan,
        Gequal,
        Lthen,
        Lequal,
        Equal,
        Interval;
    }

    private double constraintValue;
    private double constraintValue2;
    private OPERATION op;

    public BatteryLevel(String pddlSpec) {

    }

    public BatteryLevel(double constraintValue, OPERATION op) {
        this.constraintValue = constraintValue;
        constraintValue2 = Double.MAX_VALUE;
        this.op = op;
    }

    public BatteryLevel(double minVal, double maxVal) {
        constraintValue = minVal;
        constraintValue2 = maxVal;
        op = OPERATION.Interval;
    }

    @Override
    public NAME getName() {
        return NAME.BatteryLevel;
    }

    @Override
    public <T> boolean isValidated(T... value) {
        double v = (Double) value[0];

        if(op == OPERATION.Equal)
            return v == constraintValue;
        else if(op == OPERATION.Gthan)
            return v > constraintValue;
        else if(op == OPERATION.Gequal)
            return v >= constraintValue;
        else if(op == OPERATION.Lthen)
            return v < constraintValue;
        else if(op == OPERATION.Lequal)
            return v <= constraintValue;
        else
            return v > constraintValue && v < constraintValue2;
    }
}
