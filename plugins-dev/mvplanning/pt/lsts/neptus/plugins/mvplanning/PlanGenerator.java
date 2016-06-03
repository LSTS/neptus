package pt.lsts.neptus.plugins.mvplanning;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.planning.algorithm.CoverageArea;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.NameNormalizer;

public class PlanGenerator {
    private final ReadWriteLock RW_LOCK = new ReentrantReadWriteLock();

    private PlanAllocator planAloc;
    private ConsoleAdapter console;
    /* Map decomposition needed for some algorithms, e.g, A-star */
    private GridArea operationalArea;

    public PlanGenerator(PlanAllocator planAloc, ConsoleAdapter console) {
        this.planAloc = planAloc;
        this.console = console;
    }

    public PlanGenerator() {

    }

    public void setOperationalArea(GridArea opArea) {
        RW_LOCK.writeLock().lock();
        operationalArea = opArea;
        RW_LOCK.writeLock().unlock();
    }

    public void generatePlan(Profile planProfile, Object obj) {
        if(obj.getClass().getSimpleName().equals("PlanType")) {
            PlanType pType = (PlanType) obj;
            planAloc.allocate(new PlanTask(pType.getId(), pType, planProfile));
        }
        else {
        }
    }

    /**
     * Given an area generate one or more plans to cover it
     * */
    public List<PlanType> generateCoverageArea(Profile planProfile, GridArea areaToCover) {
        String id = "coverage_" + NameNormalizer.getRandomID();

        CoverageArea covArea = new CoverageArea(id, planProfile, areaToCover, console.getMission());
        List<PlanType> plans = covArea.asPlanType();

        if(!plans.isEmpty()) {
            int i = 0;
            for(PlanType planSpec : plans) {
                planAloc.allocate(new PlanTask(id + "_" + i, planSpec, planProfile));
                i++;
            }
        }
        else
            NeptusLog.pub().warn("No plans were generated");

        return covArea.asPlanType();
    }

    /**
     * Adds a safe path from the start location to the first
     * waypoint of the given plan, and from the last waypoint
     * of the plan to the end location.
     * */
    public PlanSpecification closePlan(PlanTask ptask, LocationType start, LocationType end) {
        PlanType plan = ptask.asPlanType();
        GraphType planGraph = plan.getGraph();

        LocationType planFirstLocation = ((LocatedManeuver) planGraph.getManeuver(planGraph.getInitialManeuverId())).getManeuverLocation();
        LocationType planLastLocation = ((LocatedManeuver) planGraph.getLastManeuver()).getManeuverLocation();

        RW_LOCK.readLock().lock();

        List<ManeuverLocation> initialSafePath = operationalArea.getShortestPath(start, planFirstLocation);
        List<ManeuverLocation> endSafePath = operationalArea.getShortestPath(planLastLocation, end);

        RW_LOCK.readLock().unlock();


        FollowPath initialFollowPath = new FollowPath();
        Vector<double[]> offsets = new Vector<>();
        for(ManeuverLocation loc : initialSafePath) {
            double[] newPoint = new double[4];
            double[] pOffsets = loc.getOffsetFrom(start);

            newPoint[0] = pOffsets[0];
            newPoint[1] = pOffsets[1];
            newPoint[2] = pOffsets[2];

            offsets.add(pOffsets);
        }
        initialFollowPath.setOffsets(offsets);
        initialFollowPath.setId("initial_safepath");


        FollowPath endFollowPath = new FollowPath();
        offsets = new Vector<>();
        for(ManeuverLocation loc : endSafePath) {
            double[] newPoint = new double[4];
            double[] pOffsets = loc.getOffsetFrom(start);

            newPoint[0] = pOffsets[0];
            newPoint[1] = pOffsets[1];
            newPoint[2] = pOffsets[2];

            offsets.add(pOffsets);
        }
        endFollowPath.setOffsets(offsets);
        endFollowPath.setId("end_safepath");

        plan.getGraph().addManeuver(initialFollowPath);
        plan.getGraph().setInitialManeuver(initialFollowPath.id);
        plan.getGraph().addManeuverAtEnd(endFollowPath);

        return (PlanSpecification) IMCUtils.generatePlanSpecification(plan);
    }



}
