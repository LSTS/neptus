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
 * Oct 12, 2012
 * $Id:: NeptusEvents.java 9875 2013-02-06 15:38:31Z zepinto                    $:
 */
package pt.up.fe.dceg.neptus.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.renderer2d.InteractionAdapter;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * @author Hugo
 * 
 */
public enum NeptusEvents {
    INSTANCE;
    private EventBus eventBus;
    private EventBus mainSystemEventBus;
    private EventBus otherSystemEventBus;
    private Map<ConsoleLayout, EventBus> consoleBus = new HashMap<>();

    private NeptusEvents() {
        ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("global events handler");
                t.setDaemon(true);
                return t;
            }
        });
        eventBus = new AsyncEventBus(service);
        eventBus.register(this);
        service = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("main system events handler");
                t.setDaemon(true);
                return t;
            }
        });
        mainSystemEventBus = new AsyncEventBus(service);
        mainSystemEventBus.register(this);
        service = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("other system events handler");
                t.setDaemon(true);
                return t;
            }
        });
        otherSystemEventBus = new AsyncEventBus(service);
        otherSystemEventBus.register(this);
    }

    /**
     * Post a new event to the GLOBAL synchronous event bus
     * 
     * @param arg0
     */
    public static void post(Object arg0) {
        NeptusEvents.INSTANCE.eventBus.post(arg0);
    }

    /**
     * Post a new event to the given console asynchronous event bus
     * 
     * @param arg0
     * @param console
     */
    public static void post(Object arg0, ConsoleLayout console) {
        EventBus bus = INSTANCE.consoleBus.get(console);
        if (bus == null) {
            NeptusLog.pub().error("tried to post to a console bus that doesnt exist! " + console.getTitle());
        }
        else {
            bus.post(arg0);
        }
    }

    /**
     * Register for the GLOBAL synchronous event bus
     * 
     * @param object
     */
    public static void register(Object object) {
        INSTANCE.eventBus.register(object);
    }

    /**
     * Register for the given console asynchronous event bus
     * 
     * @param object
     * @param console
     */
    public static void register(Object object, ConsoleLayout console) {
        EventBus bus = INSTANCE.consoleBus.get(console);
        if (bus == null) {
            if (!(object instanceof InteractionAdapter))
                NeptusLog.pub().error(
                        "tried to register in a console bus that doesnt exist. " + ((SubPanel) object).getName());
        }
        else {
            bus.register(object);
        }
    }

    /**
     * Unregister for the given console asynchronous event bus
     * 
     * @param object
     * @param console
     */
    public static void unregister(Object object, ConsoleLayout console) {
        EventBus bus = INSTANCE.consoleBus.get(console);
        if (bus == null) {
            NeptusLog.pub().error("tried to unregister in a console bus that doesnt exist.");
        }
        else {
            try {
                bus.unregister(object);
            }
            catch (IllegalArgumentException e) {
                NeptusLog.pub().info(e, e);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
            }
        }
    }

    /**
     * Creates a event bus for a given console this should only be called at the {@link ConsoleLayout} construct
     * 
     * @param console
     */
    public static void create(ConsoleLayout console) {
        // EventBus
        ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("console event bus");
                t.setDaemon(true);
                return t;
            }
        });
        AsyncEventBus newBus = new AsyncEventBus(service);
        INSTANCE.consoleBus.put(console, newBus);
        newBus.register(INSTANCE);
    }

    public static void clean() {
        for (Entry<ConsoleLayout, EventBus> entry : INSTANCE.consoleBus.entrySet()) {
            entry.getValue().unregister(INSTANCE);
        }
        INSTANCE.consoleBus.clear();
        INSTANCE.eventBus.unregister(INSTANCE);
        INSTANCE.mainSystemEventBus.unregister(INSTANCE);
        INSTANCE.otherSystemEventBus.unregister(INSTANCE);
    }

    /*
     * Subscribe
     */
    @Subscribe
    public void onDeadEvent(DeadEvent e) {
        System.out.println("found a dead event");
        System.out.println(e.getEvent().toString());
        System.out.println(e.getSource().toString());
    }

}
