package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;

public class BatteryLevel extends TaskConstraint {
    public static enum OPERATION {
        GTHAN,
        GEQUAL,
        LTHEN,
        LEQUAL,
        EQUAL,
        BETWEEN;
    }

    private double constraintValue;
    private double constraintValue2;
    private OPERATION op;

    public BatteryLevel(double constraintValue, OPERATION op) {
        this.constraintValue = constraintValue;
        constraintValue2 = Double.MAX_VALUE;
        this.op = op;
    }

    public BatteryLevel(double minVal, double maxVal) {
        constraintValue = minVal;
        constraintValue2 = maxVal;
        op = OPERATION.BETWEEN;
    }

    @Override
    public boolean isValidated(Object value) {
        double v = (double) value;

        if(op == OPERATION.EQUAL)
            return v == constraintValue;
        else if(op == OPERATION.GTHAN)
            return v > constraintValue;
        else if(op == OPERATION.GEQUAL)
            return v >= constraintValue;
        else if(op == OPERATION.LTHEN)
            return v < constraintValue;
        else if(op == OPERATION.LEQUAL)
            return v <= constraintValue;
        else
            return v > constraintValue && v < constraintValue2;
    }
}
