package pt.lsts.neptus.plugins.mvplanning.planning.tasks;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.types.coord.LocationType;

import java.util.List;
import java.util.Vector;

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
    private LocationType fromLoc;
    private LocationType toLoc;
    private String vehicle;
    public ToSafety(LocationType from, LocationType to, String vehicleId) {
        super("s_" + vehicleId, null);
        fromLoc = from;
        toLoc = to;
        vehicle = vehicleId;
    }

    public static Maneuver getDefaultManeuver(LocationType loc) {
        StationKeeping sk = new StationKeeping();
        sk.setId("ToSafety default maneuver");
        sk.setManeuverLocation(new ManeuverLocation(loc));
        sk.setRadius(10);
        sk.setDuration(-1);
        sk.setSpeed(1);
        sk.setSpeedUnits("m/s");
        return sk;
    }

    @Override
    public TASK_TYPE getTaskType() {
        return TASK_TYPE.SAFETY;
    }

    @Override
    public ManeuverLocation getLastLocation() {
        return ((LocatedManeuver) plan
                .getGraph()
                .getLastManeuver())
                .getManeuverLocation();
    }

    public String getVehicle() {
        return vehicle;
    }

    /**
     * Returns an array with the location where the vehicle
     * is and the location where it should move to
     * */
    public LocationType[] getLocations() {
        return new LocationType[] {fromLoc, toLoc};
    }

    public FollowPath buildSafePath(List<ManeuverLocation> safePath) {
        FollowPath safeFollowPath = new FollowPath();
        Vector<double[]> offsets = new Vector<>();
        for(ManeuverLocation loc : safePath) {
            double[] newPoint = new double[4];
            double[] pOffsets = loc.getOffsetFrom(fromLoc);

            newPoint[0] = pOffsets[0];
            newPoint[1] = pOffsets[1];
            newPoint[2] = pOffsets[2];
            newPoint[3] = 0;

            offsets.add(newPoint);
        }

        ManeuverLocation manLoc = new ManeuverLocation(fromLoc);
        manLoc.setZ(toLoc.getAllZ());
        /* TODO set according to profile's parameters */
        manLoc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);

        safeFollowPath.setOffsets(offsets);
        /* TODO set according to plan profile */
        safeFollowPath.setSpeed(1.3);
        safeFollowPath.setSpeedUnits("m/s");
        safeFollowPath.setManeuverLocation(manLoc);

        return safeFollowPath;
    }
}
