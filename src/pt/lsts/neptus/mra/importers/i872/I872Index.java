/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Renato Campos
 * 10 Dec 2018
 */
package pt.lsts.neptus.mra.importers.i872;

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeSet;

/**
 * @author ineeve
 *
 */
public class I872Index implements Serializable {
    
    private static final long serialVersionUID = -9220474691062798631L;
    private HashMap<Long,Long> timestampToPosition;
    private TreeSet<Long> timestampsSet;

    public I872Index() {
        timestampToPosition = new HashMap<Long, Long>();
        timestampsSet = new TreeSet<Long>();
    }
    
    public void addPing(Long timestamp, Long position) {
        timestampToPosition.put(timestamp, position);
        timestampsSet.add(timestamp);
    }
    public long getPositionOfPing(Long timestamp) {
        long nextTimestamp = timestampsSet.ceiling(timestamp);
        return timestampToPosition.get(nextTimestamp);
    }

    public long getFirstTimestamp() {
        return timestampsSet.first();
    }
    public long getLastTimestamp() {
        return timestampsSet.last();
    }
}
