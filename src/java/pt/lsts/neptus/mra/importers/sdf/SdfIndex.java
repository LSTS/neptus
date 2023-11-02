/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SdfIndex implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, PositionMap> positionMaps = new HashMap<>();

    public void addSubsystem(int subSystem) {
        if (!positionMaps.containsKey(subSystem)) {
            positionMaps.put(subSystem, new PositionMap());
        }
    }

    public Set<Integer> getSubSystemList() {
        return positionMaps.keySet();
    }

    public long getPageHeaderPosition(int subsystem, long timestamp) {
        return positionMaps.get(subsystem).getMap().get(timestamp);
    }

    public void addPageHeaderPositionToMap(long timestamp, long pageHeaderPosition, int subsystem) {
        positionMaps.get(subsystem).addPosition(timestamp, pageHeaderPosition);
    }

    public long getFirstTimestamp(int subsystem) {
        return positionMaps.get(subsystem).getFirstTimestamp();
    }

    public long getLastTimestamp(int subsystem) {
        return positionMaps.get(subsystem).getLastTimestamp();
    }

    public long getFirstTimestamp() {
        long firstTimestamp = Long.MAX_VALUE;
        for (PositionMap positionMap : positionMaps.values()) {
            firstTimestamp = Math.min(firstTimestamp, positionMap.getFirstTimestamp());
        }
        return firstTimestamp;
    }

    public long getLastTimestamp() {
        long lastTimestamp = -1;
        for (PositionMap positionMap : positionMaps.values()) {
            lastTimestamp = Math.max(lastTimestamp, positionMap.getLastTimestamp());
        }
        return lastTimestamp;
    }

    public Long[] getTimestampsAsArray(int subsytem) {
        Long[] timestamps = getPositionMap(subsytem).keySet().toArray(new Long[]{});
        Arrays.sort(timestamps);
        return timestamps;
    }

    private LinkedHashMap<Long, Long> getPositionMap(int subsystem) {
        return positionMaps.get(subsystem).getMap();
    }

}

class PositionMap implements Serializable {
    private long firstTimestamp = Long.MAX_VALUE;
    private long lastTimestamp = -1;
    // Map timestamps to header position
    private LinkedHashMap<Long, Long> map = new LinkedHashMap<>();

    public void addPosition(long timestamp, long headerPosition) {
        map.put(timestamp, headerPosition);

        if (timestamp < firstTimestamp) {
            firstTimestamp = timestamp;
        }
        else if (timestamp > lastTimestamp) {
            lastTimestamp = timestamp;
        }
    }

    public LinkedHashMap<Long, Long> getMap() {
        return map;
    }

    public long getFirstTimestamp() {
        return firstTimestamp;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }
}

