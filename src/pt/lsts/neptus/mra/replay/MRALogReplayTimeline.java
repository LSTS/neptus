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
 * Jan 29, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.eventbus.EventBus;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
public class MRALogReplayTimeline extends JPanel implements ChangeListener {

    private static final long serialVersionUID = 785733887538053098L;
    private JComboBox<String> speedMultiplier = null;
    private JToggleButton play = null;
    private JSlider timeline = null;
    private LsfIndex index;
    private EventBus bus;
    private double timeMultiplier = 1;
    private Thread replayThread = null;
    private boolean changing = false;
    private ImageIcon playIcon = ImageUtils.getIcon("pt/lsts/neptus/mra/replay/control-play.png");
    private JLabel time = new JLabel();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    
    public MRALogReplayTimeline(MRALogReplay replay) {
        this.index = replay.getIndex();
        this.bus = replay.getReplayBus();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        time.setText(sdf.format(new Date(1000*(long)index.getStartTime())));
        setLayout(new BorderLayout());
        JPanel tmp = new JPanel();
        play = getPlayButton();
        tmp.add(play);

        speedMultiplier = getSpeedCombo();
        tmp.add(speedMultiplier, BorderLayout.WEST);
        
        add(getTimeline(replay), BorderLayout.CENTER);
        add(tmp, BorderLayout.WEST);
        add(time, BorderLayout.EAST);
    }
    
    public void cleanup() {
        if (replayThread != null)
            replayThread.interrupt();
    }
    
    private JSlider getTimeline(MRALogReplay replay) {
        if (timeline == null) {
            timeline = new JSlider((int) replay.getIndex().getStartTime(), (int) replay.getIndex().getEndTime(),
                    (int) replay.getIndex().getStartTime());
            timeline.addChangeListener(this);
            timeline.setPaintLabels(true);
            timeline.setBorder(BorderFactory.createEmptyBorder());
        }
        return timeline;
    }
    
    public void pause() {
        if (getPlayButton().isSelected())
            getPlayButton().doClick();
    }

    private JToggleButton getPlayButton() {
        if (play == null) {
            play = new JToggleButton(playIcon);
            play.setToolTipText(I18n.text("Resume replay"));

            play.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (play.isSelected()) {
                        if (replayThread != null)
                            replayThread.interrupt();
                        replayThread = createReplayThread();
                        replayThread.setDaemon(true);
                        replayThread.start();
                        play.setToolTipText(I18n.text("Pause replay"));
                    }
                    else {
                        if (replayThread != null)
                            replayThread.interrupt();
                        play.setToolTipText(I18n.text("Resume replay"));
                    }
                }
            });
        }
        return play;
    }

    private JComboBox<String> getSpeedCombo() {
        if (speedMultiplier == null) {
            speedMultiplier = new JComboBox<>(new String[] { "1x", "2x", "5x", "10x", "20x", "60x", "0.5x", "0.25x"});
            speedMultiplier.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    switch (speedMultiplier.getSelectedItem().toString()) {
                        case "1x":
                            timeMultiplier = 1;
                            break;
                        case "2x":
                            timeMultiplier = 2;
                            break;
                        case "5x":
                            timeMultiplier = 5;
                            break;
                        case "10x":
                            timeMultiplier = 10;
                            break;
                        case "20x":
                            timeMultiplier = 20;
                            break;
                        case "60x":
                            timeMultiplier = 60;
                            break;
                        case "0.5x":
                            timeMultiplier = 0.5;
                            break;
                        case "0.25x":
                            timeMultiplier = 0.25;
                            break;
                        
                        default:
                            timeMultiplier = 1;
                            break;
                    }
                    NeptusLog.pub().info("Replay speed is now " + timeMultiplier + "x realtime");
                }
            });
        }
        return speedMultiplier;
    }

    public void focusTime(double timestamp) {
        if (timestamp < timeline.getMinimum())
            timestamp = timeline.getMinimum();
        if (timestamp > timeline.getMaximum())
            timestamp = timeline.getMaximum();

        timeline.setValue((int) timestamp);
        if (replayThread != null)
            replayThread.interrupt();

        replayThread = createReplayThread();
        replayThread.start();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (timeline.getValueIsAdjusting()) {
            changing = true;
        }
        else if (changing) {
            changing = false;
            if (play.isSelected()) {
                if (replayThread != null)
                    replayThread.interrupt();
                replayThread = createReplayThread();
                replayThread.setDaemon(true);
                replayThread.start();
            }
        }
        time.setText(sdf.format(new Date(timeline.getValue()*1000l)));
    }

    private Thread createReplayThread() {
        return new Thread("MRA Log Replay") {
            public void run() {

                long lastMissionTime = timeline.getValue() * 1000l;
                long lastSystemTime = System.currentTimeMillis();
                int i = index.advanceToTime(0, lastMissionTime / 1000.0);

                int k = 0;
                
                while (true && !timeline.getValueIsAdjusting()) {
                    try {
                        long newTime = System.currentTimeMillis();
                        long ellapsed = newTime - lastSystemTime;
                        lastSystemTime = newTime;
                        long newMissionTime = lastMissionTime + (long)(ellapsed * timeMultiplier);
                        if (newMissionTime / 1000 != timeline.getValue())
                            timeline.setValue((int) (newMissionTime / 1000));

                        int oldI = i;
                        
                        while (!isInterrupted() && i < index.getNumberOfMessages() && (index.timeOf(i)*1000 < newMissionTime || k > 4)) {
                            IMCMessage m = index.getMessage(i);
                            bus.post(m);
                            i++;
                            k = 0;
                        }
                        if (i >= index.getNumberOfMessages())
                            return;

                        if (oldI == i)
                            k++;
                        
                        lastMissionTime = newMissionTime;
                        Thread.sleep(100);
                    }
                    catch (Exception e) {
                        return;
                    }
                }
            };
        };
    }
}