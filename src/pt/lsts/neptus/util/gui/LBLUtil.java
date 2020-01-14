/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Margarida Faria
 * Feb 19, 2013
 */
package pt.lsts.neptus.util.gui;

import java.util.Date;

import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author Margarida Faria
 *
 */
public class LBLUtil {

    public static String writeTimeLabel(long timeStampMillis) {
        String timeStr, deltaTimeStr;
        try {
            timeStr = (timeStampMillis <= 0 ? "" : " ("
                    + DateTimeUtil.timeFormatterNoMillis2UTC.format(new Date(timeStampMillis)) + " UTC" + ")");
        }
        catch (Exception e) {
            timeStr = "";
        }

        deltaTimeStr = calcEllapsedTime(timeStampMillis);

        // label.setText("<html><b>Beacon ch"
        // + name
        // + " ("
        // + MathMiscUtils.round(range, 1)
        // + " m)"
        // + timeStr + deltaTimeStr
        // + "<br>"
        // + (isAccepted ? "Accepted" : ("Rejected" + (rejectionReason != null ? " "
        // + rejectionReason : ""))));
        // FIXI18N reason
        String ellapsedTime = timeStr + deltaTimeStr;
        return ellapsedTime;
    }

    private static String calcEllapsedTime(long timeStampMillis) {
        String deltaTimeStr;
        try {
            deltaTimeStr = (timeStampMillis <= 0 ? "" : " \u2206t "
                    + convertTimeMilliSecondsToFormatedString(System.currentTimeMillis() - timeStampMillis));
        }
        catch (Exception e) {
            deltaTimeStr = "";
        }
        return deltaTimeStr;
    }

    private static String convertTimeMilliSecondsToFormatedString(long timeMilliSeconds) {
        String tt = "";
        if (timeMilliSeconds < 60000)
            tt = MathMiscUtils.parseToEngineeringNotation(timeMilliSeconds / 1000, 0) + "s";
        else
            tt = DateTimeUtil.milliSecondsToFormatedString(timeMilliSeconds);
        return tt;
    }

}
