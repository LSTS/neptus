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
 * 21 Jul 2016
 */

package pt.lsts.neptus.plugins.mvplanning.planning.constraints;

import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;

public class BatteryLevel extends TaskConstraint {
    public static enum OPERATION {
        Gthan,
        Gequal,
        Lthen,
        Lequal,
        Equal,
        Interval;
    }

    private double constraintValue;
    private double constraintValue2;
    private OPERATION op;

    public BatteryLevel(String pddlSpec) {

    }

    public BatteryLevel(double constraintValue, OPERATION op) {
        this.constraintValue = constraintValue;
        constraintValue2 = Double.MAX_VALUE;
        this.op = op;
    }

    public BatteryLevel(double minVal, double maxVal) {
        constraintValue = minVal;
        constraintValue2 = maxVal;
        op = OPERATION.Interval;
    }

    @Override
    public NAME getName() {
        return NAME.BatteryLevel;
    }

    @Override
    public <T> boolean isValidated(T... value) {
        double v = (Double) value[0];

        if(op == OPERATION.Equal)
            return v == constraintValue;
        else if(op == OPERATION.Gthan)
            return v > constraintValue;
        else if(op == OPERATION.Gequal)
            return v >= constraintValue;
        else if(op == OPERATION.Lthen)
            return v < constraintValue;
        else if(op == OPERATION.Lequal)
            return v <= constraintValue;
        else
            return v > constraintValue && v < constraintValue2;
    }
}
