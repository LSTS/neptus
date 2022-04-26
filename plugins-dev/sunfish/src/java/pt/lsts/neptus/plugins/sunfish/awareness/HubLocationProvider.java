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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.LinkedHashMap;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;

import pt.lsts.imc.Announce;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger.HubSystemMsg;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.conf.GeneralPreferences;


/**
 * @author zp
 *
 */
public class HubLocationProvider implements ILocationProvider {

    SituationAwareness parent;
    private String systemsUrl = GeneralPreferences.ripplesUrl + "/api/v1/systems/active";
    private String authKey = GeneralPreferences.ripplesApiKey;

    @Override
    public void onInit(SituationAwareness instance) {
        this.parent = instance;
        instance.getConsole().getImcMsgManager().registerBusListener(this);   
    }
    
    LinkedHashMap<Integer, AssetPosition> positionsToSend = new LinkedHashMap<Integer, AssetPosition>();  
    
    @Subscribe
    public void on(Announce announce) {
        if (announce.getLat() != 0 || announce.getLon() != 0) {
            AssetPosition pos = new AssetPosition(announce.getSysName(), Math.toDegrees(announce.getLat()), Math.toDegrees(announce.getLon()));
            positionsToSend.put(announce.getSrc(), pos);            
        }
    }
    
    
       
//    @Periodic(millisBetweenUpdates=3000*60)
//    public void sendToHub() {
//        if (!enabled)
//            return;
//        NeptusLog.pub().info("Uploading device updates to Hub...");
//        LinkedHashMap<Integer, AssetPosition> toSend = new LinkedHashMap<Integer, AssetPosition>();
//        LocationType myLoc = MyState.getLocation();
//        AssetPosition myPos = new AssetPosition(StringUtils.toImcName(GeneralPreferences.imcCcuName), myLoc.getLatitudeDegs(), myLoc.getLongitudeDegs());
//        toSend.put(ImcMsgManager.getManager().getLocalId().intValue(), myPos);
//        toSend.putAll(positionsToSend);
//        positionsToSend.clear();
//        DeviceUpdate upd = new DeviceUpdate(); 
//        //ExtendedDeviceUpdate upd = new ExtendedDeviceUpdate();
//        upd.source = ImcMsgManager.getManager().getLocalId().intValue();
//        upd.destination = 65535;
//        for (Entry<Integer, AssetPosition> pos : toSend.entrySet()) {
//            Position p = new Position();
//            p.id = pos.getKey();
//            p.latRads = pos.getValue().getLoc().getLatitudeRads();
//            p.lonRads = pos.getValue().getLoc().getLongitudeRads();
//            p.posType = Position.fromImcId(p.id);
//            p.timestamp = pos.getValue().getTimestamp() / 1000.0;
//            upd.getPositions().put(pos.getKey(), p);
//        }
//        
//        for (Position p : upd.getPositions().values()) {
//            NeptusLog.pub().info("Uploading position for "+p.id+": "+Math.toDegrees(p.latRads)+"/"+Math.toDegrees(p.lonRads)+"/"+new Date((long)(1000*p.timestamp)));
//        }
//        
//        try {
//            HttpPost postMethod = new HttpPost(iridiumUrl);
//            postMethod.setHeader("Content-type", "application/hub");
//            String data = new String(Hex.encodeHex(upd.serialize()));
//            NeptusLog.pub().info("Sending '"+data+"'");
//            StringEntity ent = new StringEntity(data);
//            postMethod.setEntity(ent);
//            @SuppressWarnings("resource")
//            HttpClient client = new DefaultHttpClient();
//            HttpResponse response = client.execute(postMethod);
//            NeptusLog.pub().info("Sent "+upd.getPositions().size()+" device updates to Hub: "+response.getStatusLine().toString());
//            postMethod.abort();            
//        }
//        catch (Exception e) {
//            NeptusLog.pub().error("Error sending updates to hub", e);
//            parent.postNotification(Notification.error("Situation Awareness", e.getClass().getSimpleName()+" while trying to send device updates to HUB.").requireHumanAction(false));            
//        }        
//    }
     
    @Periodic(millisBetweenUpdates=1000*60)
    public void pollActiveSystems() {
        if (!enabled)
            return;
        
        try {
            Gson gson = new Gson();
            URL url = new URL(systemsUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (authKey != null && !authKey.isEmpty()) {
                conn.setRequestProperty ("Authorization", authKey);
            }

            HubSystemMsg[] msgs = gson.fromJson(new InputStreamReader(conn.getInputStream()), HubSystemMsg[].class);
            NeptusLog.pub().info(" through HTTP: " + systemsUrl);
    
            for (HubSystemMsg m : msgs) {
                AssetPosition pos = (m.coordinates.length > 2 && m.coordinates[2] != null && !Double.isNaN(m.coordinates[2])) ? 
                        new AssetPosition(m.name, m.coordinates[0], m.coordinates[1])
                        : new AssetPosition(m.name, m.coordinates[0], m.coordinates[1], m.coordinates[2]);
                pos.setType(IMCUtils.getSystemType(m.imcid));
                pos.setTimestamp(HubIridiumMessenger.stringToDate(m.updated_at).getTime());
                pos.setSource(getName());
                if (pos.getAssetName().equals("hermes"))
                    pos.setType("ASV");
                
                if (m.pos_error_class != null && !m.pos_error_class.isEmpty()) {                    
                    pos.putExtra("Loc. Class", m.pos_error_class);
                }
                parent.addAssetPosition(pos);
                NeptusLog.pub().info("Received HUB position update for "+m.name + ": "+pos.getLoc()+" @ "+new Date(pos.getTimestamp()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);
            parent.postNotification(Notification.error("Situation Awareness", e.getClass().getSimpleName()+" while polling device updates from HUB.").requireHumanAction(false));    
        }
        
    }
    @Override
    public String getName() {
        return "HUB (Active Systems API)";
    }

    @Override
    public void onCleanup() {        
        parent.getConsole().getImcMsgManager().unregisterBusListener(this);
    }

    private boolean enabled = false;
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public static void main(String args[]) {
        SituationAwareness awareness = new SituationAwareness();
        awareness.updateMethods = "HUB (Active Systems API)";
        HubLocationProvider provider = new HubLocationProvider();
        provider.setEnabled(true);
        provider.onInit(awareness);
        provider.pollActiveSystems();
        
    }
}
