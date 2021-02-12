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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Nov 26, 2012
 */
package pt.lsts.neptus.console.events;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
public class ConsoleEventPositionEstimation {

    public enum ESTIMATION_TYPE {
        LBL_RANGES,
        OTHER
    }

    protected ESTIMATION_TYPE type;
    protected Object source;
    protected EstimatedState estimation;

    public ConsoleEventPositionEstimation(Object source, ESTIMATION_TYPE type, LocationType loc) {
        this(source, type, 
                EstimatedState.create("lat", loc.getLatitudeRads(), "lon",
                loc.getLongitudeRads(), "depth", loc.getDepth(), "x", loc.getOffsetNorth(), "y",
                loc.getOffsetEast(), "z", loc.getOffsetDown())
        );
    }

    public ConsoleEventPositionEstimation(Object source, ESTIMATION_TYPE type, EstimatedState state) {
        this.estimation = state;
        this.source = source;
        this.type = type;
    }

    /**
     * @return the type
     */
    public ESTIMATION_TYPE getType() {
        return type;
    }

    /**
     * @return the source
     */
    public Object getSource() {
        return source;
    }

    /**
     * @return the estimation
     */
    public EstimatedState getEstimation() {
        return estimation;
    }
}
