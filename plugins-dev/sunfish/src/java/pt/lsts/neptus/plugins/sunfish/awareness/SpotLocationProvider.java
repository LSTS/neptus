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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.io.File;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.update.Periodic;


/**
 * @author zp
 *
 */
public class SpotLocationProvider implements ILocationProvider {

    SituationAwareness parent;
    private String url = "https://api.findmespot.com/spot-main-web/consumer/rest-api/2.0/public/feed/0qQz420UTPODTjoHylgIOPa3RqqvOhkMK/message.xml";

    @Override
    public void onInit(SituationAwareness instance) {
        this.parent = instance;
    }

    @Periodic(millisBetweenUpdates=1000*60)
    public void updateSpots() throws Exception {
        if (!enabled)
            return;
        try {
            URL urlSpot = new URL(url);
            File tmp = File.createTempFile("neptus", "spots");
            FileUtils.copyURLToFile(urlSpot, tmp);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(tmp);

            NodeList messages = doc.getElementsByTagName("message");
            for (int i = 0; i < messages.getLength(); i++) {
                NodeList elems = messages.item(i).getChildNodes();
                String name = null;
                double lat = 0, lon = 0;
                long timestamp = System.currentTimeMillis();
                String battState = null, msgType = null;
                for (int j = 0; j < elems.getLength(); j++) {
                    Node nd = elems.item(j); 
                    switch (nd.getNodeName()) {
                        case "unixTime":
                            timestamp = Long.parseLong(nd.getTextContent()) * 1000;
                            break;
                        case "latitude":
                            lat = Double.parseDouble(nd.getTextContent());
                            break;
                        case "longitude":
                            lon = Double.parseDouble(nd.getTextContent());
                            break;
                        case "messengerName":
                            name = nd.getTextContent().toLowerCase();
                            break;
                        case "batteryState":
                            battState = nd.getTextContent();
                            break;
                        case "messageType":
                            msgType = nd.getTextContent();
                            break;
                        default:
                            break;
                    }
                }
                if (name != null) {
                    AssetPosition pos = new AssetPosition(name, lat, lon);
                    pos.setTimestamp(timestamp);
                    pos.setSource(getName());
                    pos.setType("SPOT Tag");
                    if (battState != null)
                        pos.putExtra("Battery", WordUtils.capitalize(battState.toLowerCase()));
                    if (msgType != null)
                        pos.putExtra("SPOT Mode", WordUtils.capitalize(msgType.toLowerCase()));
                    parent.addAssetPosition(pos);
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            parent.postNotification(Notification.error("Situation Awareness", e.getClass().getSimpleName()+" while polling SPOT positions from Web.").requireHumanAction(false));    
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.sunfish.awareness.ILocationProvider#getName()
     */
    @Override
    public String getName() {
        return "SPOT (Web API)";
    }

    private boolean enabled = false;
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onCleanup() {        

    }
}
