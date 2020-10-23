/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 12, 2012
 */
package pt.lsts.neptus.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.renderer2d.InteractionAdapter;

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
            private final String namePrefix = NeptusEvents.class.getSimpleName() + "::global::"
                    + Integer.toHexString(NeptusEvents.this.hashCode());
            private final AtomicInteger counter = new AtomicInteger(0);
            private final ThreadGroup group = new ThreadGroup(namePrefix);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r);
                t.setName("Global Events Handler ::" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        eventBus = new AsyncEventBus(service);
        eventBus.register(this);
        service = Executors.newCachedThreadPool(new ThreadFactory() {
            private final String namePrefix = NeptusEvents.class.getSimpleName() + "::main-system::"
                    + Integer.toHexString(NeptusEvents.this.hashCode());
            private final AtomicInteger counter = new AtomicInteger(0);
            private final ThreadGroup group = new ThreadGroup(namePrefix);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r);
                t.setName("Main System Events Handler ::" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        mainSystemEventBus = new AsyncEventBus(service);
        mainSystemEventBus.register(this);
        service = Executors.newCachedThreadPool(new ThreadFactory() {
            private final String namePrefix = NeptusEvents.class.getSimpleName() + "::other-system::"
                    + Integer.toHexString(NeptusEvents.this.hashCode());
            private final AtomicInteger counter = new AtomicInteger(0);
            private final ThreadGroup group = new ThreadGroup(namePrefix);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r);
                t.setName("Other System Events Handler ::" + counter.getAndIncrement());
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
                        "tried to register in a console bus that doesnt exist. " + ((ConsolePanel) object).getName());
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
            private final String namePrefix = NeptusEvents.class.getSimpleName()
                    + "::" + ConsoleLayout.class.getSimpleName()  + "::console::"
                    + Integer.toHexString(console.hashCode());
            private final AtomicInteger counter = new AtomicInteger(0);
            private final ThreadGroup group = new ThreadGroup(namePrefix);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r);
                t.setName("Console Event Bus ::" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
        AsyncEventBus newBus = new AsyncEventBus(service);
        INSTANCE.consoleBus.put(console, newBus);
        newBus.register(INSTANCE);
    }
    
    public static void delete(ConsoleLayout console) {
        AsyncEventBus bus = (AsyncEventBus) INSTANCE.consoleBus.remove(console);
        bus.unregister(INSTANCE);
    }

    public static void clean() {
        for (Entry<ConsoleLayout, EventBus> entry : INSTANCE.consoleBus.entrySet()) {
            entry.getValue().unregister(INSTANCE);
        }
        INSTANCE.consoleBus.clear();
    }

    /*
     * Subscribe
     */
    @Subscribe
    public void onDeadEvent(DeadEvent e) {
        NeptusLog.pub().debug("Dead event of type "+e.getEvent().getClass().getSimpleName()+", sent by "+e.getSource().getClass().getSimpleName()+" has not been received by anyone.");
    }

}
