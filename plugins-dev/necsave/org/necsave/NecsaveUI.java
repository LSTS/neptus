/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Oct 19, 2015
 */
package org.necsave;

import java.util.LinkedHashMap;

import com.google.common.eventbus.Subscribe;

import info.necsave.msgs.Contact;
import info.necsave.msgs.ContactList;
import info.necsave.msgs.Kinematics;
import info.necsave.msgs.PlatformInfo;
import info.necsave.msgs.PlatformPlanProgress;
import info.necsave.proto.Message;
import pt.lsts.imc.JsonObject;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * This class will show the states of NECSAVE platforms and allows interactions with them
 * @author zp
 * 
 */
@PluginDescription(name="NECSAVE UI")
public class NecsaveUI extends ConsoleInteraction {

    private NecsaveTransport transport = null;
    private LinkedHashMap<Integer, String> platformNames = new LinkedHashMap<>();
    private LinkedHashMap<Integer, PlatformPlanProgress> planProgresses = new LinkedHashMap<>(); 
    private LinkedHashMap<String, LocationType> contacts = new LinkedHashMap<>();
    
    @Override
    public void initInteraction() {
        try {
            transport = new NecsaveTransport(getConsole());            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }
    
    @Subscribe
    public void on(Kinematics msg) {
        if (!platformNames.containsKey(msg.getSrc()))
            return;
        
        String name = platformNames.get(msg.getSrc());
        
        if (ExternalSystemsHolder.lookupSystem(name) == null) {
            ExternalSystem es = new ExternalSystem(name);
            ExternalSystemsHolder.registerSystem(es);
            es.setActive(true);
            es.setType(SystemTypeEnum.UNKNOWN);
        }
        ExternalSystem extSys = ExternalSystemsHolder.lookupSystem(name);
        LocationType loc = new LocationType(Math.toDegrees(msg.getWaypoint().getLatitude()), 
                Math.toDegrees(msg.getWaypoint().getLongitude()));
        loc.setDepth(msg.getWaypoint().getDepth());
        extSys.setLocation(loc, System.currentTimeMillis());       
    }
    
    @Subscribe
    public void on(PlatformInfo msg) {
        platformNames.put(msg.getSrc(), msg.getPlatformName());
    }
    
    @Subscribe
    public void on(PlatformPlanProgress msg) {
        planProgresses.put(msg.getSrc(), msg);
    }
       
    @Subscribe
    public void on(ContactList msg) {
        for (Contact c : msg.getContactsList())
            on(c);        
    }
    
    @Subscribe
    public void on(Contact msg) {
        getConsole().post(Notification.info("NECSAVE", "Contact detected by "+msg.getSrc()));
        LocationType loc = new LocationType();
        loc.setLatitudeRads(msg.getObject().getLatitude());
        loc.setLongitudeRads(msg.getObject().getLongitude());
        loc.setDepth(msg.getObject().getDepth());
        contacts.put(msg.getSrc()+"."+msg.getContactId(), loc);        
    }
    
    @Subscribe
    public void on(Message msg) {
        JsonObject json = new JsonObject();
        json.setJson(msg.asJSON(false));
        LsfMessageLogger.log(json);
    }

    @Override
    public void cleanInteraction() {
        if (transport != null)
            transport.stop();
    }
}
