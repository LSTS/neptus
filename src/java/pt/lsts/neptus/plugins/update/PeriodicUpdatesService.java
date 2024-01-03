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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.plugins.update;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.ReflectionUtil;

public class PeriodicUpdatesService {

    protected static LinkedBlockingQueue<UpdateRequest> updateRequests = new LinkedBlockingQueue<UpdateRequest>();
    private static boolean updatesEnabled = true;
    private static final int DEFAULT_NUMBER_OF_THREADS = 2;
    private static boolean started = false;
    private static Vector<IPeriodicUpdates> clients = new Vector<IPeriodicUpdates>();
    private static Vector<IPeriodicUpdates> defunctClients = new Vector<IPeriodicUpdates>();
    private static Vector<Thread> updaterThreads = new Vector<Thread>();
    private static LinkedHashMap<Object, Collection<IPeriodicUpdates>> pojoPeriodicMethods = new LinkedHashMap<Object, Collection<IPeriodicUpdates>>();
    
    
    private PeriodicUpdatesService() {
    }

    protected static LinkedHashMap<Object, Long> updateTimes = new LinkedHashMap<Object, Long>();

    /**
     * Verifies that the updates are currently enabled
     * 
     * @return <b>true</b> if updates are enabled or <b>false</b> otherwise
     */
    public static boolean isUpdatesEnabled() {
        synchronized (PeriodicUpdatesService.class) {
            return updatesEnabled;
        }
    }

    /**
     * Deactivates updates
     */
    public static void stopUpdating() {
        synchronized (PeriodicUpdatesService.class) {
            updatesEnabled = false;
        }
    }
    
    public static void registerPojo(Object pojo) {
        
        Collection<IPeriodicUpdates> periodicMethods = PeriodicUpdatesService.inspect(pojo);
        
        if (periodicMethods.isEmpty())
            return;
        
        pojoPeriodicMethods.put(pojo, periodicMethods);
        
        for (IPeriodicUpdates i : periodicMethods) {
            PeriodicUpdatesService.register(i);
        }        
    }
    
    public static void unregisterPojo(Object pojo) {
        Collection<IPeriodicUpdates> updates = pojoPeriodicMethods.get(pojo);
        if (updates == null)
            return;
        
        for (IPeriodicUpdates i : updates) {
            PeriodicUpdatesService.unregister(i);
        }        
    }

    /**
     * Adds a new client that is to be updated periodically
     * 
     * @param client The client that wants to be warned at specific time intervals
     */
    public static void register(IPeriodicUpdates client) {
        if (clients.contains(client)) {
            NeptusLog.pub()
                    .info("Code in " + ReflectionUtil.getCallerStamp()
                            + " tried to add an already registered updates client");
            return;
        }
        updateTimes.put(client, 0L);
        clients.add(client);

        updateRequests.add(new UpdateRequest(client));
        if (!started) {
            started = true;
            for (int i = 0; i < DEFAULT_NUMBER_OF_THREADS; i++) {
                Thread t = getUpdaterThread("Periodic Updates - " + (i + 1));
                updaterThreads.add(t);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    public static Collection<IPeriodicUpdates> inspect(Object pojo) {
        Vector<IPeriodicUpdates> upReq = new Vector<>();
        Collection<Method> methods = ReflectionUtil.getMethodsAnnotatedWith(Periodic.class, pojo);

        if (methods.isEmpty())
            return upReq;

        for (Method m : methods) {

            if (m.getParameterTypes().length > 0) {
                NeptusLog.pub().error(
                        "The method " + pojo.getClass().getSimpleName() + "." + m.getName()
                                + "() is annotated as @Periodic but has a non-empty parameters list... ignored.");
                continue;
            }

            IPeriodicUpdates updatee = forge(pojo, m, m.getAnnotation(Periodic.class).millisBetweenUpdates());
            upReq.add(updatee);
        }

        return upReq;
    }

    private static IPeriodicUpdates forge(final Object o, final Method m, final long millisBetweenUpdates) {
        return new IPeriodicUpdates() {

            @Override
            public boolean update() {
                try {
                    Object ret = m.invoke(o);
                    return !Boolean.FALSE.equals(ret);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public long millisBetweenUpdates() {
                return millisBetweenUpdates;
            }
        };
    }

    /**
     * Removes a client that doesn't want to be updated anymore
     * 
     * @param client The client that wants to be removed
     */
    public static void unregister(IPeriodicUpdates client) {

        updateTimes.remove(client);

        if (clients.contains(client)) {
            clients.remove(client);
            if (clients.isEmpty()) {
                NeptusLog.pub().info("Periodic Listener Service with 0 clients - cleaning...");
                for (Thread t : updaterThreads) {
                    t.interrupt();
                }
                started = false;
                defunctClients.clear();
                updateRequests.clear();
            }
            else
                defunctClients.add(client);
        }
    }

    /**
     * Calculates the nearest upcoming update, waits the time until the update time and calls the update method
     */
    private static boolean nextUpdate() {
        UpdateRequest ur = null;

        try {
            ur = updateRequests.take();

            while (ur == null || ur.getNextUpdateTime() >= System.currentTimeMillis()) {
                if (defunctClients.contains(ur.getSource())) {
                    defunctClients.remove(ur.getSource());
                    ur = null;
                }

                if (ur != null)
                    updateRequests.put(ur);
                Thread.sleep(10);
                ur = updateRequests.take();
            }
            long time = System.currentTimeMillis();
            if (!updateTimes.containsKey(ur.getSource()))
                updateTimes.put(ur.getSource(), 0L);
            boolean not_finished = true;
            try {
                not_finished = ur.update();
            }
            catch (Exception e) {
                NeptusLog.pub().error("Exception: " + ReflectionUtil.getCallerStamp(), e);
            }
            catch (Error e) {
                NeptusLog.pub().error("Error: " + ReflectionUtil.getCallerStamp(), e);
            }
            time = System.currentTimeMillis() - time;
            Long lastTime = updateTimes.get(ur.getSource());
            if (lastTime != null)
                updateTimes.put(ur.getSource(), lastTime + time);

            if (not_finished)
                updateRequests.add(ur);
            else
                unregister(ur.getSource());
        }
        catch (InterruptedException e) {
            return false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Thread getUpdaterThread(String name) {
        Thread t = new Thread(name) {
            @Override
            public void run() {
                while (isUpdatesEnabled() && nextUpdate())
                    ;
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    }

    public static void main(String[] args) {

        PeriodicUpdatesService.register(new IPeriodicUpdates() {
            long previousTime = System.currentTimeMillis();

            @Override
            public long millisBetweenUpdates() {
                return 500;
            }

            @Override
            public boolean update() {
                NeptusLog.pub().info("<###>a " + (System.currentTimeMillis() - previousTime));
                previousTime = System.currentTimeMillis();
                return true;
            }
        });

        PeriodicUpdatesService.register(new IPeriodicUpdates() {
            long previousTime = System.currentTimeMillis();

            @Override
            public long millisBetweenUpdates() {
                return 900;
            }

            @Override
            public boolean update() {
                NeptusLog.pub().info("<###>b " + (System.currentTimeMillis() - previousTime));
                previousTime = System.currentTimeMillis();
                return true;
            }
        });
    }

}
