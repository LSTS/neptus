/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/09/27
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.imc.LblRangeAcceptance;
import pt.lsts.imc.LblRangeAcceptance.ACCEPTANCE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.i18n.Translate;
import pt.lsts.neptus.loader.NeptusMain;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.MapChangeListener;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.lbl.LBLTriangulationHelper;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@Popup( pos = POSITION.RIGHT, width=300, height=250, accelerator='B')
@PluginDescription(author = "Paulo Dias", name = "LBL Ranges", icon = "pt/lsts/neptus/plugins/acoustic/lbl.png", description = "Displays the LBL ranges.", documentation = "lbl-ranges/lbl-ranges.html", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 40)
public class LBLRangeDisplay extends ConsolePanel implements MainVehicleChangeListener, Renderer2DPainter,
SubPanelChangeListener, MissionChangeListener, MapChangeListener, ConfigurationListener, NeptusMessageListener {

    private final Icon ICON_LBL_SHOW = ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/lbl-show.png");
    private final Icon ICON_LBL_HIDE = ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/lbl-hide.png");
    private final Icon ICON_RESET = ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/reload.png");
    private final Icon ICON_SOUND_ON = ImageUtils.getIcon("pt/lsts/neptus/plugins/acoustic/sound.png");
    private final Icon ICON_SOUND_OFF = ImageUtils
            .getIcon("pt/lsts/neptus/plugins/acoustic/sound-off.png");
    private final Icon ICON_SETTINGS = ImageUtils.getScaledIcon(
            ImageUtils.getImage("pt/lsts/neptus/plugins/acoustic/settings.png"), 24, 24);

    @Translate
    public enum HideOrFadeRangeEnum {
        HIDE,
        FADE
    };

    @NeptusProperty(name = "Seconds to display ranges", description = "Use -1 to not clear the ranges from the renderer.", userLevel = LEVEL.REGULAR)
    public int secondsToDisplayRanges = 5;

    @NeptusProperty(name = "Hide or fade the old ranges", description = "True to hide the range and false to fade when display time elapses.", userLevel = LEVEL.REGULAR)
    public HideOrFadeRangeEnum hideOrFadeRange = HideOrFadeRangeEnum.FADE;

    @NeptusProperty(name = "Number of points to display", userLevel = LEVEL.REGULAR)
    public int numberOfShownPoints = 100;

    @NeptusProperty(name = "Accepted range color", userLevel = LEVEL.REGULAR)
    public Color acceptedColor = Color.ORANGE;

    @NeptusProperty(name = "Rejected range color", userLevel = LEVEL.REGULAR)
    public Color rejectedColor = Color.RED.brighter();

    @NeptusProperty(name = "Surface range color", userLevel = LEVEL.REGULAR)
    public Color surfaceColor = Color.BLUE;

//    @NeptusProperty(name = "System", hidden = true, description = "The system to display (use 'main' for main vehicle)")
//    public String system = "main";

    @NeptusProperty(name = "MIDI Channel", category = "Communication")
    public int midi_channel = 13;

    @NeptusProperty(name = "Number of distinct frequencies", category = "Communication", editable = false, userLevel = LEVEL.REGULAR)
    public int numFreqs = 8;

    @NeptusProperty(name = "Volume")
    public int volume = 100;

    @NeptusProperty(name = "Use Only New LBL Range Acceptance Message", category = "Communication")
    public boolean useOnlyNewLblRangeAcceptance = true;

    @NeptusProperty(name = "Draw Range Up Or Down The Point")
    public boolean drawRangeUpOrDownThePoint = true;

    @NeptusProperty(name = "Play Sound On Surface Ranges")
    public boolean playSoundOnSurfaceRanges = false;

    // UI
    private JXLabel title = null;
    private JScrollPane scrollPane = null;
    private JXPanel holder = null;

    // private Vector<LBLRangeLabel> beacons = new Vector<LBLRangeLabel>();
    private final LinkedHashMap<String, LBLRangeLabel> beacons = new LinkedHashMap<String, LBLRangeLabel>();

    // Renderer painter stuff
    private MissionType missionType = new MissionType();
//    private final LinkedList<TransponderElement> transpondersList = new LinkedList<TransponderElement>();
//    private final LinkedList<RangePainter> rangeFixPainter = new LinkedList<RangePainter>();
    private HomeReference hRef = new HomeReference();
    private final LocationType locStart = new LocationType();

//    private final LinkedList<CoordinateSystem> coordSystemsList = new LinkedList<CoordinateSystem>();
//    private final LinkedList<Double> distanciesList = new LinkedList<Double>();
//    private double[] distances = { Double.NaN, Double.NaN, Double.NaN };
//    private double[] distancesPrev = { Double.NaN, Double.NaN, Double.NaN };
//    private LocationType lastKnownPos = new LocationType();
    
    private LocationType startPos = new LocationType();
    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();
    private ScatterPointsElement scatter = null;
    private long lastCalcPosTimeMillis = -1;
    
    
    private LinkedList<TransponderElement> transponders = new LinkedList<TransponderElement>();
//    private LocationType start = new LocationType();
//    private LinkedList<LocationType> triangulatedRangesPointsList = new LinkedList<LocationType>();
    private LinkedList<RangePainter> rangeFixPainter = new LinkedList<RangePainter>();
    private LBLTriangulationHelper lblTriangulationHelper = null;


    // Play MIDI
    protected ShortMessage msg = new ShortMessage();
    protected Synthesizer synth;
    protected Receiver receiver;

    private boolean showInRender = true;
    private boolean playSound = true;
    private boolean initCalled = false;

    private AbstractAction showAction;
    private AbstractAction resetAction;
    private AbstractAction soundAction;
    private AbstractAction settingsAction;

    /**
     * 
     */
    public LBLRangeDisplay(ConsoleLayout console) {
        super(console);
        initializeActions();
        initialize();
    }

    /**
     * 
     */
    private void initialize() {
        removeAll();
        setLayout(new BorderLayout());

        title = new JXLabel("<html><b>" + I18n.text("LBL Ranges"));
        title.setHorizontalTextPosition(JLabel.CENTER);
        title.setHorizontalAlignment(JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 12));
        // title.setForeground(Color.white);

        holder = new JXPanel(true);
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // scrollPane.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
        // scrollPane.setPreferredSize(new Dimension(800,600));
        scrollPane.setViewportView(holder);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(title);
        if (getConsole() == null)
            panel.add(new ToolbarButton(settingsAction));
        panel.add(new ToolbarButton(soundAction));
        panel.add(new ToolbarButton(showAction));
        panel.add(new ToolbarButton(resetAction));

        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        try {
            synth = MidiSystem.getSynthesizer();
            receiver = synth.getReceiver();
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Unable to start MIDI sound device"));
            if (OsInfo.getName() == OsInfo.Name.LINUX) {
                System.err.println(I18n.text("For midi, try running Neptus with the command 'padsp ./neptus.sh'."));
            }
        }

        initTracker();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#init()
     */
    @Override
    public void initSubPanel() {
        if (initCalled)
            return;
        initCalled = true;

        missionType = getConsole().getMission();
        try {
            reInitTracker();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            synth.open();
        }
        catch (Exception e) {
            if (OsInfo.getName() == OsInfo.Name.LINUX) {
                System.err.println(I18n
                        .text("Unable to open midi synthesizer. Try running Neptus with the command 'padsp ./neptus.sh'."));
            }
            else {
                NeptusLog.pub().error(e);
            }
        }

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {
        if (synth != null) { // In case something happened with MIDI initialization
            try {
                synth.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.removePostRenderPainter(this);
        }

        for (RangePainter rp : rangeFixPainter) {
            rp.cleanup();
        }
        rangeFixPainter.clear();

        for (LBLRangeLabel lrl : beacons.values()) {
            lrl.dispose();
        }
        beacons.clear();
        holder.removeAll();
        NeptusLog.pub().debug("lbl cleanup end");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        for (RangePainter rgp : rangeFixPainter) {
            rgp.setSecondsToDisplayRanges(secondsToDisplayRanges);
            rgp.setHideOrFadeRange(hideOrFadeRange == HideOrFadeRangeEnum.HIDE ? true : false);
            rgp.setAcceptedColor(acceptedColor);
            rgp.setRejectedColor(rejectedColor);
        }

//        if (!system.equals("main"))
//            NeptusLog.pub().info("<###>draw from other tree");
    }

    private void reset() {
        for (LBLRangeLabel lrl : beacons.values()) {
            lrl.dispose();
        }
        beacons.clear();
        holder.removeAll();
        reInitTracker();
    }

    public void initializeActions() {

        showAction = new AbstractAction(I18n.text("Show Tracker Data"), ICON_LBL_SHOW) {
            @Override
            public void actionPerformed(ActionEvent e) {
                String actionCmd = showAction.getValue(AbstractAction.SHORT_DESCRIPTION).toString();
                if (actionCmd.equals(I18n.text("Hide Tracker Data"))) {
                    showInRender = false;
                    showAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Show Tracker Data"));
                    showAction.putValue(AbstractAction.SMALL_ICON, ICON_LBL_HIDE);
                }
                else if (actionCmd.equals(I18n.text("Show Tracker Data"))) {
                    showInRender = true;
                    showAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Hide Tracker Data"));
                    showAction.putValue(AbstractAction.SMALL_ICON, ICON_LBL_SHOW);
                }
            }
        };
        showAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Hide Tracker Data"));

        resetAction = new AbstractAction(I18n.text("Reset"), ICON_RESET) {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
                // reInitTracker();
            }
        };
        resetAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Reset Tracker"));

        soundAction = new AbstractAction(I18n.text("Play Sound"), ICON_SOUND_ON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                String actionCmd = soundAction.getValue(AbstractAction.SHORT_DESCRIPTION).toString();
                if (actionCmd.equals(I18n.text("Don't Play Sound"))) {
                    playSound = false;
                    soundAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Play Sound"));
                    soundAction.putValue(AbstractAction.SMALL_ICON, ICON_SOUND_OFF);
                }
                else if (actionCmd.equals(I18n.text("Play Sound"))) {
                    playSound = true;
                    soundAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Don't Play Sound"));
                    soundAction.putValue(AbstractAction.SMALL_ICON, ICON_SOUND_ON);
                }
            }
        };
        soundAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Don't Play Sound"));
        // PropertiesEditor.editProperties(this, getConsole(), true);

        settingsAction = new AbstractAction(I18n.text("Settings"), ICON_SETTINGS) {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(LBLRangeDisplay.this, getConsole(), true);
            }
        };
        settingsAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Settings"));
    }

    /**
     * @param channel
     */
    private void playSound(int channel, boolean accepted) {
        if (receiver != null && playSound) {
            try {
                if (!accepted) {
                    channel = channel + numFreqs / 2;
                }
                msg.setMessage(ShortMessage.NOTE_ON, midi_channel, 50 + (16 / numFreqs) * channel, volume);
                receiver.send(msg, -1);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#mainVehicleChange(java.lang.String)
     */
    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        reset();
    }

    @Subscribe
    public void consume(LblConfig msg) {
        if (!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;
        if (msg.getOp() == LblConfig.OP.CUR_CFG)
            reInitTracker();
    }

//    private void updatePosition(int trans1, int trans2, double trans1ToVehicleDistance, double trans2ToVehicleDistance) {
//        try {
//            LocationType[] locArray = calculate(trans1, trans2, trans1ToVehicleDistance, trans2ToVehicleDistance);
//            if (locArray == null) {
//                // FIXI18N
//                NeptusLog.pub().debug(this + "\nInvalid fix for calculation!!");
//                return;
//            }
//            LocationType loc = fixLocationWithLastKnown(locArray, lastKnownPos);
//            lastKnownPos.setLocation(loc);
//            
//            lastCalcPosTimeMillis = System.currentTimeMillis();
//            if (scatter == null) {
//                scatter = new ScatterPointsElement(Color.GREEN.brighter());
//                scatter.setCenterLocation(new LocationType(hRef));
//                scatter.setNumberOfPoints(numberOfShownPoints);
//            }
//            if (scatter.getPoints().size() >= numberOfShownPoints) {
//                if (scatter.getPoints().size() > 0)
//                    scatter.getPoints().remove(0);
//            }
//            double[] distFromRef = loc.getOffsetFrom(hRef);
//            scatter.addPoint(distFromRef[0], distFromRef[1], distFromRef[2]);
//            post(new ConsoleEventPositionEstimation(this, ESTIMATION_TYPE.LBL_RANGES, loc));
//        }
//        catch (Exception e) {
//            NeptusLog.pub().error(e.getMessage(), e);
//        }
//    }

//    /**
//     * @param id
//     * @param range
//     * @param timeStampMillis
//     * @param reason
//     */
//    private void updateRangeRejected(long id, double range, long timeStampMillis, String reason) {
//
//        if (playSoundOnSurfaceRanges && !reason.equals("AT_SURFACE"))
//            playSound((int) id, false);
//
//        try {
//            int nTrans = transpondersList.size();
//            int indOrigin = (int) id;
//            int indPrevOrigin = (indOrigin - 1) % nTrans;
//            if (indPrevOrigin == -1)
//                indPrevOrigin = nTrans - 1;
//            updatePosition(indPrevOrigin, indOrigin, distances[indPrevOrigin], distancesPrev[indOrigin]);
//        }
//        catch (NumberFormatException e) {
//            e.printStackTrace();
//        }
//
//        LBLRangeLabel lb = beacons.get("" + id);
//
//        if (lb == null) {
//            lb = new LBLRangeLabel("" + id);
//            beacons.put("" + id, lb);
//            holder.add(lb);
//        }
//        lb.setRange(range);
//        lb.setTimeStampMillis(timeStampMillis);
//        lb.setAccepted(false, reason);
//
//        int idx = Integer.parseInt(("" + id).replaceAll("ch", ""));
//        paintRange(idx, range, false, reason);
//        distances[idx] = Double.NaN;
//    }
//
//    /**
//     * @param id
//     * @param range
//     * @param timeStampMillis
//     */
//    private void updateRangeAccepted(long id, double range, long timeStampMillis) {
//
//        playSound((int) id, true);
//        
//        try {
//            int nTrans = transpondersList.size();
//            int indOrigin = (int) id;
//            if (indOrigin == -1) {
//                // FIXI18N
//                NeptusLog.pub().debug(this + "\nTransponder " + id + " not found in list!");
//                return;
//            }
//            int indPrevOrigin = (indOrigin - 1) % nTrans;
//            if (indPrevOrigin == -1)
//                indPrevOrigin = nTrans - 1;
//            updatePosition(indPrevOrigin, indOrigin, distances[indPrevOrigin], range /*distances[indOrigin]*/);
//        }
//        catch (Exception e) {
//            NeptusLog.pub().error(e.getMessage(), e);
//        }
//
//        LBLRangeLabel lb = beacons.get("" + id);
//        if (lb == null) {
//            lb = new LBLRangeLabel("" + id);
//            beacons.put("" + id, lb);
//            holder.add(lb);
//        }
//        lb.setRange(range);
//        lb.setTimeStampMillis(timeStampMillis);
//        lb.setAccepted(true, null);
//
//        int idx = Integer.parseInt(("" + id).replaceAll("ch", ""));
//        paintRange(idx, range, true, null);
//        distancesPrev[idx] = distances[idx];
//        distances[idx] = range;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {

        AffineTransform identity = g2.getTransform();

        if (!showInRender)
            return;
        
        double alfaPercentage = 1.0;
        long deltaTimeMillis = System.currentTimeMillis() - lastCalcPosTimeMillis;
        if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 2.0) {
            alfaPercentage = 0.5;
        }
        else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 4.0) {
            alfaPercentage = 0.7;
        }
        // double rotationAngle = renderer.getRotationAngle();
//        Point2D centerPos = renderer.getScreenPosition(new LocationType(lastKnownPos));

        if (scatter != null) {
            // Graphics2D gS = (Graphics2D)g2.create();
            // gS.setTransform(new AffineTransform());
            scatter.paint(g2, renderer, -renderer.getRotation());
        }
        try {
            for (RangePainter rfp : rangeFixPainter.toArray(new RangePainter[0])) {
                rfp.paint(g2, renderer);
            }
        }
        catch (ConcurrentModificationException e) {
            e.printStackTrace();
        }

//        Point2D centerPos = renderer.getScreenPosition(new LocationType(lastKnownPos));
        Point2D centerPos = renderer.getScreenPosition(new LocationType(lblTriangulationHelper.getLastKnownPos()));

        // Paint system loc
        Graphics2D g = (Graphics2D) g2.create();
        g.setTransform(identity);
        g.setColor(new Color(255, 255, 255, (int) (255 * alfaPercentage)));
        g.draw(new Ellipse2D.Double(centerPos.getX() - 10, centerPos.getY() - 10, 20, 20));
        g.setColor(new Color(acceptedColor.getRed(), acceptedColor.getGreen(), acceptedColor.getBlue(),
                (int) (255 * alfaPercentage)));
        g.draw(new Ellipse2D.Double(centerPos.getX() - 12, centerPos.getY() - 12, 24, 24));
        g.setColor(new Color(255, 255, 255, (int) (255 * alfaPercentage)));
        g.draw(new Ellipse2D.Double(centerPos.getX() - 14, centerPos.getY() - 14, 28, 28));
        g.translate(centerPos.getX(), centerPos.getY());

        // g.setColor(new Color(255, 255, 0, (int) (200 * alfaPercentage)).darker());
        Color color = acceptedColor.darker();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (200 * alfaPercentage)));
        g.fill(new Ellipse2D.Double(-7, -7, 14, 14));
        // g.setColor(new Color(255, 255, 0, (int) (150 * alfaPercentage)).brighter());
        color = acceptedColor.brighter();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage)));
        g.setStroke(new BasicStroke(2));
        g.draw(new Ellipse2D.Double(-7, -7, 14, 14));
        g.setColor(new Color(255, 255, 255, (int) (150 * alfaPercentage)));
        g.fill(new Ellipse2D.Double(-2, -2, 4, 4));
        g.setColor(Color.BLACK);
        g.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanelChangeListener#subPanelChanged(pt.lsts.neptus.consolebase.
     * SubPanelChangeEvent)
     */
    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {
        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), ILayerPainter.class)) {

            ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

            if (panelChange.added()) {
                renderers.add(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.addPostRenderPainter(this, "LBL Tracker Range");
                }
            }

            if (panelChange.removed()) {
                renderers.remove(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.removePostRenderPainter(this);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.consolebase.MissionChangeListener#missionChange(pt.lsts.neptus.types.mission.MissionType
     * )
     */
    @Override
    public void missionReplaced(MissionType mission) {
        this.missionType = mission;
        reInitTracker();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.consolebase.MissionChangeListener#missionUpdated(pt.lsts.neptus.types.mission.MissionType
     * )
     */
    @Override
    public void missionUpdated(MissionType mission) {
        this.missionType = mission;
        reInitTracker();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mp.MapChangeListener#mapChanged(pt.lsts.neptus.mp.MapChangeEvent)
     */
    @Override
    public void mapChanged(MapChangeEvent mapChange) {
        reInitTracker();
    }

    /**
     * 
     */
    private void reInitTracker() {
//        transpondersList.clear();
        if (scatter != null)
            scatter.clearPoints();
        for (RangePainter rp : rangeFixPainter)
            rp.cleanup();
        rangeFixPainter.clear();
//        coordSystemsList.clear();
//        distanciesList.clear();
        initTracker();
    }

    private void initTracker() {
        LinkedHashMap<String, MapMission> mapList = missionType.getMapsList();

        ImcSystem system = ImcSystemsHolder.getSystemWithName(getMainVehicleId());
        if (system != null) {
            LblConfig lbl = (LblConfig) system.retrieveData(SystemUtils.LBL_CONFIG_KEY);
            Vector<LblBeacon> beaconList;

            try {
                beaconList = lbl.getBeacons();
            }
            catch (Exception e) {
                return;
            }

            ArrayList<TransponderElement> tal = new ArrayList<TransponderElement>();
            short duneId = 0;

            MapGroup mg = MapGroup.getMapGroupInstance(getConsole().getMission());
            for (LblBeacon tmp : beaconList) {
                tal.add(new TransponderElement(tmp, duneId, mg, mg.getMaps()[0]));
                duneId++;
            }

            for (TransponderElement tmp : tal) {
                transponders.add(tmp);

                RangePainter rgp = new RangePainter(tmp.getCenterLocation()) {
                    @Override
                    public void callParentRepaint() {
                        for (ILayerPainter str2d : renderers) {
                            ((ConsolePanel) str2d).repaint();
                        }
                    }
                };

                rgp.setSecondsToDisplayRanges(secondsToDisplayRanges);
                rgp.setHideOrFadeRange(hideOrFadeRange == HideOrFadeRangeEnum.HIDE ? true : false);
                rgp.setSquareColor(Color.GREEN);
                rgp.setAcceptedColor(acceptedColor);
                rgp.setRejectedColor(rejectedColor);
                rgp.setSurfaceColor(surfaceColor);
                rgp.setDrawRangeUpOrDownThePoint(drawRangeUpOrDownThePoint);
                rangeFixPainter.add(rgp);
            }
        }

        // HomeRef
        hRef = new HomeReference(missionType.getHomeRef().asXML());
        // System.err.println(hRef);
        hRef.setRoll(0d);
        hRef.setPitch(0d);
        hRef.setYaw(0d);
        locStart.setLocation(hRef);

        // locStart;
        boolean isFound = false;

        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        if (sys != null) {
            LocationType sysLoc = sys.getLocation();
            if (!sysLoc.equals(LocationType.ABSOLUTE_ZERO)) {
                startPos = new LocationType(sysLoc);
                isFound = true;
            }
        }
        
        if (!isFound) {
            for (MapMission mpm : mapList.values()) {
                // LinkedHashMap traList = mpm.getMap().getMarksList();
                LinkedHashMap<String, MarkElement> transList = mpm.getMap().getMarksList();
                for (MarkElement tmp : transList.values()) {
                    String name = tmp.getId();
                    if (name.equalsIgnoreCase("start")) {
                        locStart.setLocation(tmp.getCenterLocation());
                        isFound = true;
                        break;
                    }
                }
                if (isFound)
                    break;
            }
        }
        if (isFound) {
            startPos = new LocationType(locStart);
        }
        else {
            startPos = new LocationType(locStart);
        }

        try {
            if (lblTriangulationHelper == null) {
                lblTriangulationHelper = new LBLTriangulationHelper(transponders.toArray(new TransponderElement[0]),
                        startPos);
            }
            else {
                lblTriangulationHelper.reset(transponders.toArray(new TransponderElement[0]),
                        startPos);
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // add rangeFixPainters
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, "LBL Tracker Display");
        }

    }

//    private void initTracker() {
//        LinkedHashMap<String, MapMission> mapList = missionType.getMapsList();
//
//        for (MapMission mpm : mapList.values()) {
//            LinkedHashMap<String, TransponderElement> transList = mpm.getMap().getTranspondersList();
//
//            ArrayList<TransponderElement> tal = new ArrayList<TransponderElement>(transList.values());
//            // Let us order the beacons in alphabetic order (case insensitive)
//            Collections.sort(tal, new Comparator<TransponderElement>() {
//                @Override
//                public int compare(TransponderElement o1, TransponderElement o2) {
//                    return o1.getId().compareToIgnoreCase(o2.getId());
//                }
//            });
//
//            for (TransponderElement tmp : tal) {
//                transpondersList.add(tmp);
//
//                RangePainter rgp = new RangePainter(tmp.getCenterLocation()) {
//                    @Override
//                    public void callParentRepaint() {
//                        for (ILayerPainter str2d : renderers) {
//                            ((SubPanel) str2d).repaint();
//                        }
//                    }
//                };
//
//                rgp.setSecondsToDisplayRanges(secondsToDisplayRanges);
//                rgp.setHideOrFadeRange(hideOrFadeRange == HideOrFadeRangeEnum.HIDE ? true : false);
//                rgp.setSquareColor(Color.GREEN);
//                rgp.setAcceptedColor(acceptedColor);
//                rgp.setRejectedColor(rejectedColor);
//                rgp.setSurfaceColor(surfaceColor);
//                rgp.setDrawRangeUpOrDownThePoint(drawRangeUpOrDownThePoint);
//                rangeFixPainter.add(rgp);
//            }
//        }
//
//        // setupTranspondersConf();
//
//        // Init distances array
//        resetDistArray();
//
//        // HomeRef
//        hRef = new HomeReference(missionType.getHomeRef().asXML());
//        // System.err.println(hRef);
//        hRef.setRoll(0d);
//        hRef.setPitch(0d);
//        hRef.setYaw(0d);
//        locStart.setLocation(hRef);
//
//        // locStart;
//        boolean isFound = false;
//        for (MapMission mpm : mapList.values()) {
//            // LinkedHashMap traList = mpm.getMap().getMarksList();
//            LinkedHashMap<String, MarkElement> transList = mpm.getMap().getMarksList();
//            for (MarkElement tmp : transList.values()) {
//                String name = tmp.getName();
//                if (name.equalsIgnoreCase("start")) {
//                    locStart.setLocation(tmp.getCenterLocation());
//                    isFound = true;
//                    break;
//                }
//            }
//            if (isFound)
//                break;
//        }
//        if (isFound) {
//            lastKnownPos = new LocationType(locStart);
//            startPos = new LocationType(locStart);
//        }
//        else {
//            lastKnownPos = new LocationType();
//            startPos = new LocationType(locStart);
//        }
//
//        // Calc the CoordinateSystems
//        for (int i = 0; i < transpondersList.size(); i++) {
//            LocationType t1 = transpondersList.get(i).getCenterLocation();
//            LocationType t2;
//            if (i < (transpondersList.size() - 1))
//                t2 = transpondersList.get(i + 1).getCenterLocation();
//            else
//                t2 = transpondersList.getFirst().getCenterLocation();
//            double[] res = t1.getOffsetFrom(t2);
//            CoordinateUtil.cartesianToCylindricalCoordinates(res[0], res[1], res[2]);
//            double distance = t1.getHorizontalDistanceInMeters(new LocationType(t2));
//            double xyAngle = t1.getXYAngle(new LocationType(t2));
//            CoordinateSystem cs = new CoordinateSystem();
//            cs.setLocation(t1);
//            cs.setYaw(Math.toDegrees(xyAngle - Math.PI / 2));
//            cs.setId(t1.getId() + t2.getId());
//            cs.setName(cs.getId());
//            coordSystemsList.add(cs);
//            distanciesList.add(Double.valueOf(distance));
//        }
//
//        // add rangeFixPainters
//        for (ILayerPainter str2d : renderers) {
//            str2d.addPostRenderPainter(this, "LBL Tracker Display");
//        }
//    }
//
//    private void resetDistArray() {
//        // Init distances array
//        int ts = transpondersList.size();
//        if (ts > 0) {
//            distances = new double[ts];
//            distancesPrev = new double[ts];
//            for (int i = 0; i < distances.length; i++) {
//                distances[i] = Double.NaN;
//                distancesPrev[i] = Double.NaN;
//            }
//        }
//    }

    private void paintRange(int indOrigin, double distance, boolean accepted, String reason) {
        RangePainter rangePainter;
        try {
            rangePainter = rangeFixPainter.get(indOrigin);
            if (rangePainter != null) {
                rangePainter.updateGraphics(distance, accepted, reason);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage() + "   ::  " + this);
        }
    }

//    /**
//     * @param trans1
//     * @param trans2
//     * @param trans1ToVehicleDistance
//     * @param trans2ToVehicleDistance
//     * @return
//     */
//    private LocationType[] calculate(int trans1, int trans2, double trans1ToVehicleDistance,
//            double trans2ToVehicleDistance) {
//        CoordinateSystem cs = coordSystemsList.get(trans1);
//        double distance = distanciesList.get(trans1);
//
//        double da1 = trans1ToVehicleDistance;
//        double db1 = trans2ToVehicleDistance;
//        double paY = 0;
//        double pbY = distance;
//
//        String lat = cs.getLatitude();
//        String lon = cs.getLongitude();
//        double yawHR = cs.getYaw();
//        double[] cyl = CoordinateUtil.sphericalToCylindricalCoordinates(cs.getOffsetDistance(), cs.getAzimuth(),
//                cs.getZenith());
//        double legacyOffsetDistance = MathMiscUtils.round(cyl[0], 3);
//        double legacyTheta = MathMiscUtils.round(Math.toDegrees(cyl[1]), 3);
//        double legacyOffsetNorth = cs.getOffsetNorth();
//        double legacyOffsetEast = cs.getOffsetEast();
//
//        double t1Depth = 0;
//        double daH1 = Math.sqrt(Math.pow(da1, 2) - Math.pow(t1Depth, 2));
//        double dbH1 = Math.sqrt(Math.pow(db1, 2) - Math.pow(t1Depth, 2));
//        double offsetY = (Math.pow(daH1, 2) - Math.pow(dbH1, 2) + Math.pow(pbY, 2) - Math.pow(paY, 2))
//                / (2 * pbY - 2 * paY);
//        double offsetX = Math.sqrt(Math.pow(daH1, 2) - Math.pow(offsetY - paY, 2));
//
//        NeptusLog.pub().debug(this + "\n....... offsetX= " + offsetX + "    offsetY= " + offsetY);
//        if (Double.isNaN(offsetX) || Double.isNaN(offsetY))
//            return null;
//
//        double[] offsetsIne = CoordinateUtil.bodyFrameToInertialFrame(offsetX, offsetY, 0, 0, 0, Math.toRadians(yawHR));
//        double offsetNorth = MathMiscUtils.round(offsetsIne[0], 3) + legacyOffsetNorth;
//        double offsetEast = MathMiscUtils.round(offsetsIne[1], 3) + legacyOffsetEast;
//
//        double[] offsetsIne2 = CoordinateUtil.bodyFrameToInertialFrame(-offsetX, offsetY, 0, 0, 0,
//                Math.toRadians(yawHR));
//        double offsetNorth2 = MathMiscUtils.round(offsetsIne2[0], 3) + legacyOffsetNorth;
//        double offsetEast2 = MathMiscUtils.round(offsetsIne2[1], 3) + legacyOffsetEast;
//
//        LocationType loc = new LocationType();
//        loc.setLatitude(lat);
//        loc.setLongitude(lon);
//        loc.setDepth(t1Depth);
//        loc.setOffsetNorth(offsetNorth);
//        loc.setOffsetEast(offsetEast);
//        loc.setOffsetDistance(legacyOffsetDistance);
//        loc.setAzimuth(legacyTheta);
//
//        LocationType loc2 = new LocationType();
//        loc2.setLatitude(lat);
//        loc2.setLongitude(lon);
//        loc2.setDepth(t1Depth);
//        loc2.setOffsetNorth(offsetNorth2);
//        loc2.setOffsetEast(offsetEast2);
//        loc2.setOffsetDistance(legacyOffsetDistance);
//        loc2.setAzimuth(legacyTheta);
//
//        LocationType[] locArray = { loc, loc2 };
//
//        return locArray;
//    }
//
//    /**
//     * @param newLoc
//     * @param lasKnownLoc
//     * @return
//     */
//    private LocationType fixLocationWithLastKnown(LocationType[] newLocArray, LocationType lasKnownLoc) {
//        // FIXME hardcoded
//        lasKnownLoc = startPos;
//
//        LocationType fixedLoc = new LocationType();
//        LocationType newLoc = new LocationType(newLocArray[0]);
//        LocationType helperLoc = new LocationType(newLocArray[1]);
//
//        double newLocDist = lasKnownLoc.getDistanceInMeters(newLoc);
//        double lasKnownLocDist = lasKnownLoc.getDistanceInMeters(helperLoc);
//        if (newLocDist <= lasKnownLocDist) {
//            fixedLoc = newLoc;
//            NeptusLog.pub().debug(this + "\n" + newLocDist + " & " + lasKnownLocDist);
//        }
//        else {
//            fixedLoc = helperLoc;
//            NeptusLog.pub().debug(this + "\nSwitch!! " + newLocDist + " & " + lasKnownLocDist);
//        }
//        NeptusLog.pub().debug(this + "\n    " + newLoc.getDebugString());
//        NeptusLog.pub().debug(this + "\n    " + helperLoc.getDebugString());
//
//        return fixedLoc;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return new String[] { "LblRangeAcceptance", "LblRange" };
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#messageArrived(pt.lsts.neptus.imc.IMCMessage)
     */
    @Override
    public void messageArrived(IMCMessage message) {

        if (transponders.size() == 0) // In case there is no transponders don't bother with any message...
            return;

        if (message.getAbbrev().equals("LblRangeAcceptance")) {
            try {
                LblRangeAcceptance acceptance = LblRangeAcceptance.clone(message);

                LBLRangeLabel lb = beacons.get("" + acceptance.getId());
                if (lb == null) {
                    lb = new LBLRangeLabel("" + acceptance.getId());
                    beacons.put("" + acceptance.getId(), lb);
                    holder.add(lb);
                }
                lb.setRange(acceptance.getRange());
                lb.setTimeStampMillis(acceptance.getTimestampMillis());

                switch (acceptance.getAcceptance()) {
                    case ACCEPTED:
                        lblTriangulationHelper.updateRangeAccepted(
                                acceptance.getId(), acceptance.getRange(),
                                acceptance.getTimestampMillis());
                        paintRange(acceptance.getId(), acceptance.getRange(), true, "");
                        lb.setAccepted(true, "");
                        playSound((int) acceptance.getId(), true);
                        break;

                    default:
                        lblTriangulationHelper.updateRangeRejected(
                                acceptance.getId(), acceptance.getRange(),
                                acceptance.getTimestampMillis(), acceptance.getAcceptance().toString());
                        paintRange(acceptance.getId(), acceptance.getRange(), false,
                                I18n.text(acceptance.getAcceptance().toString()));
                        lb.setAccepted(false, I18n.text(acceptance.getAcceptance().toString()));
                        if (!acceptance.getAcceptance().equals(ACCEPTANCE.AT_SURFACE) || playSoundOnSurfaceRanges
                                && acceptance.getAcceptance().equals(ACCEPTANCE.AT_SURFACE))
                            playSound((int) acceptance.getId(), false);
                        break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (!useOnlyNewLblRangeAcceptance && message.getAbbrev().equals("LblRange")) {
            int id = message.getInteger("id");
            double range = message.getDouble("range");
            long timeStampMillis = message.getTimestampMillis();
            lblTriangulationHelper.updateRangeAccepted(id, range, timeStampMillis);
            paintRange(id, range, true, "");
            LBLRangeLabel lb = beacons.get("" + id);
            if (lb == null) {
                lb = new LBLRangeLabel("" + id);
                beacons.put("" + id, lb);
                holder.add(lb);
            }
            lb.setRange(range);
            lb.setTimeStampMillis(timeStampMillis);
            lb.setAccepted(true, "");
            playSound(id, true);
        }
        else if (!useOnlyNewLblRangeAcceptance && message.getAbbrev().equals("LblRangeRejection")) {
            int id = message.getInteger("id");
            double range = message.getDouble("range");
            long timeStampMillis = message.getTimestampMillis();
            String reason = message.getString("reason");
            lblTriangulationHelper.updateRangeRejected(id, range, timeStampMillis, reason);
            paintRange(id, range, false, I18n.text(reason));
            LBLRangeLabel lb = beacons.get("" + id);
            if (lb == null) {
                lb = new LBLRangeLabel("" + id);
                beacons.put("" + id, lb);
                holder.add(lb);
            }
            lb.setRange(range);
            lb.setTimeStampMillis(timeStampMillis);
            lb.setAccepted(false, I18n.text(reason));
            if (!reason.equals("AT_SURFACE") || playSoundOnSurfaceRanges
                    && reason.equals("AT_SURFACE"))
                playSound(id, false);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        NeptusMain.main(new String[] { "./conf/consoles/seacon-light.ncon" });
    }
}
