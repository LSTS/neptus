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
 * Author: zp
 * Dec 3, 2015
 */
package pt.lsts.neptus.util.sidescan;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;

import pt.lsts.imc.UamRxFrame;
import pt.lsts.imc.UamTxStatus;
import pt.lsts.imc.UamTxStatus.VALUE;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mra.importers.IMraLogGroup;

/**
 * @author zp
 *
 */
public class AcousticCommsFilter {

    private static long millisBeforeIP = 0;
    private static long millisAfterDONE = 0;
    private LinkedList<Pair<Date, Date>> corruptedTime = new LinkedList<>();
    
    Date start = null;
    
    public AcousticCommsFilter(IMraLogGroup source) {
        for (UamTxStatus msg : source.getLsfIndex().getIterator(UamTxStatus.class)) {
            if (msg.getValue() == VALUE.IP) {
                start = msg.getDate();                                
            }
            else if (msg.getValue() == VALUE.DONE || msg.getValue() == VALUE.FAILED) {
                if (start != null) {
                    synchronized (corruptedTime) {
                        corruptedTime.add(new Pair<Date, Date>(new Date(start.getTime() - millisBeforeIP), 
                                new Date(msg.getDate().getTime() + millisAfterDONE)));                        
                    }
                    start = null;
                }
            }
        }
        
        for (UamRxFrame msg : source.getLsfIndex().getIterator(UamRxFrame.class)) {
            synchronized (corruptedTime) {
                corruptedTime.add(new Pair<Date, Date>(new Date(msg.getTimestampMillis()-200), 
                        new Date(msg.getTimestampMillis() + 400)));                        
            }            
        }
        
        // sort everything by start time
        Collections.sort(corruptedTime, new Comparator<Pair<Date, Date>>() {
            @Override
            public int compare(Pair<Date, Date> o1, Pair<Date, Date> o2) {
                return o1.first().compareTo(o2.first());
            }
        });
    }
    
    public boolean isDataValid(Date date) {
        synchronized (corruptedTime) {
            for (Pair<Date, Date> p : corruptedTime) {
                // if it starts after is already past this time
                if (p.first().after(date))
                    return true;
                
                // if it ends after and started before, the data is corrupt
                if (p.second().after(date))
                    return false;
            }            
        }
        return true;
    }
}
