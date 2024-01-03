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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jun 4, 2020
 */
package pt.lsts.neptus.comm.manager.imc;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This class is used to check if a remote peer is reachable
 * @author zp
 */
public class ReachableCache {

    private static final int REACHABILITY_CACHE_MAX_AGE_MS = 120000;

    static LinkedHashMap<InetSocketAddress, HostReachability> reachabilityCache = new LinkedHashMap<>();
    static ExecutorService executorService = Executors.newCachedThreadPool();
    
    public static Future<Boolean> isReachable(int timeout, InetSocketAddress addr) {
        return executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isReachableBlocking(timeout, addr);
            }
        });
    }

    public static boolean isReachableBlocking(int timeout, InetSocketAddress addr) {

        synchronized (reachabilityCache) {
            HostReachability reachability = reachabilityCache.get(addr);
            if (reachability != null && reachability.getAge() < REACHABILITY_CACHE_MAX_AGE_MS) {
                return reachability.isReachable();
            }
            else {
                try {
                    if (addr.getAddress().isReachable(timeout))
                        reachabilityCache.put(addr, new HostReachability(true));
                    else
                        reachabilityCache.put(addr, new HostReachability(false));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    reachabilityCache.put(addr, new HostReachability(false));
                }
            }
            return reachabilityCache.get(addr).isReachable();
        }
    }

    public static InetSocketAddress firstReachable(int timeout, InetSocketAddress... addrs) {
        
        long endTime = System.currentTimeMillis() + timeout;

        LinkedHashMap<InetSocketAddress, Future<Boolean>> pings = new LinkedHashMap<>();

        for (InetSocketAddress addr : addrs)
            pings.put(addr, isReachable(timeout, addr));

        while(System.currentTimeMillis() < endTime) {
            try {
                for (Map.Entry<InetSocketAddress, Future<Boolean>> ping : pings.entrySet()) {
                    try {
                        if (ping.getValue().isDone() && ping.getValue().get()) {
                            return ping.getKey();
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Thread.sleep(10);                
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private static class HostReachability {
        long lastTested;
        boolean reachable;

        public HostReachability(boolean reachable) {
            this.reachable = reachable;
            this.lastTested = System.currentTimeMillis();
        }

        long getAge() {
            return System.currentTimeMillis() - lastTested;
        }

        boolean isReachable() {
            return reachable;
        }
    }
}