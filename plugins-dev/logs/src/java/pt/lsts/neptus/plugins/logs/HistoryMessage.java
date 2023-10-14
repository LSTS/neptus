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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.plugins.logs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class HistoryMessage implements Comparable<HistoryMessage> {
    public enum msg_type {
        info,
        warning,
        error,
        critical,
        debug
    };

    @SuppressWarnings("serial")
    protected final DateFormat format = new SimpleDateFormat("HH:mm:ss") {{ setTimeZone(TimeZone.getTimeZone("UTC")); }};
    public long timestamp;
    public String text;
    public String context;
    public boolean assynchronous = false;
    public msg_type type = msg_type.info;
    
    public HistoryMessage(){
    }
    
    @Override
    public String toString() {
        return "["+format.format(new Date(timestamp))+" UTC] "+" ["+context+"] "+text;
    }
    
    public HistoryMessage(long timestamp, String text, String context, boolean assynchronous, msg_type type) {
        super();
        this.timestamp = timestamp;
        this.text = text;
        this.context = context;
        this.assynchronous = assynchronous;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        HistoryMessage o;
        try {
            o = (HistoryMessage) obj;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        if (timestamp == o.timestamp && text.equalsIgnoreCase(o.text) && context.equalsIgnoreCase(o.context)
                && type == o.type)
            return true;
        else
            return false;
    }

    @Override
    public int compareTo(HistoryMessage o) {
        return (int) (timestamp - o.timestamp);
    }

    @Override
    public int hashCode() {
        return (context + text + timestamp).hashCode();
    }

}
