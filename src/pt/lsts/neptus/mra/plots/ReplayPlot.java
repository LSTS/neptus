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
 * Author: jqcorreia
 * Jun 4, 2013
 */
package pt.lsts.neptus.mra.plots;

import java.awt.Component;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.gui.Timeline;
import pt.lsts.neptus.gui.TimelineChangeListener;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.lsf.LsfMraLog;

/**
 * @author jqcorreia
 *
 */
public class ReplayPlot extends MRATimeSeriesPlot implements TimelineChangeListener {

    Timeline timeline;
    long firstTimestamp;
    private long lastTimestamp;
    String[] fieldsToPlot;
    LinkedHashMap<String, IMraLog> parsers = new LinkedHashMap<String, IMraLog>();

    /**
     * @param panel
     */
    public ReplayPlot(MRAPanel panel, String[] fieldsToPlot) {
        super(panel);
        this.fieldsToPlot = fieldsToPlot;
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        for (String field : fieldsToPlot) {
            String messageName = field.split("\\.")[0];
            if (index.getFirstMessageOfType(messageName) == -1)
                return false;
        }
        return true;
    }

    // In this case process serves as parser initializer
    @Override
    public void process(LsfIndex source) {
        firstTimestamp = (long) (source.timeOf(0) * 1000);
        lastTimestamp = (long) (source.timeOf(source.getNumberOfMessages()-2) * 1000);

        timeline = new Timeline(0, (int)(lastTimestamp - firstTimestamp), 24, 1000, false);
        timeline.getSlider().setValue(0);
        timeline.addTimelineChangeListener(this);

        for (String field : fieldsToPlot) {
            String messageName = field.split("\\.")[0];
            parsers.put(messageName, new LsfMraLog(index, messageName));
        }

    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        Component comp = super.getComponent(source, timestep);
        JPanel panel = new JPanel(new MigLayout());

        panel.add(comp, "w 100%, h 100%, wrap");
        panel.add(timeline, "w 100%, h 80");

        return panel;
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder(Arrays.toString(fieldsToPlot));
        sb.append(" Timeline");
        return sb.toString();
    }    

    @Override
    public void timelineChanged(int value) {
        try {
            long ts = firstTimestamp + value;

            for (String field : fieldsToPlot) {
                String messageName = field.split("\\.")[0];
                String fieldName = field.split("\\.")[1];
                addValue(firstTimestamp + value, field,
                        parsers.get(messageName).getEntryAtOrAfter(ts).getDouble(fieldName));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setTimelineVisible(boolean visible) {
        timeline.setVisible(visible);
    }
}
