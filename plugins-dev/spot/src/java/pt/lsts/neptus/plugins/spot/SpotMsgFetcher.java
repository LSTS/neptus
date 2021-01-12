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
 * Author: Margarida Faria
 * Apr 10, 2013
 */
package pt.lsts.neptus.plugins.spot;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import pt.lsts.neptus.NeptusLog;

/**
 * @author Margarida Faria
 *
 */
public class SpotMsgFetcher {
    /**
     * Get messages on SPOT page.
     *
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static HashMap<String, TreeSet<SpotMessage>> get(int hours, String stream)
            throws ParserConfigurationException, SAXException, IOException {
        long currentTime = System.currentTimeMillis() / 1000;
        long timeWindow = hours * 60 * 60;
        long startOfTimeWindowSecs = currentTime - timeWindow;
        String url = "https://api.findmespot.com/spot-main-web/consumer/rest-api/2.0/public/feed/" + stream
                + "/message.xml";

        HashMap<String, TreeSet<SpotMessage>> msgBySpot = new HashMap<String, TreeSet<SpotMessage>>();
        // TreeSet<SpotMessage> spotMsgTree;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        try {
            Document doc = db.parse(new URL(url).openStream());
            // TODO Error with first char being space
            // File file = new File("/home/meg/LSTS/spot.xml");
            // Document doc = db.parse(file);
            NodeList nlist = doc.getFirstChild().getChildNodes();
            // go through messages
            // TODO Different structure
            if (nlist.getLength() == 1) {
                Node feedMsgResp = nlist.item(0);
                String tagName = feedMsgResp.getNodeName();
                if (tagName.equals("feedMessageResponse")) {
                    nlist = feedMsgResp.getChildNodes();
                    if (nlist.getLength() == 5) {
                        for (int i = 1; i < nlist.getLength(); i++) {
                            Node messages = nlist.item(i);
                            tagName = messages.getNodeName();
                            if (tagName.equals("messages")) {
                                nlist = messages.getChildNodes();
                                for (int m = 1; m < nlist.getLength(); m++) {
                                    tagName = nlist.item(m).getNodeName();
                                    if (tagName.equals("message")) {
                                        // TODO this is the same
                                        processMsg(startOfTimeWindowSecs, msgBySpot, nlist, m);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        // Bad xml
                        NeptusLog.pub().error("Unexpected element number in xml structure level 2.");
                    }
                }
                else {
                    // Bad xml
                    NeptusLog.pub().error("Unexpected element at xml structure level 1.");
                }
            }
            else {
                // Bad xml
                NeptusLog.pub().error("Unexpected root elements");
            }

        }
        catch (SAXParseException e) {
            NeptusLog.pub().warn("Error parsing xml!");
        }
        catch (Exception e) {
            NeptusLog.pub().warn("Error getting SPOT info!");
        }
        return msgBySpot;
    }

    private static void processMsg(long startOfTimeWindowSecs, HashMap<String, TreeSet<SpotMessage>> msgBySpot,
            NodeList nlist, int i) {
        TreeSet<SpotMessage> spotMsgTree;
        double lat = 0, lon = 0;
        String id = "_";
        long timestamp = System.currentTimeMillis();

        // go through message elements
        NodeList elems = nlist.item(i).getChildNodes();
        for (int j = 0; j < elems.getLength(); j++) {
            String tag = elems.item(j).getNodeName();

            if (tag.equals("latitude"))
                lat = Double.parseDouble(elems.item(j).getTextContent());
            else if (tag.equals("longitude"))
                lon = Double.parseDouble(elems.item(j).getTextContent());
            else if (tag.equals("messengerName"))
                id = elems.item(j).getTextContent();
            else if (tag.equals("unixTime")) {
                timestamp = Long.parseLong(elems.item(j).getTextContent()); // seconds
            }
        }

        // only add messages within time window
        if (timestamp > startOfTimeWindowSecs) {
            spotMsgTree = msgBySpot.get(id);
            if (spotMsgTree == null) {
                spotMsgTree = new TreeSet<SpotMessage>();
                msgBySpot.put(id, spotMsgTree);
            }
            Spot.log.debug("Adding " + id + " " + timestamp + " @ (" + lat + ", " + lon + ")");
            spotMsgTree.add(new SpotMessage(lat, lon, timestamp, id));

        }
    }

}
