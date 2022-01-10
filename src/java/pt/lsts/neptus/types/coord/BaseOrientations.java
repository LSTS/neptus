/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zepinto
 * Author: pdias
 * 26/07/2017 (refactor to own file)
 * 08/06/2009
 */
package pt.lsts.neptus.types.coord;

import pt.lsts.neptus.util.AngleUtils;

public enum BaseOrientations {
    North,
    NorthEast,
    East,
    SouthEast,
    South,
    SouthWest,
    West,
    NorthWest;
    
    public String getAbbrev() {
        return this.name().replaceAll("[a-z]", "");
    }

    /**
     * @param baseOrientationRadians
     * @return
     */
    public static BaseOrientations convertToBaseOrientationFromRadians(double baseOrientationRadians) {
        double headingDegrees = Math.toDegrees(baseOrientationRadians);
        return convertToBaseOrientationFromDegrees(headingDegrees);
    }

    /**
     * @param baseOrientationDegrees
     * @return
     */
    public static BaseOrientations convertToBaseOrientationFromDegrees(double baseOrientationDegrees) {
        double headingDegrees = AngleUtils.nomalizeAngleDegrees360(baseOrientationDegrees);
        if(headingDegrees >= -22.5 && headingDegrees <= 22.5)
            return North;
        else if(headingDegrees > 22.5 && headingDegrees < 67.5)
            return NorthEast;
        else if(headingDegrees >= 67.5 && headingDegrees <= 112.5)
            return East;
        else if(headingDegrees > 112.5 && headingDegrees < 157.5)
            return SouthEast;
        else if(headingDegrees >= 157.5 && headingDegrees <= 202.5)
            return South;
        else if(headingDegrees > 202.5 && headingDegrees < 247.5)
            return SouthWest;
        else if(headingDegrees >= 247.5 && headingDegrees <= 292.5)
            return West;
        else if(headingDegrees > 292.5 && headingDegrees < 337.5)
            return NorthWest;
        return North;
    }
}