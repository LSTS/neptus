/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * Jun 25, 2014
 */
package pt.lsts.neptus.util.logdownload;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author pdias
 *
 */
public class QueueWorkTickets <C extends Object> {

    private int maximumTickets = 10;
    private LinkedBlockingQueue<C> waitingClients = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<C> workingClients = new LinkedBlockingQueue<>();
    private HashMap<C, QueueFuture> futures = new HashMap<>();

    public QueueWorkTickets(int maximumTickets) {
        this.maximumTickets = maximumTickets;
    }
    
    /**
     * Call this to lease a working ticket.
     * If is immediately granted, return true. If not returns false and one should {@link #release(Object)}
     * or periodically check with {@link #isLeased(Object)}. 
     * @param client
     * @return
     */
    public boolean lease(C client) {
        if (waitingClients.contains(client)) {
            return false;
        }
        else {
            synchronized (workingClients) {
                if (workingClients.contains(client)) {
                    return true;
                }
            }
        }
        
        waitingClients.offer(client);
        leaseNext();
        return isLeased(client);
    }

    public Future<Boolean> leaseAndWait(C client) {
        return leaseAndWait(client, null);
    }

    public Future<Boolean> leaseAndWait(C client, Callable<Boolean> callable) {
        QueueFuture future = new QueueFuture(callable);
        if (futures.containsKey(client)) {
            QueueFuture fTmp = futures.remove(client);
            fTmp.cancel(true);
        }
        boolean res = lease(client);
        if (res)
            runFuture(future);
        else
            futures.put(client, future);
        return future;
    }

    
    /**
     * @param future
     */
    private void runFuture(final QueueFuture future) {
        new Thread(QueueWorkTickets.class.getSimpleName() + ":: "
                + Integer.toHexString(QueueWorkTickets.this.hashCode())) {
            @Override
            public void run() {
                future.run();
            }  
        }.start();;
    }

    public boolean isLeased(C client) {
        boolean ret = workingClients.contains(client);
        if (!ret) {
            leaseNext();
            return workingClients.contains(client);
        }
        return ret;
    }

    public boolean release(C client) {
        boolean ret;
        synchronized (workingClients) {
            ret = workingClients.remove(client);
            ret |= waitingClients.remove(client);
            QueueFuture fTmp = futures.remove(client);
            if (fTmp != null)
                fTmp.cancel(true);
        }
        
        leaseNext();
        
        return ret;
    }
    
    private void leaseNext() {
        synchronized (workingClients) {
            if (workingClients.size() < maximumTickets) {
                try {
                    C client = waitingClients.poll(100, TimeUnit.MILLISECONDS);
                    if (client != null) {
                        workingClients.offer(client);
                        QueueFuture future = futures.remove(client);
                        if (future != null)
                            runFuture(future);
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void cancelAll() {
        synchronized (workingClients) {
            workingClients.clear();
            waitingClients.clear();
            for (QueueFuture ft : futures.values()) {
                ft.cancel(true);
            }
        }
    }

    private class QueueFuture implements Future<Boolean> {

        private Callable<Boolean> callable = null;
        private Boolean result = null;
        private boolean canceled = false;
        
        public QueueFuture(Callable<Boolean> callable) {
            this.callable = callable;
        }

        boolean run() {
            if (canceled == true) {
                result = false;
            }
            else if (callable != null) {
                try {
                    result = callable.call();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    result = false;
                }
            }
            else {
                result = true;
            }
            return result;
        }
        
        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
            while (result == null) {
                Thread.sleep(100);
            }
            return result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            canceled = true;
            return false;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                TimeoutException {
            long startTime = System.currentTimeMillis();
            while (result == null) {
                Thread.sleep(100);
                if (System.currentTimeMillis() - startTime > unit.toMillis(timeout))
                    throw new TimeoutException("Time out while waiting");
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            return canceled;
        }

        @Override
        public boolean isDone() {
            return result != null;
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        Object o4 = new Object();
        
        QueueWorkTickets<Object> qt = new QueueWorkTickets<>(2);
        System.out.println("true = " + qt.lease(o1));
        System.out.println("true = " + qt.lease(o2));
        System.out.println("false = " + qt.lease(o3));
        System.out.println("false = " + qt.lease(o4));
        System.out.println("----------------------------------");
        System.out.println("true = " + qt.release(o2));
        System.out.println("true = " + qt.lease(o3));
        System.out.println("false = " + qt.lease(o4));
        
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("----------------------------------");
        
        qt.cancelAll();
        
        System.out.println("true = " + qt.lease(o1));
        System.out.println("true = " + qt.lease(o2));
        
        Future<Boolean> ft = qt.leaseAndWait(o3, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                System.out.println("true = o3");
                return true;
            }
        });
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
        System.out.println("true = " + qt.release(o2));
        try { System.out.println("true = " + ft.get()); } catch (Exception e) { e.printStackTrace(); }
        
    }
}

