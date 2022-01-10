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
 * Author: rasm
 * Apr 12, 2011
 */
package pt.lsts.neptus.plugins.gps.device;

/**
 * Class representing a broken down UTC time.
 * 
 * @author Ricardo Martins
 */
public class Time {
    /** Hour of the day. */
    private int hour;
    /** Minute of the hour. */
    private int minutes;
    /** Seconds of the minute. */
    private double seconds;

    /**
     * Get the hour of the day.
     * 
     * @return hour of the day
     */
    public int getHour() {
        return hour;
    }

    /**
     * Set the hour of the day.
     * 
     * @param aHour
     *            hour of the day.
     */
    public void setHour(int aHour) {
        hour = aHour;
    }

    /**
     * Get minutes of the hour.
     * 
     * @return minutes of the hour.
     */
    public int getMinutes() {
        return minutes;
    }

    /**
     * Set minutes of the hour.
     * 
     * @param m
     *            minutes of the hour.
     */
    public void setMinutes(int m) {
        minutes = m;
    }

    /**
     * Get seconds of the minute.
     * 
     * @return seconds of the minute.
     */
    public double getSeconds() {
        return seconds;
    }

    /**
     * Set seconds of the minute.
     * 
     * @param s
     *            seconds of the minute.
     */
    public void setSeconds(double s) {
        seconds = s;
    }
}
