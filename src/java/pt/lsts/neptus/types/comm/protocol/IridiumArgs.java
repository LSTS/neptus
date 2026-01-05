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
 * Author: zp
 * Feb 24, 2014
 */
package pt.lsts.neptus.types.comm.protocol;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import org.dom4j.Node;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

import java.util.Date;

/**
 * @author zp
 *
 */
public class IridiumArgs extends ProtocolArgs implements PropertiesProvider {

    @NeptusProperty
    private String imei = "";
    @NeptusProperty
    private String imei1 = "";

    private int lastActiveImei = 0;
    private Date lastImeiDateReceived = new Date(0);

    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement(rootElementName);
        if (!imei.isEmpty())
            root.addElement("imei", imei);
        if (!imei1.isEmpty())
            root.addElement("imei1", imei1);
        return document;
    }

    @Override
    public boolean load(Element elem) {
        try {
            imei = elem.selectSingleNode("//imei").getText();
            Node imei1Elem = elem.selectSingleNode("//imei1");
            imei1 = imei1Elem != null ? imei1Elem.getText() : "";
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setLastSeenImei(int imei, Date date) {
        if (!lastImeiDateReceived.before(date))
            return false;
        int oldImei = lastActiveImei;
        if (imei == 0)
            lastActiveImei = 0;
        else if (imei == 1)
            lastActiveImei = 1;
        else
            return false;
        if (oldImei != lastActiveImei) {
            NeptusLog.pub().info("Switched to IMEI {} from old {}", getLastSeenImei(), oldImei);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Switched to IMEI " + getLastSeenImei() + " from old " + oldImei);
        }
        lastImeiDateReceived = date;
        return true;
    }

    public boolean setLastSeenImei(String imei, Date date) {
        if (!lastImeiDateReceived.before(date))
            return false;
        int oldImei = lastActiveImei;
        if (imei == null || imei.isEmpty())
            return false;
        if (imei.equals(this.imei))
            lastActiveImei = 0;
        else if (imei.equals(this.imei1))
            lastActiveImei = 1;
        else
            return false;
        if (oldImei != lastActiveImei) {
            NeptusLog.pub().info("Switched to IMEI {} from old {} to new {}", getLastSeenImei(), oldImei, lastActiveImei);
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Switched to IMEI " + getLastSeenImei() + " from old " + oldImei + " to new " + lastActiveImei);
        }
        lastImeiDateReceived = date;
        return true;
    }

    public String getLastSeenImei() {
        if (lastActiveImei == 0)
            return imei;
        else if (lastActiveImei == 1)
            return imei1;
        return "";
    }

    /**
     * @return the imei
     */
    public String getImei() {
        return imei;
    }

    /**
     * @param imei the imei to set
     */
    public void setImei(String imei) {
        this.imei = imei;
    }    

    public String getImei1() {
        return imei1;
    }

    public void setImei1(String imei1) {
        this.imei1 = imei1;
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }
    
    
    @Override
    public String getPropertiesDialogTitle() {
        return "Iridium parameters";
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
