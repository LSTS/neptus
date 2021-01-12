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
 * Author: jfortuna
 * Jun 20, 2014
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.AutopilotMode;
import pt.lsts.imc.AutopilotMode.AUTONOMY;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.update.Periodic;

/** Shows Autopilot mode and level of autonomy
 * @author jfortuna
 * M. Ribeiro
 *
 */
@PluginDescription(name = "Autopilot Mode Indicator", author = "jfortuna",  version = "0.1", category = CATEGORY.INTERFACE)
public class AutopilotModePanel extends ConsolePanel implements MainVehicleChangeListener {

    @NeptusProperty
    public long oldMsgTimeout = 2000;

    private static final long serialVersionUID = 1L;
    private long lastMsgMillis;

    // Current mode variables
    private String currentMode = null;
    private AUTONOMY autonomyLevel = AUTONOMY.MANUAL;

    // GUI
    private JPanel modePanel = null;

    private boolean updated = false;

    @Periodic
    public void checkIfOld() {
        if (!updated && (System.currentTimeMillis() - lastMsgMillis >= oldMsgTimeout)) {
            modePanel.setBackground(UIManager.getColor( "Panel.background"));
            modePanel.removeAll();
            modePanel.repaint();
            updated = true;
        }
    }

    // Listener
    @Subscribe
    public void on(AutopilotMode msg) {
        if(!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;

        lastMsgMillis = System.currentTimeMillis();
        currentMode = msg.getMode();
        autonomyLevel = msg.getAutonomy();

        modePanel.removeAll();

        JLabel modeLabel = new JLabel(currentMode,SwingConstants.CENTER);
        modeLabel.setFont(new Font(modeLabel.getFont().getFontName(),Font.BOLD,modeLabel.getFont().getSize()));

        switch (autonomyLevel) {
            case MANUAL:
                modePanel.setBackground(Color.yellow);
                break;
            case ASSISTED:
                modePanel.setBackground(Color.blue);
                modeLabel.setForeground(Color.white);
                break;
            case AUTO:
                modePanel.setBackground(Color.green.darker());
                break;
        };

        modePanel.add(modeLabel, "w 100%, h 100%");
        updated = false;
    }

    /**
     * @param console
     */
    public AutopilotModePanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    @Override
    public void initSubPanel() {

        titlePanelSetup();

        //panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(modePanel,"w 100%, h 100%, wrap"); 
    }

    private void titlePanelSetup() {
        modePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        modePanel.add(new JLabel(I18n.text("Mode Indicator"),SwingConstants.CENTER), "w 100%, h 100%");
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
