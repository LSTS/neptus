/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 1 Mar 2016
 */
package pt.lsts.neptus.plugins.mvplanning.consoles;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Future;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.mission.MissionType;

/**
 * @author tsmarques
 *
 */
public class NeptusConsoleAdapter implements ConsoleAdapter {
    private ConsoleLayout console;
    
    public NeptusConsoleAdapter(ConsoleLayout console) {
        this.console = console;
    }

    @Override
    public void registerToEventBus(Object obj) {
        NeptusEvents.register(obj, console);
        
    }

    @Override
    public void post(Object event) {
        console.post(event);
    }

    @Override
    public boolean sendMessage(String dest, IMCMessage msg) {
        try {
            return console.getImcMsgManager().sendMessageToSystem(msg, dest);
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public Future<SendResult> sendMessageReliably(String dest, IMCMessage message) {
        return console.getImcMsgManager().sendMessageReliably(message, dest);
    }

    @Override
    public Map<String, ConsoleSystem> getSystems() {
        return console.getSystems();
    }

    @Override
    public MapGroup getMapGroup() {
        return MapGroup.getMapGroupInstance(console.getMission());
    }

    @Override
    public MissionType getMission() {
        return console.getMission();
    }

    @Override
    public AbstractElement[] getMapObstacles() {
        return (AbstractElement[]) console.getMission().
                generateMapGroup().
                getObstacles().
                toArray();
    }
}
