/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 13/05/2016
 */
package pt.lsts.neptus.wizard;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JScrollPane;

import pt.lsts.neptus.gui.CheckboxList;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanSelectionPage extends WizardPage<Collection<PlanType>> {

    private static final long serialVersionUID = 1L;
    private CheckboxList component;
    private boolean multiSelect;
    private MissionType mission;
    
    public PlanSelectionPage(MissionType mission, boolean multipleSelection) {
        this.multiSelect = multipleSelection;
        this.mission = mission;
        component = CheckboxList.getInstance(mission.getIndividualPlansList().keySet().toArray(new String[0]));
        setLayout(new BorderLayout());
        add(new JScrollPane(component), BorderLayout.CENTER);
    }
    
    public void setMission(MissionType mission) {
        this.mission = mission;
        removeAll();
        component = CheckboxList.getInstance(mission.getIndividualPlansList().keySet().toArray(new String[0]));
        setLayout(new BorderLayout());
        add(new JScrollPane(component), BorderLayout.CENTER);
        revalidate();
    }
    
    @Override
    public String getTitle() {
        return "Select plan"+(multiSelect? "s" : "");
    }

    @Override
    public Collection<PlanType> getSelection() throws InvalidUserInputException {
        ArrayList<PlanType> result = new ArrayList<>();
        for (String s : component.getSelectedStrings())
            result.add(mission.getIndividualPlansList().get(s));
        
        if (result.size() == 0)
            throw new InvalidUserInputException("You must select a plan.");
        if (result.size() > 1 && !multiSelect)
            throw new InvalidUserInputException("You must select only one plan.");
        
        return result;
    }

}
