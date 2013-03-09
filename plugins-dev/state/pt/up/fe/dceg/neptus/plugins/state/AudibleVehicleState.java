/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 12, 2012
 */
package pt.up.fe.dceg.neptus.plugins.state;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.Teleoperation;
import pt.up.fe.dceg.neptus.imc.VehicleState;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.speech.SpeechUtil;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Audio Vehicle State Alerts", category = CATEGORY.INTERFACE)
public class AudibleVehicleState extends SimpleSubPanel implements IPeriodicUpdates {
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

    public void say(String text) {
        if (useAudioAlerts)
            SpeechUtil.readSimpleText(text.replaceAll("xtreme", "extreme"));
    }

    @Subscribe
    public void consume(VehicleState msg) {
        String src = msg.getSourceName();
        if (src == null)
            return;

        VehicleState oldState = vStatesImc.get(src);

        if (oldState != null) {
            if (oldState.getOpMode() != msg.getOpMode()) {
                
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
