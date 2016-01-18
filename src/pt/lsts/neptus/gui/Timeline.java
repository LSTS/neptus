package pt.lsts.neptus.gui;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
    
    private List<TimelineChangeListener> listeners = new ArrayList<TimelineChangeListener>();
    
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Timeline Thread");
            t.setDaemon(true);
            return t;
        }
    });

    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    
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
    
    private Runnable updater;
    
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
        updater = new Runnable() {
            @Override
            public void run() {
                if (running) {
                    slider.setValue(slider.getValue() + (advancePerSecond / Timeline.this.frequency) * speed);
                }
            }
        };
        service.scheduleAtFixedRate(updater, 0, 1000 / Timeline.this.frequency, TimeUnit.MILLISECONDS);
    }

    public AbstractAction getPlayAction() {
        return new AbstractAction("", ICON_PLAY) {
            @Override
            public void actionPerformed(ActionEvent e) {
                play();
            }
        };
    }

    public AbstractAction getPauseAction() {
        return new AbstractAction("", ICON_PAUSE) {
            @Override
            public void actionPerformed(ActionEvent e) {
                pause();
            }
        };
    }

    public AbstractAction getSpeedUpAction() {
        return new AbstractAction("", ICON_FW) {
            @Override
            public void actionPerformed(ActionEvent e) {
                speed *= 2;
                if(speed > maxSpeed)
                    speed = maxSpeed;
            }
        };
    }

    public AbstractAction getSpeedDownAction() {
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
    public void play() {
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
    
    public void focusTime(int timestamp) {
        slider.setValue(timestamp);
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

        timeline.addTimelineChangeListener(new TimelineChangeListener() {

            @Override
            public void timelineChanged(int value) {
                label.setText(value + "");
            }
        });
    }
}
