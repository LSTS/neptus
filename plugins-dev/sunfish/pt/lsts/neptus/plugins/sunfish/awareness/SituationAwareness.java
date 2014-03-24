/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.reflections.Reflections;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.IConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.speech.SpeechUtil;

/**
 * @author zp
 * 
 */
@PluginDescription(name="Situation Awareness")
public class SituationAwareness extends ConsoleInteraction implements IConsoleLayer, Renderer2DPainter {

    private LinkedHashMap<String, AssetTrack> assets = new LinkedHashMap<String, AssetTrack>();
    private Vector<ILocationProvider> localizers = new Vector<ILocationProvider>();
    private Vector<IPeriodicUpdates> updaters = new Vector<IPeriodicUpdates>();
    private AssetPosition intercepted = null;
    private JDialog dialogDecisionSupport;
    private DecisionSupportTable supportTable = new DecisionSupportTable();
    
    //@NeptusProperty
    //public String shipAsset = "lauv-xtreme-2";

    @NeptusProperty
    public String uuvAsset = "lauv-xplore-1";
    
    @NeptusProperty
    public double shipSpeedMps = 10;
    
    @NeptusProperty
    public double uuvSpeedMps = 1.25;
    
    @NeptusProperty
    public boolean audibleUpdates = true;
    
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

        for (MapPanel map : getConsole().getSubPanelsOfClass(MapPanel.class))
            map.addLayer(this);
    }

    public void addAssetPosition(AssetPosition pos) {
        String asset = pos.getAssetName();
        if (!assets.containsKey(asset)) {
            java.util.Random rnd = new java.util.Random();
            AssetTrack track = new AssetTrack(asset, new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255)));
            assets.put(asset, track);
        }
        AssetTrack track = assets.get(asset);
        boolean newPos = track.addPosition(pos);

        if (newPos && (track.getLatest() == null || track.getLatest().getAge() > 30000)) {
            getConsole().post(Notification.info("New Position", "Received position for " + pos.getAssetName()));
            if (audibleUpdates)
                SpeechUtil.readSimpleText(track.getAssetName()+" has been updated");      
        }
        
        if (newPos) {
            logPosition(pos);
        }
    }
    
    private void logPosition(AssetPosition pos) {
        //TODO
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
            g.drawString(pivot.getAssetName() + ", age: " + getAge(pivot), 10, source.getHeight() - 50);
        }

        for (AssetTrack t : assets.values()) {
            AssetPosition prev = t.getLatest();
            AssetPosition pred = t.getPrediction();
            if (prev != null && pred != null) {
                g.setColor(t.getColor());
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[] { 3f, 3f }, 0));
                g.draw(new Line2D.Double(source.getScreenPosition(prev.getLoc()), source.getScreenPosition(pred
                        .getLoc())));
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

        ColorMap cmap = ColorMapFactory.createRedYellowGreenColorMap();
        
        if (event.getButton() == MouseEvent.BUTTON3) {
            LinkedHashMap<String, Vector<AssetPosition>> positions = positionsByType();
            JPopupMenu popup = new JPopupMenu();
            for (String type : positions.keySet()) {
                JMenu menu = new JMenu(type + "s");
                for (final AssetPosition p : positions.get(type)) {
                    
                    Color c = cmap.getColor(1-(p.getAge() / (3600000.0)));
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
                    tags.addAll(positions.get("SPOT Tag"));
                    //FIXME add argos tags
                    supportTable.setAssets(assets.get(uuvAsset).getLatest(), tags);
                    dialogDecisionSupport.setContentPane(new JScrollPane(new JXTable(supportTable)));
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

            List<AssetPosition> positions = track.getTrack(15, 0);
            Point2D lastLoc = null;
            for (AssetPosition p : positions) {
                Point2D pt = renderer.getScreenPosition(p.getLoc());
                g.setColor(track.getColor());
                if (lastLoc != null) {
                    g.draw(new Line2D.Double(lastLoc, pt));
                }
                g.fill(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
                lastLoc = pt;
            }
            g.setColor(Color.orange);
            if (lastLoc != null)
                g.draw(new Ellipse2D.Double(lastLoc.getX() - radius, lastLoc.getY() - radius, radius * 2, radius * 2));
        }
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
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        for (AssetTrack track : assets.values()) {
            for (AssetPosition p : track.getTrack(15, 0)) {
                double dist = event.getPoint().distance(source.getScreenPosition(p.getLoc()));
                if (dist < 5) {
                    intercepted = p;
                    return;
                }
            }
        }
        intercepted = null;
    }
}
