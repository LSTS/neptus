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
 * Author: José Quadrado Correia
 *
 */
package pt.lsts.neptus.gui;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

@SuppressWarnings("serial")
public class Timeline extends JPanel implements ChangeListener {
    public static final ImageIcon ICON_PLAY = ImageUtils.getIcon("images/icons/play.png");
    public static final ImageIcon ICON_PAUSE = ImageUtils.getIcon("images/icons/pause.png");
    public static final ImageIcon ICON_FW = ImageUtils.getIcon("images/icons/forward.png");
    public static final ImageIcon ICON_BW = ImageUtils.getIcon("images/icons/backward.png");

    private JSlider slider;
    private JButton play;
    private JButton speedUp;
    private JButton speedDown;
    private JLabel time;

    private AbstractAction playAction;
    private AbstractAction pauseAction;

    private final List<TimelineChangeListener> listeners = new ArrayList<>();

    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private final String namePrefix = Timeline.class.getSimpleName() + "::"
                + Integer.toHexString(Timeline.this.hashCode());
        private final AtomicInteger counter = new AtomicInteger(0);
        private final ThreadGroup group = new ThreadGroup(namePrefix);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r);
            t.setName(namePrefix + "::" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    /**
     * This flag means that Timeline will wait for the user to finish the dragging action before posting a
     * TimelineChange event
     */
    private boolean waitForAdjustment;

    private boolean running;

    private int frequency;
    private int advancePerSecond;
    private int speed;
    private int maxSpeed = 16;

    private final ScheduledFuture<?> updaterHandle;

    public Timeline(int min, int max, int frequency, int perSecond, boolean wait) {
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        // To avoid exception
        max = Math.max(min, max);

        slider = new JSlider(min, max);
        slider.addChangeListener(this);

        slider.setPaintTicks(true);
        play = new JButton(I18n.text("Play"));
        speedUp = new JButton(I18n.text("Faster"));
        speedDown = new JButton(I18n.text("Slower"));
        time = new JLabel("");

        playAction = getPlayAction();
        pauseAction = getPauseAction();

        play.setAction(playAction);

        speedUp.setAction(getSpeedUpAction());
        speedDown.setAction(getSpeedDownAction());

        // Layout definition
        setLayout(new MigLayout());

        add(play, "sg buttons");
        add(speedDown, "sg buttons");
        add(speedUp, "sg buttons");
        add(slider, "h 100%, w 100%");
        add(time);
        this.speed = 1;
        this.frequency = frequency;
        this.advancePerSecond = perSecond;
        this.waitForAdjustment = wait;

        running = false;
        Runnable updater = () -> {
            if (running) {
                slider.setValue(slider.getValue() + (advancePerSecond / Timeline.this.frequency) * speed);
            }
        };

        updaterHandle = service.scheduleAtFixedRate(updater, 0, 1000 / Timeline.this.frequency, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        running = false;
        updaterHandle.cancel(false);
        service.shutdown();
    }

    private AbstractAction getPlayAction() {
        return new AbstractAction("", ICON_PLAY) {
            @Override
            public void actionPerformed(ActionEvent e) {
                play();
            }
        };
    }

    private AbstractAction getPauseAction() {
        return new AbstractAction("", ICON_PAUSE) {
            @Override
            public void actionPerformed(ActionEvent e) {
                pause();
            }
        };
    }

    private AbstractAction getSpeedUpAction() {
        return new AbstractAction("", ICON_FW) {
            @Override
            public void actionPerformed(ActionEvent e) {
                speed *= 2;
                if(speed > maxSpeed)
                    speed = maxSpeed;
            }
        };
    }

    private AbstractAction getSpeedDownAction() {
        return new AbstractAction("", ICON_BW) {
            @Override
            public void actionPerformed(ActionEvent e) {
                speed /= 2;
                if(speed < 1)
                    speed = 1;
            }
        };
    }

    public void pause() {
        running = false;
        play.setAction(playAction);
    }

    private void play() {
        running = true;
        play.setAction(pauseAction);
    }

    public JSlider getSlider() {
        return slider;
    }

    /**
     * Set the max speed for the playback
     * Keep in mind this a multiplicative factor
     * @param maxSpeed
     */
    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    /**
     * @return the speed
     */
    public int getSpeed() {
        return speed;
    }

    public boolean isRunning() {
        return running;
    }

    public void addTimelineChangeListener(TimelineChangeListener changeListener) {
        listeners.add(changeListener);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (slider.getValueIsAdjusting() == waitForAdjustment)
            for (TimelineChangeListener l : listeners) {
                l.timelineChanged(slider.getValue());
            }
        if(slider.getValue() >= slider.getMaximum()) {
            running = false;
        }
    }

    public void setTime(long epochTimeMillis) {
        this.time.setText(fmt.format(new Date(epochTimeMillis)) + " UTC (x" + speed + ")");
    }

    public static void main(String args[]) {
        JFrame frame = new JFrame();
        final JLabel label = new JLabel("asdad");
        Timeline timeline = new Timeline(0, 100, 10, 10, true);

        frame.setSize(600, 100);
        frame.setLayout(new MigLayout());

        frame.add(label, "w 100%, wrap");
        frame.add(timeline);

        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        timeline.addTimelineChangeListener(value -> label.setText(value + ""));
    }
}
