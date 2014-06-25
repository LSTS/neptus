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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author pdias
 *
 */
public class QueueWorkTickets <C extends Object> {

    private int maximumTickets = 10;
    private LinkedBlockingQueue<C> waitingClients = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<C> workingClients = new LinkedBlockingQueue<>();

    public QueueWorkTickets(int maximumTickets) {
        this.maximumTickets = maximumTickets;
    }
    
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
    }
}
