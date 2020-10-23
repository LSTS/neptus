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
 * Nov 12, 2012
 */
package pt.lsts.neptus.plugins.state;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Teleoperation;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.speech.SpeechUtil;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Audio Vehicle State Alerts", category = CATEGORY.INTERFACE)
public class AudibleVehicleState extends ConsolePanel implements IPeriodicUpdates {
    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, VehicleState> vStatesImc = new LinkedHashMap<>();
    //protected LinkedHashMap<String, Long> lastUpdates = new LinkedHashMap<>();
    protected JMenuItem audioAlerts = null;

    @NeptusProperty(name = "Use Audio Alerts", userLevel = LEVEL.REGULAR)
    public boolean useAudioAlerts = true;

    /**
     * @param console
     */
    public AudibleVehicleState(ConsoleLayout console) {
        super(console);
    }

    @Override
    public long millisBetweenUpdates() {
        return 2500;
    }

    @Override
    public void initSubPanel() {

        audioAlerts = new JCheckBoxMenuItem(I18n.text("Audio Alerts"));
        audioAlerts.setSelected(useAudioAlerts);
        audioAlerts.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                useAudioAlerts = audioAlerts.isSelected();
                if (!useAudioAlerts)
                    SpeechUtil.stop();
            }
        });
        getConsole().getOrCreateJMenu(new String[] { I18n.text("Tools") }).add(audioAlerts);
    }

    @Override
    public void cleanSubPanel() {
        try {            
            audioAlerts.getParent().remove(audioAlerts);
            SpeechUtil.stop();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean update() {
        for (String src : vStatesImc.keySet().toArray(new String[0])) {
            try {
                if (!ImcSystemsHolder.getSystemWithName(src).isActive()){
                    vStatesImc.remove(src);
                    say("Lost connection to " + src);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().debug(
                        AudibleVehicleState.class.getSimpleName() + " for " + src + " gave an error: "
                                + e.getMessage());
            }
        }
        return true;
    }

    private String cleanUp(String text) {
        return text
                .replaceAll("xtreme", "extreme")
                .replaceAll("xplore", "explore")
                .replaceAll("arpao", "arpau")
                .replaceAll("lauv-seacon-1", "eereena")
                .replaceAll("lauv-seacon-2", "premah")
                .replaceAll("lauv-seacon-3", "valenteenah");
    }
    public void say(String text) {
        if (useAudioAlerts) 
            SpeechUtil.readSimpleText(cleanUp(text));
    }

    @Subscribe
    public void consume(VehicleState msg) {
        String src = msg.getSourceName();
        if (src == null)
            return;

        VehicleState oldState = vStatesImc.get(src);

        if (oldState != null) {
            boolean stoppedTeleop = oldState.getManeuverType() == Teleoperation.ID_STATIC && msg.getManeuverType() != Teleoperation.ID_STATIC;
            boolean startedTeleop = oldState.getManeuverType() != Teleoperation.ID_STATIC && msg.getManeuverType() == Teleoperation.ID_STATIC;
            if (oldState.getOpMode() != msg.getOpMode() || startedTeleop || stoppedTeleop) {
                String text = src + " is in " + msg.getOpMode().toString() + " mode";
                if (msg.getManeuverType() == Teleoperation.ID_STATIC)
                    text = src + " is in teleh operation mode";
                
                String regexp = "e?"+src+" is in .* mode";
                SpeechUtil.removeStringsFromQueue(regexp);
                say(text);
            }
        }
        else {
            String text = src + " is connected";
            SpeechUtil.removeStringsFromQueue("Lost connection to e?"+src);            
            say(text);
        }
        vStatesImc.put(src, msg);
    }
}
