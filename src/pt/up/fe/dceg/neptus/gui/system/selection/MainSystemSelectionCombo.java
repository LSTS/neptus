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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventNewSystem;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.up.fe.dceg.neptus.events.NeptusEvents;
import pt.up.fe.dceg.neptus.i18n.I18n;

import com.google.common.eventbus.Subscribe;
import com.sun.java.swing.plaf.windows.WindowsComboBoxUI;

/**
 * @author Paulo Dias
 * @author Hugo Dias
 */
@SuppressWarnings("serial")
public class MainSystemSelectionCombo extends JComboBox<String> implements ItemListener {

    private static final Color DEFAULT_COLOR = new Color(0xC8BF5F);
    private static final Color DEFAULT_SEL_COLOR = new Color(0xB9AF3F);
    private static final Color CALIBRATION_COLOR = new Color(0x3A87AD);
    private static final Color CALIBRATION_SEL_COLOR = new Color(0x307191);
    private static final Color ERROR_COLOR = new Color(0xB94A48);
    private static final Color ERROR_SEL_COLOR = new Color(0xA23F3E);
    private static final Color SERVICE_COLOR = new Color(0x57B768);
    private static final Color SERVICE_SEL_COLOR = new Color(0x4AAE5C);

    private ConsoleLayout console;
    private Map<String, STATE> systemState = new ConcurrentHashMap<>();

    public MainSystemSelectionCombo(ConsoleLayout console) {
        this.console = console;
        NeptusEvents.register(this, console);
        this.setSize(200, 50);
        this.setMinimumSize(new Dimension(200, 50));
        this.setMaximumSize(new Dimension(200, 50));
        this.setRenderer(new MainSystemRenderer());
        this.addItemListener(this);
        this.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        this.setUI(new WindowsComboBoxUI());
        setFocusable(false);
    }

    /*
     * EVENTS
     */

    @Subscribe
    public void onNewSystem(ConsoleEventNewSystem e) {
        systemState.put(e.getSystem().getVehicleId(), e.getSystem().getVehicleState());
        
        this.addItem(e.getSystem().getVehicleId());
    }

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        try {
            systemState.put(e.getVehicle(), e.getState());
            if(console.getMainSystem().equals(e.getVehicle())){
                switch (e.getState()) {
                    case SERVICE:
                        setBackground(SERVICE_COLOR);
                        break;
                    case ERROR:
                        setBackground(ERROR_COLOR);
                        break;
                    case CALIBRATION:
                        setBackground(CALIBRATION_COLOR);
                        break;
                    default:
                        setBackground(DEFAULT_COLOR);
                        break;
                }
            }
            
            this.repaint();
        }
        catch (Exception e1) {
            NeptusLog.pub().warn(e);
        }
    }

    @Subscribe
    public void onMainSystemChange(ConsoleEventMainSystemChange e) {
        try {
            this.setSelectedItem(e.getCurrent());

            switch (console.getSystem(e.getCurrent()).getVehicleState()) {
                case SERVICE:
                    setBackground(SERVICE_COLOR);
                    break;
                case ERROR:
                    setBackground(ERROR_COLOR);
                    break;
                case CALIBRATION:
                    setBackground(CALIBRATION_COLOR);
                    break;
                default:
                    setBackground(DEFAULT_COLOR);
                    break;
            }
        }
        catch (Exception e1) {
            NeptusLog.pub().warn(e);
        }
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
            if(systemState.isEmpty()){
                return this;
            }
            if (isSelected) {
                if(systemState.get(value) != null)  {
                    switch (systemState.get(value)) {
                        case SERVICE:
                            setBackground(SERVICE_SEL_COLOR);
                            break;
                        case ERROR:
                            setBackground(ERROR_SEL_COLOR);
                            break;
                        case CALIBRATION:
                            setBackground(CALIBRATION_SEL_COLOR);
                            break;
                        default:
                            setBackground(DEFAULT_SEL_COLOR);
                            break;
                    }
                    setForeground(list.getSelectionForeground());                 
                }
            }
            else {
                if (systemState.get(value) != null) {

                    switch (systemState.get(value)) {
                        case SERVICE:
                            setBackground(SERVICE_COLOR);
                            break;
                        case ERROR:
                            setBackground(ERROR_COLOR);
                            break;
                        case CALIBRATION:
                            setBackground(CALIBRATION_COLOR);
                            break;
                        default:
                            setBackground(DEFAULT_COLOR);
                            break;
                    }
                    setForeground(list.getForeground());
                }
            }

            if (value != null)
                this.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            
            setText(" "+ value.toUpperCase() + "  " + I18n.text("Status") + ": " + I18n.text(systemState.get(value).toString()));
            
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
