/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Margarida Faria
 * Apr 10, 2013
 */
package pt.up.fe.dceg.neptus.plugins.spot;

import java.io.IOException;
import java.util.HashMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Margarida Faria
 *
 */
public class SpotMsgFetcher {
    // private static final String id = "LSTSSPOT";
    private static final String pageUrl = "http://tiny.cc/spot1";

    /**
     * Get messages on SPOT page.
     * 
     * @return
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static HashMap<String, TreeSet<SpotMessage>> get() throws ParserConfigurationException, SAXException,
            IOException {

        HashMap<String, TreeSet<SpotMessage>> msgBySpot = new HashMap<String, TreeSet<SpotMessage>>();
        TreeSet<SpotMessage> spotMsgTree;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(pageUrl);
        NodeList nlist = doc.getFirstChild().getChildNodes();

        // go through messages
        for (int i = 1; i < nlist.getLength(); i++) {
            String tagName = nlist.item(i).getNodeName();
            if (tagName.equals("message")) {
                double lat = 0, lon = 0;
                String id = "SPOT";
                long timestamp = System.currentTimeMillis();

                // go through message elements
                NodeList elems = nlist.item(i).getChildNodes();
                for (int j = 0; j < elems.getLength(); j++) {
                    String tag = elems.item(j).getNodeName();

                    if (tag.equals("latitude"))
                        lat = Double.parseDouble(elems.item(j).getTextContent());
                    else if (tag.equals("longitude"))
                        lon = Double.parseDouble(elems.item(j).getTextContent());
                    else if (tag.equals("esnName"))
                        id = elems.item(j).getTextContent();
                    else if (tag.equals("timeInGMTSecond")) {
                        timestamp = Long.parseLong(elems.item(j).getTextContent());
                        timestamp *= 1000; // secs to millis
                    }
                }
                spotMsgTree = msgBySpot.get(id);
                if (spotMsgTree == null) {
                    spotMsgTree = new TreeSet<SpotMessage>();
                }
                spotMsgTree.add(new SpotMessage(lat, lon, timestamp, id));
            }
        }
        return msgBySpot;
    }

}
