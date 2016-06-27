package pt.lsts.neptus.plugins.mvplanning.tests;

import pt.lsts.neptus.plugins.mvplanning.jaxb.PlanTaskMarshaler;
import pt.lsts.neptus.plugins.mvplanning.planning.PlanTask;
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
            /* test unrmashaling */
            List<PlanTask> tasks = pTaskMarsh.unmarshalAll(new MissionType());
            /* test marshaling */
            pTaskMarsh.marshalAll(tasks);
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
