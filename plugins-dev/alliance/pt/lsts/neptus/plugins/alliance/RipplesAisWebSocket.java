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
 * Sep 6, 2019
 */
package pt.lsts.neptus.plugins.alliance;

import java.net.URI;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import eu.mivrenik.stomp.StompFrame;
import eu.mivrenik.stomp.client.StompClient;
import eu.mivrenik.stomp.client.listener.StompMessageListener;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.alliance.NmeaPlotter.MTShip;

/**
 * @author zp
 *
 */
public class RipplesAisWebSocket implements StompMessageListener {

    private StompClient client;
    private MTShipListener listener = null;
    private boolean connected = false;
    
    public RipplesAisWebSocket(MTShipListener listener) {
        this.listener = listener;
    }
    
    public void connect() {
        try {
            client = new StompClient(URI.create("wss://ripples.lsts.pt/ws"));
            client.connect();
            client.subscribe("/topic/ais", this);    
            this.connected = true;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error subscribing to websocket: "+e.getMessage());
        }        
    }
    
    public void disconnect() {
        try {
            client.close();
            this.connected = false;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error closing websocket: "+e.getMessage());
        }
    }
    
    @Override
    public void onMessage(StompFrame arg0) {
        JsonObject obj = Json.parse(arg0.getBody()).asObject();
        MTShip ship = RipplesAisParser.parse(obj);     
        listener.received(ship);
    }

    /**
     * @return the connected
     */
    public synchronized final boolean isConnected() {
        return connected;
    }

    /**
     * @param connected the connected to set
     */
    public synchronized final void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    public interface MTShipListener {
        public void received(MTShip ship);
    }
}
