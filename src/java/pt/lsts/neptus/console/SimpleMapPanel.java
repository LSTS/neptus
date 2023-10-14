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
 * Author: pdias
 * Jan 7, 2018
 */
package pt.lsts.neptus.console;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.IMapRendererChangeEvent;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.IMapRendererChangeEvent.RendererChangeEvent;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.MissionType;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SimpleMapPanel extends ConsolePanel implements ConfigurationListener {

    @NeptusProperty(name = "Syncronize All Maps Movements", userLevel = LEVEL.ADVANCED)
    public boolean isSyncronizeAllMapsMovements = true;

    protected StateRenderer2D renderer = new StateRenderer2D();
    
    protected MissionType mission = null;
    private MapGroup mapGroup = null;

    /**
     * @param console
     */
    public SimpleMapPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        removeAll();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder()); // editor.setEditable(false);

        renderer.setMinDelay(0);
        renderer.setShowWorldMapOnScreenControls(false);
        add(renderer, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        setSize(500, 500);
        setMission(getConsole().getMission());
        
        renderer.addRendererChangeEvent(new IMapRendererChangeEvent() {
            @Override
            public void mapRendererChangeEvent(RendererChangeEvent event) {
                getConsole().post(event);
            }
        });
        
        propertiesChanged();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
    
    @Periodic(millisBetweenUpdates = 300)
    public void periodicRendererPaintCall() {
        renderer.repaint();
    }
    
    @Subscribe
    public void on(RendererChangeEvent event) {
        renderer.newRendererChangeEvent(event);
    }
    
    /** 
     * If override call this also.
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        renderer.setRespondToRendererChangeEvents(isSyncronizeAllMapsMovements);
    }

    public void setMission(MissionType mission) {
        if (mission == null)
            return;

        mapGroup = MapGroup.getMapGroupInstance(mission);
        renderer.setMapGroup(mapGroup);
        this.mission = mission;
    }
}
