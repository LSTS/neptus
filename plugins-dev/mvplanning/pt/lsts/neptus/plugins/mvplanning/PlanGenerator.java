package pt.lsts.neptus.plugins.mvplanning;

import java.util.List;
import java.util.Vector;

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
        operationalArea = opArea;
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
}
