/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 31, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fr.cls.argos.dataxmldistribution.service.DixService;
import fr.cls.argos.dataxmldistribution.service.types.XmlRequestType;
import fr.cls.argos.dataxmldistribution.service.types.XsdRequestType;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * 
 */
public class ArgosLocationProvider implements ILocationProvider {

    @NeptusProperty
    private String argosUsername = null;

    @NeptusProperty
    private String argosPassword = null;

    @NeptusProperty
    private String platformId = "136978";

    private boolean enabled = true;

    private boolean askCredentials = true;
    private SituationAwareness sitAwareness;

    {
        try {
            PluginUtils.loadProperties("conf/argosCredentials.props", this);
        }
        catch (Exception e) {
        }
    }

    private String getArgosUsername() {
        if (argosUsername == null)
            return "";
        return argosUsername;
    }

    private String getArgosPassword() {
        if (argosPassword == null)
            return "";
        return StringUtils.newStringUtf8(DatatypeConverter.parseBase64Binary(argosPassword));
    }

    private void setArgosPassword(String password) {
        if (password == null)
            this.argosPassword = null;

        this.argosPassword = DatatypeConverter.printBase64Binary(password.getBytes(Charset.forName("UTF8")));
    }

    private void setArgosUsername(String username) {
        if (username == null)
            this.argosUsername = null;
        this.argosUsername = username;
    }

    public String getXsd() throws Exception {
        DixService srv = new DixService();
        XsdRequestType request = new XsdRequestType();
        return srv.getDixServicePort().getXsd(request).getReturn();
    }

    @Periodic(millisBetweenUpdates = 120000)
    public void updatePositions() {
        if (!enabled)
            return;
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));

            DixService srv = new DixService();
            XmlRequestType request = new XmlRequestType();

            if (askCredentials || argosPassword == null) {
                Pair<String, String> credentials = GuiUtils.askCredentials(ConfigFetch.getSuperParentFrame(),
                        "Enter Argos Credentials", getArgosUsername(), getArgosPassword());
                if (credentials == null) {
                    enabled = false;
                    return;
                }
                setArgosUsername(credentials.first());
                setArgosPassword(credentials.second());
                try {
                    PluginUtils.saveProperties("conf/argosCredentials.props", this);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                askCredentials = false;
            }

            request.setUsername(getArgosUsername());
            request.setPassword(getArgosPassword());
            request.setMostRecentPassages(true);
            request.setPlatformId(platformId);
            request.setNbDaysFromNow(10);

            String xml = srv.getDixServicePort().getXml(request).getReturn();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
            NodeList locations = doc.getElementsByTagName("location");
            for (int i = 0; i < locations.getLength(); i++) {
                Node locNode = locations.item(i);
                Node platformNode = locNode.getParentNode().getParentNode();
                Node platfId = platformNode.getFirstChild();
                String id = platfId.getTextContent();
                NodeList childs = locNode.getChildNodes();
                String lat = null, lon = null, date = null, locClass = null;

                for (int j = 0; j < childs.getLength(); j++) {
                    Node elem = childs.item(j);
                    switch (elem.getNodeName()) {
                        case "locationDate":
                            date = elem.getTextContent();
                            break;
                        case "latitude":
                            lat = elem.getTextContent();
                            break;
                        case "longitude":
                            lon = elem.getTextContent();
                            break;
                        case "locationClass":
                            locClass = elem.getTextContent();
                            break;
                        default:
                            break;
                    }
                }
                AssetPosition pos = new AssetPosition("Argos_" + id, Double.parseDouble(lat), Double.parseDouble(lon));
                pos.setSource(getName());
                pos.setType("Argos Tag");
                pos.putExtra("Loc. Class", locClass);
                pos.setTimestamp(df.parse(date.replaceAll("T", " ").replaceAll("Z", "")).getTime());
                if (sitAwareness != null)
                    sitAwareness.addAssetPosition(pos);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);
            sitAwareness.postNotification(Notification.error("Situation Awareness", e.getClass().getSimpleName()+" while polling ARGOS positions from Web.").requireHumanAction(false));    
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onInit(SituationAwareness instance) {
        this.sitAwareness = instance;
    }

    @Override
    public void onCleanup() {

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.sunfish.awareness.ILocationProvider#getName()
     */
    @Override
    public String getName() {
        return "ARGOS (Web Service)";
    }

    public static void main(String[] args) throws Exception {
        ArgosLocationProvider provider = new ArgosLocationProvider();
        provider.updatePositions();
    }
}
