/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: tsm 
 * 2016/12/05
 */
package pt.lsts.neptus.plugins.multibeam.viewers;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JPanel;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.google.common.collect.ObjectArrays;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.multibeam.console.MultibeamRealTimeWaterfall;
import pt.lsts.neptus.plugins.update.Periodic;

/**
 * @author tsm
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Tiago Marques", version = "0.5", name = "Multibeam: Dual Viewer", description = "Displays multibeam waterfall and cross-section viewers")
@Popup(pos = Popup.POSITION.TOP_LEFT, width = 920, height = 500)
public class MultibeamDualViewer extends ConsolePanel {
    
    // Parameters Tmp
    @NeptusProperty (name="Color map to use", category="Common - Visualization parameters", userLevel = LEVEL.REGULAR)
    private ColorMap colorMap = ColorMapFactory.createJetColorMap();

    @NeptusProperty (name="Max depth", description="Max depth used to normalize depth data", 
            category="Waterfall Viewer - Visualization parameters", userLevel = LEVEL.REGULAR)
    private double maxDepth = 30;

    @NeptusProperty (name="Use adaptive max depth", description = "Use the highest value processed as max depth. Minimum value will be 'Max depth'",
            category="Waterfall Viewer - Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean adaptativeMaxDepth = true;
    
    @NeptusProperty (name="Clean lines on vehicle change", category="Waterfall Viewer - Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean cleanLinesOnVehicleChange = false;

    @NeptusProperty(name="Sensor's range", category="Cross Section - Visualization parameters", userLevel = NeptusProperty.LEVEL.REGULAR)
    private double mbRange = 30;

    // GUI
    private JPanel viewersPanel;

    // data viewers
    private MultibeamCrossSection crossSection;
    private MultibeamRealTimeWaterfall waterfall;

    public MultibeamDualViewer(ConsoleLayout console) {
        super(console);
        crossSection = new MultibeamCrossSection(console, true);
        waterfall = new MultibeamRealTimeWaterfall(console, true);
        
        crossSection.addListener(waterfall);
        waterfall.addListener(crossSection);

        viewersPanel = new JPanel();
        viewersPanel.setLayout(new MigLayout());
        viewersPanel.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));
        viewersPanel.setLayout(new MigLayout());

        viewersPanel.add(waterfall, "w 37%, h 100%");
        viewersPanel.add(crossSection, "w 63%, h 100%");
        this.setLayout(new MigLayout("ins 0, gap 0", "[][]"));
        this.add(viewersPanel, "w 100%, h 100%,  grow");
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#init()
     */
    @Override
    public void init() { // Needed overwritten in order to proper initialize the "sub" ConsolePanels
        crossSection.init();
        waterfall.init();
        super.init();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#clean()
     */
    @Override
    public void clean() { // Needed overwritten in order to proper clean the "sub" ConsolePanels
        super.clean();
        crossSection.clean();
        waterfall.clean();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#XML_PropertiesRead(org.dom4j.Element)
     */
    protected void readPropertiesFromXml(Element e) {
        super.readPropertiesFromXml(e);
        
        PluginUtils.setConfigXml(crossSection, e.asXML());
        PluginUtils.setConfigXml(waterfall, e.asXML());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#XML_PropertiesWrite(org.dom4j.Element)
     */
    protected void writePropertiesToXml(Element e) {
        //super.XML_PropertiesWrite(e);
        
        String xml = PluginUtils.getConfigXml(this, waterfall, crossSection);
        try {
            Element el = DocumentHelper.parseText(xml).getRootElement();

            for (Object child : el.elements()) {
                Element aux = (Element) child;
                aux.detach();
                e.add(aux);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public DefaultProperty[] getProperties() {
        //DefaultProperty[] props = super.getProperties();
        
        DefaultProperty[] props1 = waterfall.getProperties();
        DefaultProperty[] props2 = crossSection.getProperties();

        List<DefaultProperty> propsLst = new ArrayList<>();
        
        DefaultProperty[] propsTmp = ObjectArrays.concat(props1, props2, DefaultProperty.class);

        for (DefaultProperty p : propsTmp) {
            Predicate<DefaultProperty> p1 = pr -> pr.getName().equalsIgnoreCase(p.getName());
            if (propsLst.stream().anyMatch(p1))
                continue;
            else
                propsLst.add(p);
        }
        
        return propsLst.toArray(new DefaultProperty[propsLst.size()]);
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        String[] errors0 = super.getPropertiesErrors(properties);
        
        String[] errors1 = waterfall.getPropertiesErrors(properties);
        String[] errors2 = crossSection.getPropertiesErrors(properties);
        
        String[] errors = ObjectArrays.concat(errors0, errors1, String.class);
        errors = ObjectArrays.concat(errors, errors2, String.class);
        
        return errors;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        
        waterfall.setProperties(properties);
        crossSection.setProperties(properties);
    }

    // FIXME
    @Periodic(millisBetweenUpdates = 800)
    public void onPeriodicUpdate() {
        super.setProperties(this.getProperties());
    }
}
