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
 * Author: Manuel R.
 * Oct 21, 2014
 */
package pt.lsts.neptus.mra.importers.sdf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SdfIndex implements Serializable {
    private static final long serialVersionUID = 1L;

    long firstTimestampHigh = -1;
    long lastTimestampHigh = -1;
    
    long firstTimestampLow = -1;
    long lastTimestampLow = -1;

    long numberOfPackets = -1;
    
    boolean hasHigh = false;
    boolean hasLow = false;
    
    LinkedHashMap<Long, ArrayList<Long>> positionMapHigh = new LinkedHashMap<Long, ArrayList<Long>>();
    LinkedHashMap<Long, ArrayList<Long>> positionMapLow = new LinkedHashMap<Long, ArrayList<Long>>();
    
    ArrayList<Integer> frequenciesList = new ArrayList<Integer>();
    ArrayList<Integer> subSystemsList = new ArrayList<Integer>();
    
}

