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
 * May 8, 2018
 */
package pt.lsts.neptus.soi.risk;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.aismanager.ShipAisSnapshot;
import pt.lsts.aismanager.api.AisContactManager;
import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.endurance.Asset;
import pt.lsts.neptus.endurance.AssetState;
import pt.lsts.neptus.endurance.AssetsManager;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.util.WGS84Utilities;

/**
 * @author zp
 *
 */
@PluginDescription(name = "SOI Risk Analysis", icon = "pt/lsts/neptus/soi/icons/soi_risk.png")
@Popup(pos = POSITION.CENTER, width = 600, height = 600, accelerator = 'R')
public class SoiRiskAnalysis extends ConsolePanel {

    @NeptusProperty(name = "Minimum distance allowed between AUVs and Ships (meters)")
    int collisionDistance = 100;
    
    @NeptusProperty(name = "Font Size")
    int fontSize = 18;
    
    private static final long serialVersionUID = -3929616332138737684L;
    private ConcurrentHashMap<String, VehicleRiskAnalysis> state = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, VehicleRiskPanel> panels = new ConcurrentHashMap<>();
    
    public SoiRiskAnalysis(ConsoleLayout console) {
        super(console);
        initialize();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                    SwingUtilities.getWindowAncestor(SoiRiskAnalysis.this).setBounds(0,0, (int)screen.getWidth(), (int)screen.getHeight());
                }
            }
        });
    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Override
    public void initSubPanel() {

    }

    private void initialize() {
        removeAll();
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JPanel top = new JPanel(new GridLayout(1, 7));
        Font headerFont = new Font("Arial", Font.BOLD, 18);
        
        top.add(createLabel("Vehicle", headerFont));
        top.add(createLabel("Last Comm", headerFont));
        top.add(createLabel("Next Comm", headerFont));
        top.add(createLabel("Fuel", headerFont));
        top.add(createLabel("Distance", headerFont));
        top.add(createLabel("Collisions", headerFont));
        top.add(createLabel("Errors", headerFont));
        top.setMaximumSize(new Dimension(4000,60));
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray));
        add(top);
    }
    
    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        panels.forEachValue(1, p -> p.setTextSize(fontSize));
        repaint();
    }

    private JLabel createLabel(String text, Font font) {
        JLabel lbl = new JLabel(text) {
            private static final long serialVersionUID = 1L;

            @Override
            public void paint(Graphics g) {
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                super.paint(g);
            }
        };
        lbl.setHorizontalAlignment(JLabel.CENTER);
        lbl.setVerticalAlignment(JLabel.BOTTOM);
        lbl.setFont(font);
        return lbl;
    }
    
    @Subscribe
    public void on(TextMessage msg) {
        if (msg.getText().startsWith("ERROR:")) {
            NeptusLog.pub().error("Received error from "+msg.getSourceName());
            if (state.containsKey(msg.getSourceName()))
                state.get(msg.getSourceName()).errors.add(msg.getText());
        }
            
    }
    
    @Periodic(millisBetweenUpdates = 10_000)
    void updateCollisions() {
        long start = System.currentTimeMillis();

        // (vehicle, ship) -> (distance, timestamp)
        final ConcurrentHashMap<Pair<String, String>, Pair<Double, Date>> collisions = new ConcurrentHashMap<>();

        for (long timeOffset = 0; timeOffset < 3_600 * 3_000; timeOffset += 1_000 * collisionDistance/4) {
            final long time = timeOffset;
            HashMap<String, ShipAisSnapshot> ships = AisContactManager.getInstance().getFutureSnapshots(time);

            AssetsManager.getInstance().getAssets().parallelStream().forEach(asset -> {
                Date t = new Date(System.currentTimeMillis() + time);
                AssetState aState = asset.stateAt(t);
                if (aState == null)
                    return;

                ships.values().forEach(ship -> {
                    double distance = WGS84Utilities.distance(aState.getLatitude(), aState.getLongitude(),
                            ship.getLatDegs(), ship.getLonDegs());
                    if (distance < collisionDistance)
                        collisions.putIfAbsent(new Pair<>(asset.getAssetName(), ship.getLabel()), new Pair<>(distance, t));
                });
            });
        }

        state.forEachValue(1, s -> {
            s.collisions.clear();
        });
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        collisions.forEach((systems, info) -> {
            String vehicle = systems.first();
            String ship = systems.second();
            Date when = info.second();
            
            
            
            double distance = info.first();
            
            VehicleRiskAnalysis analysis = state.get(vehicle);
            if (analysis != null) {
                analysis.collisions.put(when, ship+" within "+(int)distance+"m @ "+sdf.format(when)+" UTC");
            }
        });
                
        long diff = System.currentTimeMillis() - start;

        NeptusLog.pub()
                .info("RiskAnalysis detected " + collisions.size() + " collisions in " + diff + " milliseconds.");
    }

    @Periodic(millisBetweenUpdates = 1_000)
    void update() {
        boolean doLayout = false;
        List<Asset> assets = AssetsManager.getInstance().getAssets();
        NeptusLog.pub().debug("Processing "+assets.size()+" assets for risk analysis.");
        for (Asset asset : assets) {
            String name = asset.getAssetName();
            VehicleRiskAnalysis risk = state.getOrDefault(name, new VehicleRiskAnalysis());
            AssetState next = asset.futureState();
            AssetState current = asset.stateAt(new Date());

            ImcSystem system = ImcSystemsHolder.lookupSystemByName(name);
            
            if (system != null && system.getType() == SystemTypeEnum.VEHICLE) {
                if (system.getLocationTimeMillis() < 0 )
                    continue;
                risk.lastCommunication = new Date(system.getLocationTimeMillis()); 
            }
            else {
                NeptusLog.pub().debug("Ignoring asset state for "+name);
                continue;
            }
            
            
           /* 
            if (risk.lastCommunication != null && System.currentTimeMillis() - risk.lastCommunication.getTime() > 24 * 3600_000) {
                // remove this panel
                if (panels.containsKey(name)) {
                    remove(panels.get(name));
                    panels.remove(name);
                    doLayout();
                }
                return;
            }*/
            
            if (next != null)
                risk.nextCommunication = next.getTimestamp();

            if (current != null) {
                risk.location = new LocationType(current.getLatitude(), current.getLongitude());
                risk.fuelLevel = current.getFuel();
            }
            
            state.put(name, risk);
            
            if (!panels.containsKey(name)) {
                VehicleType vt = VehiclesHolder.getVehicleById(name);
                String nickname = name;
                if (vt != null)
                    nickname = vt.getNickname().toUpperCase();
                
                VehicleRiskPanel panel = new VehicleRiskPanel(nickname);
                doLayout = true;
                panel.setTextSize(fontSize);
                add(panel);
                panels.put(name, panel);                
            }
            else {
                panels.get(name).setRiskAnalysis(risk);
            }
        }
        if (doLayout)
            doLayout();
        
        repaint();
    }
}
