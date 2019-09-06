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
import java.net.URI;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import eu.mivrenik.stomp.StompFrame;
import eu.mivrenik.stomp.client.StompClient;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger.HubSystemMsg;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.endurance.Asset;
import pt.lsts.neptus.endurance.AssetState;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ripples Uploader", icon = "pt/lsts/ripples/ripples_on.png")
public class RipplesUpload extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = -8036937519999303108L;

    private JCheckBoxMenuItem menuItem;
    private ImageIcon onIcon, offIcon;
    private String checkMenuTxt = I18n.text("Tools") + ">Ripples Synch.";

    private StompClient client;
    
    @NeptusProperty(name = "Synch external systems")
    private boolean synchExternalSystems = false;

    @NeptusProperty
    private boolean synch = false;
    
    @NeptusProperty(name = "Web Socket URL")
    private String ripples_url = "ws://localhost:9090/ws";
    
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

    @Override
    public void propertiesChanged() {
        if (menuItem == null)
            return;

        if (synch && !menuItem.isSelected())
            menuItem.doClick();
        else if (!synch && menuItem.isSelected())
            menuItem.doClick();
    }

    @Periodic(millisBetweenUpdates = 1000)
    public void sendPositions() {
        if (!synch)
            return;

        System.out.println("Send positions");
        for (ImcSystem system : ImcSystemsHolder.lookupAllActiveSystems()) {
            LocationType loc = system.getLocation().convertToAbsoluteLatLonDepth();
            AssetState state = AssetState.builder()
                    .withLatitude(loc.getLatitudeDegs())
                    .withLongitude(loc.getLongitudeDegs())
                    .withHeading(system.getYawDegrees())
                    .withTimestamp(new Date(system.getLocationTimeMillis()))
                    .build();
            Asset asset = new Asset(system.getName());
            asset.setState(state);           
            
            if (client != null)
                client.send("/topic/position", asset.toString());
            System.out.println("Sent asset.");
        }        
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
    

    private void startSynch() {
        NeptusLog.pub().info("Started synch'ing with ripples.");
        try {
            client = new StompClient(URI.create(ripples_url));
            client.connectBlocking();
            client.subscribe("/topic/ais", RipplesUpload.this::onPosition);     
            NeptusLog.pub().info("Connected to web socket in "+ripples_url);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error connecting to web socket: "+e.getMessage(), e);  
            e.printStackTrace();
        }
        
        synch = true;
    }

    private void stopSynch() {
        NeptusLog.pub().info("Stopped synch'ing with ripples.");
        try {
            if (client != null)
                client.close();
            client = null;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error disconnecting from web socket: "+e.getMessage(), e);
        }
            
        synch = false;
    }
    
    
    public void onPosition(StompFrame arg0) {
        JsonObject obj = Json.parse(arg0.getBody()).asObject();
        System.out.println("Received: "+obj);
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
