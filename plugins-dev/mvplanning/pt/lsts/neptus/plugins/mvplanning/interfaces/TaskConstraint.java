package pt.lsts.neptus.plugins.mvplanning.interfaces;

/**
 * Created by tsmarques on 21/07/16.
 */
public abstract class TaskConstraint {
    public enum NAME {
        BatteryLevel,
        HasPayload,
        HasSafeLocationSet,
        HasTcpOn,
        IsActive,
        IsAvailable;
    }
    public abstract <T> boolean isValidated(T... value);
    public abstract NAME getName();
}
