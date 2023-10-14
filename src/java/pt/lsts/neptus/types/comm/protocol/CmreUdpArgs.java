/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 24, 2014
 */
package pt.lsts.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author zp
 *
 */
public class CmreUdpArgs extends ProtocolArgs implements PropertiesProvider {

    @NeptusProperty
    private String hostname = "127.0.0.1";
    
    @NeptusProperty
    private int command_port = 5983;

    @NeptusProperty
    private int reply_port = 5001;
    
    @NeptusProperty
    private int telemetry_port = 5984;
    
    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        if (!hostname.isEmpty())
            root.addElement("hostname", hostname);
        root.addElement("command", ""+command_port);
        root.addElement("telemetry", ""+telemetry_port);
        root.addElement("reply", ""+reply_port);
        return document;
    }

    @Override
    public boolean load(Element elem) {
        try {
            hostname = elem.selectSingleNode("//hostname").getText();
            telemetry_port = Integer.parseInt(elem.selectSingleNode("//telemetry").getText());
            reply_port = Integer.parseInt(elem.selectSingleNode("//reply").getText());
            command_port = Integer.parseInt(elem.selectSingleNode("//command").getText());
            return true;            
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }
    
    
    @Override
    public String getPropertiesDialogTitle() {
        return "CMRE UDP parameters";
    }
    
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }
    
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the command_port
     */
    public int getCommandPort() {
        return command_port;
    }

    /**
     * @param command_port the command_port to set
     */
    public void setCommandPort(int command_port) {
        this.command_port = command_port;
    }

    /**
     * @return the reply_port
     */
    public int getReplyPort() {
        return reply_port;
    }

    /**
     * @param reply_port the reply_port to set
     */
    public void setReplyPort(int reply_port) {
        this.reply_port = reply_port;
    }

    /**
     * @return the telemetry_port
     */
    public int getTelemetryPort() {
        return telemetry_port;
    }

    /**
     * @param telemetry_port the telemetry_port to set
     */
    public void setTelemetryPort(int telemetry_port) {
        this.telemetry_port = telemetry_port;
    }
}
