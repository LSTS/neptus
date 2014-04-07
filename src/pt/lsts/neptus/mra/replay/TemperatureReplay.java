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
 * Author: hfq
 * Apr 7, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Graphics2D;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author hfq
 * 
 */
@PluginDescription(name = "Temperature Replay", icon = "pt/lsts/neptus/plugins/ctd/thermometer.png")
public class TemperatureReplay implements LogReplayLayer {

    /**
     * 
     */
    public TemperatureReplay() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#canBeApplied(pt.lsts.neptus.mra.importers.IMraLogGroup,
     * pt.lsts.neptus.mra.replay.LogReplayComponent.Context)
     */
    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLsfIndex().getEntityId("CTD") != 255
                && source.getLsfIndex().containsMessagesOfType("Temperature");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getName()
     */
    @Override
    public String getName() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#parse(pt.lsts.neptus.mra.importers.IMraLogGroup)
     */
    @Override
    public void parse(IMraLogGroup source) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#onMessage(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void onMessage(IMCMessage message) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#getVisibleByDefault()
     */
    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.mra.replay.LogReplayComponent#cleanup()
     */
    @Override
    public void cleanup() {

    }

}
