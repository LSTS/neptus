/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 9, 2014
 */
package pt.lsts.ripples;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import com.eclipsesource.json.JsonObject;
import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.endurance.Asset;
import pt.lsts.neptus.endurance.AssetState;
import pt.lsts.neptus.endurance.EnduranceWebApi;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.util.WGS84Utilities;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ripples Uploader", icon = "pt/lsts/ripples/ripples_on.png")
public class RipplesUpload extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = -8036937519999303108L;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ");
    static {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private JCheckBoxMenuItem menuItem;
    private ImageIcon onIcon, offIcon;
    private String checkMenuTxt = I18n.text("Advanced") + ">Ripples";

    private LinkedHashMap<String, EstimatedState> toSend = new LinkedHashMap<String, EstimatedState>();

    @NeptusProperty(name = "Synch external systems")
    private boolean synchExternalSystems = false;

    @NeptusProperty(name = "Poll positions from server", userLevel = LEVEL.REGULAR)
    private boolean pollPositions = false;

    @NeptusProperty(name = "Post positions from server", userLevel = LEVEL.REGULAR)
    private boolean postPositions = false;

    @NeptusProperty(name = "POST URL", userLevel = LEVEL.REGULAR)
    private String postURL = "https://ripples.lsts.pt/positions";

    @NeptusProperty(name = "WebSocket URL", userLevel = LEVEL.REGULAR)
    private String wsURL = "wss://ripples.lsts.pt/ws";

    @NeptusProperty(name = "Enable Synch", userLevel = LEVEL.REGULAR)
    private boolean synch = false;

    /**
     * @param console
     */
    public RipplesUpload(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
        onIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_on.png", 16, 16);
        offIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_off.png", 16, 16);
        menuItem = addCheckMenuItem(checkMenuTxt + ">" + I18n.text("Start synch"), offIcon,
                new CheckMenuChangeListener() {

                    @Override
                    public void menuUnchecked(ActionEvent e) {
                        menuItem.setText(I18n.text("Start synch"));
                        menuItem.setIcon(offIcon);
                        stopSynch();
                    }

                    @Override
                    public void menuChecked(ActionEvent e) {
                        menuItem.setText(I18n.text("Stop synch"));
                        menuItem.setIcon(onIcon);
                        startSynch();
                    }
                });

        if (synch)
            menuItem.doClick();
    }

    @Override
    public void cleanSubPanel() {
        stopSynch();

        if (menuItem != null) {
            removeCheckMenuItem(checkMenuTxt);
        }
    }

    @Override
    public void propertiesChanged() {
        if (menuItem == null)
            return;

        if (synch && !menuItem.isSelected())
            menuItem.doClick();
        else if (!synch && menuItem.isSelected())
            menuItem.doClick();
    }

    @Subscribe
    public void on(EstimatedState state) {
        if (!synch)
            return;
        synchronized (toSend) {
            toSend.put(state.getSourceName(), state);
        }
    }

    
    private Asset asAsset(EstimatedState state) {
        Asset asset = new Asset(state.getSourceName());
        double lld[] = WGS84Utilities.toLatLonDepth(state);
        asset.setState(AssetState.builder().withHeading(Math.toDegrees(state.getPsi())).withLatitude(lld[0])
                .withLongitude(lld[1]).withTimestamp(state.getDate()).build());

        ImcSystem system = ImcSystemsHolder.getSystemWithName(state.getSourceName());
        if (system != null && system.getActivePlan() != null) {
            asset.setPlan(Plan.parse(system.getActivePlan().asIMCPlan()));
        }
        
        return asset;    
    }
    
    private void sendState(EstimatedState state) {
        Asset asset = new Asset(state.getSourceName());
        double lld[] = WGS84Utilities.toLatLonDepth(state);
        asset.setState(AssetState.builder().withHeading(Math.toDegrees(state.getPsi())).withLatitude(lld[0])
                .withLongitude(lld[1]).withTimestamp(state.getDate()).build());

        ImcSystem system = ImcSystemsHolder.getSystemWithName(state.getSourceName());
        if (system != null && system.getActivePlan() != null) {
            asset.setPlan(Plan.parse(system.getActivePlan().asIMCPlan()));
        }
        try {
            EnduranceWebApi.setAsset(asset).get();    
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Periodic(millisBetweenUpdates = 2500)
    public void sendPositions() {
        if (!synch)
            return;

        List<Asset> assets;
        synchronized (toSend) {
            assets = toSend.values().stream().map(s -> asAsset(s)).collect(Collectors.toList());
            toSend.clear();
        }
        try {
            EnduranceWebApi.setAssets(assets).get(2, TimeUnit.SECONDS);    
        }
        catch (Exception e) {
            NeptusLog.pub().error("Could not send assets to Ripples: "+e.getMessage(), e);            
        }
        
    }

    private void startSynch() {
        NeptusLog.pub().info("Started synch'ing with ripples.");
        this.synch = true;
        synchronized (toSend) {
            toSend.clear();
        }
    }

    private void stopSynch() {
        NeptusLog.pub().info("Stopped synch'ing with ripples.");
        this.synch = false;
    }

    public void pollActiveSystems() {
        if (!synch || !pollPositions)
            return;
    }

    class Position {
        int imcId;
        Date timestamp;
        double lat;
        double lon;
        String name;

        public Position(EstimatedState state) {
            this.imcId = state.getSrc();
            this.name = state.getSourceName();
            LocationType loc = IMCUtils.parseLocationAlt(state).convertToAbsoluteLatLonDepth();
            this.lat = loc.getLatitudeDegs();
            this.lon = loc.getLongitudeDegs();
            this.timestamp = state.getDate();
        }

        public Position(Announce state) {
            this.imcId = state.getSrc();
            this.name = state.getSourceName();
            this.lat = Math.toDegrees(state.getLat());
            this.lon = Math.toDegrees(state.getLon());
            this.timestamp = state.getDate();
        }

        public JsonObject asJson() {
            JsonObject obj = new JsonObject();
            obj.set("id", imcId);
            obj.set("imcId", imcId);
            obj.set("timestamp", sdf.format(timestamp));
            obj.set("lat", lat);
            obj.set("lon", lon);
            obj.set("name", name);

            return obj;
        }

        @Override
        public String toString() {
            return asJson().toString();
        }
    }
}
