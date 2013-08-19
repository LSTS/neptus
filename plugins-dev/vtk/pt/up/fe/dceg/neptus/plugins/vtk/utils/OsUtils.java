/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: hfq
 * May 8, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.utils;

/**
 * @author hfq
 * 
 */

public class OsUtils {

    private static String OS = System.getProperty("os.name").toLowerCase();

    private static enum OSValidator {
        unix,
        win,
        mac,
        solaris
    }

    private static OSValidator osVal;

    private static boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    private static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    private static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") >= 0);
    }

    private static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }

    public static void checkOS() {
        if (isUnix()) {
            setOsVal(OSValidator.unix);
        }
        else if (isWindows()) {
            setOsVal(OSValidator.win);
        }
        else if (isMac()) {
            setOsVal(OSValidator.mac);
        }
        else if (isSolaris()) {
            setOsVal(OSValidator.solaris);
        }
    }

    /**
     * @return the osVal
     */
    public static OSValidator getOsVal() {
        return osVal;
    }

    /**
     * @param osVal the osVal to set
     */
    public static void setOsVal(OSValidator osVal) {
        OsUtils.osVal = osVal;
    }
}
