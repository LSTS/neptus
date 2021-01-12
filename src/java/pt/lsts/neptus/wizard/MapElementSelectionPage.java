/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JScrollPane;

import pt.lsts.neptus.gui.CheckboxList;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.AbstractElement.ELEMENT_TYPE;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.MissionType;

/**
 * @author zp
 *
 */
public class MapElementSelectionPage extends WizardPage<AbstractElement> {

    private static final long serialVersionUID = 1L;
    private CheckboxList component;
    private MissionType mission;
    private ELEMENT_TYPE[] types;

    public MapElementSelectionPage(MissionType mission, ELEMENT_TYPE... types) {
        this.types = types;
        setMission(mission);
    }

    public void setMission(MissionType mission) {
        this.mission = mission;
        removeAll();
        List<ELEMENT_TYPE> validTypes = Arrays.asList(types);
        
        List<String> objs = Stream.of(MapGroup.getMapGroupInstance(mission).getAllObjects())
                .filter(p -> validTypes.isEmpty() || validTypes.contains(p.getElementType()))
                .map(p -> p.getId()).sorted()
                .collect(Collectors.toList());

        component = CheckboxList.getInstance(objs.toArray(new String[0]));

        setLayout(new BorderLayout());
        add(new JScrollPane(component), BorderLayout.CENTER);

        revalidate();
    }

    @Override
    public String getTitle() {
        return "Select map object";
    }

    @Override
    public AbstractElement getSelection() throws InvalidUserInputException {
        if (component.getSelectedIndices().length != 1)
            throw new InvalidUserInputException("You must select one object.");
        return MapGroup.getMapGroupInstance(mission).getMapObjectsByID(component.getSelectedStrings()[0])[0];
    }

}
