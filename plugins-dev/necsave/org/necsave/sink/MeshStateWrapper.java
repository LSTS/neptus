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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 29/08/2016
 */
package org.necsave.sink;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import info.necsave.msgs.MeshState;

/**
 * @author zp
 *
 */
public class MeshStateWrapper {

    public int numPlatforms, numAreas;
    
    public LinkedHashMap<Integer, Integer> allocatedAreas = new LinkedHashMap<>();
    public LinkedHashMap<Integer, Integer> currentAreas = new LinkedHashMap<>();
    public LinkedHashMap<Integer, Integer> allocationNumber = new LinkedHashMap<>();
    public ArrayList<Integer> scannedAreas = new ArrayList<>();
    public ArrayList<Integer> platforms = new ArrayList<>();
    
    
    public MeshStateWrapper(MeshState state) {
        ByteBuffer buffer = ByteBuffer.wrap(state.getState());
        
        allocatedAreas.clear();
        currentAreas.clear();
        allocationNumber.clear();
        scannedAreas.clear();
        
        numPlatforms = buffer.get() & 0xFF;
        numAreas = buffer.get() & 0xFF;
        
        for (int i = 0; i < numAreas; i++) {
            int platf = buffer.get() & 0xFF;
            if (platf == 0xFF)
                scannedAreas.add(i);
            else {
                if (!platforms.contains(platf))
                    platforms.add(platf);
                allocatedAreas.put(i, platf);
            }
        }
        
        Collections.sort(platforms);
        Collections.sort(scannedAreas);
        
        for (int platf : platforms)
            allocationNumber.put(platf, buffer.get() & 0xFF);
        
        for (int platf : platforms)
            currentAreas.put(platf, buffer.get() & 0xFF);        
    }
}
