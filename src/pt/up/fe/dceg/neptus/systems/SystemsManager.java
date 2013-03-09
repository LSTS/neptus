/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
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
