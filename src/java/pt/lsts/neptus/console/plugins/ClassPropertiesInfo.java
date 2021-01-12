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
 * Author: Margarida Faria
 * Nov 30, 2012
 */
package pt.lsts.neptus.console.plugins;

import javax.swing.ImageIcon;

import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.util.ImageUtils;

/**
 * Data structure to hold all the information relevant to one class for settings editing
 * 
 * @author Margarida Faria
 * 
 */
public class ClassPropertiesInfo {
    private final PropertiesProvider classInstance;
    private final CATEGORY functionality;
    private final String name;
    private PropertySheetPanel propertiesPanel;
    private final ImageIcon icon;
    private String defaultIcon = "images/menus/settings.png";


    /**
     * 
     * @param classInstance
     * @param functionality
     * @param name
     * @param panel
     * @param pathToIcon
     */
    public ClassPropertiesInfo(PropertiesProvider classInstance, CATEGORY functionality, String name,
            PropertySheetPanel panel,
            String pathToIcon) {
        super();
        this.classInstance = classInstance;
        this.functionality = functionality;
        this.name = name;
        propertiesPanel = panel;

        ImageIcon raw;
        if (pathToIcon == null || pathToIcon.length() == 0) {
            raw = ImageUtils.createImageIcon(defaultIcon);
        }
        else {
            raw = ImageUtils.createImageIcon(pathToIcon);
            if (raw == null) {
                raw = ImageUtils.createImageIcon(defaultIcon);
            }
            else {
                defaultIcon = pathToIcon;
                raw = new ImageIcon(ImageUtils.getFastScaledImage(raw.getImage(), 12, 12, false));
            }
        }
        this.icon = raw;
    }

    /**
     * @return the classInstance
     */
    public PropertiesProvider getClassInstance() {
        return classInstance;
    }

    /**
     * @return the functionality
     */
    public CATEGORY getFunctionality() {
        return functionality;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the defaultIcon
     */
    public String getDefaultIconPath() {
        return defaultIcon;
    }

    /**
     * @return the propertiesPanel
     */
    public PropertySheetPanel getPropertiesPanel() {
        return propertiesPanel;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * @return the icon
     */
    public ImageIcon getIcon() {
        return icon;
    }

    /**
     * @param propertiesPanel the propertiesPanel to set
     */
    public void setPropertiesPanel(PropertySheetPanel propertiesPanel) {
        this.propertiesPanel = propertiesPanel;
    }
}
