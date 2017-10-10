/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 10/10/2017
 */
package pt.lsts.neptus.plugins.atlas.elektronik;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LatLonFormatEnum;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "SeaCat-MK1 Config")
public class SeaCatMK1Config extends ConsolePanel implements IEditorMenuExtension, ConfigurationListener {

    @NeptusProperty(name = "Lat/Lon Preferable Display Format", category = "Location", userLevel = LEVEL.REGULAR)
    private LatLonFormatEnum latLonPrefFormat = LatLonFormatEnum.DM;

    private GeneralPreferences gp;
    
    /**
     * @param console
     */
    public SeaCatMK1Config(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
     * @param console
     * @param usedInsideAnotherConsolePanel
     */
    public SeaCatMK1Config(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);
        initialize();
    }

    private void initialize() {
        gp = new GeneralPreferences();
        latLonPrefFormat = GeneralPreferences.latLonPrefFormat;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        gp.setProperties(PluginUtils.getPluginProperties(this));
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        for (IMapPopup str2d : getConsole().getSubPanelsOfInterface(IMapPopup.class))
            str2d.addMenuExtension(this);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        for (IMapPopup str2d : getConsole().getSubPanelsOfInterface(IMapPopup.class))
            str2d.removeMenuExtension(this);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType, pt.lsts.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
        JMenuItem confMenu = new JMenuItem(new AbstractAction(I18n.text("Configurations")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(SeaCatMK1Config.this, true);
            }
        });
        return Arrays.asList(new JMenuItem[] { confMenu });
    }
}
