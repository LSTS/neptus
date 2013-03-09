/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Hugo
 * Oct 22, 2012
 */
package pt.up.fe.dceg.neptus.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.up.fe.dceg.neptus.NeptusConfig;
import pt.up.fe.dceg.neptus.imc.Announce;
import pt.up.fe.dceg.neptus.systems.links.ImcSystemLink;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.google.common.eventbus.Subscribe;

/**
 * @author Hugo
 * 
 */
public class SystemsManager {
    public enum SystemLinkType {
        IMC
    }

    public enum SystemClass {
        CCU(0),
        HUMANSENSOR(1),
        UUV(2),
        USV(3),
        UAV(4),
        UGV(5),
        STATICSENSOR(6),
        MOBILESENSOR(7),
        WSN(8);
        protected long value;

        public long value() {
            return value;
        }

        SystemClass(long value) {
            this.value = value;
        }
    }

    private Map<Integer, SystemType> systems = new ConcurrentHashMap<Integer, SystemType>();
    //private SystemType self;

    public SystemsManager(NeptusConfig config) {
    }

    public SystemsManager buildSelf() {
       // self = new SystemType();
        return this;
    }

    /*
     * EVENTS
     */
    @Subscribe
    public void handleIMCAnnounce(Announce announce) {
        System.out.println(announce.getSrc() + " " + announce.getSysName());
        int id = announce.getSrc();
        if (systems.containsKey(id)) {
            SystemType system = systems.get(id);
            LocationType location = new LocationType(Math.toDegrees(announce.getLat()), Math.toDegrees(announce
                    .getLon()));
            location.setHeight(announce.getHeight());
            system.setLocation(location);
           
            system.setLocationAge(announce.getTimestampMillis());
        }
        else {
            SystemType system = new SystemType();
            system.setId(id);
            system.setName(announce.getSysName());
            system.setHumanName(announce.getSysName());
            system.setType(SystemClass.valueOf(announce.getSysType().toString()));

            LocationType location = new LocationType(Math.toDegrees(announce.getLat()), Math.toDegrees(announce
                    .getLon()));
            location.setHeight(announce.getHeight());
            system.setLocation(location);
            system.setLocationAge(announce.getTimestampMillis());
            system.setLink(SystemLinkType.IMC, new ImcSystemLink().update(announce));

            systems.put(id, system);
        }
    }

    /*
     * HELPERS
     */

    /*
     * ACCESSORS
     */
    /**
     * @return the systems
     */
    public Map<Integer, SystemType> getSystems() {
        return systems;
    }
}
