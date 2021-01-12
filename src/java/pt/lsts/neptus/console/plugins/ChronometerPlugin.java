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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 2009/06/07
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.ChronometerPanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@Popup(pos = POSITION.BOTTOM_LEFT, width = 300, height = 150, accelerator = 'K')
@PluginDescription(name = "Chronometer", description = "This is a chronometer that counts up and down", author = "Paulo Dias", icon = "images/buttons/clocksync2.png", documentation = "chronometer/chronometer.html")
public class ChronometerPlugin extends ConsolePanel implements ConfigurationListener {

    @NeptusProperty(name = "Audio Alarm on Zero", userLevel = LEVEL.REGULAR, description = "Play an audio alarm on passing on alarm value defined.")
    private boolean audioAlarmOnZero = false;
    
    private ChronometerPanel chronoPanel = null;

    public ChronometerPlugin(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
	 * 
	 */
    private void initialize() {
        this.removeAll();
        this.setLayout(new BorderLayout());
        this.add(getChronoPanel(), BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        chronoPanel.setAudioAlertOnZero(audioAlarmOnZero);
    }
    
    /**
     * @return the chronoPanel
     */
    public ChronometerPanel getChronoPanel() {
        if (chronoPanel == null) {
            chronoPanel = new ChronometerPanel();
        }
        return chronoPanel;
    }

    @Override
    public void initSubPanel() {
        // nothing
    }

    @Override
    public void cleanSubPanel() {
        if (chronoPanel != null) // Let's make sure no thread is left running
            chronoPanel.stop();
    }
}
