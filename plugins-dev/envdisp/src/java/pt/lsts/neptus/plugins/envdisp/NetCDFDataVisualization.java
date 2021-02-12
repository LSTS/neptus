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
 * Author: pdias
 * Apr 22, 2018
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.util.Arrays;

import javax.swing.JDialog;

import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.envdisp.gui.LayersListPanel;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "netCDF Data Visualization", icon = "pt/lsts/neptus/plugins/envdisp/netcdf-radar.png")
public class NetCDFDataVisualization extends ConsoleLayer implements PropertiesProvider, ConfigurationListener {
    
    private static final String CATEGORY_TEST = "Test";

    @NeptusProperty(name = "Show visible data date-time interval", userLevel = LEVEL.ADVANCED, category = CATEGORY_TEST, 
            description = "Draws the string with visible currents data date-time interval.")
    private boolean showDataDebugLegend = false;

    private LayersListPanel layerList;
    private JDialog dialog = null;
    
    public NetCDFDataVisualization() {
        layerList = new LayersListPanel(getConsole());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        if (dialog != null)
            dialog.dispose();
    }

    @NeptusMenuItem("Tools > netCDF Data Visualization Config")
    public void configMenuAction() {
        if (dialog == null) {
            dialog = new JDialog(getConsole());
            dialog.setModalityType(ModalityType.MODELESS);
            dialog.setLayout(new BorderLayout());
            dialog.add(layerList);
            dialog.setSize(layerList.getPreferredSize());
        }
        dialog.setVisible(true);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        try {
            layerList.setStateRenderer2D(renderer);

            for (int i = layerList.getVarLayersList().size() - 1; i >= 0 ; i--) {
                GenericNetCDFDataPainter l = layerList.getVarLayersList().get(i);
                if (!l.isShowVar())
                    continue;
                
                Graphics2D g2 = (Graphics2D) g.create();
                l.paint(g2, renderer, showDataDebugLegend);
                g2.dispose();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.AbstractConsolePlugin#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        DefaultProperty[] superProps = super.getProperties();
        DefaultProperty[] layerProps = PluginUtils.getPluginProperties(layerList);
        DefaultProperty[] ret = Arrays.copyOf(superProps, superProps.length + layerProps.length);
        for (int i = 0; i < layerProps.length; i++) {
            ret[superProps.length + i] = layerProps[i]; 
        }
        return ret;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.AbstractConsolePlugin#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(layerList, properties);
        super.setProperties(properties);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.AbstractConsolePlugin#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        layerList.propertiesChanged();
        super.propertiesChanged();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#asElement(java.lang.String)
     */
    @Override
    public Element asElement(String rootElement) {
        Element root = super.asElement(rootElement);
        
        Element auxElm = root.addElement("aux");
        Element layersElm = layerList.asElement();
        layersElm.detach();
        auxElm.add(layersElm);

        return root;
    }
    
    @Override
    public void parseXmlElement(Element elem) {
        super.parseXmlElement(elem);
        Element auxElem = elem.element("aux");
        if (auxElem != null) {
            Element propsElem = auxElem.element("properties");
            if (propsElem != null)
                layerList.parseXmlElement(propsElem);
        }
    }
}
