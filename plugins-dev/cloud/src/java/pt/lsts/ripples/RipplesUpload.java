/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthResultHandler;
import com.firebase.client.FirebaseError;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger.HubSystemMsg;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystem.ExternalTypeEnum;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ripples Uploader", icon = "pt/lsts/ripples/ripples_on.png", active = false)
@Deprecated
public class RipplesUpload extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = -8036937519999303108L;
    
    private final String firebasePath = "https://neptus.firebaseio.com/";
    private final String ripplesActiveSysUrl = GeneralPreferences.ripplesUrl + "/api/v1/systems/active";

    private JCheckBoxMenuItem menuItem;
    private ImageIcon onIcon, offIcon;
    private String checkMenuTxt = I18n.text("Advanced") + ">Ripples";

    private Firebase firebase = null;
    @SuppressWarnings("unused")
    private AuthData authData = null;
    private LinkedHashMap<String, SystemPositionAndAttitude> toSend = new LinkedHashMap<String, SystemPositionAndAttitude>();
    private LinkedHashMap<String, PlanControlState> planStates = new LinkedHashMap<String, PlanControlState>();

    @NeptusProperty(name = "Synch external systems")
    private boolean synchExternalSystems = false;

    @NeptusProperty(name = "Poll also systems from server",  userLevel = LEVEL.REGULAR)
    private boolean pollSystems = false;

    @NeptusProperty
    private boolean synch = false;

    /**
     * @param console
     */
    public RipplesUpload(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        if (synch)
            stopSynch();
        
        if (menuItem != null) {
            removeCheckMenuItem(checkMenuTxt);
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
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
        
        SystemPositionAndAttitude pose = new SystemPositionAndAttitude(IMCUtils.parseLocationAlt(state)
                .convertToAbsoluteLatLonDepth(), state.getPhi(), state.getTheta(), state.getPsi());
        pose.setTime(state.getTimestampMillis());
        pose.setAltitude(state.getAlt());
        pose.setDepth(state.getDepth());
        pose.setVxyz(state.getVx(), state.getVy(), state.getVz());
        
        synchronized (toSend) {
            toSend.put(state.getSourceName(), pose);
        }
    }

    @Subscribe
    public void on(Announce announce) {
        if (!synch)
            return;

        LocationType loc = new LocationType(Math.toDegrees(announce.getLat()), Math.toDegrees(announce.getLon()));
        loc.setHeight(announce.getHeight());
        SystemPositionAndAttitude pose = new SystemPositionAndAttitude(loc, 0, 0, 0);
        pose.setTime(announce.getTimestampMillis());
        pose.setAltitude(-1);
        pose.setDepth(-1);
        pose.setVxyz(Double.NaN, Double.NaN, Double.NaN);

        synchronized (toSend) {
            // avoid sending announce when there are estimated states to be sent
            if (toSend.containsKey(announce.getSysName()))
                return;
            toSend.put(announce.getSysName(), pose);
        }
    }

    @Subscribe
    public void on(PlanControlState pcs) {
        if (!synch)
            return;

        synchronized (planStates) {
            planStates.put(pcs.getSourceName(), pcs);
        }
    }

    @Periodic(millisBetweenUpdates = 10000)
    public void sendPlans() {
        if (!synch)
            return;

        LinkedHashMap<String, PlanControlState> copy = new LinkedHashMap<String, PlanControlState>();
        synchronized (planStates) {
            copy.putAll(planStates);
            planStates.clear();
        }
        for (PlanControlState pcs : copy.values()) {
            Firebase planRef = firebase.child("assets/" + pcs.getSourceName() + "/plan").getRef();

            // no plan being executed
            if (pcs.getState() == STATE.BLOCKED || pcs.getState() == STATE.READY) {
                planRef.setValue(null);
            }
            else if (!pcs.getPlanId().isEmpty()) {
                planRef.child("id").setValue(pcs.getPlanId());
                planRef.child("progress").setValue(String.format(Locale.US, "%.1f", pcs.getPlanProgress()));

                if (getConsole().getMission().getIndividualPlansList().containsKey(pcs.getPlanId())) {
                    PlanType pt = getConsole().getMission().getIndividualPlansList().get(pcs.getPlanId());
                    Vector<double[]> locs = new Vector<double[]>();
                    for (ManeuverLocation m : PlanUtil.getPlanWaypoints(pt)) {
                        LocationType loc = m.convertToAbsoluteLatLonDepth();
                        locs.add(new double[] { loc.getLatitudeDegs(), loc.getLongitudeDegs() });
                    }
                    planRef.child("path").setValue(locs);
                }
            }
        }
    }

    @Periodic(millisBetweenUpdates = 500)
    public void sendPositions() {
        if (!synch)
            return;
        LinkedHashMap<String, SystemPositionAndAttitude> copy = new LinkedHashMap<String, SystemPositionAndAttitude>();
        synchronized (toSend) {
            copy.putAll(toSend);
            toSend.clear();
        }
        
        SystemPositionAndAttitude mine = new SystemPositionAndAttitude(MyState.getLocation(), 0, 0, MyState.getHeadingInRadians());
        mine.setTime(System.currentTimeMillis());
        copy.put(GeneralPreferences.imcCcuName, mine);
        
        LinkedHashMap<String, String> extSysType = new LinkedHashMap<String, String>();
        
        if (synchExternalSystems) {
            ExternalSystem[] extSys = ExternalSystemsHolder.lookupAllActiveSystems();
            for (ExternalSystem es : extSys) {
                String name = es.getName();
                
                if ("ship".equalsIgnoreCase(name))
                    continue;
                
                if(copy.containsKey(name))
                    continue;
                SystemPositionAndAttitude sysPos = new SystemPositionAndAttitude(es.getLocation(), 0, 0,
                        Math.toRadians(es.getYawDegrees()));
                sysPos.setTime(es.getLocationTimeMillis());
                copy.put(name, sysPos);
                String type = es.getTypeExternal().toString().toLowerCase();
                if ("vehicle".equalsIgnoreCase(type))
                    type = es.getTypeVehicle().toString().toLowerCase();
                extSysType.put(name, type);
            }
        }
        
        for (Entry<String, SystemPositionAndAttitude> state : copy.entrySet()) {
            Map<String, Object> assetState = new LinkedHashMap<String, Object>();
            Map<String, Object> tmp = new LinkedHashMap<String, Object>();
            tmp.put("latitude", state.getValue().getPosition().getLatitudeDegs());
            tmp.put("longitude", state.getValue().getPosition().getLongitudeDegs());
            tmp.put("height", state.getValue().getPosition().getHeight());
            tmp.put("heading", Math.toDegrees(state.getValue().getYaw()));
            tmp.put("altitude", state.getValue().getAltitude());
            if (!Double.isNaN(state.getValue().getVx())) {
                double speed = Math.sqrt(state.getValue().getVx() * state.getValue().getVx()
                        + state.getValue().getVy() * state.getValue().getVy()
                        + state.getValue().getVz() * state.getValue().getVz());
                tmp.put("speed", speed);
            }
            tmp.put("depth", state.getValue().getDepth());
            assetState.put("position", tmp);
            assetState.put("updated_at", state.getValue().getTime());
            if (state.getKey().equals(GeneralPreferences.imcCcuName)) {
                assetState.put("type", "CCU");
            }
            else {
                String typeSys = IMCUtils.getSystemType(IMCDefinition.getInstance().getResolver().resolve(state.getKey()));
                if ("Unknown".equalsIgnoreCase(typeSys) && extSysType.containsKey(state.getKey()))
                    typeSys = extSysType.get(state.getKey());
                assetState.put("type", typeSys);
            }
            
            synchronized (firebase) {
                if (firebase != null) {
                    firebase.child("assets/" + state.getKey()).getRef().updateChildren(assetState);
                }
            }
        }
    }

    @Override
    public void initSubPanel() {
        onIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_on.png", 16, 16);
        offIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_off.png", 16, 16);
        menuItem = addCheckMenuItem(checkMenuTxt + ">" + I18n.text("Start synch"), offIcon, new CheckMenuChangeListener() {

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

    private void startSynch() {
        NeptusLog.pub().info("Started synch'ing with ripples.");
        authData = null;
        firebase = new Firebase(firebasePath);
        firebase.authAnonymously(new AuthResultHandler() {
            @Override
            public void onAuthenticationError(FirebaseError error) {
                NeptusLog.pub().error(error.toException());
                RipplesUpload.this.firebase = null;
            }
            
            @Override
            public void onAuthenticated(AuthData authData) {
                RipplesUpload.this.authData = authData;
                NeptusLog.pub().info("Firebaseio sign in: " + authData);;
            }
        });
        if (firebase != null) {
            Firebase.goOnline();
            synch = true;
        }
    }

    private void stopSynch() {
        NeptusLog.pub().info("Stopped synch'ing with ripples.");
        if (firebase != null)
            firebase.unauth();
        firebase = null;
        authData = null;
        Firebase.goOffline();
        synch = false;
    }
    
    public void pollActiveSystems() {
        if (!synch || !pollSystems)
            return;
        
        try {
            Gson gson = new Gson();
            URL url = new URL(ripplesActiveSysUrl);
    
            HubSystemMsg[] msgs = gson.fromJson(new InputStreamReader(url.openStream()), HubSystemMsg[].class);
            NeptusLog.pub().info(" through HTTP: " + ripplesActiveSysUrl);
    
            for (HubSystemMsg m : msgs) {
                long id = m.imcid;
                if (id > ImcId16.MAX_VALUE) { // External system
                    fillExternalSystem(m);
                }
                else { // IMC system
                    ImcSystem imcSys = ImcSystemsHolder.lookupSystem((int) id);
                    if (imcSys == null)
                        fillExternalSystem(m);
                    else
                        fillIMCSystem(imcSys, m);
                }
                
                NeptusLog.pub().info(String.format("Received Ripples position update for '%s' :: %s @ %s", m.name, parsePos(m), m.updatedAt()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);
            post(Notification.error(getName(), e.getClass().getSimpleName()+" while polling device updates from Ripples.").requireHumanAction(false));    
        }
    }

    @Periodic(millisBetweenUpdates = 10000)
    public void pollActiveSystemsF() {
        if (!synch || !pollSystems)
            return;
        
        try {
            JsonParser parser = new JsonParser();
            URL url = new URL(firebasePath.trim() + (firebasePath.trim().endsWith("/") ? "" : "/") + ".json");
            
            JsonElement root = parser.parse(new JsonReader(new InputStreamReader(url.openConnection().getInputStream())));
            Set<Entry<String, JsonElement>> assets = root.getAsJsonObject().get("assets").getAsJsonObject().entrySet();
            
            for (Entry<String, JsonElement> asset : assets) {
                long updatedAt = asset.getValue().getAsJsonObject().get("updated_at").getAsLong();
                JsonElement position = asset.getValue().getAsJsonObject().get("position");
                if (position == null)
                    continue;
                
                double latDegs = position.getAsJsonObject().get("latitude").getAsDouble();
                double lonDegs = position.getAsJsonObject().get("longitude").getAsDouble();

                double height = position.getAsJsonObject().get("height").getAsDouble();

                ExternalSystem es = new ExternalSystem(asset.getKey());
                es = ExternalSystemsHolder.registerSystem(es);
                if (updatedAt > es.getLocationTimeMillis()) {
                    LocationType pos = new LocationType(latDegs, lonDegs);
                    if (height != 0)
                        pos.setHeight(height);
                    es.setLocation(pos, updatedAt);
                }
                
                JsonElement typeJson = asset.getValue().getAsJsonObject().get("type");
                if (typeJson != null) {
                    String type = typeJson.getAsString();
                    if (!type.isEmpty()) {
                        SystemTypeEnum sType = SystemUtils.getSystemTypeFrom(type);
                        VehicleTypeEnum vType = SystemUtils.getVehicleTypeFrom(type);
                        ExternalTypeEnum eType = SystemUtils.getExternalTypeFrom(type);
                        
                        es.setType(sType);
                        es.setTypeVehicle(vType);
                        es.setTypeExternal(eType);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);
            post(Notification.error(getName(), e.getClass().getSimpleName()+" while polling device updates from Ripples.").requireHumanAction(false));    
        }
    }

    private void fillIMCSystem(ImcSystem imcSys, HubSystemMsg m) {
        if (imcSys == null)
            return;

        LocationType pos = parsePos(m);
        if (pos == null)
            return;

        if (m.updatedAt().getTime() > imcSys.getLocationTimeMillis())
            imcSys.setLocation(pos, m.updatedAt().getTime());
    }

    private void fillExternalSystem(HubSystemMsg m) {
        LocationType pos = parsePos(m);
        if (pos == null)
            return;
        
        ExternalSystem es = new ExternalSystem(m.name);
        es = ExternalSystemsHolder.registerSystem(es);
        if (m.updatedAt().getTime() > es.getLocationTimeMillis())
            es.setLocation(pos, m.updatedAt().getTime());
    }

    /**
     * @param m
     * @return
     */
    private LocationType parsePos(HubSystemMsg m) {
        if (m.coordinates.length > 1 && m.coordinates[0] != null && Double.isFinite(m.coordinates[0]) 
                && m.coordinates[1] != null && Double.isFinite(m.coordinates[1])) {
            LocationType pos = new LocationType(m.coordinates[0], m.coordinates[1]); // degrees
            if (m.coordinates.length > 2 && m.coordinates[2] != null && Double.isFinite(m.coordinates[2]))
                pos.setHeight(m.coordinates[2]);
            
            return pos;
        }
        return null;
    }

}
