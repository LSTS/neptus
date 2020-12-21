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
 * Jan 30, 2014
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.plugins.BackVehiclePanel;
import pt.lsts.neptus.console.plugins.SideVehiclePanel;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayPanel;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 */
@PluginDescription(icon="pt/lsts/neptus/plugins/position/position.png")
public class AttitudeReplayPanel extends JPanel implements LogReplayPanel {

    private static final long serialVersionUID = -7574942224715046138L;
    private LinkedHashMap<String, SideVehiclePanel> sidePanels = new LinkedHashMap<>();
    private LinkedHashMap<String, BackVehiclePanel> backPanels = new LinkedHashMap<>();
    
    @Override
    public JComponent getComponent() {
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        setPreferredSize(new Dimension(500, 200));
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    @Override
    public void parse(IMraLogGroup source) {
        EstimatedState pivot = source.getLsfIndex().getFirst(EstimatedState.class);
        onMessage(pivot);
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"EstimatedState"};
    }
    
    @Override
    public String getName() {
        return "Attitude Replay Panel";
    }

    @Override
    public void onMessage(IMCMessage message) {
        String id = message.getSourceName();
        if (!sidePanels.containsKey(id)) {
            SideVehiclePanel sp = new SideVehiclePanel();
            sp.setVehicle(id);
            sp.setSize((int)(getWidth()*0.666), getHeight()/(1+sidePanels.size()));
            sp.setBorder(BorderFactory.createLineBorder(Color.red));
            sidePanels.put(id, sp);
            BackVehiclePanel bp = new BackVehiclePanel();
            bp.setVehicle(id);
            bp.setSize((int)(getWidth()*0.333), getHeight()/(1+sidePanels.size()));
            bp.setBorder(BorderFactory.createLineBorder(Color.red));
            backPanels.put(id, bp);
            JPanel p = new JPanel(new TableLayout(new double[]{0.666,0.333}, new double[] {TableLayout.FILL}));
            p.add(sp, "0,0");
            p.add(bp,"1,0");
            
            add(p);
            invalidate();
            revalidate();
        }
        sidePanels.get(id).setPitch(message.getFloat("theta"));
        if (message.getFloat("depth") != -1)
            sidePanels.get(id).setDepth(message.getFloat("depth"));
        else
            sidePanels.get(id).setDepth(-message.getFloat("height") + message.getFloat("z"));
        
        backPanels.get(id).setRoll(message.getFloat("phi"));
        repaint();
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void cleanup() {
        
    }
    
    public static void main(String[] args) {
        
        
    }

}
