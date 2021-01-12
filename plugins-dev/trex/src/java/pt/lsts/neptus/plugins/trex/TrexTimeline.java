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
 * Apr 23, 2013
 */
package pt.lsts.neptus.plugins.trex;

import java.util.LinkedHashSet;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.plots.MRAGanttPlot;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name="TREX Timeline", active=false)
public class TrexTimeline extends MRAGanttPlot {

    public TrexTimeline(MRAPanel panel) {
        super(panel);
    }
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("TrexToken");
    }

    @Override
    public void process(LsfIndex source) {
        LinkedHashSet<String> timelines = new LinkedHashSet<>();

        for (IMCMessage s : source.getIterator("TrexToken")) {
            startActivity(s.getTimestamp(), s.getString("timeline"), s.getString("timeline")+"."+s.getString("predicate"));
            timelines.add(s.getString("timeline"));
        }

        for (String s : timelines)
            endActivity(source.getEndTime(), s);
    }

}
