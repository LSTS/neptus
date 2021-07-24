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
 * Author: ineeve
 * July 15, 2019
 */

package pt.lsts.ripples;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;


@PluginDescription(name = "Ripples Updater", icon = "pt/lsts/ripples/ripples_on.png")
public class RipplesUpdater extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = 8901788326550597186L;

    private final String ripplesPostUrl = GeneralPreferences.ripplesUrl + "/assets";
    private final String authKey = GeneralPreferences.ripplesApiKey;

    private JCheckBoxMenuItem menuItem;

    private ImageIcon onIcon, offIcon;

    private String checkMenuTxt = I18n.text("Advanced") + ">Ripples";

    @NeptusProperty
    private boolean connected = false;

    private Gson gson = new Gson();

    private LinkedHashMap<String, RipplesAssetState> assetStates = new LinkedHashMap<String, RipplesAssetState>();
    private LinkedHashMap<String, PlanControlState> planStates = new LinkedHashMap<String, PlanControlState>();

    public RipplesUpdater(ConsoleLayout console) {
        super(console);
        console.getSystems();
    }

    @Override
    public void propertiesChanged() {
        if (menuItem == null)
            return;

        if (connected && !menuItem.isSelected())
            menuItem.doClick();
        else if (!connected && menuItem.isSelected())
            menuItem.doClick();
    }

    @Override
    public void cleanSubPanel() {
        disconnect();

        if (menuItem != null) {
            removeCheckMenuItem(checkMenuTxt);
        }
    }

    @Override
    public void initSubPanel() {
        onIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_on.png", 16, 16);
        offIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_off.png", 16, 16);
        menuItem = addCheckMenuItem(checkMenuTxt + ">" + I18n.text("Connect"), offIcon, new CheckMenuChangeListener() {

            @Override
            public void menuChecked(ActionEvent e) {
                menuItem.setText(I18n.text("Connect"));
                menuItem.setIcon(offIcon);
                connect();
            }

            @Override
            public void menuUnchecked(ActionEvent e) {
                menuItem.setText(I18n.text("Disconnect"));
                menuItem.setIcon(onIcon);
                disconnect();
            }
        });

        if (connected)
            menuItem.doClick();
    }

    private void connect() {
        this.connected = true;
    }

    private void disconnect() {
        this.connected = false;
    }

    @Subscribe
    public void on(EstimatedState state) {
        if (!this.connected)
            return;
        LocationType location = parseIMCLocation(state);
        RipplesAssetState ripplesState = new RipplesAssetState((int) state.getTimestamp(), location.getLatitudeDegs(),
                location.getLongitudeDegs(), Math.toDegrees(state.getPsi()), -1);
        synchronized (assetStates) {
            assetStates.put(state.getSourceName(), ripplesState);
        }
    }

    @Subscribe
    public void on(Announce announce) {
        if (!this.connected)
            return;
        LocationType location = new LocationType(Math.toDegrees(announce.getLat()), Math.toDegrees(announce.getLon()));
        synchronized (assetStates) {
            // only update the asset state if it does not exist yet
            if (!assetStates.containsKey(announce.getSourceName())) {
                RipplesAssetState ripplesState = new RipplesAssetState((int) announce.getTimestamp(),
                        location.getLatitudeDegs(), location.getLongitudeDegs(), -1, -1);
                assetStates.put(announce.getSourceName(), ripplesState);
            }
        }
    }

    private LocationType parseIMCLocation(IMCMessage msg) {
        LocationType location = IMCUtils.parseLocation(msg);
        location.convertToAbsoluteLatLonDepth();
        return location;
    }

    @Subscribe
    public void on(PlanControlState pcs) {
        if (!this.connected)
            return;

        synchronized (planStates) {
            planStates.put(pcs.getSourceName(), pcs);
        }

    }

    private RipplesPlan pcsToRipplesPlan(PlanControlState pcs) {
        ImcSystem vehicleIMC = ImcSystemsHolder.getSystemWithName(pcs.getSourceName());
        if (vehicleIMC == null) {
            return new RipplesPlan();
        }
        if (!getConsole().getMission().getIndividualPlansList().containsKey(pcs.getPlanId())) {
            return new RipplesPlan();
        }
        PlanType planType = getConsole().getMission().getIndividualPlansList().get(pcs.getPlanId());
        if (planType == null) {
            return new RipplesPlan();
        }
        ArrayList<double[]> locs = new ArrayList<double[]>();
        for (ManeuverLocation m : PlanUtil.getPlanWaypoints(planType)) {
            LocationType loc = m.convertToAbsoluteLatLonDepth();
            locs.add(new double[] { loc.getLatitudeDegs(), loc.getLongitudeDegs() });
        }
        NeptusLog.pub().debug("Received PCS with " + locs.size() + " waypoints");
        return new RipplesPlan(planType.getId(), locs);
    }

    @Periodic(millisBetweenUpdates = 1000)
    public void sendUpdatesToRipples() {
        if (!this.connected)
            return;
        ArrayList<RipplesAsset> payload = new ArrayList<>();
        assetStates.forEach((sysName, assetState) -> {
            PlanControlState pcs = planStates.get(sysName);
            RipplesPlan plan = new RipplesPlan();
            if (pcs != null) {
                plan = pcsToRipplesPlan(pcs);
            }
            ImcSystem imcSystem = ImcSystemsHolder.lookupSystemByName(sysName);
            int imcId = (imcSystem != null && imcSystem.getId() != null) ? imcSystem.getId().intValue() : -1;
            payload.add(new RipplesAsset(sysName, imcId, assetState, plan));
            NeptusLog.pub().debug("Asset state: " + assetState);
        });

        String assetsAsJson = gson.toJson(payload);
        try {
            NeptusLog.pub().info("Sending update for " + payload.size() + " assets");
            sendPost(assetsAsJson);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (assetStates) {
            assetStates.clear();
        }
    }

    private String sendPost(String data) throws IOException {
        if (this.connected) {
            URL url = new URL(this.ripplesPostUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            if (authKey != null && !authKey.isEmpty()) {
                con.setRequestProperty ("Authorization", authKey);
            }

            this.sendData(con, data);

            return this.read(con.getInputStream());
        }
        return "";
    }

    private void sendData(HttpURLConnection con, String data) throws IOException {
        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
        }
        catch (IOException exception) {
            throw exception;
        }
        finally {
            this.closeQuietly(wr);
        }
    }

    private String read(InputStream is) throws IOException {
        BufferedReader in = null;
        String inputLine;
        StringBuilder body;
        try {
            in = new BufferedReader(new InputStreamReader(is));

            body = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                body.append(inputLine);
            }
            in.close();

            return body.toString();
        }
        catch (IOException ioe) {
            throw ioe;
        }
        finally {
            this.closeQuietly(in);
        }
    }

    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        }
        catch (IOException ex) {

        }
    }

}