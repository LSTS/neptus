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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 18 de Nov de 2012
 */
package pt.up.fe.dceg.neptus.gui.system.selection;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventNewSystem;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.up.fe.dceg.neptus.events.NeptusEvents;
import pt.up.fe.dceg.neptus.i18n.I18n;

import com.google.common.eventbus.Subscribe;

/**
 * @author Paulo Dias
 * @author Hugo Dias
 */
@SuppressWarnings("serial")
public class MainSystemSelectionCombo extends JComboBox<String> implements ItemListener {

    private ConsoleLayout console;
    private Map<String, String> systemState = new ConcurrentHashMap<>();

    public MainSystemSelectionCombo(ConsoleLayout console) {
        this.console = console;
        NeptusEvents.register(this, console);
        this.setSize(200, 50);
        this.setMinimumSize(new Dimension(200, 50));
        this.setMaximumSize(new Dimension(200, 50));
        this.setRenderer(new MainSystemRenderer());
        this.addItemListener(this);
    }

    // private void refresh() {
    // Set<String> systems = console.getConsoleSystems().keySet();
    // // Collections.sort(systems);
    // this.removeAllItems();
    // for (String system : systems) {
    // this.addItem(system);
    // }
    // this.setSelectedItem(console.getMainSystem());
    // }

    /*
     * EVENTS
     */

    @Subscribe
    public void onNewSystem(ConsoleEventNewSystem e) {
        systemState.put(e.getSystem().getVehicleId(), I18n.text("DISCONNECTED"));
        this.addItem(e.getSystem().getVehicleId());
    }

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        systemState.put(e.getVehicle(), e.getState().toString());
        this.repaint();
    }

    @Subscribe
    public void onMainSystemChange(ConsoleEventMainSystemChange e) {
        this.setSelectedItem(e.getCurrent());
    }

    private class MainSystemRenderer extends JLabel implements ListCellRenderer<String> {
        public MainSystemRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setVerticalAlignment(CENTER);
            setPreferredSize(new Dimension(270, 25));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (value != null)
                setText(value.toUpperCase() + "  " + I18n.text("Status") + ": " + systemState.get(value));
            
            return this;
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            console.setMainSystem(this.getSelectedItem().toString());
        }
    }
}
