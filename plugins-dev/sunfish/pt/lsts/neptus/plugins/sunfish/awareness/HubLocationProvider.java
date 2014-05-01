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

import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import pt.lsts.imc.Announce;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.iridium.DeviceUpdate;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger.HubSystemMsg;
import pt.lsts.neptus.comm.iridium.Position;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;


/**
 * @author zp
 *
 */
@SuppressWarnings("deprecation")
public class HubLocationProvider implements ILocationProvider {

    SituationAwareness parent;
    private String systemsUrl = "http://hub.lsts.pt/api/v1/systems/active";
    private String iridiumUrl = "http://hub.lsts.pt/api/v1/iridium";    

    @Override
    public void onInit(SituationAwareness instance) {
        this.parent = instance;
        ImcMsgManager.registerBusListener(this);   
    }
    
    LinkedHashMap<Integer, AssetPosition> positionsToSend = new LinkedHashMap<Integer, AssetPosition>();  
    
    @Subscribe
    public void on(Announce announce) {
        if (announce.getLat() != 0 || announce.getLon() != 0) {
            AssetPosition pos = new AssetPosition(announce.getSysName(), Math.toDegrees(announce.getLat()), Math.toDegrees(announce.getLon()));
            positionsToSend.put(announce.getSrc(), pos);            
        }
    }
    
    
       
    @Periodic(millisBetweenUpdates=3000*60)
    public void sendToHub() {
        if (!enabled)
            return;
        NeptusLog.pub().info("Uploading device updates to Hub...");
        LinkedHashMap<Integer, AssetPosition> toSend = new LinkedHashMap<Integer, AssetPosition>();
        LocationType myLoc = MyState.getLocation();
        AssetPosition myPos = new AssetPosition(StringUtils.toImcName(GeneralPreferences.imcCcuName), myLoc.getLatitudeDegs(), myLoc.getLongitudeDegs());
        toSend.put(ImcMsgManager.getManager().getLocalId().intValue(), myPos);
        toSend.putAll(positionsToSend);
        positionsToSend.clear();
        DeviceUpdate upd = new DeviceUpdate(); 
        //ExtendedDeviceUpdate upd = new ExtendedDeviceUpdate();
        upd.source = ImcMsgManager.getManager().getLocalId().intValue();
        upd.destination = 65535;
        for (Entry<Integer, AssetPosition> pos : toSend.entrySet()) {
            Position p = new Position();
            p.id = pos.getKey();
            p.latitude = pos.getValue().getLoc().getLatitudeDegs();
            p.longitude = pos.getValue().getLoc().getLongitudeDegs();
            p.posType = Position.fromImcId(p.id);
            p.timestamp = pos.getValue().getTimestamp() / 1000.0;
            upd.getPositions().put(pos.getKey(), p);
        }
        try {
            HttpPost postMethod = new HttpPost(iridiumUrl);
            postMethod.setHeader("Content-type", "application/hub");
            String data = new String(Hex.encodeHex(upd.serialize()));
            NeptusLog.pub().info("Sending '"+data+"'");
            StringEntity ent = new StringEntity(data);
            postMethod.setEntity(ent);
            @SuppressWarnings("resource")
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(postMethod);
            NeptusLog.pub().info("Sent "+upd.getPositions().size()+" device updates to Hub: "+response.getStatusLine().toString());
            postMethod.abort();            
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error sending updates to hub", e);
        }        
    }
     
    @Periodic(millisBetweenUpdates=1000*60)
    public void pollActiveSystems() throws Exception {
        if (!enabled)
            return;
        Gson gson = new Gson();
        URL url = new URL(systemsUrl);

        HubSystemMsg[] msgs = gson.fromJson(new InputStreamReader(url.openStream()), HubSystemMsg[].class);
        NeptusLog.pub().info(" through HTTP: " + systemsUrl);

        for (HubSystemMsg m : msgs) {
            AssetPosition pos = new AssetPosition(m.name, m.coordinates[0],
                    m.coordinates[1]);
            pos.setType(IMCUtils.getSystemType(m.imcid));
            pos.setTimestamp(HubIridiumMessenger.stringToDate(m.updated_at).getTime());
            pos.setSource(getName());
            parent.addAssetPosition(pos);
            NeptusLog.pub().info(m.name + " " + m.imcid);
        }

    }
    @Override
    public String getName() {
        return "HUB (Active Systems API)";
    }

    @Override
    public void onCleanup() {        
        ImcMsgManager.unregisterBusListener(this);
    }

    private boolean enabled = false;
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
