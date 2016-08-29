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
 * Author: tsmarques
 * 25 May 2016
 */
package pt.lsts.neptus.plugins.mvplanning.monitors;

import com.google.common.eventbus.Subscribe;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventPlanAllocated;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.jaxb.PlanTaskMarshaler;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Class used to control the state of the plugin,
 * e.g. if the plugin is paused or running.
 * @author tsmarques
 *
 */
public class StateMonitor {
    public static enum STATE {
        RUNNING("running"),
        WAITING("waiting"),
        PAUSED("paused");

        public String value;
        STATE(String value) {
            this.value = value;
        }
    };

    private static volatile boolean isPaused = true;
    private static volatile boolean isClosing = false;

    public static void pausePlugin() {
        isPaused = true;
    }

    public static void resumePlugin() {
        if(!isClosing)
            isPaused = false;
    }

    public static boolean isPluginPaused() {
        return isClosing || isPaused;
    }

    public static boolean isPluginClosing() {
        return isClosing;
    }

    public StateMonitor() {
    }

    public void stopPlugin() {
        isClosing = true;
    }
}
