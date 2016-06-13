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
package pt.lsts.neptus.plugins.mvplanning.jaxb.plans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import pt.lsts.neptus.types.mission.plan.PlanType;


/**
 * @author tsmarques
 *
 */
public class PlanTypeJaxbAdapter extends XmlAdapter<XmlPlanType, PlanType>{
    @Override
    public PlanType unmarshal(XmlPlanType v) throws Exception {
        return new PlanType(v.planTypeAsXml, null);
    }

    @Override
    public XmlPlanType marshal(PlanType plan) throws Exception {
        XmlPlanType xmlPlan = new XmlPlanType();

        xmlPlan.planTypeAsXml = plan.asXML();
        return xmlPlan;
    }
}

class XmlPlanType {
    @XmlElement(name = "PlanType")
    public String planTypeAsXml;
}