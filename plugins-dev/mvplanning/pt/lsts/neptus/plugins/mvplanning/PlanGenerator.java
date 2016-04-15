package pt.lsts.neptus.plugins.mvplanning;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.PlanTask;
import pt.lsts.neptus.types.mission.plan.PlanType;

public class PlanGenerator {
    private PlanAllocator planAloc;
    public PlanGenerator(PlanAllocator planAloc) {
        this.planAloc = planAloc;
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
            NeptusLog.pub().info("Generating a plan");
        }
    }
}
