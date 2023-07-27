/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2022/08/22
 */
package pt.lsts.neptus.comm.transports.tcp;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.transports.IdPair;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class TCPStreamProcessor implements TCPMessageListener {
    private final ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {
        private final String namePrefix = TCPStreamProcessor.class.getSimpleName()
                + "::" + Integer.toHexString(TCPStreamProcessor.this.hashCode());
        private final AtomicInteger counter = new AtomicInteger(0);
        private final ThreadGroup group = new ThreadGroup(namePrefix);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r);
            t.setName(namePrefix + " :: " + (counter.getAndIncrement()));
            t.setDaemon(true);
            NeptusLog.pub().debug("Creating new worker thread " + t.getName() + " workers " + TCPStreamProcessor.this.listProcListeners.size());
            return t;
        }
    });

    private final Map<IdPair, TCPListenerStream> listProc = new HashMap<>();
    private final Map<IdPair, Runnable> listProcListeners = new HashMap<>();

    private BiFunction<IdPair, InputStream, Runnable> processorFactory;

    public TCPStreamProcessor(BiFunction<IdPair, InputStream, Runnable> processorFactory) {
        this.processorFactory = processorFactory;
        //service.execute(createWorker());
    }

    public void shutdown() {
        service.shutdown();
    }

    public long getActiveNumberOfConnections() {
        return listProc.size();
    }

    @Override
    public void onTCPMessageNotification(TCPNotification req) {
        IdPair id = IdPair.from(req.getAddress());
        TCPListenerStream proc;
        Runnable runnable;
        boolean spinRunnable = false;
        synchronized (listProc) {
            proc = listProc.get(id);
            runnable = listProcListeners.get(id);
            if (proc == null) {
                proc = new TCPListenerStream(id);
                listProc.put(id, proc);
                listProcListeners.remove(id);
                runnable = null; // To signal creation of new
            }
            if (runnable == null) {
                runnable = processorFactory.apply(id, proc.getInputStream());
                if (runnable != null) {
                    listProcListeners.put(id, runnable);
                    spinRunnable = true;
                }
            }
            if (req.isEosReceived()) {
                listProc.remove(id);
                listProcListeners.remove(id);
            }
        }
        TCPListenerStream finalProc = proc;
        service.execute(() -> {
            finalProc.onTCPMessageNotification(req);
        });

        if (spinRunnable) {
            service.execute(runnable);
        }
    }

//    private Thread createWorker() {
//        return new Thread() {
//            @Override
//            public void run() {
//                try {
//                    while(!isEosReached() && getInputStream().available() >= 0) { // the pis.available() not always when return '0' means end of stream
//                        if (processor == null || getInputStream().available() == 0) {
//                            Thread.yield();
//                            try { Thread.sleep(20); } catch (InterruptedException e) { }
//                            continue;
//                        }
//
//                        try {
//                            processor.accept(getInputStream());
//                        }
//                        catch (Exception e) {
//                            logger.warning(e.getMessage());
//                        }
////                            byte[] ba = new byte[pis.available()];
////                            if (ba.length > 0) {
////                            pis.read(ba);
////                            ByteUtil.dumpAsHex(ba, System.out);
////                            }
//                    }
//                }
//                catch (IOException e) {
//                    logger.warning(e.getMessage());
//                }
//                logger.finer("<###>pis.available()------------");
//                try {
//                    logger.finer("<###>pis.available()" + getInputStream().available());
//                }
//                catch (IOException e) {
//                    logger.warning(e.getMessage());
//                }
//            }
//        };
//    }
}
