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
 * 27 Jul 2016
 */

package pt.lsts.neptus.plugins.mvplanning.jaxb.plans;

import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.types.mission.plan.PlanType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class to marshal and unmarshal, to and from,
 * PlanTask's
 * @author tsmarques
 * @date 27/06/16
 */
@XmlRootElement(name="PlanTask")
@XmlAccessorType(XmlAccessType.NONE)
public class PlanTaskJaxb {
    @XmlElement(name = "PlanId")
    public String planId;

    @XmlJavaTypeAdapter(PlanTypeJaxbAdapter.class)
    public PlanType plan;

    @XmlElement
    public Profile planProfile;

    @XmlElement(name = "Timestamp")
    public double timestamp;

    @XmlElement(name = "md5")
    public byte[] md5;

    @XmlElement(name = "Completion")
    public double completion;

    @XmlElement(name = "TaskType")
    public String taskType;

    public PlanTaskJaxb() {

    }

    public PlanTaskJaxb(String id, PlanType plan, Profile planProfile, double timestamp, byte[] md5, double completion, String taskType) {
        this.planId = id;
        this.plan = plan;
        this.planProfile = planProfile;
        this.timestamp = timestamp;
        this.md5 = md5;
        this.completion = completion;
        this.taskType = taskType;
    }

    public PlanTaskJaxb(PlanTask ptask) {
        this.planId = ptask.getPlanId();
        this.plan = ptask.asPlanType();
        this.planProfile = ptask.getProfile();
        this.timestamp = ptask.getTimestamp();
        this.md5 = ptask.getMd5();
        this.completion = ptask.getCompletion();
        this.taskType = ptask.getTaskTypeAsString();
    }
}
