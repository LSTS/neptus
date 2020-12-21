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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXTable;
import org.reflections.Reflections;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.IConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.gui.swing.RangeSlider;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.sunfish.awareness.SunfishAssetProperties.AssetDesc;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.speech.SpeechUtil;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Situation Awareness", icon = "pt/lsts/neptus/plugins/sunfish/awareness/lamp.png")
@LayerPriority(priority=20)
public class SituationAwareness extends ConsoleInteraction implements IConsoleLayer, Renderer2DPainter,
        ConfigurationListener {

    private Random random = new Random();

    private LinkedHashMap<String, AssetTrack> assets = new LinkedHashMap<String, AssetTrack>();
    private Vector<ILocationProvider> localizers = new Vector<ILocationProvider>();
    private Vector<IPeriodicUpdates> updaters = new Vector<IPeriodicUpdates>();
    private AssetPosition intercepted = null;
    private JDialog dialogDecisionSupport;
    private DecisionSupportTable supportTable = new DecisionSupportTable();
    private HashSet<String> updateMethodNames = new HashSet<String>();
    private HashSet<String> hiddenPosTypes = new HashSet<String>();
    private Image argos, spot, desired, target, unknown, auv, uav, ship, ccu, wg;
    private SunfishAssetProperties props = new SunfishAssetProperties();
    private LinkedHashMap<String, SunfishAssetProperties.AssetDesc> assetProperties = new LinkedHashMap<>();
    private RangeSlider slider = new RangeSlider();
    private JLabel minTimeLabel = new JLabel(""), maxTimeLabel = new JLabel("");
    private SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm");
    ColorMap cmap = ColorMapFactory.createRedYellowGreenColorMap();
    ColorMap cmap2 = new InterpolationColorMap("fading map", 
            new double[] {0.0, 0.25, 0.5, 0.75, 1.0}, 
            new Color[] {new Color(0,0,0,0), new Color(0,0,0), new Color(255,0,0), new Color(255,255,0), new Color(0,255,0)});
    
    private long oldestTimestamp = new Date().getTime();        
    private long newestTimestamp = 0;
    
    private long oldestTimestampSelection = new Date().getTime();        
    private long newestTimestampSelection = 0;
    
    @NeptusProperty(name = "Ship speed (m/s)")
    public double shipSpeedMps = 10;

    @NeptusProperty(name = "AUV speed (m/s)")
    public double uuvSpeedMps = 1.25;

    @NeptusProperty(name = "Tag safety distance (meters)", description = "Minimum distance the ship can be from the tag position")
    public double minDist = 3000;

    @NeptusProperty(name = "Audible position updates")
    public boolean audibleUpdates = true;

    @NeptusProperty(name = "Location sources", editable = false) 
    public String updateMethods = "";

    @NeptusProperty(name = "Hidden position types", editable = false) 
    public String hiddenTypes = "";

    //@NeptusProperty(name = "Maximum position age (hours)")
    //public double maxAge = 12;
    
    //@NeptusProperty(name = "Maximum number of positions per system")
    //public int maxPositions = 15;

    
    @NeptusProperty(name = "Paint labels")
    public boolean paintLabels = true;
    
    @NeptusProperty(name = "Paint icons")
    public boolean paintIcons = false;
    
 
    void postNotification(Notification notification) {
        getConsole().post(notification);    
    }
    
    private void loadImages() {
        spot = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/spot.png");
        desired = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/desired.png");
        target = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/target.png");
        unknown = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/unknown.png");
        auv = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/auv.png");
        uav = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/uav.png");
        ship = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/ship.png");
        ccu = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/ccu.png");
        argos = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/argos.png");
        wg = ImageUtils.getImage("pt/lsts/neptus/plugins/sunfish/wg.png");
    }
    
    @Periodic(millisBetweenUpdates=1000 * 60 * 15)
    public void fetchAssetProperties() {
        NeptusLog.pub().info("Fetching asset properties");
        for (AssetDesc a : props.fetchAssets()) {
            assetProperties.put(a.name, a);
        }        
    }
    
    @Override
    public void initInteraction() {
        Reflections reflections = new Reflections(getClass().getPackage().getName());

        for (Class<? extends ILocationProvider> c : reflections.getSubTypesOf(ILocationProvider.class)) {
            try {
                ILocationProvider localizer = c.newInstance();
                updaters.addAll(PeriodicUpdatesService.inspect(localizer));
                localizer.onInit(this);
                localizers.add(localizer);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }

        for (IPeriodicUpdates upd : updaters) {
            PeriodicUpdatesService.register(upd);
        }

        if (getConsole() != null)
            for (MapPanel map : getConsole().getSubPanelsOfClass(MapPanel.class))
                map.addLayer(this);
        
        loadImages();        
        propertiesChanged();
        
        Thread t = new Thread("Asset Properties Loader") {
            public void run() {
                fetchAssetProperties();
                
                try {
                    Collection<AssetPosition> dailyPositions = PositionHistory.getHistory();
                    for (AssetPosition p : dailyPositions) {
                        p.setSource("Daily Positions CSV");
                        addAssetPosition(p);                        
                    }
                    slider.setValue(slider.getUpperValue() - 3600 * 24);                    
                }
                catch (Exception e) {
                    e.printStackTrace();
                }                           
            }
        };
        t.setDaemon(true);
        t.start();
        
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                minTimeLabel.setText(fmt.format(new Date(slider.getValue() * 1000l)));
                maxTimeLabel.setText(fmt.format(new Date(slider.getUpperValue() * 1000l)));
                oldestTimestampSelection = slider.getValue() * 1000l;
                newestTimestampSelection = (slider.getValue() + slider.getExtent()) * 1000l;
                if (slider.getUpperValue() - slider.getValue() < 3600) {
                    slider.setUpperValue(Math.min(slider.getMaximum(), slider.getValue()+3600));
                }
                    
            }
        });
    }

    @Override
    public void propertiesChanged() {
        updateMethodNames.clear();
        for (String s : updateMethods.split("\\s*,\\s*"))
            updateMethodNames.add(s);

        hiddenPosTypes.clear();
        for (String s : hiddenTypes.split("\\s*,\\s*"))
            hiddenPosTypes.add(s);

        for (ILocationProvider prov : localizers) {
            prov.setEnabled(updateMethodNames.contains(prov.getName()));
        }
        supportTable.setShipSpeed(shipSpeedMps);
        supportTable.setAuvSpeed(uuvSpeedMps);
    }

    private long minDate = new Date().getTime() - 30 * 1000 * 2600 * 24;
    public void addAssetPosition(AssetPosition pos) {
        if (pos.getTimestamp() < oldestTimestamp && pos.getTimestamp() > minDate) {
            oldestTimestamp = pos.getTimestamp();
            slider.setMinimum((int)(oldestTimestamp / 1000));
            minTimeLabel.setText(fmt.format(new Date(oldestTimestamp)));
        }
        
        if (pos.getTimestamp() > newestTimestamp) {
            newestTimestampSelection = newestTimestamp = pos.getTimestamp();
            slider.setMaximum((int)(newestTimestamp / 1000));
            maxTimeLabel.setText(fmt.format(new Date(newestTimestamp)));
            slider.setUpperValue((int)(newestTimestamp / 1000));
        }        
        
        String asset = pos.getAssetName();
        if (!assets.containsKey(asset)) {
            AssetTrack track = new AssetTrack(asset, new Color(random.nextInt(255), random.nextInt(255),
                    random.nextInt(255)));
            assets.put(asset, track);
        }
        AssetTrack track = assets.get(asset);
        boolean newPos = track.addPosition(pos);

        if (newPos && (track.getLatest() == null || track.getLatest().getAge() > 30000)) {
            if (getConsole() != null)
                getConsole().post(Notification.info("New Position", "Received position for " + pos.getAssetName()));
            if (audibleUpdates && pos.getTimestamp() > oldestTimestamp)
                SpeechUtil.readSimpleText(track.getAssetName() + " has been updated");
        }

        if (newPos) {
            logPosition(pos);
        }
    }

    private void logPosition(AssetPosition pos) {
        // TODO
    }

    public void setAssetColor(String assetName, Color c) {
        if (assets.containsKey(assetName)) {
            assets.get(assetName).setColor(c);
        }
    }

    @Override
    public void cleanInteraction() {
        for (ILocationProvider localizer : localizers)
            localizer.onCleanup();

        for (IPeriodicUpdates upd : updaters) {
            PeriodicUpdatesService.unregister(upd);
        }
    }

    public static void main(String[] args) throws Exception {
        SituationAwareness aware = new SituationAwareness();
        aware.initInteraction();
        Thread.sleep(100000);
    }

    private JLabel lbl = new JLabel();

    public void paintIcons(Graphics2D g, StateRenderer2D renderer) {
        for (AssetTrack track : assets.values()) {
            AssetPosition p = track.getLatest(newestTimestampSelection);
            if (p == null || hiddenPosTypes.contains(p.getType()))
                continue;
            if (p.getTimestamp() < oldestTimestampSelection || p.getTimestamp() > newestTimestampSelection)
                continue;
            Point2D pt = renderer.getScreenPosition(p.getLoc());
            
            //TODO paint icon here
            Image img = getIcon(p);
            if (img != null) {
                g.drawImage(img, (int)(pt.getX()-8), (int)(pt.getY()-8), (int)(pt.getX()+8), (int)(pt.getY()+8), 0, 0, img.getWidth(null), img.getHeight(null), null);
            }
        }
    }
    
    public Image getIcon(AssetPosition pos) {
        if (pos.getAssetName().equals("hermes"))
            return wg;
        switch (pos.getType().toLowerCase()) {
            case "ship":
                return ship;
            case "uuv":
            case "auv":
                return auv;
            case "uav":
                return uav;
            case "ccu":
                return ccu;
            case "argos tag":
                return argos;
            case "spot tag":
                return spot;
            case "desired position":
                return desired;
            case "target position":
                return target;
            default:
                return unknown;        
        }
        
    }
    
    public void paintLabels(Graphics2D g, StateRenderer2D renderer) {
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        
        for (AssetTrack track : assets.values()) {
            AssetPosition p = track.getLatest(newestTimestampSelection);
            
            if (p == null || hiddenPosTypes.contains(p.getType()))
                continue;

            if (p.getTimestamp() < oldestTimestampSelection || p.getTimestamp() > newestTimestampSelection)
                continue;

            Point2D pt = renderer.getScreenPosition(p.getLoc());
            g.setColor(track.getColor());

            g.setColor(Color.black);
            String name = p.getAssetName();//assetProperties.containsKey(p.getAssetName()) ? assetProperties.get(p.getAssetName()).friendly : p.getAssetName();
            g.drawString(
                    name + " ("
                            + DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - p.getTimestamp())
                            + ")", (int) (pt.getX() + 13), (int) (pt.getY() + 5));
        }
    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        super.paintInteraction(g, source);
        g.setStroke(new BasicStroke(1f));
        paint(g, source);
        AssetPosition pivot = intercepted;
        if (pivot != null) {
            Point2D pt = source.getScreenPosition(pivot.getLoc());
            g.setColor(Color.white);
            g.draw(new Ellipse2D.Double(pt.getX() - 6, pt.getY() - 6, 12, 12));
            if (assetProperties.containsKey(pivot.getAssetName()))
                pivot.putExtra("Description", assetProperties.get(pivot.getAssetName()).description);   
            if (assetProperties.containsKey(pivot.getAssetName()))
                pivot.putExtra("Friendly name", assetProperties.get(pivot.getAssetName()).friendly);   
            
            lbl.setOpaque(true);
            lbl.setBackground(new Color(255, 255, 255, 128));
            lbl.setText(pivot.getHtml());
            Dimension d = lbl.getPreferredSize();
            lbl.setSize(d);
            Graphics copy = g.create();
            copy.translate(10, 10);
            lbl.paint(copy);
        }

        for (AssetTrack t : assets.values()) {
            AssetPosition prev = t.getLatest();
            AssetPosition pred = t.getPrediction();

            if (prev != null && pred != null) {
                if (prev.getTimestamp() < oldestTimestampSelection || prev.getTimestamp() > newestTimestampSelection)
                    continue;
                g.setColor(new Color(t.getColor().getRed(), t.getColor().getGreen(), t.getColor().getBlue(), 128));
                Point2D pt1 = source.getScreenPosition(prev.getLoc());
                Point2D pt2 = source.getScreenPosition(pred.getLoc());
                if (pt1.distance(pt2) < 1000)
                    g.draw(new Line2D.Double(pt1, pt2));
            }
        }
    }

    private String getAge(AssetPosition position) {
        return DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - position.getTimestamp());
    }

    private LinkedHashMap<String, Vector<AssetPosition>> positionsByType() {
        LinkedHashMap<String, Vector<AssetPosition>> ret = new LinkedHashMap<String, Vector<AssetPosition>>();

        for (AssetTrack t : assets.values()) {
            AssetPosition last = t.getLatest();
            if (!ret.containsKey(last.getType())) {
                ret.put(last.getType(), new Vector<AssetPosition>());
            }
            ret.get(last.getType()).add(last);
        }
        return ret;
    }

    @Override
    public void mouseClicked(MouseEvent event, final StateRenderer2D source) {

        

        if (event.getButton() == MouseEvent.BUTTON3) {
            final LinkedHashMap<String, Vector<AssetPosition>> positions = positionsByType();
            JPopupMenu popup = new JPopupMenu();
            for (String type : positions.keySet()) {
                JMenu menu = new JMenu(type + "s");
                for (final AssetPosition p : positions.get(type)) {

                    if (p.getTimestamp() < oldestTimestampSelection || p.getTimestamp() > newestTimestampSelection)
                        continue;

                    Color c = cmap.getColor(1 - (p.getAge() / (7200000.0)));
                    String htmlColor = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
                    menu.add(
                            "<html><b>" + p.getAssetName() + "</b> <font color=" + htmlColor + ">" + getAge(p)
                                    + "</font>").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            source.focusLocation(p.getLoc());
                        }
                    });
                }
                popup.add(menu);
            }

            popup.addSeparator();
            popup.add("Settings").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    PluginUtils.editPluginProperties(SituationAwareness.this, true);
                }
            });
            
            popup.add("Fetch asset properties").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
//                    for (AssetTrack track : assets.values()) {
//                        track.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
//                    }
                    fetchAssetProperties();
                }
            });

            popup.add("Select location sources").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    JPanel p = new JPanel();
                    p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

                    for (ILocationProvider l : localizers) {
                        JCheckBox check = new JCheckBox(l.getName());
                        check.setSelected(true);
                        p.add(check);
                        check.setSelected(updateMethodNames.contains(l.getName()));
                    }

                    int op = JOptionPane.showConfirmDialog(getConsole(), p, "Location update sources",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (op == JOptionPane.CANCEL_OPTION)
                        return;

                    Vector<String> methods = new Vector<String>();
                    for (int i = 0; i < p.getComponentCount(); i++) {

                        if (p.getComponent(i) instanceof JCheckBox) {
                            JCheckBox sel = (JCheckBox) p.getComponent(i);
                            if (sel.isSelected())
                                methods.add(sel.getText());
                        }
                    }
                    updateMethods = StringUtils.join(methods, ", ");
                    propertiesChanged();
                }
            });

            popup.add("Select hidden positions types").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    JPanel p = new JPanel();
                    p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

                    for (String type : positions.keySet()) {
                        JCheckBox check = new JCheckBox(type);
                        check.setSelected(true);
                        p.add(check);
                        check.setSelected(hiddenPosTypes.contains(type));
                    }

                    int op = JOptionPane.showConfirmDialog(getConsole(), p, "Position types to be hidden",
                            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (op == JOptionPane.CANCEL_OPTION)
                        return;

                    Vector<String> types = new Vector<String>();
                    for (int i = 0; i < p.getComponentCount(); i++) {

                        if (p.getComponent(i) instanceof JCheckBox) {
                            JCheckBox sel = (JCheckBox) p.getComponent(i);
                            if (sel.isSelected())
                                types.add(sel.getText());
                        }
                    }
                    hiddenTypes = StringUtils.join(types, ", ");
                    propertiesChanged();
                }
            });

            popup.addSeparator();
            popup.add("Decision Support").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (dialogDecisionSupport == null) {
                        dialogDecisionSupport = new JDialog(getConsole());
                        dialogDecisionSupport.setModal(false);
                        dialogDecisionSupport.setAlwaysOnTop(true);
                        dialogDecisionSupport.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
                    }
                    ArrayList<AssetPosition> tags = new ArrayList<AssetPosition>();
                    LinkedHashMap<String, Vector<AssetPosition>> positions = positionsByType();
                    Vector<AssetPosition> spots = positions.get("SPOT Tag");
                    Vector<AssetPosition> argos = positions.get("Argos Tag");
                    if (spots != null)
                        tags.addAll(spots);
                    if (argos != null)
                        tags.addAll(argos);
                    if (!assets.containsKey(getConsole().getMainSystem())) {
                        GuiUtils.errorMessage(getConsole(), "Decision Support", "UUV asset position is unknown");
                        return;
                    }
                    supportTable.setAssets(assets.get(getConsole().getMainSystem()).getLatest(), tags);
                    JXTable table = new JXTable(supportTable);
                    dialogDecisionSupport.setContentPane(new JScrollPane(table));
                    dialogDecisionSupport.invalidate();
                    dialogDecisionSupport.validate();
                    dialogDecisionSupport.setSize(600, 300);
                    dialogDecisionSupport.setTitle("Decision Support Table");
                    dialogDecisionSupport.setVisible(true);
                    dialogDecisionSupport.toFront();
                    GuiUtils.centerOnScreen(dialogDecisionSupport);
                }
            });
            popup.show(source, event.getX(), event.getY());
        }
        super.mouseClicked(event, source);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        double radius = isActive() ? 6 : 2.5;
        for (AssetTrack track : assets.values()) {
            List<AssetPosition> positions = track.getTrack();
            Point2D lastLoc = null;
            long lastAge = 0;
            for (AssetPosition p : positions) {
                if (hiddenPosTypes.contains(p.getType()))
                    continue;

                if (p.getTimestamp() < oldestTimestampSelection || p.getTimestamp() > newestTimestampSelection)
                    continue;
                
                //if (p.getAge() >= maxAge * 3600 * 1000)
                //    continue;
                Point2D pt = renderer.getScreenPosition(p.getLoc());
                if (assetProperties.containsKey(track.getAssetName()))
                    g.setColor(assetProperties.get(track.getAssetName()).color);                
                else
                    g.setColor(track.getColor());
                if (lastLoc != null && lastLoc.distance(pt) < 20000) {
                    g.draw(new Line2D.Double(lastLoc, pt));
                }
                g.fill(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
                lastLoc = pt;
                lastAge = p.getAge();
            }
            g.setStroke(new BasicStroke(2.0f));
            if (lastLoc != null) {
                Color c = cmap2.getColor(1 - (lastAge / (7200000.0)));
                g.setColor(c);
                g.setStroke(new BasicStroke(2.0f));
                g.draw(new Ellipse2D.Double(lastLoc.getX() - radius-1.5, lastLoc.getY() - radius-1.5, radius * 2 + 3, radius * 2 + 3));
            }
        }
        
        if (paintLabels)
            paintLabels(g, renderer);
        
        if (paintIcons)
            paintIcons(g, renderer);
    }
    
    public static void exportKml() {
        
    }

    @Override
    public float getOpacity() {
        return 1.0f;
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void setOpacity(float opacity) {

    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        Container parent = source.getParent();
        while (parent != null && !(parent.getLayout() instanceof BorderLayout)) 
            parent = parent.getParent();
        if (mode) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(slider, BorderLayout.CENTER);
            panel.add(minTimeLabel, BorderLayout.WEST);
            panel.add(maxTimeLabel, BorderLayout.EAST);
            parent.add(panel, BorderLayout.SOUTH);
        }
        else {
            parent = slider.getParent().getParent();
            parent.remove(slider.getParent());
        }
        parent.invalidate();
        parent.validate();
        parent.repaint();
        
    }
    


    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {

        List<AssetPosition> allPositions = new ArrayList<AssetPosition>();

        for (AssetTrack track : assets.values())
            allPositions.addAll(track.getTrack());

        Collections.sort(allPositions);            

        for (AssetPosition p : allPositions) {
            if (p.getTimestamp() < oldestTimestampSelection || p.getTimestamp() > newestTimestampSelection)
                continue;
            
            double dist = event.getPoint().distance(source.getScreenPosition(p.getLoc()));
            if (dist < 5) {
                intercepted = p;
                return;
            }
        }

        intercepted = null;
    }
}
