/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 8, 2016
 */
package pt.lsts.neptus.console.plugins.base;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.gui.ImcStatePanel;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;

/**
 * @author zp
 *
 */
@PluginDescription(name="Incoming Data")
@Popup(accelerator=KeyEvent.VK_T, width=600, height=500, name="Incoming Data", pos=POSITION.CENTER, icon="images/menus/view_tree.png")
public class IncomingDataPanel extends ConsolePanel {

    private static final long serialVersionUID = 1L;
    private ImcStatePanel imcStatePanel = new ImcStatePanel(new ImcSystemState(IMCDefinition.getInstance()));
    private JScrollPane statePanel = new JScrollPane();
    private String selectedSystem;
    private JComboBox<String> combovt = new JComboBox<String>();
    
    public IncomingDataPanel(ConsoleLayout console) {
        super(console);
    }
    
    @Periodic(millisBetweenUpdates=5000)
    public void updateShownSystems() {
        
        if (!isVisible())
            return;
        
        ArrayList<String> toRemove = new ArrayList<>();
        for (int i = 0; i < combovt.getItemCount(); i++)
            toRemove.add(combovt.getItemAt(i));
        
        for (ImcSystem sys : ImcSystemsHolder.lookupAllActiveSystems()) {
            if (toRemove.contains(sys.getName()))
                toRemove.remove(sys.getName());
            else {
                combovt.addItem(sys.getName());    
            }
        }
        
        for (String s : toRemove) {
            if (!s.equals(selectedSystem))
                combovt.removeItem(s);
        }
    }
    
    @Override
    public void cleanSubPanel() {
        imcStatePanel.cleanup();
    }

    @Override
    public void initSubPanel() {
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        selectedSystem = getMainVehicleId();
        imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(selectedSystem));
        statePanel.setViewportView(imcStatePanel);
        top.add(combovt, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(statePanel, BorderLayout.CENTER);
        updateShownSystems();
        
        combovt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                JComboBox<?> cbox = (JComboBox<?>) evt.getSource();
                String selection = ""+cbox.getSelectedItem();
                
                if (selection.equals("null") || selection.equals(selectedSystem))
                    return;
                
                if (imcStatePanel != null)
                    imcStatePanel.cleanup();
                selectedSystem = selection;
                imcStatePanel = new ImcStatePanel(ImcMsgManager.getManager().getState(selectedSystem));
                statePanel.setViewportView(imcStatePanel);
                statePanel.revalidate();
                statePanel.repaint();
            }
        });       
    }        
}
