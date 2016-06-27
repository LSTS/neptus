package pt.lsts.neptus.plugins.mvplanning.tests;

import pt.lsts.neptus.plugins.mvplanning.jaxb.PlanTaskMarshaler;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.types.mission.MissionType;

import javax.xml.bind.JAXBException;
import java.util.List;

/**
 * @author tsmarques
 * @date 27/06/16
 */
public class PlanTaskMarshallingTests {
    public static void main(String[] args) {
        final PlanTaskMarshaler pTaskMarsh = new PlanTaskMarshaler();
        try {
            System.out.println("* Testing plan task marshaler:");
            /* test unmarshaling */
            List<PlanTask> tasks = pTaskMarsh.unmarshalAll(new MissionType());
            if(tasks.isEmpty())
                System.out.println("** No plans to marshal/unmarshal");
            else { /* test marshaling */
                for(PlanTask task : tasks) {
                    System.out.println("** Id: " + task.getPlanId());
                    System.out.println("** Type: " + task.getTaskTypeAsString());
                }
                pTaskMarsh.marshalAll(tasks);
            }
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
