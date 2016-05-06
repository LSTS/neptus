/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * 05/05/2016
 */
package pt.lsts.neptus.historicdata;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.Future;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.HistoricData;
import pt.lsts.imc.HistoricDataQuery;
import pt.lsts.imc.HistoricDataQuery.TYPE;
import pt.lsts.imc.HistoricEvent;
import pt.lsts.imc.historic.DataSample;
import pt.lsts.imc.historic.DataStore;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.historicdata.HistoricGroundOverlay.DATA_TYPE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * This plugin is used to retrieve and display historic data stored in the vehicles and on the web (Ripples)
 * 
 * @author zp
 *
 */
@PluginDescription(name = "Historic Data", icon="pt/lsts/neptus/historicdata/rewind_icon.png")
public class HistoricDataInteraction extends ConsoleInteraction {

    private DataStore dataStore = new DataStore();
    private LinkedHashMap<String, Long> lastPollTime = new LinkedHashMap<>();
    private int req_id = 1;
    private LinkedHashMap<String, ArrayList<RemotePosition>> positions = new LinkedHashMap<>();
    private LinkedHashMap<String, PathElement> positionCache = new LinkedHashMap<>();
    private ArrayList<RemoteEvent> events = new ArrayList<>();
    private ArrayList<RemoteEvent> mouseOver = new ArrayList<>();
    private HistoricGroundOverlay overlay = null;
    private HistoricWebAdapter webAdapter = new HistoricWebAdapter(dataStore);
    private long lastWebSync = System.currentTimeMillis();
    
    private Future<Boolean> ongoingPost = null, ongoingRequest = null;
    
    @NeptusProperty(name = "Automatically poll data from vehicles")
    private boolean pollFromVehicles = true;
    
    @NeptusProperty(name = "Seconds between vehicle data polling")
    private int secsBetweenPolling = 60;
    
    @NeptusProperty(name = "Automatically synchronize data with Ripples (Web)")
    private boolean syncWithRipples = true;
    
    @NeptusProperty(name = "Seconds between Ripples synchronizations")
    private int secsBetweenSyncing = 60;
    
    @NeptusProperty(name = "Data to display in ground overlay")
    private DATA_TYPE overlayType = DATA_TYPE.None;
    
    @NeptusProperty(name = "Ground overlay colormap")
    private ColorMap overlayColormap = ColorMapFactory.createJetColorMap();
    
    @Override
    public void initInteraction() {
        Vector<HistoricGroundOverlay> overlays = getConsole().getMapPluginsOfType(HistoricGroundOverlay.class);
        if (overlays.isEmpty()) {
            overlay = new HistoricGroundOverlay();
            getConsole().addMapLayer(overlay);
        }
        else {
            overlay = overlays.firstElement();
        }
        
        propertiesChanged();
    }

    @Override
    public void cleanInteraction() {
        getConsole().removeMapLayer(overlay);
    }
    
    @Override
    public void propertiesChanged() {
        super.propertiesChanged();
        overlay.setColormap(overlayColormap);
        overlay.setTypeToPaint(overlayType);
        overlay.resetImage();
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        super.mouseMoved(event, source);
        synchronized (mouseOver) {
            mouseOver = new ArrayList<>();
            for (RemoteEvent evt : events) {
                Point2D pt = source.getScreenPosition(evt.location);
                if (pt.distance(event.getPoint()) <= 3.0) {
                    mouseOver.add(evt);
                }
            }
            Collections.sort(mouseOver);    
        }        
    }
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (SwingUtilities.isRightMouseButton(event))
            rightClick(event.getPoint(), source);
    }

    @Subscribe
    public void on(Heartbeat beat) {
        if (ImcSystemsHolder.lookupSystem(beat.getSrc()).getType() == SystemTypeEnum.VEHICLE) {
            if (lastPollTime.containsKey(beat.getSourceName())) {
                if (System.currentTimeMillis() - lastPollTime.get(beat.getSourceName()) < secsBetweenPolling * 1000) {
                    return;
                }
            }
            pollDataFrom(beat.getSourceName());
        }
    }
    
    @Periodic(millisBetweenUpdates=5000)
    public void webSync() {
        if (!syncWithRipples)
            return;
        if (System.currentTimeMillis() - lastWebSync < secsBetweenSyncing * 1000)
            return;
        
        uploadLocalData(null);
        pollWeb(null);
        
        lastWebSync = System.currentTimeMillis();
    }

    private void pollDataFrom(String system) {
        HistoricDataQuery req = new HistoricDataQuery();
        req.setReqId(req_id++);
        req.setType(TYPE.QUERY);
        req.setMaxSize(64000);
        NeptusLog.pub().info("Polling historic data from " + system);
        lastPollTime.put(system, System.currentTimeMillis());
        getConsole().getImcMsgManager().sendMessageToSystem(req, system);
    }
    
    public void process(HistoricData incoming) {
        ArrayList<DataSample> newSamples = DataSample.parseSamples(incoming);
        Collections.sort(newSamples);
        overlay.process(incoming);
        
        for (DataSample sample : DataSample.parseSamples(incoming)) {
            LocationType loc = new LocationType(sample.getLatDegs(), sample.getLonDegs());
            loc.setDepth(sample.getzMeters());
            String system = ImcSystemsHolder.translateImcIdToSystemName(sample.getSource());
            
            if (!positions.containsKey(system))
                positions.put(system, new ArrayList<>());
            positions.get(system).add(new RemotePosition(sample.getTimestampMillis(), loc));
            
            if (sample.getSample().getMgid() == HistoricEvent.ID_STATIC) {
                RemoteEvent evt = new RemoteEvent();
                evt.event = (HistoricEvent)sample.getSample();
                evt.location = loc;
                evt.system = system;
                evt.time = new Date(sample.getTimestampMillis());
                events.add(evt);
            }
        }
        
        for (String key : positions.keySet()) {
            Collections.sort(positions.get(key));
            PathElement el = new PathElement();
            LocationType center = positions.get(key).get(0).getLocation(); 
            el.centerLocation.setLocation(center);
            for (RemotePosition l : positions.get(key))
                el.addPoint(l.getLocation());
            el.setFilled(false);
            el.setShape(false);
            try {
                el.setMyColor(ImcSystemsHolder.getSystemWithName(key).getVehicle().getIconColor());
            }
            catch (Exception e) {
                el.setMyColor(Color.blue);
            }
                
            positionCache.put(key, el);
        }
    }

    @Subscribe
    public void on(HistoricDataQuery query) {
        try {
            if (query.getType() == HistoricDataQuery.TYPE.REPLY) {
                dataStore.addData(query.getData());
                process(query.getData());
                HistoricDataQuery clear = new HistoricDataQuery();
                clear.setType(TYPE.CLEAR);
                clear.setData(null);
                clear.setReqId(query.getReqId());
                NeptusLog.pub().debug("Clearing received data from " + query.getSourceName());
                getConsole().getImcMsgManager().sendMessageToSystem(clear, query.getSourceName());
    
                // If message's size is near the requested size, retrieve more data
                if (query.getSize() > 50000)
                    pollDataFrom(query.getSourceName());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rightClick(Point2D point, StateRenderer2D source) {
        JPopupMenu popup = new JPopupMenu();
        popup.add(I18n.textf("Poll from %vehicle", getConsole().getMainSystem()))
                .addActionListener(this::pollFromVehicle);        
        popup.add(I18n.text("Poll from Web")).addActionListener(this::pollWeb);
        popup.add(I18n.text("Clear local data")).addActionListener(this::clearLocalData);
        popup.add(I18n.text("Upload local data")).addActionListener(this::uploadLocalData);
        popup.addSeparator();
        popup.add(I18n.text("Settings")).addActionListener(this::showSettings);
        
        popup.show(source, (int) point.getX(), (int) point.getY());
    }

    private void pollFromVehicle(ActionEvent evt) {
        pollDataFrom(getConsole().getMainSystem());
    }

    private void pollWeb(ActionEvent evt) {
        // TODO
    }

    private void clearLocalData(ActionEvent evt) {
        positions.clear();
        positionCache.clear();
        events.clear();
        dataStore.clearData();
        synchronized (mouseOver) {
            mouseOver = new ArrayList<>();
        }
        overlay.clear();
        NeptusLog.pub().info("Clear all local data.");
    }

    private void showSettings(ActionEvent evt) {
        PluginUtils.editPluginProperties(this, true);
        propertiesChanged();
    }
    
    private void uploadLocalData(ActionEvent evt) {
        if (ongoingPost != null && !ongoingPost.isDone()) {
            NeptusLog.pub().warn("Interrupting ongoing post for sending new data.");
            ongoingPost.cancel(true);
        }        
        ongoingPost = webAdapter.upload();
    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        g.setTransform(source.getIdentity());
        g.setColor(Color.red);
        
        for (PathElement p : positionCache.values()) {
            g.setTransform(source.getIdentity());
            p.paint(g, source, source.getRotation());
        }
        
        for (RemoteEvent evt : events) {
            
            Point2D pt = source.getScreenPosition(evt.location);
            switch (evt.event.getType()) {
                case ERROR:
                    g.setColor(Color.red.darker());
                    break;
                case INFO:
                    g.setColor(Color.blue.darker());
                    break;
            }
            g.fill(new Ellipse2D.Double(pt.getX()-4, pt.getY()-4, 8, 8));
        }
        
        ArrayList<RemoteEvent> copy = new ArrayList<>();
        synchronized (mouseOver) {
            copy.addAll(mouseOver);    
        }
        
        if (!copy.isEmpty()) {
           SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
           sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
           
           String html = "<html>";
           for (RemoteEvent evt : copy) {
               switch (evt.event.getType()) {
                   case ERROR:
                       html += "<font color=#990000>";
                       break;
                   case INFO:
                       html += "<font color=#009900>";
                       break;
               }
               html += "<b>"+sdf.format(evt.time)+" / "+evt.system+":</b> "+evt.event.getText()+"<br/>";
            }
            JLabel lbl = new JLabel(html);
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            g.setTransform(source.getIdentity());
            lbl.setBounds(0, 0, lbl.getPreferredSize().width, lbl.getPreferredSize().height);
            lbl.setBackground(new Color(255,255,255,180));
            g.translate(source.getWidth()-lbl.getPreferredSize().getWidth() - 3, source.getHeight()-lbl.getPreferredSize().getHeight() - 3);
            lbl.paint(g);
        }       
    }
}
