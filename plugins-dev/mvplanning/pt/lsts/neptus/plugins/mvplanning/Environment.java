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
 * 14 Mar 2016
 */
package pt.lsts.neptus.plugins.mvplanning;

import java.util.ArrayList;

import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.MapChangeListener;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;

/**
 * @author tsmarques
 *
 */
public class Environment implements MapChangeListener {
    private ConsoleAdapter console;
    private ArrayList<AbstractElement> consoleObstacles;

    public Environment() {
        console = null;
        consoleObstacles = new ArrayList<>();
    }

    public Environment(ConsoleAdapter console) {
        this.console = console;
        this.console.registerToEventBus(this);
        consoleObstacles = new ArrayList<>();

        this.console.getMapGroup().addChangeListener(this);
    }

    public boolean hasObstacle(LocationType lt) {
        return consoleObstacles
                .stream()
                .anyMatch((s) -> s.containsPoint(lt, null));
    }

    public void addObstacle(AbstractElement obstacle) {
        consoleObstacles.add(obstacle);
    }

    @Override
    public void mapChanged(MapChangeEvent mapChange) {
        if(mapChange != null) {
            AbstractElement object = mapChange.getChangedObject();
            if(object.isObstacle())
                addObstacle(object);
        }
    }
}
