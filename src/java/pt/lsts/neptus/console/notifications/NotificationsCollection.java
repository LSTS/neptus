/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Hugo Dias
 * Nov 12, 2012
 */
package pt.lsts.neptus.console.notifications;

import java.util.LinkedList;
import java.util.List;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventNewNotification;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.events.NeptusEventHiddenMenus;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;

/**
 * @author Hugo
 * 
 */
public class NotificationsCollection {
    public static final int MAX_SIZE = 20;
    private List<Notification> list = new LinkedList<Notification>();
    private ConsoleLayout console;

    public NotificationsCollection(ConsoleLayout console) {
        this.console = console;
        NeptusEvents.register(this, console);
    }

    private synchronized void add(Notification noty) {
        // auto clean
        while (list.size() > MAX_SIZE) {
           list.remove(0);
        }
        list.add(noty);
        console.post(new ConsoleEventNewNotification(noty));
    }

    /**
     * @return the list
     */
    public synchronized List<Notification> getList() {
        List<Notification> clone = new LinkedList<Notification>();
        clone.addAll(list);
        return clone;
    }

    public void clear() {
        list.clear();
    }

    /*
     * EVENTS
     */
    @Subscribe
    public void onNotification(Notification e) {
        this.add(e);
    }

    @Subscribe
    public void handleHiddenMenus(NeptusEventHiddenMenus e) {
        this.add(Notification.warning("Hidden menus", "sadfsdfsdf"));
    }

    @Subscribe
    public void onVehicleStateChanged(ConsoleEventVehicleStateChanged e) {
        switch (e.getState()) {
            case DISCONNECTED:
                this.add(Notification.warning(I18n.text("Lost connection"), e.getDescription()).src(e.getVehicle()));
                break;
            case TELEOPERATION:
                this.add(Notification.warning(I18n.text("Started Teleoperation"), e.getDescription()).src(
                        e.getVehicle()));
                break;
            case ERROR:
                this.add(Notification.error(I18n.text("System in error"), e.getDescription()).src(e.getVehicle()));
                break;
            case SERVICE:
                this.add(Notification.success(I18n.text("System in service"), e.getDescription()).src(e.getVehicle()));
                break;
            case MANEUVER:
                this.add(Notification.success(I18n.text("Started Operation"), e.getDescription()).src(e.getVehicle()));
                break;
            default:
                this.add(Notification.info(e.getState().toString(), e.getDescription()).src(e.getVehicle()));
                break;
        }
    }
}
