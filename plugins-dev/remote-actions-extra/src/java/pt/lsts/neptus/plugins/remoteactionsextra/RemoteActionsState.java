/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 26/10/2023
 */
package pt.lsts.neptus.plugins.remoteactionsextra;

import java.util.LinkedHashMap;
import java.util.Map;

class RemoteActionsState {
    public final Map<String, Integer> extraButtonActionsMap = new LinkedHashMap<>();
    public final Map<String, Double> extraAxisActionsMap = new LinkedHashMap<>();
    public final Map<String, Boolean> extraActionsLocksMap = new LinkedHashMap<>();


    public boolean decimalAxis = RemoteActionsExtra.DEFAULT_AXIS_DECIMAL_VAL;

    public RemoteActionsState() {
    }

    public boolean isEmpty() {
        return extraButtonActionsMap.isEmpty() && extraAxisActionsMap.isEmpty();
    }

    public void resetWith(RemoteActionsState other) {
        this.extraButtonActionsMap.clear();
        this.extraButtonActionsMap.putAll(other.extraButtonActionsMap);
        this.extraAxisActionsMap.clear();
        this.extraAxisActionsMap.putAll(other.extraAxisActionsMap);
        this.extraActionsLocksMap.clear();
        this.extraActionsLocksMap.putAll(other.extraActionsLocksMap);

        this.decimalAxis = other.decimalAxis;
    }

    public void reset() {
        this.extraButtonActionsMap.clear();
        this.extraAxisActionsMap.clear();
        this.extraActionsLocksMap.clear();
        this.decimalAxis = RemoteActionsExtra.DEFAULT_AXIS_DECIMAL_VAL;
    }

    public void changeButtonActionValue(String action, int val) {
        if (extraButtonActionsMap.containsKey(action)) {
            extraButtonActionsMap.put(action, val);
        }
    }

    public void changeAxisActionValue(String action, double val) {
        if (extraAxisActionsMap.containsKey(action)) {
            extraAxisActionsMap.put(action, val);
        }
    }
}
