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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * 26/08/2016
 */
package pt.lsts.neptus.console.plugins.planning.overview;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.plugins.planning.PlanEditor;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.plan.PlanType;

public class MissionOverview extends JPanel {

    private static final long serialVersionUID = 1L;
    private JTable table;
    private PlanType selectedPlan = null;
    PlanTableModel model = null;

    public MissionOverview(PlanEditor pE, PlanType plan) {

        selectedPlan = plan;
        setLayout(new MigLayout("", "[][grow]", "[]"));
        model = new PlanTableModel(selectedPlan);
        table = new JTable(model);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table = (JTable) me.getSource();
                int row = table.getSelectedRow();
                if (me.getClickCount() == 1 && row != -1) {
                    Maneuver m = model.getManeuver(row);
                    pE.updateSelected(m);
                }
            }
        });
        
        table.setPreferredScrollableViewportSize(new Dimension(700, 80));
        add(new JScrollPane(table), "cell 0 1 2 1,grow");
        setPreferredSize(getPreferredSize());
    }

}
