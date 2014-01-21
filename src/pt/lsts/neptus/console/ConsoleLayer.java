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
 * Jan 21, 2014
 */
package pt.lsts.neptus.console;

import javax.swing.ImageIcon;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 *
 */
public abstract class ConsoleLayer implements IConsoleLayer {

    private float opacity = 1.0f;
    private ImageIcon icon = null;
    private ConsoleLayout console = null;
    
    @Override
    public final DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public final void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }


    @Override
    public final String getPropertiesDialogTitle() {
        return PluginUtils.getPluginName(getClass())+" parameters";
    }

    @Override
    public final String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }

    @Override
    public final void init(ConsoleLayout console) {
        this.console = console;
        initLayer();
    }

    @Override
    public final void clean() {
        cleanLayer();
    }
    
    public abstract void initLayer();
    public abstract void cleanLayer();

    @Override
    public float getOpacity() {
        return opacity;
    }
    
    public void setVisible(boolean visible) {
        if (!visible)
            setOpacity(0f);
        else if (opacity <= 0)
            opacity = 1.0f;
    }
    
    public boolean isVisible() {
        return getOpacity() > 0;
    }    

    @Override
    public void setOpacity(float opacity) {
        this.opacity = opacity;                 
    }

    @Override
    public abstract boolean userControlsOpacity();
    
    @Override
    public Element asElement(String rootElement) {
        Document doc = null;
        doc = DocumentHelper.createDocument();
        Element root = doc.addElement(rootElement);
        root.addAttribute("class", this.getClass().getName());
        root.addAttribute("opacity", ""+getOpacity());
        Element properties = root.addElement("properties");
        
        String xml = PluginUtils.getConfigXml(this);
        try {
            Element el = DocumentHelper.parseText(xml).getRootElement();

            for (Object child : el.elements()) {
                Element aux = (Element) child;
                aux.detach();
                properties.add(aux);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return root;
    }
    
    @Override
    public void parseXmlElement(Element elem) {
        Attribute attr = elem.attribute("opacity");
        if (attr != null)
            setOpacity(Float.parseFloat(attr.getText()));
        
        PluginUtils.setConfigXml(this, elem.element("properties").asXML());      
    }
    
    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }
    
    @Override
    public final ImageIcon getIcon() {
        if (icon == null)
            icon = ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass()));
        
        return icon;
    }

    /**
     * @return the console
     */
    public final ConsoleLayout getConsole() {
        return console;
    }

}
