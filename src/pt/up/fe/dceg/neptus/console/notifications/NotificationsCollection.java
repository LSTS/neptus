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
 * Nov 12, 2012
 * $Id:: NotificationsCollection.java 9615 2012-12-30 23:08:28Z pdias           $:
 */
package pt.up.fe.dceg.neptus.console.notifications;

import java.util.LinkedList;
import java.util.List;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventNewNotification;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.up.fe.dceg.neptus.events.NeptusEventHiddenMenus;
import pt.up.fe.dceg.neptus.events.NeptusEvents;
import pt.up.fe.dceg.neptus.i18n.I18n;

import com.google.common.eventbus.Subscribe;

/**
 * @author Hugo
 * 
 */
public class NotificationsCollection {
    private static final int MAX_SIZE = 500;
    private List<Notification> list = new LinkedList<Notification>();
    private ConsoleLayout console;

    public NotificationsCollection(ConsoleLayout console) {
        this.console = console;
        NeptusEvents.register(this, console);
    }

    private synchronized void add(Notification noty) {
        list.add(noty);
        console.post(new ConsoleEventNewNotification(noty));
        // auto clean
        if (list.size() > MAX_SIZE) {
            int removeCount = list.size() - MAX_SIZE;
            for (int i = 0; i < removeCount; i++)
                list.remove(i);
        }
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
