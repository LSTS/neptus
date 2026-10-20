/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * Apr 2, 2026
 */
package pt.lsts.neptus.types.comm.protocol;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

import java.util.Date;

/**
 * @author Paulo Dias
 *
 */
public class CloudloopArgs extends ProtocolArgs implements PropertiesProvider {

    @NeptusProperty
    private String thingId = "";
    @NeptusProperty
    private String thingId1 = "";

    private int lastActiveThingId = 0;
    private Date lastThingIdDateReceived = new Date(0);

    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        if (!thingId.isEmpty())
            root.addElement("thingId", thingId);
        if (!thingId1.isEmpty())
            root.addElement("thingId1", thingId1);
        return document;
    }

    @Override
    public boolean load(Element elem) {
        try {
            thingId = elem.selectSingleNode("//thingId").getText();
            Node thingId1Elem = elem.selectSingleNode("//thingId1");
            thingId1 = thingId1Elem != null ? thingId1Elem.getText() : "";
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setLastSeenThingId(int thingId, Date date) {
        if (!lastThingIdDateReceived.before(date))
            return false;
        int oldThingId = lastActiveThingId;
        if (thingId == 0)
            lastActiveThingId = 0;
        else if (thingId == 1)
            lastActiveThingId = 1;
        else
            return false;
        if (oldThingId != lastActiveThingId) {
            NeptusLog.pub().info("Switched to ThingId {} from old {}", getLastSeenThingId(), oldThingId);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Switched to Thing " + getLastSeenThingId() + " from old " + oldThingId);
        }
        lastThingIdDateReceived = date;
        return true;
    }

    public boolean setLastSeenThingId(String thingId, Date date) {
        if (!lastThingIdDateReceived.before(date))
            return false;
        int oldThingId = lastActiveThingId;
        if (thingId == null || thingId.isEmpty())
            return false;
        if (thingId.equals(this.thingId))
            lastActiveThingId = 0;
        else if (thingId.equals(this.thingId1))
            lastActiveThingId = 1;
        else
            return false;
        if (oldThingId != lastActiveThingId) {
            NeptusLog.pub().info("Switched to ThingId {} from old {} to new {}", getLastSeenThingId(), oldThingId, lastActiveThingId);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Switched to ThingId " + getLastSeenThingId() + " from old " + oldThingId + " to new " + lastActiveThingId);
        }
        lastThingIdDateReceived = date;
        return true;
    }

    public String getLastSeenThingId() {
        if (lastActiveThingId == 0)
            return thingId;
        else if (lastActiveThingId == 1)
            return thingId1;
        return "";
    }

    /**
     * @return the thingId
     */
    public String getThingId() {
        return thingId;
    }

    /**
     * @param thingId the thingId to set
     */
    public void setThingId(String thingId) {
        this.thingId = thingId;
    }    

    public String getThingId1() {
        return thingId1;
    }

    public void setThingId1(String thingId1) {
        this.thingId1 = thingId1;
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }
    
    
    @Override
    public String getPropertiesDialogTitle() {
        return "Cloudloop Iridium parameters";
    }
    
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }
    
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }
}
