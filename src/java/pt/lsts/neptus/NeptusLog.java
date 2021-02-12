/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Hugo Dias
 * Oct 18, 2012
 */
package pt.lsts.neptus;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author Hugo
 * @author Paulo Dias
 */
public class NeptusLog {
    public static boolean extendedLog = true;

    private static final String NEPTUS_PUB = "Neptus.Pub";
    private static final String NEPTUS_ACTION = "Neptus.Action";
    private static final String NEPTUS_WASTE = "Neptus.Waste";

    private static Logger pub = Logger.getLogger(NEPTUS_PUB);
    private static Logger action = Logger.getLogger(NEPTUS_ACTION);
    private static Logger waste = Logger.getLogger(NEPTUS_WASTE);

   
    public static void init(){
        try {
            if (new File("conf/log4j.xml").exists()) {
                DOMConfigurator.configure("conf/log4j.xml");
                pub.debug("Log4J configured with conf/log4j.xml!");
            }
            else {
                PropertyConfigurator.configure("conf/log4j.properties");
                pub.debug("Log4J configured with conf/log4j.properties!");
            }
        }
        catch (Error e) {
            BasicConfigurator.configure();
            pub.warn("Could not configure Log4J with a default config, will try to load from configuration file!!");
        }
    }
    
    private static String getCallerStamp() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int idx = 4; // 3
        if (stack.length <= idx)
            return "";
        String str = stack[idx].getClassName() + "." + stack[idx].getMethodName(); // 3
        return str;
    }

    private static Logger getLogger(Logger fallbackLogger, String prefix) {
        String caller = getCallerStamp();
        if (caller.length() == 0)
            return fallbackLogger;
        else
            return Logger.getLogger(prefix + "." + caller);
    }

    public static Logger pub() {
        return GeneralPreferences.programLogExtendedLog ? NeptusLog.getLogger(NeptusLog.pub, NEPTUS_PUB) : pub;
    }

    public static Logger pubRoot() {
        return pub;
    }

    public static Logger action() {
        return GeneralPreferences.programLogExtendedLog ? NeptusLog.getLogger(NeptusLog.action, NEPTUS_ACTION) : action;
    }

    public static Logger actRoot() {
        return action;
    }

    public static Logger waste() {
        return GeneralPreferences.programLogExtendedLog ? NeptusLog.getLogger(NeptusLog.waste, NEPTUS_WASTE) : waste;
    }

    public static Logger wasteRoot() {
        return waste;
    }
}
