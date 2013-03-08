/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Feb 19, 2013
 * $Id:: LBLUtil.java 10032 2013-02-26 03:31:41Z robot                          $:
 */
package pt.up.fe.dceg.neptus.util.gui;

import java.util.Date;

import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

/**
 * @author Margarida Faria
 *
 */
public class LBLUtil {

    public static String writeTimeLabel(long timeStampMillis) {
        String timeStr, deltaTimeStr;
        try {
            timeStr = (timeStampMillis <= 0 ? "" : " ("
                    + DateTimeUtil.timeFormaterNoMillis2UTC.format(new Date(timeStampMillis)) + " UTC" + ")");
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
