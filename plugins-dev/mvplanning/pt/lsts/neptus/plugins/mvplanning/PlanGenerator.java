package pt.lsts.neptus.plugins.mvplanning;

import java.util.List;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanning.planning.algorithm.CoverageArea;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.NameNormalizer;

public class PlanGenerator {
    private PlanAllocator planAloc;
    private ConsoleAdapter console;

    public PlanGenerator(PlanAllocator planAloc, ConsoleAdapter console) {
        this.planAloc = planAloc;
        this.console = console;
    }

    public PlanGenerator() {

    }

    public void generatePlan(Profile planProfile, Object obj) {
        if(obj.getClass().getSimpleName().equals("PlanType")) {
            PlanType pType = (PlanType) obj;
            PlanSpecification pSpec = (PlanSpecification) IMCUtils.generatePlanSpecification(pType);
            planAloc.allocate(new PlanTask(pType.getId(), pSpec, planProfile, pType.asIMCPlan().payloadMD5()));
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
        List<PlanSpecification> plans = covArea.asPlanSpecification();

        if(!plans.isEmpty()) {
            int i = 0;
            for(PlanSpecification planSpec : plans) {
                planAloc.allocate(new PlanTask(id + "_" + i, planSpec, planProfile, planSpec.payloadMD5()));
                i++;
            }
        }
        else
            NeptusLog.pub().warn("No plans were generated");

        return covArea.asPlanType();
    }
}
