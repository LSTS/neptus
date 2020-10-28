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
 * Author: zp
 * Jun 30, 2014
 */
package pt.lsts.neptus.plugins.europa.gui;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import psengine.PSToken;
import pt.lsts.neptus.plugins.europa.NeptusSolver;
import pt.lsts.neptus.plugins.europa.gui.TimelineView.PlanToken;

/**
 * @author zp
 *
 */
public class PlanView extends JPanel implements TimelineViewListener {

    private static final long serialVersionUID = 25568315874216387L;
    private Vector<TimelineView> timelines = new Vector<>();
    private NeptusSolver solver;
    private HashSet<PlanViewListener> listeners = new HashSet<>();
    
    public PlanView(NeptusSolver solver) {
        setBackground(Color.white);
        setLayout(new MigLayout(new LC().fillX().gridGap("5px", "0px")));
        for (String v : solver.getVehicles()) {
            try {
                TimelineView tl = new TimelineView(solver);
                tl.setPlan(solver.getPlan(v));
                add(new JLabel(v));
                add(tl, new CC().growX().wrap());
                tl.addListener(this);
                timelines.add(tl);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        endTimeChanged(null, 0);
    }
    
    public TimelineView addTimeline(String name, Collection<PSToken> plan) {
        TimelineView view = new TimelineView(solver);
        view.setPlan(plan);
        timelines.add(view);
        return view;
    }    
    
    @Override
    public void startTimeChanged(TimelineView source, long startTimeMillis) {
        long minStart = startTimeMillis;
        for (TimelineView v : timelines)
            minStart = Math.min(v.getStartTime(), minStart);
        
        for (TimelineView v : timelines)
            v.setStartTime(minStart);        
    }
    
    

    @Override
    public void endTimeChanged(TimelineView source, long startTimeMillis) {
        long maxEnd = startTimeMillis;
        for (TimelineView v : timelines) {
           maxEnd = Math.max(v.computeEndTime(), maxEnd);
        }
        
        for (TimelineView v : timelines)
            v.setEndTime(maxEnd);        
    }

    public void addListener(PlanViewListener l) {
        listeners.add(l);
    }
    
    public void removeListener(PlanViewListener l) {
        listeners.remove(l);
    }
    
    @Override
    public void tokenSelected(TimelineView source, PlanToken token) {
        for (TimelineView v : timelines) {
            if (v.equals(source))
                continue;
            v.setSelectedToken(null);
        }
        
        for (PlanViewListener l : listeners)
            l.tokenSelected(token);
    }
    
    @Override
    public void planChanged() {
        for (TimelineView v : timelines)
            v.reloadPlan();        
    }
}
