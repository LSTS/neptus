package pt.lsts.neptus.plugins.mvplanning;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.plugins.mvplanning.utils.jaxb.Profile;

public class PlanGenerator {
    private PlanAllocator planAloc;
    public PlanGenerator(PlanAllocator planAloc) {
        this.planAloc = planAloc;
    }

    public PlanGenerator() {

    }

    public void generatePlan(Profile planProfile, Object obj) {
        if(obj.getClass().getSimpleName().equals("PlanSpecification"))
            planAloc.allocate(new PlanTask((PlanSpecification) obj, planProfile, 0));
        else {
            System.out.println("[mvplanning/PlanGenerator]: Generating a plan");
        }
    }
}
