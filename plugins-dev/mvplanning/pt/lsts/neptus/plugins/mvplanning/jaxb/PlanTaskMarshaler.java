/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 13 Jun 2016
 */
package pt.lsts.neptus.plugins.mvplanning.jaxb;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.jaxb.plans.PlanTaskJaxb;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.CoverageArea;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.NeptusPlan;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.VisitPoint;
import pt.lsts.neptus.types.mission.MissionType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author tsmarques
 *
 */
public class PlanTaskMarshaler {
    public final static String XML_PLAN_DIR = "conf/mvplanning/tasks/";

    public PlanTaskMarshaler() {
        File xmlDir = new File(XML_PLAN_DIR);
        if(!xmlDir.exists()) {
            xmlDir.mkdir();
            NeptusLog.pub().info(XML_PLAN_DIR + " path doesn't exist. Creating...");
        }
    }

    public void marshalAll(List<PlanTask> plans) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(PlanTaskJaxb.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        for(PlanTask task : plans) {
            NeptusLog.pub().info("Marshaling PlanTask " + task.getPlanId());

            if(task.getTaskType() != PlanTask.TASK_TYPE.SAFETY) {
                PlanTaskJaxb taskJaxb = new PlanTaskJaxb(task);
                jaxbMarshaller.marshal(taskJaxb, System.out);
                jaxbMarshaller.marshal(taskJaxb, new File(XML_PLAN_DIR + task.getPlanId() + ".xml"));
            }
        }
    }

    public List<PlanTask> unmarshalAll(MissionType mtype) throws JAXBException {
        List<PlanTask> plans = new ArrayList<>();
        JAXBContext jaxbContext = JAXBContext.newInstance(PlanTaskJaxb.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        File dir = new File(XML_PLAN_DIR);
        for(File file : dir.listFiles()) {
            PlanTaskJaxb taskJaxb =(PlanTaskJaxb) jaxbUnmarshaller.unmarshal(file);
            PlanTask plan = jaxbToPlanTask(taskJaxb);

            plan.setMissionType(mtype);
            plans.add(plan);
        }
        return plans;
    }

    /**
     * Given a task type and data, PlanTaskJaxb, from
     * an xml file, instatiates an object of type PlanTask
     * */
    private PlanTask jaxbToPlanTask(PlanTaskJaxb taskJaxb) {
        try {
            PlanTask.TASK_TYPE type = PlanTask.string2TaskType(taskJaxb.taskType);
            if (type == PlanTask.TASK_TYPE.COVERAGE_AREA)
                return new CoverageArea(taskJaxb.planId, taskJaxb.plan, taskJaxb.planProfile);
            else if (type == PlanTask.TASK_TYPE.VISIT_POINT)
                return new VisitPoint(taskJaxb.planId, taskJaxb.plan, taskJaxb.planProfile);
            else
                return new NeptusPlan(taskJaxb.planId, taskJaxb.plan, taskJaxb.planProfile);
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
            return new NeptusPlan(taskJaxb.planId, taskJaxb.plan, taskJaxb.planProfile);
        }
    }
}
