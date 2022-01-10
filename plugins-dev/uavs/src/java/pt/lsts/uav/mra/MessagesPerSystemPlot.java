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
 * Dec 12, 2015
 */
package pt.lsts.uav.mra;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRATimeSeriesPlot;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * This plug-in will plot the average number of messages per each system in the log.
 * It can be used for communications range analysis.
 * @author zp
 */
@PluginDescription(name = "Messages per System")
public class MessagesPerSystemPlot extends MRATimeSeriesPlot {

    private LsfIndex source;
    // The number of seconds to use to compute the average frequency
    private int timestepSecs = 10;
    // Store the total received messages in the last interval per system
    private LinkedHashMap<Integer, Integer> msgCount = new LinkedHashMap<>();

    /**
     * Class constructor
     * @param panel
     */
    public MessagesPerSystemPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public String getHorizontalAxisName() {
        return I18n.text("Messages per second");
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return true;
    }

    private void countMessages(int second) {
        for (Entry<Integer, Integer> entry : msgCount.entrySet()) {
            String name = source.getDefinitions().getResolver().resolve(entry.getKey());
            addValue(second * 1000, name, entry.getValue() / (double) timestepSecs);
            msgCount.put(entry.getKey(), 0);
        }
        
    }

    @Override
    public void process(LsfIndex source) {
        msgCount.clear();
        this.source = source;
        int curTime = 0;

        // go through all the messages in the log
        for (int i = 0; i < source.getNumberOfMessages(); i++) {
            double timestamp = source.timeOf(i);
            int src = source.sourceOf(i);
            // if there is a message after the current interval...
            if (timestamp > curTime + timestepSecs) {
                // count, update the plot and reset the variables 
                countMessages(curTime);
                // update the current interval start
                curTime = (int) timestamp;
            }
            // make sure there is an entry for this message's source
            if (!msgCount.containsKey(src))
                msgCount.put(src, 0);
            // increment the number of messages from this source
            msgCount.put(src, msgCount.get(src) + 1);
        }
    }
}
