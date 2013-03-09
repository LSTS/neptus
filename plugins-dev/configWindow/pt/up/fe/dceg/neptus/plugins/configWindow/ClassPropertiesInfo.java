/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Nov 30, 2012
 */
package pt.up.fe.dceg.neptus.plugins.configWindow;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.util.ImageUtils;

import com.l2fprod.common.propertysheet.PropertySheetPanel;

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
    String defaultIcon = "images/menus/settings.png";


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
