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
 * Author: pdias
 * 04/03/2016
 */
package pt.lsts.neptus.util.datetime;

import java.util.concurrent.TimeUnit;

/**
 * @author pdias
 *
 */
public class TimeDuration {

    private TimeUnit timeUnit = TimeUnit.SECONDS;
    private long time = 0;
    
    public TimeDuration(long time, TimeUnit units) {
        setTime(time, units);
    }
    
    public void setTime(long time, TimeUnit units) {
        this.time = time;
        this.timeUnit = units;
    }
    
    public long getTimeAsMillis() {
        return TimeUnit.MILLISECONDS.convert(time, timeUnit);
    }
    
    public long getTimeAsSeconds() {
        return TimeUnit.SECONDS.convert(time, timeUnit);
    }

    public long getTimeAsMinutes() {
        return TimeUnit.MINUTES.convert(time, timeUnit);
    }

    public long getTimeAsHours() {
        return TimeUnit.HOURS.convert(time, timeUnit);
    }

    public long getTimeAsDays() {
        return TimeUnit.DAYS.convert(time, timeUnit);
    }
}
