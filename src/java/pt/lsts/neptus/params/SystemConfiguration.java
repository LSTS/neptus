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
 * Author: José Pinto
 * Dec 14, 2012
 */
package pt.lsts.neptus.params;

import java.awt.BorderLayout;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author zp
 * @author jqcorreia
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(name = "System Configuration", icon = "images/settings2.png")
@Popup(accelerator='1', pos=POSITION.CENTER, width=600, height=600)
public class SystemConfiguration extends ConsolePanel implements NeptusMessageListener, MainVehicleChangeListener {

    private SystemConfigurationEditorPanel systemConfEditor;
        
    public SystemConfiguration(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
        initialize();
    }

    private void initialize() {
        removeAll();
        
        systemConfEditor = new SystemConfigurationEditorPanel(getMainVehicleId(), Scope.GLOBAL, Visibility.USER, true,
                false, true, getConsole().getImcMsgManager());
        
        setLayout(new BorderLayout());
        add(systemConfEditor);

        revalidate();
        repaint();
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        systemConfEditor.setSystemId(ev.getCurrent());
    }
        

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EntityParameters" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(systemConfEditor, message);
    }
}
